package com.danrod.dcuniverse.ability.passive;

import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.ability.Passiva;
import com.danrod.dcuniverse.ability.Passivas;
import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.guia.Guia;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * counter (Contra-ataque): apertar agachar (ou levantar o escudo) logo antes de levar um golpe é a defesa. Se o golpe
 * chega dentro de window segundos, o dano é anulado e o Batman devolve: corpo a corpo, golpeia quem atacou
 * (damage_mult do dano da ficha), atordoa por stun segundos e empurra; projétil, rebate de volta. Depois de um
 * contra-ataque, ou de uma defesa no tempo errado, espera cooldown segundos para abrir outra janela (sem spam).
 */
public final class ContraAtaque implements Passiva {
    /** Por jogador: [tick em que abriu a janela, tick em que pode abrir a próxima]. */
    private static final Map<UUID, long[]> JANELAS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> AGACHADO = new ConcurrentHashMap<>();

    public static void registrar() {
        LivingAttackEvent.BUS.addListener((Predicate<LivingAttackEvent>) e -> e.getEntity() instanceof ServerPlayer sp
            && defender(sp, e.getSource().getEntity(), e.getSource().getDirectEntity()));
    }

    @Override
    public void tick(ServerPlayer player, Params p) {
        boolean agora = player.isShiftKeyDown() || player.isBlocking();
        Boolean antes = AGACHADO.put(player.getUUID(), agora);
        if (!agora || Boolean.TRUE.equals(antes)) return;
        long t = player.level().getGameTime();
        long[] j = JANELAS.computeIfAbsent(player.getUUID(), k -> new long[]{Long.MIN_VALUE, Long.MIN_VALUE});
        if (t < j[1]) return;
        j[0] = t;
        // Errou o tempo: a próxima janela só depois do cooldown.
        j[1] = t + Math.round(p.f("cooldown", 1) * 20);
    }

    @Override
    public void aoDestransformar(ServerPlayer player, Params p) {
        JANELAS.remove(player.getUUID());
        AGACHADO.remove(player.getUUID());
    }

    /** Na janela? Então anula o golpe e devolve. */
    public static boolean defender(ServerPlayer player, Entity atacante, Entity direto) {
        Params prm = Passivas.params(player, "counter");
        long[] j = JANELAS.get(player.getUUID());
        if (prm == null || j == null || !(player.level() instanceof ServerLevel level)) return false;
        long t = level.getGameTime();
        if (j[0] == Long.MIN_VALUE || t - j[0] > Math.round(prm.f("window", 0.4f) * 20)) return false;
        boolean projetil = direto instanceof Projectile;
        if (!projetil && !(atacante instanceof LivingEntity)) return false;
        if (!projetil && atacante.distanceToSqr(player) > 25) return false;
        j[0] = Long.MIN_VALUE;
        j[1] = t + Math.round(prm.f("cooldown", 1) * 20);
        Personagem p = EstadoJogador.de(player).personagem();
        Vec3 onde;
        if (projetil) {
            Projectile proj = (Projectile) direto;
            Vec3 v = proj.getDeltaMovement();
            Vec3 volta = atacante != null ? atacante.getEyePosition().subtract(proj.position()).normalize().scale(Math.max(1.2, v.length()))
                : v.scale(-1.1);
            proj.setOwner(player);
            proj.setDeltaMovement(volta);
            proj.hurtMarked = true;
            onde = proj.position();
        } else {
            LivingEntity alvo = (LivingEntity) atacante;
            if (p != null) Alvos.ferir(player, alvo, player.damageSources().playerAttack(player), Alvos.dano(p, prm.d("damage_mult", 1.5)));
            int atordoa = Math.round(prm.f("stun", 1.5f) * 20);
            alvo.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, atordoa, 4), player);
            alvo.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, atordoa, 1), player);
            Vec3 fora = alvo.position().subtract(player.position()).multiply(1, 0, 1);
            fora = fora.lengthSqr() < 1e-4 ? player.getLookAngle() : fora.normalize();
            alvo.knockback(prm.d("knockback", 1.2), -fora.x, -fora.z, player.damageSources().playerAttack(player), 0);
            if (player.connection != null) player.swing(InteractionHand.MAIN_HAND, true);
            onde = alvo.getEyePosition().add(player.getEyePosition()).scale(0.5);
        }
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, onde.x, onde.y, onde.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.CRIT, onde.x, onde.y, onde.z, 14, 0.25, 0.25, 0.25, 0.35);
        level.playSound(null, onde.x, onde.y, onde.z, Sons.CONTRA_ATAQUE.get(), SoundSource.PLAYERS, 1.1f, 0.95f + level.getRandom().nextFloat() * 0.1f);
        Rede.efeitoPara(player, "counter", onde.x, onde.y, onde.z, projetil ? 1 : 0, 0, 0xFFFFFF);
        Guia.concluir(player, "contra_ataque");
        return true;
    }
}
