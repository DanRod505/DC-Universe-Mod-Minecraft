package com.danrod.dcuniverse.content;

import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.ability.active.Raio;
import com.danrod.dcuniverse.character.Params;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.joml.Vector3f;

import java.util.function.Predicate;
import com.danrod.dcuniverse.core.Eventos;
import net.neoforged.neoforge.common.NeoForge;

/**
 * A nave kryptoniana da Supergirl (vehicle, model nave_kryptoniana): a cápsula de cristal de Argo vinda do céu, que
 * voa como o Batwing (mesma física de piloto em client/Piloto) com outras armas. Botão de atacar segura o canhão
 * solar (um raio de calor contínuo pela proa que fere, põe fogo e funde gelo, neve e areia; esquenta e precisa
 * esfriar); botão de usar dispara um orbe solar que persegue o alvo na mira e estoura em luz; a tecla de usar
 * habilidade liga o escudo de cristal, que gasta carga enquanto ligado, deixa quem está dentro intocável e devolve
 * flechas e projéteis a quem atirou.
 */
public class NaveKryptoniana extends BatwingEntidade {
    /** A nave é desenhada em blocos, sem aumentar (client/NaveKryptonianaModelo). */
    public static final float ESCALA_NAVE = 1f;
    /** Altura do centro de giro no modelo. */
    public static final float PIVO_Y = 0.8f;
    /** Ponta do canhão no modelo (antes da escala), no nariz. */
    public static final Vector3f CANHAO = new Vector3f(0, 0.55f, 3.25f);
    public static final int ORBE = SECUNDARIA, ESCUDO = ESPECIAL;
    private static final float RAIO_ESCUDO = 3.6f;

