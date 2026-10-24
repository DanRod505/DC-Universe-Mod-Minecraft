package com.danrod.dcuniverse.npc;

import com.danrod.dcuniverse.DCUniverse;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Streaky, o Supergato (docs/MUNDO.md, mobs): gato laranja com o raio amarelo no flanco. Doma com peixe como
 * qualquer gato (um por jogador), voa por instantes para alcançar o dono e os creepers fogem dele de longe.
 */
public class Streaky extends Cat implements Residentes.Residente {
    public static final Identifier TEXTURA = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "textures/entity/streaky.png");
    /** Creepers fogem dele a partir dessa distância (os gatos comuns assustam a 6). */
    public static final float SUSTO = 16f;
    /** Duração do voo curto e o descanso entre dois voos, em ticks. */
    private static final int VOO = 50, DESCANSO = 160;
    private static final EntityDataAccessor<Boolean> VOANDO = SynchedEntityData.defineId(Streaky.class, EntityDataSerializers.BOOLEAN);

    private String morador = "";
    private int voo, descanso;

    public Streaky(EntityType<? extends Streaky> tipo, Level level) {
        super(tipo, level);
    }

    public static AttributeSupplier.Builder atributos() {
        return Cat.createAttributes().add(Attributes.MAX_HEALTH, 16).add(Attributes.ATTACK_DAMAGE, 4);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(VOANDO, false);
    }

    public boolean voando() {
        return entityData.get(VOANDO);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isTame() && player.getItemInHand(hand).is(ItemTags.CAT_FOOD) && player instanceof ServerPlayer sp && !Mascotes.podeDomar(sp, "streaky")) {
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void tame(Player player) {
        super.tame(player);
        setComponent(net.minecraft.core.component.DataComponents.CAT_COLLAR, DyeColor.YELLOW);
        if (player instanceof ServerPlayer sp) Mascotes.domou(this, sp, "streaky");
    }

    @Override
    public boolean canMate(Animal outro) {
        return false;
    }

    @Override
    public Cat getBreedOffspring(ServerLevel level, AgeableMob outro) {
        return null;
    }

    @Override
    public boolean causeFallDamage(double distancia, float mult, DamageSource fonte) {
        return false;
    }

    @Override
    public void die(DamageSource fonte) {
        super.die(fonte);
        Mascotes.morreu(this, "streaky");
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) {
            if (voando() && random.nextInt(2) == 0) {
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, getRandomX(0.4), getY() + 0.2, getRandomZ(0.4), 0, -0.02, 0);
            }
            return;
        }
        if (descanso > 0) descanso--;
        LivingEntity dono = getOwner();
        if (voo > 0) {
            voo--;
            if (dono == null || dono.level() != level() || voo == 0) {
                pousar();
                return;
            }
            Vec3 falta = dono.position().add(0, 1.2, 0).subtract(position());
            double dist = falta.length();
            if (dist < 1.5) {
                pousar();
                return;
            }
            setDeltaMovement(getDeltaMovement().lerp(falta.scale(Math.min(0.9, dist * 0.2) / dist), 0.35));
            Vec3 mov = getDeltaMovement();
            float giro = (float) (Math.atan2(mov.z, mov.x) * 180 / Math.PI) - 90f;
            setYRot(giro);
            yBodyRot = giro;
            resetFallDistance();
            return;
        }
        // Voo curto: o dono subiu (ou se afastou) demais para ir andando.
        if (isTame() && !isOrderedToSit() && descanso == 0 && dono != null && dono.level() == level()) {
            double alto = dono.getY() - getY(), longe = dono.distanceToSqr(this);
            if (alto > 3.5 && longe < 24 * 24 || longe > 14 * 14 && longe < 40 * 40) {
                voo = VOO;
                descanso = DESCANSO;
                entityData.set(VOANDO, true);
                setNoGravity(true);
                getNavigation().stop();
                setDeltaMovement(getDeltaMovement().add(0, 0.5, 0));
                playAmbientSound();
            }
        }
    }

    private void pousar() {
        voo = 0;
        setNoGravity(false);
        entityData.set(VOANDO, false);
    }

    /** Creepers ganham o medo do Streaky a 16 blocos (chamado quando um creeper entra no mundo). */
    public static void assustar(Creeper c) {
        c.goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.AvoidEntityGoal<>(c, Streaky.class, SUSTO, 1.0, 1.25));
    }

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
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        morador = in.getStringOr("morador", "");
    }
}
