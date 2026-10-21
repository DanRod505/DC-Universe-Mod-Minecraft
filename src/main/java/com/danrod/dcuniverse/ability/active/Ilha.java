package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.character.Transformacao;
import com.danrod.dcuniverse.content.IlhaTelecinetica;
import com.danrod.dcuniverse.content.VeiculoDono;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * floating_island: Ilha Telecinética (content/IlhaTelecinetica). Arranca do chão, onde o jogador mira (até range
 * blocos), um disco de raio radius com o fundo afinando em cúpula até depth blocos, levando junto plantas e árvores
 * baixas de cima (até max_blocks; nada de líquido, bedrock ou bloco com conteúdo). Usar de novo com a ilha no mundo
 * sobe nela de volta, sem gastar a suprema.
 */
public final class Ilha implements TipoHabilidade {
    private static final Map<UUID, IlhaTelecinetica> ATIVAS = new ConcurrentHashMap<>();
    /** Quanto acima do chão de cada coluna ainda é levado junto (mato, flores, árvores pequenas). */
    private static final int ACIMA = 7;

    @Override
    public boolean gratis(ServerPlayer player, Personagem.Habilidade h) {
        IlhaTelecinetica i = ATIVAS.get(player.getUUID());
        return i != null && i.isAlive() && i.estado() != VeiculoDono.INDO;
    }

    @Override
    public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        IlhaTelecinetica atual = ATIVAS.get(player.getUUID());
        if (atual != null && atual.isAlive() && atual.estado() != VeiculoDono.INDO) {
            atual.chamar(player);
            return false;
        }
        Params prm = h.params();
        BlockHitResult hit = Alvos.mirarBloco(player, prm.d("range", 24), ClipContext.Fluid.NONE);
        BlockPos alvo = hit.getType() == HitResult.Type.MISS ? (player.onGround() ? player.blockPosition().below() : null) : hit.getBlockPos();
        if (alvo == null) {
            Transformacao.avisar(player, Component.translatable("dcuniverse.msg.sem_chao"));
            return false;
        }
        // A superfície perto do bloco mirado (e não o topo do mundo: embaixo de uma árvore ou de um teto também vale).
        int chao = superficie(level, alvo.getX(), alvo.getZ(), alvo.getY());
        if (chao == Integer.MIN_VALUE) chao = alvo.getY();
        BlockPos centro = new BlockPos(alvo.getX(), chao + 1, alvo.getZ());
        List<BlockPos> blocos = disco(level, player, centro, prm);
        if (blocos.size() < 8) {
            Transformacao.avisar(player, Component.translatable("dcuniverse.msg.ilha_sem_terreno"));
            return false;
        }
        int[] pos = new int[blocos.size()], ids = new int[blocos.size()];
        for (int i = 0; i < blocos.size(); i++) {
            BlockPos b = blocos.get(i);
            pos[i] = IlhaTelecinetica.pack(b.getX() - centro.getX(), b.getY() - centro.getY(), b.getZ() - centro.getZ());
            ids[i] = Block.getId(level.getBlockState(b));
        }
        // De cima para baixo: as plantas saem antes do chão que as segura (e não caem como item).
        for (BlockPos b : blocos) level.setBlock(b, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        ATIVAS.put(player.getUUID(), IlhaTelecinetica.criar(player, centro, pos, ids, prm));
        return true;
    }

    /** Os blocos do disco, de cima para baixo, até max_blocks. */
    static List<BlockPos> disco(ServerLevel level, ServerPlayer player, BlockPos centro, Params prm) {
        int r = Math.max(2, prm.i("radius", 5)), fundo = Math.max(2, prm.i("depth", 5)), max = prm.i("max_blocks", 420);
        List<BlockPos> out = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > r + 0.35) continue;
                int x = centro.getX() + dx, z = centro.getZ() + dz;
                int chao = superficie(level, x, z, centro.getY() - 1);
                // Só colunas perto da altura do centro (não arranca um morro inteiro do lado).
                if (chao == Integer.MIN_VALUE) continue;
                double f = Math.max(0, 1 - (d / (r + 0.5)) * (d / (r + 0.5)));
                int prof = 1 + (int) Math.round(fundo * Math.sqrt(f));
                for (int y = chao + ACIMA; y > chao - prof; y--) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (movel(level, player, p)) out.add(p);
                }
            }
        }
        out.sort(Comparator.comparingInt((BlockPos b) -> b.getY()).reversed());
        if (out.size() > max) {
            // Passou do limite: tira os de baixo (o fundo da cúpula), não os de cima.
            out = new ArrayList<>(out.subList(0, max));
        }
        return out;
    }

    /**
     * A superfície da coluna ligada à altura y: se y é chão, sobe enquanto o de cima também for (até 4); se é ar,
     * desce até achar chão (até 4). Assim um teto ou uma copa de árvore em cima não conta.
     */
    static int superficie(ServerLevel level, int x, int z, int y) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        if (chao(level.getBlockState(p.set(x, y, z)))) {
            int yy = y;
            while (yy < y + 4 && chao(level.getBlockState(p.set(x, yy + 1, z)))) yy++;
            return yy;
        }
        for (int yy = y - 1; yy >= y - 4; yy--) {
            if (chao(level.getBlockState(p.set(x, yy, z)))) return yy;
        }
        return Integer.MIN_VALUE;
    }

    private static boolean chao(BlockState st) {
        return st.blocksMotion() && st.getFluidState().isEmpty() && !st.is(net.minecraft.tags.BlockTags.LEAVES) && !st.is(net.minecraft.tags.BlockTags.LOGS);
    }

    private static boolean movel(ServerLevel level, ServerPlayer player, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        if (st.isAir() || !st.getFluidState().isEmpty() || st.hasBlockEntity()) return false;
        float d = st.getDestroySpeed(level, pos);
        return d >= 0 && d < 50 && level.mayInteract(player, pos);
    }

    public static void tick() {
        ATIVAS.values().removeIf(i -> !i.isAlive());
    }

    public static void aoDestransformar(ServerPlayer player) {
        IlhaTelecinetica i = ATIVAS.remove(player.getUUID());
        if (i != null && i.isAlive()) i.irEmbora();
    }

    public static void limpar() {
        ATIVAS.clear();
    }
}
