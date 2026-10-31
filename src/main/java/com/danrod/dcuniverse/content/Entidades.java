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

    public static final RegistryObject<EntityType<BatarangueEntidade>> BATARANGUE = TIPOS.register("batarangue",
        () -> EntityType.Builder.<BatarangueEntidade>of(BatarangueEntidade::new, MobCategory.MISC)
            .noLootTable().sized(0.5f, 0.25f).clientTrackingRange(6).updateInterval(1)
            .build(TIPOS.key("batarangue")));

    public static final RegistryObject<EntityType<LacoEntidade>> LACO = TIPOS.register("laco",
        () -> EntityType.Builder.<LacoEntidade>of(LacoEntidade::new, MobCategory.MISC)
            .noLootTable().sized(0.4f, 0.4f).clientTrackingRange(8).updateInterval(1)
            .build(TIPOS.key("laco")));

    public static final RegistryObject<EntityType<ArmaArremessadaEntidade>> ARMA_ARREMESSADA = TIPOS.register("arma_arremessada",
        () -> EntityType.Builder.<ArmaArremessadaEntidade>of(ArmaArremessadaEntidade::new, MobCategory.MISC)
            .noLootTable().sized(0.5f, 0.25f).clientTrackingRange(6).updateInterval(1)
            .build(TIPOS.key("arma_arremessada")));

    public static final RegistryObject<EntityType<BatwingEntidade>> BATWING = TIPOS.register("batwing",
        () -> EntityType.Builder.<BatwingEntidade>of(BatwingEntidade::new, MobCategory.MISC)
            .noLootTable().sized(5.6f, 1.6f).clientTrackingRange(12).updateInterval(1)
            .build(TIPOS.key("batwing")));

    public static final RegistryObject<EntityType<MissilEntidade>> MISSIL = TIPOS.register("missil",
        () -> EntityType.Builder.<MissilEntidade>of(MissilEntidade::new, MobCategory.MISC)
            .noLootTable().sized(0.7f, 0.7f).clientTrackingRange(8).updateInterval(1)
            .build(TIPOS.key("missil")));

    public static final RegistryObject<EntityType<GelEntidade>> GEL = TIPOS.register("gel",
        () -> EntityType.Builder.<GelEntidade>of(GelEntidade::new, MobCategory.MISC)
            .noLootTable().sized(0.5f, 0.5f).clientTrackingRange(8).updateInterval(20)
            .build(TIPOS.key("gel")));

    public static final RegistryObject<EntityType<RedeEntidade>> REDE = TIPOS.register("rede",
        () -> EntityType.Builder.<RedeEntidade>of(RedeEntidade::new, MobCategory.MISC)
            .noLootTable().sized(0.6f, 0.4f).clientTrackingRange(6).updateInterval(1)
            .build(TIPOS.key("rede")));

    public static final RegistryObject<EntityType<VeiculoBat>> BATMOVEL = TIPOS.register("batmovel",
        () -> EntityType.Builder.<VeiculoBat>of(VeiculoBat::new, MobCategory.MISC)
            .noLootTable().sized(2.4f, 1.4f).clientTrackingRange(12).updateInterval(1)
            .build(TIPOS.key("batmovel")));

    public static final RegistryObject<EntityType<VeiculoBat>> BATMOTO = TIPOS.register("batmoto",
        () -> EntityType.Builder.<VeiculoBat>of(VeiculoBat::new, MobCategory.MISC)
            .noLootTable().sized(1.1f, 1.2f).clientTrackingRange(12).updateInterval(1)
            .build(TIPOS.key("batmoto")));

    public static final RegistryObject<EntityType<VeiculoBat>> BATBARCO = TIPOS.register("batbarco",
        () -> EntityType.Builder.<VeiculoBat>of(VeiculoBat::new, MobCategory.MISC)
            .noLootTable().sized(2.6f, 1.1f).clientTrackingRange(12).updateInterval(1)
            .build(TIPOS.key("batbarco")));

    public static final RegistryObject<EntityType<com.danrod.dcuniverse.npc.NpcEntidade>> NPC = TIPOS.register("npc",
        () -> EntityType.Builder.<com.danrod.dcuniverse.npc.NpcEntidade>of(com.danrod.dcuniverse.npc.NpcEntidade::new, MobCategory.MISC)
            .noLootTable().sized(0.6f, 1.8f).eyeHeight(1.62f).clientTrackingRange(10)
            .build(TIPOS.key("npc")));

    /** O mesmo NPC com braços finos (modelo slim). */
    public static final RegistryObject<EntityType<com.danrod.dcuniverse.npc.NpcEntidade>> NPC_SLIM = TIPOS.register("npc_slim",
        () -> EntityType.Builder.<com.danrod.dcuniverse.npc.NpcEntidade>of(com.danrod.dcuniverse.npc.NpcEntidade::new, MobCategory.MISC)
            .noLootTable().sized(0.6f, 1.8f).eyeHeight(1.62f).clientTrackingRange(10)
            .build(TIPOS.key("npc_slim")));

    public static final RegistryObject<EntityType<com.danrod.dcuniverse.npc.Krypto>> KRYPTO = TIPOS.register("krypto",
        () -> EntityType.Builder.<com.danrod.dcuniverse.npc.Krypto>of(com.danrod.dcuniverse.npc.Krypto::new, MobCategory.CREATURE)
            .noLootTable().sized(0.6f, 0.85f).eyeHeight(0.68f).clientTrackingRange(10)
            .build(TIPOS.key("krypto")));

    public static final RegistryObject<EntityType<com.danrod.dcuniverse.npc.Streaky>> STREAKY = TIPOS.register("streaky",
        () -> EntityType.Builder.<com.danrod.dcuniverse.npc.Streaky>of(com.danrod.dcuniverse.npc.Streaky::new, MobCategory.CREATURE)
            .noLootTable().sized(0.6f, 0.7f).eyeHeight(0.35f).clientTrackingRange(10)
            .build(TIPOS.key("streaky")));

    public static final RegistryObject<EntityType<BlocoTelecinetico>> BLOCO_TK = TIPOS.register("bloco_telecinetico",
        () -> EntityType.Builder.<BlocoTelecinetico>of(BlocoTelecinetico::new, MobCategory.MISC)
            .noLootTable().sized(0.98f, 0.98f).clientTrackingRange(8).updateInterval(1)
            .build(TIPOS.key("bloco_telecinetico")));

    public static final RegistryObject<EntityType<IlhaTelecinetica>> ILHA = TIPOS.register("ilha_telecinetica",
        () -> EntityType.Builder.<IlhaTelecinetica>of(IlhaTelecinetica::new, MobCategory.MISC)
            .noLootTable().sized(1f, 1f).clientTrackingRange(16).updateInterval(1)
            .build(TIPOS.key("ilha_telecinetica")));

    public static final RegistryObject<EntityType<NaveKryptoniana>> NAVE_KRYPTONIANA = TIPOS.register("nave_kryptoniana",
        () -> EntityType.Builder.<NaveKryptoniana>of(NaveKryptoniana::new, MobCategory.MISC)
            .noLootTable().sized(4.0f, 2.2f).clientTrackingRange(12).updateInterval(1)
            .build(TIPOS.key("nave_kryptoniana")));

    public static final RegistryObject<EntityType<OrbeSolar>> ORBE_SOLAR = TIPOS.register("orbe_solar",
        () -> EntityType.Builder.<OrbeSolar>of(OrbeSolar::new, MobCategory.MISC)
            .noLootTable().sized(0.6f, 0.6f).clientTrackingRange(8).updateInterval(1)
            .build(TIPOS.key("orbe_solar")));

    private Entidades() {}
}
