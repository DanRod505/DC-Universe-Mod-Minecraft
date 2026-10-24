package com.danrod.dcuniverse.content;

import com.danrod.dcuniverse.ability.Alvos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Gel explosivo grudado na face de um bloco (habilidade explosive_gel). Fica brilhando até o dono detonar: aí bipa,
 * pisca rápido e explode, abrindo um buraco redondo de raio "raio" no bloco onde está (mineração controlada: todos os
 * blocos caem como item; nunca obsidiana nem bedrock, nem o que o jogador não poderia quebrar ali) e ferindo e
 * empurrando quem estiver perto. Se o bloco onde está some, o gel cai e se desfaz. Não é salvo no mundo.
 */
public class GelEntidade extends Entity {
    private static final EntityDataAccessor<Integer> FACE = SynchedEntityData.defineId(GelEntidade.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ARMADO = SynchedEntityData.defineId(GelEntidade.class, EntityDataSerializers.INT);
    /** Ticks entre o bipe da detonação e a explosão. */
    public static final int PAVIO = 12;
    /** Tempo máximo grudado (10 minutos). */
    private static final int VIDA = 12000;

    private UUID dono;
    private BlockPos bloco = BlockPos.ZERO;
    private float raio = 2.5f, dano = 8;

    public GelEntidade(EntityType<? extends GelEntidade> tipo, Level level) {
        super(tipo, level);
        noPhysics = true;
    }

    public GelEntidade(ServerPlayer dono, BlockPos bloco, Direction face, Vec3 onde, float raio, float dano) {
        this(Entidades.GEL.get(), dono.level());
        this.dono = dono.getUUID();
        this.bloco = bloco.immutable();
        this.raio = raio;
        this.dano = dano;
        entityData.set(FACE, face.get3DDataValue());
        setPos(onde.add(Vec3.atLowerCornerOf(face.getUnitVec3i()).scale(0.02)));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(FACE, Direction.UP.get3DDataValue());
        b.define(ARMADO, -1);
    }

    public Direction face() {
        return Direction.from3DDataValue(entityData.get(FACE));
    }

    /** Ticks desde que foi detonado (-1 = esperando). */
    public int armadoHa() {
        int a = entityData.get(ARMADO);
        return a < 0 ? -1 : tickCount - a;
    }

    public boolean doDono(ServerPlayer p) {
        return p.getUUID().equals(dono);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {}

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource fonte, float quanto) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        if (level.getBlockState(bloco).isAir() || tickCount > VIDA) {
            level.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY(), getZ(), 6, 0.1, 0.1, 0.1, 0.05);
            discard();
            return;
        }
        int armado = armadoHa();
        if (armado >= 0 && armado % 4 == 0) {
            level.playSound(null, getX(), getY(), getZ(), Sons.GEL_BIPE.get(), SoundSource.PLAYERS, 0.8f, 1f + armado / (float) PAVIO * 0.6f);
        }
        if (armado >= PAVIO) explodir(level);
    }

    /** Começa a contagem (o dono detonou). */
    public void armar(int atraso) {
        if (entityData.get(ARMADO) < 0) entityData.set(ARMADO, tickCount + atraso);
    }

    private void explodir(ServerLevel level) {
        ServerPlayer quem = dono == null ? null : level.getServer().getPlayerList().getPlayer(dono);
        Vec3 c = Vec3.atCenterOf(bloco);
        Vec3 aqui = position();
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, aqui.x, aqui.y, aqui.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, aqui.x, aqui.y, aqui.z, 30, 0.6, 0.6, 0.6, 0.4);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, aqui.x, aqui.y, aqui.z, 20, 0.8, 0.8, 0.8, 0.04);
        level.playSound(null, aqui.x, aqui.y, aqui.z, Sons.GEL_DETONA.get(), SoundSource.PLAYERS, 1.6f, 1f);
        level.playSound(null, aqui.x, aqui.y, aqui.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2f, 1.15f);
        int r = (int) Math.ceil(raio);
        double r2 = raio * raio;
        for (BlockPos p : BlockPos.betweenClosed(bloco.offset(-r, -r, -r), bloco.offset(r, r, r))) {
            if (Vec3.atCenterOf(p).distanceToSqr(c) > r2) continue;
            BlockState st = level.getBlockState(p);
            if (!quebravel(st, level, p) || quem != null && !level.mayInteract(quem, p)) continue;
            level.destroyBlock(p, true, quem);
        }
        double alcance = raio + 1.5;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(aqui, aqui).inflate(alcance))) {
            double d = Math.sqrt(e.distanceToSqr(aqui));
            if (d > alcance || e == quem) continue;
            float f = (float) (1 - d / (alcance + 1));
            DamageSource fonte = damageSources().explosion(this, quem);
            if (quem != null) Alvos.ferir(quem, e, fonte, dano * f);
            else e.hurtServer(level, fonte, dano * f);
            Vec3 fora = e.position().subtract(aqui);
            fora = fora.lengthSqr() < 1e-4 ? new Vec3(0, 1, 0) : fora.normalize();
            e.push(fora.x * 1.1 * f, 0.5 * f + 0.1, fora.z * 1.1 * f);
            e.hurtMarked = true;
        }
        Alvos.cristais(quem, new AABB(aqui, aqui).inflate(alcance), damageSources().explosion(this, quem));
        discard();
    }

    /** O gel quebra tudo o que uma picareta de ferro quebraria rápido; nunca obsidiana, bedrock ou líquidos. */
    public static boolean quebravel(BlockState st, ServerLevel level, BlockPos p) {
        if (st.isAir() || !st.getFluidState().isEmpty() && st.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) return false;
        float dureza = st.getDestroySpeed(level, p);
        return dureza >= 0 && dureza < 25;
    }

    /** Detona os géis do jogador, do mais perto ao mais longe, um depois do outro. @return quantos */
    public static int detonar(ServerPlayer player) {
        List<GelEntidade> geis = level(player);
        for (int i = 0; i < geis.size(); i++) geis.get(i).armar(i * 4);
        return geis.size();
    }

    /** Os géis do jogador (até 96 blocos), do mais perto ao mais longe. */
    public static List<GelEntidade> level(ServerPlayer player) {
        return player.level().getEntitiesOfClass(GelEntidade.class, player.getBoundingBox().inflate(96), g -> g.doDono(player) && g.armadoHa() < 0)
            .stream().sorted(Comparator.comparingDouble(g -> g.distanceToSqr(player))).toList();
    }
}
