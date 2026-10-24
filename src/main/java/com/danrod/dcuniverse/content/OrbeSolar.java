package com.danrod.dcuniverse.content;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Orbe solar da nave kryptoniana: uma bola de luz do sol amarelo que sai do nariz, persegue o alvo na mira como o
 * míssil do Batwing (mais rápida e virando mais) e estoura num clarão que fere e empurra em volta, sem quebrar blocos.
 */
public class OrbeSolar extends MissilEntidade {
    public OrbeSolar(EntityType<? extends OrbeSolar> tipo, Level level) {
        super(tipo, level);
    }

    public OrbeSolar(Level level, ServerPlayer dono, Vec3 de, Vec3 dir, double velInicial, Entity alvo, float dano, float raio) {
        super(Entidades.ORBE_SOLAR.get(), level, dono, de, dir, Math.max(1.2, velInicial), alvo, dano, raio);
    }

    @Override
    protected int acende() {
        return 1;
    }

    @Override
    protected float velMaxima() {
        return 3.2f;
    }

    @Override
    protected void rastro() {
        var r = random;
        for (int i = 0; i < 2; i++) {
            level().addParticle(new DustParticleOptions(r.nextBoolean() ? 0xFFE27A : 0xFFF6D0, 0.9f + r.nextFloat() * 0.6f),
                getX() + r.nextGaussian() * 0.12, getY() + 0.2 + r.nextGaussian() * 0.12, getZ() + r.nextGaussian() * 0.12, 0, 0, 0);
        }
        if (r.nextInt(3) == 0) level().addParticle(ParticleTypes.END_ROD, getX(), getY() + 0.2, getZ(), 0, 0, 0);
    }

    @Override
    protected void estouro(ServerLevel level, Vec3 onde) {
        level.sendParticles(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFE27A), onde.x, onde.y, onde.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.END_ROD, onde.x, onde.y, onde.z, 40, 0.3, 0.3, 0.3, 0.35);
        level.sendParticles(new DustParticleOptions(0xFFE27A, 2.2f), onde.x, onde.y, onde.z, 30, 1.0, 1.0, 1.0, 0);
        level.sendParticles(ParticleTypes.SMALL_FLAME, onde.x, onde.y, onde.z, 16, 0.6, 0.6, 0.6, 0.08);
        level.playSound(null, onde.x, onde.y, onde.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.8f, 1.35f);
        level.playSound(null, onde.x, onde.y, onde.z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.5f, 0.7f);
    }
}