    private static final EntityDataAccessor<Float> FEIXE = SynchedEntityData.defineId(NaveKryptoniana.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CARGA = SynchedEntityData.defineId(NaveKryptoniana.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> ESCUDO_LIGADO = SynchedEntityData.defineId(NaveKryptoniana.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> IMPACTO = SynchedEntityData.defineId(NaveKryptoniana.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ORBES = SynchedEntityData.defineId(NaveKryptoniana.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RECARGA = SynchedEntityData.defineId(NaveKryptoniana.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> CALOR_CANHAO = SynchedEntityData.defineId(NaveKryptoniana.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> TRAVADO = SynchedEntityData.defineId(NaveKryptoniana.class, EntityDataSerializers.INT);

    private int disparos;

    public NaveKryptoniana(EntityType<? extends NaveKryptoniana> tipo, Level level) {
        super(tipo, level);
    }

    public static NaveKryptoniana invocar(ServerPlayer dono, Params prm) {
        return preparar(new NaveKryptoniana(Entidades.NAVE_KRYPTONIANA.get(), dono.level()), dono, prm);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(FEIXE, 0f);
        b.define(CARGA, 1f);
        b.define(ESCUDO_LIGADO, false);
        b.define(IMPACTO, -1000);
        b.define(ORBES, 0);
        b.define(RECARGA, 0);
        b.define(CALOR_CANHAO, 0f);
        b.define(TRAVADO, -1);
    }

    @Override
    protected void aoInvocar() {
        entityData.set(ORBES, prm.i("orbs", 3));
        entityData.set(CARGA, 1f);
    }

    // ------------------------------------------------------------------ medidas e sons

    @Override
    public float escala() {
        return ESCALA_NAVE;
    }

    @Override
    public float pivo() {
        return PIVO_Y;
    }

    @Override
    protected double assentoY() {
        return 0.65;
    }

    @Override
    protected float assentoZ(boolean piloto) {
        return piloto ? 0.9f : -0.5f;
    }

    @Override
    protected SoundEvent somChegada() {
        return Sons.NAVE_CHEGA.get();
    }

    @Override
    public SoundEvent somMotor() {
        return Sons.NAVE_MOTOR.get();
    }

    @Override
    protected String tarefa() {
        return "nave";
    }

    // ------------------------------------------------------------------ consultas (painel e desenho)

    public boolean escudoAtivo() {
        return entityData.get(ESCUDO_LIGADO);
    }

    /** Carga do escudo, 0 a 1. */
    public float escudo() {
        return entityData.get(CARGA);
    }

    /** Ticks desde o último golpe no escudo (o desenho acende a onda nos primeiros). */
    public int impactoEscudo() {
        return (int) Math.min(1000, level().getGameTime() - entityData.get(IMPACTO));
    }

    /** Comprimento do raio do canhão agora (0 = desligado). */
    public float canhao() {
        return entityData.get(FEIXE);
    }

    public float calorCanhao() {
        return entityData.get(CALOR_CANHAO);
    }

    public int orbes() {
        return entityData.get(ORBES);
    }

    public float recargaOrbe() {
        int total = Math.round(prm.f("orb_reload", 6) * 20);
        return 1f - entityData.get(RECARGA) / (float) Math.max(1, total);
    }

    public int alvoOrbe() {
        return entityData.get(TRAVADO);
    }

    /** A ponta do canhão no mundo, girada com a nave. */
    public Vec3 bocaCanhao(float pt) {
        float pv = pivo() * escala();
        Vector3f v = rotacao(pt).transform(new Vector3f(CANHAO.x, CANHAO.y - pivo(), CANHAO.z).mul(escala()));
        return position().add(v.x, v.y + pv, v.z);
    }

    /** Para onde o nariz aponta. */
    public Vec3 proa(float pt) {
        Vector3f v = rotacao(pt).transform(new Vector3f(0, 0, 1));
        return new Vec3(v.x, v.y, v.z);
    }

    // ------------------------------------------------------------------ escudo

    /** Com o escudo ligado, quem está dentro da nave não se machuca e o golpe acende a onda no cristal. */
    public static void registrar() {
        NeoForge.EVENT_BUS.addListener(LivingIncomingDamageEvent.class, Eventos.cancela((Predicate<LivingIncomingDamageEvent>) e -> {
            if (!(e.getEntity().getVehicle() instanceof NaveKryptoniana n) || !n.escudoAtivo()) return false;
            n.golpe();
            return true;
        }));
    }

    private void golpe() {
        long agora = level().getGameTime();
        if (agora - entityData.get(IMPACTO) < 4) return;
        entityData.set(IMPACTO, (int) agora);
        level().playSound(null, getX(), getY() + 1, getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, 1.3f);
    }

    private void escudo(boolean ligar) {
        if (ligar == escudoAtivo()) return;
        if (ligar && escudo() < 0.15f) return;
        entityData.set(ESCUDO_LIGADO, ligar);
        level().playSound(null, getX(), getY(), getZ(), ligar ? Sons.NAVE_ESCUDO_LIGA.get() : Sons.NAVE_ESCUDO_DESLIGA.get(), SoundSource.PLAYERS, 1.2f, 1f);
    }

    /** Projéteis que chegam na bolha voltam para quem os atirou (já do lado de fora dela). */
    private void refletir(ServerLevel level) {
        Vec3 c = position().add(0, pivo() * escala(), 0);
        for (Projectile p : level.getEntitiesOfClass(Projectile.class, new AABB(c, c).inflate(RAIO_ESCUDO + 1))) {
            if (p instanceof OrbeSolar || p.getOwner() != null && (p.getOwner() == dono() || hasPassenger(p.getOwner()))) continue;
            Vec3 v = p.getDeltaMovement();
            Vec3 rel = p.position().subtract(c);
            if (rel.length() > RAIO_ESCUDO + 0.6 || v.dot(rel) >= 0) continue;
            Entity atirou = p.getOwner();
            Vec3 volta = atirou != null ? atirou.getEyePosition().subtract(p.position()).normalize() : rel.normalize();
            double vel = Math.max(0.8, v.length());
            p.setPos(c.add(rel.normalize().scale(RAIO_ESCUDO + 0.7)));
            p.setDeltaMovement(volta.scale(vel));
            if (dono() instanceof ServerPlayer sp) p.setOwner(sp);
            p.hurtMarked = true;
            golpe();
        }
    }

    // ------------------------------------------------------------------ armas

    @Override
    public void comando(ServerPlayer piloto, int acao) {
        if (pilotando(piloto) != this || estado() != PILOTADO) return;
        switch (acao) {
            case TIRO_LIGA -> atirando = true;
            case TIRO_DESLIGA -> atirando = false;
            case ORBE -> orbe(piloto);
            case ESCUDO -> escudo(!escudoAtivo());
            default -> {}
        }
    }

    @Override
    public void irEmbora() {
        super.irEmbora();
        entityData.set(FEIXE, 0f);
        entityData.set(ESCUDO_LIGADO, false);
    }

    @Override
    protected void armas(ServerLevel level, Entity dono) {
        boolean pode = estado() == PILOTADO && dono instanceof ServerPlayer;
        // Canhão solar: segura para manter o raio; esquenta e trava até esfriar.
        float calor = calorCanhao();
        if (pode && atirando && !superaquecido) {
            canhao(level, (ServerPlayer) dono);
            calor += 0.011f;
            if (calor >= 1) {
                superaquecido = true;
                level.playSound(null, getX(), getY(), getZ(), Sons.BATWING_SUPERAQUECE.get(), SoundSource.PLAYERS, 0.9f, 1.25f);
            }
        } else {
            if (canhao() > 0) entityData.set(FEIXE, 0f);
            calor -= superaquecido ? 0.009f : 0.016f;
            if (calor <= 0.2f) superaquecido = false;
        }
        entityData.set(CALOR_CANHAO, Mth.clamp(calor, 0, 1));
        // Orbes: recarregam um por vez.
        int max = prm.i("orbs", 3);
        int rec = entityData.get(RECARGA);
        if (orbes() < max) {
            if (rec <= 0) rec = Math.round(prm.f("orb_reload", 6) * 20);
            else if (--rec <= 0) entityData.set(ORBES, orbes() + 1);
            entityData.set(RECARGA, Math.max(0, rec));
        }
        // Escudo: gasta carga ligado, recarrega desligado.
        float carga = escudo();
        if (escudoAtivo()) {
            carga -= 1f / Math.max(20, prm.f("shield_time", 12) * 20);
            refletir(level);
            if (tickCount % 10 == 0) afastar(level);
            if (carga <= 0 || estado() != PILOTADO) escudo(false);
        } else {
            carga += 1f / Math.max(20, prm.f("shield_recharge", 18) * 20);
        }
        entityData.set(CARGA, Mth.clamp(carga, 0, 1));
        if (pode && tickCount % 4 == 0) {
            Entity alvo = procurarAlvo((ServerPlayer) dono);
            entityData.set(TRAVADO, alvo == null ? -1 : alvo.getId());
        }
    }

    /** Mobs que encostam na bolha são empurrados para fora. */
    private void afastar(ServerLevel level) {
        Vec3 c = position().add(0, pivo() * escala(), 0);
        for (Mob m : level.getEntitiesOfClass(Mob.class, new AABB(c, c).inflate(RAIO_ESCUDO))) {
            if (!(m instanceof net.minecraft.world.entity.monster.Enemy) || hasPassenger(m)) continue;
            Vec3 fora = m.position().subtract(c).multiply(1, 0, 1);
            fora = fora.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : fora.normalize();
            m.push(fora.x * 0.8, 0.3, fora.z * 0.8);
            m.hurtMarked = true;
            golpe();
        }
    }

    private void canhao(ServerLevel level, ServerPlayer piloto) {
        disparos++;
        Vec3 de = bocaCanhao(1);
        double alcance = prm.d("beam_range", 48);
        Vec3 fim = de.add(proa(1).scale(alcance));
        BlockHitResult bloco = level.clip(new ClipContext(de, fim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 limite = bloco.getType() == HitResult.Type.MISS ? fim : bloco.getLocation();
        EntityHitResult ent = ProjectileUtil.getEntityHitResult(level, this, de, limite, new AABB(de, limite).inflate(1),
            e -> e != this && !hasPassenger(e) && Alvos.podeAtingir(piloto, e), 0.6f);
        Vec3 acerto = ent != null ? ent.getLocation() : limite;
        entityData.set(FEIXE, (float) acerto.distanceTo(de));
        if (ent != null && disparos % 5 == 0) {
            Entity e = ent.getEntity();
            // Fogo antes do dano: se morrer com o golpe, a comida já cai assada.
            e.igniteForSeconds(4);
            Alvos.ferir(piloto, e, damageSources().inFire(), (float) prm.d("beam_damage", 2.5) * 2);
            e.invulnerableTime = 0;
            if (e instanceof LivingEntity vivo) com.danrod.dcuniverse.ability.Congelamento.derreter(vivo);
        } else if (ent == null && bloco.getType() == HitResult.Type.BLOCK && disparos % 10 == 0
            && piloto.mayBuild() && level.mayInteract(piloto, bloco.getBlockPos())) {
            Raio.esquentar(level, piloto, bloco, true, disparos % 20 == 0);
        }
        if (disparos % 3 == 0) {
            level.sendParticles(new DustParticleOptions(0xFFE27A, 1.4f), acerto.x, acerto.y, acerto.z, 4, 0.15, 0.15, 0.15, 0);
            if (disparos % 6 == 0) level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMALL_FLAME, acerto.x, acerto.y, acerto.z, 2, 0.1, 0.1, 0.1, 0.02);
        }
    }

    private void orbe(ServerPlayer piloto) {
        if (orbes() <= 0) return;
        int n = orbes();
        entityData.set(ORBES, n - 1);
        Vec3 de = bocaCanhao(1).add(proa(1).scale(0.6)).add(0, -0.35, 0);
        Entity alvo = procurarAlvo(piloto);
        Vec3 ponto = mira(piloto, prm.d("lock_range", 96));
        Vec3 dir = ponto.subtract(de).normalize();
        OrbeSolar o = new OrbeSolar(level(), piloto, de, dir, position().subtract(xo, yo, zo).length(), alvo,
            (float) prm.d("orb_damage", 8) * 2, prm.f("orb_radius", 3.5f));
        o.mirando(ponto);
        level().addFreshEntity(o);
        level().playSound(null, de.x, de.y, de.z, Sons.NAVE_ORBE.get(), SoundSource.PLAYERS, 1.5f, 1f);
    }

    @Override
    protected void particulas() {
        // Luz dourada saindo dos cristais de trás.
        if (estado() == CHEGANDO && tickCount < 3) return;
        Vec3 tras = proa(1).scale(-1);
        Vec3 p = position().add(0, pivo() * escala(), 0).add(tras.scale(2.1 * escala()));
        if (random.nextInt(2) == 0) {
            level().addParticle(new DustParticleOptions(random.nextBoolean() ? 0xBFE8FF : 0xFFE27A, 0.9f),
                p.x + (random.nextDouble() - 0.5) * 0.6, p.y + (random.nextDouble() - 0.5) * 0.4, p.z + (random.nextDouble() - 0.5) * 0.6, 0, 0, 0);
        }
    }
}
