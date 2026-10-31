package com.danrod.dcuniverse.npc;

import com.danrod.dcuniverse.content.Itens;
import com.danrod.dcuniverse.core.Persistente;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.common.NeoForge;
import com.danrod.dcuniverse.core.Eventos;
import java.util.function.Predicate;

/**
 * Coleira do S: a capinha vermelha com o S do Krypto e do Streaky para qualquer cão ou gato domado do jogador. Fica
 * guardada nos dados do animal e vai para os clientes por pacote (Rede.Coleira); tesoura tira e devolve a coleira, e
 * ela cai se o animal morrer. Quem doma o Krypto pela primeira vez ganha uma (conquista Melhor Amigo do Homem de Aço).
 */
public final class Coleira {
    public static final String CHAVE = "dcuniverse_coleira";
    private static final String DADA = "coleira_dada";
    private static final String RAIZ = "dcuniverse_mascotes";

    private Coleira() {}

    /** Item da coleira. */
    public static final class ColeiraItem extends Item {
        public ColeiraItem(Properties p) {
            super(p);
        }

        /** Só alcança animais que não respondem ao clique (os domados passam antes pelo evento em registrar()). */
        @Override
        public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity alvo, InteractionHand hand) {
            return alvo instanceof TamableAnimal pet && aceita(pet) ? vestir(player, stack, pet) : InteractionResult.PASS;
        }
    }

    /** Veste a coleira no animal se ele é do jogador, não tem capa própria e ainda não usa uma. */
    public static InteractionResult vestir(Player player, ItemStack stack, TamableAnimal pet) {
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        if (!pet.isOwnedBy(player)) {
            avisar(sp, "dcuniverse.coleira.nao_e_seu");
        } else if (pet instanceof Krypto || pet instanceof Streaky) {
            avisar(sp, "dcuniverse.coleira.ja_tem_capa");
        } else if (usa(pet)) {
            avisar(sp, "dcuniverse.coleira.ja_usa");
        } else {
            definir(pet, true);
            stack.consume(1, player);
            pet.level().playSound(null, pet.getX(), pet.getY(), pet.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.NEUTRAL, 1f, 1.2f);
            ((ServerLevel) pet.level()).sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, pet.getX(), pet.getY() + pet.getBbHeight(), pet.getZ(), 3, 0.3, 0.2, 0.3, 0);
        }
        return InteractionResult.SUCCESS;
    }

    /** Cães e gatos (o Krypto e o Streaky já nascem com a capa). */
    public static boolean aceita(TamableAnimal pet) {
        return pet instanceof Wolf || pet instanceof Cat;
    }

    public static boolean usa(Entity e) {
        if (e.level().isClientSide()) return com.danrod.dcuniverse.core.EstadoCliente.COLEIRAS.contains(e.getId());
        return e.getPersistentData().getBooleanOr(CHAVE, false);
    }

    public static void definir(Entity e, boolean usa) {
        if (usa) e.getPersistentData().putBoolean(CHAVE, true);
        else e.getPersistentData().remove(CHAVE);
        Rede.coleira(e, usa);
    }

    /** Recompensa de domar o Krypto: uma coleira, só na primeira vez. */
    public static void recompensa(ServerPlayer p) {
        CompoundTag g = Persistente.ler(p, RAIZ);
        if (g.getBooleanOr(DADA, false)) return;
        g.putBoolean(DADA, true);
        Persistente.salvar(p, RAIZ, g);
        ItemStack c = new ItemStack(Itens.COLEIRA_S.get());
        if (!p.getInventory().add(c)) p.drop(c, false);
        if (p.connection != null) {
            p.sendSystemMessage(Component.translatable("dcuniverse.coleira.conquista").withStyle(net.minecraft.ChatFormatting.GOLD));
        }
    }

    private static void avisar(ServerPlayer p, String chave) {
        if (p.connection != null) p.sendOverlayMessage(Component.translatable(chave));
    }

    public static void registrar() {
        // Coleira num cão ou gato: roda antes do clique do animal, senão o domado só senta/levanta.
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.EntityInteract.class, Eventos.cancela((Predicate<PlayerInteractEvent.EntityInteract>) e -> {
            if (!e.getItemStack().is(Itens.COLEIRA_S.get()) || !(e.getTarget() instanceof TamableAnimal pet) || !aceita(pet)) return false;
            e.setCancellationResult(vestir(e.getEntity(), e.getItemStack(), pet));
            return true;
        }));
        // Tesoura no próprio animal: tira a coleira e devolve o item.
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.EntityInteract.class, Eventos.cancela((Predicate<PlayerInteractEvent.EntityInteract>) e -> {
            Entity alvo = e.getTarget();
            if (!e.getItemStack().is(Items.SHEARS) || !usa(alvo) || !(alvo instanceof TamableAnimal pet) || !pet.isOwnedBy(e.getEntity())) return false;
            if (e.getEntity() instanceof ServerPlayer sp) {
                definir(pet, false);
                ItemStack c = new ItemStack(Itens.COLEIRA_S.get());
                if (!sp.getInventory().add(c)) sp.drop(c, false);
                pet.level().playSound(null, pet.getX(), pet.getY(), pet.getZ(), SoundEvents.SHEEP_SHEAR, SoundSource.NEUTRAL, 0.8f, 1.3f);
            }
            e.setCancellationResult(InteractionResult.SUCCESS);
            return true;
        }));
        NeoForge.EVENT_BUS.addListener(LivingDeathEvent.class, e -> {
            LivingEntity morto = e.getEntity();
            if (usa(morto) && morto.level() instanceof ServerLevel level) {
                morto.spawnAtLocation(level, new ItemStack(Itens.COLEIRA_S.get()));
                morto.getPersistentData().remove(CHAVE);
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.StartTracking.class, e -> {
            if (e.getEntity() instanceof ServerPlayer quem && usa(e.getTarget())) Rede.coleiraPara(quem, e.getTarget());
        });
    }
}
