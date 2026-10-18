package com.danrod.dcuniverse.ability;

import com.danrod.dcuniverse.core.Rede;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Congelamento do sopro: 1 s de sopro contínuo prende o mob num bloco de gelo (parado, sem atacar) por 3 s,
 * até no máximo 4 s se o sopro continuar (teto de controle em BALANCEAMENTO.md); depois fica 2 s imune.
 * Fogo e a visão de calor derretem o gelo na hora. Jogadores e chefes só ficam lentos.
 * A casca de gelo é desenhada no cliente (client/Mundo, efeito "freeze").
 */
public final class Congelamento {
    private Congelamento() {}

    private static final int EXPOSICAO = 2, DURA = 60, TETO = 80, IMUNE = 40;
    private static final String MARCA = "dcuniverse_congelado";

    private static final class Estado {
        int exposicao;
        long ultimaExposicao, inicio, ate, imuneAte;
        boolean congelado, tinhaNoAi;
        float yaw;
    }

    private static final Map<LivingEntity, Estado> ESTADOS = new HashMap<>();

    public static void registrar() {
        // Mob congelado não fere ninguém (nem com o que já tinha disparado).
        LivingAttackEvent.BUS.addListener((Predicate<LivingAttackEvent>) e ->
            e.getSource().getEntity() instanceof LivingEntity atacante && congelado(atacante));
        // Mob que ficou salvo congelado (servidor fechou no meio) volta ao normal.
        EntityJoinLevelEvent.BUS.addListener(e -> {
            if (!e.getLevel().isClientSide() && e.getEntity() instanceof Mob m && m.getPersistentData().getBooleanOr(MARCA, false)) {
                m.getPersistentData().remove(MARCA);
                m.setNoAi(false);
            }
        });
    }

    public static boolean congelado(LivingEntity e) {
        Estado s = ESTADOS.get(e);
        return s != null && s.congelado;
    }

