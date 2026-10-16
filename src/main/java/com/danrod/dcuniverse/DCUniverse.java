package com.danrod.dcuniverse;

import com.danrod.dcuniverse.content.Blocos;
import com.danrod.dcuniverse.content.Entidades;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.content.Itens;
import com.danrod.dcuniverse.core.Config;
import com.danrod.dcuniverse.core.Eventos;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.test.Testes;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(DCUniverse.MODID)
public final class DCUniverse {
    public static final String MODID = "dcuniverse";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DCUniverse(FMLJavaModLoadingContext context) {
        var bus = context.getModBusGroup();
        Itens.ITEMS.register(bus);
        Itens.ABAS.register(bus);
        Entidades.TIPOS.register(bus);
        Sons.SONS.register(bus);
        Blocos.BLOCOS.register(bus);
        com.danrod.dcuniverse.world.Mundo.FEATURES.register(bus);
        Testes.FUNCOES.register(bus);
        context.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        FMLCommonSetupEvent.getBus(bus).addListener(e -> Rede.registrar());
        Eventos.registrar();
        if (FMLEnvironment.dist == Dist.CLIENT) com.danrod.dcuniverse.client.Cliente.registrar();
    }
}
