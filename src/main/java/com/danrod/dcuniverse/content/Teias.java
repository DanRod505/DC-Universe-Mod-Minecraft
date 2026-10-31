package com.danrod.dcuniverse.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;
import com.danrod.dcuniverse.core.Eventos;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Teias temporárias da rede do Batman (content/RedeEntidade): blocos de teia de verdade, que prendem quem estiver
 * dentro, e somem sozinhos no prazo. Quebradas antes (espada, batarangue), não deixam linha: não viram fonte de
 * itens. Quando o servidor fecha, todas são tiradas na hora.
 */
public final class Teias {
    private record Chave(ServerLevel level, BlockPos pos) {}

    private static final Map<Chave, Long> ATE = new HashMap<>();

    private Teias() {}

    public static void registrar() {
        NeoForge.EVENT_BUS.addListener(BreakBlockEvent.class, Eventos.cancela((Predicate<BreakBlockEvent>) e -> {
            if (!(e.getLevel() instanceof ServerLevel level) || ATE.remove(new Chave(level, e.getPos().immutable())) == null) return false;
            level.removeBlock(e.getPos(), false);
            return true;
        }));
    }

    /** Põe uma teia em pos (só no ar ou em plantas que se substituem) até o tick ate. @return true se pôs */
    public static boolean por(ServerLevel level, BlockPos pos, long ate) {
        var st = level.getBlockState(pos);
        Chave k = new Chave(level, pos.immutable());
        if (st.is(Blocks.COBWEB) && ATE.containsKey(k)) {
            ATE.put(k, Math.max(ATE.get(k), ate));
            return true;
        }
        if (!st.isAir() && !(st.canBeReplaced() && st.getFluidState().isEmpty())) return false;
        level.setBlockAndUpdate(k.pos(), Blocks.COBWEB.defaultBlockState());
        ATE.put(k, ate);
        return true;
    }

    public static boolean temporaria(ServerLevel level, BlockPos pos) {
        return ATE.containsKey(new Chave(level, pos.immutable()));
    }

    /** Tira a teia sem deixar item (o batarangue cortando, o prazo acabando). */
    public static void tirar(ServerLevel level, BlockPos pos) {
        if (ATE.remove(new Chave(level, pos.immutable())) != null && level.getBlockState(pos).is(Blocks.COBWEB)) {
            level.removeBlock(pos, false);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBWEB.defaultBlockState()),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.05);
        }
    }

    public static void tick(ServerLevel level) {
        long agora = level.getGameTime();
        Iterator<Map.Entry<Chave, Long>> it = ATE.entrySet().iterator();
        while (it.hasNext()) {
            var en = it.next();
            Chave k = en.getKey();
            if (k.level() != level || en.getValue() > agora) continue;
            it.remove();
            if (level.isLoaded(k.pos()) && level.getBlockState(k.pos()).is(Blocks.COBWEB)) {
                level.removeBlock(k.pos(), false);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBWEB.defaultBlockState()),
                    k.pos().getX() + 0.5, k.pos().getY() + 0.5, k.pos().getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.03);
                if (level.getRandom().nextInt(3) == 0) {
                    level.playSound(null, k.pos(), SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.4f, 1.3f);
                }
            }
        }
    }

    /** Servidor fechando: nenhuma teia temporária fica no mundo salvo. */
    public static void limpar() {
        for (Chave k : ATE.keySet()) {
            if (k.level().isLoaded(k.pos()) && k.level().getBlockState(k.pos()).is(Blocks.COBWEB)) k.level().removeBlock(k.pos(), false);
        }
        ATE.clear();
    }
}
