package com.danrod.dcuniverse.world.estrutura;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.function.Consumer;

/**
 * Canteiro de obras de uma peça: desenha em coordenadas locais (x = largura, y = altura a partir do piso,
 * z = fundo; a frente fica em z = 0 e olha para o norte local) e converte para o mundo com a rotação da peça.
 * Só escreve dentro do chunk que está sendo gerado ({@code recorte}), então cada chunk pode rodar a obra
 * inteira de novo e o resultado se encaixa. Todo sorteio por bloco usa {@link #ruido}, que depende só da
 * posição e da semente: nada muda de um chunk para o outro.
 */
public final class Obra {
    public final WorldGenLevel level;
    private final BoundingBox recorte;
    private final BlockPos origem;
    private final Rotation rot;
    private final long semente;
    /** Sorteios da obra inteira (paleta, variações); sempre na mesma ordem em todos os chunks. */
    public final RandomSource rand;
    /** Dados extras da peça calculados no traçado (ver {@link PecaObra}); pode ser vazio. */
    public final int[] perfil;

    public Obra(WorldGenLevel level, BoundingBox recorte, BlockPos origem, Rotation rot, long semente) {
        this(level, recorte, origem, rot, semente, new int[0]);
    }

    public Obra(WorldGenLevel level, BoundingBox recorte, BlockPos origem, Rotation rot, long semente, int[] perfil) {
        this.perfil = perfil;
        this.level = level;
        this.recorte = recorte;
        this.origem = origem;
        this.rot = rot;
        this.semente = semente;
        this.rand = RandomSource.create(semente);
    }

    // ---------- posições ----------

    public BlockPos mundo(int x, int y, int z) {
        return StructureTemplate.transform(new BlockPos(x, y, z), Mirror.NONE, rot, BlockPos.ZERO).offset(origem);
    }

    public boolean dentro(int x, int y, int z) {
        return recorte.isInside(mundo(x, y, z));
    }

    /** Direção local para direção no mundo. */
    public Direction dir(Direction local) {
        return local.getAxis().isHorizontal() ? rot.rotate(local) : local;
    }

    /** Número de 0 a 1 fixo para a posição (mesmo valor em qualquer chunk). */
    public float ruido(int x, int y, int z) {
        long h = semente ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (z * 0x165667B19E3779F9L);
        h = (h ^ (h >>> 31)) * 0x7FB5D329728EA185L;
        h = (h ^ (h >>> 27)) * 0x81DADEF4BC2DD44DL;
        return ((h ^ (h >>> 33)) >>> 40) / (float) (1 << 24);
    }

    // ---------- blocos ----------

    public void por(int x, int y, int z, BlockState st) {
        BlockPos p = mundo(x, y, z);
        if (!recorte.isInside(p)) return;
        if (level.getBlockState(p).is(Blocks.BEDROCK)) return;
        // Sem onPlace, como na geração do mundo: na abóbora de Halloween ele procura o padrão do golem nos vizinhos e
        // quebra (NullPointerException) quando um deles está num chunk ainda não carregado.
        level.setBlock(p, st.rotate(rot), Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_ON_PLACE);
        if (!st.getFluidState().isEmpty()) level.scheduleTick(p, st.getFluidState().getType(), 0);
        // Cercas, vidraças, muros e escadas se ligam aos vizinhos quando o chunk termina de gerar.
        Block b = st.getBlock();
        if (b instanceof CrossCollisionBlock || b instanceof WallBlock || b instanceof StairBlock || b instanceof net.minecraft.world.level.block.ChainBlock
            || b instanceof net.minecraft.world.level.block.FenceGateBlock) level.getChunk(p).markPosForPostProcessing(p);
    }

    public void por(int x, int y, int z, Block b) {
        por(x, y, z, b.defaultBlockState());
    }

    /**
     * Bloco na posição local. Fora do chunk que está sendo desenhado devolve ar: ler longe do chunk durante a
     * geração é inseguro (o vizinho pode nem existir ainda) e o que estiver lá é redesenhado no chunk dele.
     */
    public BlockState ler(int x, int y, int z) {
        BlockPos p = mundo(x, y, z);
        return recorte.isInside(p) ? level.getBlockState(p) : Blocks.AIR.defaultBlockState();
    }

