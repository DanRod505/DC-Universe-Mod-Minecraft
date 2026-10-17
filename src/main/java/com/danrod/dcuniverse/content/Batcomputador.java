package com.danrod.dcuniverse.content;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.ability.Cargas;
import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.energy.Recargas;
import com.danrod.dcuniverse.guia.Guia;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Batcomputador (o bloco é {@link BatcomputadorBloco}): o clique direito abre a tela (client/BatcomputadorScreen) com
 * quatro abas. O servidor decide tudo e reenvia o estado depois de cada ação:
 * - Gadgets: herói com recarga tecnológica (Batman) enche o cinto e a energia na hora, a cada {@link #RECARGA_SEG} s;
 *   o bloco também é estação de recarga (tag dcuniverse:recarga_tecnologica) para quem fica perto;
 * - Fabricar: as receitas tecnológicas do mod (as que usam ou fazem placas, componentes, núcleos e prométio) saem
 *   direto do inventário, sem montar a grade;
 * - Análise: uma amostra (o drop típico do mob) registra o tipo no computador do jogador; mobs analisados (e os da
 *   mesma família) levam +{@link #BONUS_PCT}% de dano dele. Fica nos dados persistentes (sobrevive à morte);
 * - Mapa: o terreno em volta, com mobs e jogadores, desenhado no cliente com os chunks que ele já tem.
 */
public final class Batcomputador {
    public static final int RECARGA_SEG = 60, BONUS_PCT = 15, ALCANCE_USO = 8, MAPA_RAIO = 36;
    public static final int RECARREGAR = 0, FABRICAR = 1, ANALISAR = 2;
    private static final String RAIZ = "dcuniverse_batcomputador";
    private static final Map<UUID, Long> RECARREGOU = new ConcurrentHashMap<>();
    private static Map<Item, EntityType<?>> amostras;
    private static Map<EntityType<?>, EntityType<?>> familias;

    private Batcomputador() {}

    /** Amostra de cada mob analisável, na ordem da tela. */
    public static synchronized Map<Item, EntityType<?>> amostras() {
        if (amostras == null) {
            Map<Item, EntityType<?>> m = new LinkedHashMap<>();
            m.put(Items.ROTTEN_FLESH, EntityTypes.ZOMBIE);
            m.put(Items.BONE, EntityTypes.SKELETON);
            m.put(Items.STRING, EntityTypes.SPIDER);
            m.put(Items.GUNPOWDER, EntityTypes.CREEPER);
            m.put(Items.ENDER_PEARL, EntityTypes.ENDERMAN);
            m.put(Items.SLIME_BALL, EntityTypes.SLIME);
            m.put(Items.PHANTOM_MEMBRANE, EntityTypes.PHANTOM);
            m.put(Items.PRISMARINE_SHARD, EntityTypes.GUARDIAN);
            m.put(Items.GOLD_NUGGET, EntityTypes.ZOMBIFIED_PIGLIN);
            m.put(Items.MAGMA_CREAM, EntityTypes.MAGMA_CUBE);
            m.put(Items.BLAZE_ROD, EntityTypes.BLAZE);
            m.put(Items.GHAST_TEAR, EntityTypes.GHAST);
            m.put(Items.BREEZE_ROD, EntityTypes.BREEZE);
            m.put(Items.WITHER_SKELETON_SKULL, EntityTypes.WITHER_SKELETON);
            m.put(Items.SHULKER_SHELL, EntityTypes.SHULKER);
            amostras = m;
        }
        return amostras;
    }

    /** Parentes que a análise de um tipo também cobre (a do zumbi vale para o afogado, a do esqueleto para o errante...). */
    public static synchronized EntityType<?> familia(EntityType<?> tipo) {
        if (familias == null) {
            Map<EntityType<?>, EntityType<?>> f = new ConcurrentHashMap<>();
            for (EntityType<?> t : List.of(EntityTypes.HUSK, EntityTypes.DROWNED, EntityTypes.ZOMBIE_VILLAGER)) f.put(t, EntityTypes.ZOMBIE);
            for (EntityType<?> t : List.of(EntityTypes.STRAY, EntityTypes.BOGGED)) f.put(t, EntityTypes.SKELETON);
            f.put(EntityTypes.CAVE_SPIDER, EntityTypes.SPIDER);
            f.put(EntityTypes.ELDER_GUARDIAN, EntityTypes.GUARDIAN);
            familias = f;
        }
        return familias.getOrDefault(tipo, tipo);
    }

    public static void registrar() {
        LivingHurtEvent.BUS.addListener(e -> {
            if (!(e.getEntity() instanceof Mob m) || !(e.getSource().getEntity() instanceof ServerPlayer sp)) return;
            if (analisados(sp).contains(id(familia(m.getType())))) e.setAmount(e.getAmount() * (1 + BONUS_PCT / 100f));
        });
    }

    // ---------------------------------------------------------------- tela

    public static void abrir(ServerPlayer player, BlockPos pos) {
        Rede.CANAL.send(montar(player, pos), net.minecraftforge.network.PacketDistributor.PLAYER.with(player));
    }

    public static Rede.BatAbrir montar(ServerPlayer player, BlockPos pos) {
        Personagem p = EstadoJogador.de(player).personagem();
        boolean heroi = p != null && Recargas.tecnologica(p);
        return new Rede.BatAbrir(pos, heroi, recargaFalta(player), fabricaveis(player), List.copyOf(analisados(player)));
    }

    public static void acao(ServerPlayer player, Rede.BatAcao a) {
        if (!(player.level().getBlockState(a.pos()).getBlock() instanceof BatcomputadorBloco)
            || player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(a.pos())) > ALCANCE_USO * ALCANCE_USO) return;
        switch (a.tipo()) {
            case RECARREGAR -> recarregar(player, a.pos());
            case FABRICAR -> fabricar(player, a.pos(), a.arg());
            case ANALISAR -> analisar(player, a.pos(), a.arg());
            default -> { }
        }
        abrir(player, a.pos());
    }

    // ---------------------------------------------------------------- gadgets

    private static int recargaFalta(ServerPlayer player) {
        Long em = RECARREGOU.get(player.getUUID());
        if (em == null || player.isCreative()) return 0;
        return (int) Math.max(0, (em + RECARGA_SEG * 20L - player.level().getGameTime() + 19) / 20);
    }

    public static void recarregar(ServerPlayer player, BlockPos pos) {
        EstadoJogador e = EstadoJogador.de(player);
        Personagem p = e.personagem();
        if (p == null || !Recargas.tecnologica(p) || recargaFalta(player) > 0) return;
        Cargas.encher(player, p);
        e.setEnergia(p.energia().max());
        e.setEsgotado(false);
        RECARREGOU.put(player.getUUID(), player.level().getGameTime());
        Rede.sincronizar(player);
        player.level().playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.7f, 1.6f);
        player.level().sendParticles(new DustParticleOptions(0x4FE3FF, 0.8f), player.getX(), player.getY() + 1, player.getZ(),
            24, 0.4, 0.6, 0.4, 0.02);
    }

    // ---------------------------------------------------------------- fabricar

    /** Itens que marcam uma receita como tecnológica (ela faz ou usa um deles). */
    private static Set<Item> tecnologicos() {
        return Set.of(Itens.PLACA_CIRCUITO.get(), Itens.COMPONENTE_TECNOLOGICO.get(), Itens.NUCLEO_ENERGIA.get(),
            Itens.LINGOTE_PROMETIO.get());
    }

    private record Custo(Ingredient ingrediente, int qtd) {}

    private record Fabricavel(String id, ItemStack resultado, List<Custo> custos) {}

    private static List<Fabricavel> receitas(ServerPlayer player) {
        Set<Item> tec = tecnologicos();
        List<Fabricavel> out = new ArrayList<>();
        for (RecipeHolder<?> h : player.level().getServer().getRecipeManager().getRecipes()) {
            if (!h.id().identifier().getNamespace().equals(DCUniverse.MODID) || !(h.value() instanceof CraftingRecipe r)) continue;
            ItemStack res;
            try {
                res = r.assemble(CraftingInput.EMPTY);
            } catch (RuntimeException ex) {
                continue;
            }
            if (res.isEmpty()) continue;
            List<Ingredient> ings = r.placementInfo().ingredients();
            boolean usa = ings.stream().anyMatch(i -> i.items().anyMatch(it -> tec.contains(it.value())));
            if (!tec.contains(res.getItem()) && !usa) continue;
            List<Custo> custos = new ArrayList<>();
            for (Ingredient i : ings) {
                int k = -1;
                for (int j = 0; j < custos.size(); j++) if (custos.get(j).ingrediente().equals(i)) k = j;
                if (k < 0) custos.add(new Custo(i, 1));
                else custos.set(k, new Custo(i, custos.get(k).qtd() + 1));
            }
            out.add(new Fabricavel(h.id().identifier().getPath(), res, custos));
        }
        out.sort((a, b) -> BuiltInRegistries.ITEM.getKey(a.resultado().getItem()).getPath()
            .compareTo(BuiltInRegistries.ITEM.getKey(b.resultado().getItem()).getPath()));
        return out;
    }

    private static int tem(Inventory inv, Ingredient i) {
        int n = 0;
        for (int s = 0; s < inv.getContainerSize(); s++) {
            ItemStack st = inv.getItem(s);
            if (!st.isEmpty() && i.test(st)) n += st.getCount();
        }
        return n;
    }

    private static List<Rede.BatFab> fabricaveis(ServerPlayer player) {
        List<Rede.BatFab> out = new ArrayList<>();
        for (Fabricavel f : receitas(player)) {
            List<Rede.BatCusto> cs = new ArrayList<>();
            boolean pode = true;
            for (Custo c : f.custos()) {
                Holder<Item> rep = c.ingrediente().items().findFirst().orElse(null);
                if (rep == null) continue;
                int t = player.getAbilities().instabuild ? c.qtd() : tem(player.getInventory(), c.ingrediente());
                pode &= t >= c.qtd();
                cs.add(new Rede.BatCusto(BuiltInRegistries.ITEM.getKey(rep.value()).toString(), c.qtd(), t));
            }
            out.add(new Rede.BatFab(f.id(), BuiltInRegistries.ITEM.getKey(f.resultado().getItem()).toString(),
                f.resultado().getCount(), List.copyOf(cs), pode));
        }
        return out;
    }

    public static void fabricar(ServerPlayer player, BlockPos pos, String receita) {
        Fabricavel f = receitas(player).stream().filter(x -> x.id().equals(receita)).findFirst().orElse(null);
        if (f == null) return;
        Inventory inv = player.getInventory();
        if (!player.getAbilities().instabuild) {
            for (Custo c : f.custos()) if (tem(inv, c.ingrediente()) < c.qtd()) return;
            for (Custo c : f.custos()) {
                int falta = c.qtd();
                for (int s = 0; s < inv.getContainerSize() && falta > 0; s++) {
                    ItemStack st = inv.getItem(s);
                    if (st.isEmpty() || !c.ingrediente().test(st)) continue;
                    int tira = Math.min(falta, st.getCount());
                    st.shrink(tira);
                    falta -= tira;
                }
            }
        }
        ItemStack res = f.resultado().copy();
        if (!inv.add(res)) player.drop(res, false);
        inv.setChanged();
        player.level().playSound(null, pos, SoundEvents.CRAFTER_CRAFT, SoundSource.BLOCKS, 0.9f, 1.2f);
        player.level().playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.4f, 1.8f);
    }

    // ---------------------------------------------------------------- análise

    public static String id(EntityType<?> t) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(t).toString();
    }

    public static List<String> analisados(ServerPlayer player) {
        List<String> out = new ArrayList<>();
        for (Tag t : dados(player).getListOrEmpty("analisados")) t.asString().ifPresent(out::add);
        return out;
    }

    public static void analisar(ServerPlayer player, BlockPos pos, String itemId) {
        Item item = BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.tryParse(itemId));
        EntityType<?> tipo = amostras().get(item);
        if (tipo == null || analisados(player).contains(id(tipo))) return;
        if (!player.getAbilities().instabuild) {
            Inventory inv = player.getInventory();
            int slot = -1;
            for (int s = 0; s < inv.getContainerSize(); s++) if (inv.getItem(s).is(item)) slot = s;
            if (slot < 0) return;
            inv.getItem(slot).shrink(1);
        }
        CompoundTag g = dados(player);
        ListTag l = g.getListOrEmpty("analisados");
        l.add(StringTag.valueOf(id(tipo)));
        g.put("analisados", l);
        salvar(player, g);
        Guia.concluir(player, "batcomputador");
        player.level().playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.5f, 1.8f);
    }

    /** Apaga as análises (comando de teste e GameTests). */
    public static void resetar(ServerPlayer player) {
        salvar(player, new CompoundTag());
        RECARREGOU.remove(player.getUUID());
    }

    private static CompoundTag dados(ServerPlayer player) {
        return player.getPersistentData().getCompoundOrEmpty(ServerPlayer.PERSISTED_NBT_TAG).getCompoundOrEmpty(RAIZ);
    }

    private static void salvar(ServerPlayer player, CompoundTag g) {
        CompoundTag dados = player.getPersistentData();
        CompoundTag persistente = dados.getCompoundOrEmpty(ServerPlayer.PERSISTED_NBT_TAG);
        persistente.put(RAIZ, g);
        dados.put(ServerPlayer.PERSISTED_NBT_TAG, persistente);
    }
}
