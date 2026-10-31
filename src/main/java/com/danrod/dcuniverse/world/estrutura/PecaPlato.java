package com.danrod.dcuniverse.world.estrutura;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.neoforged.neoforge.common.world.PieceBeardifierModifier;

/**
 * Platô de um local: a primeira peça de todo local nivelado. Deixa a área inteira num nível só, seca e limpa, e
 * em volta faz uma rampa suave até o terreno natural. Duas camadas trabalham juntas:
 * <ul>
 *   <li>no ruído do terreno, o "beard" do próprio jogo (o mesmo das vilas) já puxa o chão para o nível do platô
 *   até uns 12 blocos acima e abaixo, então o relevo e a superfície (grama, terra) nascem certos;</li>
 *   <li>na geração da peça, cada coluna é acertada de vez: corta morros, aterra buracos, tira água, troncos e
 *   folhas, e na rampa (até {@link #RAMPA} blocos) mistura o nível do platô com o chão natural.</li>
 * </ul>
 * As outras peças do local vêm depois na lista, então já encontram o chão pronto em {@link #y}.
 */
public final class PecaPlato extends StructurePiece implements PieceBeardifierModifier {
    /** Largura máxima da rampa que liga o platô ao terreno natural (locais grandes usam menos no lado comprido, ver
     * {@link LocalEstrutura#rampa}). */
    public static final int RAMPA = 20;
    /** Quanto aterrar no máximo por baixo do platô (vales mais fundos ganham um "pé" de pedra). */
    private static final int ATERRO = 48;
    /** Até onde (abaixo do platô) o beard mexe no terreno e pode deixar vãos: ver {@link #fecharVaos}. */
    private static final int VAO = 14;

    /** Retângulo do platô (já com a folga) e altura do topo do chão. */
    private final int x1, z1, x2, z2, y, rampaX, rampaZ;
    private final BlockState solo;

    /** {@code rampaX}, {@code rampaZ} = largura da rampa nos lados leste/oeste e norte/sul. */
    public PecaPlato(int x1, int z1, int x2, int z2, int y, int rampaX, int rampaZ, BlockState solo) {
        super(Estruturas.PECA_PLATO.get(), 0, new BoundingBox(x1 - rampaX, y - ATERRO, z1 - rampaZ, x2 + rampaX, y + 8, z2 + rampaZ));
        this.rampaX = rampaX;
        this.rampaZ = rampaZ;
        this.x1 = x1;
        this.z1 = z1;
        this.x2 = x2;
        this.z2 = z2;
        this.y = y;
        this.solo = solo;
    }

    public PecaPlato(CompoundTag tag) {
        super(Estruturas.PECA_PLATO.get(), tag);
        int[] r = tag.getIntArray("plato").orElse(new int[7]);
        this.rampaX = r.length > 6 ? r[5] : RAMPA;
        this.rampaZ = r.length > 6 ? r[6] : RAMPA;
        this.x1 = r[0];
        this.z1 = r[1];
        this.x2 = r[2];
        this.z2 = r[3];
        this.y = r[4];
        BlockState s = Blocks.GRASS_BLOCK.defaultBlockState();
        var id = net.minecraft.resources.Identifier.tryParse(tag.getStringOr("solo", ""));
        if (id != null) s = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(id).defaultBlockState();
        this.solo = s;
    }

    /** Altura do topo do chão do platô. */
    public int y() {
        return y;
    }

    /** O retângulo do platô contém a coluna? */
    public boolean noPlato(int x, int z) {
        return x >= x1 && x <= x2 && z >= z1 && z <= z2;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putIntArray("plato", new int[]{x1, z1, x2, z2, y, rampaX, rampaZ});
        tag.putString("solo", net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(solo.getBlock()).toString());
    }

    // ---------- beard do ruído (Forge) ----------

    @Override
    public BoundingBox getBeardifierBox() {
        // O "chão" do beard é o y mínimo da caixa: tudo abaixo dele fica sólido, tudo acima vira ar.
        return new BoundingBox(x1, y + 1, z1, x2, y + 1, z2);
    }

    @Override
    public TerrainAdjustment getTerrainAdjustment() {
        return TerrainAdjustment.BEARD_THIN;
    }

    @Override
    public int getGroundLevelDelta() {
        return 0;
    }

