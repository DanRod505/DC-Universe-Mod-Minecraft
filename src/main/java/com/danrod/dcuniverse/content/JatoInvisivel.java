package com.danrod.dcuniverse.content;

import com.danrod.dcuniverse.ability.Aliados;
import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.guia.Guia;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Predicate;

/**
 * O Jato Invisível da Mulher-Maravilha (vehicle, model jato_invisivel): a aeronave amazônica de vidro, que voa como o
 * Batwing (mesma física de piloto em client/Piloto) e leva o piloto e mais três. Monstros não enxergam o jato nem
 * quem está nele: perdem o alvo e não conseguem mirar ninguém a bordo. Comandos: botão de atacar abre a rampa e
 * a Diana salta em mergulho (ao bater no chão, um impacto em área com onda pelo chão); botão de usar acende o
 * feixe de embarque, que puxa para os assentos livres os jogadores e os aliados do laço por perto; a tecla de usar
 * habilidade liga a camuflagem total (o jato quase some até para os jogadores). Usar a suprema de novo com o jato
 * no mundo chama o jato de volta, e ele vem buscar a Diana onde ela estiver.
 */
public class JatoInvisivel extends BatwingEntidade {
    public static final int RAMPA = TIRO_LIGA, EMBARQUE = SECUNDARIA, CAMUFLAGEM = ESPECIAL;
    /** Altura do centro de giro no modelo (client/JatoModelo). */
    public static final float PIVO_Y = 0.9f;
    /** Assentos (x, z) no modelo: piloto na frente, dois lado a lado no meio e um atrás. */
    public static final float[][] ASSENTOS = {{0, 1.55f}, {0.62f, 0.35f}, {-0.62f, 0.35f}, {0, -0.85f}};

