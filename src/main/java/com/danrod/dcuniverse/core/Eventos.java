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
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.listener.Priority;

import java.util.function.Predicate;

/** Eventos do lado do servidor (lógico). */
public final class Eventos {
    /** De quantos em quantos ticks o estado vai para o cliente enquanto transformado. */
    private static final int SYNC_TICKS = 5;

    private Eventos() {}

    public static void registrar() {
        AddReloadListenerEvent.BUS.addListener(e -> {
            e.addListener(new Personagens());
            e.addListener(new com.danrod.dcuniverse.npc.Npcs.CarregarNpcs());
            e.addListener(new com.danrod.dcuniverse.npc.Npcs.CarregarMissoes());
        });
        net.minecraftforge.event.entity.EntityAttributeCreationEvent.BUS.addListener(e -> {
            e.put(com.danrod.dcuniverse.content.Entidades.NPC.get(), com.danrod.dcuniverse.npc.NpcEntidade.atributos().build());
            e.put(com.danrod.dcuniverse.content.Entidades.NPC_SLIM.get(), com.danrod.dcuniverse.npc.NpcEntidade.atributos().build());
            e.put(com.danrod.dcuniverse.content.Entidades.KRYPTO.get(), com.danrod.dcuniverse.npc.Krypto.atributos().build());
            e.put(com.danrod.dcuniverse.content.Entidades.STREAKY.get(), com.danrod.dcuniverse.npc.Streaky.atributos().build());
        });
        RegisterCommandsEvent.BUS.addListener(e -> Comandos.registrar(e.getDispatcher()));
        // Vale no login e a cada /reload: o Guia mostra os personagens exatamente como o servidor os carregou.
        OnDatapackSyncEvent.BUS.addListener(e -> e.getPlayers().forEach(p -> {
            Rede.catalogo(p);
            Rede.npcCatalogo(p);
        }));
        TickEvent.PlayerTickEvent.Post.BUS.addListener(e -> {
            if (e.player() instanceof ServerPlayer sp) tick(sp);
        });
        LivingDeathEvent.BUS.addListener(Priority.HIGHEST, (Predicate<LivingDeathEvent>) e -> {
            // Decisão 3: destransforma antes do drop; a armadura original cai normalmente, o traje nunca.
            if (e.getEntity() instanceof ServerPlayer sp && EstadoJogador.de(sp).transformado()) Transformacao.destransformar(sp);
            return false;
        });
        LivingDeathEvent.BUS.addListener(e -> {
            if (e.getSource().getEntity() instanceof ServerPlayer sp && !(e.getEntity() instanceof net.minecraft.world.entity.player.Player)) {
                com.danrod.dcuniverse.missao.Diario.abateu(sp, e.getEntity());
                if (e.getEntity() instanceof net.minecraft.world.entity.monster.Enemy) com.danrod.dcuniverse.npc.Afinidade.protegeu(sp, e.getEntity());
            }
        });
        PlayerEvent.ItemCraftedEvent.BUS.addListener(e -> {
            if (e.getEntity() instanceof ServerPlayer sp) com.danrod.dcuniverse.missao.Diario.fabricou(sp, e.getCrafting());
        });
        EntityJoinLevelEvent.BUS.addListener(e -> {
            if (e.getEntity() instanceof net.minecraft.world.entity.monster.Creeper c && !e.getLevel().isClientSide()) com.danrod.dcuniverse.npc.Streaky.assustar(c);
        });
        LivingFallEvent.BUS.addListener((Predicate<LivingFallEvent>) e -> e.getEntity() instanceof ServerPlayer sp && Passivas.semQueda(sp));
        ItemTossEvent.BUS.addListener((Predicate<ItemTossEvent>) e -> Traje.ehTraje(e.getEntity().getItem()));
        EntityJoinLevelEvent.BUS.addListener((Predicate<EntityJoinLevelEvent>) e -> e.getEntity() instanceof ItemEntity item && Traje.ehTraje(item.getItem()));
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(e -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                Guia.darGuia(p);
                Rede.guia(p, "");
                com.danrod.dcuniverse.missao.Diario.sincronizar(p);
            }
            sync(e.getEntity());
        });
        PlayerEvent.PlayerRespawnEvent.BUS.addListener(e -> sync(e.getEntity()));
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(e -> {
            if (e.getEntity() instanceof ServerPlayer sp) {
                DemoHud.parar(sp);
                com.danrod.dcuniverse.world.estrutura.Chegada.sair(sp);
                com.danrod.dcuniverse.npc.Dialogo.fechar(sp);
            }
        });
        PlayerEvent.StartTracking.BUS.addListener(e -> {
            if (e.getEntity() instanceof ServerPlayer quem && e.getTarget() instanceof ServerPlayer alvo) {
                Rede.aparenciaPara(quem, alvo);
                // Ver outro jogador transformado também libera a página do personagem no Guia.
                var id = EstadoJogador.de(alvo).personagemId();
                if (id != null) Guia.descobrir(quem, id);
            }
        });
        PlayerEvent.PlayerChangedDimensionEvent.BUS.addListener(e -> sync(e.getEntity()));
        TickEvent.ServerTickEvent.Post.BUS.addListener(e -> {
            Onda.tick();
            com.danrod.dcuniverse.ability.Congelamento.tick();
            Fortaleza.tick();
            com.danrod.dcuniverse.missao.Cenas.tick();
            com.danrod.dcuniverse.missao.Cenas.tickFumaca();
            com.danrod.dcuniverse.liga.Liga.tick(e.server());
            com.danrod.dcuniverse.ability.active.Gancho.tick();
            com.danrod.dcuniverse.ability.active.Fumaca.tick(e.server());
            com.danrod.dcuniverse.ability.active.Disruptor.tick();
            for (var level : e.server().getAllLevels()) com.danrod.dcuniverse.content.Teias.tick(level);
        });
        ServerStoppingEvent.BUS.addListener(e -> {
            Onda.limpar();
            com.danrod.dcuniverse.ability.Congelamento.limpar();
            Fortaleza.limpar();
            com.danrod.dcuniverse.missao.Cenas.limpar();
            com.danrod.dcuniverse.ability.active.Gancho.limpar();
            com.danrod.dcuniverse.ability.active.Veiculo.limpar();
            com.danrod.dcuniverse.ability.active.Fumaca.limpar();
            com.danrod.dcuniverse.ability.active.Disruptor.limpar();
            com.danrod.dcuniverse.content.Teias.limpar();
        });
        Naturais.registrar();
        com.danrod.dcuniverse.content.Batcomputador.registrar();
        com.danrod.dcuniverse.content.Teias.registrar();
        com.danrod.dcuniverse.content.BatwingEntidade.registrar();
        com.danrod.dcuniverse.ability.Congelamento.registrar();
        com.danrod.dcuniverse.ability.active.Detetive.registrar();
        com.danrod.dcuniverse.ability.passive.Furtividade.registrar();
        com.danrod.dcuniverse.ability.passive.ContraAtaque.registrar();
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
