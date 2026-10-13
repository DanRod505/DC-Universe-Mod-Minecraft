package com.danrod.dcuniverse.test;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.character.Transformacao;
import com.danrod.dcuniverse.content.Blocos;
import com.danrod.dcuniverse.content.Itens;
import com.danrod.dcuniverse.content.TortaItem;
import com.danrod.dcuniverse.core.Eventos;
import com.danrod.dcuniverse.energy.Kryptonita;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.component.DataComponents;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.energy.Energia;
import com.danrod.dcuniverse.suit.Traje;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Consumer;

/**
 * GameTests do mod (rodam em ./gradlew runGameTestServer e no CI). Cada função tem um
 * data/dcuniverse/test_instance/<nome>.json. Os grupos seguem os módulos: transform, suit_lock, energy.
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
        FUNCOES.register("kryptonite", () -> Testes::kryptonita);
        FUNCOES.register("content", () -> Testes::conteudo);
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

        // Troca direta para o Batman (trava a cabeça também) e volta ao normal.
        h.assertTrue(Transformacao.transformar(p, BATMAN, true), "trocar para o Batman");
        h.assertTrue(p.getItemBySlot(EquipmentSlot.HEAD).is(Itens.TRAJE_CABECA.get()), "Batman trava a cabeça");
        Transformacao.destransformar(p);
        h.assertTrue(p.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE), "peitoral devolvido");
        h.assertTrue(p.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "capacete devolvido");
        h.assertValueEqual((double) p.getMaxHealth(), 20.0, "vida máxima normal");
        h.assertFalse(e.transformado(), "destransformado");
        h.succeed();
    }

    /** Peças de traje fora do slot somem; slot vazio recebe a peça de volta. */
    static void trajeTravado(GameTestHelper h) {
        ServerPlayer p = jogador(h);
        Transformacao.transformar(p, BATMAN, true);
        Personagem bat = EstadoJogador.de(p).personagem();
        p.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        p.getInventory().add(new ItemStack(Itens.TRAJE_PES.get()));
        Traje.verificar(p, bat);
        h.assertTrue(p.getItemBySlot(EquipmentSlot.FEET).is(Itens.TRAJE_PES.get()), "bota recolocada");
        h.assertFalse(p.getInventory().getNonEquipmentItems().stream().anyMatch(Traje::ehTraje), "nenhuma peça no inventário");
        Transformacao.destransformar(p);
        h.assertFalse(Traje.SLOTS.stream().anyMatch(s -> Traje.ehTraje(p.getItemBySlot(s))), "sem traje depois de destransformar");
        h.succeed();
    }

    /** Recarga por tick, gasto e o estado esgotado até voltar a 10%. */
    static void energia(GameTestHelper h) {
        ServerPlayer p = jogador(h);
        Transformacao.transformar(p, BATMAN, true);
        EstadoJogador e = EstadoJogador.de(p);
        Personagem bat = e.personagem();
        float antes = e.energia();
        for (int i = 0; i < 20; i++) Energia.tick(p, bat);
        h.assertTrue(e.energia() >= antes + bat.energia().regen() - 0.01f, "regenera pelo menos o regen da ficha em 1 s");
        Energia.adicionar(p, bat, -10_000);
        h.assertTrue(e.esgotado(), "esgotado ao zerar");
        h.assertFalse(Energia.gastar(p, bat, 1), "não gasta esgotado");
        Energia.adicionar(p, bat, bat.energia().max() * 0.11f);
        h.assertFalse(e.esgotado(), "libera ao passar de 10%");
        h.succeed();
    }

    /** O pacote de sincronização leva as habilidades do personagem e passa ida e volta pelo codec. */
    static void sincronizacao(GameTestHelper h) {
        ServerPlayer p = jogador(h);
        Transformacao.transformar(p, SUPERMAN, true);
        var buf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        Rede.Sync.encode(Rede.montar(p), buf);
        Rede.Sync s = Rede.Sync.decode(buf);
        h.assertValueEqual(s.personagem(), SUPERMAN.toString(), "personagem no sync");
        h.assertTrue(s.habilidades().stream().anyMatch(x -> x.id().equals("heat_vision") && x.hold()), "visão de calor canalizada");
        h.succeed();
    }

    /** Bloco de Kryptonita a 3 blocos drena o Superman; longe dele a energia volta a subir. */
    static void kryptonita(GameTestHelper h) {
        ServerPlayer p = jogador(h);
        BlockPos bloco = h.absolutePos(new BlockPos(0, 1, 0));
        h.getLevel().setBlockAndUpdate(bloco, Blocos.BLOCO_KRYPTONITA.get().defaultBlockState());
        p.setPos(bloco.getX() + 3.5, bloco.getY(), bloco.getZ() + 0.5);
        Transformacao.transformar(p, SUPERMAN, true);
        Personagem sup = EstadoJogador.de(p).personagem();
        h.assertTrue(sup.kryptoniano(), "Superman é kryptoniano");
        float antes = EstadoJogador.de(p).energia();
        Energia.tick(p, sup);
        h.assertTrue(EstadoJogador.de(p).energia() < antes, "kryptonita perto drena energia");
        h.assertTrue(Kryptonita.perto(p, 6, 10), "bloco detectado a 3 blocos");
        h.assertFalse(Kryptonita.perto(p, 6, 2), "fora do raio do bloco");
        p.getInventory().add(new ItemStack(Itens.FRAGMENTO_KRYPTONITA.get()));
        h.assertTrue(Kryptonita.noInventario(p), "fragmento no inventário conta");
        h.succeed();
    }

    /** Receitas geradas carregam, o Guia é dado uma vez só e a Torta recarrega kryptonianos. */
    static void conteudo(GameTestHelper h) {
        var receitas = h.getLevel().getServer().getRecipeManager();
        for (String id : new String[] {"cristal_superman", "cinto_batman", "nucleo_meta_1", "batarangue_simples"}) {
            var chave = ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(DCUniverse.MODID, id));
            h.assertTrue(receitas.byKey(chave).isPresent(), "receita " + id);
        }
        ServerPlayer p = jogador(h);
        h.assertTrue(Eventos.darGuia(p), "primeiro login recebe o guia");
        h.assertFalse(Eventos.darGuia(p), "só uma vez");
        h.assertTrue(p.getInventory().contains(s -> s.is(Itens.GUIA_HEROI.get())), "guia no inventário");

        Transformacao.transformar(p, SUPERMAN, true);
        Personagem sup = EstadoJogador.de(p).personagem();
        float antes = EstadoJogador.de(p).energia();
        h.assertTrue(TortaItem.recarregar(p), "torta recarrega kryptoniano");
        h.assertValueEqual(EstadoJogador.de(p).energia(), antes + sup.energia().max() * TortaItem.ENERGIA, "torta devolve 10%");
        var comida = new ItemStack(Itens.TORTA_MARTHA.get()).get(DataComponents.FOOD);
        h.assertTrue(comida != null && comida.nutrition() == 8, "torta dá 8 de fome");
        Transformacao.transformar(p, BATMAN, true);
        h.assertFalse(TortaItem.recarregar(p), "Batman não é kryptoniano");
        h.succeed();
    }
}
