package com.danrod.dcuniverse.content;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.suit.TrajeItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Function;

/** Itens do mod. O lote de itens da versão (minérios, cristais, núcleos) entra na última etapa dela. */
public final class Itens {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, DCUniverse.MODID);
    public static final DeferredRegister<CreativeModeTab> ABAS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DCUniverse.MODID);

    // Traje travado (uma peça por slot, igual para todos os personagens; o visual vem da skin)
    public static final RegistryObject<Item> TRAJE_CABECA = traje("traje_cabeca", EquipmentSlot.HEAD);
    public static final RegistryObject<Item> TRAJE_PEITO = traje("traje_peito", EquipmentSlot.CHEST);
    public static final RegistryObject<Item> TRAJE_PERNAS = traje("traje_pernas", EquipmentSlot.LEGS);
    public static final RegistryObject<Item> TRAJE_PES = traje("traje_pes", EquipmentSlot.FEET);

    // Itens de transformação (ids de docs/dados/personagens.json)
    public static final RegistryObject<Item> CRISTAL_SUPERMAN = item("cristal_superman",
        p -> new TransformacaoItem("superman", p.rarity(Rarity.EPIC)));

    // Utilidades
    public static final RegistryObject<Item> GUIA_HEROI = item("guia_heroi", p -> new GuiaItem(p.stacksTo(1)));

    public static final RegistryObject<CreativeModeTab> ABA = ABAS.register("dcuniverse", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.dcuniverse"))
        .icon(() -> new ItemStack(CRISTAL_SUPERMAN.get()))
        .displayItems((params, out) -> ITEMS.getEntries().stream().map(RegistryObject::get)
            .filter(i -> !(i instanceof TrajeItem)).forEach(out::accept))
        .build());

    private Itens() {}

    public static Item traje(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> TRAJE_CABECA.get();
            case CHEST -> TRAJE_PEITO.get();
            case LEGS -> TRAJE_PERNAS.get();
            default -> TRAJE_PES.get();
        };
    }

    private static RegistryObject<Item> item(String id, Function<Item.Properties, Item> fabrica) {
        return ITEMS.register(id, () -> fabrica.apply(new Item.Properties().setId(ITEMS.key(id))));
    }

    private static RegistryObject<Item> traje(String id, EquipmentSlot slot) {
        return item(id, p -> new TrajeItem(p.stacksTo(1).equippableUnswappable(slot)));
    }
}
