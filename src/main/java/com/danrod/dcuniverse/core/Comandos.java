package com.danrod.dcuniverse.core;

import com.danrod.dcuniverse.ability.DemoHud;
import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.character.Personagens;
import com.danrod.dcuniverse.character.Transformacao;
import com.danrod.dcuniverse.energy.Energia;
import com.danrod.dcuniverse.energy.Recargas;
import com.danrod.dcuniverse.guia.Guia;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** /dcu transform|untransform|select|energy|ready|status|guide|hud: comandos de admin para testes (seção 2.4). */
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
                            var id = IdentifierArgument.getId(c, "personagem");
                            boolean ok = Transformacao.transformar(alvo, id, true);
                            if (ok) c.getSource().sendSuccess(() -> Component.literal(alvo.getScoreboardName() + " -> " + id), true);
                            return ok ? 1 : 0;
                        }))))
            .then(Commands.literal("untransform")
                .then(Commands.argument("jogador", EntityArgument.player())
                    .executes(c -> {
                        ServerPlayer alvo = EntityArgument.getPlayer(c, "jogador");
                        if (!EstadoJogador.de(alvo).transformado()) return 0;
                        Transformacao.destransformar(alvo);
                        c.getSource().sendSuccess(() -> Component.literal(alvo.getScoreboardName() + " destransformado"), true);
                        return 1;
                    })))
            .then(Commands.literal("select")
                .then(Commands.argument("jogador", EntityArgument.player())
                    .then(Commands.argument("habilidade", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes(c -> {
                            ServerPlayer alvo = EntityArgument.getPlayer(c, "jogador");
                            Personagem p = EstadoJogador.de(alvo).personagem();
                            String id = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "habilidade");
                            int i = p == null ? -1 : com.danrod.dcuniverse.character.Cinto.equipadas(alvo, p).indexOf(p.habilidade(id));
                            if (i < 0) return 0;
                            com.danrod.dcuniverse.ability.Habilidades.selecionar(alvo, i);
                            c.getSource().sendSuccess(() -> Component.literal(alvo.getScoreboardName() + ": " + id), false);
                            return 1;
                        }))))
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
                        }))))
            .then(Commands.literal("ready")
                .then(Commands.argument("jogador", EntityArgument.player())
                    .executes(c -> {
                        ServerPlayer alvo = EntityArgument.getPlayer(c, "jogador");
                        EstadoJogador.de(alvo).limparRecargas();
                        Rede.sincronizar(alvo);
                        c.getSource().sendSuccess(() -> Component.literal(alvo.getScoreboardName() + ": recargas zeradas"), false);
                        return 1;
                    })))
            .then(Commands.literal("status")
                .then(Commands.argument("jogador", EntityArgument.player())
                    .executes(c -> {
                        ServerPlayer alvo = EntityArgument.getPlayer(c, "jogador");
                        EstadoJogador e = EstadoJogador.de(alvo);
                        Personagem p = e.personagem();
                        if (p == null) {
                            c.getSource().sendSuccess(() -> Component.translatable("dcuniverse.cmd.status_normal", alvo.getDisplayName()), false);
                            return 0;
                        }
                        String recarga = String.format(java.util.Locale.ROOT, "%+.1f", Recargas.porSegundo(alvo, p));
                        c.getSource().sendSuccess(() -> Component.translatable("dcuniverse.cmd.status", alvo.getDisplayName(),
                            Component.translatable(p.chaveNome()), Math.round(e.energia()), Math.round(p.energia().max()), recarga,
                            Component.translatable(e.esgotado() ? "dcuniverse.cmd.sim" : "dcuniverse.cmd.nao")), false);
                        return 1;
                    })))
            .then(Commands.literal("guide")
                .then(Commands.literal("reset")
                    .then(Commands.argument("jogador", EntityArgument.player())
                        .executes(c -> {
                            ServerPlayer alvo = EntityArgument.getPlayer(c, "jogador");
                            Guia.resetar(alvo);
                            c.getSource().sendSuccess(() -> Component.translatable("dcuniverse.cmd.guia_resetado", alvo.getDisplayName()), true);
                            return 1;
                        }))))
            .then(Commands.literal("hud")
                .then(Commands.literal("demo")
                    .then(Commands.argument("jogador", EntityArgument.player())
                        .executes(c -> {
                            ServerPlayer alvo = EntityArgument.getPlayer(c, "jogador");
                            if (!EstadoJogador.de(alvo).transformado()) {
                                c.getSource().sendFailure(Component.translatable("dcuniverse.cmd.precisa_transformar", alvo.getDisplayName()));
                                return 0;
                            }
                            DemoHud.iniciar(alvo);
                            return 1;
                        })))
                .then(Commands.literal("stop")
                    .then(Commands.argument("jogador", EntityArgument.player())
                        .executes(c -> {
                            DemoHud.parar(EntityArgument.getPlayer(c, "jogador"));
                            return 1;
                        }))))
            // Teleporta para um local gerado (e um ponto dele): /dcu local smallville nave
            .then(Commands.literal("local")
                .then(Commands.argument("local", com.mojang.brigadier.arguments.StringArgumentType.word())
                    .suggests((c, b) -> SharedSuggestionProvider.suggest(com.danrod.dcuniverse.world.estrutura.Estruturas.LOCAIS.keySet(), b))
                    .executes(c -> irPara(c.getSource(), com.mojang.brigadier.arguments.StringArgumentType.getString(c, "local"), ""))
                    .then(Commands.argument("ponto", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests((c, b) -> {
                            var l = com.danrod.dcuniverse.world.estrutura.Estruturas.LOCAIS.get(com.mojang.brigadier.arguments.StringArgumentType.getString(c, "local"));
                            return SharedSuggestionProvider.suggest(l == null ? java.util.Set.of() : l.pontos().keySet(), b);
                        })
                        .executes(c -> irPara(c.getSource(), com.mojang.brigadier.arguments.StringArgumentType.getString(c, "local"),
                            com.mojang.brigadier.arguments.StringArgumentType.getString(c, "ponto")))))));
    }

    private static int irPara(CommandSourceStack src, String local, String ponto) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        var pos = com.danrod.dcuniverse.world.estrutura.Locais.ponto(p.level(), local, ponto, p.blockPosition());
        if (pos.isEmpty()) {
            src.sendFailure(Component.translatable("dcuniverse.cmd.local_nao_achado", local));
            return 0;
        }
        var b = pos.get();
        p.teleportTo(p.level(), b.getX() + 0.5, b.getY(), b.getZ() + 0.5, java.util.Set.of(), p.getYRot(), p.getXRot(), true);
        src.sendSuccess(() -> Component.literal(local + (ponto.isEmpty() ? "" : " / " + ponto) + ": " + b.toShortString()), false);
        return 1;
    }
}
