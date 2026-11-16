package com.danrod.dcuniverse.ability;

import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.core.Canal;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.energy.Energia;
import net.minecraft.server.level.ServerPlayer;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Habilidade como cena: o JSON da habilidade pode pedir uma animação ("anim", tocada em todo mundo que vê o jogador)
 * e um tempo de preparo ("windup", segundos) antes do efeito de verdade. A animação traz a coreografia no tempo
 * (câmera, luz, som e partículas, ver client/Cena); aqui o servidor só segura o efeito até o momento certo, para o
 * soco sair quando o punho chega e o sopro quando o peito está cheio.
 * <p>
 * Instantânea: a energia e a recarga são cobradas no aperto (não dá para apertar de novo no preparo); se no fim do
 * preparo o efeito não puder sair, devolve. Segurada: o preparo começa no aperto e a canalização só liga depois dele,
 * com a animação em laço ("loop_anim"); soltar antes cancela. "end_anim" toca ao soltar.
 */
public final class Coreografia {
    private record Preparo(ServerPlayer jogador, Personagem p, Personagem.Habilidade h, long quando, boolean segurar, float pago) {}

    private static final Map<UUID, Preparo> PREPAROS = new ConcurrentHashMap<>();

    private Coreografia() {}

    public static float preparo(Personagem.Habilidade h) {
        return h.params().f("windup", 0);
    }

    /** O jogador está no meio do preparo de alguma habilidade. */
    public static boolean emCena(ServerPlayer player) {
        return PREPAROS.containsKey(player.getUUID());
    }

    /** Está preparando esta habilidade segurada. */
    public static boolean preparando(ServerPlayer player, String id) {
        Preparo pr = PREPAROS.get(player.getUUID());
        return pr != null && pr.h.id().equals(id);
    }

    /** Toca a animação no jogador para quem o vê e para ele mesmo ("" solta a que está tocando). */
    public static void animar(ServerPlayer player, String anim) {
        if (player.connection == null) return;
        Rede.CANAL.send(new Rede.GolpeVisto(player.getId(), anim, true), Canal.rastreandoEProprio(player));
    }

    /** Começa o preparo; o efeito sai em tick() quando o tempo chegar. pago: energia já cobrada (devolvida se falhar). */
    static void comecar(ServerPlayer player, Personagem p, Personagem.Habilidade h, boolean segurar, float pago) {
        long quando = player.level().getGameTime() + Math.max(1, Math.round(preparo(h) * 20));
        PREPAROS.put(player.getUUID(), new Preparo(player, p, h, quando, segurar, pago));
        String anim = h.params().s("anim", "");
        if (!anim.isEmpty()) animar(player, anim);
        TipoHabilidade tipo = TiposHabilidade.TIPOS.get(h.tipo());
        if (tipo != null) tipo.preparar(player, p, h, preparo(h));
    }

    /** Soltou a tecla da habilidade segurada antes do fim do preparo: cancela e volta à pose normal. */
    static void cancelar(ServerPlayer player) {
        Preparo pr = PREPAROS.remove(player.getUUID());
        if (pr == null) return;
        String fim = pr.h.params().s("end_anim", "");
        animar(player, fim);
    }

    public static void tick() {
        for (Iterator<Preparo> it = PREPAROS.values().iterator(); it.hasNext(); ) {
            Preparo pr = it.next();
            ServerPlayer pl = pr.jogador;
            if (pl.isRemoved() || EstadoJogador.de(pl).personagem() != pr.p) {
                it.remove();
                continue;
            }
            pl.resetFallDistance();
            if (pl.level().getGameTime() < pr.quando) continue;
            it.remove();
            soltar(pr);
        }
    }

    /** Leva todos os preparos do jogador ao fim agora (testes). */
    public static void adiantar(ServerPlayer player) {
        Preparo pr = PREPAROS.remove(player.getUUID());
        if (pr != null) soltar(pr);
    }

    private static void soltar(Preparo pr) {
        ServerPlayer pl = pr.jogador;
        if (pr.segurar) {
            Habilidades.canalizarJa(pl, pr.h);
            String laco = pr.h.params().s("loop_anim", "");
            if (!laco.isEmpty()) animar(pl, laco);
            return;
        }
        TipoHabilidade tipo = TiposHabilidade.TIPOS.get(pr.h.tipo());
        if (tipo != null && tipo.usar(pl, pr.p, pr.h)) return;
        // Não deu (perdeu o chão, saiu do mundo...): devolve a energia e a recarga.
        if (pr.pago > 0) Energia.adicionar(pl, pr.p, pr.pago);
        EstadoJogador.de(pl).setProntaEm(pr.h.id(), 0);
        animar(pl, "");
        Rede.sincronizar(pl);
    }

    public static void esquecer(ServerPlayer player) {
        PREPAROS.remove(player.getUUID());
    }

    public static void limpar() {
        PREPAROS.clear();
    }
}
