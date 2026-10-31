package com.danrod.dcuniverse.ability.passive;

import com.danrod.dcuniverse.ability.Passivas;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.content.Sons;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.function.Predicate;
import com.danrod.dcuniverse.core.Eventos;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Passivas "naturais", que agem pelos eventos do jogo em vez de um tick:
 * super_strength (quebra pedra e minério com a mão; params speed e max_hardness, nunca obsidiana nem bedrock)
 * e projectile_resistance (projéteis fracos ricocheteiam; param max_damage em pontos de vida).
 */
public final class Naturais {
    private Naturais() {}

    public static void registrar() {
        NeoForge.EVENT_BUS.addListener(PlayerEvent.BreakSpeed.class, e -> {
            float vel = velocidadeMao(e.getEntity(), e.getState());
            if (vel > 0) e.setNewSpeed(Math.max(e.getOriginalSpeed(), vel));
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.HarvestCheck.class, e -> {
            if (!e.canHarvest() && velocidadeMao(e.getEntity(), e.getTargetBlock()) > 0) e.setCanHarvest(true);
        });
        NeoForge.EVENT_BUS.addListener(LivingIncomingDamageEvent.class, Eventos.cancela((Predicate<LivingIncomingDamageEvent>) e -> e.getEntity() instanceof ServerPlayer sp
            && ricocheteia(sp, e.getSource().is(DamageTypeTags.IS_PROJECTILE), e.getAmount(), e.getSource().getDirectEntity())));
    }

    /** Velocidade de quebra da superforça com a mão vazia para esse bloco (0 = não vale). */
    public static float velocidadeMao(Player player, BlockState st) {
        if (!player.getMainHandItem().isEmpty()) return 0;
        Params prm = Passivas.params(player, "super_strength");
        if (prm == null || com.danrod.dcuniverse.energy.Kryptonita.exposto(player, 6, 10)) return 0;
        float dureza = st.getBlock().defaultDestroyTime();
        // Obsidiana (50) e bedrock (-1) nunca: BALANCEAMENTO.md.
        if (dureza < 0 || dureza >= Math.min(prm.f("max_hardness", 30), 49)) return 0;
        return prm.f("speed", 9);
    }

    /** Cancela o dano de um projétil fraco e mostra o ricochete. */
    public static boolean ricocheteia(ServerPlayer player, boolean projetil, float dano, Entity projetilEnt) {
        if (!projetil) return false;
        Params prm = Passivas.params(player, "projectile_resistance");
        if (prm == null || dano > prm.f("max_damage", 10) || com.danrod.dcuniverse.energy.Kryptonita.exposto(player, 6, 10)) return false;
        if (player.level() instanceof ServerLevel level) {
            Vec3 onde = projetilEnt != null ? projetilEnt.position() : player.getEyePosition();
            level.playSound(null, onde.x, onde.y, onde.z, Sons.RICOCHETE.get(), SoundSource.PLAYERS, 1f, 0.9f + level.getRandom().nextFloat() * 0.3f);
            level.sendParticles(ParticleTypes.CRIT, onde.x, onde.y, onde.z, 8, 0.1, 0.1, 0.1, 0.25);
        }
        return true;
    }
}
