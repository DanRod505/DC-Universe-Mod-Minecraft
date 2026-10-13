package com.danrod.dcuniverse.content;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import com.danrod.dcuniverse.ability.Alvos;

/** Projétil genérico das habilidades do tipo projectile. O item define só o visual. */
public class ProjetilEntidade extends ThrowableItemProjectile {
    private float dano;
    private float atordoaSeg;

    public ProjetilEntidade(EntityType<? extends ProjetilEntidade> tipo, Level level) {
        super(tipo, level);
    }

    public ProjetilEntidade(LivingEntity dono, ItemStack visual, float dano, float atordoaSeg) {
        super(Entidades.PROJETIL.get(), dono, dono.level(), visual);
        this.dano = dano;
        this.atordoaSeg = atordoaSeg;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SNOWBALL;
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        Entity alvo = hit.getEntity();
        if (getOwner() instanceof ServerPlayer dono) {
            if (!Alvos.podeAtingir(dono, alvo)) return;
            Alvos.ferir(dono, alvo, damageSources().thrown(this, dono), dano);
        } else if (level() instanceof ServerLevel level && alvo instanceof LivingEntity vivo) {
            vivo.hurtServer(level, damageSources().thrown(this, getOwner()), dano);
        }
        if (atordoaSeg > 0 && alvo instanceof LivingEntity vivo) {
            vivo.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, (int) (atordoaSeg * 20), 4), getOwner());
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        // Corta teias (batarangue).
        if (level() instanceof ServerLevel level && level.getBlockState(hit.getBlockPos()).is(Blocks.COBWEB)
            && (!(getOwner() instanceof ServerPlayer dono) || level.mayInteract(dono, hit.getBlockPos()))) {
            level.destroyBlock(hit.getBlockPos(), true, getOwner());
        }
        // Aciona botões à distância.
        if (!level().isClientSide()) {
            BlockState estado = level().getBlockState(hit.getBlockPos());
            if (estado.getBlock() instanceof ButtonBlock botao && !estado.getValue(ButtonBlock.POWERED)) {
                botao.press(estado, level(), hit.getBlockPos(), getOwner() instanceof Player pl ? pl : null);
            }
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide()) {
            level().broadcastEntityEvent(this, (byte) 3);
            discard();
        }
    }
}
