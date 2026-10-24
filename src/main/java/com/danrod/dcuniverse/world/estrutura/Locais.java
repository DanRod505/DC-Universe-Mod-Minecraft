package com.danrod.dcuniverse.world.estrutura;

import com.danrod.dcuniverse.DCUniverse;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.Optional;

/** Acha um local gerado no mundo e os pontos com nome dele (para o comando /dcu local e para testes). */
public final class Locais {
    private Locais() {}

    public static ResourceKey<Structure> chave(String local) {
        return ResourceKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath(DCUniverse.MODID, local));
    }

    /** O início da estrutura mais perto de {@code perto} (até ~1600 blocos), ou vazio. */
    public static Optional<StructureStart> achar(ServerLevel level, String local, BlockPos perto) {
        var reg = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Optional<Holder.Reference<Structure>> h = reg.get(chave(local));
        if (h.isEmpty()) return Optional.empty();
        Pair<BlockPos, Holder<Structure>> p = level.getChunkSource().getGenerator()
            .findNearestMapStructure(level, HolderSet.direct(h.get()), perto, 100, false);
        if (p == null) return Optional.empty();
        BlockPos pos = p.getFirst();
        var chunk = level.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.STRUCTURE_STARTS);
        StructureStart s = level.structureManager().getStartForStructure(SectionPos.of(pos), h.get().value(), chunk);
        return s != null && s.isValid() ? Optional.of(s) : Optional.empty();
    }

    /** Posição de um ponto com nome; sem nome, o centro do local. */
    public static Optional<BlockPos> ponto(ServerLevel level, String local, String nome, BlockPos perto) {
        Optional<StructureStart> s = achar(level, local, perto);
        if (s.isEmpty()) return Optional.empty();
        Local l = Estruturas.LOCAIS.get(local);
        Local.Ponto pt = l == null ? null : l.pontos().get(nome);
        BlockPos alvo = null;
        if (pt != null) {
            for (StructurePiece pc : s.get().getPieces()) {
                if (pc instanceof PecaObra po && po.planta().equals(pt.planta())) {
                    alvo = po.mundo(pt.x(), pt.y(), pt.z());
                    break;
                }
            }
        }
        if (alvo == null) alvo = s.get().getBoundingBox().getCenter();
        if (pt == null || pt.chao()) {
            level.getChunk(alvo);
            alvo = new BlockPos(alvo.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, alvo.getX(), alvo.getZ()), alvo.getZ());
        }
        return Optional.of(alvo);
    }

    /**
     * Confere o nivelamento de um local já gerado (comando /dcu conferir_local): altura do platô, água, buracos e
     * chão fora do nível onde não há construção, degrau máximo da rampa e os biomas do platô.
     */
    public static String conferir(ServerLevel level, String local, BlockPos perto) {
        Optional<StructureStart> s = achar(level, local, perto);
        if (s.isEmpty()) return local + ": não achado";
        PecaPlato plato = null;
        java.util.List<net.minecraft.world.level.levelgen.structure.BoundingBox> obras = new java.util.ArrayList<>();
        for (StructurePiece pc : s.get().getPieces()) {
            if (pc instanceof PecaPlato pp) plato = pp;
            else if (pc instanceof PecaObra) obras.add(pc.getBoundingBox());
        }
        if (plato == null) return local + ": sem platô (gerado antes da v0.4)";
        var caixa = plato.getBoundingBox();
        int y = plato.y();
        int agua = 0, buracos = 0, fora = 0, colunas = 0, degrau = 0;
        String ondeAgua = "", ondeFora = "", ondeBuraco = "", ondeSolto = "";
        int soltos = 0, soltosRampa = 0;
        java.util.Map<String, Integer> biomas = new java.util.TreeMap<>();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = caixa.minX(); x <= caixa.maxX(); x++) {
            for (int z = caixa.minZ(); z <= caixa.maxZ(); z++) {
                level.getChunk(x >> 4, z >> 4);
                if (!plato.noPlato(x, z)) {
                    // Na rampa: nada solto no ar acima do chão (folhas, troncos e cipós das árvores não contam).
                    int chaoRampa = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                    for (int yy = chaoRampa + 2; yy <= chaoRampa + 24; yy++) {
                        var b = level.getBlockState(p.set(x, yy, z));
                        if (b.isAir() || b.is(net.minecraft.tags.BlockTags.LEAVES) || b.is(net.minecraft.tags.BlockTags.LOGS) || b.is(Blocks.VINE)) continue;
                        if (level.getBlockState(p.set(x, yy - 1, z)).isAir() && soltosRampa++ < 3) ondeSolto += " rampa:" + x + "," + yy + "," + z + "="
                            + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b.getBlock()).getPath() + "(chão " + chaoRampa + ")";
                    }
                    continue;
                }
                colunas++;
                boolean obra = false;
                for (var b : obras) if (b.isInside(x, b.minY(), z)) { obra = true; break; }
                // Água de propósito (bebedouro, poço) fica dentro das construções; fora delas não pode haver.
                if (obra) continue;
                for (int yy = y - 2; yy <= y + 3; yy++) if (!level.getFluidState(p.set(x, yy, z)).isEmpty()) {
                    if (agua++ == 0) ondeAgua = x + "," + yy + "," + z;
                }
                var chao = level.getBlockState(p.set(x, y, z));
                if ((chao.isAir() || !chao.getFluidState().isEmpty()) && buracos++ < 3) ondeBuraco += " " + x + "," + z;
                int topo = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                // Solto no ar: um bloco sem nada embaixo acima do chão (capim e flor só valem em cima do chão).
                for (int yy = y + 2; yy <= y + 24; yy++) {
                    var b = level.getBlockState(p.set(x, yy, z));
                    if (!b.isAir() && level.getBlockState(p.set(x, yy - 1, z)).isAir() && soltos++ < 3) ondeSolto += " " + x + "," + yy + "," + z + "="
                        + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b.getBlock()).getPath();
                }
                if (topo != y && fora++ < 3) ondeFora += " " + x + "," + topo + "," + z + "="
                    + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(p.set(x, topo, z)).getBlock()).getPath();
                if (Math.floorMod(x, 8) == 0 && Math.floorMod(z, 8) == 0)
                    biomas.merge(level.getBiome(p.set(x, y + 1, z)).unwrapKey().map(k -> k.identifier().getPath()).orElse("?"), 1, Integer::sum);
            }
        }
        // Rampa: quatro linhas saindo do meio de cada lado do platô.
        int cx = (caixa.minX() + caixa.maxX()) / 2, cz = (caixa.minZ() + caixa.maxZ()) / 2;
        int[][] linhas = {{caixa.minX(), cz, 1, 0}, {caixa.maxX(), cz, -1, 0}, {cx, caixa.minZ(), 0, 1}, {cx, caixa.maxZ(), 0, -1}};
        for (int[] l : linhas) {
            int antes = Integer.MIN_VALUE;
            for (int x = l[0], z = l[1]; !plato.noPlato(x, z); x += l[2], z += l[3]) {
                int topo = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                if (antes != Integer.MIN_VALUE) degrau = Math.max(degrau, Math.abs(topo - antes));
                antes = topo;
            }
        }
        // Vizinhança: biomas fora da tag <local>_vizinhanca numa faixa de ~100 blocos em volta.
        var tag = net.minecraft.tags.TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(DCUniverse.MODID, local + "_vizinhanca"));
        java.util.Map<String, Integer> estranhos = new java.util.TreeMap<>();
        for (int x = caixa.minX() - 96; x <= caixa.maxX() + 96; x += 32)
            for (int z = caixa.minZ() - 96; z <= caixa.maxZ() + 96; z += 32) {
                if (caixa.isInside(x, caixa.minY(), z)) continue;
                level.getChunk(x >> 4, z >> 4);
                int sy = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                var b = level.getBiome(new BlockPos(x, sy, z));
                if (!b.is(tag)) estranhos.merge(b.unwrapKey().map(k -> k.identifier().getPath()).orElse("?"), 1, Integer::sum);
            }
        BlockPos c = caixa.getCenter();
        return local + " em " + c.getX() + "," + y + "," + c.getZ() + ": colunas " + colunas + ", água fora das construções " + agua + (agua > 0 ? " (" + ondeAgua + ")" : "")
            + ", buracos " + buracos + (buracos > 0 ? " (" + ondeBuraco.trim() + ")" : "") + ", chão fora do nível " + fora + (fora > 0 ? " (" + ondeFora.trim() + ")" : "") + ", soltos no ar " + soltos + " no platô e " + soltosRampa + " na rampa" + (soltos + soltosRampa > 0 ? " (" + ondeSolto.trim() + ")" : "") + ", maior degrau da rampa " + degrau + ", biomas " + biomas + ", vizinhos estranhos " + estranhos;
    }
}
