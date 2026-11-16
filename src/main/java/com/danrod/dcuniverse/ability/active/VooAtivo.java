package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.ability.passive.Voo;
import com.danrod.dcuniverse.character.Personagem;
import net.minecraft.server.level.ServerPlayer;

/**
 * flight: a habilidade Voo da árvore (docs/plano/HABILIDADES.md). Num slot, ela libera o voo completo de sempre
 * (passiva flight, ligada pelo "unlocks_passive" do JSON): cruzeiro, impulso, manobras, rastro e trilha. A tecla do
 * slot decola e pousa; o pulo duplo no ar continua funcionando.
 */
public final class VooAtivo implements TipoHabilidade {
    @Override
    public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        return Voo.alternar(player);
    }
}
