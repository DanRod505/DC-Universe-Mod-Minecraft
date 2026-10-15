package com.danrod.dcuniverse.content;

import com.danrod.dcuniverse.DCUniverse;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Function;

/** Blocos do mod. Os temporários das habilidades não têm item nem drop. */
public final class Blocos {
    public static final DeferredRegister<Block> BLOCOS = DeferredRegister.create(ForgeRegistries.BLOCKS, DCUniverse.MODID);

    /** Cristal da Fortaleza da Solidão: inquebrável, brilha e some sozinho quando a suprema acaba. */
    public static final RegistryObject<Block> CRISTAL_FORTALEZA = bloco("cristal_fortaleza", p -> new CristalFortaleza(p
        .mapColor(MapColor.ICE).strength(-1f, 3600000f).noLootTable().noOcclusion().lightLevel(s -> 12)
        .sound(SoundType.AMETHYST).pushReaction(PushReaction.BLOCK)
        .isValidSpawn((s, l, pos, t) -> false).isRedstoneConductor((s, l, pos) -> false)
        .isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false)));

    private Blocos() {}

    private static RegistryObject<Block> bloco(String id, Function<BlockBehaviour.Properties, Block> fabrica) {
        return BLOCOS.register(id, () -> fabrica.apply(BlockBehaviour.Properties.of().setId(BLOCOS.key(id))));
    }
}
