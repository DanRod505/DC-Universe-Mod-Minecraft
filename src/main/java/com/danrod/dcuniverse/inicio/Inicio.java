package com.danrod.dcuniverse.inicio;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.character.Personagens;
import com.danrod.dcuniverse.content.TransformacaoItem;
import com.danrod.dcuniverse.core.Config;
import com.danrod.dcuniverse.core.Persistente;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.guia.Guia;
import com.danrod.dcuniverse.world.estrutura.Estruturas;
import com.danrod.dcuniverse.world.estrutura.Locais;
import com.danrod.dcuniverse.world.mundo.FonteMundoDC;
import com.danrod.dcuniverse.world.mundo.MapaMundo;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Início guiado (decisão 11): no primeiro login o jogador vê o mapa do mundo, escolhe onde começar (um local
 * pronto ou o ponto de nascimento) e o personagem inicial, e ganha o item de transformação dele. A escolha fica
 * nos dados persistentes do jogador; o servidor pode desligar tudo (inicioGuiado) ou só o item (itemInicial).
 * O mesmo mapa abre na tecla M, só para ver.
 */
public final class Inicio {
    private static final String RAIZ = "dcuniverse_inicio";
    /** "Começar onde estou": o ponto de nascimento do mundo. */
    public static final String NASCIMENTO = "";

    /** Posição de cada local pronto (centro da rua principal etc.), achada uma vez por servidor. */
    private static final Map<String, Optional<BlockPos>> PONTOS = new HashMap<>();

    private Inicio() {}

    public static boolean escolheu(ServerPlayer player) {
        return Persistente.ler(player, RAIZ).getBooleanOr("escolheu", false);
    }

    /** No login: abre a escolha para quem ainda não escolheu. */
    public static void entrou(ServerPlayer player) {
        if (Config.INICIO_GUIADO.get() && !escolheu(player)) abrir(player, true);
    }

    /** Manda o mapa ao cliente: {@code escolher} abre a tela de início, senão só o mapa (tecla M). */
    public static void abrir(ServerPlayer player, boolean escolher) {
        MinecraftServer server = player.level().getServer();
        ServerLevel mundo = server.overworld();
        List<Rede.Marca> marcas = marcas(mundo);
        List<Rede.Inicial> opcoes = escolher && Config.ITEM_INICIAL.get() ? personagens() : List.of();
        MapaMundo.de(mundo).thenAcceptAsync(m -> {
            if (player.hasDisconnected()) return;
            BlockPos aqui = player.level() == mundo ? player.blockPosition() : mundo.getRespawnData().pos();
            Rede.inicio(player, new Rede.InicioAbrir(escolher, m, marcas, aqui.getX(), aqui.getZ(), opcoes));
        }, server);
    }

    /** O jogador escolheu (pacote InicioEscolha). Vale uma vez; depois, só com /dcu inicio de um operador. */
    public static void escolher(ServerPlayer player, String local, String personagem) {
        if (!Config.INICIO_GUIADO.get() || escolheu(player)) return;
        ServerLevel mundo = player.level().getServer().overworld();
        Optional<BlockPos> destino = Optional.empty();
        if (!local.equals(NASCIMENTO)) {
            if (!Estruturas.LOCAIS.containsKey(local)) return;
            destino = ponto(mundo, local);
            if (destino.isEmpty()) {
                if (player.connection != null) player.sendSystemMessage(Component.translatable("dcuniverse.inicio.sem_local", Component.translatable("dcuniverse.local." + local)).withStyle(ChatFormatting.RED));
                return;
            }
        }
        ItemStack item = ItemStack.EMPTY;
        if (Config.ITEM_INICIAL.get()) {
            Rede.Inicial op = personagens().stream().filter(o -> o.personagem().equals(personagem)).findFirst().orElse(null);
            if (op == null) return;
            item = new ItemStack(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(op.item())));
        }
        CompoundTag g = new CompoundTag();
        g.putBoolean("escolheu", true);
        g.putString("local", local);
        g.putString("personagem", item.isEmpty() ? "" : personagem);
        Persistente.salvar(player, RAIZ, g);

