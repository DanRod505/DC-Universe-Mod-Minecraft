package com.danrod.dcuniverse.world.mundo;

import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Map;

/**
 * Mapa de biomas visto de cima (comando /dcu mapa_biomas), direto da fonte de biomas, sem gerar chunks: serve
 * para conferir as regiões do Mundo DC. Grava um PNG na pasta do servidor, com uma cruz em cada ponto marcado.
 */
public final class MapaBiomas {
    private MapaBiomas() {}

    static final Map<String, Integer> CORES = Map.ofEntries(
        Map.entry("plains", 0x8DB360), Map.entry("sunflower_plains", 0xB5DB88), Map.entry("meadow", 0x9ED67A),
        Map.entry("forest", 0x056621), Map.entry("flower_forest", 0x2D8E49), Map.entry("birch_forest", 0x307444),
        Map.entry("old_growth_birch_forest", 0x589C6C), Map.entry("dark_forest", 0x40511A), Map.entry("pale_garden", 0x8A8F86),
        Map.entry("savanna", 0xBDB25F), Map.entry("savanna_plateau", 0xA79D64), Map.entry("windswept_savanna", 0xE5DA87),
        Map.entry("desert", 0xFA9418), Map.entry("badlands", 0xD94515), Map.entry("eroded_badlands", 0xFF6D3D), Map.entry("wooded_badlands", 0xB09765),
        Map.entry("jungle", 0x537B09), Map.entry("sparse_jungle", 0x628B17), Map.entry("bamboo_jungle", 0x768E14),
        Map.entry("swamp", 0x07F9B2), Map.entry("mangrove_swamp", 0x2CCC8E), Map.entry("river", 0x0000FF), Map.entry("frozen_river", 0xA0A0FF),
        Map.entry("beach", 0xFADE55), Map.entry("snowy_beach", 0xFAF0C0), Map.entry("stony_shore", 0xA2A284),
        Map.entry("ocean", 0x000070), Map.entry("deep_ocean", 0x000030), Map.entry("lukewarm_ocean", 0x000090), Map.entry("deep_lukewarm_ocean", 0x000040),
        Map.entry("warm_ocean", 0x0000AC), Map.entry("cold_ocean", 0x202070), Map.entry("deep_cold_ocean", 0x202038),
        Map.entry("frozen_ocean", 0x7070D6), Map.entry("deep_frozen_ocean", 0x404090),
        Map.entry("taiga", 0x0B6659), Map.entry("snowy_taiga", 0x31554A), Map.entry("old_growth_pine_taiga", 0x596651), Map.entry("old_growth_spruce_taiga", 0x818E79),
        Map.entry("snowy_plains", 0xFFFFFF), Map.entry("ice_spikes", 0xB4DCDC), Map.entry("snowy_slopes", 0xC4C4C4), Map.entry("grove", 0x47875A),
        Map.entry("frozen_peaks", 0xA0A0A0), Map.entry("jagged_peaks", 0xDCDCC8), Map.entry("stony_peaks", 0x888888),
        Map.entry("windswept_hills", 0x606060), Map.entry("windswept_gravelly_hills", 0x789878), Map.entry("windswept_forest", 0x507050),
        Map.entry("cherry_grove", 0xFFB7C5), Map.entry("mushroom_fields", 0xFF00FF));

    /** Biomas sem cor própria no último mapa (para a legenda do comando). */
    private static final java.util.Set<String> CAVERNAS = java.util.Set.of("lush_caves", "dripstone_caves", "deep_dark", "sulfur_caves");
    public static final java.util.Set<String> SEM_COR = new java.util.TreeSet<>();

