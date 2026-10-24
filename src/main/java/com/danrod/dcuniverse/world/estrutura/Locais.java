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
}
