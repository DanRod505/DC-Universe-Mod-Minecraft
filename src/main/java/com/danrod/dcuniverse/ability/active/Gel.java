package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.content.GelEntidade;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.guia.Guia;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/**
 * explosive_gel: mirando num bloco (até range), espalha uma carga de gel explosivo na face dele (até max_placed
 * ao mesmo tempo). Agachado, ou mirando no vazio com gel colocado, detona todos (sem custo): cada um bipa e explode
 * em sequência (content/GelEntidade). Parâmetros: range, radius (raio do buraco), damage_mult, max_placed.
 */
public final class Gel implements TipoHabilidade {
    @Override
    public boolean gratis(ServerPlayer player, Personagem.Habilidade h) {
        return detonando(player, h);
    }

    private static boolean detonando(ServerPlayer player, Personagem.Habilidade h) {
        if (GelEntidade.level(player).isEmpty()) return false;
        return player.isShiftKeyDown() || mira(player, h).getType() == HitResult.Type.MISS;
    }

    private static BlockHitResult mira(ServerPlayer player, Personagem.Habilidade h) {
        return Alvos.mirarBloco(player, h.params().d("range", 6), ClipContext.Fluid.NONE);
    }

    @Override
    public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        Params prm = h.params();
        if (detonando(player, h)) {
            if (GelEntidade.detonar(player) == 0) return false;
            player.swing(InteractionHand.OFF_HAND, true);
            Guia.concluir(player, "gel_detonar");
            return true;
        }
        BlockHitResult hit = mira(player, h);
        if (hit.getType() == HitResult.Type.MISS || player.level().getBlockState(hit.getBlockPos()).isAir()) return false;
        List<GelEntidade> colocados = GelEntidade.level(player);
        int max = prm.i("max_placed", 3);
        // Passou do máximo: o mais antigo (o mais longe) se desfaz para dar lugar ao novo.
        if (colocados.size() >= max) colocados.get(colocados.size() - 1).discard();
        GelEntidade g = new GelEntidade(player, hit.getBlockPos(), hit.getDirection(), hit.getLocation(), prm.f("radius", 2.5f),
            Alvos.dano(p, prm.d("damage_mult", 2.0)));
        player.level().addFreshEntity(g);
        player.level().playSound(null, g.getX(), g.getY(), g.getZ(), Sons.GEL_APLICA.get(), SoundSource.PLAYERS, 0.9f,
            0.9f + player.getRandom().nextFloat() * 0.2f);
        player.swing(InteractionHand.MAIN_HAND, true);
        Guia.concluir(player, "gel");
        return true;
    }
}
