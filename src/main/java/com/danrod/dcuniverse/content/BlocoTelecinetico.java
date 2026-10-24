package com.danrod.dcuniverse.content;

import com.danrod.dcuniverse.ability.Alvos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * Um bloco arrancado pela telecinese tátil do Superboy (ability/active/Telecinese). Segurado, flutua girando devagar
 * na formação à frente do dono, com a mesma posição relativa que tinha no chão. Lançado, voa em arco, fere quem
 * acerta e, ao bater, volta a ser bloco ali (ou cai como item se não couber). Pousado, desliza até o lugar marcado
 * e vira bloco. Se sumir antes (o dono saiu, o mundo fechou), o bloco é devolvido como item: nada se perde.
 */
public class BlocoTelecinetico extends Entity {
    public static final int SEGURO = 0, VOANDO = 1, POUSANDO = 2;
    private static final EntityDataAccessor<BlockState> BLOCO = SynchedEntityData.defineId(BlocoTelecinetico.class, EntityDataSerializers.BLOCK_STATE);
    private static final EntityDataAccessor<Integer> FASE = SynchedEntityData.defineId(BlocoTelecinetico.class, EntityDataSerializers.INT);

    private final InterpolationHandler interpolacao = new InterpolationHandler(this, 2);
    private ServerPlayer dono;
    /** Lugar na formação (em relação ao centro do grupo) e destino ao pousar. */
    private Vec3 rel = Vec3.ZERO;
    private BlockPos destino;
    private Vec3 vel = Vec3.ZERO;
    private float dano;
    /** Quantas vezes cada alvo já foi atingido por este grupo (compartilhado entre os blocos do mesmo lançamento). */
    private Map<Integer, Integer> golpes;
    private int voo;
    private boolean resolvido;

    /** Giro visual (graus por tick) e a fase de cada bloco, para não girarem todos juntos. */
    public final float giroX, giroY, fase;

    public BlocoTelecinetico(EntityType<? extends BlocoTelecinetico> tipo, Level level) {
        super(tipo, level);
        noPhysics = true;
        setNoGravity(true);
        giroX = (random.nextFloat() - 0.5f) * 3f;
        giroY = (random.nextFloat() - 0.5f) * 3f;
        fase = random.nextFloat() * 360f;
    }

    public static BlocoTelecinetico criar(ServerLevel level, ServerPlayer dono, BlockPos pos, BlockState st, Vec3 rel) {
        BlocoTelecinetico b = new BlocoTelecinetico(Entidades.BLOCO_TK.get(), level);
        b.entityData.set(BLOCO, st);
        b.dono = dono;
        b.rel = rel;
        b.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        level.addFreshEntity(b);
        return b;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(BLOCO, Blocks.STONE.defaultBlockState());
        b.define(FASE, SEGURO);
    }

    public BlockState bloco() {
        return entityData.get(BLOCO);
    }

