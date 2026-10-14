package com.danrod.dcuniverse.content;

import com.danrod.dcuniverse.DCUniverse;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class Entidades {
    public static final DeferredRegister<EntityType<?>> TIPOS = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DCUniverse.MODID);

    public static final RegistryObject<EntityType<ProjetilEntidade>> PROJETIL = TIPOS.register("projetil",
        () -> EntityType.Builder.<ProjetilEntidade>of(ProjetilEntidade::new, MobCategory.MISC)
            .noLootTable().sized(0.3f, 0.3f).clientTrackingRange(4).updateInterval(10)
            .build(TIPOS.key("projetil")));

    private Entidades() {}
}
