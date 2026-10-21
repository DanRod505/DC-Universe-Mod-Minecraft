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
import net.minecraft.core.BlockPos;
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
    private static final Identifier BATMAN = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "batman");

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
        FUNCOES.register("kryptonite", () -> Testes::kryptonita);
        FUNCOES.register("gadgets", () -> Testes::gadgets);
        FUNCOES.register("grapple", () -> Testes::gancho);
        FUNCOES.register("batwing", () -> Testes::batwing);
        FUNCOES.register("detective", () -> Testes::detetive);
        FUNCOES.register("smoke", () -> Testes::fumaca);
        FUNCOES.register("explosive_gel", () -> Testes::gel);
        FUNCOES.register("counter", () -> Testes::contraAtaque);
        FUNCOES.register("batch_items", () -> Testes::loteItens);
        FUNCOES.register("utility_belt", () -> Testes::cinto);
        FUNCOES.register("new_gadgets", () -> Testes::gadgetsNovos);
        FUNCOES.register("freeze_sonic", () -> Testes::batarangues);
        FUNCOES.register("garage", () -> Testes::garagem);
        FUNCOES.register("smallville", () -> Testes::smallville);
        FUNCOES.register("npcs", () -> Testes::npcs);
        FUNCOES.register("pets", () -> Testes::mascotes);
        FUNCOES.register("creative_tabs", () -> Testes::abas);
        FUNCOES.register("prologue", () -> Testes::prologo);
        FUNCOES.register("meteor_shower", () -> Testes::chuvaMeteoros);
        FUNCOES.register("league_communicator", () -> Testes::comunicador);
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
        var hab = new Rede.Sync("x", 1, 2, true, 0, "", List.of(new Rede.HabInfo("a", false, true, 210, 5, 1800)), 0, true, 4.5f, true, false, 32f, 15f, true, false);
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
        h.assertFalse(new Rede.Sync("x", 300, 300, false, 0, "", List.of(), 0, false, 0, false, false, 0, 0, false, false)
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

        // A onda é uma entidade: no tick 0 a seção de entidades do chunk recém-gerado pode ainda não estar
        // carregada, e a entidade nova fica invisível para a busca. Espera alguns ticks.
        h.runAfterDelay(5, () -> {
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
        });
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
     * Efeitos secundários: a visão de calor derrete gelo compactado, põe fogo e acende a fornalha com algo para assar; o sopro prende o mob no gelo
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

        var forno = h.absolutePos(new net.minecraft.core.BlockPos(1, 2, 0));
        level.setBlockAndUpdate(forno, net.minecraft.world.level.block.Blocks.FURNACE.defaultBlockState());
        ((net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity) level.getBlockEntity(forno)).setItem(0, new ItemStack(Items.BEEF));
        com.danrod.dcuniverse.ability.active.Raio.esquentar(level, p,
            new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(forno), net.minecraft.core.Direction.SOUTH, forno, false), true, false);
        h.assertTrue(level.getBlockState(forno).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT), "calor acende a fornalha sem combustível");

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
     * Lote da v0.1: kryptonita drena o kryptoniano e tira a superforça; a Câmara Solar conta como sol mesmo coberta.
     */
    static void kryptonita(GameTestHelper h) {
        ServerPlayer p = jogador(h);
        var aqui = h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2, 1.5));
        p.setPos(aqui.x, aqui.y, aqui.z);
        Transformacao.transformar(p, SUPERMAN, true);
        Personagem sup = EstadoJogador.de(p).personagem();
        var pedra = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        h.assertTrue(com.danrod.dcuniverse.energy.Recargas.porSegundo(p, sup) >= 0, "sem kryptonita a recarga não é negativa");
        h.assertTrue(Naturais.velocidadeMao(p, pedra) > 1, "superforça normal");
        // Fragmento no inventário (um bloco no chão afetaria os testes vizinhos, que rodam a poucos blocos).
        p.getInventory().add(new ItemStack(com.danrod.dcuniverse.content.Itens.FRAGMENTO_KRYPTONITA.get()));
        // A busca guarda o resultado por 1 s: espera passar para ver a mudança.
        h.runAfterDelay(21, () -> {
            h.assertTrue(com.danrod.dcuniverse.energy.Recargas.porSegundo(p, sup) < 0, "kryptonita no inventário drena energia");
            h.assertTrue(Naturais.velocidadeMao(p, pedra) == 0, "com kryptonita perde a superforça");
            p.getInventory().clearContent();
            h.setBlock(new net.minecraft.core.BlockPos(1, 5, 1), net.minecraft.world.level.block.Blocks.STONE);
            h.setBlock(new net.minecraft.core.BlockPos(3, 2, 3), com.danrod.dcuniverse.content.Blocos.CAMARA_SOLAR.get());
            h.runAfterDelay(21, () -> {
                h.assertTrue(com.danrod.dcuniverse.energy.Recargas.sobOSol(p), "a Câmara Solar conta como sol, mesmo coberta");
                h.assertTrue(com.danrod.dcuniverse.energy.Recargas.porSegundo(p, sup) > sup.energia().regen(), "e soma a recarga solar");
                h.succeed();
            });
        });
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

    /** Gadgets: cada uso gasta uma carga, sem cargas não usa, o cinto fabrica gastando energia; no criativo é livre. */
    static void gadgets(GameTestHelper h) {
        ServerPlayer p = jogador(h);
        h.assertTrue(Transformacao.transformar(p, BATMAN, true), "transformar no Batman");
        EstadoJogador e = EstadoJogador.de(p);
        Personagem bat = e.personagem();
        Personagem.Habilidade b = bat.habilidade("batarangue");
        int max = com.danrod.dcuniverse.ability.Cargas.max(b);
        h.assertValueEqual(max, 6, "batarangue com 6 cargas");
        h.assertValueEqual(com.danrod.dcuniverse.ability.Cargas.atuais(p, b), max, "começa cheio");
        e.setEnergia(bat.energia().max());
        for (int i = 0; i < max; i++) {
            e.setProntaEm(b.id(), 0);
            h.assertTrue(Habilidades.executar(p, bat, b), "arremesso " + (i + 1));
        }
        h.assertValueEqual(com.danrod.dcuniverse.ability.Cargas.atuais(p, b), 0, "cargas acabaram");
        e.setProntaEm(b.id(), 0);
        h.assertFalse(Habilidades.executar(p, bat, b), "sem cargas não arremessa");
        // Fabricação: a próxima carga sai na hora marcada, gastando reload_cost de energia.
        com.danrod.dcuniverse.ability.Cargas.tick(p, bat);
        h.assertTrue(com.danrod.dcuniverse.ability.Cargas.faltam(p, b) > 0, "o cinto começou a fabricar");
        e.setProximaCarga(b.id(), p.level().getGameTime());
        float antes = e.energia();
        com.danrod.dcuniverse.ability.Cargas.tick(p, bat);
        h.assertValueEqual(com.danrod.dcuniverse.ability.Cargas.atuais(p, b), 1, "uma carga fabricada");
        h.assertValueEqual(e.energia(), antes - 2f, "custou 2 de energia");
        // Criativo: não gasta carga nem energia.
        ServerPlayer c = (ServerPlayer) h.makeMockServerPlayer(GameType.CREATIVE);
        Transformacao.transformar(c, BATMAN, true);
        EstadoJogador ec = EstadoJogador.de(c);
        ec.setCargas(b.id(), 1);
        h.assertTrue(Habilidades.executar(c, bat, b), "arremessa no criativo");
        h.assertValueEqual(com.danrod.dcuniverse.ability.Cargas.atuais(c, b), 1, "criativo não gasta carga");
        Personagem.Habilidade g = bat.habilidade("gancho");
        float energia = ec.energia();
        com.danrod.dcuniverse.ability.active.Gancho.limpar();
        h.assertTrue(Habilidades.executar(c, bat, g), "gancho no criativo");
        h.assertValueEqual(ec.energia(), energia, "criativo não gasta energia");
        com.danrod.dcuniverse.ability.active.Gancho.limpar();
        h.getLevel().getEntitiesOfClass(com.danrod.dcuniverse.content.BatarangueEntidade.class, p.getBoundingBox().inflate(64)).forEach(x -> x.discard());
        h.succeed();
    }

    /** Vira o jogador de teste para o ponto (lookAt manda pacote, e o jogador de teste não tem conexão). */
    private static void mirar(ServerPlayer p, net.minecraft.world.phys.Vec3 alvo) {
        net.minecraft.world.phys.Vec3 d = alvo.subtract(p.getEyePosition());
        p.setYRot((float) (Math.atan2(-d.x, d.z) * 180 / Math.PI));
        p.setXRot((float) (-Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 180 / Math.PI));
        p.setYHeadRot(p.getYRot());
    }

    private static List<String> ids(List<Personagem.Habilidade> habs) {
        return habs.stream().map(Personagem.Habilidade::id).toList();
    }

    /**
     * Cinto: só os gadgets dos espaços vão para o menu (e para o HUD); trocar no Batcomputador troca de lugar, tira e
     * mantém a habilidade escolhida; a escolha sobrevive à destransformação; só o que está no cinto é fabricado.
     */
    static void cinto(GameTestHelper h) {
        ServerPlayer p = jogador(h);
        h.assertTrue(Transformacao.transformar(p, BATMAN, true), "transformar no Batman");
        EstadoJogador e = EstadoJogador.de(p);
        Personagem bat = e.personagem();
        var pos = h.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1));
        h.assertValueEqual(com.danrod.dcuniverse.character.Cinto.gadgets(bat).size(), 9, "9 gadgets no arsenal");
        h.assertValueEqual(ids(com.danrod.dcuniverse.character.Cinto.equipadas(p, bat)), List.of("gancho", "modo_detetive", "batarangue",
            "batarangue_explosivo", "batarangue_eletrico", "bomba_fumaca", "gel_explosivo", "batwing"), "cinto padrão");
        h.assertValueEqual(Rede.montar(p).habilidades().stream().map(Rede.HabInfo::id).toList(),
            ids(com.danrod.dcuniverse.character.Cinto.equipadas(p, bat)), "o HUD recebe só o que está equipado");
        // Escolhe a fumaça e troca o espaço 1: a escolha continua na fumaça.
        Habilidades.selecionar(p, 5);
        com.danrod.dcuniverse.content.Batcomputador.equipar(p, pos, 0, "lancador_rede");
        h.assertValueEqual(com.danrod.dcuniverse.character.Cinto.slots(p, bat).get(0), "lancador_rede", "rede no espaço 1");
        h.assertFalse(com.danrod.dcuniverse.character.Cinto.equipada(p, bat, "batarangue"), "batarangue foi para o Batcomputador");
        var habs = com.danrod.dcuniverse.character.Cinto.equipadas(p, bat);
        h.assertValueEqual(habs.get(e.selecionada()).id(), "bomba_fumaca", "continua escolhida a fumaça");
        h.assertTrue(Guia.concluida(p, "cinto"), "tarefa do cinto");
        // Pôr a rede no espaço 2 troca os dois de lugar; "" esvazia; o que não é gadget não entra.
        com.danrod.dcuniverse.content.Batcomputador.equipar(p, pos, 1, "lancador_rede");
        var slots = com.danrod.dcuniverse.character.Cinto.slots(p, bat);
        h.assertValueEqual(slots.subList(0, 2), List.of("batarangue_explosivo", "lancador_rede"), "trocaram de lugar");
        com.danrod.dcuniverse.content.Batcomputador.equipar(p, pos, 1, "");
        h.assertValueEqual(com.danrod.dcuniverse.character.Cinto.equipadas(p, bat).size(), 7, "espaço vazio sai do menu");
        h.assertFalse(com.danrod.dcuniverse.character.Cinto.equipar(p, bat, 1, "batwing"), "a suprema não vai no cinto");
        h.assertFalse(com.danrod.dcuniverse.character.Cinto.equipar(p, bat, 1, "gancho"), "o gancho é fixo");
        h.assertFalse(com.danrod.dcuniverse.character.Cinto.equipar(p, bat, 9, "disruptor"), "espaço que não existe");
        com.danrod.dcuniverse.content.Batcomputador.equipar(p, pos, 1, "disruptor");
        // Sobrevive à destransformação.
        Transformacao.destransformar(p);
        Transformacao.transformar(p, BATMAN, true);
        bat = EstadoJogador.de(p).personagem();
        h.assertValueEqual(com.danrod.dcuniverse.character.Cinto.slots(p, bat).get(1), "disruptor", "cinto guardado");
        // Só o que está no cinto é fabricado.
        e = EstadoJogador.de(p);
        e.setCargas("batarangue", 0);
        e.setCargas("disruptor", 0);
        com.danrod.dcuniverse.ability.Cargas.tick(p, bat);
        h.assertValueEqual(e.proximaCarga("batarangue"), 0L, "fora do cinto não fabrica");
        h.assertTrue(e.proximaCarga("disruptor") > 0, "no cinto fabrica");
        com.danrod.dcuniverse.character.Cinto.resetar(p);
        h.assertValueEqual(com.danrod.dcuniverse.character.Cinto.slots(p, bat).get(0), "batarangue", "resetar volta ao padrão");
        h.succeed();
    }

    /**
     * Gadgets novos: o disruptor desarma o esqueleto e tira o pavio do creeper na hora; a rede prende o mob em teias
     * (que somem sem deixar linha); o congelante e o sônico ficam em Testes#batarangues.
     */
    static void gadgetsNovos(GameTestHelper h) {
        ServerPlayer p = jogador(h);
        var aqui = h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 1, 1.5));
        p.setPos(aqui.x, aqui.y, aqui.z);
        p.setYRot(0);
        p.setXRot(0);
        Transformacao.transformar(p, BATMAN, true);
        EstadoJogador e = EstadoJogador.de(p);
        Personagem bat = e.personagem();
        e.setEnergia(bat.energia().max());
        // Disruptor no esqueleto: o arco queima.
        var esq = h.spawn(net.minecraft.world.entity.EntityTypes.SKELETON, new net.minecraft.core.BlockPos(1, 1, 6));
        esq.setNoAi(true);
        esq.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        mirar(p, esq.getBoundingBox().getCenter());
        h.assertTrue(Habilidades.executar(p, bat, bat.habilidade("disruptor")), "disruptor dispara");
        h.assertTrue(esq.getMainHandItem().isEmpty(), "esqueleto desarmado");
        esq.discard();
        // Disruptor no creeper: sem pavio.
        var creeper = h.spawn(net.minecraft.world.entity.EntityTypes.CREEPER, new net.minecraft.core.BlockPos(1, 1, 6));
        creeper.setNoAi(true);
        mirar(p, creeper.getBoundingBox().getCenter());
        e.setProntaEm("disruptor", 0);
        h.assertTrue(Habilidades.executar(p, bat, bat.habilidade("disruptor")), "disruptor no creeper");
        h.assertTrue(com.danrod.dcuniverse.ability.active.Disruptor.semPavio(creeper), "creeper sem pavio");
        creeper.discard();
        e.setProntaEm("disruptor", 0);
        h.assertFalse(Habilidades.executar(p, bat, bat.habilidade("disruptor")), "sem alvo não gasta");
        // Rede no vindicador parado.
        var alvo = h.spawn(net.minecraft.world.entity.EntityTypes.VINDICATOR, new net.minecraft.core.BlockPos(1, 1, 5));
        alvo.setNoAi(true);
        mirar(p, alvo.getBoundingBox().getCenter());
        h.assertTrue(Habilidades.executar(p, bat, bat.habilidade("lancador_rede")), "lança a rede");
        h.succeedWhen(() -> {
            var pe = alvo.blockPosition();
            h.assertTrue(h.getLevel().getBlockState(pe).is(net.minecraft.world.level.block.Blocks.COBWEB), "mob preso na teia");
            h.assertTrue(com.danrod.dcuniverse.content.Teias.temporaria(h.getLevel(), pe), "teia temporária");
            com.danrod.dcuniverse.content.Teias.tirar(h.getLevel(), pe);
            h.assertTrue(h.getLevel().getBlockState(pe).isAir(), "teia saiu sem deixar nada");
            alvo.discard();
        });
    }

    /** Congelante e sônico, separados para não cruzar com a rede. */
    static void batarangues(GameTestHelper h) {
        ServerPlayer p = jogador(h);
        var aqui = h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 1, 1.5));
        p.setPos(aqui.x, aqui.y, aqui.z);
        Transformacao.transformar(p, BATMAN, true);
        EstadoJogador e = EstadoJogador.de(p);
        Personagem bat = e.personagem();
        e.setEnergia(bat.energia().max());
        var alvo = h.spawn(net.minecraft.world.entity.EntityTypes.VINDICATOR, new net.minecraft.core.BlockPos(1, 1, 5));
        alvo.setNoAi(true);
        mirar(p, alvo.getBoundingBox().getCenter());
        h.assertTrue(Habilidades.executar(p, bat, bat.habilidade("batarangue_congelante")), "arremessa o congelante");
        h.setBlock(1, 2, 9, net.minecraft.world.level.block.Blocks.GLASS);
        boolean[] sonico = {false};
        h.succeedWhen(() -> {
            if (!sonico[0]) {
                h.assertTrue(com.danrod.dcuniverse.ability.Congelamento.congelado(alvo), "mob no gelo");
                sonico[0] = true;
                alvo.discard();
                mirar(p, h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2.5, 9.5)));
                h.assertTrue(Habilidades.executar(p, bat, bat.habilidade("batarangue_sonico")), "arremessa o sônico");
            }
            h.assertBlockPresent(net.minecraft.world.level.block.Blocks.AIR, 1, 2, 9);
        });
    }

    /** Gancho: crava no bloco mirado e solta ao usar de novo; o batarangue sai voando. */
    static void gancho(GameTestHelper h) {
        ServerPlayer p = jogador(h);
        h.setBlock(1, 8, 1, net.minecraft.world.level.block.Blocks.STONE);
        var aqui = h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 1, 1.5));
        p.setPos(aqui.x, aqui.y, aqui.z);
        p.setXRot(-90);
        Transformacao.transformar(p, BATMAN, true);
        EstadoJogador e = EstadoJogador.de(p);
        Personagem bat = e.personagem();
        e.setEnergia(bat.energia().max());
        h.assertTrue(Habilidades.executar(p, bat, bat.habilidade("gancho")), "dispara o gancho para cima");
        h.assertTrue(com.danrod.dcuniverse.ability.active.Gancho.ativo(p), "cabo preso no bloco");
        h.assertTrue(Guia.concluida(p, "gancho"), "tarefa do gancho");
        com.danrod.dcuniverse.ability.active.Gancho.soltar(p);
        h.assertFalse(com.danrod.dcuniverse.ability.active.Gancho.ativo(p), "soltou o cabo");
        p.setXRot(0);
        h.assertTrue(Habilidades.executar(p, bat, bat.habilidade("batarangue")), "arremessa o batarangue");
        var lista = h.getLevel().getEntitiesOfClass(com.danrod.dcuniverse.content.BatarangueEntidade.class, p.getBoundingBox().inflate(8));
        h.assertValueEqual(lista.size(), 1, "um batarangue no ar");
        h.assertTrue(Guia.concluida(p, "batarangue"), "tarefa do batarangue");
        lista.forEach(x -> x.discard());
        h.succeed();
    }

    /** Batwing: a suprema invoca o jato do dono, ele embarca e o jato vai embora quando o Batman destransforma. */
    static void batwing(GameTestHelper h) {
        ServerPlayer p = jogador(h);
        var aqui = h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2, 1.5));
        p.setPos(aqui.x, aqui.y, aqui.z);
        Transformacao.transformar(p, BATMAN, true);
        EstadoJogador e = EstadoJogador.de(p);
        Personagem bat = e.personagem();
        e.setEnergia(bat.energia().max());
        Personagem.Habilidade sup = bat.suprema();
        h.assertValueEqual(sup.id(), "batwing", "a suprema é o Batwing");
        h.assertTrue(Habilidades.executar(p, bat, sup), "invoca o Batwing");
        h.assertValueEqual(e.energia(), bat.energia().max() * 0.25f, "custa 75% da energia");
        var bw = (com.danrod.dcuniverse.content.BatwingEntidade) com.danrod.dcuniverse.ability.active.Veiculo.de(p);
        h.assertTrue(bw != null && bw.isAlive(), "Batwing no mundo");
        // (Embarcar teleporta o jogador, o que o jogador de teste não suporta: testado no jogo.)
        Transformacao.destransformar(p);
        h.assertValueEqual(bw.estado(), com.danrod.dcuniverse.content.BatwingEntidade.INDO, "o Batwing vai embora");
        bw.discard();
        com.danrod.dcuniverse.ability.active.Veiculo.limpar();
        h.succeed();
    }

    /**
     * Garagem: o veículo escolhido no Batcomputador é o que a suprema chama; chamar de novo o mesmo não gasta nada;
     * o Batbarco precisa de água funda e, quando vem, o veículo antigo vai embora.
     */
    static void garagem(GameTestHelper h) {
        ServerPlayer p = jogador(h);
        var aqui = h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2, 1.5));
        p.setPos(aqui.x, aqui.y, aqui.z);
        Transformacao.transformar(p, BATMAN, true);
        EstadoJogador e = EstadoJogador.de(p);
        Personagem bat = e.personagem();
        h.assertTrue(com.danrod.dcuniverse.character.Cinto.escolherVeiculo(p, bat, "batmoto"), "escolhe a Batmoto");
        h.assertFalse(com.danrod.dcuniverse.character.Cinto.escolherVeiculo(p, bat, "gancho"), "o gancho não é veículo");
        Personagem.Habilidade sup = com.danrod.dcuniverse.character.Cinto.suprema(p, bat);
        h.assertValueEqual(sup.id(), "batmoto", "a suprema agora é a Batmoto");
        var equipadas = com.danrod.dcuniverse.character.Cinto.equipadas(p, bat);
        h.assertValueEqual(equipadas.get(equipadas.size() - 1).id(), "batmoto", "no menu radial vai a Batmoto");
        e.setEnergia(bat.energia().max());
        h.assertTrue(Habilidades.executar(p, bat, sup), "chama a Batmoto");
        var moto = com.danrod.dcuniverse.ability.active.Veiculo.de(p);
        h.assertTrue(moto instanceof com.danrod.dcuniverse.content.VeiculoBat v && v.modelo().equals("batmoto"), "Batmoto no mundo");
        float depois = e.energia();
        h.assertTrue(depois < bat.energia().max(), "a primeira chamada custa");
        Habilidades.executar(p, bat, sup);
        h.assertValueEqual(e.energia(), depois, "chamar de novo a mesma não custa");
        // Batbarco sem água: não vem, e a Batmoto fica.
        com.danrod.dcuniverse.character.Cinto.escolherVeiculo(p, bat, "batbarco");
        Personagem.Habilidade barco = com.danrod.dcuniverse.character.Cinto.suprema(p, bat);
        e.setProntaEm(barco.id(), 0);
        e.setEnergia(bat.energia().max());
        h.assertFalse(Habilidades.executar(p, bat, barco), "sem água o Batbarco não vem");
        h.assertValueEqual(e.energia(), bat.energia().max(), "e não gasta");
        h.assertTrue(com.danrod.dcuniverse.ability.active.Veiculo.de(p) == moto, "a Batmoto continua");
        // Com um lago do lado, ele emerge e a Batmoto vai embora.
        for (int x = 3; x <= 7; x++)
            for (int z = 0; z <= 4; z++)
                for (int y = -2; y <= 0; y++) h.setBlock(x, 1 + y, z, net.minecraft.world.level.block.Blocks.WATER);
        h.assertTrue(Habilidades.executar(p, bat, barco), "com água o Batbarco vem");
        var b = com.danrod.dcuniverse.ability.active.Veiculo.de(p);
        h.assertTrue(b instanceof com.danrod.dcuniverse.content.VeiculoBat v && v.modelo().equals("batbarco"), "Batbarco no mundo");
        h.assertValueEqual(((com.danrod.dcuniverse.content.VeiculoDono) moto).estado(), com.danrod.dcuniverse.content.VeiculoDono.INDO, "a Batmoto vai embora");
        Transformacao.destransformar(p);
        h.assertValueEqual(((com.danrod.dcuniverse.content.VeiculoDono) b).estado(), com.danrod.dcuniverse.content.VeiculoDono.INDO, "o Batbarco vai embora");
        moto.discard();
        b.discard();
        com.danrod.dcuniverse.character.Cinto.resetar(p);
        com.danrod.dcuniverse.ability.active.Veiculo.limpar();
        h.succeed();
    }


    /** Modo Detetive: liga gastando energia, drena por segundo, desliga de graça e ao destransformar. */
    static void detetive(GameTestHelper h) {
        ServerPlayer p = jogador(h);
        Transformacao.transformar(p, BATMAN, true);
        EstadoJogador e = EstadoJogador.de(p);
        Personagem bat = e.personagem();
        Personagem.Habilidade d = bat.habilidade("modo_detetive");
        e.setEnergia(100);
        h.assertTrue(Habilidades.executar(p, bat, d), "liga o modo detetive");
        h.assertTrue(e.detetive(), "ligado");
        h.assertValueEqual(e.energia(), 95f, "ligar custa 5");
        h.assertValueEqual(com.danrod.dcuniverse.core.Rede.montarAparencia(p).efeito(), "detective_vision", "lentes acesas para os outros");
        for (int i = 0; i < 20; i++) com.danrod.dcuniverse.ability.active.Detetive.tick(p, bat);
        h.assertTrue(Math.abs(e.energia() - 93.5f) < 0.01f, "drena 1,5 por segundo");
        h.assertTrue(Habilidades.executar(p, bat, d), "desliga mesmo em recarga");
        h.assertFalse(e.detetive(), "desligado");
        h.assertTrue(Math.abs(e.energia() - 93.5f) < 0.01f, "desligar não custa");
        e.setProntaEm(d.id(), 0);
        Habilidades.executar(p, bat, d);
        e.setEnergia(0.01f);
        com.danrod.dcuniverse.ability.active.Detetive.tick(p, bat);
        h.assertFalse(e.detetive(), "sem energia desliga sozinho");
        e.setEnergia(100);
        e.setProntaEm(d.id(), 0);
        Habilidades.executar(p, bat, d);
        Transformacao.destransformar(p);
        h.assertFalse(e.detetive(), "destransformar desliga");
        h.assertTrue(Guia.concluida(p, "detetive"), "tarefa do modo detetive");
        h.succeed();
    }

    /** Bomba de fumaça: cega e tira o alvo do mob dentro da nuvem; quem jogou fica invisível e rápido. */
    static void fumaca(GameTestHelper h) {
        ServerPlayer p = jogador(h);
        var aqui = h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 1, 1.5));
        p.setPos(aqui.x, aqui.y, aqui.z);
        Transformacao.transformar(p, BATMAN, true);
        EstadoJogador e = EstadoJogador.de(p);
        Personagem bat = e.personagem();
        e.setEnergia(bat.energia().max());
        var zumbi = h.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, new net.minecraft.core.BlockPos(3, 1, 1));
        zumbi.setTarget(p);
        h.assertTrue(Habilidades.executar(p, bat, bat.habilidade("bomba_fumaca")), "estoura a fumaça");
        h.assertTrue(zumbi.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS), "mob cego");
        h.assertTrue(zumbi.getTarget() == null, "mob perdeu o alvo");
        h.assertValueEqual(com.danrod.dcuniverse.ability.Cargas.atuais(p, bat.habilidade("bomba_fumaca")), 2, "gastou uma carga");
        h.assertTrue(Guia.concluida(p, "fumaca"), "tarefa da fumaça");
        zumbi.discard();
        com.danrod.dcuniverse.ability.active.Fumaca.limpar();
        h.succeed();
    }

    /** Gel explosivo: cola na parede mirada, agachado detona e o bloco quebra; obsidiana resiste. */
    static void gel(GameTestHelper h) {
        ServerPlayer p = jogador(h);
        var aqui = h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 1, 1.5));
        p.setPos(aqui.x, aqui.y, aqui.z);
        p.setYRot(0);
        p.setXRot(0);
        // Corredor limpo até a parede (os testes vizinhos podem deixar blocos por perto).
        for (int x = 0; x <= 2; x++) for (int y = 1; y <= 3; y++) for (int z = 1; z <= 3; z++) h.setBlock(x, y, z, net.minecraft.world.level.block.Blocks.AIR);
        h.setBlock(1, 2, 4, net.minecraft.world.level.block.Blocks.STONE);
        h.setBlock(1, 2, 5, net.minecraft.world.level.block.Blocks.OBSIDIAN);
        Transformacao.transformar(p, BATMAN, true);
        EstadoJogador e = EstadoJogador.de(p);
        Personagem bat = e.personagem();
        e.setEnergia(bat.energia().max());
        Personagem.Habilidade g = bat.habilidade("gel_explosivo");
        h.assertTrue(Habilidades.executar(p, bat, g), "cola o gel na pedra");
        h.assertValueEqual(com.danrod.dcuniverse.content.GelEntidade.level(p).size(), 1, "um gel colado");
        p.setShiftKeyDown(true);
        h.assertTrue(Habilidades.executar(p, bat, g), "agachado detona (mesmo em recarga)");
        p.setShiftKeyDown(false);
        h.assertTrue(Guia.concluida(p, "gel") && Guia.concluida(p, "gel_detonar"), "tarefas do gel");
        h.runAfterDelay(com.danrod.dcuniverse.content.GelEntidade.PAVIO + 4, () -> {
            h.assertBlockPresent(net.minecraft.world.level.block.Blocks.AIR, 1, 2, 4);
            h.assertBlockPresent(net.minecraft.world.level.block.Blocks.OBSIDIAN, 1, 2, 5);
            h.succeed();
        });
    }

    /** Contra-ataque: agachar na hora anula o golpe e devolve; fora da janela o golpe entra. */
    static void contraAtaque(GameTestHelper h) {
        ServerPlayer p = jogador(h);
        var aqui = h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 1, 1.5));
        p.setPos(aqui.x, aqui.y, aqui.z);
        Transformacao.transformar(p, BATMAN, true);
        Personagem bat = EstadoJogador.de(p).personagem();
        var zumbi = h.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, new net.minecraft.core.BlockPos(2, 1, 1));
        float vidaZumbi = zumbi.getHealth();
        p.setShiftKeyDown(true);
        // Só a passiva do contra-ataque (a visão noturna manda pacote, que o jogador de teste não tem).
        com.danrod.dcuniverse.ability.TiposPassiva.TIPOS.get("counter").tick(p, bat.passiva("counter").params());
        // (O jogador de teste não pode levar dano: chama direto o que o LivingAttackEvent chama.)
        h.assertTrue(com.danrod.dcuniverse.ability.passive.ContraAtaque.defender(p, zumbi, zumbi), "golpe anulado");
        h.assertTrue(zumbi.getHealth() < vidaZumbi, "devolveu o golpe");
        h.assertTrue(zumbi.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS), "atordoado");
        h.assertTrue(Guia.concluida(p, "contra_ataque"), "tarefa do contra-ataque");
        // Segundo golpe sem novo agachar: a janela já fechou.
        h.assertFalse(com.danrod.dcuniverse.ability.passive.ContraAtaque.defender(p, zumbi, zumbi), "fora da janela o golpe entra");
        zumbi.discard();
        h.succeed();
    }

    /**
     * Lote de itens da v0.2: prométio no mundo e na fornalha, Batcomputador (fabricar do inventário, análise de
     * amostra com família, recarga do cinto) e a Capa Planadora dando o planar sem transformação.
     */
    static void loteItens(GameTestHelper h) {
        var level = h.getLevel();
        var minerio = net.minecraft.resources.ResourceKey.create(Registries.PLACED_FEATURE,
            Identifier.fromNamespaceAndPath(DCUniverse.MODID, "minerio_prometio"));
        h.assertTrue(level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).get(minerio).isPresent(), "veios de prométio no mundo");
        var rm = level.getServer().getRecipeManager();
        for (String r : List.of("lingote_prometio", "lingote_prometio_alto_forno", "batcomputador", "carregador_tecnologico", "cinto_batman")) {
            h.assertTrue(rm.byKey(net.minecraft.resources.ResourceKey.create(Registries.RECIPE,
                Identifier.fromNamespaceAndPath(DCUniverse.MODID, r))).isPresent(), "receita " + r);
        }
        ServerPlayer p = jogador(h);
        var aqui = h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 1, 1.5));
        p.setPos(aqui.x, aqui.y, aqui.z);
        var pos = h.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1));
        // Fabricar: o Componente Tecnológico sai direto do inventário, gastando só o que a receita pede.
        var inv = p.getInventory();
        inv.add(new ItemStack(Items.IRON_INGOT, 6));
        inv.add(new ItemStack(Itens.PLACA_CIRCUITO.get(), 2));
        inv.add(new ItemStack(Items.REDSTONE, 2));
        inv.add(new ItemStack(Items.COMPARATOR));
        com.danrod.dcuniverse.content.Batcomputador.fabricar(p, pos, "componente_tecnologico");
        h.assertTrue(inv.countItem(Itens.COMPONENTE_TECNOLOGICO.get()) == 1, "fabricou o componente");
        h.assertTrue(inv.countItem(Items.IRON_INGOT) == 2 && inv.countItem(Items.COMPARATOR) == 0, "gastou os ingredientes");
        com.danrod.dcuniverse.content.Batcomputador.fabricar(p, pos, "componente_tecnologico");
        h.assertTrue(inv.countItem(Itens.COMPONENTE_TECNOLOGICO.get()) == 1, "sem ingredientes não fabrica");
        // Análise: carne podre registra o zumbi, e a família inteira (o zumbi do deserto também) fica analisada.
        inv.add(new ItemStack(Items.ROTTEN_FLESH));
        com.danrod.dcuniverse.content.Batcomputador.analisar(p, pos, "minecraft:rotten_flesh");
        h.assertTrue(com.danrod.dcuniverse.content.Batcomputador.analisados(p).contains("minecraft:zombie"), "zumbi analisado");
        h.assertTrue(inv.countItem(Items.ROTTEN_FLESH) == 0, "a amostra foi usada");
        h.assertTrue(com.danrod.dcuniverse.content.Batcomputador.familia(net.minecraft.world.entity.EntityTypes.HUSK)
            == net.minecraft.world.entity.EntityTypes.ZOMBIE, "família do zumbi");
        h.assertTrue(Guia.concluida(p, "batcomputador"), "tarefa do Batcomputador");
        // Capa Planadora: sem herói, só plana com a capa vestida.
        h.assertTrue(Rede.montar(p).planar() == 0, "sem capa não plana");
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Itens.CAPA_PLANADOR.get()));
        h.assertTrue(Rede.montar(p).planar() == com.danrod.dcuniverse.content.Equipamentos.CAPA_VELOCIDADE, "a capa dá o planar");
        p.setOnGround(false);
        com.danrod.dcuniverse.ability.passive.Planar.definir(p, com.danrod.dcuniverse.ability.passive.Planar.ABERTA);
        h.assertTrue(EstadoJogador.de(p).planar() == com.danrod.dcuniverse.ability.passive.Planar.ABERTA, "abriu a capa no ar");
        com.danrod.dcuniverse.ability.passive.Planar.definir(p, com.danrod.dcuniverse.ability.passive.Planar.FECHADA);
        p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        p.setOnGround(false);
        com.danrod.dcuniverse.ability.passive.Planar.definir(p, com.danrod.dcuniverse.ability.passive.Planar.ABERTA);
        h.assertTrue(EstadoJogador.de(p).planar() == com.danrod.dcuniverse.ability.passive.Planar.FECHADA, "sem a capa não abre");
        // Recarga do Batcomputador: enche a energia do Batman na hora, e só de novo depois do intervalo.
        Transformacao.transformar(p, BATMAN, true);
        Personagem bat = EstadoJogador.de(p).personagem();
        EstadoJogador.de(p).setEnergia(0);
        com.danrod.dcuniverse.content.Batcomputador.recarregar(p, pos);
        h.assertTrue(EstadoJogador.de(p).energia() == bat.energia().max(), "energia cheia");
        EstadoJogador.de(p).setEnergia(0);
        com.danrod.dcuniverse.content.Batcomputador.recarregar(p, pos);
        h.assertTrue(EstadoJogador.de(p).energia() == 0, "só recarrega de novo depois do intervalo");
        com.danrod.dcuniverse.content.Batcomputador.resetar(p);
        h.succeed();
    }

    /**
     * Smallville (v0.3): cada planta do local constrói sem erro num canto longe do teste, a nave de Kal-El fica
     * no porão do celeiro, a cratera tem o baú do meteoro, as placas recebem texto e a chegada marca a tarefa.
     */
    static void smallville(GameTestHelper h) {
        var level = h.getLevel();
        net.minecraft.core.BlockPos base = h.absolutePos(net.minecraft.core.BlockPos.ZERO).offset(0, 0, 3000);
        int i = 0;
        java.util.Map<String, net.minecraft.world.level.levelgen.structure.BoundingBox> caixas = new java.util.HashMap<>();
        for (var e : new java.util.TreeMap<>(com.danrod.dcuniverse.world.estrutura.Estruturas.PLANTAS).entrySet()) {
            if (!e.getKey().startsWith("smallville/")) continue;
            var p = e.getValue();
            net.minecraft.core.BlockPos origem = base.offset(i++ * 160, 0, 0);
            var caixa = new net.minecraft.world.level.levelgen.structure.BoundingBox(origem.getX() - 6, origem.getY() - p.abaixo() - 2, origem.getZ() - 6,
                origem.getX() + p.largura() + 6, origem.getY() + p.altura() + 2, origem.getZ() + p.fundo() + 6);
            p.construir(new com.danrod.dcuniverse.world.estrutura.Obra(level, caixa, origem, net.minecraft.world.level.block.Rotation.NONE, 42L));
            caixas.put(e.getKey(), caixa);
        }
        h.assertTrue(caixas.size() >= 20, "todas as plantas de Smallville construíram (" + caixas.size() + ")");
        h.assertTrue(achar(level, caixas.get("smallville/celeiro"), b -> b.is(com.danrod.dcuniverse.content.Blocos.NAVE_KALEL.get())), "nave de Kal-El no porão do celeiro");
        h.assertTrue(achar(level, caixas.get("smallville/cratera"), b -> b.is(com.danrod.dcuniverse.content.Blocos.MINERIO_KRYPTONITA.get())), "kryptonita na cratera");
        h.assertTrue(achar(level, caixas.get("smallville/cratera"), b -> b.is(net.minecraft.world.level.block.Blocks.CHEST)), "baú do meteoro na cratera");
        boolean placaComTexto = false;
        var cx = caixas.get("smallville/talon");
        for (net.minecraft.core.BlockPos q : net.minecraft.core.BlockPos.betweenClosed(cx.minX(), cx.minY(), cx.minZ(), cx.maxX(), cx.maxY(), cx.maxZ()))
            if (level.getBlockEntity(q) instanceof net.minecraft.world.level.block.entity.SignBlockEntity s
                && s.getFrontText().getMessage(1, false).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                && t.getKey().startsWith("dcuniverse.placa.talon")) placaComTexto = true;
        h.assertTrue(placaComTexto, "placas do Talon com texto traduzível");
        h.succeed();
    }

    private static Identifier dcu(String path) {
        return Identifier.fromNamespaceAndPath(DCUniverse.MODID, path);
    }

    private static com.danrod.dcuniverse.npc.NpcEntidade npc(GameTestHelper h, String id, ServerPlayer p) {
        var d = com.danrod.dcuniverse.npc.Npcs.npc(dcu(id));
        h.assertTrue(d != null, "NPC " + id + " carregado do datapack");
        var tipo = d.slim() ? com.danrod.dcuniverse.content.Entidades.NPC_SLIM.get() : com.danrod.dcuniverse.content.Entidades.NPC.get();
        var n = tipo.create(h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        net.minecraft.core.BlockPos pos = h.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2));
        n.configurar(d, pos, "");
        n.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
        h.getLevel().addFreshEntity(n);
        p.snapTo(pos.getX() + 1.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
        return n;
    }

    /**
     * Missões do Prólogo (v0.3 etapa 3): a ordem (Meteoro abre O Primeiro Emblema e Colheita em Família), a afinidade
     * extra com os cidadãos, fabricar pela tag de transformação de nível 1, transformar e o Comunicador de recompensa.
     */
    static void prologo(GameTestHelper h) {
        ServerPlayer p = jogador(h);
        Identifier meteoro = dcu("meteoro_smallville"), emblema = dcu("primeiro_emblema"), colheita = dcu("colheita_familia");
        var mEmblema = com.danrod.dcuniverse.npc.Npcs.missao(emblema);
        h.assertTrue(com.danrod.dcuniverse.npc.Npcs.missao(meteoro) != null && mEmblema != null && com.danrod.dcuniverse.npc.Npcs.missao(colheita) != null, "missões do Prólogo carregadas");
        h.assertValueEqual(mEmblema.capitulo(), "prologo", "capítulo do Prólogo");
        h.assertTrue(com.danrod.dcuniverse.missao.Diario.disponivel(p, com.danrod.dcuniverse.npc.Npcs.missao(meteoro)), "Meteoro em Smallville abre a campanha");
        h.assertTrue(!com.danrod.dcuniverse.missao.Diario.disponivel(p, mEmblema), "O Primeiro Emblema pede o Meteoro antes");
        h.assertTrue(com.danrod.dcuniverse.missao.Diario.disponiveis(p).contains(meteoro.toString()), "Martha mostra ! com o Meteoro");
        com.danrod.dcuniverse.missao.Diario.aceitar(p, meteoro);
        com.danrod.dcuniverse.missao.Diario.concluir(p, meteoro);
        h.assertValueEqual(com.danrod.dcuniverse.npc.Afinidade.de(p, "cidadaos"), 5, "Meteoro dá +5 com os cidadãos");
        h.assertTrue(p.getInventory().countItem(com.danrod.dcuniverse.content.Itens.FRAGMENTO_ALIENIGENA.get()) >= 4, "recompensa: fragmentos alienígenas");
        h.assertTrue(com.danrod.dcuniverse.missao.Diario.disponivel(p, mEmblema), "Meteoro libera O Primeiro Emblema");
        h.assertTrue(com.danrod.dcuniverse.missao.Diario.disponivel(p, com.danrod.dcuniverse.npc.Npcs.missao(colheita)), "Meteoro libera a Colheita em Família");

        h.assertTrue(com.danrod.dcuniverse.missao.Diario.aceitar(p, emblema), "aceitar O Primeiro Emblema");
        h.assertTrue(p.getInventory().countItem(com.danrod.dcuniverse.content.Itens.TECIDO_TATICO.get()) >= 2, "Martha dá o tecido ao aceitar");
        com.danrod.dcuniverse.missao.Diario.fabricou(p, new ItemStack(com.danrod.dcuniverse.content.Itens.CINTO_BATMAN.get()));
        h.assertValueEqual(com.danrod.dcuniverse.missao.Diario.ativas(p).get(emblema).objetivo(), 0, "o cinto não conta antes do núcleo");
        com.danrod.dcuniverse.missao.Diario.fabricou(p, new ItemStack(com.danrod.dcuniverse.content.Itens.NUCLEO_META_1.get()));
        com.danrod.dcuniverse.missao.Diario.fabricou(p, new ItemStack(com.danrod.dcuniverse.content.Itens.CRISTAL_SUPERMAN.get()));
        h.assertValueEqual(com.danrod.dcuniverse.missao.Diario.ativas(p).get(emblema).objetivo(), 1, "o cristal do Superman não é de nível 1");
        com.danrod.dcuniverse.missao.Diario.fabricou(p, new ItemStack(com.danrod.dcuniverse.content.Itens.CINTO_BATMAN.get()));
        h.assertValueEqual(com.danrod.dcuniverse.missao.Diario.ativas(p).get(emblema).objetivo(), 2, "o cinto vale pela tag de nível 1");
        Transformacao.transformar(p, BATMAN, true);
        com.danrod.dcuniverse.missao.Diario.verificar(p);
        h.assertValueEqual(com.danrod.dcuniverse.missao.Diario.ativas(p).get(emblema).objetivo(), 3, "transformar-se cumpre o objetivo");
        Transformacao.destransformar(p);
        h.assertTrue(com.danrod.dcuniverse.missao.Diario.falou(p, dcu("martha_kent")).isPresent(), "falar com a Martha fecha a missão");
        h.assertTrue(com.danrod.dcuniverse.missao.Diario.concluida(p, emblema), "O Primeiro Emblema concluída");
        h.assertTrue(com.danrod.dcuniverse.liga.Liga.carrega(p), "recompensa: Comunicador da Liga");
        h.assertTrue(!com.danrod.dcuniverse.missao.Diario.disponiveis(p).contains(emblema.toString()), "missão única não volta");
        p.discard();
        h.succeed();
    }

    /** Chuva de meteoros: três meteoros caem em volta do ponto e cada um deixa um Meteorito que solta fragmentos. */
    static void chuvaMeteoros(GameTestHelper h) {
        ServerPlayer p = jogador(h);
        BlockPos centro = h.absolutePos(new BlockPos(1, 1, 1));
        var pontos = com.danrod.dcuniverse.missao.Cenas.chuvaMeteoros(p, centro);
        h.assertValueEqual(pontos.size(), 3, "três meteoros");
        h.runAfterDelay(140, () -> {
            var level = h.getLevel();
            for (BlockPos b : pontos) h.assertTrue(level.getBlockState(b).is(com.danrod.dcuniverse.content.Blocos.METEORITO.get()), "meteorito caiu em " + b);
            var drops = net.minecraft.world.level.block.Block.getDrops(level.getBlockState(pontos.get(0)), level, pontos.get(0), null);
            h.assertTrue(drops.stream().anyMatch(s -> s.is(com.danrod.dcuniverse.content.Itens.FRAGMENTO_ALIENIGENA.get())), "meteorito solta fragmentos alienígenas");
            for (BlockPos b : pontos) level.removeBlock(b, false);
            p.discard();
            h.succeed();
        });
    }

    /** Comunicador da Liga: vínculo dos dois lados (e desfazer), só aparece quem carrega um, e o ponto de encontro. */
    static void comunicador(GameTestHelper h) {
        ServerPlayer a = jogador(h), b = jogador(h);
        h.assertTrue(com.danrod.dcuniverse.liga.Liga.alternar(a, b), "vincular");
        h.assertTrue(com.danrod.dcuniverse.liga.Liga.aliados(a).containsKey(b.getUUID().toString())
            && com.danrod.dcuniverse.liga.Liga.aliados(b).containsKey(a.getUUID().toString()), "vínculo dos dois lados");
        a.getInventory().add(new ItemStack(com.danrod.dcuniverse.content.Itens.COMUNICADOR_LIGA.get()));
        h.assertValueEqual(com.danrod.dcuniverse.liga.Liga.marcar(a, a.blockPosition(), List.of(a, b)), 0, "aliado sem comunicador não recebe o ponto");
        b.getInventory().add(new ItemStack(com.danrod.dcuniverse.content.Itens.COMUNICADOR_LIGA.get()));
        h.assertValueEqual(com.danrod.dcuniverse.liga.Liga.marcar(a, a.blockPosition(), List.of(a, b)), 1, "aliado com comunicador recebe o ponto");
        com.danrod.dcuniverse.liga.Liga.tick(h.getLevel().getServer());
        h.assertTrue(!com.danrod.dcuniverse.liga.Liga.alternar(a, b), "desvincular");
        h.assertTrue(com.danrod.dcuniverse.liga.Liga.aliados(a).isEmpty() && com.danrod.dcuniverse.liga.Liga.aliados(b).isEmpty(), "vínculo desfeito dos dois lados");
        a.discard();
        b.discard();
        h.succeed();
    }

    /**
     * Abas do criativo: montadas com as tags do servidor, todo item do mod (menos as peças do traje, que só existem
     * transformado) aparece em exatamente uma aba, e cada aba do mod tem o item dela.
     */
    static void abas(GameTestHelper h) {
        var params = new net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters(
            h.getLevel().enabledFeatures(), true, h.getLevel().registryAccess());
        java.util.Map<net.minecraft.world.item.Item, String> onde = new java.util.HashMap<>();
        for (var a : com.danrod.dcuniverse.content.Itens.Aba.values()) {
            a.get().buildContents(params);
            h.assertTrue(!a.get().getDisplayItems().isEmpty(), "aba " + a.id + " vazia");
            for (ItemStack s : a.get().getDisplayItems()) {
                String antes = onde.put(s.getItem(), a.id);
                h.assertTrue(antes == null, s.getItem() + " em duas abas: " + antes + " e " + a.id);
            }
        }
        for (var r : com.danrod.dcuniverse.content.Itens.ITEMS.getEntries()) {
            if (r.get() instanceof com.danrod.dcuniverse.suit.TrajeItem) continue;
            h.assertTrue(onde.containsKey(r.get()), "item fora do criativo: " + r.getId());
        }
        h.assertTrue("dcuniverse_blocos".equals(onde.get(com.danrod.dcuniverse.content.Blocos.CAMARA_SOLAR.get().asItem())), "Câmara Solar na aba Blocos");
        h.succeed();
    }

    /**
     * NPCs (v0.3 etapa 2): o datapack de NPCs e missões é coerente, a afinidade sobe por nível, presente vale uma
     * vez por dia, e uma missão completa pelo diálogo (aceitar, coletar, entregar) dá a recompensa.
     */
    static void npcs(GameTestHelper h) {
        var npcs = com.danrod.dcuniverse.npc.Npcs.npcs();
        var missoes = com.danrod.dcuniverse.npc.Npcs.missoes();
        h.assertTrue(npcs.size() >= 7 && missoes.size() >= 10, "NPCs e missões carregados (" + npcs.size() + ", " + missoes.size() + ")");
        for (var m : missoes) {
            h.assertTrue(com.danrod.dcuniverse.npc.Npcs.npc(m.npc()) != null, "quem dá " + m.id() + " existe");
            for (var o : m.objetivos()) {
                if (o.npc() != null) h.assertTrue(com.danrod.dcuniverse.npc.Npcs.npc(o.npc()) != null, "NPC do objetivo de " + m.id());
                if (o.item() != null) h.assertTrue(!o.item().pilha().isEmpty(), "item do objetivo de " + m.id());
            }
            for (var r : m.recompensa().itens()) h.assertTrue(!r.pilha().isEmpty(), "recompensa de " + m.id());
            for (var r : m.requer()) h.assertTrue(com.danrod.dcuniverse.npc.Npcs.missao(r) != null, "requisito de " + m.id());
        }
        for (var d : npcs) {
            for (var t : d.trocas()) h.assertTrue(!t.saida().pilha().isEmpty(), "troca de " + d.id());
            for (String g : d.gostos().keySet()) h.assertTrue(!new com.danrod.dcuniverse.npc.ItemRef(g, 1).pilha().isEmpty(), "presente " + g + " de " + d.id());
        }
        ServerPlayer p = jogador(h);
        var martha = npc(h, "martha_kent", p);
        h.assertValueEqual(com.danrod.dcuniverse.npc.Afinidade.de(p, "martha_kent"), 0, "afinidade começa em zero");
        com.danrod.dcuniverse.npc.Dialogo.abrir(p, martha);
        h.assertTrue(com.danrod.dcuniverse.npc.Afinidade.conhecidos(p).contains("dcuniverse:martha_kent"), "conversar registra o NPC");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.PUMPKIN, 2));
        com.danrod.dcuniverse.npc.Dialogo.escolher(p, martha.getId(), "presente");
        h.assertValueEqual(com.danrod.dcuniverse.npc.Afinidade.de(p, "martha_kent"), 5, "presente que ela gosta dá afinidade");
        com.danrod.dcuniverse.npc.Dialogo.escolher(p, martha.getId(), "presente");
        h.assertValueEqual(com.danrod.dcuniverse.npc.Afinidade.de(p, "martha_kent"), 5, "só um presente por dia");
        h.assertValueEqual(p.getMainHandItem().getCount(), 1, "o segundo presente não é gasto");
        Identifier ovos = dcu("martha_ovos");
        com.danrod.dcuniverse.npc.Dialogo.escolher(p, martha.getId(), "aceitar:" + ovos);
        h.assertTrue(com.danrod.dcuniverse.missao.Diario.ativa(p, ovos), "missão aceita pelo diálogo");
        h.assertValueEqual(com.danrod.dcuniverse.missao.Diario.rastreada(p), ovos.toString(), "a missão aceita passa a ser seguida");
        p.getInventory().add(new ItemStack(Items.EGG, 6));
        com.danrod.dcuniverse.missao.Diario.verificar(p);
        h.assertTrue(com.danrod.dcuniverse.missao.Diario.entregas(p, dcu("martha_kent")).stream().anyMatch(m -> m.id().equals(ovos)), "pronta para entregar");
        com.danrod.dcuniverse.npc.Dialogo.escolher(p, martha.getId(), "entregar:" + ovos);
        h.assertTrue(com.danrod.dcuniverse.missao.Diario.concluida(p, ovos) && !com.danrod.dcuniverse.missao.Diario.ativa(p, ovos), "missão concluída");
        h.assertValueEqual(p.getInventory().countItem(Items.EGG), 0, "ovos entregues");
        h.assertTrue(p.getInventory().countItem(Items.BREAD) >= 4, "recompensa recebida");
        h.assertValueEqual(com.danrod.dcuniverse.npc.Afinidade.de(p, "martha_kent"), 10, "missão dá afinidade");
        h.assertTrue(!com.danrod.dcuniverse.missao.Diario.disponivel(p, com.danrod.dcuniverse.npc.Npcs.missao(ovos)), "repetível só no dia seguinte");
        h.assertTrue(!com.danrod.dcuniverse.missao.Diario.disponivel(p, com.danrod.dcuniverse.npc.Npcs.missao(dcu("martha_streaky"))), "missão trancada por afinidade e requisito");
        com.danrod.dcuniverse.npc.Afinidade.definir(p, "cidadaos", 80);
        h.assertValueEqual(com.danrod.dcuniverse.npc.Afinidade.nivel(80), 3, "80 é Confidente");
        h.assertValueEqual(com.danrod.dcuniverse.npc.DefNpc.preco(12, 80), 9, "Confidente paga 25% menos");
        h.assertTrue(martha.hurtServer(h.getLevel(), h.getLevel().damageSources().playerAttack(p), 50f) == false || martha.isAlive(), "NPC não morre por dano comum");
        h.assertTrue(martha.isAlive(), "Martha continua viva");
        h.succeed();
    }

    /** Mascotes: o Krypto só aceita o Osso de Cristal, é um por jogador e conta para a missão da Martha. */
    static void mascotes(GameTestHelper h) {
        ServerPlayer p = jogador(h);
        Identifier m = dcu("martha_krypto");
        h.assertTrue(com.danrod.dcuniverse.missao.Diario.aceitar(p, m), "aceitar Um Amigo de Quatro Patas");
        h.assertValueEqual(p.getInventory().countItem(Itens.OSSO_CRISTAL.get()), 1, "a Martha entrega o Osso de Cristal");
        var k = com.danrod.dcuniverse.content.Entidades.KRYPTO.get().create(h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        net.minecraft.core.BlockPos pos = h.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2));
        k.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
        k.setInvulnerable(true); // testes vizinhos soltam ondas de choque; o dono falso não recebe a mensagem de morte
        h.getLevel().addFreshEntity(k);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.BONE));
        k.mobInteract(p, net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(!k.isTame(), "osso comum não doma o Krypto");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Itens.OSSO_CRISTAL.get()));
        k.mobInteract(p, net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(k.isTame() && k.isOwnedBy(p), "Osso de Cristal doma na hora");
        h.assertTrue(com.danrod.dcuniverse.npc.Mascotes.tem(p, "krypto"), "Krypto registrado no jogador");
        h.assertValueEqual(com.danrod.dcuniverse.missao.Diario.ativas(p).get(m).objetivo(), 1, "domar avança a missão");
        var k2 = com.danrod.dcuniverse.content.Entidades.KRYPTO.get().create(h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        k2.snapTo(pos.getX() + 2.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
        h.getLevel().addFreshEntity(k2);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Itens.OSSO_CRISTAL.get()));
        k2.mobInteract(p, net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(!k2.isTame(), "só um Krypto por jogador");
        var s = com.danrod.dcuniverse.content.Entidades.STREAKY.get().create(h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        s.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 2.5, 0f, 0f);
        s.setInvulnerable(true);
        h.getLevel().addFreshEntity(s);
        s.tame(p);
        h.assertTrue(com.danrod.dcuniverse.npc.Mascotes.tem(p, "streaky"), "Streaky registrado no jogador");
        h.assertTrue(!s.causeFallDamage(20, 1f, h.getLevel().damageSources().fall()), "Streaky não sofre dano de queda");
        // O dono voa: o Krypto sai do chão e vai junto.
        p.snapTo(pos.getX() + 0.5, pos.getY() + 4, pos.getZ() + 0.5, 0f, 0f);
        p.getAbilities().flying = true;
        h.runAfterDelay(10, () -> {
            h.assertTrue(k.voando() && k.isNoGravity(), "Krypto voa quando o dono voa");
            h.assertTrue(k.getY() > pos.getY() + 0.5, "Krypto subiu do chão (" + k.getY() + ")");
            p.getAbilities().flying = false;
            h.runAfterDelay(3, () -> {
                h.assertTrue(!k.voando(), "Krypto pousa quando o dono para de voar");
                k.discard();
                k2.discard();
                s.discard();
                h.succeed();
            });
        });
    }

    private static boolean achar(net.minecraft.server.level.ServerLevel level, net.minecraft.world.level.levelgen.structure.BoundingBox c,
                                 java.util.function.Predicate<net.minecraft.world.level.block.state.BlockState> teste) {
        for (net.minecraft.core.BlockPos q : net.minecraft.core.BlockPos.betweenClosed(c.minX(), c.minY(), c.minZ(), c.maxX(), c.maxY(), c.maxZ()))
            if (teste.test(level.getBlockState(q))) return true;
        return false;
    }
}
