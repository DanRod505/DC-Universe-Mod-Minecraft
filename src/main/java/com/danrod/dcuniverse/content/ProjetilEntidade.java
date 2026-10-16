package com.danrod.dcuniverse.content;

import com.danrod.dcuniverse.ability.Alvos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Projétil genérico das habilidades do tipo projectile. O item define o visual; sem item é uma onda de choque
 * (soco sônico): voa reto, deixa o rastro do estrondo sônico e, ao bater, empurra tudo em volta.
 * Não é salvo com o mundo: dura no máximo o alcance da habilidade.
 */
public class ProjetilEntidade extends ThrowableItemProjectile {
    private float dano;
    private float atordoaSeg;
    private float empurrao;
    private float raioImpacto;
    private int vida = 100;

    public ProjetilEntidade(EntityType<? extends ProjetilEntidade> tipo, Level level) {
        super(tipo, level);
    }

    public ProjetilEntidade(LivingEntity dono, ItemStack visual, float dano, float atordoaSeg, float empurrao, float raioImpacto, int vida) {
        super(Entidades.PROJETIL.get(), dono, dono.level(), visual);
        this.dano = dano;
        this.atordoaSeg = atordoaSeg;
        this.empurrao = empurrao;
        this.raioImpacto = raioImpacto;
        this.vida = vida;
    }

    /** Sem item = onda sônica (o cliente sabe pelo item sincronizado). */
    public boolean sonico() {
        return getItem().isEmpty();
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SNOWBALL;
    }

    @Override
    protected double getDefaultGravity() {
        return sonico() ? 0 : super.getDefaultGravity();
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            // A onda só aparece a partir de 2,5 blocos do dono, para não tapar a visão de quem soca.
            Entity dono = getOwner();
            boolean longe = dono == null || dono.getEyePosition().distanceToSqr(position()) > 6.25;
            if (sonico() && isAlive() && longe) {
                level().addParticle(ParticleTypes.SONIC_BOOM, getX(), getY(), getZ(), 0, 0, 0);
                if (tickCount % 2 == 0) level().addParticle(ParticleTypes.CLOUD, getX(), getY(), getZ(), 0, 0, 0);
            }
        } else if (tickCount > vida && isAlive()) {
            impacto(position(), null);
            discard();
        }
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
        if (alvo instanceof LivingEntity vivo) {
            if (atordoaSeg > 0) vivo.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, (int) (atordoaSeg * 20), 4), getOwner());
            empurrar(vivo, getDeltaMovement().normalize(), empurrao);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (!(level() instanceof ServerLevel level)) return;
        BlockState estado = level.getBlockState(hit.getBlockPos());
        // Corta teias (batarangue).
        if (estado.is(Blocks.COBWEB) && (!(getOwner() instanceof ServerPlayer dono) || level.mayInteract(dono, hit.getBlockPos()))) {
            level.destroyBlock(hit.getBlockPos(), true, getOwner());
        }
        // Aciona botões à distância.
        if (estado.getBlock() instanceof ButtonBlock botao && !estado.getValue(ButtonBlock.POWERED)) {
            botao.press(estado, level, hit.getBlockPos(), getOwner() instanceof Player pl ? pl : null);
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide()) {
            impacto(hit.getLocation(), hit instanceof EntityHitResult eh ? eh.getEntity() : null);
            if (!sonico()) level().broadcastEntityEvent(this, (byte) 3);
            discard();
        }
    }

    /** Onda de choque no ponto do impacto: empurra para fora e fere (metade do dano) quem está perto. */
    private void impacto(Vec3 onde, Entity jaAtingido) {
        if (raioImpacto <= 0 || !(level() instanceof ServerLevel level)) return;
        level.sendParticles(ParticleTypes.EXPLOSION, onde.x, onde.y, onde.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.CLOUD, onde.x, onde.y, onde.z, 24, 0.4, 0.4, 0.4, 0.25);
        level.playSound(null, onde.x, onde.y, onde.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7f, 1.4f);
        ServerPlayer dono = getOwner() instanceof ServerPlayer sp ? sp : null;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(onde, onde).inflate(raioImpacto),
            e -> e != jaAtingido && e.distanceToSqr(onde) <= raioImpacto * raioImpacto)) {
            if (dono == null ? e == getOwner() : !Alvos.podeAtingir(dono, e)) continue;
            if (dono != null) Alvos.ferir(dono, e, damageSources().thrown(this, dono), dano / 2);
            Vec3 fora = e.position().subtract(onde);
            empurrar(e, fora.lengthSqr() < 1e-4 ? getDeltaMovement().normalize() : fora.normalize(),
                empurrao * (float) (1 - Math.sqrt(e.distanceToSqr(onde)) / (raioImpacto + 1)));
        }
    }

    private static void empurrar(LivingEntity e, Vec3 dir, float forca) {
        if (forca <= 0) return;
        double resiste = 1 - e.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
        if (resiste <= 0) return;
        e.push(dir.x * forca * resiste, 0.35 * forca * resiste, dir.z * forca * resiste);
        e.hurtMarked = true;
    }
}
