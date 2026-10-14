package com.danrod.dcuniverse.character;

import com.danrod.dcuniverse.ability.DemoHud;
import com.danrod.dcuniverse.ability.Passivas;
import com.danrod.dcuniverse.ability.QuedaAmortecida;
import com.danrod.dcuniverse.guia.Guia;
import com.danrod.dcuniverse.core.Config;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.suit.Traje;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Transformar e destransformar (seção 2.4 e decisões 3, 4 e 5). Tudo roda no servidor. */
public final class Transformacao {
    /** Duração da destransformação, em ticks (decisão 5). */
    public static final int TEMPO_DESTRANSFORMAR = 20;
    /** Distância (blocos) em que outro jogador vê a transformação e descobre o personagem no Guia. */
    private static final double VISTO_A = 48;

    private Transformacao() {}

    /** Uso do item de transformação. */
    public static void usarItem(ServerPlayer player, Identifier personagem) {
        EstadoJogador estado = EstadoJogador.de(player);
        if (personagem.equals(estado.personagemId())) {
            pedirDestransformar(player);
        } else {
            transformar(player, personagem, false);
        }
    }

    /**
     * Transforma no personagem. Troca direta de um personagem para outro é permitida.
     * @param forcar ignora a espera entre transformações (comandos de admin e testes)
     */
    public static boolean transformar(ServerPlayer player, Identifier id, boolean forcar) {
        Personagem p = Personagens.get(id);
        if (p == null) {
            avisar(player, Component.translatable("dcuniverse.msg.personagem_desconhecido", id.toString()));
            return false;
        }
        EstadoJogador estado = EstadoJogador.de(player);
        long agora = player.level().getGameTime();
        long espera = estado.ultimaTransformacao() + Config.ESPERA_TRANSFORMACAO.get() * 20L - agora;
        if (!forcar && espera > 0) {
            avisar(player, Component.translatable("dcuniverse.msg.espera_transformacao", (espera + 19) / 20));
            return false;
        }
        if (estado.transformado()) destransformar(player);

        estado.setPersonagem(id);
        estado.setUltimaTransformacao(agora);
        estado.setSelecionada(0);
        estado.setEnergia((float) (p.energia().max() * Config.ENERGIA_INICIAL.get()));
        Traje.vestir(player, p);
        Atributos.aplicar(player, p, true);
        Passivas.aoTransformar(player, p);
        efeito(player, true);
        Rede.sincronizar(player);
        Rede.aparencia(player);
        Guia.descobrir(player, id);
        Guia.concluir(player, "transformar");
        // Quem está por perto e vê a transformação também descobre o personagem no Guia.
        for (ServerPlayer outro : player.level().players()) {
            if (outro != player && outro.distanceToSqr(player) < VISTO_A * VISTO_A) Guia.descobrir(outro, id);
        }
        return true;
    }

    /** Pede a destransformação, que termina depois de 1 s. Não vale durante uma habilidade canalizada. */
    public static void pedirDestransformar(ServerPlayer player) {
        EstadoJogador estado = EstadoJogador.de(player);
        if (!estado.transformado() || estado.destransformarEm() != 0L) return;
        if (!estado.canalizando().isEmpty()) {
            avisar(player, Component.translatable("dcuniverse.msg.canalizando"));
            return;
        }
        estado.setDestransformarEm(player.level().getGameTime() + TEMPO_DESTRANSFORMAR);
    }

    /** Destransforma na hora: tira o traje, devolve a armadura e remove os bônus. */
    public static void destransformar(ServerPlayer player) {
        EstadoJogador estado = EstadoJogador.de(player);
        Personagem p = estado.personagem();
        Passivas.aoDestransformar(player, p);
        DemoHud.parar(player);
        Traje.remover(player);
        Atributos.remover(player);
        estado.limparTransformacao();
        // Destransformar no ar não mata de queda: desce planando devagar (decisão 6).
        if (player.isAlive()) QuedaAmortecida.iniciarSeNoAr(player);
        efeito(player, false);
        Rede.sincronizar(player);
        Rede.aparencia(player);
    }

    /** Tick do servidor: conclui a destransformação pedida e mantém traje e atributos corretos. */
    public static void tick(ServerPlayer player) {
        EstadoJogador estado = EstadoJogador.de(player);
        if (!estado.transformado()) return;
        Personagem p = estado.personagem();
        if (p == null) {
            // O datapack que definia o personagem sumiu: volta ao normal sem perder a armadura.
            destransformar(player);
            return;
        }
        long fim = estado.destransformarEm();
        if (fim != 0L && player.level().getGameTime() >= fim) {
            destransformar(player);
            Guia.concluir(player, "destransformar");
            return;
        }
        Traje.verificar(player, p);
        if (!Atributos.temModificador(player)) Atributos.aplicar(player, p, false);
    }

    private static void efeito(ServerPlayer player, boolean entrando) {
        if (player.level() instanceof ServerLevel level && player.connection != null) {
            // A espiral de entrada é desenhada no cliente (client/Efeitos); na saída, uma nuvem.
            if (!entrando) level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1, player.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
            level.playSound(null, player.blockPosition(),
                entrando ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6f, 1.4f);
        }
    }

    public static void avisar(ServerPlayer player, Component msg) {
        if (player.connection != null) player.sendOverlayMessage(msg);
    }
}
