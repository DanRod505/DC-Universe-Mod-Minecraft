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
        AddReloadListenerEvent.BUS.addListener(e -> e.addListener(new Personagens()));
        RegisterCommandsEvent.BUS.addListener(e -> Comandos.registrar(e.getDispatcher()));
        // Vale no login e a cada /reload: o Guia mostra os personagens exatamente como o servidor os carregou.
        OnDatapackSyncEvent.BUS.addListener(e -> e.getPlayers().forEach(Rede::catalogo));
        TickEvent.PlayerTickEvent.Post.BUS.addListener(e -> {
            if (e.player() instanceof ServerPlayer sp) tick(sp);
        });
        LivingDeathEvent.BUS.addListener(Priority.HIGHEST, (Predicate<LivingDeathEvent>) e -> {
            // Decisão 3: destransforma antes do drop; a armadura original cai normalmente, o traje nunca.
            if (e.getEntity() instanceof ServerPlayer sp && EstadoJogador.de(sp).transformado()) Transformacao.destransformar(sp);
            return false;
        });
        LivingFallEvent.BUS.addListener((Predicate<LivingFallEvent>) e -> e.getEntity() instanceof ServerPlayer sp && Passivas.semQueda(sp));
        ItemTossEvent.BUS.addListener((Predicate<ItemTossEvent>) e -> Traje.ehTraje(e.getEntity().getItem()));
        EntityJoinLevelEvent.BUS.addListener((Predicate<EntityJoinLevelEvent>) e -> e.getEntity() instanceof ItemEntity item && Traje.ehTraje(item.getItem()));
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(e -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                Guia.darGuia(p);
                Rede.guia(p, "");
            }
            sync(e.getEntity());
        });
        PlayerEvent.PlayerRespawnEvent.BUS.addListener(e -> sync(e.getEntity()));
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(e -> {
            if (e.getEntity() instanceof ServerPlayer sp) DemoHud.parar(sp);
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
            Fortaleza.tick();
        });
        ServerStoppingEvent.BUS.addListener(e -> {
            Onda.limpar();
            Fortaleza.limpar();
        });
        Naturais.registrar();
    }

    static void tick(ServerPlayer player) {
        QuedaAmortecida.tick(player);
        if (player.tickCount % 20 == 0) Guia.tickSegundo(player);
        EstadoJogador estado = EstadoJogador.de(player);
        if (!estado.transformado()) {
            if (player.tickCount % 20 == 0) Traje.verificar(player, null);
            return;
        }
        Transformacao.tick(player);
        Personagem p = estado.personagem();
        if (p == null) return;
        Energia.tick(player, p);
        Passivas.tick(player, p);
        Habilidades.tick(player, p);
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
