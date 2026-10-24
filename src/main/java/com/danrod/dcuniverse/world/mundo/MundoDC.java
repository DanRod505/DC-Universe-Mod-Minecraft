package com.danrod.dcuniverse.world.mundo;

import com.danrod.dcuniverse.DCUniverse;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Tipo de mundo Mundo DC (v0.4, decisão 11): o padrão ao criar um mundo, com a semente oficial já preenchida.
 * O preset fica em data/dcuniverse/worldgen/world_preset/mundo_dc.json (posições e regiões de cada local) e a
 * fonte de biomas em {@link FonteMundoDC}.
 */
public final class MundoDC {
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> FONTES = DeferredRegister.create(Registries.BIOME_SOURCE, DCUniverse.MODID);
    public static final RegistryObject<MapCodec<FonteMundoDC>> FONTE = FONTES.register("mundo_dc", () -> FonteMundoDC.CODEC);

    public static final ResourceKey<WorldPreset> PRESET = ResourceKey.create(Registries.WORLD_PRESET, Identifier.fromNamespaceAndPath(DCUniverse.MODID, "mundo_dc"));
    /** Semente oficial: com ela o Mundo DC sai igual, bloco a bloco, para todo mundo. */
    public static final String SEMENTE = "dcuniverse";

    private MundoDC() {}

    /** Grade de chunks onde o local pode nascer no Mundo DC: até {@code busca} blocos do ponto marcado, de 64 em 64, do mais perto ao mais longe. */
    public static java.util.List<net.minecraft.world.level.ChunkPos> candidatos(FonteMundoDC.Regiao r) {
        java.util.List<net.minecraft.world.level.ChunkPos> l = new java.util.ArrayList<>();
        for (int dx = -r.busca(); dx <= r.busca(); dx += 64)
            for (int dz = -r.busca(); dz <= r.busca(); dz += 64)
                if (dx * dx + dz * dz <= r.busca() * r.busca()) l.add(new net.minecraft.world.level.ChunkPos((r.x() + dx) >> 4, (r.z() + dz) >> 4));
        l.sort(java.util.Comparator.comparingDouble(c -> Math.hypot(c.getMiddleBlockX() - r.x(), c.getMiddleBlockZ() - r.z())));
        return l;
    }
}
