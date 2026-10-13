package com.danrod.dcuniverse.core;

import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.character.Personagens;
import com.danrod.dcuniverse.character.Transformacao;
import com.danrod.dcuniverse.energy.Energia;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** /dcu transform|untransform|energy: comandos de admin para testes (seção 2.4). */
public final class Comandos {
    private Comandos() {}

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("dcu")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("transform")
                .then(Commands.argument("jogador", EntityArgument.player())
                    .then(Commands.argument("personagem", IdentifierArgument.id())
                        .suggests((c, b) -> SharedSuggestionProvider.suggestResource(Personagens.todos().stream().map(Personagem::id), b))
                        .executes(c -> {
                            ServerPlayer alvo = EntityArgument.getPlayer(c, "jogador");
                            boolean ok = Transformacao.transformar(alvo, IdentifierArgument.getId(c, "personagem"), true);
                            return ok ? 1 : 0;
                        }))))
            .then(Commands.literal("untransform")
                .then(Commands.argument("jogador", EntityArgument.player())
                    .executes(c -> {
                        ServerPlayer alvo = EntityArgument.getPlayer(c, "jogador");
                        if (!EstadoJogador.de(alvo).transformado()) return 0;
                        Transformacao.destransformar(alvo);
                        return 1;
                    })))
            .then(Commands.literal("energy")
                .then(Commands.argument("jogador", EntityArgument.player())
                    .then(Commands.argument("valor", FloatArgumentType.floatArg(0))
                        .executes(c -> {
                            ServerPlayer alvo = EntityArgument.getPlayer(c, "jogador");
                            EstadoJogador e = EstadoJogador.de(alvo);
                            Personagem p = e.personagem();
                            if (p == null) return 0;
                            e.setEnergia(0);
                            e.setEsgotado(false);
                            Energia.adicionar(alvo, p, FloatArgumentType.getFloat(c, "valor"));
                            Rede.sincronizar(alvo);
                            c.getSource().sendSuccess(() -> Component.literal("Energia: " + e.energia()), false);
                            return 1;
                        })))));
    }
}
