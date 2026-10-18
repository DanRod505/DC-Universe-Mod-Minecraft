package com.danrod.dcuniverse.world.estrutura;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Estrutura {@code dcuniverse:local}: um local inteiro traçado em código ({@link Local}), escolhido pelo
 * campo {@code "local"} do JSON em data/dcuniverse/worldgen/structure. Onde e quantas vezes aparece
 * fica no structure_set.
 * <p>
 * Com {@code "unico": true} e um structure_set em anéis (concentric_rings), o local sai uma vez só por
 * mundo: dos candidatos do anel, fica o de terreno mais plano e seco, longe das vilas do jogo (os outros
 * não geram nada).
 * Assim um mundo com mar de um lado ainda ganha o local do outro.
 */
public final class LocalEstrutura extends Structure {
    public static final MapCodec<LocalEstrutura> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        settingsCodec(i),
        Codec.STRING.fieldOf("local").forGetter(e -> e.local),
        Codec.BOOL.optionalFieldOf("unico", false).forGetter(e -> e.unico)
    ).apply(i, LocalEstrutura::new));

    /** Desnível máximo (em blocos) para um candidato contar como plano. */
    private static final int DESNIVEL_PLANO = 6;

    private final String local;
    private final boolean unico;
    /** Escolhido por semente do mundo (o servidor pode trocar de mundo sem reiniciar). */
    private final Map<Long, Optional<ChunkPos>> escolhido = new ConcurrentHashMap<>();

    public LocalEstrutura(StructureSettings settings, String local, boolean unico) {
        super(settings);
        this.local = local;
        this.unico = unico;
    }

    /** Nome do local (o mesmo de {@link Estruturas#LOCAIS}). */
    public String local() {
        return local;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        Local l = Estruturas.LOCAIS.get(local);
        if (l == null) return Optional.empty();
        if (unico) {
            Optional<ChunkPos> c = escolhido.computeIfAbsent(ctx.seed(), s -> escolher(ctx));
            if (c.isPresent() && !c.get().equals(ctx.chunkPos())) return Optional.empty();
        }
        int x = ctx.chunkPos().getMiddleBlockX(), z = ctx.chunkPos().getMiddleBlockZ();
        Local.Terreno t = new Local.Terreno(ctx);
        BlockPos centro = new BlockPos(x, t.fundo(x, z), z);
        long semente = ctx.seed() ^ ctx.chunkPos().pack() * 0x5DEECE66DL ^ local.hashCode();
        return Optional.of(new GenerationStub(centro, b -> l.tracar(t, centro, b, semente)));
    }

    /**
     * O candidato do anel com o terreno mais plano e seco em toda a área do local (o centro tem de estar em
     * terra e num bioma aceito). Amostra a altura numa grade de 16 em 16 blocos.
     */
    private Optional<ChunkPos> escolher(GenerationContext ctx) {
        ChunkGeneratorStructureState estado = estado(ctx);
        if (estado == null) return Optional.empty();
        List<ChunkPos> candidatos = candidatos(ctx, estado);
        if (candidatos.isEmpty()) return Optional.empty();
        Local.Terreno t = new Local.Terreno(ctx);
        int[] area = Estruturas.LOCAIS.get(local).area();
        ChunkPos melhor = null;
        int melhorNota = Integer.MAX_VALUE;
        for (int i = 0; i < candidatos.size(); i++) {
            ChunkPos c = candidatos.get(i);
            int x = c.getMiddleBlockX(), z = c.getMiddleBlockZ();
            if (t.agua(x, z)) continue;
            int y = t.fundo(x, z);
            var bioma = ctx.biomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), ctx.randomState().sampler());
            if (!biomes().contains(bioma)) continue;
            List<Integer> alturas = new java.util.ArrayList<>();
            int molhados = 0, amostras = 0;
            for (int dx = area[0]; dx <= area[2]; dx += 16) {
                for (int dz = area[1]; dz <= area[3]; dz += 16) {
                    amostras++;
                    if (t.agua(x + dx, z + dz)) molhados++;
                    else alturas.add(t.fundo(x + dx, z + dz));
                }
            }
            if (alturas.isEmpty()) continue;
            java.util.Collections.sort(alturas);
            // Desnível sem os extremos (um morro isolado na borda não estraga a cidade).
            int desnivel = alturas.get(alturas.size() * 9 / 10) - alturas.get(alturas.size() / 10);
            // Água pesa mais que desnível: rio no meio da cidade estraga mais que um morro. Os anéis de fora
            // ficam mais longe do ponto de nascimento, então perdem um pouco.
            int nota = Math.max(0, desnivel - DESNIVEL_PLANO) * 3 + molhados * 400 / amostras + (i < 8 ? 0 : 40);
            if (temVila(ctx, estado, x, z, area)) nota += 1000;
            if (nota < melhorNota) {
                melhorNota = nota;
                melhor = c;
            }
        }
        if (melhor != null) com.danrod.dcuniverse.DCUniverse.LOGGER.info("Local {} no chunk {} (nota do terreno {})", local, melhor, melhorNota);
        return Optional.ofNullable(melhor);
    }

    /** Estado de estruturas do mundo que está gerando (anéis e posições das outras estruturas). */
    private static ChunkGeneratorStructureState estado(GenerationContext ctx) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        for (var level : server.getAllLevels())
            if (level.getChunkSource().getGenerator() == ctx.chunkGenerator()) return level.getChunkSource().getGeneratorState();
        return null;
    }

    /** Posições do anel deste local. */
    private List<ChunkPos> candidatos(GenerationContext ctx, ChunkGeneratorStructureState estado) {
        var holder = ctx.registryAccess().lookupOrThrow(Registries.STRUCTURE).wrapAsHolder(this);
        for (StructurePlacement p : estado.getPlacementsForStructure(holder)) {
            if (p instanceof ConcentricRingsStructurePlacement anel) {
                List<ChunkPos> l = estado.getRingPositionsFor(anel);
                if (l != null) return l;
            }
        }
        return List.of();
    }

    /** Alguma vila do jogo pode nascer na área (com folga de 4 chunks)? Vila colada na cidade fica feio. */
    private static boolean temVila(GenerationContext ctx, ChunkGeneratorStructureState estado, int x, int z, int[] area) {
        var sets = ctx.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        var vilas = sets.get(BuiltinStructureSets.VILLAGES);
        if (vilas.isEmpty()) return false;
        StructurePlacement p = vilas.get().value().placement();
        for (int cx = ((x + area[0]) >> 4) - 4; cx <= ((x + area[2]) >> 4) + 4; cx++)
            for (int cz = ((z + area[1]) >> 4) - 4; cz <= ((z + area[3]) >> 4) + 4; cz++)
                if (p.isStructureChunk(estado, cx, cz)) return true;
        return false;
    }

    @Override
    public StructureType<?> type() {
        return Estruturas.LOCAL.get();
    }
}
