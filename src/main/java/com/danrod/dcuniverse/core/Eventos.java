package com.danrod.dcuniverse.core;

import com.danrod.dcuniverse.ability.DemoHud;
import com.danrod.dcuniverse.ability.Habilidades;
import com.danrod.dcuniverse.ability.Passivas;
import com.danrod.dcuniverse.ability.QuedaAmortecida;
import com.danrod.dcuniverse.ability.active.Fortaleza;
import com.danrod.dcuniverse.ability.active.Onda;
import com.danrod.dcuniverse.ability.passive.Naturais;
import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.character.Personagens;
import com.danrod.dcuniverse.character.Transformacao;
import com.danrod.dcuniverse.energy.Energia;
import com.danrod.dcuniverse.guia.Guia;
import com.danrod.dcuniverse.suit.Traje;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.EventPriority;

import java.util.function.Consumer;
import java.util.function.Predicate;
import com.danrod.dcuniverse.DCUniverse;
import net.neoforged.neoforge.common.NeoForge;

/** Eventos do lado do servidor (lógico). */
public final class Eventos {
    /** De quantos em quantos ticks o estado vai para o cliente enquanto transformado. */
    private static final int SYNC_TICKS = 5;

    private Eventos() {}

    /** Ouvinte que cancela o evento quando o teste dá true (o jeito do EventBus do Forge, que o mod usava). */
    private static net.minecraft.resources.Identifier id(String nome) {
        return net.minecraft.resources.Identifier.fromNamespaceAndPath(com.danrod.dcuniverse.DCUniverse.MODID, nome);
    }

    public static <E extends Event & ICancellableEvent> Consumer<E> cancela(Predicate<E> teste) {
        return e -> {
            if (teste.test(e)) e.setCanceled(true);
        };
    }

