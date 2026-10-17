package com.danrod.dcuniverse.content;

import com.danrod.dcuniverse.ability.Alvos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Rede do lançador (tipo net): sai dobrada, abre no ar e cai um pouco no caminho. Acertando alguém, embrulha o alvo
 * em teias de verdade (content/Teias) em todos os blocos que ele ocupa, e ele fica preso até elas sumirem; batendo
 * num bloco, abre uma armadilha de teias 3x3 grudada na superfície. A física roda no servidor; o cliente só
 * interpola e desenha (client/render/RedeRenderer).
 */
public class RedeEntidade extends Projectile {
    private final InterpolationHandler interpolacao = new InterpolationHandler(this, 1);
    private int vida = 20, prendeTicks = 120, armadilhaTicks = 300;

    public RedeEntidade(EntityType<? extends RedeEntidade> tipo, Level level) {
        super(tipo, level);
        noPhysics = true;
    }

    public RedeEntidade(LivingEntity dono, float alcance, float vel, float prendeSeg, float armadilhaSeg) {
        this(Entidades.REDE.get(), dono.level());
        setOwner(dono);
        vida = Math.max(4, Math.round(alcance / vel));
        prendeTicks = Math.round(prendeSeg * 20);
        armadilhaTicks = Math.round(armadilhaSeg * 20);
        Vec3 olhar = dono.getLookAngle();
        setPos(dono.getEyePosition().add(olhar.scale(0.8)).add(0, -0.2, 0));
        setDeltaMovement(olhar.scale(vel));
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder b) {}

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
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            interpolacao.interpolate();
            return;
        }
        Entity dono = getOwner();
        if (dono == null || !dono.isAlive() || dono.level() != level() || tickCount > vida + 20) {
            discard();
            return;
        }
        // Voa reto no começo e depois vai caindo, como uma rede de pesos.
        Vec3 v = getDeltaMovement();
        if (tickCount > vida) v = v.scale(0.9).add(0, -0.08, 0);
        setDeltaMovement(v);
        Vec3 de = position(), ate = de.add(v);
        BlockHitResult bloco = level().clip(new ClipContext(de, ate, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 limite = bloco.getType() == HitResult.Type.MISS ? ate : bloco.getLocation();
        EntityHitResult ent = ProjectileUtil.getEntityHitResult(level(), this, de, limite, getBoundingBox().expandTowards(v).inflate(0.7),
            this::podeAtingir);
        if (ent != null) {
            setPos(ent.getLocation());
            prender((LivingEntity) ent.getEntity());
            return;
        }
        if (bloco.getType() != HitResult.Type.MISS) {
            setPos(bloco.getLocation());
            armadilha(bloco);
            return;
        }
        setPos(ate);
    }

    private boolean podeAtingir(Entity e) {
        if (!(e instanceof LivingEntity) || !e.isAlive() || e == getOwner()) return false;
        return !(getOwner() instanceof ServerPlayer sp) || Alvos.podeAtingir(sp, e);
    }

    private boolean pode(ServerLevel level, BlockPos p) {
        return !(getOwner() instanceof ServerPlayer sp) || sp.mayBuild() && level.mayInteract(sp, p);
    }

    /** Embrulha o alvo: teia em cada bloco que a caixa dele toca (até 3x3x3) e um tranco para baixo. */
    private void prender(LivingEntity alvo) {
        ServerLevel level = (ServerLevel) level();
        long ate = level.getGameTime() + prendeTicks;
        AABB caixa = alvo.getBoundingBox();
        BlockPos a = BlockPos.containing(caixa.minX, caixa.minY, caixa.minZ), b = BlockPos.containing(caixa.maxX - 1e-3, caixa.maxY - 1e-3, caixa.maxZ - 1e-3);
        int postas = 0;
        for (BlockPos p : BlockPos.betweenClosed(a, b)) {
            if (Math.abs(p.getX() - a.getX()) > 2 || Math.abs(p.getY() - a.getY()) > 2 || Math.abs(p.getZ() - a.getZ()) > 2) continue;
            if (pode(level, p) && Teias.por(level, p, ate)) postas++;
        }
        // Sem lugar para teia (área protegida, dentro d'água): a rede ainda segura um pouco.
        alvo.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, Math.min(80, prendeTicks), postas > 0 ? 2 : 5), getOwner());
        alvo.setDeltaMovement(alvo.getDeltaMovement().multiply(0.2, 0, 0.2).add(0, -0.3, 0));
        alvo.hurtMarked = true;
        if (alvo instanceof Mob m) m.getNavigation().stop();
        Vec3 c = alvo.getBoundingBox().getCenter();
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBWEB.defaultBlockState()), c.x, c.y, c.z, 30,
            alvo.getBbWidth() / 2, alvo.getBbHeight() / 2, alvo.getBbWidth() / 2, 0.1);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1f, 0.7f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.LEAD_TIED, SoundSource.PLAYERS, 1f, 0.8f);
        discard();
    }

    /** Armadilha na superfície: teias 3x3 no plano do bloco atingido, uma camada para fora dele. */
    private void armadilha(BlockHitResult hit) {
        ServerLevel level = (ServerLevel) level();
        long ate = level.getGameTime() + armadilhaTicks;
        Direction face = hit.getDirection();
        BlockPos centro = hit.getBlockPos().relative(face);
        Direction.Axis eixo = face.getAxis();
        int postas = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                BlockPos p = switch (eixo) {
                    case X -> centro.offset(0, i, j);
                    case Y -> centro.offset(i, 0, j);
                    case Z -> centro.offset(i, j, 0);
                };
                // A teia precisa de apoio: o bloco atrás dela (na direção da face) tem que ser sólido.
                if (!level.getBlockState(p.relative(face.getOpposite())).isFaceSturdy(level, p.relative(face.getOpposite()), face)) continue;
                if (pode(level, p) && Teias.por(level, p, ate)) postas++;
            }
        }
        Vec3 c = Vec3.atCenterOf(centro);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBWEB.defaultBlockState()), c.x, c.y, c.z, 20, 0.8, 0.8, 0.8, 0.05);
        level.playSound(null, c.x, c.y, c.z, postas > 0 ? SoundEvents.WOOL_PLACE : SoundEvents.WOOL_HIT, SoundSource.PLAYERS, 1f, 0.8f);
        discard();
    }
}
