package com.danrod.dcuniverse.test;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.ability.DemoHud;
import com.danrod.dcuniverse.ability.Habilidades;
import com.danrod.dcuniverse.ability.Passivas;
import com.danrod.dcuniverse.ability.QuedaAmortecida;
import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.ability.TiposHabilidade;
import com.danrod.dcuniverse.ability.TiposPassiva;
import com.danrod.dcuniverse.ability.active.Fortaleza;
import com.danrod.dcuniverse.ability.passive.Naturais;
import com.danrod.dcuniverse.ability.passive.Voo;
import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.character.Transformacao;
import com.danrod.dcuniverse.content.Itens;
import com.danrod.dcuniverse.content.ProjetilEntidade;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.energy.Energia;
import com.danrod.dcuniverse.guia.Guia;
import com.danrod.dcuniverse.suit.Traje;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraftforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Consumer;

/**
 * GameTests do mod (rodam em ./gradlew runGameTestServer e no CI). Cada função tem um
 * data/dcuniverse/test_instance/<nome>.json. Os grupos seguem os módulos do núcleo.
 */
public final class Testes {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCOES = DeferredRegister.create(Registries.TEST_FUNCTION, DCUniverse.MODID);

    private static final Identifier SUPERMAN = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "superman");

    static {
        FUNCOES.register("transform", () -> Testes::transforma);
        FUNCOES.register("suit_lock", () -> Testes::trajeTravado);
        FUNCOES.register("energy", () -> Testes::energia);
        FUNCOES.register("sync", () -> Testes::sincronizacao);
        FUNCOES.register("supreme", () -> Testes::suprema);
        FUNCOES.register("cushioned_fall", () -> Testes::quedaAmortecida);
        FUNCOES.register("guide", () -> Testes::guia);
        FUNCOES.register("hud", () -> Testes::hud);
        FUNCOES.register("abilities", () -> Testes::habilidades);
        FUNCOES.register("flight", () -> Testes::voo);
        FUNCOES.register("powers", () -> Testes::poderes);
        FUNCOES.register("fortress", () -> Testes::fortaleza);
        FUNCOES.register("elements", () -> Testes::elementos);
    }

    private Testes() {}

    private static ServerPlayer jogador(GameTestHelper h) {
        return (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
    }

    /** Transforma guardando a armadura, aplica a ficha e destransforma devolvendo tudo. */
    static void transforma(GameTestHelper h) {
        ServerPlayer p = jogador(h);
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        h.assertTrue(Transformacao.transformar(p, SUPERMAN, true), "transformar no Superman");
        EstadoJogador e = EstadoJogador.de(p);
        Personagem sup = e.personagem();
        h.assertTrue(sup != null, "personagem carregado do datapack");
        h.assertTrue(p.getItemBySlot(EquipmentSlot.CHEST).is(Itens.TRAJE_PEITO.get()), "traje no peito");
        h.assertTrue(p.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "Superman não trava a cabeça");
        h.assertValueEqual((double) p.getMaxHealth(), sup.ficha().vida() * 2, "vida máxima da ficha");
        h.assertValueEqual(e.energia(), sup.energia().max() * 0.5f, "começa com 50% de energia");
        Transformacao.destransformar(p);
        h.assertTrue(p.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE), "peitoral devolvido");
        h.assertTrue(p.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "capacete intacto");
        h.assertValueEqual((double) p.getMaxHealth(), 20.0, "vida máxima normal");
        h.assertFalse(e.transformado(), "destransformado");
        h.succeed();
    }

    /** Peças de traje fora do slot somem; slot vazio recebe a peça de volta. */
    static void trajeTravado(GameTestHelper h) {
        ServerPlayer p = jogador(h);
        Transformacao.transformar(p, SUPERMAN, true);
        Personagem sup = EstadoJogador.de(p).personagem();
        p.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        p.getInventory().add(new ItemStack(Itens.TRAJE_PES.get()));
        Traje.verificar(p, sup);
        h.assertTrue(p.getItemBySlot(EquipmentSlot.FEET).is(Itens.TRAJE_PES.get()), "bota recolocada");
        h.assertFalse(p.getInventory().getNonEquipmentItems().stream().anyMatch(Traje::ehTraje), "nenhuma peça no inventário");
        Transformacao.destransformar(p);
        h.assertFalse(Traje.SLOTS.stream().anyMatch(s -> Traje.ehTraje(p.getItemBySlot(s))), "sem traje depois de destransformar");
        h.succeed();
    }

    /** Recarga por tick, gasto e o estado esgotado até voltar a 10%. */
    static void energia(GameTestHelper h) {
        ServerPlayer p = jogador(h);
        Transformacao.transformar(p, SUPERMAN, true);
        EstadoJogador e = EstadoJogador.de(p);
        Personagem sup = e.personagem();
        float antes = e.energia();
        for (int i = 0; i < 20; i++) Energia.tick(p, sup);
        h.assertTrue(e.energia() >= antes + sup.energia().regen() - 0.01f, "regenera pelo menos o regen da ficha em 1 s");
        Energia.adicionar(p, sup, -10_000);
        h.assertTrue(e.esgotado(), "esgotado ao zerar");
        h.assertFalse(Energia.gastar(p, sup, 1), "não gasta esgotado");
        Energia.adicionar(p, sup, sup.energia().max() * 0.11f);
        h.assertFalse(e.esgotado(), "libera ao passar de 10%");
        h.succeed();
    }

    /** Os pacotes passam ida e volta pelo codec sem perder nada. */
    static void sincronizacao(GameTestHelper h) {
        ServerPlayer p = jogador(h);
        Transformacao.transformar(p, SUPERMAN, true);
        var buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        Rede.Sync.encode(Rede.montar(p), buf);
        Rede.Sync s = Rede.Sync.decode(buf);
        h.assertValueEqual(s.personagem(), SUPERMAN.toString(), "personagem no sync");
        h.assertValueEqual(s.max(), 300f, "energia máxima no sync");
        var hab = new Rede.Sync("x", 1, 2, true, 0, "", List.of(new Rede.HabInfo("a", false, true, 210, 5, 1800)), 0, true, 4.5f, true, false, 32f, 15f);
        Rede.Sync.encode(hab, buf);
        h.assertValueEqual(Rede.Sync.decode(buf), hab, "sync com suprema e queda amortecida");
        var guia = new Rede.GuiaSync(List.of("transformar"), List.of(SUPERMAN.toString()), "sol");
        Rede.GuiaSync.encode(guia, buf);
        h.assertValueEqual(Rede.GuiaSync.decode(buf), guia, "progresso do guia");
        h.succeed();
    }

    /** Suprema: custo em % da energia máxima, recarga longa e limites de 60-80% e 60-180 s. */
    static void suprema(GameTestHelper h) {
        // Tipo de teste: as supremas de verdade chegam com as habilidades de cada personagem.
        TiposHabilidade.TIPOS.putIfAbsent("test_marker", new TipoHabilidade() {
            @Override
            public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade hab) {
                return true;
            }
        });
        String base = "{\"n\":1,\"level\":4,\"stats\":{\"vida\":40,\"armadura\":22,\"dano\":12,\"terra\":6.5},"
            + "\"energy\":{\"max\":300,\"regen\":0,\"recharge\":[]},\"suit\":[\"chest\"],\"passives\":[],\"abilities\":[%s]}";
        Personagem p = Personagem.parse(SUPERMAN, JsonParser.parseString(String.format(base,
            "{\"id\":\"teste\",\"type\":\"test_marker\",\"supreme\":true,\"cost_pct\":70,\"cooldown\":90}")).getAsJsonObject());
        Personagem.Habilidade sup = p.suprema();
        h.assertTrue(sup != null, "suprema lida do JSON");
        h.assertValueEqual(sup.custo(), 210f, "70% de 300");
        for (String ruim : new String[] {
            "{\"id\":\"a\",\"type\":\"test_marker\",\"supreme\":true,\"cost_pct\":50,\"cooldown\":90}",
            "{\"id\":\"a\",\"type\":\"test_marker\",\"supreme\":true,\"cost_pct\":70,\"cooldown\":20}",
            "{\"id\":\"a\",\"type\":\"test_marker\",\"supreme\":true,\"cost_pct\":70,\"cooldown\":90,\"mode\":\"hold\"}"}) {
            boolean recusou = false;
            try {
                Personagem.parse(SUPERMAN, JsonParser.parseString(String.format(base, ruim)).getAsJsonObject());
            } catch (RuntimeException e) {
                recusou = true;
            }
            h.assertTrue(recusou, "suprema fora das regras recusada: " + ruim);
        }
        // Uso: com o personagem do teste no lugar do Superman, gasta 70% e entra em recarga.
        ServerPlayer jog = jogador(h);
        Transformacao.transformar(jog, SUPERMAN, true);
        EstadoJogador e = EstadoJogador.de(jog);
        e.setEnergia(300);
        long agora = jog.level().getGameTime();
        h.assertTrue(Habilidades.executar(jog, p, sup), "usa a suprema com energia cheia");
        h.assertValueEqual(e.energia(), 90f, "sobram 30%");
        h.assertTrue(e.prontaEm(sup.id()) >= agora + 90 * 20, "recarga de 90 s");
        h.assertFalse(Habilidades.executar(jog, p, sup), "não usa de novo em recarga");
        h.succeed();
    }

    /** Destransformar no ar liga a queda amortecida: sem dano de queda até tocar o chão. */
    static void quedaAmortecida(GameTestHelper h) {
        ServerPlayer p = jogador(h);
        var alto = h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 20, 1.5));
        p.setPos(alto.x, alto.y, alto.z);
        p.setOnGround(false);
        Transformacao.transformar(p, SUPERMAN, true);
        Transformacao.destransformar(p);
        h.assertTrue(QuedaAmortecida.ativa(p), "queda amortecida ligada no ar");
        h.assertTrue(Passivas.semQueda(p), "sem dano de queda");
        p.setOnGround(true);
        QuedaAmortecida.tick(p);
        h.assertFalse(QuedaAmortecida.ativa(p), "desliga ao pousar");
        h.assertTrue(Guia.concluida(p, "amortecida"), "tarefa do guia concluída");
        // No chão, destransformar não liga nada.
        Transformacao.transformar(p, SUPERMAN, true);
        Transformacao.destransformar(p);
        h.assertFalse(QuedaAmortecida.ativa(p), "no chão não amortece");
        h.succeed();
    }

    /** Guia dado uma vez só; tarefas e descobertas ficam salvas e o reset limpa. */
    static void guia(GameTestHelper h) {
        ServerPlayer p = jogador(h);
        h.assertTrue(Guia.darGuia(p), "primeiro login recebe o guia");
        h.assertFalse(Guia.darGuia(p), "só uma vez");
        h.assertTrue(p.getInventory().contains(s -> s.is(Itens.GUIA_HEROI.get())), "guia no inventário");
        h.assertFalse(Guia.descoberto(p, SUPERMAN), "Superman ainda desconhecido");
        p.getInventory().add(new ItemStack(Itens.CRISTAL_SUPERMAN.get()));
        Guia.tickSegundo(p);
        h.assertTrue(Guia.descoberto(p, SUPERMAN), "ter o cristal descobre o Superman");
        Transformacao.transformar(p, SUPERMAN, true);
        h.assertTrue(Guia.concluida(p, "transformar"), "tarefa transformar");
        Guia.resetar(p);
        h.assertFalse(Guia.concluida(p, "transformar"), "reset limpa as tarefas");
        h.assertFalse(Guia.darGuia(p), "reset não dá outro livro");
        h.succeed();
    }

    /** HUD: recarga e sol no sync, quando a suprema está pronta e a demonstração com as habilidades de exemplo. */
    static void hud(GameTestHelper h) {
        ServerPlayer p = jogador(h);
        Transformacao.transformar(p, SUPERMAN, true);
        EstadoJogador e = EstadoJogador.de(p);
        Personagem sup = e.personagem();
        Rede.Sync s = Rede.montar(p);
        h.assertTrue(s.solar(), "Superman recarrega no sol");
        h.assertTrue(s.recarga() >= sup.energia().regen(), "recarga inclui a regeneração da ficha");
        h.assertValueEqual(s.habilidades().size(), sup.habilidades().size(), "só as habilidades do personagem fora da demonstração");
        DemoHud.iniciar(p);
        e.setEnergia(sup.energia().max());
        s = Rede.montar(p);
        h.assertValueEqual(s.habilidades().size(), sup.habilidades().size() + 1, "a demonstração acrescenta a suprema de exemplo");
        Rede.HabInfo suprema = s.habilidades().stream().filter(Rede.HabInfo::suprema).findFirst().orElseThrow();
        h.assertValueEqual(suprema.custo(), sup.energia().max() * 0.7f, "suprema custa 70%");
        h.assertTrue(s.pronta(suprema), "com energia cheia a suprema está pronta");
        e.setEnergia(suprema.custo() - 1);
        h.assertFalse(Rede.montar(p).pronta(suprema), "sem energia suficiente não está pronta");
        h.assertFalse(new Rede.Sync("x", 300, 300, false, 0, "", List.of(), 0, false, 0, false, false, 0, 0)
            .pronta(new Rede.HabInfo("a", false, true, 210, 20, 1800)), "em recarga não está pronta");
        Transformacao.destransformar(p);
        h.assertFalse(DemoHud.ativa(p), "destransformar encerra a demonstração");
        h.succeed();
    }

    /** Menu radial e tipos: escolher, canalizar o raio (gasta por tick, recarga ao soltar) e disparar o soco sônico. */
    static void habilidades(GameTestHelper h) {
        ServerPlayer p = jogador(h);
        // No lugar do teste: na origem do mundo o chunk pode não estar carregado e a onda some.
        var aqui = h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2, 1.5));
        p.setPos(aqui.x, aqui.y, aqui.z);
        Transformacao.transformar(p, SUPERMAN, true);
        EstadoJogador e = EstadoJogador.de(p);
        Personagem sup = e.personagem();
        e.setEnergia(sup.energia().max());
        h.assertValueEqual(sup.habilidade("visao_de_calor").tipo(), "projectile_beam", "visão de calor é um raio");
        h.assertValueEqual(sup.habilidade("sopro_congelante").tipo(), "area_cone", "sopro congelante é um cone");
        h.assertValueEqual(sup.habilidade("soco_sonico").tipo(), "projectile", "soco sônico é um projétil");

        Habilidades.selecionar(p, 0);
        h.assertTrue(Guia.concluida(p, "radial"), "escolher no menu conclui a tarefa");
        Habilidades.usar(p, true);
        h.assertValueEqual(e.canalizando(), "visao_de_calor", "segurar V canaliza o raio");
        h.assertValueEqual(Rede.montarAparencia(p).efeito(), "projectile_beam", "quem vê recebe o efeito do raio");
        float antes = e.energia();
        Habilidades.tick(p, sup);
        h.assertTrue(e.energia() < antes, "o raio gasta energia enquanto canaliza");
        Habilidades.usar(p, false);
        h.assertValueEqual(e.canalizando(), "", "soltar V para o raio");
        h.assertTrue(e.prontaEm("visao_de_calor") > p.level().getGameTime(), "o raio entra em recarga ao soltar");
        h.assertTrue(Guia.concluida(p, "habilidade"), "usar conclui a tarefa");

        int soco = sup.habilidades().indexOf(sup.habilidade("soco_sonico"));
        Habilidades.selecionar(p, soco);
        float energia = e.energia();
        Habilidades.usar(p, true);
        h.assertValueEqual(e.energia(), energia - 40f, "soco sônico custa 40");
        h.assertFalse(p.level().getEntitiesOfClass(ProjetilEntidade.class, p.getBoundingBox().inflate(4)).isEmpty(), "a onda sônica sai");
        Habilidades.usar(p, false);
        Habilidades.usar(p, true);
        h.assertValueEqual(e.energia(), energia - 40f, "em recarga não dispara de novo");
        h.succeed();
    }

    /**
     * Voo: transformado libera o pulo duplo, voar gasta energia, conclui as tarefas e avisa quem vê;
     * sem energia o voo desliga e a descida é amortecida.
     */
    static void voo(GameTestHelper h) {
        ServerPlayer p = jogador(h);
        var alto = h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 20, 1.5));
        p.setPos(alto.x, alto.y, alto.z);
        p.setOnGround(false);
        Transformacao.transformar(p, SUPERMAN, true);
        EstadoJogador e = EstadoJogador.de(p);
        // Só a passiva de voo: as de efeito (fogo) precisam de conexão.
        var voo = TiposPassiva.TIPOS.get("flight");
        var prm = e.personagem().passiva("flight").params();
        h.assertTrue(p.getAbilities().mayfly, "transformado pode voar");
        h.assertValueEqual(p.getAbilities().getFlyingSpeed(), 0f, "quem move é o cliente");
        h.assertValueEqual(Rede.montar(p).voo(), 56f, "velocidade máxima vai no sync");
        p.getAbilities().flying = true;
        e.setEnergia(100);
        voo.tick(p, prm);
        h.assertTrue(e.energia() < 100, "voar gasta energia");
        h.assertTrue(Guia.concluida(p, "voar"), "tarefa voar");
        h.assertTrue(Rede.montarAparencia(p).voando(), "quem vê sabe que ele voa");
        Voo.estrondo(p);
        h.assertTrue(Guia.concluida(p, "estrondo"), "tarefa estrondo");
        float antes = e.energia();
        Voo.manobra(p, Voo.ARRANCADA);
        h.assertValueEqual(e.energia(), antes - 15f, "a arrancada custa energia");
        h.assertTrue(Guia.concluida(p, "manobra"), "tarefa manobra");
        // Esgota direto: o aviso de energia zerada precisa de conexão.
        e.setEnergia(0);
        e.setEsgotado(true);
        voo.tick(p, prm);
        h.assertFalse(p.getAbilities().flying, "sem energia o voo desliga");
        h.assertFalse(p.getAbilities().mayfly, "e não religa sem energia");
        h.assertTrue(QuedaAmortecida.ativa(p), "desce amortecido");
        Transformacao.destransformar(p);
        h.assertFalse(p.getAbilities().mayfly, "normal não voa");
        // No criativo vale o voo do mod; ao destransformar volta o voo do criativo.
        ServerPlayer c = (ServerPlayer) h.makeMockServerPlayer(GameType.CREATIVE);
        c.setPos(alto.x, alto.y, alto.z);
        Transformacao.transformar(c, SUPERMAN, true);
        h.assertValueEqual(c.getAbilities().getFlyingSpeed(), 0f, "criativo usa o voo do mod");
        c.getAbilities().flying = true;
        EstadoJogador.de(c).setEnergia(0);
        EstadoJogador.de(c).setEsgotado(true);
        voo.tick(c, prm);
        h.assertTrue(c.getAbilities().flying, "no criativo a energia não derruba");
        Transformacao.destransformar(c);
        h.assertTrue(c.getAbilities().mayfly && c.getAbilities().flying, "continua voando no criativo");
        h.assertValueEqual(c.getAbilities().getFlyingSpeed(), 0.05f, "com a velocidade do criativo");
        h.succeed();
    }

    /** Piso de pedra em volta do ponto relativo (x, y, z), com raio r, para as habilidades que precisam de chão. */
    private static void piso(GameTestHelper h, int x, int y, int z, int r) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                h.getLevel().setBlockAndUpdate(h.absolutePos(new net.minecraft.core.BlockPos(x + dx, y, z + dz)),
                    net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            }
        }
    }

    /**
     * Pisão sísmico (precisa de chão, atinge e deixa lento quem a onda alcança), visão de raio X (só cobra:
     * o desenho é do cliente) e as passivas naturais: superforça (pedra sim, obsidiana nunca, só de mão
     * vazia) e ricochete de projéteis fracos.
     */
    static void poderes(GameTestHelper h) {
        ServerPlayer p = jogador(h);
        piso(h, 1, 1, 1, 6);
        var aqui = h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2, 1.5));
        p.setPos(aqui.x, aqui.y, aqui.z);
        Transformacao.transformar(p, SUPERMAN, true);
        EstadoJogador e = EstadoJogador.de(p);
        Personagem sup = e.personagem();
        e.setEnergia(sup.energia().max());
        var alvo = h.spawn(net.minecraft.world.entity.EntityTypes.PIG, new net.minecraft.core.BlockPos(4, 2, 1));
        Personagem.Habilidade pisao = sup.habilidade("pisao_sismico");
        h.assertValueEqual(pisao.tipo(), "area_blast", "pisão sísmico é uma onda");
        float antes = e.energia();
        h.assertTrue(Habilidades.executar(p, sup, pisao), "pisão no chão funciona");
        h.assertValueEqual(e.energia(), antes - 30f, "pisão custa 30");

        ServerPlayer noAr = jogador(h);
        var alto = h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 12, 1.5));
        noAr.setPos(alto.x, alto.y, alto.z);
        Transformacao.transformar(noAr, SUPERMAN, true);
        EstadoJogador.de(noAr).setEnergia(sup.energia().max());
        h.assertFalse(Habilidades.executar(noAr, sup, pisao), "longe do chão o pisão não sai");
        h.assertValueEqual(EstadoJogador.de(noAr).energia(), sup.energia().max(), "e não gasta energia");

        Personagem.Habilidade raioX = sup.habilidade("visao_raio_x");
        antes = e.energia();
        h.assertTrue(Habilidades.executar(p, sup, raioX), "visão de raio X liga");
        h.assertValueEqual(e.energia(), antes - 20f, "raio X custa 20");

        var pedra = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        h.assertTrue(Naturais.velocidadeMao(p, pedra) > 1, "superforça quebra pedra com a mão");
        h.assertTrue(Naturais.velocidadeMao(p, net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState()) == 0, "obsidiana nunca");
        h.assertTrue(Naturais.velocidadeMao(p, net.minecraft.world.level.block.Blocks.BEDROCK.defaultBlockState()) == 0, "bedrock nunca");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        h.assertTrue(Naturais.velocidadeMao(p, pedra) == 0, "só com a mão vazia");
        h.assertTrue(Naturais.ricocheteia(p, true, 6, null), "flecha fraca ricocheteia");
        h.assertFalse(Naturais.ricocheteia(p, true, 30, null), "projétil forte passa");
        h.assertFalse(Naturais.ricocheteia(p, false, 2, null), "golpe corpo a corpo não ricocheteia");
        ServerPlayer normal = jogador(h);
        h.assertFalse(Naturais.ricocheteia(normal, true, 2, null), "sem transformar não ricocheteia");
        h.assertTrue(Naturais.velocidadeMao(normal, pedra) == 0, "sem transformar não tem superforça");

        // A onda leva 10 ticks para chegar na borda: o porco a 3 blocos já foi atingido.
        h.runAfterDelay(12, () -> {
            h.assertTrue(alvo.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS), "a onda deixa lento");
            h.assertTrue(alvo.getHealth() < alvo.getMaxHealth(), "a onda fere");
            h.succeed();
        });
    }

    /**
     * Efeitos secundários: a visão de calor derrete gelo compactado e põe fogo; o sopro prende o mob no gelo
     * (parado e sem IA) depois de 1 s, e o calor o solta na hora.
     */
    static void elementos(GameTestHelper h) {
        ServerPlayer p = jogador(h);
        piso(h, 1, 1, 1, 4);
        var level = h.getLevel();
        var gelo = h.absolutePos(new net.minecraft.core.BlockPos(2, 2, 1));
        level.setBlockAndUpdate(gelo, net.minecraft.world.level.block.Blocks.PACKED_ICE.defaultBlockState());
        com.danrod.dcuniverse.ability.active.Raio.esquentar(level, p,
            new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(gelo), net.minecraft.core.Direction.UP, gelo, false), true, false);
        h.assertTrue(level.getBlockState(gelo).is(net.minecraft.world.level.block.Blocks.WATER), "calor derrete gelo compactado");
        var madeira = h.absolutePos(new net.minecraft.core.BlockPos(4, 2, 1));
        level.setBlockAndUpdate(madeira, net.minecraft.world.level.block.Blocks.OAK_PLANKS.defaultBlockState());
        com.danrod.dcuniverse.ability.active.Raio.esquentar(level, p,
            new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(madeira), net.minecraft.core.Direction.UP, madeira, false), true, true);
        h.assertTrue(level.getBlockState(madeira.above()).is(net.minecraft.world.level.block.Blocks.FIRE), "calor põe fogo");

        var zumbi = h.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, new net.minecraft.core.BlockPos(1, 2, 3));
        com.danrod.dcuniverse.ability.Congelamento.expor(zumbi);
        h.assertFalse(com.danrod.dcuniverse.ability.Congelamento.congelado(zumbi), "meio segundo de sopro só deixa lento");
        com.danrod.dcuniverse.ability.Congelamento.expor(zumbi);
        h.assertTrue(com.danrod.dcuniverse.ability.Congelamento.congelado(zumbi) && zumbi.isNoAi(), "1 s de sopro prende no gelo");
        com.danrod.dcuniverse.ability.Congelamento.derreter(zumbi);
        h.assertFalse(com.danrod.dcuniverse.ability.Congelamento.congelado(zumbi) || zumbi.isNoAi(), "o calor solta o gelo");
        h.succeed();
    }

    /**
     * Fortaleza da Solidão: a forma tem porta para quem usou e torres em volta; a suprema cresce o cristal
     * do chão (nunca em cima de alguém), agenda o fim de cada bloco e recarrega quem está dentro.
     */
    static void fortaleza(GameTestHelper h) {
        var forma = Fortaleza.forma(6, net.minecraft.core.Direction.NORTH, net.minecraft.util.RandomSource.create(1));
        h.assertTrue(forma.size() > 150, "fortaleza com cúpula e torres (" + forma.size() + " blocos)");
        h.assertTrue(forma.stream().allMatch(b -> b.getY() >= 0), "nada abaixo do chão");
        h.assertTrue(forma.stream().mapToInt(net.minecraft.core.BlockPos::getY).max().orElse(0) >= 9, "torres mais altas que a cúpula");
        h.assertFalse(forma.contains(new net.minecraft.core.BlockPos(0, 1, -6)), "porta aberta para o norte");
        h.assertTrue(forma.contains(new net.minecraft.core.BlockPos(0, 1, 6)), "parede fechada atrás");

        ServerPlayer p = jogador(h);
        piso(h, 1, 1, 1, 8);
        var aqui = h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2, 1.5));
        p.setPos(aqui.x, aqui.y, aqui.z);
        p.setXRot(90);
        Transformacao.transformar(p, SUPERMAN, true);
        EstadoJogador e = EstadoJogador.de(p);
        Personagem sup = e.personagem();
        e.setEnergia(sup.energia().max());
        Personagem.Habilidade h2 = sup.suprema();
        h.assertValueEqual(h2.id(), "fortaleza_da_solidao", "a suprema é a Fortaleza da Solidão");
        h.assertTrue(Habilidades.executar(p, sup, h2), "a fortaleza nasce onde ele olha (o chão)");
        h.assertValueEqual(e.energia(), sup.energia().max() * 0.3f, "custa 70% da energia");
        // Um porco em cima de onde seria a parede de trás: o cristal não nasce em cima dele.
        h.spawn(net.minecraft.world.entity.EntityTypes.PIG, new net.minecraft.core.BlockPos(1, 2, 7));
        var base = h.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1));
        var cristal = com.danrod.dcuniverse.content.Blocos.CRISTAL_FORTALEZA.get();
        h.runAfterDelay(Fortaleza.CRESCE + 5, () -> {
            var level = h.getLevel();
            long cristais = forma.stream().filter(b -> level.getBlockState(base.offset(b)).is(cristal)).count();
            h.assertTrue(cristais > 100, "o cristal cresceu (" + cristais + " blocos)");
            h.assertFalse(level.getBlockState(base.offset(0, 0, 6)).is(cristal), "não cresce onde há alguém");
            h.assertTrue(level.getBlockState(base.offset(-1, 0, 6)).is(cristal), "mas cresce do lado");
            var parede = forma.stream().map(base::offset).filter(b -> level.getBlockState(b).is(cristal)).findFirst().orElseThrow();
            h.assertTrue(level.getBlockTicks().hasScheduledTick(parede, cristal), "cada cristal tem hora para sumir");
            // A câmara solar dá 8 de energia por segundo (o jogador de teste não recarrega sozinho).
            h.assertTrue(e.energia() > sup.energia().max() * 0.3f + 10, "a câmara solar recarrega quem está dentro");
            // Limpa para não sobrar cristal perto dos outros testes.
            Fortaleza.limpar();
            for (var b : forma) {
                for (int dy = 0; dy <= 2; dy++) {
                    if (level.getBlockState(base.offset(b).above(dy - 1)).is(cristal)) level.removeBlock(base.offset(b).above(dy - 1), false);
                }
            }
            h.succeed();
        });
    }
}
