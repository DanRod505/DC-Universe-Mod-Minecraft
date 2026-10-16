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
        "sunlight", (player, p) -> sobOSol(player) ? p.f("per_second", 0f) : 0f,
        // Kryptonita por perto ou no inventário (per_second negativo): drena até zerar e esgotar os poderes.
        "kryptonite", (player, p) -> exposto(player, p) ? p.f("per_second", 0f) : 0f
    );

    private Recargas() {}

    /** Sob o sol de verdade (dia claro, céu visível) ou perto de uma Câmara Solar. */
    public static boolean sobOSol(Player player) {
        BlockPos olhos = BlockPos.containing(player.getEyePosition());
        return player.level().isBrightOutside() && player.level().canSeeSky(olhos) || Kryptonita.pertoDaCamara(player);
    }

    public static boolean exposto(Player player, Params p) {
        return Kryptonita.exposto(player, p.i("ore_radius", 6), p.i("block_radius", 10));
    }

    /** O personagem é afetado por kryptonita e está exposto agora. */
    public static boolean naKryptonita(Player player, com.danrod.dcuniverse.character.Personagem p) {
        for (var r : p.energia().recargas()) if (r.tipo().equals("kryptonite") && exposto(player, r.params())) return true;
        return false;
    }

    /** Recarga total agora, em energia por segundo (regen da ficha + fontes ativas). */
    public static float porSegundo(Player player, com.danrod.dcuniverse.character.Personagem p) {
        float total = p.energia().regen();
        for (var r : p.energia().recargas()) total += TIPOS.get(r.tipo()).porSegundo(player, r.params());
        return total;
    }
}
