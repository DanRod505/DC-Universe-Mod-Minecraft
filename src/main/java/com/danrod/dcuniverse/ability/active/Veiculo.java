package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.content.BatwingEntidade;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * vehicle: veículo da suprema (seção 2.11), por enquanto o Batwing. Um por dono: usar de novo com ele no mundo
 * chama o veículo de volta (e não gasta a suprema). Parâmetros do veículo em content/BatwingEntidade:
 * duration, missiles, missile_reload, missile_damage, missile_radius, gun_damage, gun_range, speed, boost_speed.
 */
public final class Veiculo implements TipoHabilidade {
    private static final Map<UUID, BatwingEntidade> ATIVOS = new ConcurrentHashMap<>();

    @Override
    public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        BatwingEntidade atual = ATIVOS.get(player.getUUID());
        if (atual != null && atual.isAlive() && atual.estado() != BatwingEntidade.INDO) {
            // Já tem um: ele vem buscar o dono (se estiver perto, embarca).
            if (atual.distanceTo(player) < 8) atual.embarcar(player);
            return false;
        }
        ATIVOS.put(player.getUUID(), BatwingEntidade.invocar(player, h.params()));
        return true;
    }

    public static BatwingEntidade de(ServerPlayer player) {
        BatwingEntidade b = ATIVOS.get(player.getUUID());
        return b != null && b.isAlive() ? b : null;
    }

    public static void aoDestransformar(ServerPlayer player) {
        BatwingEntidade b = ATIVOS.remove(player.getUUID());
        if (b != null && b.isAlive()) b.irEmbora();
    }

    public static void limpar() {
        ATIVOS.clear();
    }
}
