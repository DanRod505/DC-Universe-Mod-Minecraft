package com.danrod.dcuniverse.world.estrutura;

import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.ArrayList;
import java.util.List;

/**
 * Bioma certo em cada local: o platô e a metade de dentro da rampa recebem o bioma do JSON do local (campo
 * {@code "bioma"}; Smallville = planície do Kansas), do nível 32 para cima. Entra quando o chunk ganha os biomas,
 * antes do relevo e da vegetação, então grama, árvores, chuva e criaturas já saem do bioma do local. Funciona
 * porque nessa hora o chunk já sabe quais inícios de estrutura o tocam, e o traçado do local não depende do terreno:
 * o retângulo sai só da posição do início ({@link LocalEstrutura#plato}).
 */
public final class BiomaLocal {
    /** Altura a partir da qual o bioma é pintado (cavernas fundas ficam com o bioma delas). */
    private static final int DESDE_Y = 32;

    private BiomaLocal() {}

    private record Pintura(int x1, int z1, int x2, int z2, net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> bioma) {}

    /**
     * A posição fica no platô de um local do mod? Usado para não deixar árvores soltas nascerem nele (ver mixin
     * TreeFeatureMixin): na vegetação, que vem depois das estruturas, uma árvore podia parar na frente de uma porta.
     */
    public static boolean noPlato(ChunkAccess chunk, int x, int z) {
        return perto(chunk, x, z, 2, false);
    }

    /**
     * A posição fica a até {@code margem} blocos do platô (com a rampa, se {@code comRampa}) de um local do mod?
     */
    public static boolean perto(ChunkAccess chunk, int x, int z, int margem, boolean comRampa) {
        for (var e : chunk.getAllReferences().entrySet()) {
            if (!(e.getKey() instanceof LocalEstrutura le)) continue;
            Local l = Estruturas.LOCAIS.get(le.local());
            if (l == null) continue;
            for (long inicio : e.getValue()) {
                int[] r = LocalEstrutura.plato(l, ChunkPos.unpack(inicio));
                int[] rp = comRampa ? LocalEstrutura.rampa(l) : new int[2];
                int mx = margem + rp[0], mz = margem + rp[1];
                if (x >= r[0] - mx && x <= r[2] + mx && z >= r[1] - mz && z <= r[3] + mz) return true;
            }
        }
        return false;
    }

    public static BiomeResolver resolver(ChunkAccess chunk, BiomeResolver original) {
        List<Pintura> pinturas = null;
        for (var e : chunk.getAllReferences().entrySet()) {
            if (!(e.getKey() instanceof LocalEstrutura le) || le.bioma().isEmpty()) continue;
            Local l = Estruturas.LOCAIS.get(le.local());
            if (l == null) continue;
            for (long inicio : e.getValue()) {
                int[] r = LocalEstrutura.plato(l, ChunkPos.unpack(inicio));
                int[] m = LocalEstrutura.rampa(l);
                if (pinturas == null) pinturas = new ArrayList<>();
                pinturas.add(new Pintura(r[0] - m[0] / 2, r[1] - m[1] / 2, r[2] + m[0] / 2, r[3] + m[1] / 2, le.bioma().get()));
            }
        }
        if (pinturas == null) return original;
        List<Pintura> lista = pinturas;
        int qy = QuartPos.fromBlock(DESDE_Y);
        return (x, y, z, sampler) -> {
            if (y >= qy) {
                int bx = QuartPos.toBlock(x) + 2, bz = QuartPos.toBlock(z) + 2;
                for (Pintura p : lista)
                    if (bx >= p.x1 && bx <= p.x2 && bz >= p.z1 && bz <= p.z2) return p.bioma;
            }
            return original.getNoiseBiome(x, y, z, sampler);
        };
    }
}
