package com.danrod.dcuniverse.world.estrutura;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Peça genérica de estrutura: guarda só o id da planta, a origem, a rotação e a semente; a planta
 * (registrada em {@link Estruturas#PLANTAS}) redesenha tudo em cada chunk que a peça toca.
 */
public final class PecaObra extends StructurePiece implements net.minecraftforge.common.world.PieceBeardifierModifier {
    private final String planta;
    private final BlockPos origem;
    private final Rotation rot;
    private final long semente;
    /** Dados extras calculados no traçado (ex.: a altura de cada trecho de uma estrada); pode ser vazio. */
    private final int[] perfil;

    /** Folga em volta da caixa: a rampa de terra da terraplenagem passa um pouco da planta. */
    private static final int FOLGA = 4;

    public PecaObra(String planta, BlockPos origem, Rotation rot, long semente) {
        this(planta, origem, rot, semente, new int[0]);
    }

    public PecaObra(String planta, BlockPos origem, Rotation rot, long semente, int[] perfil) {
        super(Estruturas.PECA_OBRA.get(), 0, caixa(Estruturas.planta(planta), origem, rot).inflatedBy(FOLGA, 0, FOLGA));
        this.planta = planta;
        this.origem = origem;
        this.rot = rot;
        this.semente = semente;
        this.perfil = perfil;
    }

    public PecaObra(CompoundTag tag) {
        super(Estruturas.PECA_OBRA.get(), tag);
        this.planta = tag.getStringOr("planta", "");
        this.origem = BlockPos.of(tag.getLongOr("origem", 0L));
        this.rot = Rotation.values()[Math.floorMod(tag.getIntOr("rot", 0), 4)];
        this.semente = tag.getLongOr("semente", 0L);
        this.perfil = tag.getIntArray("perfil").orElse(new int[0]);
    }

    /**
     * Põe a planta com o canto mínimo da caixa (já girada) em {@code canto}, de frente para {@code frente}
     * (a direção para onde a fachada olha no mundo).
     */
    public static PecaObra comFrente(String planta, BlockPos canto, net.minecraft.core.Direction frente, long semente) {
        return comFrente(planta, canto, frente, semente, new int[0]);
    }

    public static PecaObra comFrente(String planta, BlockPos canto, net.minecraft.core.Direction frente, long semente, int[] perfil) {
        Rotation r = switch (frente) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
        BoundingBox b = caixa(Estruturas.planta(planta), BlockPos.ZERO, r);
        BlockPos origem = canto.offset(-b.minX(), 0, -b.minZ());
        return new PecaObra(planta, origem, r, semente, perfil);
    }

    /** Tamanho no mundo (x, z) da planta virada para {@code frente}. */
    public static int[] pegada(String planta, net.minecraft.core.Direction frente) {
        Planta p = Estruturas.planta(planta);
        boolean deLado = frente == net.minecraft.core.Direction.EAST || frente == net.minecraft.core.Direction.WEST;
        return deLado ? new int[]{p.fundo(), p.largura()} : new int[]{p.largura(), p.fundo()};
    }

    private static BoundingBox caixa(Planta p, BlockPos origem, Rotation r) {
        BlockPos a = StructureTemplate.transform(new BlockPos(0, -p.abaixo(), 0), Mirror.NONE, r, BlockPos.ZERO).offset(origem);
        BlockPos b = StructureTemplate.transform(new BlockPos(p.largura() - 1, p.altura() - 1, p.fundo() - 1), Mirror.NONE, r, BlockPos.ZERO).offset(origem);
        return BoundingBox.fromCorners(a, b);
    }

    public String planta() {
        return planta;
    }

    /** Posição no mundo de um ponto local da planta. */
    public BlockPos mundo(int x, int y, int z) {
        return StructureTemplate.transform(new BlockPos(x, y, z), Mirror.NONE, rot, BlockPos.ZERO).offset(origem);
    }

    // O terreno do local é acertado só pelo platô (PecaPlato); as construções não mexem no ruído.
    @Override
    public BoundingBox getBeardifierBox() {
        return getBoundingBox();
    }

    @Override
    public net.minecraft.world.level.levelgen.structure.TerrainAdjustment getTerrainAdjustment() {
        return net.minecraft.world.level.levelgen.structure.TerrainAdjustment.NONE;
    }

    @Override
    public int getGroundLevelDelta() {
        return 0;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("planta", planta);
        tag.putLong("origem", origem.asLong());
        tag.putInt("rot", rot.ordinal());
        tag.putLong("semente", semente);
        if (perfil.length > 0) tag.putIntArray("perfil", perfil);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
        Planta p = Estruturas.PLANTAS.get(planta);
        if (p == null) return;
        p.construir(new Obra(level, chunkBB, origem, rot, semente, perfil));
    }
}