    /** Mapa de {@code (2*raio)} blocos de lado em volta de (cx, cz), um pixel a cada {@code passo} blocos. */
    public static Path gerar(ServerLevel level, int cx, int cz, int raio, int passo, boolean relevo) throws java.io.IOException {
        var fonte = level.getChunkSource().getGenerator().getBiomeSource();
        var sampler = level.getChunkSource().randomState().sampler();
        int n = 2 * raio / passo;
        BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int x = cx - raio + i * passo, z = cz - raio + j * passo;
                var b = superficie(fonte, sampler, x, z, level.getSeaLevel(), level.getMaxY());
                String id = b.unwrapKey().map(k -> k.identifier().getPath()).orElse("?");
                if (!CORES.containsKey(id)) SEM_COR.add(id);
                int cor = CORES.getOrDefault(id, id.hashCode() & 0xFFFFFF);
                if (relevo) {
                    // Relevo: o chão sai mais claro quanto mais alto; água fica com a cor do bioma.
                    var gen = level.getChunkSource().getGenerator();
                    int h = gen.getBaseHeight(x, z, net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, level, level.getChunkSource().randomState());
                    if (h >= level.getSeaLevel()) {
                        float f = Math.min(1f, Math.max(0f, (h - level.getSeaLevel()) / 120f));
                        int r = (cor >> 16) & 255, g = (cor >> 8) & 255, bb = cor & 255;
                        cor = ((int) (r + (255 - r) * f * 0.7f) << 16) | ((int) (g + (255 - g) * f * 0.7f) << 8) | (int) (bb + (255 - bb) * f * 0.7f);
                    } else cor = 0x1030A0 - Math.min(0x30, (level.getSeaLevel() - h)) ;
                }
                img.setRGB(i, j, cor);
            }
        }
        if (fonte instanceof FonteMundoDC f) {
            for (var r : f.regioes()) {
                int px = (r.x() - cx + raio) / passo, pz = (r.z() - cz + raio) / passo;
                for (int d = -8; d <= 8; d++)
                    for (int e = -1; e <= 1; e++) {
                        pinta(img, px + d, pz + e, 0xFF0000);
                        pinta(img, px + e, pz + d, 0xFF0000);
                    }
            }
        }
        Path saida = level.getServer().getServerDirectory().resolve(relevo ? "mapa_relevo.png" : "mapa_biomas.png");
        javax.imageio.ImageIO.write(img, "png", saida.toFile());
        return saida;
    }

    /** Terra firme em volta de (x, z) até {@code r} blocos, de 32 em 32: "% de terra, altura média" (para escolher posições). */
    public static String terreno(ServerLevel level, int x, int z, int r) {
        var gen = level.getChunkSource().getGenerator();
        int terra = 0, n = 0;
        long soma = 0;
        for (int dx = -r; dx <= r; dx += 32)
            for (int dz = -r; dz <= r; dz += 32) {
                if (dx * dx + dz * dz > r * r) continue;
                n++;
                int h = gen.getBaseHeight(x + dx, z + dz, net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, level, level.getChunkSource().randomState());
                if (h > level.getSeaLevel()) {
                    terra++;
                    soma += h;
                }
            }
        return x + "," + z + ": terra " + (100 * terra / Math.max(1, n)) + "%, altura " + (terra == 0 ? 0 : soma / terra);
    }

    /** Bioma da superfície em (x, z): no nível do mar, ou mais alto quando ali já é caverna (debaixo de um morro). */
    public static net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> superficie(net.minecraft.world.level.biome.BiomeSource fonte,
            net.minecraft.world.level.biome.Climate.Sampler sampler, int x, int z, int mar, int maxY) {
        var b = fonte.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(mar), QuartPos.fromBlock(z), sampler);
        for (int y = mar + 48; y < maxY && CAVERNAS.contains(b.unwrapKey().map(k -> k.identifier().getPath()).orElse("")); y += 48)
            b = fonte.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), sampler);
        return b;
    }

    /** Cor do bioma no mapa (biomas de outros mods ganham uma cor fixa tirada do nome). */
    public static int cor(net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> b) {
        String id = b.unwrapKey().map(k -> k.identifier().getPath()).orElse("?");
        return CORES.getOrDefault(id, id.hashCode() & 0xFFFFFF);
    }

    private static void pinta(BufferedImage img, int x, int y, int cor) {
        if (x >= 0 && y >= 0 && x < img.getWidth() && y < img.getHeight()) img.setRGB(x, y, cor);
    }
}
