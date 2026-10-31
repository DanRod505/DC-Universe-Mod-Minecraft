package com.danrod.dcuniverse.content;

import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.guia.Guia;
import net.minecraft.core.HolderSet;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Espada de Themyscira (a espada da Mulher-Maravilha, ArmaVinculada): soma {@link #DANO} ao soco da heroína, bate
 * a cada 0,6 s, alcança meio bloco mais longe, ignora 30% da armadura do alvo e corta madeira e folhas como um
 * machado. A cada terceiro golpe seguido (sem passar {@link #COMBO_TICKS} entre eles) a Diana gira com a espada
 * e atinge todos em volta.
 */
public class EspadaThemyscira extends ArmaVinculada {
    public static final float DANO = 4, ALCANCE_EXTRA = 0.5f, IGNORA_ARMADURA = 0.3f, GIRO_RAIO = 3.5f, GIRO_MULT = 0.7f;
    public static final int COMBO_TICKS = 24;
    private static final Identifier ALCANCE_ID = Identifier.fromNamespaceAndPath("dcuniverse", "espada_alcance");
    /** Golpes seguidos e o tick do último, por jogador. */
    private static final Map<UUID, long[]> COMBO = new ConcurrentHashMap<>();

    public EspadaThemyscira(Properties p) {
        super(propriedades(p));
    }

    private static Properties propriedades(Properties p) {
        var blocos = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
        Tool ferramenta = new Tool(List.of(
            Tool.Rule.minesAndDrops(HolderSet.direct(Blocks.COBWEB.builtInRegistryHolder()), 15f),
            Tool.Rule.minesAndDrops(blocos.getOrThrow(BlockTags.MINEABLE_WITH_AXE), 10f),
            Tool.Rule.overrideSpeed(blocos.getOrThrow(BlockTags.LEAVES), 15f),
            Tool.Rule.overrideSpeed(blocos.getOrThrow(BlockTags.SWORD_INSTANTLY_MINES), Float.MAX_VALUE)), 1f, 0, false);
        ItemAttributeModifiers atributos = ItemAttributeModifiers.builder()
            .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, DANO, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND)
            .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.35, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND)
            .add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(ALCANCE_ID, ALCANCE_EXTRA, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND)
            .build();
        return p.stacksTo(1).rarity(Rarity.EPIC).fireResistant()
            .component(DataComponents.TOOL, ferramenta)
            .component(DataComponents.WEAPON, new Weapon(0))
            .component(DataComponents.UNBREAKABLE, net.minecraft.util.Unit.INSTANCE)
            .attributes(atributos);
    }

    /** Ignora 30% da armadura: soma a diferença entre o dano com a armadura toda e com 70% dela. */
    @Override
    public float getAttackDamageBonus(Entity alvo, float dano, DamageSource fonte) {
        if (!(alvo instanceof LivingEntity vivo) || vivo.getArmorValue() <= 0) return 0;
        float armadura = vivo.getArmorValue(), dureza = (float) vivo.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float cheio = CombatRules.getDamageAfterAbsorb(vivo, dano, fonte, armadura, dureza);
        float menos = CombatRules.getDamageAfterAbsorb(vivo, dano, fonte, armadura * (1 - IGNORA_ARMADURA), dureza);
        return cheio <= 0.01f ? 0 : dano * (menos / cheio - 1);
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity alvo, LivingEntity atacante) {
        if (!(atacante instanceof ServerPlayer sp) || !(sp.level() instanceof ServerLevel level)) return;
        long agora = level.getGameTime();
        long[] c = COMBO.computeIfAbsent(sp.getUUID(), k -> new long[2]);
        c[0] = agora - c[1] <= COMBO_TICKS ? c[0] + 1 : 1;
        c[1] = agora;
        Vec3 corte = alvo.position().add(0, alvo.getBbHeight() * 0.6, 0);
        if (c[0] >= 3) {
            c[0] = 0;
            giro(level, sp, alvo);
        } else {
            level.playSound(null, corte.x, corte.y, corte.z, Sons.ESPADA_GOLPE.get(), SoundSource.PLAYERS, 1f, 0.9f + 0.15f * c[0]);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, corte.x, corte.y, corte.z, 1, 0, 0, 0, 0);
        }
        Guia.concluir(sp, "espada");
    }

    /** Terceiro golpe: a Diana gira e a lâmina pega todos em volta (menos o alvo, que já levou o golpe). */
    private static void giro(ServerLevel level, ServerPlayer sp, LivingEntity principal) {
        float dano = (float) sp.getAttributeValue(Attributes.ATTACK_DAMAGE) * GIRO_MULT;
        Vec3 c = sp.position();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, sp.getBoundingBox().inflate(GIRO_RAIO, 0.8, GIRO_RAIO),
            e -> e != principal && Alvos.podeAtingir(sp, e))) {
            if (e.distanceToSqr(c) > GIRO_RAIO * GIRO_RAIO * 1.3) continue;
            Alvos.ferir(sp, e, level.damageSources().playerAttack(sp), dano);
            Vec3 fora = e.position().subtract(c).multiply(1, 0, 1);
            fora = fora.lengthSqr() < 1e-4 ? sp.getLookAngle() : fora.normalize();
            e.setDeltaMovement(e.getDeltaMovement().add(fora.x * 0.9, 0.3, fora.z * 0.9));
            e.hurtMarked = true;
        }
        level.playSound(null, c.x, c.y, c.z, Sons.ESPADA_GIRO.get(), SoundSource.PLAYERS, 1.3f, 1f);
        level.sendParticles(new DustParticleOptions(0xFFE7A0, 1.2f), c.x, c.y + 1, c.z, 24, GIRO_RAIO * 0.5, 0.2, GIRO_RAIO * 0.5, 0);
        Rede.efeito(level, c.x, c.y + 1.05, c.z, "giro_espada", GIRO_RAIO, sp.getYRot(), 0xFFE7A0);
        sp.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        Guia.concluir(sp, "espada_giro");
    }

    public static void limpar() {
        COMBO.clear();
    }
}
