package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.content.BatwingEntidade;
import com.danrod.dcuniverse.content.VeiculoBat;
import com.danrod.dcuniverse.content.VeiculoDono;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * vehicle: veículo da suprema (seção 2.11). O parâmetro "model" diz qual: batwing (padrão, content/BatwingEntidade),
 * batmovel, batmoto ou batbarco (content/VeiculoBat). Um por dono: usar de novo com ele no mundo chama o veículo
 * (perto, embarca; o Batmóvel e a Batmoto vêm buscar quem está longe) e não gasta a suprema. Com o cinto, o
 * jogador escolhe no Batcomputador qual veículo a suprema chama (character/Cinto).
 */
public final class Veiculo implements TipoHabilidade {
    private static final Map<UUID, Entity> ATIVOS = new ConcurrentHashMap<>();

    /** Chamar de volta o veículo que já está no mundo não espera a recarga nem gasta energia. */
    @Override
    public boolean gratis(ServerPlayer player, Personagem.Habilidade h) {
        Entity atual = ATIVOS.get(player.getUUID());
        return atual != null && atual.isAlive() && atual instanceof VeiculoDono v && v.estado() != VeiculoDono.INDO
            && h.params().s("model", "batwing").equals(modelo(atual));
    }

    @Override
    public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        Entity atual = ATIVOS.get(player.getUUID());
        VeiculoDono antigo = null;
        if (atual != null && atual.isAlive() && atual instanceof VeiculoDono v && v.estado() != VeiculoDono.INDO) {
            String modelo = h.params().s("model", "batwing");
            if (modelo.equals(modelo(atual))) {
                v.chamar(player);
                return false;
            }
            antigo = v;
        }
        Entity novo = invocar(player, h);
        if (novo == null) return false;
        // Trocou de veículo no Batcomputador: o antigo vai embora quando o novo chega (sem água, o Batbarco não vem e o antigo fica).
        if (antigo != null) antigo.irEmbora();
        ATIVOS.put(player.getUUID(), novo);
        return true;
    }

    private static Entity invocar(ServerPlayer player, Personagem.Habilidade h) {
        String modelo = h.params().s("model", "batwing");
        if (modelo.equals("batwing")) return BatwingEntidade.invocar(player, h.params());
        if (modelo.equals("nave_kryptoniana")) return com.danrod.dcuniverse.content.NaveKryptoniana.invocar(player, h.params());
        VeiculoBat v = VeiculoBat.invocar(player, modelo, h.params());
        if (v == null && modelo.equals("batbarco")) {
            com.danrod.dcuniverse.character.Transformacao.avisar(player, Component.translatable("dcuniverse.msg.batbarco_sem_agua"));
        }
        return v;
    }

    private static String modelo(Entity e) {
        if (e instanceof com.danrod.dcuniverse.content.NaveKryptoniana) return "nave_kryptoniana";
        return e instanceof VeiculoBat v ? v.modelo() : "batwing";
    }

    /** O veículo do jogador no mundo (qualquer um), ou null. */
    public static Entity de(ServerPlayer player) {
        Entity e = ATIVOS.get(player.getUUID());
        return e != null && e.isAlive() ? e : null;
    }

    public static void aoDestransformar(ServerPlayer player) {
        Entity e = ATIVOS.remove(player.getUUID());
        if (e != null && e.isAlive() && e instanceof VeiculoDono v) v.irEmbora();
    }

    public static void limpar() {
        ATIVOS.clear();
    }
}
