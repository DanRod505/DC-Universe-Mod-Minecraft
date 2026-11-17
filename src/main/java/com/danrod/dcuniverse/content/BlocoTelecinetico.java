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
    public static final int SEGURO = 0, VOANDO = 1, POUSANDO = 2, PAIRANDO = 3;
    /** Estilo de desenho: o bloco comum da telecinese, a rocha das habilidades de combate e o piso das plataformas. */
    public static final int COMUM = 0, ROCHA = 1, PLATAFORMA = 2;
    private static final EntityDataAccessor<BlockState> BLOCO = SynchedEntityData.defineId(BlocoTelecinetico.class, EntityDataSerializers.BLOCK_STATE);
    private static final EntityDataAccessor<Integer> FASE = SynchedEntityData.defineId(BlocoTelecinetico.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ESTILO = SynchedEntityData.defineId(BlocoTelecinetico.class, EntityDataSerializers.INT);

    private final InterpolationHandler interpolacao = new InterpolationHandler(this, 2);
    private ServerPlayer dono;
    /** Lugar na formação (em relação ao centro do grupo) e destino ao pousar. */
    private Vec3 rel = Vec3.ZERO;
    private BlockPos destino;
    private Vec3 vel = Vec3.ZERO;
    private float dano;
    /** Quantas vezes cada alvo já foi atingido por este grupo (compartilhado entre os blocos do mesmo lançamento). */
    private Map<Integer, Integer> golpes;
    /** Empurrão de quem a rocha acerta (as do combo; o comum é 0,8). */
    private double empurra = 0.8;
    private int voo;
    private boolean resolvido;
    /** Pairando: o ponto onde ele fica parado no ar (os pés da entidade = a base do bloco). */
    private Vec3 ancora;
    private float suavidade = 0.25f;
    /**
     * Efêmero: cópia do chão feita pela telecinese onde o terreno não é natural (construção, estrada, perto de
     * estrutura). Ao fim ele se desfaz em pó, sem virar bloco nem item: o mundo fica como estava.
     */
    private boolean efemero;
    /** Ao acertar alguém, o bloco se espatifa no alvo e cai ali (as rochas do combo) em vez de seguir voando. */
    private boolean estilhaca;

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

    /**
     * Bloco solto para as habilidades coreografadas: nasce em de (pés = base do bloco) e já sobe pairando até ancora.
     * efemero: veja o campo. O bloco real deve ter sido tirado do mundo antes por quem chama.
     */
    public static BlocoTelecinetico pairando(ServerLevel level, ServerPlayer dono, Vec3 de, BlockState st, Vec3 ancora,
                                           boolean efemero, int estilo) {
        BlocoTelecinetico b = new BlocoTelecinetico(Entidades.BLOCO_TK.get(), level);
        b.entityData.set(BLOCO, st);
        b.entityData.set(ESTILO, estilo);
        b.entityData.set(FASE, PAIRANDO);
        b.dono = dono;
        b.efemero = efemero;
        b.ancora = ancora;
        b.setPos(de);
        level.addFreshEntity(b);
        return b;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(BLOCO, Blocks.STONE.defaultBlockState());
        b.define(FASE, SEGURO);
        b.define(ESTILO, COMUM);
    }

    public int estilo() {
        return entityData.get(ESTILO);
    }

    public boolean efemero() {
        return efemero;
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

    public void empurrar(double forca) {
        empurra = forca;
    }

    public void lancar(Vec3 velocidade, float dano, Map<Integer, Integer> golpes) {
        vel = velocidade;
        this.dano = dano;
        this.golpes = golpes;
        voo = 0;
        entityData.set(FASE, VOANDO);
    }

    /** Pairando: muda o ponto no ar (suavidade = fração do caminho andada por tick). */
    public void pairar(Vec3 onde, float suavidade) {
        ancora = onde;
        this.suavidade = suavidade;
        entityData.set(FASE, PAIRANDO);
    }

    public void estilhacar(boolean sim) {
        estilhaca = sim;
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
            if (fase() == PAIRANDO && random.nextInt(estilo() == PLATAFORMA ? 5 : 2) == 0) {
                // Terra esfarelando por baixo do que a telecinese segura no ar (mais quando ainda está subindo).
                boolean subindo = getY() - yo > 0.02;
                for (int i = 0; i < (subindo ? 3 : 1); i++) {
                    level().addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, bloco()),
                        getX() + (random.nextDouble() - 0.5) * 0.9, getY() - 0.05, getZ() + (random.nextDouble() - 0.5) * 0.9, 0, 0, 0);
                }
                if (subindo) {
                    level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, bloco()),
                        getX() + (random.nextDouble() - 0.5) * 0.8, getY() - 0.2, getZ() + (random.nextDouble() - 0.5) * 0.8,
                        0, -0.1, 0);
                }
            }
            if (fase() == VOANDO && estilo() == ROCHA && getDeltaMovement().lengthSqr() + position().distanceToSqr(xo, yo, zo) > 0.5) {
                for (int i = 0; i < 2; i++) {
                    level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, bloco()),
                        getX() + (random.nextDouble() - 0.5) * 0.7, getY() + random.nextDouble(), getZ() + (random.nextDouble() - 0.5) * 0.7, 0, 0, 0);
                }
                if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.POOF, xo, yo + 0.5, zo, 0, 0, 0);
            }
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
            case PAIRANDO -> pairar(level);
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

    /** Vai até a âncora (suave) e fica ali balançando de leve; a plataforma fica firme. */
    private void pairar(ServerLevel level) {
        if (ancora == null || dono == null || dono.isRemoved() || tickCount > 20 * 40) {
            soltar();
            return;
        }
        double bob = estilo() == PLATAFORMA ? 0 : Math.sin((tickCount + fase) * 0.15) * 0.06;
        Vec3 alvo = ancora.add(0, bob, 0);
        Vec3 p = position();
        setPos(p.distanceToSqr(alvo) < 0.0004 ? alvo : p.lerp(alvo, suavidade));
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
                Vec3 emp = vel.normalize().scale(estilhaca ? empurra : 0.8);
                e.setDeltaMovement(e.getDeltaMovement().add(emp.x, estilhaca ? 0.45 : 0.3, emp.z));
                e.hurtMarked = true;
                vel = vel.scale(0.55);
                if (estilhaca) {
                    // Espatifa no alvo: estilhaços, um estalo seco e o que sobra do bloco cai ali.
                    Vec3 c = e.position().add(0, e.getBbHeight() * 0.6, 0);
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, bloco()), c.x, c.y, c.z, 40, 0.4, 0.4, 0.4, 0.25);
                    level.sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 14, 0.3, 0.3, 0.3, 0.6);
                    level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 1, 0, 0, 0, 0);
                    level.playSound(null, c.x, c.y, c.z, Sons.GOLPE_PESADO.get(), SoundSource.PLAYERS, 1.2f, 0.9f + random.nextFloat() * 0.2f);
                    level.playSound(null, c.x, c.y, c.z, bloco().getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.4f, 0.7f);
                    vel = new Vec3(vel.x * 0.12, 0.15, vel.z * 0.12);
                    dano = 0;
                    if (efemero) {
                        devolver(level);
                        return;
                    }
                    break;
                }
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
        if (efemero) {
            devolver(level);
            return;
        }
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
        if (efemero) {
            // A cópia se desfaz em pó: nada entra nem sai do mundo.
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, bloco()), getX(), getY() + 0.5, getZ(), 24, 0.35, 0.35, 0.35, 0.1);
            level.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.5, getZ(), 4, 0.3, 0.3, 0.3, 0.02);
            discard();
            return;
        }
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
        if (!resolvido && !efemero && level() instanceof ServerLevel level && motivo != RemovalReason.KILLED) {
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