    private static final EntityDataAccessor<Boolean> CAMUFLADO = SynchedEntityData.defineId(JatoInvisivel.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> FEIXE = SynchedEntityData.defineId(JatoInvisivel.class, EntityDataSerializers.INT);

    /** A Diana saltou pela rampa e está mergulhando (o impacto vem quando ela tocar o chão). */
    private int mergulho;
    private int recargaEmbarque;

    public JatoInvisivel(EntityType<? extends JatoInvisivel> tipo, Level level) {
        super(tipo, level);
    }

    public static JatoInvisivel invocar(ServerPlayer dono, Params prm) {
        return preparar(new JatoInvisivel(Entidades.JATO_INVISIVEL.get(), dono.level()), dono, prm);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(CAMUFLADO, false);
        b.define(FEIXE, -1000);
    }

    // ------------------------------------------------------------------ medidas e sons

    @Override
    public float escala() {
        return 1f;
    }

    @Override
    public float pivo() {
        return PIVO_Y;
    }

    @Override
    protected double assentoY() {
        return 0.55;
    }

    @Override
    protected SoundEvent somChegada() {
        return Sons.JATO_CHEGA.get();
    }

    @Override
    public SoundEvent somMotor() {
        return Sons.JATO_MOTOR.get();
    }

    @Override
    protected String tarefa() {
        return "jato";
    }

    public boolean camuflado() {
        return entityData.get(CAMUFLADO);
    }

    /** Ticks desde o último feixe de embarque (o desenho acende o facho dourado embaixo do jato). */
    public int feixe() {
        return (int) Math.min(1000, level().getGameTime() - entityData.get(FEIXE));
    }

    public int lugaresLivres() {
        return ASSENTOS.length - getPassengers().size();
    }

    // ------------------------------------------------------------------ assentos

    @Override
    protected boolean canAddPassenger(Entity passageiro) {
        return getPassengers().size() < ASSENTOS.length && estado() != INDO;
    }

    /** Assento de cada um: o dono sempre no do piloto; os outros nos de trás, na ordem em que entraram. */
    private int assento(Entity passageiro) {
        if (passageiro.getId() == donoId()) return 0;
        int i = 1;
        for (Entity p : getPassengers()) {
            if (p == passageiro) return Math.min(i, ASSENTOS.length - 1);
            if (p.getId() != donoId()) i++;
        }
        return ASSENTOS.length - 1;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passageiro, EntityDimensions dim, float escala) {
        float[] a = ASSENTOS[assento(passageiro)];
        Vector3f v = rotacao(1).transform(new Vector3f(a[0], (float) assentoY() - PIVO_Y, a[1]));
        return new Vec3(v.x, v.y + PIVO_Y, v.z);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 onde) {
        if (player.isSecondaryUseActive() || hasPassenger(player)) return InteractionResult.PASS;
        if (!level().isClientSide()) {
            // O lugar do piloto fica guardado para a dona.
            int reservado = hasPassenger(dono()) ? 0 : 1;
            if (player.getId() == donoId()) embarcar((ServerPlayer) player);
            else if (getPassengers().size() < ASSENTOS.length - reservado) {
                player.startRiding(this);
                if (dono() instanceof ServerPlayer sp) Guia.concluir(sp, "jato_aliados");
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Longe, o jato vem buscar a dona (desce até ela e embarca), em vez de só esperar. */
    @Override
    public void chamar(ServerPlayer dono) {
        if (dono.distanceTo(this) < 8) {
            embarcar(dono);
            return;
        }
        if (estado() != PAIRANDO) return;
        origemChegada = position();
        chegada = dono.position().add(0, 0.6, 0);
        tempoEstado = 0;
        noPhysics = true;
        definirEstado(CHEGANDO);
        level().playSound(null, getX(), getY(), getZ(), somChegada(), SoundSource.PLAYERS, 1.4f, 1.1f);
    }

    // ------------------------------------------------------------------ invisível para monstros

    /** Monstros não conseguem mirar ninguém dentro do jato (nem o próprio jato). */
    public static void registrar() {
        LivingChangeTargetEvent.BUS.addListener((Predicate<LivingChangeTargetEvent>) e ->
            e.getEntity() instanceof Mob && e.getNewTarget() != null && e.getNewTarget().getVehicle() instanceof JatoInvisivel);
    }

    private void esconder(ServerLevel level) {
        for (Mob mob : level.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(64))) {
            LivingEntity alvo = mob.getTarget();
            if (alvo != null && (hasPassenger(alvo) || alvo == dono() && mergulho > 0)) mob.setTarget(null);
        }
    }

    // ------------------------------------------------------------------ comandos

    @Override
    public void comando(ServerPlayer piloto, int acao) {
        if (pilotando(piloto) != this || estado() != PILOTADO) return;
        switch (acao) {
            case RAMPA -> rampa(piloto);
            case EMBARQUE -> embarque(piloto);
            case CAMUFLAGEM -> {
                boolean novo = !camuflado();
                entityData.set(CAMUFLADO, novo);
                level().playSound(null, getX(), getY(), getZ(), novo ? Sons.BATWING_FURTIVO.get() : Sons.JATO_MOSTRA.get(),
                    SoundSource.PLAYERS, 1f, novo ? 1.25f : 1f);
            }
            default -> {}
        }
    }

    /** A rampa abre embaixo da cabine e a Diana salta de cabeça, rápida, para onde o jato aponta. */
    private void rampa(ServerPlayer piloto) {
        if (mergulho > 0) return;
        Vec3 frente = Vec3.directionFromRotation(0, getYRot());
        piloto.stopRiding();
        Vec3 vel = getDeltaMovement().scale(0.5).add(frente.scale(0.6)).add(0, -prm.d("dive_speed", 1.8), 0);
        piloto.setDeltaMovement(vel);
        piloto.hurtMarked = true;
        piloto.resetFallDistance();
        mergulho = 1;
        level().playSound(null, getX(), getY(), getZ(), Sons.JATO_RAMPA.get(), SoundSource.PLAYERS, 1.6f, 1f);
        Guia.concluir(piloto, "jato_rampa");
    }

    /** Feixe dourado: quem estiver perto (jogadores e aliados do laço) sobe para os assentos livres. */
    private void embarque(ServerPlayer piloto) {
        if (recargaEmbarque > 0 || !(level() instanceof ServerLevel level)) return;
        recargaEmbarque = 20;
        double alcance = prm.d("board_range", 14);
        entityData.set(FEIXE, (int) level.getGameTime());
        level.playSound(null, getX(), getY(), getZ(), Sons.JATO_EMBARQUE.get(), SoundSource.PLAYERS, 1.4f, 1f);
        List<LivingEntity> perto = level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(alcance), e ->
            e.isAlive() && !e.isPassenger() && e != piloto && !e.isSpectator()
                && (e instanceof Player || e instanceof Mob m && piloto.getUUID().equals(Aliados.donoDe(m))));
        perto.sort((a, b) -> Double.compare(a.distanceToSqr(this), b.distanceToSqr(this)));
        int subiram = 0;
        for (LivingEntity e : perto) {
            if (lugaresLivres() <= 0) break;
            if (e.distanceTo(this) > alcance) continue;
            Vec3 de = e.position().add(0, e.getBbHeight() / 2, 0), ate = position().add(0, PIVO_Y, 0);
            for (int i = 0; i <= 12; i++) {
                Vec3 p = de.lerp(ate, i / 12.0);
                level.sendParticles(new DustParticleOptions(0xFFE07A, 1.2f), p.x, p.y, p.z, 2, 0.08, 0.08, 0.08, 0);
            }
            if (e.startRiding(this, true, true)) subiram++;
        }
        if (subiram > 0) Guia.concluir(piloto, "jato_aliados");
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        if (recargaEmbarque > 0) recargaEmbarque--;
        if (tickCount % 5 == 0) esconder(level);
        if (mergulho > 0) mergulhar(level);
    }

    /** Segue a Diana no mergulho: rastro dourado e, ao tocar o chão (ou a água), o impacto. */
    private void mergulhar(ServerLevel level) {
        if (!(dono() instanceof ServerPlayer d) || d.isRemoved() || d.isPassenger()) {
            mergulho = 0;
            return;
        }
        mergulho++;
        // O jato desembarcou a dona: aqui ela cai de cabeça, sem planar (a queda amortecida não entra).
        EstadoJogador e = EstadoJogador.de(d);
        if (e.amortecendo()) {
            e.setAmortecendo(false);
            Rede.sincronizar(d);
        }
        d.resetFallDistance();
        Vec3 v = d.getDeltaMovement();
        if (v.y > -prm.d("dive_speed", 1.8) && mergulho < 80) {
            d.setDeltaMovement(v.x, Math.max(-prm.d("dive_speed", 1.8), v.y - 0.12), v.z);
            d.hurtMarked = true;
        }
        level.sendParticles(new DustParticleOptions(0xFFD54A, 1.0f), d.getX(), d.getY() + 1, d.getZ(), 3, 0.2, 0.4, 0.2, 0);
        if (d.onGround() || d.isInWater() || mergulho > 200) {
            if (d.onGround()) impacto(level, d);
            mergulho = 0;
        }
    }

    /** A Diana aterrissa do mergulho: onda de choque pelo chão que fere, joga para cima e empurra. */
    private void impacto(ServerLevel level, ServerPlayer d) {
        Personagem p = EstadoJogador.de(d).personagem();
        double raio = prm.d("dive_radius", 6);
        float dano = p == null ? 8 : Alvos.dano(p, prm.d("dive_damage_mult", 1.0));
        Vec3 c = d.position();
        for (LivingEntity alvo : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(raio, 3, raio), x -> Alvos.podeAtingir(d, x))) {
            double dist = alvo.position().distanceTo(c);
            if (dist > raio) continue;
            Alvos.ferir(d, alvo, level.damageSources().playerAttack(d), dano);
            Vec3 fora = alvo.position().subtract(c).multiply(1, 0, 1);
            fora = fora.lengthSqr() < 1e-4 ? Vec3.ZERO : fora.normalize();
            double f = 1.4 * (1 - 0.5 * dist / raio);
            alvo.setDeltaMovement(alvo.getDeltaMovement().add(fora.x * f, 0.6, fora.z * f));
            alvo.hurtMarked = true;
            alvo.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 3), d);
        }
        BlockState chao = level.getBlockState(BlockPos.containing(c.x, c.y - 0.2, c.z));
        level.playSound(null, c.x, c.y, c.z, Sons.MERGULHO_POUSO.get(), SoundSource.PLAYERS, 2f, 1f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6f, 0.6f);
        level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.5, c.z, 2, 0.5, 0.2, 0.5, 0);
        Rede.efeito(level, c.x, c.y + 0.02, c.z, "slam", (float) raio, 8, Block.getId(chao));
        Guia.concluir(d, "jato_mergulho");
    }

    @Override
    protected void particulas() {
        // Quase nada: um brilho fraco nos bocais, e só com o jato andando.
        if (estado() == CHEGANDO && tickCount < 3 || camuflado()) return;
        double v = position().subtract(xo, yo, zo).length();
        if (v < 0.25 || random.nextInt(3) != 0) return;
        Vector3f tras = rotacao(1).transform(new Vector3f(random.nextBoolean() ? 0.42f : -0.42f, 0, -3.5f));
        level().addParticle(new DustParticleOptions(0xCFEFFF, 0.6f), getX() + tras.x, getY() + PIVO_Y + tras.y, getZ() + tras.z, 0, 0, 0);
    }

    @Override
    protected void armas(ServerLevel level, Entity dono) {
        // Sem armas: o jato é transporte e emboscada (rampa, embarque e camuflagem são comandos).
    }
}
