package com.danrod.dcuniverse;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(DCUniverse.MODID)
public final class DCUniverse {
    public static final String MODID = "dcuniverse";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DCUniverse(FMLJavaModLoadingContext context) {
        FMLCommonSetupEvent.getBus(context.getModBusGroup()).addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("DC Universe carregado");
    }
}