        if (destino.isPresent()) {
            BlockPos p = destino.get();
            player.teleportTo(mundo, p.getX() + 0.5, p.getY(), p.getZ() + 0.5, Set.of(), player.getYRot(), 0, true);
            // Morrer leva de volta ao local escolhido (até o jogador dormir numa cama).
            player.setRespawnPosition(new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(mundo.dimension(), p, player.getYRot(), 0), true), false);
        }
        if (!item.isEmpty()) {
            if (item.getItem() instanceof TransformacaoItem t) Guia.descobrir(player, t.personagem());
            Component nome = item.getHoverName();
            if (!player.getInventory().add(item)) player.drop(item, false);
            if (player.connection != null) player.sendSystemMessage(Component.translatable("dcuniverse.inicio.ganhou", nome).withStyle(ChatFormatting.GOLD));
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.2f);
    }

    /** Apaga a escolha (comando de operador): o jogador escolhe de novo no próximo login ou agora, se estiver online. */
    public static void resetar(ServerPlayer player) {
        Persistente.salvar(player, RAIZ, new CompoundTag());
        if (Config.INICIO_GUIADO.get()) abrir(player, true);
    }

    /** No Mundo DC, o ponto de nascimento do mundo vai para a rua principal de Smallville (se ele caiu na cidade). */
    public static void ajustarNascimento(MinecraftServer server) {
        ServerLevel mundo = server.overworld();
        if (!(mundo.getChunkSource().getGenerator().getBiomeSource() instanceof FonteMundoDC)) return;
        BlockPos atual = mundo.getRespawnData().pos();
        Optional<BlockPos> centro = ponto(mundo, "smallville");
        if (centro.isEmpty() || centro.get().equals(atual) || centro.get().distSqr(atual) > 300 * 300) return;
        mundo.setRespawnData(LevelData.RespawnData.of(mundo.dimension(), centro.get(), mundo.getRespawnData().yaw(), 0));
        DCUniverse.LOGGER.info("Mundo DC: ponto de nascimento na rua principal de Smallville ({})", centro.get().toShortString());
    }

    public static void limpar() {
        PONTOS.clear();
        MapaMundo.limpar();
    }

    /** Personagens que dá para escolher: os que têm item de transformação registrado e ficha carregada, na ordem do registro. */
    public static List<Rede.Inicial> personagens() {
        List<Rede.Inicial> out = new ArrayList<>();
        for (var item : BuiltInRegistries.ITEM) {
            if (item instanceof TransformacaoItem t && Personagens.get(t.personagem()) != null)
                out.add(new Rede.Inicial(t.personagem().toString(), BuiltInRegistries.ITEM.getKey(item).toString()));
        }
        return out;
    }

    /** Onde o jogador aparece num local: o ponto "centro" dele (em Smallville, a rua principal). */
    private static Optional<BlockPos> ponto(ServerLevel mundo, String local) {
        return PONTOS.computeIfAbsent(local, l -> {
            BlockPos perto = mundo.getChunkSource().getGenerator().getBiomeSource() instanceof FonteMundoDC f
                ? f.regiao(l).map(r -> new BlockPos(r.x(), 0, r.z())).orElse(mundo.getRespawnData().pos())
                : mundo.getRespawnData().pos();
            return Locais.ponto(mundo, l, "centro", perto);
        });
    }

    /** Marcas do mapa: no Mundo DC, todas as regiões (os locais prontos onde nasceram); nos outros mundos, só os prontos achados. */
    private static List<Rede.Marca> marcas(ServerLevel mundo) {
        List<Rede.Marca> out = new ArrayList<>();
        if (mundo.getChunkSource().getGenerator().getBiomeSource() instanceof FonteMundoDC f) {
            for (var r : f.regioes()) {
                boolean pronto = Estruturas.LOCAIS.containsKey(r.local());
                BlockPos p = pronto ? ponto(mundo, r.local()).orElse(null) : null;
                out.add(p != null ? new Rede.Marca(r.local(), p.getX(), p.getZ(), true) : new Rede.Marca(r.local(), r.x(), r.z(), false));
            }
            return out;
        }
        for (String l : Estruturas.LOCAIS.keySet()) ponto(mundo, l).ifPresent(p -> out.add(new Rede.Marca(l, p.getX(), p.getZ(), true)));
        return out;
    }
}