    public void caixa(int x1, int y1, int z1, int x2, int y2, int z2, BlockState st) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) por(x, y, z, st);
    }

    public void caixa(int x1, int y1, int z1, int x2, int y2, int z2, Block b) {
        caixa(x1, y1, z1, x2, y2, z2, b.defaultBlockState());
    }

    public void ar(int x1, int y1, int z1, int x2, int y2, int z2) {
        caixa(x1, y1, z1, x2, y2, z2, Blocks.AIR.defaultBlockState());
    }

    /** Só as quatro paredes do retângulo (sem piso nem teto). */
    public void paredes(int x1, int y1, int z1, int x2, int y2, int z2, BlockState st) {
        caixa(x1, y1, z1, x2, y2, z1, st);
        caixa(x1, y1, z2, x2, y2, z2, st);
        caixa(x1, y1, z1, x1, y2, z2, st);
        caixa(x2, y1, z1, x2, y2, z2, st);
    }

    // ---------- terreno ----------

    /** Bloco que conta como chão firme (árvores e plantas não contam). */
    public static boolean chaoFirme(BlockState s) {
        return s.isSolid() && !s.is(BlockTags.LEAVES) && !s.is(BlockTags.LOGS) && !s.is(Blocks.BAMBOO);
    }

    /** Chão do terreno natural (terra, grama, pedra, areia, neve, água...), não de uma construção ou rua. */
    public static boolean natural(BlockState s) {
        return !s.getFluidState().isEmpty() || (s.is(BlockTags.DIRT) && !s.is(Blocks.DIRT_PATH)) || s.is(BlockTags.SAND) || s.is(BlockTags.BASE_STONE_OVERWORLD)
            || s.is(Blocks.GRAVEL) || s.is(Blocks.CLAY) || s.is(Blocks.SNOW_BLOCK) || s.is(BlockTags.TERRACOTTA) || s.is(Blocks.SANDSTONE);
    }

    /** Altura local do topo do chão natural na coluna (procura de cima para baixo). */
    public int chao(int x, int z, int deY, int ateY) {
        for (int y = deY; y >= ateY; y--) {
            BlockState s = ler(x, y, z);
            if (chaoFirme(s) || !s.getFluidState().isEmpty()) return y;
        }
        return ateY;
    }

    /**
     * Prepara o terreno do retângulo para que o chão fique em y = 0: limpa tudo acima (árvores e mato
     * inclusive) e enche por baixo até o chão firme com {@code base}. Em volta, uma rampa de {@code margem}
     * blocos funde o nível da obra com o terreno natural; o bloco de cima vira {@code topo}.
     */
    public void terraplenar(int x1, int z1, int x2, int z2, int margem, BlockState topo, BlockState base, int alturaLivre) {
        BlockState terra = Blocks.DIRT.defaultBlockState();
        for (int x = x1 - margem; x <= x2 + margem; x++) {
            for (int z = z1 - margem; z <= z2 + margem; z++) {
                if (!dentro(x, 0, z)) continue;
                int dx = Math.max(0, Math.max(x1 - x, x - x2));
                int dz = Math.max(0, Math.max(z1 - z, z - z2));
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > margem + 0.5) continue;
                // Chão natural: procura a partir do topo do terreno (morros mais altos que a obra também são cortados).
                BlockPos w = mundo(x, 0, z);
                int topoTerreno = Math.max(level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, w.getX(), w.getZ()),
                    level.getHeight(Heightmap.Types.WORLD_SURFACE, w.getX(), w.getZ())) - w.getY();
                int g = chao(x, z, Math.min(Math.max(topoTerreno, alturaLivre + 8), 64), -30);
                // Na rampa em volta, só mexe em chão natural: com a construção vizinha colada, o "chão" achado era o
                // telhado ou a parede dela, que era cortada e ganhava grama (virando terra) lá no alto.
                if (d > 0 && !natural(ler(x, g, z))) continue;
                double f = d / (margem + 1.0);
                f = f * f * (3 - 2 * f);
                int t = (int) Math.round(g * f);
                int limpaAte = Math.max(d == 0 ? alturaLivre : t + 1, Math.min(g + 10, Math.max(topoTerreno, g)));
                for (int y = t + 1; y <= limpaAte; y++) {
                    if (!ler(x, y, z).isAir()) por(x, y, z, Blocks.AIR.defaultBlockState());
                }
                if (d == 0) {
                    por(x, 0, z, topo);
                    for (int y = -1; y >= -30; y--) {
                        BlockState s = ler(x, y, z);
                        if (chaoFirme(s) && s.getFluidState().isEmpty() && y < -2) break;
                        por(x, y, z, base);
                    }
                } else {
                    for (int y = g + 1; y < t; y++) por(x, y, z, terra);
                    if (t != g || !ler(x, t, z).is(topo.getBlock())) por(x, t, z, topo);
                }
            }
        }
    }

    // ---------- peças prontas ----------

    public static BlockState escada(Block b, Direction facing, boolean deCabeca) {
        return b.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, deCabeca ? Half.TOP : Half.BOTTOM);
    }

    public static BlockState laje(Block b, boolean cima) {
        return b.defaultBlockState().setValue(SlabBlock.TYPE, cima ? SlabType.TOP : SlabType.BOTTOM);
    }

    public static BlockState virado(Block b, Direction facing) {
        BlockState s = b.defaultBlockState();
        if (s.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return s.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        if (s.hasProperty(BlockStateProperties.FACING)) return s.setValue(BlockStateProperties.FACING, facing);
        return s;
    }

    public static BlockState eixo(Block b, Direction.Axis a) {
        BlockState s = b.defaultBlockState();
        return s.hasProperty(BlockStateProperties.AXIS) ? s.setValue(BlockStateProperties.AXIS, a) : s;
    }

    public static BlockState alcapao(Block b, Direction facing, boolean aberto, boolean cima) {
        return b.defaultBlockState().setValue(TrapDoorBlock.FACING, facing).setValue(TrapDoorBlock.OPEN, aberto)
            .setValue(TrapDoorBlock.HALF, cima ? Half.TOP : Half.BOTTOM);
    }

    /** Porta de duas metades. {@code facing} = para onde a pessoa olha ao entrar. */
    public void porta(int x, int y, int z, Block b, Direction facing, boolean dobradicaDireita) {
        BlockState s = b.defaultBlockState().setValue(DoorBlock.FACING, facing)
            .setValue(DoorBlock.HINGE, dobradicaDireita ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT);
        por(x, y, z, s.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        por(x, y + 1, z, s.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /** Cama: o pé em (x, y, z) e a cabeceira um bloco para {@code facing}. */
    public void cama(int x, int y, int z, DyeColor cor, Direction facing) {
        Block b = Blocks.BED.pick(cor);
        BlockState s = b.defaultBlockState().setValue(BedBlock.FACING, facing);
        por(x, y, z, s.setValue(BedBlock.PART, BedPart.FOOT));
        int hx = x + facing.getStepX(), hz = z + facing.getStepZ();
        por(hx, y, hz, s.setValue(BedBlock.PART, BedPart.HEAD));
    }

    public void lanterna(int x, int y, int z, boolean pendurada) {
        por(x, y, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, pendurada));
    }

    /** Baú com loot (a tabela só é sorteada quando alguém abre). */
    public void bau(int x, int y, int z, Direction facing, ResourceKey<LootTable> loot) {
        BlockPos p = mundo(x, y, z);
        if (!recorte.isInside(p)) return;
        por(x, y, z, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing));
        RandomizableContainer.setBlockEntityLootTable(level, RandomSource.create(semente ^ p.asLong()), p, loot);
    }

    /** Placa com texto traduzível (cada linha é uma chave do lang; o cliente mostra no idioma dele). */
    public void placa(int x, int y, int z, BlockState placa, DyeColor cor, boolean brilho, String... linhas) {
        BlockPos p = mundo(x, y, z);
        if (!recorte.isInside(p)) return;
        por(x, y, z, placa);
        if (level.getBlockEntity(p) instanceof SignBlockEntity sb) {
            SignText t = new SignText().setColor(cor).setHasGlowingText(brilho);
            for (int i = 0; i < Math.min(4, linhas.length); i++) {
                if (!linhas[i].isEmpty()) t = t.setMessage(i, Component.translatable(linhas[i]));
            }
            // Na geração o bloco ainda não está num mundo carregado: o texto entra como dado salvo.
            var ops = level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
            net.minecraft.nbt.Tag texto = SignText.DIRECT_CODEC.encodeStart(ops, t).getOrThrow();
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            tag.put("front_text", texto);
            tag.put("back_text", texto.copy());
            tag.putBoolean("is_waxed", true);
            sb.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), tag));
        }
    }

    /** Põe uma criatura parada no lugar (só no chunk que contém a posição, então nasce uma vez). */
    public <T extends Entity> void criatura(EntityType<T> tipo, double x, int y, double z, float yawLocal, Consumer<T> ajuste) {
        BlockPos p = mundo((int) Math.floor(x), y, (int) Math.floor(z));
        if (!recorte.isInside(p)) return;
        T e = tipo.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
        if (e == null) return;
        double fx = x - Math.floor(x), fz = z - Math.floor(z);
        e.snapTo(p.getX() + fx, p.getY(), p.getZ() + fz, yawLocal + rotYaw(), 0f);
        if (e instanceof Mob m) {
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(p), EntitySpawnReason.STRUCTURE, null);
            m.setPersistenceRequired();
        }
        ajuste.accept(e);
        level.addFreshEntityWithPassengers(e);
    }

    private float rotYaw() {
        return switch (rot) {
            case CLOCKWISE_90 -> 90f; case CLOCKWISE_180 -> 180f; case COUNTERCLOCKWISE_90 -> 270f; default -> 0f;
        };
    }
}
