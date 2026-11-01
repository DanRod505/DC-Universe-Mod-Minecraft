package com.danrod.dcuniverse.core;

import com.danrod.dcuniverse.DCUniverse;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Canal de pacotes do mod sobre os payloads do NeoForge. Os pacotes continuam records simples com encoder e
 * decoder (como no SimpleChannel do Forge): cada classe vira um tipo dcuniverse:&lt;nome&gt; embrulhado em {@link Envelope}.
 */
public final class Canal {
    /** O pacote embrulhado para o NeoForge. */
    public record Envelope(CustomPacketPayload.Type<Envelope> type, Object msg) implements CustomPacketPayload {}

    /** Quem mandou o pacote (null quando vem do servidor). */
    public record Contexto(ServerPlayer getSender) {}

    private record Registro<T>(Class<T> classe, PacketFlow direcao, BiConsumer<T, FriendlyByteBuf> encoder,
                               Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Contexto> consumidor) {}

    /** Para onde vai o pacote: Canal.SERVIDOR, jogador(p), rastreando(e)... */
    public interface Alvo extends Consumer<CustomPacketPayload> {}

    /** Do cliente para o servidor (só chamar no cliente). */
    public static final Alvo SERVIDOR = p -> net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(p);

    public static Alvo jogador(ServerPlayer player) {
        return p -> PacketDistributor.sendToPlayer(player, p);
    }

    public static Alvo rastreando(Entity e) {
        return p -> PacketDistributor.sendToPlayersTrackingEntity(e, p);
    }

    public static Alvo rastreandoEProprio(Entity e) {
        return p -> PacketDistributor.sendToPlayersTrackingEntityAndSelf(e, p);
    }

    public static Alvo perto(ServerLevel level, double x, double y, double z, double raio) {
        return p -> PacketDistributor.sendToPlayersNear(level, null, x, y, z, raio, p);
    }

    private final String versao;
    private final List<Registro<?>> registros = new ArrayList<>();
    private final Map<Class<?>, CustomPacketPayload.Type<Envelope>> tipos = new HashMap<>();

    public Canal(int versao) {
        this.versao = Integer.toString(versao);
    }

    public <T> Construtor<T> messageBuilder(Class<T> classe) {
        return new Construtor<>(classe);
    }

    public void send(Object msg, Alvo alvo) {
        CustomPacketPayload.Type<Envelope> tipo = tipos.get(msg.getClass());
        if (tipo == null) throw new IllegalArgumentException("Pacote não registrado: " + msg.getClass());
        alvo.accept(new Envelope(tipo, msg));
    }

    /** O pacote de jogo que leva a mensagem ao cliente (para quem grava pacotes por conta própria, como o Flashback). */
    public net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket pacoteCliente(Object msg) {
        CustomPacketPayload.Type<Envelope> tipo = tipos.get(msg.getClass());
        if (tipo == null) throw new IllegalArgumentException("Pacote não registrado: " + msg.getClass());
        return new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(new Envelope(tipo, msg));
    }

    /** Entrega os pacotes ao NeoForge (barramento do mod). */
    public void registrar(RegisterPayloadHandlersEvent e) {
        PayloadRegistrar r = e.registrar(versao);
        for (Registro<?> reg : registros) adicionar(r, reg);
    }

    private <T> void adicionar(PayloadRegistrar r, Registro<T> reg) {
        CustomPacketPayload.Type<Envelope> tipo = tipos.get(reg.classe());
        StreamCodec<RegistryFriendlyByteBuf, Envelope> codec = StreamCodec.of(
            (buf, env) -> reg.encoder().accept(reg.classe().cast(env.msg()), buf),
            buf -> new Envelope(tipo, reg.decoder().apply(buf)));
        if (reg.direcao() == PacketFlow.CLIENTBOUND) {
            r.playToClient(tipo, codec, (env, ctx) -> reg.consumidor().accept(reg.classe().cast(env.msg()), new Contexto(null)));
        } else {
            r.playToServer(tipo, codec, (env, ctx) -> reg.consumidor().accept(reg.classe().cast(env.msg()),
                new Contexto(ctx.player() instanceof ServerPlayer sp ? sp : null)));
        }
    }

    public final class Construtor<T> {
        private final Class<T> classe;
        private PacketFlow direcao = PacketFlow.CLIENTBOUND;
        private BiConsumer<T, FriendlyByteBuf> encoder;
        private Function<FriendlyByteBuf, T> decoder;
        private BiConsumer<T, Contexto> consumidor;

        private Construtor(Class<T> classe) {
            this.classe = classe;
        }

        public Construtor<T> direction(PacketFlow d) {
            direcao = d;
            return this;
        }

        public Construtor<T> encoder(BiConsumer<T, FriendlyByteBuf> e) {
            encoder = e;
            return this;
        }

        public Construtor<T> decoder(Function<FriendlyByteBuf, T> d) {
            decoder = d;
            return this;
        }

        public Construtor<T> consumerMainThread(BiConsumer<T, Contexto> c) {
            consumidor = c;
            return this;
        }

        public void add() {
            tipos.put(classe, new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DCUniverse.MODID, classe.getSimpleName().toLowerCase(Locale.ROOT))));
            registros.add(new Registro<>(classe, direcao, encoder, decoder, consumidor));
        }
    }
}