    // ---------- acerto de cada coluna ----------

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
        acertar(level, chunkBB, generator.getSeaLevel(), Heightmap.Types.WORLD_SURFACE_WG);
    }

    /**
     * Acerta as colunas do platô e da rampa dentro de {@code chunkBB}. {@code alturas} = o mapa de altura para achar
     * o topo de cada coluna (na geração, o de geração; num mundo pronto, o normal).
     */
    public void acertar(WorldGenLevel level, BoundingBox chunkBB, int mar, Heightmap.Types alturas) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = Math.max(chunkBB.minX(), x1 - rampaX); x <= Math.min(chunkBB.maxX(), x2 + rampaX); x++) {
            for (int z = Math.max(chunkBB.minZ(), z1 - rampaZ); z <= Math.min(chunkBB.maxZ(), z2 + rampaZ); z++) {
                int dx = Math.max(0, Math.max(x1 - x, x - x2));
                int dz = Math.max(0, Math.max(z1 - z, z - z2));
                // Distância na rampa de 0 (borda do platô) a 1 (terreno natural); nos cantos, uma curva (elipse).
                double ax = dx / (rampaX + 1.0), az = dz / (rampaZ + 1.0);
                double n = Math.sqrt(ax * ax + az * az);
                if (dx > rampaX || dz > rampaZ || n >= 1) continue;
                coluna(level, p, x, z, n, mar, alturas);
            }
        }
    }

    private void coluna(WorldGenLevel level, BlockPos.MutableBlockPos p, int x, int z, double n, int mar, Heightmap.Types alturas) {
        // O mapa de altura "de geração" não vê o que a vegetação dos chunks vizinhos já pôs aqui (capim, flores,
        // galhos): o de superfície vê, então vale o mais alto dos dois.
        int topo = Math.max(level.getHeight(alturas, x, z), level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z)) - 1;
        int min = level.getMinY();
        // Chão natural: o primeiro bloco firme de cima para baixo (sem água, folhas, troncos e plantas).
        int g = topo;
        while (g > min && !firme(level.getBlockState(p.set(x, g, z)))) g--;
        BlockState natural = level.getBlockState(p.set(x, g, z));
        boolean agua = false;
        for (int yy = g + 1; yy <= topo && !agua; yy++) agua = !level.getBlockState(p.set(x, yy, z)).getFluidState().isEmpty();

        if (n == 0) {
            // Dentro do platô: tudo acima do nível vira ar (morro, árvore, água) e o buraco embaixo é aterrado.
            for (int yy = Math.max(topo, y + 1); yy > y; yy--) limpar(level, p.set(x, yy, z));
            por(level, p.set(x, y, z), solo);
            for (int yy = y - 1; yy >= y - ATERRO; yy--) {
                BlockState s = level.getBlockState(p.set(x, yy, z));
                if (yy < y - 3 && firme(s)) break;
                por(level, p, yy >= y - 3 ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState());
            }
            fecharVaos(level, p, x, z, y, true);
            return;
        }
        // Rampa: do nível do platô até o chão natural, com curva suave (sem degrau na borda nem no fim).
        double f = n * n * (3 - 2 * n);
        int t = (int) Math.round(y + (g - y) * f);
        BlockState cobertura = cobertura(natural);
        if (t > g) {
            // Aterro: cobre o chão natural; com água em cima, só aterra se a rampa sair do nível do mar.
            if (agua && t < mar) return;
            for (int yy = g; yy < t; yy++) por(level, p.set(x, yy, z), Blocks.DIRT.defaultBlockState());
            por(level, p.set(x, t, z), cobertura);
            for (int yy = t + 1; yy <= Math.min(topo, t + 3); yy++) {
                BlockState s = level.getBlockState(p.set(x, yy, z));
                if (!s.getFluidState().isEmpty() || (!s.isAir() && !s.is(BlockTags.LEAVES) && !s.is(BlockTags.LOGS))) limpar(level, p);
            }
            fecharVaos(level, p, x, z, t, true);
        } else if (t < g) {
            // Corte: tira o morro acima da rampa (com as árvores) e refaz a grama.
            for (int yy = topo; yy > t; yy--) {
                BlockState s = level.getBlockState(p.set(x, yy, z));
                // Abaixo do nível do mar a água fica (é a margem de um lago ou do mar).
                if (yy < mar && !s.getFluidState().isEmpty()) continue;
                limpar(level, p);
            }
            if (t >= mar || !agua) {
                por(level, p.set(x, t, z), cobertura);
                fecharVaos(level, p, x, z, t, t >= mar);
            }
        } else {
            // No nível certo: só os vãos embaixo (um oco no pé do morro no fim da rampa).
            fecharVaos(level, p, x, z, t, !agua && t >= mar);
        }
    }

    /**
     * Fecha os vãos embaixo do chão novo (topo em {@code chao}) até {@link #VAO} blocos abaixo do platô. O "beard"
     * do ruído só puxa o terreno para o nível do platô perto dele: num vale ou lago sobra uma casca fina de terra com
     * o vazio embaixo, e no pé de um morro ele escava um oco por baixo, que o corte da rampa deixava pendurado.
     * {@code agua}: também aterra água (a rampa saiu do nível do mar).
     */
    private void fecharVaos(WorldGenLevel level, BlockPos.MutableBlockPos p, int x, int z, int chao, boolean agua) {
        for (int yy = chao - 1; yy >= y - VAO; yy--) {
            BlockState s = level.getBlockState(p.set(x, yy, z));
            if (firme(s) || (!agua && !s.getFluidState().isEmpty())) continue;
            por(level, p, yy >= chao - 3 ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState());
        }
    }

    /** Bloco firme o bastante para ser o chão (folhas, troncos, plantas e líquidos não contam). */
    private static boolean firme(BlockState s) {
        return Obra.chaoFirme(s) && s.getFluidState().isEmpty();
    }

    /** O bloco de cima da rampa: o mesmo tipo de chão do terreno natural (areia na praia, neve na montanha). */
    private BlockState cobertura(BlockState natural) {
        if (natural.is(BlockTags.SAND) || natural.is(Blocks.SNOW_BLOCK) || natural.is(Blocks.PODZOL) || natural.is(Blocks.MYCELIUM)
            || natural.is(Blocks.COARSE_DIRT) || natural.is(Blocks.MUD) || natural.is(BlockTags.TERRACOTTA)) return natural;
        return solo;
    }

    private static void limpar(WorldGenLevel level, BlockPos p) {
        if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_ON_PLACE);
    }

    private static void por(WorldGenLevel level, BlockPos p, BlockState s) {
        if (level.getBlockState(p).is(Blocks.BEDROCK)) return;
        level.setBlock(p, s, Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_ON_PLACE);
    }
}
