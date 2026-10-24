package com.danrod.dcuniverse.content;

import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.energy.Energia;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.Level;

/**
 * Torta da Martha: enche 8 de fome com Regeneração I por 10 s e, para quem está transformado num kryptoniano
 * (personagem que recarrega com o sol), devolve 10% da energia.
 */
public final class TortaItem extends Item {
    public static final FoodProperties COMIDA = new FoodProperties.Builder().nutrition(8).saturationModifier(0.6f).build();
    public static final Consumable CONSUMO = Consumables.defaultFood()
        .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0)))
        .build();
    /** Fração da energia máxima que a torta devolve a um kryptoniano. */
    public static final float ENERGIA = 0.10f;

    public TortaItem(Properties p) {
        super(p.food(COMIDA, CONSUMO));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity quem) {
        if (quem instanceof Player p && !level.isClientSide()) recarregar(p);
        return super.finishUsingItem(stack, level, quem);
    }

    /** Devolve a energia se o personagem atual é kryptoniano; retorna se devolveu. */
    public static boolean recarregar(Player p) {
        Personagem pers = EstadoJogador.de(p).personagem();
        if (pers == null || !kryptoniano(pers)) return false;
        Energia.adicionar(p, pers, pers.energia().max() * ENERGIA);
        if (p.level() instanceof ServerLevel l) {
            // em volta das pernas: na altura dos olhos as faíscas tapam a visão em primeira pessoa
            l.sendParticles(ParticleTypes.WAX_ON, p.getX(), p.getY() + 0.35, p.getZ(), 12, 0.55, 0.2, 0.55, 0.01);
        }
        com.danrod.dcuniverse.core.Rede.sincronizar((net.minecraft.server.level.ServerPlayer) p);
        return true;
    }

    public static boolean kryptoniano(Personagem p) {
        return p.energia().recargas().stream().anyMatch(t -> t.tipo().equals("sunlight"));
    }
}
