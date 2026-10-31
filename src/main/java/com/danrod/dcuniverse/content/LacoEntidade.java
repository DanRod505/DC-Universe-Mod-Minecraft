package com.danrod.dcuniverse.content;

import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.guia.Guia;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Laço (tipo lasso): a laçada sai girando da mão e a corda fica presa ao dono. Estados:
 * - VOANDO: vai na direção da mira (cai um pouco, como corda de verdade) até o alcance;
 * - PRESO: fechou num ser vivo. A corda tem um comprimento: se o alvo se afasta além dele, é puxado de volta
 *   (prender). Mob laçado anda devagar e, com a corda esticada, não ataca nem foge. Um alvo pesado (maior que
 *   heavy_size de volume, chefes) não se mexe: é o dono que fica preso a ele (física no cliente, client/Laceador);
 * - ANCORADO: fechou num bloco e puxa o dono até lá (como o gancho);
 * - RECOLHE: a corda volta para a mão e some.
 * Usar de novo (sem custo) puxa: o alvo leve vem voando até o dono e fica numa guia curta de "leash" blocos (dá
 * para levar animais e mobs para onde quiser, até voando); o pesado puxa o dono até ele. Agachado, usar de novo
 * solta. O laço também solta depois de max_time segundos, se o alvo morrer ou se a distância passar muito do
 * comprimento (teleporte). A física dos alvos roda no servidor; o desenho da corda é do cliente.
 */
