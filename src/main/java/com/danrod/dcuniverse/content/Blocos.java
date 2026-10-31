package com.danrod.dcuniverse.content;

import com.danrod.dcuniverse.DCUniverse;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Function;

/** Blocos do mod. Os temporários das habilidades não têm item nem drop. */
public final class Blocos {
    public static final DeferredRegister<Block> BLOCOS = DeferredRegister.create(Registries.BLOCK, DCUniverse.MODID);

    /** Cristal da Fortaleza da Solidão: inquebrável, brilha e some sozinho quando a suprema acaba. */
    public static final DeferredHolder<Block, Block> CRISTAL_FORTALEZA = bloco("cristal_fortaleza", p -> new CristalFortaleza(p
        .mapColor(MapColor.ICE).strength(-1f, 3600000f).noLootTable().noOcclusion().lightLevel(s -> 12)
        .sound(SoundType.AMETHYST).pushReaction(PushReaction.BLOCK)
        .isValidSpawn((s, l, pos, t) -> false).isRedstoneConductor((s, l, pos) -> false)
        .isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false)));

    /** Minério de Kryptonita: no fundo do mundo e nas crateras; brilha verde e enfraquece kryptonianos perto. */
    public static final DeferredHolder<Block, Block> MINERIO_KRYPTONITA = blocoComItem("minerio_kryptonita", p -> new KryptonitaBloco(p
        .mapColor(MapColor.DEEPSLATE).strength(4.5f, 3f).requiresCorrectToolForDrops().lightLevel(s -> 7)
        .sound(SoundType.DEEPSLATE), 6), Rarity.COMMON);

    /** Bloco de Kryptonita: 9 fragmentos; aura mais forte (defesa de base contra kryptonianos). */
    public static final DeferredHolder<Block, Block> BLOCO_KRYPTONITA = blocoComItem("bloco_kryptonita", p -> new KryptonitaBloco(p
        .mapColor(MapColor.EMERALD).strength(5f, 6f).requiresCorrectToolForDrops().lightLevel(s -> 12)
        .sound(SoundType.AMETHYST), 18), Rarity.UNCOMMON);

    /** Câmara Solar: recarrega kryptonianos em volta como se estivessem sob o sol. */
    public static final DeferredHolder<Block, Block> CAMARA_SOLAR = blocoComItem("camara_solar", p -> new CamaraSolar(p
        .mapColor(MapColor.QUARTZ).strength(3f, 6f).requiresCorrectToolForDrops().lightLevel(s -> 15)
        .sound(SoundType.GLASS).noOcclusion().isRedstoneConductor((s, l, pos) -> false)
        .isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false)), Rarity.RARE);

    /** Minério de Prométio: veios raros no fundo do deepslate; o metal das armaduras e gadgets de ponta. */
    public static final DeferredHolder<Block, Block> MINERIO_PROMETIO = blocoComItem("minerio_prometio", p -> new Block(p
        .mapColor(MapColor.DEEPSLATE).strength(5f, 6f).requiresCorrectToolForDrops().lightLevel(s -> 3)
        .sound(SoundType.DEEPSLATE)), Rarity.UNCOMMON);

    /** Carregador Tecnológico: estação de recarga para heróis de tecnologia e seus aparelhos. */
    public static final DeferredHolder<Block, Block> CARREGADOR_TECNOLOGICO = blocoComItem("carregador_tecnologico", p -> new CarregadorBloco(p
        .mapColor(MapColor.METAL).strength(4f, 8f).requiresCorrectToolForDrops().lightLevel(s -> 10)
        .sound(SoundType.NETHERITE_BLOCK).noOcclusion().isRedstoneConductor((s, l, pos) -> false)
        .isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false)), Rarity.UNCOMMON);

    /** Batcomputador: gadgets, fabricação, análise de mobs e mapa (content/Batcomputador). */
    public static final DeferredHolder<Block, Block> BATCOMPUTADOR = blocoComItem("batcomputador", p -> new BatcomputadorBloco(p
        .mapColor(MapColor.COLOR_BLACK).strength(4f, 8f).requiresCorrectToolForDrops().lightLevel(s -> 7)
        .sound(SoundType.NETHERITE_BLOCK).noOcclusion().isRedstoneConductor((s, l, pos) -> false)
        .isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false)), Rarity.RARE);

    /** Nave de Kal-El: só aparece no porão do celeiro da Fazenda Kent (sem item; inquebrável no sobrevivência). */
    public static final DeferredHolder<Block, Block> NAVE_KALEL = bloco("nave_kalel", p -> new NaveKalEl(p
        .mapColor(MapColor.METAL).strength(-1f, 3600000f).noLootTable().noOcclusion().lightLevel(s -> 10)
        .sound(SoundType.NETHERITE_BLOCK).pushReaction(PushReaction.BLOCK).isRedstoneConductor((s, l, pos) -> false)
        .isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false)));

    /** Meteorito: cai em volta da Cratera do Meteoro na missão "Meteoro em Smallville" (missao/Cenas); sem item, solta Fragmentos Alienígenas. */
    public static final DeferredHolder<Block, Block> METEORITO = bloco("meteorito", p -> new Block(p
        .mapColor(MapColor.COLOR_BLACK).strength(1.5f, 6f).lightLevel(s -> 9).sound(SoundType.ANCIENT_DEBRIS)));

    private Blocos() {}

    private static DeferredHolder<Block, Block> blocoComItem(String id, Function<BlockBehaviour.Properties, Block> fabrica, Rarity raridade) {
        DeferredHolder<Block, Block> bloco = bloco(id, fabrica);
        Itens.ITEMS.register(id, () -> new BlockItem(bloco.get(), new Item.Properties().setId(DCUniverse.chave(Itens.ITEMS, id))
            .useBlockDescriptionPrefix().rarity(raridade)));
        return bloco;
    }

    private static DeferredHolder<Block, Block> bloco(String id, Function<BlockBehaviour.Properties, Block> fabrica) {
        return BLOCOS.register(id, () -> fabrica.apply(BlockBehaviour.Properties.of().setId(DCUniverse.chave(BLOCOS, id))));
    }
}