    public static void registrar() {
        NeoForge.EVENT_BUS.addListener(AddServerReloadListenersEvent.class, e -> {
            e.addListener(id("personagens"), new Personagens());
            e.addListener(id("npcs"), new com.danrod.dcuniverse.npc.Npcs.CarregarNpcs());
            e.addListener(id("missoes"), new com.danrod.dcuniverse.npc.Npcs.CarregarMissoes());
        });
        DCUniverse.modBus().addListener(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent.class, e -> {
            e.put(com.danrod.dcuniverse.content.Entidades.NPC.get(), com.danrod.dcuniverse.npc.NpcEntidade.atributos().build());
            e.put(com.danrod.dcuniverse.content.Entidades.NPC_SLIM.get(), com.danrod.dcuniverse.npc.NpcEntidade.atributos().build());
            e.put(com.danrod.dcuniverse.content.Entidades.KRYPTO.get(), com.danrod.dcuniverse.npc.Krypto.atributos().build());
            e.put(com.danrod.dcuniverse.content.Entidades.STREAKY.get(), com.danrod.dcuniverse.npc.Streaky.atributos().build());
        });
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, e -> Comandos.registrar(e.getDispatcher()));
        // Vale no login e a cada /reload: o Guia mostra os personagens exatamente como o servidor os carregou.
        NeoForge.EVENT_BUS.addListener(OnDatapackSyncEvent.class, e -> e.getRelevantPlayers().forEach(p -> {
            Rede.catalogo(p);
            Rede.npcCatalogo(p);
        }));
        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, e -> {
            if (e.getEntity() instanceof ServerPlayer sp) tick(sp);
        });
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST, false, LivingDeathEvent.class, Eventos.cancela((Predicate<LivingDeathEvent>) e -> {
            // Decisão 3: destransforma antes do drop; a armadura original cai normalmente, o traje nunca.
            if (e.getEntity() instanceof ServerPlayer sp && EstadoJogador.de(sp).transformado()) Transformacao.destransformar(sp);
            return false;
        }));
        NeoForge.EVENT_BUS.addListener(LivingDeathEvent.class, e -> {
            if (e.getSource().getEntity() instanceof ServerPlayer sp && !(e.getEntity() instanceof net.minecraft.world.entity.player.Player)) {
                com.danrod.dcuniverse.missao.Diario.abateu(sp, e.getEntity());
                if (e.getEntity() instanceof net.minecraft.world.entity.monster.Enemy) com.danrod.dcuniverse.npc.Afinidade.protegeu(sp, e.getEntity());
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.ItemCraftedEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer sp) com.danrod.dcuniverse.missao.Diario.fabricou(sp, e.getCrafting());
        });
        NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, e -> {
            if (e.getEntity() instanceof net.minecraft.world.entity.monster.Creeper c && !e.getLevel().isClientSide()) com.danrod.dcuniverse.npc.Streaky.assustar(c);
        });
        NeoForge.EVENT_BUS.addListener(LivingFallEvent.class, Eventos.cancela((Predicate<LivingFallEvent>) e -> e.getEntity() instanceof ServerPlayer sp && Passivas.semQueda(sp)));
        NeoForge.EVENT_BUS.addListener(ItemTossEvent.class, Eventos.cancela((Predicate<ItemTossEvent>) e -> Traje.ehTraje(e.getEntity().getItem())));
        NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, Eventos.cancela((Predicate<EntityJoinLevelEvent>) e -> e.getEntity() instanceof ItemEntity item && Traje.ehTraje(item.getItem())));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                Guia.darGuia(p);
                Rede.guia(p, "");
                com.danrod.dcuniverse.inicio.Inicio.entrou(p);
                com.danrod.dcuniverse.missao.Diario.sincronizar(p);
            }
            sync(e.getEntity());
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerRespawnEvent.class, e -> sync(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer sp) {
                DemoHud.parar(sp);
                com.danrod.dcuniverse.world.estrutura.Chegada.sair(sp);
                com.danrod.dcuniverse.npc.Dialogo.fechar(sp);
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.StartTracking.class, e -> {
            if (e.getEntity() instanceof ServerPlayer quem && e.getTarget() instanceof com.danrod.dcuniverse.content.IlhaTelecinetica ilha) {
                Rede.ilha(quem, ilha.pacote());
            }
            if (e.getEntity() instanceof ServerPlayer quem && e.getTarget() instanceof ServerPlayer alvo) {
                Rede.aparenciaPara(quem, alvo);
                // Ver outro jogador transformado também libera a página do personagem no Guia.
                var id = EstadoJogador.de(alvo).personagemId();
                if (id != null) Guia.descobrir(quem, id);
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, e -> sync(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, e -> {
            Onda.tick();
            com.danrod.dcuniverse.combate.Combo.tick(e.getServer());
            com.danrod.dcuniverse.ability.Congelamento.tick();
            Fortaleza.tick();
            com.danrod.dcuniverse.missao.Cenas.tick();
            com.danrod.dcuniverse.missao.Cenas.tickFumaca();
            com.danrod.dcuniverse.liga.Liga.tick(e.getServer());
            com.danrod.dcuniverse.ability.active.Gancho.tick();
            com.danrod.dcuniverse.ability.active.Fumaca.tick(e.getServer());
            com.danrod.dcuniverse.ability.active.Disruptor.tick();
            com.danrod.dcuniverse.ability.active.Investida.tick();
            com.danrod.dcuniverse.ability.active.Telecinese.tick(e.getServer());
            com.danrod.dcuniverse.ability.active.EscudoTk.tick();
            com.danrod.dcuniverse.ability.active.Ilha.tick();
            for (var level : e.getServer().getAllLevels()) com.danrod.dcuniverse.content.Teias.tick(level);
        });
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.server.ServerStartedEvent.class, e -> com.danrod.dcuniverse.inicio.Inicio.ajustarNascimento(e.getServer()));
        NeoForge.EVENT_BUS.addListener(ServerStoppingEvent.class, e -> {
            com.danrod.dcuniverse.inicio.Inicio.limpar();
            Onda.limpar();
            com.danrod.dcuniverse.ability.Congelamento.limpar();
            Fortaleza.limpar();
            com.danrod.dcuniverse.missao.Cenas.limpar();
            com.danrod.dcuniverse.ability.active.Gancho.limpar();
            com.danrod.dcuniverse.ability.active.Veiculo.limpar();
            com.danrod.dcuniverse.ability.active.Fumaca.limpar();
            com.danrod.dcuniverse.ability.active.Disruptor.limpar();
            com.danrod.dcuniverse.ability.active.Investida.limpar();
            com.danrod.dcuniverse.ability.active.Telecinese.limpar();
            com.danrod.dcuniverse.ability.active.EscudoTk.limpar();
            com.danrod.dcuniverse.ability.active.Ilha.limpar();
            com.danrod.dcuniverse.content.Teias.limpar();
        });
        Naturais.registrar();
        com.danrod.dcuniverse.content.Batcomputador.registrar();
        com.danrod.dcuniverse.content.Teias.registrar();
        com.danrod.dcuniverse.content.BatwingEntidade.registrar();
        com.danrod.dcuniverse.content.NaveKryptoniana.registrar();
        com.danrod.dcuniverse.npc.Coleira.registrar();
        com.danrod.dcuniverse.ability.Congelamento.registrar();
        com.danrod.dcuniverse.ability.active.Detetive.registrar();
        com.danrod.dcuniverse.ability.passive.Furtividade.registrar();
        com.danrod.dcuniverse.ability.passive.ContraAtaque.registrar();
        com.danrod.dcuniverse.ability.active.EscudoTk.registrar();
    }

    static void tick(ServerPlayer player) {
        QuedaAmortecida.tick(player);
        if (player.tickCount % 20 == 0) {
            Guia.tickSegundo(player);
            com.danrod.dcuniverse.world.estrutura.Chegada.tickSegundo(player);
            com.danrod.dcuniverse.missao.Diario.tickSegundo(player);
        }
        if (player.tickCount % 100 == 0) com.danrod.dcuniverse.npc.Residentes.tick(player);
        EstadoJogador estado = EstadoJogador.de(player);
        if (!estado.transformado()) {
            if (player.tickCount % 20 == 0) Traje.verificar(player, null);
            com.danrod.dcuniverse.content.Equipamentos.tick(player);
            return;
        }
        Transformacao.tick(player);
        Personagem p = estado.personagem();
        if (p == null) return;
        Energia.tick(player, p);
        Passivas.tick(player, p);
        Habilidades.tick(player, p);
        com.danrod.dcuniverse.ability.Cargas.tick(player, p);
        com.danrod.dcuniverse.ability.active.Detetive.tick(player, p);
        DemoHud.tick(player, p);
        if (player.tickCount % SYNC_TICKS == 0) Rede.sincronizar(player);
    }

    private static void sync(net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer sp) {
            Rede.sincronizar(sp);
            Rede.aparencia(sp);
        }
    }
}
