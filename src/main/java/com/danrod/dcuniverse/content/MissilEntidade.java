package com.danrod.dcuniverse.content;

import com.danrod.dcuniverse.ability.Alvos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Míssil teleguiado do Batwing: cai um instante da asa, acende o motor e persegue o alvo travado (ou vai até o ponto
 * mirado, ou segue reto).
 * Explode ao bater ou no fim do combustível: fere e empurra em volta, sem quebrar blocos.
 */
public class MissilEntidade extends Projectile {
    private final InterpolationHandler interpolacao = new InterpolationHandler(this, 1);
    private Entity alvo;
    private Vec3 ponto;
    private float dano = 14, raio = 3.5f, vel;

    public MissilEntidade(EntityType<? extends MissilEntidade> tipo, Level level) {
        super(tipo, level);
        noPhysics = true;
    }

    public MissilEntidade(Level level, ServerPlayer dono, Vec3 de, Vec3 dir, double velInicial, Entity alvo, float dano, float raio) {
        this(Entidades.MISSIL.get(), level, dono, de, dir, velInicial, alvo, dano, raio);
    }

    protected MissilEntidade(EntityType<? extends MissilEntidade> tipo, Level level, ServerPlayer dono, Vec3 de, Vec3 dir, double velInicial, Entity alvo, float dano, float raio) {
        this(tipo, level);
        setOwner(dono);
        this.alvo = alvo;
        this.dano = dano;
        this.raio = raio;
        this.vel = (float) Math.max(0.6, velInicial);
        setPos(de);
        setDeltaMovement(dir.scale(vel));
        apontar(getDeltaMovement());
    }

    /** Sem alvo travado, vai até este ponto (onde a mira do piloto apontava) em vez de seguir reto. */
    public MissilEntidade mirando(Vec3 ponto) {
        this.ponto = ponto;
        return this;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {}

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

    private void apontar(Vec3 v) {
        double h = v.horizontalDistance();
        setYRot((float) (Mth.atan2(v.x, v.z) * Mth.RAD_TO_DEG));
        setXRot((float) (Mth.atan2(v.y, h) * Mth.RAD_TO_DEG));
        yRotO = getYRot();
        xRotO = getXRot();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            interpolacao.interpolate();
            rastro();
            return;
        }
        Vec3 v = getDeltaMovement();
        if (tickCount > acende()) {
            // Motor aceso: acelera e persegue o alvo virando aos poucos.
            vel = Math.min(velMaxima(), vel + 0.12f);
            Vec3 quer = v.normalize();
            if (alvo != null && alvo.isAlive()) quer = alvo.getBoundingBox().getCenter().subtract(position()).normalize();
            else if (ponto != null && position().distanceToSqr(ponto) > 1) quer = ponto.subtract(position()).normalize();
            Vec3 dir = v.normalize().lerp(quer, 0.2).normalize();
            v = dir.scale(vel);
        }
        setDeltaMovement(v);
        apontar(v);
        Vec3 de = position(), ate = de.add(v);
        BlockHitResult bloco = level().clip(new ClipContext(de, ate, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 limite = bloco.getType() == HitResult.Type.MISS ? ate : bloco.getLocation();
        EntityHitResult ent = ProjectileUtil.getEntityHitResult(level(), this, de, limite, getBoundingBox().expandTowards(v).inflate(0.8),
            e -> e instanceof LivingEntity && e.isAlive() && e != getOwner() && !(getOwner() != null && e.getVehicle() != null && e.getVehicle() == getOwner().getVehicle()));
        if (ent != null) {
            explodir(ent.getLocation());
        } else if (bloco.getType() != HitResult.Type.MISS || tickCount > 120) {
            explodir(limite);
        } else {
            setPos(ate);
        }
    }

    /** Ticks caindo antes de acender o motor e começar a perseguir. */
    protected int acende() {
        return 4;
    }

    protected float velMaxima() {
        return 2.8f;
    }

    /** Fumaça e chama atrás (no cliente). */
    protected void rastro() {
        if (tickCount <= 4) return;
        Vec3 tras = Vec3.directionFromRotation(-getXRot(), -getYRot()).scale(-0.5);
        level().addParticle(ParticleTypes.FLAME, getX() + tras.x, getY() + tras.y, getZ() + tras.z, 0, 0, 0);
        level().addParticle(ParticleTypes.LARGE_SMOKE, getX() + tras.x * 2, getY() + tras.y * 2, getZ() + tras.z * 2, 0, 0.02, 0);
    }

    protected void estouro(ServerLevel level, Vec3 onde) {
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, onde.x, onde.y, onde.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.FLAME, onde.x, onde.y, onde.z, 30, 0.6, 0.6, 0.6, 0.15);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, onde.x, onde.y, onde.z, 24, 0.8, 0.8, 0.8, 0.06);
        level.playSound(null, onde.x, onde.y, onde.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.5f, 0.9f);
    }

    private void explodir(Vec3 onde) {
        ServerLevel level = (ServerLevel) level();
        estouro(level, onde);
        ServerPlayer dono = getOwner() instanceof ServerPlayer sp ? sp : null;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(onde, onde).inflate(raio))) {
            double d = Math.sqrt(e.getBoundingBox().distanceToSqr(onde));
            if (d > raio || dono == null || !Alvos.podeAtingir(dono, e) || e.getVehicle() instanceof VeiculoDono) continue;
            float f = (float) (1 - d / (raio + 1));
            Alvos.ferir(dono, e, damageSources().explosion(this, dono), dano * f);
            Vec3 fora = e.position().subtract(onde);
            fora = fora.lengthSqr() < 1e-4 ? new Vec3(0, 1, 0) : fora.normalize();
            e.push(fora.x * 1.2 * f, 0.6 * f, fora.z * 1.2 * f);
            e.hurtMarked = true;
        }
        discard();
    }
}
