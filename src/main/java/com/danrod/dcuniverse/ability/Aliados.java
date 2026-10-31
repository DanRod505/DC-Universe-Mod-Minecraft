package com.danrod.dcuniverse.ability;

import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.guia.Guia;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Mobs que viraram aliados temporários de um jogador (o Laço da Verdade, depois de segurar o mob alguns segundos).
 * Enquanto dura, o aliado não mira o dono nem os outros jogadores, segue o dono quando fica longe e ataca os
 * monstros por perto (quem sabe atacar: zumbis batem, esqueletos atiram; uma vaca só acompanha). Um brilho
 * dourado em volta da cabeça mostra quem é aliado. Não vale para chefes nem jogadores.
 */
public final class Aliados {
    private record Aliado(UUID dono, long ate) {}

    private static final Map<Mob, Aliado> ATIVOS = new ConcurrentHashMap<>();

    private Aliados() {}

    public static void registrar() {
        LivingChangeTargetEvent.BUS.addListener((Predicate<LivingChangeTargetEvent>) e -> {
            if (!(e.getEntity() instanceof Mob mob)) return false;
            LivingEntity novo = e.getNewTarget();
            if (novo == null) return false;
            Aliado a = ATIVOS.get(mob);
            // O aliado nunca mira jogadores nem outros aliados do mesmo dono.
            return a != null && (novo instanceof Player || mesmoLado(a.dono(), novo));
        });
    }

    private static boolean mesmoLado(UUID dono, LivingEntity e) {
        Aliado a = e instanceof Mob m ? ATIVOS.get(m) : null;
        return a != null && a.dono().equals(dono);
    }

    /** O mob passa a lutar do lado do dono por "segundos". @return false se não pode (chefe, já é aliado dele). */
    public static boolean tornar(Mob mob, ServerPlayer dono, float segundos) {
        if (Alvos.chefe(mob)) return false;
        Aliado antes = ATIVOS.get(mob);
        long ate = mob.level().getGameTime() + Math.round(segundos * 20);
        ATIVOS.put(mob, new Aliado(dono.getUUID(), ate));
        if (antes != null && antes.dono().equals(dono.getUUID())) return false;
        mob.setTarget(null);
        mob.setPersistenceRequired();
        if (mob.level() instanceof ServerLevel level) {
            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), Sons.ALIADO.get(), SoundSource.PLAYERS, 1f, 1f);
            level.sendParticles(new DustParticleOptions(0xFFD54A, 1.3f), mob.getX(), mob.getY() + mob.getBbHeight() * 0.6, mob.getZ(),
                24, mob.getBbWidth() * 0.5, mob.getBbHeight() * 0.4, mob.getBbWidth() * 0.5, 0);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, mob.getX(), mob.getY() + mob.getBbHeight() + 0.3, mob.getZ(), 6, 0.3, 0.2, 0.3, 0);
        }
        Guia.concluir(dono, "aliado");
        return true;
    }

    public static boolean aliado(Mob mob) {
        return ATIVOS.containsKey(mob);
    }

    /** Quem é o dono do aliado (null se o mob não é aliado de ninguém). */
    public static UUID donoDe(Mob mob) {
        Aliado a = ATIVOS.get(mob);
        return a == null ? null : a.dono();
    }

    public static void tick() {
        ATIVOS.entrySet().removeIf(en -> {
            Mob mob = en.getKey();
            Aliado a = en.getValue();
            if (mob.isRemoved() || !mob.isAlive() || !(mob.level() instanceof ServerLevel level)) return true;
            long agora = level.getGameTime();
            if (agora >= a.ate()) {
                mob.setTarget(null);
                level.sendParticles(ParticleTypes.CLOUD, mob.getX(), mob.getY() + mob.getBbHeight() * 0.7, mob.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
                return true;
            }
            ServerPlayer dono = level.getServer().getPlayerList().getPlayer(a.dono());
            LivingEntity alvo = mob.getTarget();
            if (alvo instanceof Player || alvo != null && mesmoLado(a.dono(), alvo)) mob.setTarget(alvo = null);
            if (agora % 10 == 0) {
                if (alvo == null || !alvo.isAlive()) {
                    Mob inimigo = inimigoPerto(level, mob, a.dono());
                    if (inimigo != null) mob.setTarget(alvo = inimigo);
                }
                if (alvo == null && dono != null && dono.level() == level) {
                    double d = mob.distanceTo(dono);
                    if (d > 24) mob.teleportTo(dono.getX(), dono.getY(), dono.getZ());
                    else if (d > 5) mob.getNavigation().moveTo(dono, 1.25);
                }
                level.sendParticles(new DustParticleOptions(0xFFD54A, 0.7f), mob.getX(), mob.getY() + mob.getBbHeight() + 0.25, mob.getZ(),
                    3, mob.getBbWidth() * 0.35, 0.05, mob.getBbWidth() * 0.35, 0);
            }
            return false;
        });
    }

    private static Mob inimigoPerto(ServerLevel level, Mob mob, UUID dono) {
        Mob melhor = null;
        double perto = 14 * 14;
        for (Mob m : level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(14), m -> m != mob && m instanceof Enemy && m.isAlive())) {
            if (mesmoLado(dono, m)) continue;
            double d = m.distanceToSqr(mob);
            if (d < perto) {
                perto = d;
                melhor = m;
            }
        }
        return melhor;
    }

    public static void limpar() {
        ATIVOS.clear();
    }
}
