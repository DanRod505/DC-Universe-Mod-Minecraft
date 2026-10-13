package com.danrod.dcuniverse.core;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.ability.Habilidades;
import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.character.Transformacao;
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

/** Pacotes do mod. O servidor decide tudo; o cliente só pede (Selecionar, Usar, Destransformar) e desenha (Sync). */
public final class Rede {
    private static final int VERSAO = 1;
    public static final SimpleChannel CANAL = ChannelBuilder
        .named(Identifier.fromNamespaceAndPath(DCUniverse.MODID, "main"))
        .networkProtocolVersion(VERSAO)
        .simpleChannel();

    /** Uma habilidade como o cliente precisa ver: menu radial e HUD. */
    public record HabInfo(String id, boolean hold, float custo, int restante, int recargaTotal) {}

    public record Sync(String personagem, float energia, float max, boolean esgotado, int selecionada,
                       String canalizando, List<HabInfo> habilidades, float planar) {
        public static final Sync VAZIO = new Sync("", 0, 0, false, 0, "", List.of(), 0);

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
                b.writeFloat(h.custo);
                b.writeVarInt(h.restante);
                b.writeVarInt(h.recargaTotal);
            }
            b.writeFloat(m.planar);
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
                habs.add(new HabInfo(b.readUtf(), b.readBoolean(), b.readFloat(), b.readVarInt(), b.readVarInt()));
            }
            return new Sync(p, e, max, esg, sel, can, List.copyOf(habs), b.readFloat());
        }
    }

    public record Selecionar(int indice) {}

    public record Usar(boolean pressionado) {}

    public record Destransformar() {}

    private Rede() {}

    public static void registrar() {
        CANAL.messageBuilder(Sync.class)
            .direction(PacketFlow.CLIENTBOUND)
            .encoder(Sync::encode).decoder(Sync::decode)
            .consumerMainThread((m, ctx) -> EstadoCliente.atual = m)
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

    private static void comJogador(CustomPayloadEvent.Context ctx, java.util.function.Consumer<ServerPlayer> acao) {
        ServerPlayer p = ctx.getSender();
        if (p != null) acao.accept(p);
    }

    /** Envia o estado completo ao próprio jogador. Jogadores sem conexão (testes) são ignorados. */
    public static void sincronizar(ServerPlayer player) {
        if (player.connection == null) return;
        CANAL.send(montar(player), PacketDistributor.PLAYER.with(player));
    }

    public static Sync montar(ServerPlayer player) {
        EstadoJogador e = EstadoJogador.de(player);
        Personagem p = e.personagem();
        if (p == null) return Sync.VAZIO;
        long agora = player.level().getGameTime();
        List<HabInfo> habs = new ArrayList<>();
        for (Personagem.Habilidade h : p.habilidades()) {
            int restante = (int) Math.max(0, e.prontaEm(h.id()) - agora);
            habs.add(new HabInfo(h.id(), h.modo() == Personagem.Modo.HOLD, h.custo(), restante, Math.round(h.recargaSeg() * 20)));
        }
        Personagem.Tipo planar = p.passiva("glide");
        return new Sync(p.id().toString(), e.energia(), p.energia().max(), e.esgotado(), e.selecionada(),
            e.canalizando(), List.copyOf(habs), planar == null ? 0f : planar.params().f("speed", 10f));
    }
}