    public int fase() {
        return entityData.get(FASE);
    }

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
    public boolean hurtServer(ServerLevel level, DamageSource fonte, float dano) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 96 * 96;
    }

    // ------------------------------------------------------------------ comandos (ability/active/Telecinese)

    public void lancar(Vec3 velocidade, float dano, Map<Integer, Integer> golpes) {
        vel = velocidade;
        this.dano = dano;
        this.golpes = golpes;
        voo = 0;
        entityData.set(FASE, VOANDO);
    }

    /** Solta sem força: cai e vira bloco onde parar. */
    public void soltar() {
        lancar(Vec3.ZERO, 0, Map.of());
    }

    public void pousar(BlockPos onde) {
        destino = onde.immutable();
        entityData.set(FASE, POUSANDO);
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            interpolacao.interpolate();
            if (random.nextInt(fase() == SEGURO ? 6 : 3) == 0) {
                level().addParticle(new net.minecraft.core.particles.DustParticleOptions(0x8FD0FF, 0.7f),
                    getX() + (random.nextDouble() - 0.5) * 1.2, getY() + random.nextDouble() * 1.1, getZ() + (random.nextDouble() - 0.5) * 1.2, 0, 0, 0);
            }
            return;
        }
        ServerLevel level = (ServerLevel) level();
        switch (fase()) {
            case SEGURO -> segurar(level);
            case VOANDO -> voar(level);
            case POUSANDO -> descer(level);
            default -> {}
        }
    }

    /** Segue a formação na frente do dono, um pouco acima da cabeça (para não tapar a mira). */
    private void segurar(ServerLevel level) {
        if (dono == null || dono.isRemoved() || dono.level() != level) {
            soltar();
            return;
        }
        float yaw = dono.getYRot() * Mth.DEG_TO_RAD;
        // Acima e à frente, quase na horizontal: a carga acompanha o olhar sem tapar a mira.
        Vec3 frente = Vec3.directionFromRotation(net.minecraft.util.Mth.clamp(dono.getXRot(), -30, 15), dono.getYRot());
        Vec3 centro = dono.getEyePosition().add(frente.scale(3.2)).add(0, 1.9, 0);
        Vec3 alvo = centro.add(rel.yRot(-yaw)).add(0, Math.sin((tickCount + fase) * 0.12) * 0.08, 0);
        // Pés da entidade = base do bloco: o centro do bloco fica meio bloco acima.
        alvo = alvo.add(0, -0.5, 0);
        Vec3 p = position();
        setPos(p.lerp(alvo, tickCount < 10 ? 0.18 : 0.35));
    }

    private void voar(ServerLevel level) {
        voo++;
        vel = vel.add(0, -0.045, 0).scale(0.99);
        Vec3 de = position().add(0, 0.5, 0), ate = de.add(vel);
        // Fere quem estiver no caminho (cada alvo no máximo 3 vezes por lançamento).
        if (dano > 0 && dono != null) {
            AABB caixa = new AABB(de, ate).inflate(0.55);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, caixa, e -> Alvos.podeAtingir(dono, e))) {
                int n = golpes.getOrDefault(e.getId(), 0);
                if (n >= 3) continue;
                golpes.put(e.getId(), n + 1);
                Alvos.ferir(dono, e, level.damageSources().thrown(this, dono), dano);
                e.invulnerableTime = 0;
                Vec3 emp = vel.normalize().scale(0.8);
                e.setDeltaMovement(e.getDeltaMovement().add(emp.x, 0.3, emp.z));
                e.hurtMarked = true;
                vel = vel.scale(0.55);
            }
            Alvos.cristais(dono, caixa, level.damageSources().thrown(this, dono));
        }
        BlockHitResult hit = level.clip(new ClipContext(de, ate, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() != HitResult.Type.MISS) {
            BlockPos onde = hit.getBlockPos().relative(hit.getDirection());
            setPos(hit.getLocation().subtract(0, 0.5, 0));
            assentar(level, onde, vel.length() > 0.6);
            return;
        }
        setPos(position().add(vel));
        if (voo > 200 || getY() < level.getMinY()) devolver(level);
    }

    private void descer(ServerLevel level) {
        Vec3 alvo = new Vec3(destino.getX() + 0.5, destino.getY(), destino.getZ() + 0.5);
        Vec3 p = position();
        if (p.distanceToSqr(alvo) < 0.02) {
            assentar(level, destino, false);
            return;
        }
        Vec3 d = alvo.subtract(p);
        double l = d.length();
        setPos(l < 0.5 ? alvo : p.add(d.scale(Math.min(0.6, Math.max(0.12, l * 0.25)) / l)));
    }

    /** Vira bloco em onde (se couber e pode construir ali); senão cai como item. */
    private void assentar(ServerLevel level, BlockPos onde, boolean forte) {
        if (resolvido) return;
        resolvido = true;
        BlockState st = bloco();
        boolean pode = dono == null || level.mayInteract(dono, onde) && dono.mayBuild();
        if (pode && level.isLoaded(onde) && level.getBlockState(onde).canBeReplaced() && st.canSurvive(level, onde)) {
            level.setBlockAndUpdate(onde, Block.updateFromNeighbourShapes(st, level, onde));
            level.playSound(null, onde, st.getSoundType().getPlaceSound(), SoundSource.BLOCKS, forte ? 1.2f : 0.8f, forte ? 0.7f : 0.9f);
        } else {
            soltarItem(level, st, position());
        }
        if (forte) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), getX(), getY() + 0.5, getZ(), 18, 0.3, 0.3, 0.3, 0.15);
        }
        discard();
    }

    private void devolver(ServerLevel level) {
        if (resolvido) return;
        resolvido = true;
        soltarItem(level, bloco(), position());
        discard();
    }

    private static void soltarItem(ServerLevel level, BlockState st, Vec3 onde) {
        ItemStack it = new ItemStack(st.getBlock().asItem());
        if (it.isEmpty()) return;
        ItemEntity item = new ItemEntity(level, onde.x, onde.y + 0.5, onde.z, it);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }

    @Override
    public void remove(RemovalReason motivo) {
        // Sumiu sem virar bloco (o dono saiu, o chunk descarregou, o servidor fechou): devolve como item.
        if (!resolvido && level() instanceof ServerLevel level && motivo != RemovalReason.KILLED) {
            resolvido = true;
            soltarItem(level, bloco(), position());
        }
        super.remove(motivo);
    }

    @Override
    public void kill(ServerLevel level) {
        devolver(level);
    }
}
