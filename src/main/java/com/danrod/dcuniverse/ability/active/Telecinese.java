package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.character.Transformacao;
import com.danrod.dcuniverse.content.BlocoTelecinetico;
import com.danrod.dcuniverse.content.Sons;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * telekinesis: telecinese tátil do Superboy. Sem nada seguro, arranca um pedaço do terreno onde mira (radius em
 * volta, depth para dentro da face mirada, até max_blocks, só o que a superforça quebraria) e o segura flutuando
 * na frente, na mesma forma. Com os blocos seguros: usar de novo lança todos para a mira (ferem quem acertam e
 * viram bloco onde batem); agachado, pousa o pedaço inteiro, na mesma forma, onde mira (construção). Agachado sem
 * nada seguro, esfarela o pedaço mirado e os itens vêm para o inventário (mineração em área). Lançar e pousar não
 * custam energia nem esperam a recarga. Depois de hold segundos os blocos caem sozinhos.
 */
public final class Telecinese implements TipoHabilidade {
    private static final Map<UUID, Carga> SEGUROS = new ConcurrentHashMap<>();

    private record Carga(List<BlocoTelecinetico> blocos, List<BlockPos> forma, long ate) {
        boolean viva() {
            return blocos.stream().anyMatch(b -> b.isAlive() && b.fase() == BlocoTelecinetico.SEGURO);
        }
    }

    @Override
    public boolean gratis(ServerPlayer player, Personagem.Habilidade h) {
        Carga c = SEGUROS.get(player.getUUID());
        return c != null && c.viva();
    }

    public static boolean segurando(Player player) {
        Carga c = SEGUROS.get(player.getUUID());
        return c != null && c.viva();
    }

    @Override
    public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        Params prm = h.params();
        Carga c = SEGUROS.get(player.getUUID());
        if (c != null && c.viva()) {
            if (player.isShiftKeyDown()) pousar(level, player, c, prm);
            else lancar(level, player, p, c, prm);
            return true;
        }
        BlockHitResult hit = player.level().clip(new ClipContext(player.getEyePosition(),
            player.getEyePosition().add(player.getLookAngle().scale(prm.d("range", 20))), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            Transformacao.avisar(player, Component.translatable("dcuniverse.msg.sem_alvo"));
            return false;
        }
        List<BlockPos> area = area(level, player, hit, prm);
        if (area.isEmpty()) {
            Transformacao.avisar(player, Component.translatable("dcuniverse.msg.telecinese_nada"));
            return false;
        }
        if (player.isShiftKeyDown()) esfarelar(level, player, area);
        else erguer(level, player, area, prm);
        return true;
    }

    /** Os blocos do pedaço mirado que a telecinese consegue mover, do mais perto da face para dentro. */
    static List<BlockPos> area(ServerLevel level, ServerPlayer player, BlockHitResult hit, Params prm) {
        int r = prm.i("radius", 1), fundo = Math.max(1, prm.i("depth", 2)), max = prm.i("max_blocks", 18);
        float dureza = prm.f("max_hardness", 30);
        Direction face = hit.getDirection();
        Direction.Axis eixo = face.getAxis();
        BlockPos c = hit.getBlockPos();
        List<BlockPos> out = new ArrayList<>();
        for (int k = 0; k < fundo; k++) {
            for (int a = -r; a <= r; a++) {
                for (int b = -r; b <= r; b++) {
                    BlockPos pos = switch (eixo) {
                        case X -> c.offset(0, a, b);
                        case Y -> c.offset(a, 0, b);
                        case Z -> c.offset(a, b, 0);
                    };
                    pos = pos.relative(face.getOpposite(), k);
                    if (out.size() < max && movel(level, player, pos, dureza)) out.add(pos.immutable());
                }
            }
        }
        return out;
    }

