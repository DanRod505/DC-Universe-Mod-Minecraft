package com.danrod.dcuniverse.npc;

import com.danrod.dcuniverse.core.Persistente;
import com.danrod.dcuniverse.guia.Guia;
import com.danrod.dcuniverse.missao.Diario;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;

import java.util.UUID;

/**
 * Mascotes do mod (Krypto, Streaky): um de cada por jogador. O dono guarda o UUID do mascote; quando ele morre
 * longe do dono, o mundo marca o UUID como morto (Residentes) e o jogador pode domar outro.
 */
public final class Mascotes {
    private static final String RAIZ = "dcuniverse_mascotes";

    private Mascotes() {}

    /** O jogador já tem um mascote deste tipo vivo? */
    public static boolean tem(ServerPlayer p, String tipo) {
        String u = Persistente.ler(p, RAIZ).getStringOr(tipo, "");
        if (u.isEmpty()) return false;
        UUID id = UUID.fromString(u);
        if (Residentes.de((ServerLevel) p.level()).morto(id)) {
            esquecer(p, tipo);
            return false;
        }
        return true;
    }

    /** Recusa a doma (e avisa) quando o jogador já tem um mascote deste tipo. */
    public static boolean podeDomar(ServerPlayer p, String tipo) {
        if (!tem(p, tipo)) return true;
        if (p.connection != null) p.sendOverlayMessage(Component.translatable("dcuniverse.mascote.ja_tem", Component.translatable("entity.dcuniverse." + tipo)));
        return false;
    }

    /** Chamado quando o mascote ganha dono: registra, tira da lista de moradores e marca Guia e missões. */
    public static void domou(TamableAnimal pet, ServerPlayer p, String tipo) {
        CompoundTag g = Persistente.ler(p, RAIZ);
        g.putString(tipo, pet.getUUID().toString());
        Persistente.salvar(p, RAIZ, g);
        pet.setPersistenceRequired();
        pet.clearHome();
        if (pet instanceof Residentes.Residente r) {
            Residentes.saiu((ServerLevel) p.level(), r.morador());
            r.morador("");
        }
        Guia.concluir(p, "domar_" + tipo);
        Diario.domou(p, pet);
    }

    /** O mascote morreu: o dono pode domar outro. */
    public static void morreu(TamableAnimal pet, String tipo) {
        if (!(pet.level() instanceof ServerLevel level)) return;
        if (pet instanceof Residentes.Residente r) Residentes.saiu(level, r.morador());
        if (!pet.isTame()) return;
        if (pet.getOwner() instanceof ServerPlayer dono) esquecer(dono, tipo);
        else Residentes.de(level).marcarMorto(pet.getUUID());
    }

    private static void esquecer(ServerPlayer p, String tipo) {
        CompoundTag g = Persistente.ler(p, RAIZ);
        g.remove(tipo);
        Persistente.salvar(p, RAIZ, g);
    }
}
