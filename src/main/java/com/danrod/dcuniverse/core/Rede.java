package com.danrod.dcuniverse.core;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.character.Personagens;
import com.danrod.dcuniverse.character.Transformacao;
import com.danrod.dcuniverse.ability.DemoHud;
import com.danrod.dcuniverse.ability.Habilidades;
import com.danrod.dcuniverse.energy.Recargas;
import com.danrod.dcuniverse.guia.Guia;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Pacotes do mod. O servidor decide tudo; o cliente só pede (Selecionar, Usar, Destransformar) e desenha (Sync, Aparencia, Catalogo, GuiaSync). */
public final class Rede {
    private static final int VERSAO = 4;
    public static final SimpleChannel CANAL = ChannelBuilder
        .named(Identifier.fromNamespaceAndPath(DCUniverse.MODID, "main"))
        .networkProtocolVersion(VERSAO)
        .simpleChannel();

    /** Uma habilidade como o cliente precisa ver: menu radial e HUD. */
    public record HabInfo(String id, boolean hold, boolean suprema, float custo, int restante, int recargaTotal) {}

    /**
     * Estado do próprio jogador. amortecendo vale também fora da transformação (queda amortecida depois de
     * destransformar no ar); planar é a velocidade do planar do personagem (0 = não plana). recarga é quanto
     * a energia sobe por segundo agora; solar diz se o personagem recarrega no sol e sol se ele está no sol.
     */
    public record Sync(String personagem, float energia, float max, boolean esgotado, int selecionada,
                       String canalizando, List<HabInfo> habilidades, float planar, boolean amortecendo,
                       float recarga, boolean solar, boolean sol) {
        public static final Sync VAZIO = new Sync("", 0, 0, false, 0, "", List.of(), 0, false, 0, false, false);

        /** A habilidade pode ser usada agora: sem recarga pendente e com energia para o custo. */
        public boolean pronta(HabInfo h) {
            return h.restante() <= 0 && !esgotado && energia >= h.custo();
        }

        public static void encode(Sync m, FriendlyByteBuf b) {
            b.writeUtf(m.personagem);
            b.writeFloat(m.energia);
            b.writeFloat(m.max);
            b.writeBoolean(m.esgotado);
            b.writeVarInt(m.selecionada);
            b.writeUtf(m.canalizando);
            b.writeVarInt(m.habilidades.size());
            for (HabInfo h : m.habilidades) {
                b.writeUtf(h.id);
                b.writeBoolean(h.hold);
                b.writeBoolean(h.suprema);
                b.writeFloat(h.custo);
                b.writeVarInt(h.restante);
                b.writeVarInt(h.recargaTotal);
            }
            b.writeFloat(m.planar);
            b.writeBoolean(m.amortecendo);
            b.writeFloat(m.recarga);
            b.writeBoolean(m.solar);
            b.writeBoolean(m.sol);
        }

        public static Sync decode(FriendlyByteBuf b) {
            String p = b.readUtf();
            float e = b.readFloat(), max = b.readFloat();
            boolean esg = b.readBoolean();
            int sel = b.readVarInt();
            String can = b.readUtf();
            int n = b.readVarInt();
            List<HabInfo> habs = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                habs.add(new HabInfo(b.readUtf(), b.readBoolean(), b.readBoolean(), b.readFloat(), b.readVarInt(), b.readVarInt()));
            }
            return new Sync(p, e, max, esg, sel, can, List.copyOf(habs), b.readFloat(), b.readBoolean(),
                b.readFloat(), b.readBoolean(), b.readBoolean());
        }
    }

    /**
     * Aparência de um jogador para quem está vendo ele: personagem ("" = normal), a cor do personagem e o
     * efeito da habilidade canalizada agora (tipo, "" = nenhuma), com cor, alcance e ângulo, para todo mundo
     * ver o raio ou o sopro.
     */
    public record Aparencia(int entidade, String personagem, int cor, String efeito, int efeitoCor, float alcance, float angulo) {
        public static void encode(Aparencia m, FriendlyByteBuf b) {
            b.writeVarInt(m.entidade);
            b.writeUtf(m.personagem);
            b.writeInt(m.cor);
            b.writeUtf(m.efeito);
            b.writeInt(m.efeitoCor);
            b.writeFloat(m.alcance);
            b.writeFloat(m.angulo);
        }

        public static Aparencia decode(FriendlyByteBuf b) {
            return new Aparencia(b.readVarInt(), b.readUtf(), b.readInt(), b.readUtf(), b.readInt(), b.readFloat(), b.readFloat());
        }
    }

    /** Os personagens do datapack (JSON cru), para o Guia mostrar fichas e habilidades iguais às do servidor. */
    public record Catalogo(Map<String, String> personagens) {
        public static void encode(Catalogo m, FriendlyByteBuf b) {
            b.writeMap(m.personagens, FriendlyByteBuf::writeUtf, (buf, s) -> buf.writeUtf(s, 262144));
        }

        public static Catalogo decode(FriendlyByteBuf b) {
            return new Catalogo(Map.copyOf(b.readMap(FriendlyByteBuf::readUtf, buf -> buf.readUtf(262144))));
        }
    }

    /** Progresso do jogador no Guia: tarefas concluídas de Primeiros Passos e personagens descobertos. */
    public record GuiaSync(List<String> tarefas, List<String> descobertos, String novaTarefa) {
        public static final GuiaSync VAZIO = new GuiaSync(List.of(), List.of(), "");

        public static void encode(GuiaSync m, FriendlyByteBuf b) {
            b.writeCollection(m.tarefas, FriendlyByteBuf::writeUtf);
            b.writeCollection(m.descobertos, FriendlyByteBuf::writeUtf);
            b.writeUtf(m.novaTarefa);
        }

        public static GuiaSync decode(FriendlyByteBuf b) {
            return new GuiaSync(List.copyOf(b.readList(FriendlyByteBuf::readUtf)), List.copyOf(b.readList(FriendlyByteBuf::readUtf)), b.readUtf());
        }
    }

    /** Escolha no menu radial (índice na lista de habilidades). */
    public record Selecionar(int indice) {}

    /** Tecla de uso apertada (true) ou solta (false). */
    public record Usar(boolean pressionado) {}

    public record Destransformar() {}

    private Rede() {}

    public static void registrar() {
        CANAL.messageBuilder(Sync.class)
            .direction(PacketFlow.CLIENTBOUND)
            .encoder(Sync::encode).decoder(Sync::decode)
            .consumerMainThread((m, ctx) -> EstadoCliente.atual = m)
            .add();
        CANAL.messageBuilder(Aparencia.class)
            .direction(PacketFlow.CLIENTBOUND)
            .encoder(Aparencia::encode).decoder(Aparencia::decode)
            .consumerMainThread((m, ctx) -> EstadoCliente.aparencia(m))
            .add();
        CANAL.messageBuilder(Catalogo.class)
            .direction(PacketFlow.CLIENTBOUND)
            .encoder(Catalogo::encode).decoder(Catalogo::decode)
            .consumerMainThread((m, ctx) -> EstadoCliente.catalogo(m))
            .add();
        CANAL.messageBuilder(GuiaSync.class)
            .direction(PacketFlow.CLIENTBOUND)
            .encoder(GuiaSync::encode).decoder(GuiaSync::decode)
            .consumerMainThread((m, ctx) -> EstadoCliente.guia(m))
            .add();
        CANAL.messageBuilder(Selecionar.class)
            .direction(PacketFlow.SERVERBOUND)
            .encoder((m, b) -> b.writeVarInt(m.indice())).decoder(b -> new Selecionar(b.readVarInt()))
            .consumerMainThread((m, ctx) -> comJogador(ctx, p -> Habilidades.selecionar(p, m.indice())))
            .add();
        CANAL.messageBuilder(Usar.class)
            .direction(PacketFlow.SERVERBOUND)
            .encoder((m, b) -> b.writeBoolean(m.pressionado())).decoder(b -> new Usar(b.readBoolean()))
            .consumerMainThread((m, ctx) -> comJogador(ctx, p -> Habilidades.usar(p, m.pressionado())))
            .add();
        CANAL.messageBuilder(Destransformar.class)
            .direction(PacketFlow.SERVERBOUND)
            .encoder((m, b) -> {}).decoder(b -> new Destransformar())
            .consumerMainThread((m, ctx) -> comJogador(ctx, Transformacao::pedirDestransformar))
            .add();
    }

    private static void comJogador(CustomPayloadEvent.Context ctx, Consumer<ServerPlayer> acao) {
        ServerPlayer p = ctx.getSender();
        if (p != null) acao.accept(p);
    }

    /** Avisa o próprio jogador e quem está vendo ele que a skin mudou. Jogadores sem conexão (testes) são ignorados. */
    public static void aparencia(ServerPlayer player) {
        if (player.connection == null) return;
        CANAL.send(montarAparencia(player), PacketDistributor.TRACKING_ENTITY_AND_SELF.with(player));
    }

    /** Manda a aparência de alvo só para quem começou a ver ele. */
    public static void aparenciaPara(ServerPlayer quemVe, ServerPlayer alvo) {
        if (quemVe.connection == null) return;
        CANAL.send(montarAparencia(alvo), PacketDistributor.PLAYER.with(quemVe));
    }

    public static Aparencia montarAparencia(ServerPlayer player) {
        EstadoJogador e = EstadoJogador.de(player);
        Personagem p = e.personagem();
        if (p == null) return new Aparencia(player.getId(), "", 0, "", 0, 0, 0);
        Personagem.Habilidade h = e.canalizando().isEmpty() ? null : p.habilidade(e.canalizando());
        if (h == null) return new Aparencia(player.getId(), p.id().toString(), p.cor(), "", 0, 0, 0);
        return new Aparencia(player.getId(), p.id().toString(), p.cor(), h.tipo(), h.params().color("color", 0xFFFFFF),
            h.params().f("range", 8), h.params().f("angle", 30));
    }

    /** Envia o estado completo ao próprio jogador. */
    public static void sincronizar(ServerPlayer player) {
        if (player.connection == null) return;
        CANAL.send(montar(player), PacketDistributor.PLAYER.with(player));
    }

    public static Sync montar(ServerPlayer player) {
        EstadoJogador e = EstadoJogador.de(player);
        Personagem p = e.personagem();
        if (p == null) return e.amortecendo() ? new Sync("", 0, 0, false, 0, "", List.of(), 0, true, 0, false, false) : Sync.VAZIO;
        long agora = player.level().getGameTime();
        List<HabInfo> habs = new ArrayList<>();
        for (Personagem.Habilidade h : p.habilidades()) {
            int restante = (int) Math.max(0, e.prontaEm(h.id()) - agora);
            habs.add(new HabInfo(h.id(), h.modo() == Personagem.Modo.HOLD, h.suprema(), h.custo(), restante, Math.round(h.recargaSeg() * 20)));
        }
        int selecionada = e.selecionada();
        if (DemoHud.ativa(player)) {
            selecionada = DemoHud.adicionar(player, p, habs);
        }
        Personagem.Tipo planar = p.passiva("glide");
        boolean solar = p.energia().recargas().stream().anyMatch(r -> r.tipo().equals("sunlight"));
        return new Sync(p.id().toString(), e.energia(), p.energia().max(), e.esgotado(), selecionada,
            e.canalizando(), List.copyOf(habs), planar == null ? 0f : planar.params().f("speed", 10f), e.amortecendo(),
            Recargas.porSegundo(player, p), solar, solar && Recargas.sobOSol(player));
    }

    public static void catalogo(ServerPlayer player) {
        if (player.connection == null) return;
        CANAL.send(new Catalogo(Personagens.jsons()), PacketDistributor.PLAYER.with(player));
    }

    public static void guia(ServerPlayer player, String novaTarefa) {
        if (player.connection == null) return;
        CANAL.send(Guia.montar(player, novaTarefa), PacketDistributor.PLAYER.with(player));
    }
}