public class LacoEntidade extends Projectile {
    public static final int VOANDO = 0, PRESO = 1, ANCORADO = 2, RECOLHE = 3;
    private static final EntityDataAccessor<Integer> ESTADO = SynchedEntityData.defineId(LacoEntidade.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ALVO = SynchedEntityData.defineId(LacoEntidade.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DONO = SynchedEntityData.defineId(LacoEntidade.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> COMPRIMENTO = SynchedEntityData.defineId(LacoEntidade.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> PUXANDO = SynchedEntityData.defineId(LacoEntidade.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> PESADO = SynchedEntityData.defineId(LacoEntidade.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> COR = SynchedEntityData.defineId(LacoEntidade.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> PUXA_VEL = SynchedEntityData.defineId(LacoEntidade.class, EntityDataSerializers.FLOAT);

    /** O laço de cada jogador (só um por vez), no servidor. */
    private static final Map<UUID, LacoEntidade> ATIVOS = new ConcurrentHashMap<>();

    private final InterpolationHandler interpolacao = new InterpolationHandler(this, 1);
    private float alcance = 24, vel = 2.2f, dano = 4, pesadoVolume = 2.6f, guia = 3;
    private int maxTicks = 90 * 20;
    /** Ticks preso até o mob virar aliado (0 = nunca) e por quantos segundos ele fica do lado do dono. */
    private int aliadoApos;
    private float aliadoSeg = 60;
    private boolean virouAliado;
    private double viajado;
    private long presoEm, puxandoDesde;
    private Entity alvo;

    public LacoEntidade(EntityType<? extends LacoEntidade> tipo, Level level) {
        super(tipo, level);
        noPhysics = true;
    }

    public LacoEntidade(ServerPlayer dono, float alcance, float vel, float dano, float puxaVelBlocosSeg, float pesadoVolume, float guia,
                        float maxSeg, int cor) {
        this(Entidades.LACO.get(), dono.level());
        setOwner(dono);
        entityData.set(DONO, dono.getId());
        entityData.set(COR, cor);
        entityData.set(PUXA_VEL, puxaVelBlocosSeg / 20f);
        this.alcance = alcance;
        this.vel = vel;
        this.dano = dano;
        this.pesadoVolume = pesadoVolume;
        this.guia = guia;
        this.maxTicks = Math.round(maxSeg * 20);
        Vec3 olhar = dono.getLookAngle();
        setPos(dono.getEyePosition().add(olhar.scale(0.7)).add(0, -0.2, 0));
        setDeltaMovement(olhar.scale(vel).add(0, 0.05, 0));
    }

    /** O mob segurado por "apos" segundos vira aliado do dono por "dura" segundos (ability/Aliados). */
    public LacoEntidade aliados(float apos, float dura) {
        this.aliadoApos = Math.round(apos * 20);
        this.aliadoSeg = dura;
        return this;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(ESTADO, VOANDO);
        b.define(ALVO, -1);
        b.define(DONO, -1);
        b.define(COMPRIMENTO, 0f);
        b.define(PUXANDO, false);
        b.define(PESADO, false);
        b.define(COR, 0xFFC93C);
        b.define(PUXA_VEL, 1.3f);
    }

    // ------------------------------------------------------------------ leitura (cliente e servidor)

    public int estado() {
        return entityData.get(ESTADO);
    }

    public int alvoId() {
        return entityData.get(ALVO);
    }

    public int donoId() {
        return entityData.get(DONO);
    }

    public float comprimento() {
        return entityData.get(COMPRIMENTO);
    }

    public boolean puxando() {
        return entityData.get(PUXANDO);
    }

    public boolean pesado() {
        return entityData.get(PESADO);
    }

    public int cor() {
        return entityData.get(COR);
    }

    /** Velocidade da puxada (blocos/tick), para a física do dono no cliente. */
    public float puxaVel() {
        return entityData.get(PUXA_VEL);
    }

    /** Onde a corda sai do dono (mão direita), aproximado no servidor. */
    public static Vec3 mao(Entity dono) {
        float yaw = (dono instanceof LivingEntity v ? v.yBodyRot : dono.getYRot()) * ((float) Math.PI / 180f);
        Vec3 direita = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 frente = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        double s = dono instanceof LivingEntity v ? v.getScale() : 1;
        return dono.position().add(direita.scale(0.36 * s)).add(frente.scale(0.3 * s)).add(0, 1.3 * s, 0);
    }

    /** Ponto do alvo onde a laçada fecha: um pouco acima do meio do corpo (os braços). */
    public static Vec3 cintura(Entity alvo) {
        return alvo.position().add(0, alvo.getBbHeight() * 0.6, 0);
    }

    public static LacoEntidade ativo(Player player) {
        LacoEntidade l = ATIVOS.get(player.getUUID());
        return l != null && !l.isRemoved() ? l : null;
    }

    // ------------------------------------------------------------------ ciclo de vida

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolacao;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {}

    @Override
    protected void readAdditionalSaveData(ValueInput in) {}

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!level().isClientSide() && getOwner() instanceof ServerPlayer dono) {
            LacoEntidade antigo = ATIVOS.put(dono.getUUID(), this);
            if (antigo != null && antigo != this) antigo.discard();
        }
    }

    @Override
    public void onRemoval(RemovalReason motivo) {
        super.onRemoval(motivo);
        if (!level().isClientSide() && getOwner() instanceof ServerPlayer dono) ATIVOS.remove(dono.getUUID(), this);
    }

    public static void limpar() {
        ATIVOS.clear();
    }

    /** Destransformou: a corda some na hora. */
    public static void aoDestransformar(ServerPlayer player) {
        LacoEntidade l = ATIVOS.remove(player.getUUID());
        if (l != null) l.discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            interpolacao.interpolate();
            return;
        }
        if (!(getOwner() instanceof ServerPlayer dono) || !dono.isAlive() || dono.isRemoved() || dono.level() != level() || dono.isSpectator()) {
            discard();
            return;
        }
        switch (estado()) {
            case VOANDO -> voar(dono);
            case PRESO -> preso(dono);
            case ANCORADO -> ancorado(dono);
            default -> recolher(dono);
        }
    }

    // ------------------------------------------------------------------ voando

    private void voar(ServerPlayer dono) {
        if (tickCount % 5 == 1) level().playSound(null, getX(), getY(), getZ(), Sons.LACO_GIRO.get(), SoundSource.PLAYERS, 0.5f, 0.9f + random.nextFloat() * 0.2f);
        // A corda pesa: a laçada cai um pouco no fim do arremesso.
        Vec3 v = getDeltaMovement().add(0, -0.025, 0);
        Vec3 de = position(), ate = de.add(v);
        BlockHitResult bloco = level().clip(new ClipContext(de, ate, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 limite = bloco.getType() == HitResult.Type.MISS ? ate : bloco.getLocation();
        EntityHitResult ent = ProjectileUtil.getEntityHitResult(level(), this, de, limite, getBoundingBox().expandTowards(v).inflate(1.5),
            e -> podeLacar(dono, e), 0.75f); // a laçada é larga: pega quem passa a até 3/4 de bloco
        if (ent != null) {
            prender(dono, ent.getEntity());
            return;
        }
        if (bloco.getType() != HitResult.Type.MISS) {
            ancorar(dono, bloco);
            return;
        }
        setDeltaMovement(v);
        setPos(ate);
        viajado += v.length();
        if (viajado > alcance) soltar();
    }

    private static boolean podeLacar(ServerPlayer dono, Entity e) {
        if (e == dono || !(e instanceof LivingEntity vivo) || !vivo.isAlive() || e.isSpectator() || e.isPassengerOfSameVehicle(dono)) return false;
        // Os próprios animais e os aldeões também podem ser laçados (para levar); jogadores só com PvP.
        return !(e instanceof Player p) || dono.canHarmPlayer(p);
    }

    /** Chefes e mobs maiores que o limite não saem do lugar: o laço puxa o dono até eles. */
    private boolean ehPesado(Entity e) {
        if (com.danrod.dcuniverse.ability.Alvos.chefe(e)) return true;
        return e.getBbWidth() * e.getBbWidth() * e.getBbHeight() > pesadoVolume;
    }

    private void prender(ServerPlayer dono, Entity e) {
        alvo = e;
        presoEm = level().getGameTime();
        entityData.set(ESTADO, PRESO);
        entityData.set(ALVO, e.getId());
        entityData.set(PESADO, ehPesado(e));
        entityData.set(COMPRIMENTO, (float) Math.clamp(mao(dono).distanceTo(cintura(e)), 2.5, alcance));
        setPos(cintura(e));
        setDeltaMovement(Vec3.ZERO);
        // Só fere quem é inimigo: laçar a vaca para levar não machuca.
        if (e instanceof Enemy && dano > 0) Alvos.ferir(dono, e, damageSources().thrown(this, dono), dano);
        if (e instanceof Mob mob && !pesado()) mob.getNavigation().stop();
        ServerLevel level = (ServerLevel) level();
        Vec3 c = cintura(e);
        level.playSound(null, c.x, c.y, c.z, Sons.LACO_PRENDE.get(), SoundSource.PLAYERS, 1f, 0.95f + random.nextFloat() * 0.1f);
        level.sendParticles(new DustParticleOptions(cor(), 1.0f), c.x, c.y, c.z, 16, e.getBbWidth() * 0.5, 0.25, e.getBbWidth() * 0.5, 0);
        Rede.efeito(level, c.x, c.y, c.z, "laco", e.getBbWidth() * 0.5f + 0.2f, e.getBbHeight(), cor());
        Guia.concluir(dono, "laco");
    }

    private void ancorar(ServerPlayer dono, BlockHitResult hit) {
        Vec3 ponto = hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i()).scale(0.08));
        presoEm = level().getGameTime();
        setPos(ponto);
        setDeltaMovement(Vec3.ZERO);
        entityData.set(ESTADO, ANCORADO);
        entityData.set(PESADO, true);
        entityData.set(PUXANDO, true);
        entityData.set(COMPRIMENTO, (float) mao(dono).distanceTo(ponto));
        level().playSound(null, ponto.x, ponto.y, ponto.z, Sons.LACO_PRENDE.get(), SoundSource.PLAYERS, 0.7f, 1.2f);
        level().playSound(null, dono.getX(), dono.getY(), dono.getZ(), Sons.LACO_PUXA.get(), SoundSource.PLAYERS, 0.9f, 1f);
        Guia.concluir(dono, "laco");
    }

    // ------------------------------------------------------------------ preso

    private void preso(ServerPlayer dono) {
        Entity e = alvo != null ? alvo : level().getEntity(alvoId());
        long t = level().getGameTime();
        if (e == null || !e.isAlive() || e.isRemoved() || e.level() != level() || t - presoEm > maxTicks) {
            soltar();
            return;
        }
        alvo = e;
        Vec3 c = cintura(e);
        setPos(c);
        Vec3 m = mao(dono);
        Vec3 para = m.subtract(c);
        double d = para.length();
        float comp = comprimento();
        if (d > comp + 14) {
            soltar();
            return;
        }
        Vec3 dir = d < 1e-4 ? Vec3.ZERO : para.scale(1 / d);
        boolean puxa = puxando();
        if (!pesado()) {
            if (puxa) {
                // Puxão: o alvo vem voando num arco até a frente do dono.
                double perto = e.getBbWidth() / 2 + 1.5;
                if (d < perto || t - puxandoDesde > 40) {
                    entityData.set(PUXANDO, false);
                    entityData.set(COMPRIMENTO, Math.max(guia, (float) Math.min(d, comp)));
                    e.setDeltaMovement(e.getDeltaMovement().scale(0.2));
                } else {
                    double v = Math.min(puxaVel() * 1.6, 0.4 + (t - puxandoDesde) * 0.12);
                    e.setDeltaMovement(dir.scale(Math.min(d, v)).add(0, d > 4 ? 0.12 : 0.04, 0));
                }
                e.hurtMarked = true;
                e.resetFallDistance();
            } else if (d > comp) {
                // Corda esticada: tira a velocidade que afasta e puxa de volta o quanto passou.
                Vec3 vel = e.getDeltaMovement();
                double fora = -vel.dot(dir);
                if (fora > 0) vel = vel.add(dir.scale(fora));
                vel = vel.add(dir.scale(Math.min(0.9, (d - comp) * 0.3)));
                e.setDeltaMovement(vel);
                e.hurtMarked = true;
                e.resetFallDistance();
            }
            if (e instanceof Mob mob && aliadoApos > 0 && !virouAliado && t - presoEm >= aliadoApos) {
                virouAliado = true;
                // Convencido pela verdade, o mob passa a lutar do lado dela: o laço se solta e volta para a mão.
                if (com.danrod.dcuniverse.ability.Aliados.tornar(mob, dono, aliadoSeg)) {
                    soltar();
                    return;
                }
            }
            if (e instanceof Mob mob) {
                boolean esticado = d >= comp - 0.4;
                mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 10, esticado ? 4 : 1, false, false), dono);
                // Amarrado e com a corda esticada: não ataca nem foge.
                if (esticado || puxa) {
                    mob.setTarget(null);
                    mob.getNavigation().stop();
                }
            }
        } else {
            // Pesado: quem se mexe é o dono (cliente). Aqui só termina o puxão quando ele chega.
            dono.resetFallDistance();
            if (puxa && (dono.position().distanceTo(e.position()) < e.getBbWidth() / 2 + 2.2 || t - puxandoDesde > 80)) {
                entityData.set(PUXANDO, false);
                entityData.set(COMPRIMENTO, (float) Math.max(2.5, d));
            }
        }
        if (t % 8 == 0) {
            ((ServerLevel) level()).sendParticles(new DustParticleOptions(cor(), 0.6f), c.x, c.y, c.z, 2, e.getBbWidth() * 0.4, 0.15,
                e.getBbWidth() * 0.4, 0);
        }
    }

    // ------------------------------------------------------------------ ancorado e recolhendo

    private void ancorado(ServerPlayer dono) {
        dono.resetFallDistance();
        long t = level().getGameTime() - presoEm;
        double d = dono.position().add(0, dono.getBbHeight() * 0.5, 0).distanceTo(position());
        if (d < 1.9 || t > 100 || dono.isShiftKeyDown() && t > 4) soltar();
    }

    private void recolher(ServerPlayer dono) {
        Vec3 m = mao(dono);
        Vec3 d = m.subtract(position());
        double dist = d.length();
        if (dist < 1.2 || tickCount > 600) {
            discard();
            return;
        }
        double v = Math.min(dist, 1.2 + (tickCount % 40) * 0.05);
        setDeltaMovement(d.normalize().scale(v));
        setPos(position().add(getDeltaMovement()));
    }

    /** Afrouxa e recolhe a corda. */
    public void soltar() {
        if (estado() == RECOLHE) return;
        entityData.set(ESTADO, RECOLHE);
        entityData.set(PUXANDO, false);
        entityData.set(ALVO, -1);
        alvo = null;
        level().playSound(null, getX(), getY(), getZ(), Sons.LACO_SOLTA.get(), SoundSource.PLAYERS, 0.7f, 1f);
    }

    /** O dono usou o laço de novo: puxa (ou, agachado, solta). */
    public void usarDeNovo(ServerPlayer dono) {
        int e = estado();
        if (dono.isShiftKeyDown() || e != PRESO) {
            soltar();
            return;
        }
        if (puxando()) return;
        entityData.set(PUXANDO, true);
        puxandoDesde = level().getGameTime();
        dono.swing(InteractionHand.MAIN_HAND, true);
        level().playSound(null, dono.getX(), dono.getEyeY(), dono.getZ(), Sons.LACO_PUXA.get(), SoundSource.PLAYERS, 1f, 0.95f + random.nextFloat() * 0.1f);
        Guia.concluir(dono, "laco_puxar");
    }
}
