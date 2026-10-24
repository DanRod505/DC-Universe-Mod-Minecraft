package com.danrod.dcuniverse.npc;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.ability.passive.Voo;
import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.content.Itens;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Krypto, o Supercão (docs/MUNDO.md, mobs): cão branco de capa vermelha. Só aceita o Osso de Cristal
 * Kryptoniano (que também o cura), um por jogador. Voa junto quando o dono voa, luta e busca itens no chão.
 */
public class Krypto extends Wolf implements Residentes.Residente {
    public static final Identifier TEXTURA = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "textures/entity/krypto.png");
    private static final EntityDataAccessor<Boolean> VOANDO = SynchedEntityData.defineId(Krypto.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<ItemStack> BOCA = SynchedEntityData.defineId(Krypto.class, EntityDataSerializers.ITEM_STACK);
    /** Longe assim do dono no ar, alcança ele num salto (como o lobo que se teleporta no chão). */
    private static final double SALTO = 24;

    private String morador = "";
    private int golpe;

    public Krypto(EntityType<? extends Krypto> tipo, Level level) {
        super(tipo, level);
    }

    public static AttributeSupplier.Builder atributos() {
        return Wolf.createAttributes().add(Attributes.ATTACK_DAMAGE, 6).add(Attributes.MOVEMENT_SPEED, 0.33).add(Attributes.FLYING_SPEED, 0.8);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(VOANDO, false);
        b.define(BOCA, ItemStack.EMPTY);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(7, new Buscar());
    }

    @Override
    public Identifier getTexture() {
        return TEXTURA;
    }

    public boolean voando() {
        return entityData.get(VOANDO);
    }

    public ItemStack boca() {
        return entityData.get(BOCA);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        boolean osso = s.is(Itens.OSSO_CRISTAL.get());
        if (!isTame()) {
            if (osso) {
                if (player instanceof ServerPlayer sp && Mascotes.podeDomar(sp, "krypto")) {
                    s.consume(1, player);
                    tame(player);
                    getNavigation().stop();
                    setTarget(null);
                    setOrderedToSit(false);
                    level().broadcastEntityEvent(this, (byte) 7);
                }
                return InteractionResult.SUCCESS;
            }
            if (s.is(Items.BONE)) {
                if (player instanceof ServerPlayer sp) {
                    level().broadcastEntityEvent(this, (byte) 6);
                    if (sp.connection != null) sp.sendOverlayMessage(Component.translatable("dcuniverse.mascote.krypto.osso"));
                }
                return InteractionResult.SUCCESS;
            }
        } else if (osso && isOwnedBy(player) && getHealth() < getMaxHealth()) {
            if (!level().isClientSide()) {
                s.consume(1, player);
                setHealth(getMaxHealth());
                level().broadcastEntityEvent(this, (byte) 7);
                playAmbientSound();
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void tame(Player player) {
        super.tame(player);
        setComponent(net.minecraft.core.component.DataComponents.WOLF_COLLAR, DyeColor.RED);
        if (player instanceof ServerPlayer sp) Mascotes.domou(this, sp, "krypto");
    }

    @Override
    public boolean canMate(Animal outro) {
        return false;
    }

    @Override
    public Wolf getBreedOffspring(ServerLevel level, AgeableMob outro) {
        return null;
    }

    @Override
    public boolean causeFallDamage(double distancia, float mult, DamageSource fonte) {
        return false;
    }

    @Override
    public void die(DamageSource fonte) {
        largar();
        super.die(fonte);
        Mascotes.morreu(this, "krypto");
    }

    // ------------------------------------------------------------ voo

    private static boolean noAr(Player d) {
        if (d.getAbilities().flying || d.isFallFlying()) return true;
        return d instanceof ServerPlayer sp && (Voo.voando(sp) || EstadoJogador.de(sp).planar() != 0);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) return;
        LivingEntity dono = getOwner();
        LivingEntity alvo = getTarget();
        boolean voar = isTame() && !isOrderedToSit() && dono instanceof Player d && d.level() == level()
            && (noAr(d) || alvo != null && alvo.isAlive() && !alvo.onGround() && alvo.getY() > getY() + 2);
        if (voar) voar((Player) dono, alvo);
        else if (voando()) {
            setNoGravity(false);
            entityData.set(VOANDO, false);
        }
    }

    /** Vai junto do dono (ao lado, um pouco atrás) ou para cima do alvo, sem caminho: direto pelo ar. */
    private void voar(Player dono, LivingEntity alvo) {
        if (!voando()) {
            entityData.set(VOANDO, true);
            setNoGravity(true);
            setOrderedToSit(false);
        }
        getNavigation().stop();
        resetFallDistance();
        Vec3 olhar = dono.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 lado = new Vec3(-olhar.z, 0, olhar.x);
        Vec3 destino = alvo != null && alvo.isAlive() ? alvo.position().add(0, alvo.getBbHeight() * 0.5, 0)
            : dono.position().add(lado.scale(1.6)).subtract(olhar.scale(1.2)).add(0, 0.6, 0);
        Vec3 falta = destino.subtract(position());
        double dist = falta.length();
        if (alvo == null && dist > SALTO) {
            snapTo(destino.x, destino.y, destino.z, dono.getYRot(), 0);
            return;
        }
        double vel = Math.min(dist * 0.18, 0.6 + dono.getDeltaMovement().length() * 1.1);
        Vec3 quer = dist < 0.05 ? Vec3.ZERO : falta.scale(vel / dist);
        setDeltaMovement(getDeltaMovement().lerp(quer, 0.3));
        Vec3 mov = getDeltaMovement();
        if (mov.horizontalDistanceSqr() > 0.004) {
            float giro = (float) (Math.atan2(mov.z, mov.x) * 180 / Math.PI) - 90f;
            setYRot(giro);
            yBodyRot = giro;
            yHeadRot = giro;
        }
        if (alvo != null && dist < 2.2 && --golpe <= 0) {
            golpe = 15;
            doHurtTarget((ServerLevel) level(), alvo);
            swing(InteractionHand.MAIN_HAND);
        }
    }

    // ------------------------------------------------------------ buscar itens

    private void largar() {
        ItemStack b = boca();
        if (!b.isEmpty() && level() instanceof ServerLevel s) {
            spawnAtLocation(s, b);
            entityData.set(BOCA, ItemStack.EMPTY);
        }
    }

    /** Pega um item caído perto do dono e leva até ele. */
    private final class Buscar extends Goal {
        private ItemEntity item;
        private int tempo;

        Buscar() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private Player dono() {
            return getOwner() instanceof Player p ? p : null;
        }

        @Override
        public boolean canUse() {
            Player d = dono();
            if (d == null || isOrderedToSit() || voando() || getTarget() != null || tickCount % 10 != 0) return false;
            if (!boca().isEmpty()) return true;
            ItemEntity melhor = null;
            for (ItemEntity i : level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(10, 4, 10))) {
                if (!i.isAlive() || i.hasPickUpDelay() || i.getAge() < 40 || i.distanceToSqr(d) > 16 * 16 || i.distanceToSqr(d) < 4) continue;
                if (melhor == null || i.distanceToSqr(Krypto.this) < melhor.distanceToSqr(Krypto.this)) melhor = i;
            }
            item = melhor;
            return item != null;
        }

        @Override
        public void start() {
            tempo = 0;
        }

        @Override
        public boolean canContinueToUse() {
            return dono() != null && !isOrderedToSit() && !voando() && tempo < 400 && (!boca().isEmpty() || item != null && item.isAlive());
        }

        @Override
        public void tick() {
            tempo++;
            Player d = dono();
            if (boca().isEmpty()) {
                getLookControl().setLookAt(item, 30, 30);
                if (tempo % 10 == 1) getNavigation().moveTo(item, 1.2);
                if (distanceToSqr(item) < 2.5) {
                    entityData.set(BOCA, item.getItem().copy());
                    item.discard();
                    item = null;
                    playSound(SoundEvents.ITEM_PICKUP, 0.6f, 1f);
                }
            } else if (d != null) {
                getLookControl().setLookAt(d, 30, 30);
                if (tempo % 10 == 1) getNavigation().moveTo(d, 1.2);
                if (distanceToSqr(d) < 5) {
                    ItemStack b = boca().copy();
                    entityData.set(BOCA, ItemStack.EMPTY);
                    if (!d.getInventory().add(b)) d.drop(b, false);
                    playAmbientSound();
                }
            }
        }

        @Override
        public void stop() {
            item = null;
            getNavigation().stop();
            if (dono() == null || isOrderedToSit()) largar();
        }
    }

    // ------------------------------------------------------------ salvar

    @Override
    public String morador() {
        return morador;
    }

    @Override
    public void morador(String chave) {
        morador = chave;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putString("morador", morador);
        if (!boca().isEmpty()) out.store("boca", ItemStack.CODEC, boca());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        morador = in.getStringOr("morador", "");
        entityData.set(BOCA, in.read("boca", ItemStack.CODEC).orElse(ItemStack.EMPTY));
    }
}
