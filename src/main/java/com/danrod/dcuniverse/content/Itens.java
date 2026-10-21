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

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;

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
    public static final RegistryObject<Item> CINTO_BATMAN = item("cinto_batman",
        p -> new TransformacaoItem("batman", p.rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> CRISTAL_SUPERGIRL = item("cristal_supergirl",
        p -> new TransformacaoItem("supergirl", p.rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> JAQUETA_CONNER = item("jaqueta_conner",
        p -> new TransformacaoItem("superboy", p.rarity(Rarity.EPIC)));

    // Lote da v0.1: kryptonita e Krypton
    public static final RegistryObject<Item> FRAGMENTO_KRYPTONITA = item("fragmento_kryptonita", p -> new Item(p.rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> CRISTAL_KRYPTONIANO = item("cristal_kryptoniano", p -> new Item(p.rarity(Rarity.RARE)));
    public static final RegistryObject<Item> FRAGMENTO_ALIENIGENA = item("fragmento_alienigena", p -> new Item(p.rarity(Rarity.UNCOMMON)));

    // Componentes: base das receitas dos itens de transformação e dos trajes
    public static final RegistryObject<Item> TECIDO_TATICO = item("tecido_tatico", Item::new);
    public static final RegistryObject<Item> NUCLEO_META_1 = item("nucleo_meta_1", Item::new);
    public static final RegistryObject<Item> NUCLEO_META_2 = item("nucleo_meta_2", p -> new Item(p.rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> NUCLEO_META_3 = item("nucleo_meta_3", p -> new Item(p.rarity(Rarity.RARE)));
    public static final RegistryObject<Item> NUCLEO_META_4 = item("nucleo_meta_4", p -> new Item(p.rarity(Rarity.EPIC).fireResistant()));

    // Lote da v0.2: prométio e tecnologia (Batman)
    public static final RegistryObject<Item> LINGOTE_PROMETIO = item("lingote_prometio", p -> new Item(p.rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> PLACA_CIRCUITO = item("placa_circuito", Item::new);
    public static final RegistryObject<Item> COMPONENTE_TECNOLOGICO = item("componente_tecnologico", Item::new);
    public static final RegistryObject<Item> NUCLEO_ENERGIA = item("nucleo_energia", p -> new Item(p.rarity(Rarity.RARE)));

    // Equipamentos sem poderes (qualquer jogador usa)
    public static final RegistryObject<Item> BATARANGUE_SIMPLES = item("batarangue_simples", p -> new BatarangueItem(p.stacksTo(16)));
    public static final RegistryObject<Item> GANCHO_ESCALADA = item("gancho_escalada", p -> new GanchoItem(p.durability(GanchoItem.USOS)
        .repairable(net.minecraft.world.item.Items.STRING)));
    public static final RegistryObject<Item> CAPA_PLANADOR = item("capa_planador", p -> new Item(p.stacksTo(1)
        .equippable(EquipmentSlot.CHEST).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> VISOR_TATICO = item("visor_tatico", p -> new Item(p.durability(Equipamentos.VISOR_DURACAO)
        .component(net.minecraft.core.component.DataComponents.EQUIPPABLE, net.minecraft.world.item.equipment.Equippable.builder(EquipmentSlot.HEAD)
            .setAsset(net.minecraft.resources.ResourceKey.create(net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID,
                net.minecraft.resources.Identifier.fromNamespaceAndPath(DCUniverse.MODID, "visor_tatico")))
            .setEquipSound(net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON).build())
        .repairable(PLACA_CIRCUITO.get()).rarity(Rarity.UNCOMMON)));

    // Utilidades
    public static final RegistryObject<Item> OSSO_CRISTAL = item("osso_cristal", p -> new Item(p.stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> COMUNICADOR_LIGA = item("comunicador_liga", p -> new com.danrod.dcuniverse.liga.ComunicadorItem(p.stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> GUIA_HEROI = item("guia_heroi", p -> new GuiaItem(p.stacksTo(1)));

    /**
     * Abas do inventário criativo, uma por tipo de item. O tipo é o "cat" de docs/dados (o mesmo dos grupos do Guia):
     * tools/gerar_conteudo.py grava cada aba como a tag de item dcuniverse:aba/&lt;nome&gt;, que chega ao cliente com as
     * outras tags ao entrar no mundo. Item sem tag cai em Utilidades; o GameTest "abas" confere que nada fica de fora.
     */
    public enum Aba {
        HEROIS("dcuniverse", "herois", "cristal_superman"),
        EQUIPAMENTOS("dcuniverse_equipamentos", "equipamentos", "batarangue_simples"),
        UTILIDADES("dcuniverse_utilidades", "utilidades", "guia_heroi"),
        RECURSOS("dcuniverse_recursos", "recursos", "fragmento_kryptonita"),
        COMPONENTES("dcuniverse_componentes", "componentes", "nucleo_meta_1"),
        BLOCOS("dcuniverse_blocos", "blocos", "batcomputador");

        public final String id;
        public final TagKey<Item> tag;
        final String icone;
        RegistryObject<CreativeModeTab> aba;

        Aba(String id, String tag, String icone) {
            this.id = id;
            this.tag = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(DCUniverse.MODID, "aba/" + tag));
            this.icone = icone;
        }

        public CreativeModeTab get() {
            return aba.get();
        }

        /** A aba do item: a primeira tag de aba que ele tem, ou Utilidades. */
        static Aba de(HolderLookup.Provider registros, Item item) {
            var itens = registros.lookupOrThrow(Registries.ITEM);
            for (Aba a : values()) {
                if (a != UTILIDADES && itens.get(a.tag).map(t -> t.contains(item.builtInRegistryHolder())).orElse(false)) return a;
            }
            return UTILIDADES;
        }
    }

    static {
        Aba antes = null;
        for (Aba a : Aba.values()) {
            Aba anterior = antes;
            a.aba = ABAS.register(a.id, () -> {
                CreativeModeTab.Builder b = CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + a.id))
                    .icon(() -> new ItemStack(ITEMS.getEntries().stream().filter(r -> r.getId().getPath().equals(a.icone))
                        .findFirst().map(RegistryObject::get).orElse(CRISTAL_SUPERMAN.get())))
                    .displayItems((params, out) -> ITEMS.getEntries().stream()
                        .filter(r -> !(r.get() instanceof TrajeItem) && Aba.de(params.holders(), r.get()) == a)
                        .forEach(r -> out.accept(r.get())));
                if (anterior != null) b.withTabsBefore(Identifier.fromNamespaceAndPath(DCUniverse.MODID, anterior.id));
                return b.build();
            });
            antes = a;
        }
    }

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