    /** Pode mover? Nada de ar, líquido, bloco com conteúdo (baú, fornalha), meio bloco (porta, cama) ou duro demais. */
    static boolean movel(ServerLevel level, ServerPlayer player, BlockPos pos, float dureza) {
        BlockState st = level.getBlockState(pos);
        if (st.isAir() || !st.getFluidState().isEmpty() || st.hasBlockEntity()) return false;
        float d = st.getDestroySpeed(level, pos);
        if (d < 0 || d >= Math.min(dureza, 49)) return false;
        if (st.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) || st.hasProperty(BlockStateProperties.BED_PART)) return false;
        return level.mayInteract(player, pos) && player.mayBuild();
    }

    private static void erguer(ServerLevel level, ServerPlayer player, List<BlockPos> area, Params prm) {
        Vec3 meio = Vec3.ZERO;
        for (BlockPos b : area) meio = meio.add(Vec3.atCenterOf(b));
        meio = meio.scale(1.0 / area.size());
        // A forma fica de frente para quem levantou: gira para o yaw do jogador.
        float yaw = player.getYRot() * net.minecraft.util.Mth.DEG_TO_RAD;
        BlockPos base = BlockPos.containing(meio);
        List<BlocoTelecinetico> blocos = new ArrayList<>();
        List<BlockPos> forma = new ArrayList<>();
        for (BlockPos pos : area) {
            BlockState st = level.getBlockState(pos);
            Vec3 rel = Vec3.atCenterOf(pos).subtract(meio).yRot(yaw);
            level.levelEvent(2001, pos, Block.getId(st));
            level.removeBlock(pos, false);
            blocos.add(BlocoTelecinetico.criar(level, player, pos, st, rel));
            forma.add(pos.subtract(base));
        }
        SEGUROS.put(player.getUUID(), new Carga(blocos, forma, level.getGameTime() + Math.round(prm.f("hold", 30) * 20)));
        level.playSound(null, meio.x, meio.y, meio.z, Sons.TELECINESE_ERGUE.get(), SoundSource.PLAYERS, 1.4f, 1f);
        com.danrod.dcuniverse.guia.Guia.concluir(player, "telecinese");
    }

    private static void lancar(ServerLevel level, ServerPlayer player, Personagem p, Carga c, Params prm) {
        HitResult mira = Alvos.mirar(player, 64);
        Vec3 alvo = mira.getType() == HitResult.Type.MISS ? player.getEyePosition().add(player.getLookAngle().scale(64)) : mira.getLocation();
        float dano = Alvos.dano(p, prm.d("damage_mult", 0.6));
        double vel = prm.d("throw_speed", 2.2);
        Map<Integer, Integer> golpes = new HashMap<>();
        for (BlocoTelecinetico b : c.blocos()) {
            if (!b.isAlive() || b.fase() != BlocoTelecinetico.SEGURO) continue;
            Vec3 de = b.position().add(0, 0.5, 0);
            Vec3 d = alvo.subtract(de);
            // Compensa a queda do arco (gravidade de BlocoTelecinetico, 0,045 por tick²): mira acima o quanto ele cai.
            double dist = d.length();
            Vec3 dir = d.add(0, 0.0225 * dist * dist / (vel * vel), 0).normalize();
            b.lancar(dir.scale(vel), dano, golpes);
        }
        SEGUROS.remove(player.getUUID());
        level.playSound(null, player.getX(), player.getY(), player.getZ(), Sons.TELECINESE_LANCA.get(), SoundSource.PLAYERS, 1.4f, 1f);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
    }

    /** Pousa a forma inteira onde a mira encosta (em cima da face mirada), sem girar. */
    private static void pousar(ServerLevel level, ServerPlayer player, Carga c, Params prm) {
        BlockHitResult hit = player.level().clip(new ClipContext(player.getEyePosition(),
            player.getEyePosition().add(player.getLookAngle().scale(prm.d("range", 20) + 8)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            Transformacao.avisar(player, Component.translatable("dcuniverse.msg.sem_alvo"));
            return;
        }
        int minY = c.forma().stream().mapToInt(BlockPos::getY).min().orElse(0);
        BlockPos base = hit.getBlockPos().relative(hit.getDirection()).below(minY);
        for (int i = 0; i < c.blocos().size(); i++) {
            BlocoTelecinetico b = c.blocos().get(i);
            if (b.isAlive() && b.fase() == BlocoTelecinetico.SEGURO) b.pousar(base.offset(c.forma().get(i)));
        }
        SEGUROS.remove(player.getUUID());
        level.playSound(null, hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, Sons.TELECINESE_POUSA.get(), SoundSource.PLAYERS, 1.2f, 1f);
    }

    /** Mineração em área: o pedaço se desfaz e os itens vêm para o inventário (o que não couber cai aos pés). */
    private static void esfarelar(ServerLevel level, ServerPlayer player, List<BlockPos> area) {
        for (BlockPos pos : area) {
            BlockState st = level.getBlockState(pos);
            List<ItemStack> drops = Block.getDrops(st, level, pos, null, player, ItemStack.EMPTY);
            level.levelEvent(2001, pos, Block.getId(st));
            level.removeBlock(pos, false);
            st.spawnAfterBreak(level, pos, ItemStack.EMPTY, true);
            for (ItemStack it : drops) {
                if (!player.getInventory().add(it)) player.drop(it, false);
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), Sons.TELECINESE_ERGUE.get(), SoundSource.PLAYERS, 1.1f, 1.3f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6f, 0.8f);
    }

    /** Tick do servidor: depois do tempo de segurar, os blocos caem sozinhos. */
    public static void tick(net.minecraft.server.MinecraftServer server) {
        SEGUROS.entrySet().removeIf(e -> {
            Carga c = e.getValue();
            if (!c.viva()) return true;
            ServerPlayer dono = server.getPlayerList().getPlayer(e.getKey());
            long agora = c.blocos().get(0).level().getGameTime();
            if (dono == null || agora > c.ate() || !com.danrod.dcuniverse.character.EstadoJogador.de(dono).transformado()) {
                soltar(c);
                return true;
            }
            return false;
        });
    }

    private static void soltar(Carga c) {
        for (BlocoTelecinetico b : c.blocos()) if (b.isAlive() && b.fase() == BlocoTelecinetico.SEGURO) b.soltar();
    }

    public static void aoDestransformar(ServerPlayer player) {
        Carga c = SEGUROS.remove(player.getUUID());
        if (c != null) soltar(c);
    }

    public static void limpar() {
        SEGUROS.clear();
    }
}
