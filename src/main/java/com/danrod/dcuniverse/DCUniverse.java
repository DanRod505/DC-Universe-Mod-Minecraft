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
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

@Mod(DCUniverse.MODID)
public final class DCUniverse {
    public static final String MODID = "dcuniverse";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static IEventBus modBus;

    /** Chave de registro de um id do mod (para o setId dos itens, blocos e entidades). */
    public static <T> ResourceKey<T> chave(DeferredRegister<T> registro, String id) {
        return ResourceKey.create(registro.getRegistryKey(), Identifier.fromNamespaceAndPath(MODID, id));
    }

    /** Barramento do mod (registro de renderizadores, teclas, atributos...). */
    public static IEventBus modBus() {
        return modBus;
    }

    public DCUniverse(IEventBus bus, ModContainer container) {
        modBus = bus;
        Itens.ITEMS.register(bus);
        Itens.ABAS.register(bus);
        Entidades.TIPOS.register(bus);
        Sons.SONS.register(bus);
        Blocos.BLOCOS.register(bus);
        com.danrod.dcuniverse.world.Mundo.FEATURES.register(bus);
        com.danrod.dcuniverse.world.estrutura.Estruturas.TIPOS.register(bus);
        com.danrod.dcuniverse.world.estrutura.Estruturas.PECAS.register(bus);
        com.danrod.dcuniverse.world.mundo.MundoDC.FONTES.register(bus);
        Testes.FUNCOES.register(bus);
        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        Rede.registrar();
        bus.addListener(RegisterPayloadHandlersEvent.class, Rede.CANAL::registrar);
        Eventos.registrar();
        if (FMLEnvironment.getDist().isClient()) com.danrod.dcuniverse.client.Cliente.registrar();
    }
}
