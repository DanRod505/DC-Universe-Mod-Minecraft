package com.danrod.dcuniverse.content;

import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Arremessável que volta (tipo throwable): o batarangue. Vai girando na direção da mira e, guiado, segue a mira
 * do dono enquanto voa; ao acertar um mob ricocheteia para o próximo (até "ricochet" vezes) e depois volta para
 * a mão como bumerangue, acertando quem estiver no caminho. Corta teias, cordas, folhas e trepadeiras sem
 * parar e aciona botões e alavancas à distância. Variações (campo variant):
 * - explosivo: explode no primeiro impacto (fere e empurra em volta, sem quebrar blocos) e não volta;
 * - elétrico: dá um choque que paralisa e salta para os mobs próximos.
 * A física roda só no servidor; o cliente interpola a posição e desenha o giro (client/BatarangueRenderer).
 */
public class BatarangueEntidade extends Projectile {
    public static final int NORMAL = 0, EXPLOSIVO = 1, ELETRICO = 2;
    private static final EntityDataAccessor<Integer> VARIANTE = SynchedEntityData.defineId(BatarangueEntidade.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> VOLTANDO = SynchedEntityData.defineId(BatarangueEntidade.class, EntityDataSerializers.BOOLEAN);

    private final InterpolationHandler interpolacao = new InterpolationHandler(this, 1);
    private final Set<Integer> atingidos = new HashSet<>();
    private float dano = 4;
    private float atordoaSeg = 1;
    private int ricochetes;
    private int ida = 15;
    private float vel = 1.6f;
    private float raio = 3;
    private boolean guiado = true;
    private int voltandoHa;
    private Entity proximo;

    public BatarangueEntidade(EntityType<? extends BatarangueEntidade> tipo, Level level) {
        super(tipo, level);
        noPhysics = true;
    }

    public BatarangueEntidade(LivingEntity dono, int variante, float dano, float atordoaSeg, int ricochetes, float alcance, float vel,
                              float raio, boolean guiado) {
        this(Entidades.BATARANGUE.get(), dono.level());
        setOwner(dono);
        entityData.set(VARIANTE, variante);
        this.dano = dano;
        this.atordoaSeg = atordoaSeg;
        this.ricochetes = ricochetes;
        this.vel = vel;
        this.ida = Math.max(4, Math.round(alcance / vel));
        this.raio = raio;
        this.guiado = guiado;
        Vec3 olhar = dono.getLookAngle();
        setPos(dono.getEyePosition().add(olhar.scale(0.6)).add(0, -0.15, 0));
        setDeltaMovement(olhar.scale(vel));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(VARIANTE, NORMAL);
        b.define(VOLTANDO, false);
    }

    public int variante() {
        return entityData.get(VARIANTE);
    }

    public boolean voltando() {
        return entityData.get(VOLTANDO);
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
    public boolean isPickable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            interpolacao.interpolate();
            if (variante() == ELETRICO && random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 0, 0, 0);
            } else if (variante() == EXPLOSIVO && tickCount % 2 == 0) {
                level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0, 0.01, 0);
            }
            return;
        }
        Entity dono = getOwner();
        if (dono == null || !dono.isAlive() || dono.level() != level()) {
            discard();
            return;
        }
        if (tickCount % 5 == 1) level().playSound(null, getX(), getY(), getZ(), Sons.BATARANGUE_GIRO.get(), SoundSource.PLAYERS, 0.55f, 0.9f + random.nextFloat() * 0.2f);
        if (voltando()) {
            voltar(dono);
            return;
        }
        if (tickCount > ida) {
            if (variante() == EXPLOSIVO) explodir(position());
            else comecarVolta();
            return;
        }
        // Ricochete em direção ao próximo alvo; senão, guiado pela mira do dono.
        Vec3 v = getDeltaMovement();
        if (proximo != null && proximo.isAlive()) {
            v = proximo.getBoundingBox().getCenter().subtract(position()).normalize().scale(vel);
        } else if (guiado && dono instanceof LivingEntity vivo) {
            double d = dono.getEyePosition().distanceTo(position());
            Vec3 mira = dono.getEyePosition().add(vivo.getLookAngle().scale(d + 3));
            Vec3 quer = mira.subtract(position()).normalize().scale(vel);
            v = v.lerp(quer, 0.22).normalize().scale(vel);
        }
        setDeltaMovement(v);
        mover(v);
    }

    /** Anda v neste tick tratando o que for atingido no caminho. */
    private void mover(Vec3 v) {
        Vec3 de = position(), ate = de.add(v);
        BlockHitResult bloco = level().clip(new ClipContext(de, ate, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        // Teias, folhas e cordas não seguram o batarangue: são cortadas no caminho.
        cortarNoCaminho(de, ate);
        Vec3 limite = bloco.getType() == HitResult.Type.MISS ? ate : bloco.getLocation();
        EntityHitResult ent = ProjectileUtil.getEntityHitResult(level(), this, de, limite, getBoundingBox().expandTowards(v).inflate(0.6),
            this::podeAtingir);
        if (ent != null) {
            setPos(ent.getLocation());
            acertou(ent.getEntity());
            return;
        }
        if (bloco.getType() != HitResult.Type.MISS) {
            setPos(bloco.getLocation().subtract(v.normalize().scale(0.1)));
            bateuBloco(bloco);
            return;
        }
        setPos(ate);
    }

    private boolean podeAtingir(Entity e) {
        if (!(e instanceof LivingEntity) || !e.isAlive() || atingidos.contains(e.getId())) return false;
        Entity dono = getOwner();
        if (e == dono) return false;
        return !(dono instanceof ServerPlayer sp) || Alvos.podeAtingir(sp, e);
    }

    private void acertou(Entity alvo) {
        atingidos.add(alvo.getId());
        ServerLevel level = (ServerLevel) level();
        if (variante() == EXPLOSIVO) {
            explodir(position());
            return;
        }
        ferir(alvo, dano);
        level.playSound(null, getX(), getY(), getZ(), Sons.BATARANGUE_ACERTA.get(), SoundSource.PLAYERS, 0.9f, 1f);
        if (variante() == ELETRICO) choque(alvo);
        level.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 8, 0.2, 0.2, 0.2, 0.3);
        if (ricochetes > 0) {
            Entity novo = proximoAlvo(alvo);
            if (novo != null) {
                ricochetes--;
                proximo = novo;
                return;
            }
        }
        comecarVolta();
    }

    private void ferir(Entity alvo, float quanto) {
        Entity dono = getOwner();
        if (dono instanceof ServerPlayer sp) Alvos.ferir(sp, alvo, damageSources().thrown(this, sp), quanto);
        else if (alvo instanceof LivingEntity vivo && level() instanceof ServerLevel l) vivo.hurtServer(l, damageSources().thrown(this, dono), quanto);
        if (alvo instanceof LivingEntity vivo && atordoaSeg > 0) {
            vivo.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, Math.round(atordoaSeg * 20), 3), dono);
        }
    }

    /** Mob mais perto (até 8 blocos) que ainda não foi atingido e que dá para ver daqui. */
    private Entity proximoAlvo(Entity de) {
        Entity melhor = null;
        double melhorD = 64;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, de.getBoundingBox().inflate(8), this::podeAtingir)) {
            if (e == de || !(e instanceof net.minecraft.world.entity.Mob) && !(e instanceof Player)) continue;
            double d = e.distanceToSqr(this);
            if (d < melhorD && visivel(e)) {
                melhorD = d;
                melhor = e;
            }
        }
        return melhor;
    }

    private boolean visivel(Entity e) {
        return level().clip(new ClipContext(position(), e.getBoundingBox().getCenter(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
            .getType() == HitResult.Type.MISS;
    }

    /** Choque elétrico: paralisa o alvo e salta para até dois mobs a 5 blocos, com metade do dano. */
    private void choque(Entity alvo) {
        ServerLevel level = (ServerLevel) level();
        if (alvo instanceof LivingEntity vivo) vivo.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 6), getOwner());
        Vec3 de = alvo.getBoundingBox().getCenter();
        int saltos = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, alvo.getBoundingBox().inflate(5), this::podeAtingir)) {
            if (e == alvo || saltos >= 2) continue;
            atingidos.add(e.getId());
            ferir(e, dano / 2);
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 6), getOwner());
            Vec3 ate = e.getBoundingBox().getCenter();
            Rede.efeito(level, de.x, de.y, de.z, "arco", (float) (ate.x - de.x), (float) (ate.z - de.z), Float.floatToIntBits((float) (ate.y - de.y)));
            saltos++;
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, de.x, de.y, de.z, 20, 0.3, 0.4, 0.3, 0.2);
        level.playSound(null, de.x, de.y, de.z, Sons.CHOQUE.get(), SoundSource.PLAYERS, 1f, 1f);
    }

    /** Explosão do batarangue explosivo: fere e empurra em volta, sem quebrar blocos. */
    private void explodir(Vec3 onde) {
        ServerLevel level = (ServerLevel) level();
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, onde.x, onde.y, onde.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.FLAME, onde.x, onde.y, onde.z, 20, 0.3, 0.3, 0.3, 0.12);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, onde.x, onde.y, onde.z, 16, 0.5, 0.5, 0.5, 0.05);
        level.playSound(null, onde.x, onde.y, onde.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2f, 1.1f);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(onde, onde).inflate(raio), this::podeAtingirExplosao)) {
            double d = Math.sqrt(e.distanceToSqr(onde));
            if (d > raio) continue;
            float f = (float) (1 - d / (raio + 1));
            ferir(e, dano * f);
            Vec3 fora = e.position().subtract(onde);
            fora = fora.lengthSqr() < 1e-4 ? new Vec3(0, 1, 0) : fora.normalize();
            e.push(fora.x * 0.9 * f, 0.45 * f, fora.z * 0.9 * f);
            e.hurtMarked = true;
        }
        discard();
    }

    private boolean podeAtingirExplosao(Entity e) {
        Entity dono = getOwner();
        if (!(e instanceof LivingEntity) || !e.isAlive() || e == dono) return false;
        return !(dono instanceof ServerPlayer sp) || Alvos.podeAtingir(sp, e);
    }

    private void bateuBloco(BlockHitResult hit) {
        ServerLevel level = (ServerLevel) level();
        BlockPos pos = hit.getBlockPos();
        BlockState st = level.getBlockState(pos);
        Player dono = getOwner() instanceof Player p ? p : null;
        if (st.getBlock() instanceof ButtonBlock botao && !st.getValue(ButtonBlock.POWERED)) {
            botao.press(st, level, pos, dono);
        } else if (st.getBlock() instanceof LeverBlock alavanca) {
            alavanca.pull(st, level, pos, dono);
        }
        if (variante() == EXPLOSIVO) {
            explodir(position());
            return;
        }
        level.playSound(null, getX(), getY(), getZ(), Sons.RICOCHETE.get(), SoundSource.PLAYERS, 0.6f, 1.3f);
        level.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 6, 0.1, 0.1, 0.1, 0.2);
        comecarVolta();
    }

    /** Teias, folhas, trepadeiras e cordas de armadilha no caminho: corta (e deixa cair o que der). */
    private void cortarNoCaminho(Vec3 de, Vec3 ate) {
        ServerLevel level = (ServerLevel) level();
        if (getOwner() instanceof ServerPlayer sp && !level.mayInteract(sp, BlockPos.containing(ate))) return;
        int passos = (int) Math.ceil(de.distanceTo(ate) / 0.4);
        for (int i = 0; i <= passos; i++) {
            BlockPos pos = BlockPos.containing(de.lerp(ate, i / (double) Math.max(1, passos)));
            BlockState st = level.getBlockState(pos);
            if (cortavel(st)) {
                level.destroyBlock(pos, true, getOwner());
                level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 0.6f, 1.4f);
            }
        }
    }

    public static boolean cortavel(BlockState st) {
        return st.is(Blocks.COBWEB) || st.is(BlockTags.LEAVES) || st.is(Blocks.VINE) || st.is(Blocks.TRIPWIRE)
            || st.is(Blocks.TWISTING_VINES) || st.is(Blocks.WEEPING_VINES) || st.is(Blocks.GLOW_LICHEN) || st.is(Blocks.HANGING_ROOTS);
    }

    private void comecarVolta() {
        entityData.set(VOLTANDO, true);
        proximo = null;
        voltandoHa = 0;
    }

    /** Volta para o dono acelerando, atravessando blocos, e é pego ao chegar. */
    private void voltar(Entity dono) {
        voltandoHa++;
        Vec3 alvo = dono.getEyePosition().add(0, -0.4, 0);
        Vec3 d = alvo.subtract(position());
        double dist = d.length();
        if (dist < 1.3 || voltandoHa > 200) {
            level().playSound(null, dono.getX(), dono.getEyeY(), dono.getZ(), Sons.BATARANGUE_PEGA.get(), SoundSource.PLAYERS, 0.8f, 1f);
            discard();
            return;
        }
        double v = Math.min(dist, Math.min(2.4, vel * (0.6 + voltandoHa * 0.08)));
        Vec3 mov = d.normalize().scale(v);
        // Acerta quem estiver no caminho da volta (uma vez cada).
        EntityHitResult ent = ProjectileUtil.getEntityHitResult(level(), this, position(), position().add(mov),
            getBoundingBox().expandTowards(mov).inflate(0.5), this::podeAtingir);
        if (ent != null) {
            atingidos.add(ent.getEntity().getId());
            ferir(ent.getEntity(), dano * 0.5f);
            level().playSound(null, getX(), getY(), getZ(), Sons.BATARANGUE_ACERTA.get(), SoundSource.PLAYERS, 0.7f, 1.2f);
        }
        cortarNoCaminho(position(), position().add(mov));
        setDeltaMovement(mov);
        setPos(position().add(mov));
    }
}
