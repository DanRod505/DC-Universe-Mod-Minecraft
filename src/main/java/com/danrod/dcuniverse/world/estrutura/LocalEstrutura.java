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
 * <p>
 * Todo local é nivelado (v0.4): a primeira peça é um {@link PecaPlato} na altura mediana da área, o traçado
 * recebe um terreno plano ({@link Local.Terreno#plano}) e uma semente fixa, então a planta sai igual em qualquer
 * mundo, e o campo {@code "bioma"} pinta o bioma do local na área ({@link BiomaLocal}).
 */
public final class LocalEstrutura extends Structure {
    public static final MapCodec<LocalEstrutura> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        settingsCodec(i),
        Codec.STRING.fieldOf("local").forGetter(e -> e.local),
        Codec.BOOL.optionalFieldOf("unico", false).forGetter(e -> e.unico),
        net.minecraft.world.level.biome.Biome.CODEC.optionalFieldOf("bioma").forGetter(e -> e.bioma)
    ).apply(i, LocalEstrutura::new));

    /** Desnível máximo (em blocos) para um candidato contar como plano. */
    private static final int DESNIVEL_PLANO = 6;

    private final String local;
    private final boolean unico;
    /** Bioma pintado no platô e em metade da rampa (ver mixin ChunkAccessMixin); vazio = o do mundo. */
    private final Optional<net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>> bioma;
    /** Escolhido por semente do mundo (o servidor pode trocar de mundo sem reiniciar). */
    private final Map<Long, Optional<ChunkPos>> escolhido = new ConcurrentHashMap<>();

    public LocalEstrutura(StructureSettings settings, String local, boolean unico,
                          Optional<net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>> bioma) {
        super(settings);
        this.local = local;
        this.unico = unico;
        this.bioma = bioma;
    }

    /** Nome do local (o mesmo de {@link Estruturas#LOCAIS}). */
    public String local() {
        return local;
    }

    public Optional<net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>> bioma() {
        return bioma;
    }

    /**
     * Centro do traçado para um início no chunk {@code c}: a área do local fica centrada no meio do chunk, para
     * caber no alcance das estruturas (8 chunks para cada lado, ~135 blocos).
     */
    public static int[] centro(Local l, ChunkPos c) {
        int[] a = l.area();
        return new int[]{c.getMiddleBlockX() - (a[0] + a[2]) / 2, c.getMiddleBlockZ() - (a[1] + a[3]) / 2};
    }

    /** Retângulo do platô (x1, z1, x2, z2) no mundo para o início no chunk {@code c}. */
    public static int[] plato(Local l, ChunkPos c) {
        int[] a = l.area(), m = centro(l, c);
        return new int[]{m[0] + a[0], m[1] + a[1], m[0] + a[2], m[1] + a[3]};
    }

    /**
     * Largura da rampa do local nos lados leste/oeste e norte/sul: até {@link PecaPlato#RAMPA}, mas platô e rampa
     * juntos têm de caber no alcance de 8 chunks em volta do chunk do início (135 blocos do meio dele), senão a borda
     * não é gerada.
     */
    public static int[] rampa(Local l) {
        int[] a = l.area();
        return new int[]{rampa((a[2] - a[0] + 2) / 2), rampa((a[3] - a[1] + 2) / 2)};
    }

    private static int rampa(int meiaLargura) {
        return Math.max(6, Math.min(PecaPlato.RAMPA, 135 - meiaLargura));
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        Local l = Estruturas.LOCAIS.get(local);
        if (l == null) return Optional.empty();
        if (unico) {
            Optional<ChunkPos> c = escolhido.computeIfAbsent(ctx.seed(), s -> escolher(ctx));
            if (c.isPresent() && !c.get().equals(ctx.chunkPos())) return Optional.empty();
        }
        int[] m = centro(l, ctx.chunkPos()), r = plato(l, ctx.chunkPos());
        int y = nivelPlato(new Local.Terreno(ctx), r, ctx.chunkGenerator().getSeaLevel());
        BlockPos centro = new BlockPos(m[0], y + 1, m[1]);
        // A planta não depende da semente do mundo: o local sai igual em todo mundo (só a altura muda).
        long semente = local.hashCode() * 0x5DEECE66DL;
        BlockPos meio = new BlockPos(ctx.chunkPos().getMiddleBlockX(), y + 1, ctx.chunkPos().getMiddleBlockZ());
        return Optional.of(new GenerationStub(meio, b -> {
            int[] rp = rampa(l);
            b.addPiece(new PecaPlato(r[0], r[1], r[2], r[3], y, rp[0], rp[1], l.solo()));
            l.tracar(Local.Terreno.plano(y), centro, b, semente);
        }));
    }

    /**
     * Altura do platô: a mediana do chão seco numa grade de 16 em 16 blocos (metade da área é cortada, metade
     * aterrada), nunca abaixo do nível do mar (o platô fica seco mesmo à beira d'água).
     */
    static int nivelPlato(Local.Terreno t, int[] r, int mar) {
        List<Integer> alturas = new java.util.ArrayList<>();
        for (int x = r[0]; x <= r[2]; x += 16)
            for (int z = r[1]; z <= r[3]; z += 16)
                if (!t.agua(x, z)) alturas.add(t.fundo(x, z) - 1);
        if (alturas.isEmpty()) return mar + 1;
        java.util.Collections.sort(alturas);
        return Math.max(mar + 1, alturas.get(alturas.size() / 2));
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
        Local l = Estruturas.LOCAIS.get(local);
        ChunkPos melhor = null;
        float melhorNota = Float.MAX_VALUE;
        // No Mundo DC os candidatos são a grade em volta do ponto marcado (ver FonteMundoDC): o bioma já é o do
        // tema e o local tem de nascer ali de qualquer jeito, então nenhum candidato é descartado.
        var mundoDC = ctx.biomeSource() instanceof com.danrod.dcuniverse.world.mundo.FonteMundoDC f ? f.regiao(local) : Optional.<com.danrod.dcuniverse.world.mundo.FonteMundoDC.Regiao>empty();
        for (int i = 0; i < candidatos.size(); i++) {
            ChunkPos c = candidatos.get(i);
            int x = c.getMiddleBlockX(), z = c.getMiddleBlockZ();
            if (mundoDC.isEmpty()) {
                if (t.agua(x, z)) continue;
                int y = t.fundo(x, z);
                var bioma = ctx.biomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), ctx.randomState().sampler());
                if (!biomes().contains(bioma)) continue;
            }
            List<Integer> alturas = new java.util.ArrayList<>();
            int molhados = 0, amostras = 0;
            int[] r = plato(l, c);
            for (int ax = r[0]; ax <= r[2]; ax += 16) {
                for (int az = r[1]; az <= r[3]; az += 16) {
                    amostras++;
                    if (t.agua(ax, az)) molhados++;
                    else alturas.add(t.fundo(ax, az));
                }
            }
            if (alturas.isEmpty() && mundoDC.isEmpty()) continue;
            if (alturas.isEmpty()) alturas.add(0);
            java.util.Collections.sort(alturas);
            // Desnível sem os extremos (um morro isolado na borda não estraga a cidade).
            int desnivel = alturas.get(alturas.size() * 9 / 10) - alturas.get(alturas.size() / 10);
            // Água pesa mais que desnível: rio no meio da cidade estraga mais que um morro. Os anéis de fora
            // ficam mais longe do ponto de nascimento, então perdem um pouco. Vizinhança fora do tema (neve e gelo
            // em volta do Kansas) pesa quase como água.
            float nota = Math.max(0, desnivel - DESNIVEL_PLANO) * 3 + molhados * 400 / amostras;
            if (mundoDC.isPresent()) nota += (float) Math.hypot(x - mundoDC.get().x(), z - mundoDC.get().z()) / 8;
            else nota += (i < 8 ? 0 : i < 16 ? 40 : 90) + vizinhosEstranhos(ctx, t, r) * 600;
            if (temVila(ctx, estado, r)) nota += 1000;
            if (nota < melhorNota) {
                melhorNota = nota;
                melhor = c;
            }
        }
        if (melhor != null) com.danrod.dcuniverse.DCUniverse.LOGGER.info("Local {} no chunk {} (nota do terreno {})", local, melhor, melhorNota);
        return Optional.ofNullable(melhor);
    }

    /**
     * Fração (0 a 1) da vizinhança do local, numa faixa de ~100 blocos em volta da rampa, com bioma fora da tag
     * {@code dcuniverse:<local>_vizinhanca} (ex.: neve e gelo colados numa vila do Kansas). Sem a tag, 0.
     */
    private float vizinhosEstranhos(GenerationContext ctx, Local.Terreno t, int[] r) {
        var tag = net.minecraft.tags.TagKey.create(Registries.BIOME,
            net.minecraft.resources.Identifier.fromNamespaceAndPath(com.danrod.dcuniverse.DCUniverse.MODID, local + "_vizinhanca"));
        if (ctx.registryAccess().lookupOrThrow(Registries.BIOME).get(tag).isEmpty()) return 0;
        int dentro = PecaPlato.RAMPA, fora = 112, estranhos = 0, amostras = 0;
        for (int x = r[0] - fora; x <= r[2] + fora; x += 32) {
            for (int z = r[1] - fora; z <= r[3] + fora; z += 32) {
                if (x > r[0] - dentro && x < r[2] + dentro && z > r[1] - dentro && z < r[3] + dentro) continue;
                amostras++;
                var b = ctx.biomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(t.superficie(x, z)), QuartPos.fromBlock(z), ctx.randomState().sampler());
                if (!b.is(tag)) estranhos++;
            }
        }
        return amostras == 0 ? 0 : estranhos / (float) amostras;
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
    private static boolean temVila(GenerationContext ctx, ChunkGeneratorStructureState estado, int[] r) {
        var sets = ctx.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        var vilas = sets.get(BuiltinStructureSets.VILLAGES);
        if (vilas.isEmpty()) return false;
        StructurePlacement p = vilas.get().value().placement();
        for (int cx = (r[0] >> 4) - 4; cx <= (r[2] >> 4) + 4; cx++)
            for (int cz = (r[1] >> 4) - 4; cz <= (r[3] >> 4) + 4; cz++)
                if (p.isStructureChunk(estado, cx, cz)) return true;
        return false;
    }

    @Override
    public StructureType<?> type() {
        return Estruturas.LOCAL.get();
    }
}
