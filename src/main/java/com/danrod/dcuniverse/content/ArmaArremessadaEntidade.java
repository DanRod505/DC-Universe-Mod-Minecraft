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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Arma arremessável que volta (tipo thrown_weapon): qualquer item do jogo pode voar girando e voltar para a mão
 * (a tiara da Mulher-Maravilha; depois o escudo amazônico, o machado do Lobo da Estepe...). Vai reto para a mira;
 * ao acertar um mob, ricocheteia para o próximo à vista (até "ricochet" vezes, como o escudo do Capitão); numa
 * parede, quica no ângulo certo e, se houver um alvo à vista, vai nele (também gasta um ricochete). Sem ricochete,
 * volta acelerando, atravessando blocos e acertando quem estiver no caminho com metade do dano, e é pega pelo
 * dono. No caminho corta teias, folhas, cipós, linhas e guias; acerta botões e alavancas à distância.
 * Quando é um item de verdade (devolver), volta para o mesmo espaço do inventário, ou cai no chão se o dono sumir.
 * A física roda no servidor; o cliente desenha o item girando com um rastro de luz (client/render/ArmaRenderer).
 */
public class ArmaArremessadaEntidade extends Projectile implements ItemSupplier {
    /** Como gira: deitada (disco: tiara, escudo) ou de pé, de ponta a ponta (roda: machado, martelo). */
    public static final int DISCO = 0, RODA = 1;
    private static final EntityDataAccessor<ItemStack> ITEM = SynchedEntityData.defineId(ArmaArremessadaEntidade.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> GIRO = SynchedEntityData.defineId(ArmaArremessadaEntidade.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COR = SynchedEntityData.defineId(ArmaArremessadaEntidade.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> VOLTANDO = SynchedEntityData.defineId(ArmaArremessadaEntidade.class, EntityDataSerializers.BOOLEAN);

    /** Parâmetros do arremesso (dano em pontos de vida, alcance em blocos, vel em blocos/tick, atordoa em s). */
    public record Config(float dano, float alcance, float vel, int ricochetes, float atordoa, boolean guiado, int giro, int cor) {}

    private final InterpolationHandler interpolacao = new InterpolationHandler(this, 1);
    private final Set<Integer> atingidos = new HashSet<>();
    private Config cfg = new Config(4, 20, 1.8f, 0, 0, false, DISCO, 0xFFD86A);
    private int ricochetes, ida = 12, voltandoHa;
    private Entity proximo;
    private ItemStack devolver = ItemStack.EMPTY;
    private int slot = -1;

    /** Posições recentes (só no cliente), para o rastro de luz. */
    public final Vec3[] rastro = new Vec3[10];
    public int rastroN;

    public ArmaArremessadaEntidade(EntityType<? extends ArmaArremessadaEntidade> tipo, Level level) {
        super(tipo, level);
        noPhysics = true;
    }

    public ArmaArremessadaEntidade(LivingEntity dono, ItemStack visual, Config cfg) {
        this(Entidades.ARMA_ARREMESSADA.get(), dono.level());
        setOwner(dono);
        this.cfg = cfg;
        this.ricochetes = cfg.ricochetes();
        this.ida = Math.max(4, Math.round(cfg.alcance() / cfg.vel()));
        entityData.set(ITEM, visual.copyWithCount(1));
        entityData.set(GIRO, cfg.giro());
        entityData.set(COR, cfg.cor());
        Vec3 olhar = dono.getLookAngle();
        setPos(dono.getEyePosition().add(olhar.scale(0.6)).add(0, -0.2, 0));
        setDeltaMovement(olhar.scale(cfg.vel()));
    }

    /** Lança e toca o som. */
    public static ArmaArremessadaEntidade lancar(LivingEntity dono, ItemStack visual, Config cfg) {
        ArmaArremessadaEntidade a = new ArmaArremessadaEntidade(dono, visual, cfg);
        dono.level().addFreshEntity(a);
        dono.level().playSound(null, dono.getX(), dono.getEyeY(), dono.getZ(), Sons.ARMA_ARREMESSO.get(), SoundSource.PLAYERS,
            1f, 0.95f + dono.getRandom().nextFloat() * 0.1f);
        dono.swing(InteractionHand.MAIN_HAND, true);
        return a;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(ITEM, ItemStack.EMPTY);
        b.define(GIRO, DISCO);
        b.define(COR, 0xFFD86A);
        b.define(VOLTANDO, false);
    }

    /** A arma é um item de verdade: volta para o espaço "slot" do inventário (ou para onde couber). */
    public void devolver(ItemStack item, int slot) {
        this.devolver = item;
        this.slot = slot;
    }

    @Override
    public ItemStack getItem() {
        return entityData.get(ITEM);
    }

    public int giro() {
        return entityData.get(GIRO);
    }

    public int cor() {
        return entityData.get(COR);
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

    /** Some sem voltar: o item (se houver) cai onde a arma está. */
    private void cair() {
        if (!devolver.isEmpty() && level() instanceof ServerLevel l) spawnAtLocation(l, devolver);
        devolver = ItemStack.EMPTY;
        discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            interpolacao.interpolate();
            System.arraycopy(rastro, 0, rastro, 1, rastro.length - 1);
            rastro[0] = position();
            rastroN = Math.min(rastro.length, rastroN + 1);
            return;
        }
        Entity dono = getOwner();
        if (dono == null || !dono.isAlive() || dono.level() != level()) {
            cair();
            return;
        }
        if (tickCount % 5 == 1) level().playSound(null, getX(), getY(), getZ(), Sons.ARMA_GIRO.get(), SoundSource.PLAYERS, 0.5f, 0.9f + random.nextFloat() * 0.2f);
        if (voltando()) {
            voltar(dono);
            return;
        }
        if (tickCount > ida) {
            comecarVolta();
            return;
        }
        Vec3 v = getDeltaMovement();
        if (proximo != null && proximo.isAlive()) {
            v = proximo.getBoundingBox().getCenter().subtract(position()).normalize().scale(cfg.vel());
        } else if (cfg.guiado() && dono instanceof LivingEntity vivo) {
            double d = dono.getEyePosition().distanceTo(position());
            Vec3 mira = dono.getEyePosition().add(vivo.getLookAngle().scale(d + 3));
            v = v.lerp(mira.subtract(position()).normalize().scale(cfg.vel()), 0.22).normalize().scale(cfg.vel());
        }
        setDeltaMovement(v);
        mover(v);
    }

    private void mover(Vec3 v) {
        Vec3 de = position(), ate = de.add(v);
        BlockHitResult bloco = level().clip(new ClipContext(de, ate, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        BatarangueEntidade.cortar((ServerLevel) level(), this, getOwner(), de, ate);
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
            bateuBloco(bloco, v);
            return;
        }
        setPos(ate);
    }

    private boolean podeAtingir(Entity e) {
        if (!Alvos.atingivel(e) || !e.isAlive() || atingidos.contains(e.getId())) return false;
        Entity dono = getOwner();
        if (e == dono) return false;
        return !(dono instanceof ServerPlayer sp) || Alvos.podeAtingir(sp, e);
    }

    private void ferir(Entity alvo, float quanto) {
        Entity dono = getOwner();
        if (dono instanceof ServerPlayer sp) Alvos.ferir(sp, alvo, damageSources().thrown(this, sp), quanto);
        else if (alvo instanceof LivingEntity vivo && level() instanceof ServerLevel l) vivo.hurtServer(l, damageSources().thrown(this, dono), quanto);
        if (alvo instanceof LivingEntity vivo && cfg.atordoa() > 0) {
            vivo.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, Math.round(cfg.atordoa() * 20), 3), dono);
        }
    }

    private void acertou(Entity alvo) {
        atingidos.add(alvo.getId());
        ServerLevel level = (ServerLevel) level();
        ferir(alvo, cfg.dano());
        level.playSound(null, getX(), getY(), getZ(), Sons.ARMA_ACERTA.get(), SoundSource.PLAYERS, 1f, 0.95f + random.nextFloat() * 0.1f);
        level.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 10, 0.2, 0.2, 0.2, 0.35);
        com.danrod.dcuniverse.core.Rede.efeito(level, getX(), getY(), getZ(), "faisca", 0.6f, 0, cor());
        if (ricochetes > 0) {
            Entity novo = proximoAlvo(position(), alvo);
            if (novo != null) {
                ricochetes--;
                proximo = novo;
                ida = tickCount + Math.max(6, Math.round(cfg.alcance() / cfg.vel()));
                return;
            }
        }
        comecarVolta();
    }

    /** Mob mais perto (até 10 blocos) que ainda não foi atingido e que dá para ver daqui. */
    private Entity proximoAlvo(Vec3 de, Entity menos) {
        Entity melhor = null;
        double melhorD = 100;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(de, de).inflate(10), this::podeAtingir)) {
            if (e == menos || !(e instanceof Mob) && !(e instanceof Player)) continue;
            double d = e.distanceToSqr(de);
            if (d < melhorD && visivel(de, e)) {
                melhorD = d;
                melhor = e;
            }
        }
        return melhor;
    }

    private boolean visivel(Vec3 de, Entity e) {
        return level().clip(new ClipContext(de, e.getBoundingBox().getCenter(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
            .getType() == HitResult.Type.MISS;
    }

    private void bateuBloco(BlockHitResult hit, Vec3 v) {
        ServerLevel level = (ServerLevel) level();
        BlockPos pos = hit.getBlockPos();
        BlockState st = level.getBlockState(pos);
        Player dono = getOwner() instanceof Player p ? p : null;
        if (st.getBlock() instanceof ButtonBlock botao && !st.getValue(ButtonBlock.POWERED)) {
            botao.press(st, level, pos, dono);
        } else if (st.getBlock() instanceof LeverBlock alavanca) {
            alavanca.pull(st, level, pos, dono);
        }
        level.playSound(null, getX(), getY(), getZ(), Sons.RICOCHETE.get(), SoundSource.PLAYERS, 0.8f, 1.1f);
        level.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 6, 0.1, 0.1, 0.1, 0.2);
        com.danrod.dcuniverse.core.Rede.efeito(level, getX(), getY(), getZ(), "faisca", 0.4f, 0, cor());
        if (ricochetes > 0) {
            // Quica como uma bola na parede: espelha na face e, se houver alvo à vista daqui, vai nele.
            ricochetes--;
            Direction face = hit.getDirection();
            Vec3 n = Vec3.atLowerCornerOf(face.getUnitVec3i());
            Vec3 refletido = v.subtract(n.scale(2 * v.dot(n)));
            Entity novo = proximoAlvo(position(), null);
            proximo = novo;
            setDeltaMovement(novo != null ? novo.getBoundingBox().getCenter().subtract(position()).normalize().scale(cfg.vel())
                : refletido.normalize().scale(cfg.vel()));
            ida = tickCount + Math.max(6, Math.round(cfg.alcance() / cfg.vel() / 2));
            return;
        }
        comecarVolta();
    }

    private void comecarVolta() {
        entityData.set(VOLTANDO, true);
        proximo = null;
        voltandoHa = 0;
    }

    /** Volta para o dono acelerando, atravessando blocos, e é pega ao chegar. */
    private void voltar(Entity dono) {
        voltandoHa++;
        Vec3 alvo = dono.getEyePosition().add(0, -0.4, 0);
        Vec3 d = alvo.subtract(position());
        double dist = d.length();
        if (dist < 1.3 || voltandoHa > 200) {
            pegar(dono);
            return;
        }
        double v = Math.min(dist, Math.min(2.6, cfg.vel() * (0.6 + voltandoHa * 0.09)));
        Vec3 mov = d.normalize().scale(v);
        EntityHitResult ent = ProjectileUtil.getEntityHitResult(level(), this, position(), position().add(mov),
            getBoundingBox().expandTowards(mov).inflate(0.5), this::podeAtingir);
        if (ent != null) {
            atingidos.add(ent.getEntity().getId());
            ferir(ent.getEntity(), cfg.dano() * 0.5f);
            level().playSound(null, getX(), getY(), getZ(), Sons.ARMA_ACERTA.get(), SoundSource.PLAYERS, 0.7f, 1.2f);
        }
        BatarangueEntidade.cortar((ServerLevel) level(), this, dono, position(), position().add(mov));
        setDeltaMovement(mov);
        setPos(position().add(mov));
    }

    private void pegar(Entity dono) {
        level().playSound(null, dono.getX(), dono.getEyeY(), dono.getZ(), Sons.ARMA_PEGA.get(), SoundSource.PLAYERS, 0.9f, 1f);
        if (dono instanceof LivingEntity vivo && vivo instanceof ServerPlayer sp && sp.connection != null) vivo.swing(InteractionHand.MAIN_HAND, true);
        if (!devolver.isEmpty() && dono instanceof Player p) {
            var inv = p.getInventory();
            if (slot >= 0 && slot < inv.getContainerSize() && inv.getItem(slot).isEmpty()) inv.setItem(slot, devolver);
            else if (!inv.add(devolver)) p.drop(devolver, false);
        }
        devolver = ItemStack.EMPTY;
        discard();
    }
}
