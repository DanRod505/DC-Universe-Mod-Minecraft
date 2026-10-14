package com.danrod.dcuniverse.energy;

import com.danrod.dcuniverse.character.Params;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

/**
 * Fontes de recarga plugáveis (seção 2.7). Cada tipo devolve quanto soma por segundo agora.
 * A regeneração base ("regen" da ficha) não é um tipo: vale sempre.
 */
public final class Recargas {
    public interface Recarga {
        float porSegundo(Player player, Params params);
    }

    public static final Map<String, Recarga> TIPOS = Map.of(
        // Bônus sob o sol: dia claro e céu visível de onde o jogador está.
        "sunlight", (player, p) -> sobOSol(player) ? p.f("per_second", 0f) : 0f
    );

    private Recargas() {}

    public static boolean sobOSol(Player player) {
        BlockPos olhos = BlockPos.containing(player.getEyePosition());
        return player.level().isBrightOutside() && player.level().canSeeSky(olhos);
    }

    /** Recarga total agora, em energia por segundo (regen da ficha + fontes ativas). */
    public static float porSegundo(Player player, com.danrod.dcuniverse.character.Personagem p) {
        float total = p.energia().regen();
        for (var r : p.energia().recargas()) total += TIPOS.get(r.tipo()).porSegundo(player, r.params());
        return total;
    }
}
