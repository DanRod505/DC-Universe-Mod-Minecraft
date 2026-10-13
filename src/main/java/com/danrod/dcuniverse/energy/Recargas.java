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
        // Bônus sob o sol: céu visível e dia claro.
        "sunlight", (player, p) -> {
            BlockPos olhos = BlockPos.containing(player.getEyePosition());
            return player.level().isBrightOutside() && player.level().canSeeSky(olhos) ? p.f("per_second", 0f) : 0f;
        },
        // Bônus no escuro (personagens sombrios; Batman à noite).
        "darkness", (player, p) -> player.level().getMaxLocalRawBrightness(BlockPos.containing(player.getEyePosition())) <= p.i("max_light", 4)
            ? p.f("per_second", 0f) : 0f,
        // Kryptonita por perto ou no inventário (per_second negativo): drena e, ao zerar, anula os poderes.
        "kryptonite", (player, p) -> Kryptonita.exposto(player, p.i("ore_radius", 6), p.i("block_radius", 10))
            ? p.f("per_second", 0f) : 0f
    );

    private Recargas() {}
}
