package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.character.Transformacao;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * disruptor: um pulso eletrônico na mira (até range). O alvo leva um choque que paralisa (stun, até 4 s) e é
 * desarmado: a arma na mão do mob (arco, besta, tridente, espada, machado) queima e se desfaz, e o creeper fica
 * sem pavio por duration segundos (não consegue explodir). TNT acesa perto da linha da mira é desarmada e volta a
 * ser um bloco de TNT no chão. Jogadores só levam o choque. Sem nada para desarmar, não gasta a carga.
 */
public final class Disruptor implements TipoHabilidade {
    /** Creepers sem pavio -> tick em que voltam ao normal. */
    private static final Map<Creeper, Long> SEM_PAVIO = new HashMap<>();

    @Override
    public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        Params prm = h.params();
        double alcance = prm.d("range", 32);
        HitResult mira = Alvos.mirar(player, alcance);
        Vec3 de = player.getEyePosition().add(player.getLookAngle().scale(0.5)).add(0, -0.25, 0);
        Vec3 ate = mira.getLocation();
        boolean fez = desarmarTnt(level, player, player.getEyePosition(), ate);
        if (mira instanceof EntityHitResult eh && eh.getEntity() instanceof LivingEntity alvo) {
            fez = true;
            int stun = Math.round(Math.min(4f, prm.f("stun", 2f)) * 20);
            alvo.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, stun, 6), player);
            alvo.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, stun, 2), player);
            if (alvo instanceof Creeper c) {
                SEM_PAVIO.put(c, level.getGameTime() + Math.round(prm.f("duration", 10f) * 20));
                c.setSwellDir(-1);
            } else if (!(alvo instanceof Player)) {
                desarmar(level, alvo);
            }
            if (alvo instanceof Mob m) m.getNavigation().stop();
            ate = alvo.getBoundingBox().getCenter();
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, ate.x, ate.y, ate.z, 25, alvo.getBbWidth() / 2, alvo.getBbHeight() / 2,
                alvo.getBbWidth() / 2, 0.15);
            level.playSound(null, ate.x, ate.y, ate.z, Sons.CHOQUE.get(), SoundSource.PLAYERS, 1f, 1.4f);
        }
        if (!fez) {
            Transformacao.avisar(player, Component.translatable("dcuniverse.msg.disruptor_sem_alvo"));
            return false;
        }
        Rede.efeito(level, de.x, de.y, de.z, "arco", (float) (ate.x - de.x), (float) (ate.z - de.z), Float.floatToIntBits((float) (ate.y - de.y)));
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.7f, 2f);
        player.swing(InteractionHand.MAIN_HAND, true);
        return true;
    }

    /** A arma da mão principal queima: some com estalo e faíscas (não cai, para não virar fonte de arcos). */
    private static void desarmar(ServerLevel level, LivingEntity alvo) {
        ItemStack arma = alvo.getItemBySlot(EquipmentSlot.MAINHAND);
        if (arma.isEmpty()) return;
        boolean e = arma.is(Items.BOW) || arma.is(Items.CROSSBOW) || arma.is(Items.TRIDENT)
            || arma.is(net.minecraft.tags.ItemTags.SWORDS) || arma.is(net.minecraft.tags.ItemTags.AXES);
        if (!e) return;
        Vec3 m = alvo.getEyePosition();
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, arma.getItem()), m.x, m.y - 0.4, m.z, 14, 0.2, 0.2, 0.2, 0.12);
        level.sendParticles(ParticleTypes.SMOKE, m.x, m.y - 0.4, m.z, 8, 0.15, 0.15, 0.15, 0.02);
        level.playSound(null, m.x, m.y, m.z, SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 1f, 0.9f);
        alvo.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    }

    /** TNT acesa a até 2 blocos da linha da mira: o pavio apaga e ela cai como bloco de TNT. */
    private static boolean desarmarTnt(ServerLevel level, ServerPlayer player, Vec3 de, Vec3 ate) {
        boolean fez = false;
        for (PrimedTnt t : level.getEntitiesOfClass(PrimedTnt.class, new AABB(de, ate).inflate(2))) {
            Vec3 c = t.getBoundingBox().getCenter();
            if (distanciaLinha(de, ate, c) > 2) continue;
            t.discard();
            t.spawnAtLocation(level, new ItemStack(Items.TNT));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z, 20, 0.3, 0.3, 0.3, 0.1);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8f, 1.4f);
            fez = true;
        }
        return fez;
    }

    private static double distanciaLinha(Vec3 a, Vec3 b, Vec3 p) {
        Vec3 ab = b.subtract(a);
        double t = ab.lengthSqr() < 1e-6 ? 0 : Math.clamp(p.subtract(a).dot(ab) / ab.lengthSqr(), 0, 1);
        return a.add(ab.scale(t)).distanceTo(p);
    }

    /** Tick do servidor: creeper desarmado não consegue inchar até o prazo acabar. */
    public static void tick() {
        var it = SEM_PAVIO.entrySet().iterator();
        while (it.hasNext()) {
            var en = it.next();
            Creeper c = en.getKey();
            if (c.isRemoved() || !c.isAlive() || c.level().getGameTime() > en.getValue()) {
                it.remove();
                continue;
            }
            c.setSwellDir(-1);
            if (c.tickCount % 6 == 0 && c.level() instanceof ServerLevel l) {
                l.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.getX(), c.getY() + 1.2, c.getZ(), 2, 0.3, 0.4, 0.3, 0.05);
            }
        }
    }

    public static boolean semPavio(Entity e) {
        return e instanceof Creeper c && SEM_PAVIO.containsKey(c);
    }

    public static void limpar() {
        SEM_PAVIO.clear();
    }
}
