package com.danrod.dcuniverse.character;

import com.danrod.dcuniverse.DCUniverse;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Converte a ficha (docs/BALANCEAMENTO.md) em modificadores de atributo do jogador.
 * São modificadores transitórios: reaplicados no tick se faltarem (login, renascer, troca de dimensão).
 */
public final class Atributos {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "personagem");
    /** Corrida comum do jogador, em blocos por segundo. */
    public static final double CORRIDA_VANILLA = 5.612;
    private static final List<Holder<Attribute>> USADOS = List.of(Attributes.MAX_HEALTH, Attributes.ARMOR, Attributes.ATTACK_DAMAGE, Attributes.MOVEMENT_SPEED);

    private Atributos() {}

    /** @param curar enche a vida no novo máximo (só ao transformar, não ao reaplicar). */
    public static void aplicar(Player player, Personagem p, boolean curar) {
        Personagem.Ficha f = p.ficha();
        set(player, Attributes.MAX_HEALTH, f.vida() * 2 - 20, AttributeModifier.Operation.ADD_VALUE);
        set(player, Attributes.ARMOR, f.armadura(), AttributeModifier.Operation.ADD_VALUE);
        set(player, Attributes.ATTACK_DAMAGE, f.dano() * 2 - 1, AttributeModifier.Operation.ADD_VALUE);
        set(player, Attributes.MOVEMENT_SPEED, f.terra() / CORRIDA_VANILLA - 1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        if (curar) player.setHealth(player.getMaxHealth());
    }

    public static void remover(Player player) {
        for (Holder<Attribute> a : USADOS) {
            AttributeInstance inst = player.getAttribute(a);
            if (inst != null) inst.removeModifier(ID);
        }
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    public static boolean temModificador(Player player) {
        AttributeInstance inst = player.getAttribute(Attributes.MAX_HEALTH);
        return inst != null && inst.hasModifier(ID);
    }

    private static void set(Player player, Holder<Attribute> a, double valor, AttributeModifier.Operation op) {
        AttributeInstance inst = player.getAttribute(a);
        if (inst != null) inst.addOrUpdateTransientModifier(new AttributeModifier(ID, valor, op));
    }
}
