package com.danrod.dcuniverse.content;

import com.danrod.dcuniverse.DCUniverse;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Function;

/** Blocos do mod; cada um ganha o item de bloco com o mesmo id. Ferramenta certa e drops ficam nos JSON de data. */
public final class Blocos {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, DCUniverse.MODID);

    // Minério de Kryptonita: Y -60 a 0, picareta de diamante, brilha no escuro (drena kryptonianos a 6 blocos).
    public static final RegistryObject<Block> MINERIO_KRYPTONITA = bloco("minerio_kryptonita",
        p -> new DropExperienceBlock(UniformInt.of(3, 7), p),
        BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).requiresCorrectToolForDrops().strength(4.5f, 3f)
            .sound(SoundType.DEEPSLATE).lightLevel(s -> 7));
    // Bloco de Kryptonita: armazenamento e aura que drena kryptonianos a 10 blocos.
    public static final RegistryObject<Block> BLOCO_KRYPTONITA = bloco("bloco_kryptonita", Block::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).requiresCorrectToolForDrops().strength(5f, 6f)
            .sound(SoundType.AMETHYST).lightLevel(s -> 10));
    public static final RegistryObject<Block> BATCOMPUTADOR = bloco("batcomputador", Block::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops().strength(3.5f, 6f)
            .sound(SoundType.METAL).lightLevel(s -> 5));

    private Blocos() {}

    private static RegistryObject<Block> bloco(String id, Function<BlockBehaviour.Properties, Block> fabrica, BlockBehaviour.Properties props) {
        RegistryObject<Block> bloco = BLOCKS.register(id, () -> fabrica.apply(props.setId(BLOCKS.key(id))));
        Itens.ITEMS.register(id, () -> new BlockItem(bloco.get(), new Item.Properties().setId(Itens.ITEMS.key(id)).useBlockDescriptionPrefix()));
        return bloco;
    }
}