    /** Um golpe do sopro (a cada meio segundo). */
    public static void expor(LivingEntity e) {
        if (!(e.level() instanceof ServerLevel level)) return;
        long agora = level.getGameTime();
        e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2));
        e.setTicksFrozen(Math.min(e.getTicksRequiredToFreeze(), e.getTicksFrozen() + 40));
        e.clearFire();
        if (e instanceof Player || e.is(Tags.EntityTypes.BOSSES) || !(e instanceof Mob)) return;
        Estado s = ESTADOS.computeIfAbsent(e, k -> new Estado());
        if (s.congelado) {
            s.ate = Math.min(s.inicio + TETO, Math.max(s.ate, agora + 20));
            return;
        }
        if (agora < s.imuneAte) return;
        s.exposicao = agora - s.ultimaExposicao > 15 ? 1 : s.exposicao + 1;
        s.ultimaExposicao = agora;
        if (s.exposicao >= EXPOSICAO) congelar(level, (Mob) e, s, agora);
    }

    /**
     * Congela na hora (batarangue congelante), sem precisar da exposição do sopro: o mob fica preso por ticks (até o
     * teto de 4 s); jogadores e chefes só ficam lentos e gelados. Respeita a imunidade de quem acabou de sair do gelo.
     */
    public static void congelarJa(LivingEntity e, int ticks) {
        if (!(e.level() instanceof ServerLevel level)) return;
        long agora = level.getGameTime();
        e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, Math.min(TETO, ticks) + 20, 3));
        e.setTicksFrozen(e.getTicksRequiredToFreeze() + 40);
        e.clearFire();
        if (e instanceof Player || e.is(Tags.EntityTypes.BOSSES) || !(e instanceof Mob m)) return;
        Estado s = ESTADOS.computeIfAbsent(e, k -> new Estado());
        if (s.congelado || agora < s.imuneAte) return;
        s.ultimaExposicao = agora;
        congelar(level, m, s, agora);
        s.ate = agora + Math.min(TETO, Math.max(20, ticks));
    }

    private static void congelar(ServerLevel level, Mob m, Estado s, long agora) {
        s.congelado = true;
        s.inicio = agora;
        s.ate = agora + DURA;
        s.yaw = m.getYRot();
        s.tinhaNoAi = m.isNoAi();
        if (!s.tinhaNoAi) {
            m.setNoAi(true);
            m.getPersistentData().putBoolean(MARCA, true);
        }
        m.getNavigation().stop();
        m.setDeltaMovement(0, Math.min(0, m.getDeltaMovement().y), 0);
        level.playSound(null, m.getX(), m.getY(), m.getZ(), SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1f, 0.7f);
        level.playSound(null, m.getX(), m.getY(), m.getZ(), SoundEvents.POWDER_SNOW_PLACE, SoundSource.PLAYERS, 1f, 0.8f);
        level.sendParticles(ParticleTypes.SNOWFLAKE, m.getX(), m.getY() + m.getBbHeight() / 2, m.getZ(), 30,
            m.getBbWidth() / 2, m.getBbHeight() / 2, m.getBbWidth() / 2, 0.05);
        Rede.efeito(level, m.getX(), m.getY(), m.getZ(), "freeze", m.getId(), TETO, 0);
    }

    /** Fogo, lava ou a visão de calor: o gelo estoura na hora. */
    public static void derreter(LivingEntity e) {
        Estado s = ESTADOS.get(e);
        if (s != null && s.congelado && e.level() instanceof ServerLevel level) soltar(level, e, s, true);
    }

    private static void soltar(ServerLevel level, LivingEntity e, Estado s, boolean derreteu) {
        s.congelado = false;
        s.exposicao = 0;
        s.imuneAte = level.getGameTime() + IMUNE;
        if (e instanceof Mob m && !s.tinhaNoAi) {
            m.setNoAi(false);
            m.getPersistentData().remove(MARCA);
        }
        e.setTicksFrozen(0);
        level.playSound(null, e.getX(), e.getY(), e.getZ(), derreteu ? SoundEvents.FIRE_EXTINGUISH : SoundEvents.GLASS_BREAK,
            SoundSource.PLAYERS, 0.9f, derreteu ? 1.2f : 1.1f);
        level.sendParticles(derreteu ? ParticleTypes.CLOUD : new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()),
            e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), 40, e.getBbWidth() / 2, e.getBbHeight() / 2, e.getBbWidth() / 2, 0.1);
        Rede.efeito(level, e.getX(), e.getY(), e.getZ(), "thaw", e.getId(), 0, 0);
    }

    public static void tick() {
        Iterator<Map.Entry<LivingEntity, Estado>> it = ESTADOS.entrySet().iterator();
        while (it.hasNext()) {
            var en = it.next();
            LivingEntity e = en.getKey();
            Estado s = en.getValue();
            if (!(e.level() instanceof ServerLevel level) || e.isRemoved() || !e.isAlive()) {
                if (s.congelado && e instanceof Mob m && !s.tinhaNoAi) m.setNoAi(false);
                it.remove();
                continue;
            }
            long agora = level.getGameTime();
            if (s.congelado) {
                if (e.isOnFire() || e.isInLava()) {
                    soltar(level, e, s, true);
                } else if (agora >= s.ate) {
                    soltar(level, e, s, false);
                } else {
                    // Parado como estátua: tremendo de frio, de cara para onde estava.
                    e.setTicksFrozen(e.getTicksRequiredToFreeze() + 2);
                    e.setYRot(s.yaw);
                    e.setYHeadRot(s.yaw);
                    e.yBodyRot = s.yaw;
                    e.setDeltaMovement(0, Math.min(0, e.getDeltaMovement().y), 0);
                    if ((agora - s.inicio) % 8 == 0) {
                        level.sendParticles(ParticleTypes.SNOWFLAKE, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 2,
                            e.getBbWidth() / 2, e.getBbHeight() / 3, e.getBbWidth() / 2, 0.01);
                    }
                }
            } else if (agora > s.imuneAte && agora - s.ultimaExposicao > 100) {
                it.remove();
            }
        }
    }

    public static void limpar() {
        ESTADOS.forEach((e, s) -> {
            if (s.congelado && e instanceof Mob m && !s.tinhaNoAi) {
                m.setNoAi(false);
                m.getPersistentData().remove(MARCA);
            }
        });
        ESTADOS.clear();
    }
}
