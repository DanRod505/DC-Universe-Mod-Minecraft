package com.danrod.dcuniverse.content;

import com.danrod.dcuniverse.DCUniverse;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class Entidades {
    public static final DeferredRegister<EntityType<?>> TIPOS = DeferredRegister.create(Registries.ENTITY_TYPE, DCUniverse.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<ProjetilEntidade>> PROJETIL = TIPOS.register("projetil",
        () -> EntityType.Builder.<ProjetilEntidade>of(ProjetilEntidade::new, MobCategory.MISC)
            .noLootTable().sized(0.3f, 0.3f).clientTrackingRange(4).updateInterval(10)
            .build(DCUniverse.chave(TIPOS, "projetil")));

    public static final DeferredHolder<EntityType<?>, EntityType<BatarangueEntidade>> BATARANGUE = TIPOS.register("batarangue",
        () -> EntityType.Builder.<BatarangueEntidade>of(BatarangueEntidade::new, MobCategory.MISC)
            .noLootTable().sized(0.5f, 0.25f).clientTrackingRange(6).updateInterval(1)
            .build(DCUniverse.chave(TIPOS, "batarangue")));

    public static final DeferredHolder<EntityType<?>, EntityType<BatwingEntidade>> BATWING = TIPOS.register("batwing",
        () -> EntityType.Builder.<BatwingEntidade>of(BatwingEntidade::new, MobCategory.MISC)
            .noLootTable().sized(5.6f, 1.6f).clientTrackingRange(12).updateInterval(1)
            .build(DCUniverse.chave(TIPOS, "batwing")));

    public static final DeferredHolder<EntityType<?>, EntityType<MissilEntidade>> MISSIL = TIPOS.register("missil",
        () -> EntityType.Builder.<MissilEntidade>of(MissilEntidade::new, MobCategory.MISC)
            .noLootTable().sized(0.7f, 0.7f).clientTrackingRange(8).updateInterval(1)
            .build(DCUniverse.chave(TIPOS, "missil")));

    public static final DeferredHolder<EntityType<?>, EntityType<GelEntidade>> GEL = TIPOS.register("gel",
        () -> EntityType.Builder.<GelEntidade>of(GelEntidade::new, MobCategory.MISC)
            .noLootTable().sized(0.5f, 0.5f).clientTrackingRange(8).updateInterval(20)
            .build(DCUniverse.chave(TIPOS, "gel")));

    public static final DeferredHolder<EntityType<?>, EntityType<RedeEntidade>> REDE = TIPOS.register("rede",
        () -> EntityType.Builder.<RedeEntidade>of(RedeEntidade::new, MobCategory.MISC)
            .noLootTable().sized(0.6f, 0.4f).clientTrackingRange(6).updateInterval(1)
            .build(DCUniverse.chave(TIPOS, "rede")));

    public static final DeferredHolder<EntityType<?>, EntityType<VeiculoBat>> BATMOVEL = TIPOS.register("batmovel",
        () -> EntityType.Builder.<VeiculoBat>of(VeiculoBat::new, MobCategory.MISC)
            .noLootTable().sized(2.4f, 1.4f).clientTrackingRange(12).updateInterval(1)
            .build(DCUniverse.chave(TIPOS, "batmovel")));

    public static final DeferredHolder<EntityType<?>, EntityType<VeiculoBat>> BATMOTO = TIPOS.register("batmoto",
        () -> EntityType.Builder.<VeiculoBat>of(VeiculoBat::new, MobCategory.MISC)
            .noLootTable().sized(1.1f, 1.2f).clientTrackingRange(12).updateInterval(1)
            .build(DCUniverse.chave(TIPOS, "batmoto")));

    public static final DeferredHolder<EntityType<?>, EntityType<VeiculoBat>> BATBARCO = TIPOS.register("batbarco",
        () -> EntityType.Builder.<VeiculoBat>of(VeiculoBat::new, MobCategory.MISC)
            .noLootTable().sized(2.6f, 1.1f).clientTrackingRange(12).updateInterval(1)
            .build(DCUniverse.chave(TIPOS, "batbarco")));

    public static final DeferredHolder<EntityType<?>, EntityType<com.danrod.dcuniverse.npc.NpcEntidade>> NPC = TIPOS.register("npc",
        () -> EntityType.Builder.<com.danrod.dcuniverse.npc.NpcEntidade>of(com.danrod.dcuniverse.npc.NpcEntidade::new, MobCategory.MISC)
            .noLootTable().sized(0.6f, 1.8f).eyeHeight(1.62f).clientTrackingRange(10)
            .build(DCUniverse.chave(TIPOS, "npc")));

    /** O mesmo NPC com braços finos (modelo slim). */
    public static final DeferredHolder<EntityType<?>, EntityType<com.danrod.dcuniverse.npc.NpcEntidade>> NPC_SLIM = TIPOS.register("npc_slim",
        () -> EntityType.Builder.<com.danrod.dcuniverse.npc.NpcEntidade>of(com.danrod.dcuniverse.npc.NpcEntidade::new, MobCategory.MISC)
            .noLootTable().sized(0.6f, 1.8f).eyeHeight(1.62f).clientTrackingRange(10)
            .build(DCUniverse.chave(TIPOS, "npc_slim")));

    public static final DeferredHolder<EntityType<?>, EntityType<com.danrod.dcuniverse.npc.Krypto>> KRYPTO = TIPOS.register("krypto",
        () -> EntityType.Builder.<com.danrod.dcuniverse.npc.Krypto>of(com.danrod.dcuniverse.npc.Krypto::new, MobCategory.CREATURE)
            .noLootTable().sized(0.6f, 0.85f).eyeHeight(0.68f).clientTrackingRange(10)
            .build(DCUniverse.chave(TIPOS, "krypto")));

    public static final DeferredHolder<EntityType<?>, EntityType<com.danrod.dcuniverse.npc.Streaky>> STREAKY = TIPOS.register("streaky",
        () -> EntityType.Builder.<com.danrod.dcuniverse.npc.Streaky>of(com.danrod.dcuniverse.npc.Streaky::new, MobCategory.CREATURE)
            .noLootTable().sized(0.6f, 0.7f).eyeHeight(0.35f).clientTrackingRange(10)
            .build(DCUniverse.chave(TIPOS, "streaky")));

    public static final DeferredHolder<EntityType<?>, EntityType<BlocoTelecinetico>> BLOCO_TK = TIPOS.register("bloco_telecinetico",
        () -> EntityType.Builder.<BlocoTelecinetico>of(BlocoTelecinetico::new, MobCategory.MISC)
            .noLootTable().sized(0.98f, 0.98f).clientTrackingRange(8).updateInterval(1)
            .build(DCUniverse.chave(TIPOS, "bloco_telecinetico")));

    public static final DeferredHolder<EntityType<?>, EntityType<IlhaTelecinetica>> ILHA = TIPOS.register("ilha_telecinetica",
        () -> EntityType.Builder.<IlhaTelecinetica>of(IlhaTelecinetica::new, MobCategory.MISC)
            .noLootTable().sized(1f, 1f).clientTrackingRange(16).updateInterval(1)
            .build(DCUniverse.chave(TIPOS, "ilha_telecinetica")));

    public static final DeferredHolder<EntityType<?>, EntityType<NaveKryptoniana>> NAVE_KRYPTONIANA = TIPOS.register("nave_kryptoniana",
        () -> EntityType.Builder.<NaveKryptoniana>of(NaveKryptoniana::new, MobCategory.MISC)
            .noLootTable().sized(4.0f, 2.2f).clientTrackingRange(12).updateInterval(1)
            .build(DCUniverse.chave(TIPOS, "nave_kryptoniana")));

    public static final DeferredHolder<EntityType<?>, EntityType<OrbeSolar>> ORBE_SOLAR = TIPOS.register("orbe_solar",
        () -> EntityType.Builder.<OrbeSolar>of(OrbeSolar::new, MobCategory.MISC)
            .noLootTable().sized(0.6f, 0.6f).clientTrackingRange(8).updateInterval(1)
            .build(DCUniverse.chave(TIPOS, "orbe_solar")));

    private Entidades() {}
}
