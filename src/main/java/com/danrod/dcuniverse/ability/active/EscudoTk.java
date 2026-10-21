package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * tk_shield: escudo telecinético. Uma bolha em volta do jogador por duration segundos (usar de novo desliga antes):
 * projéteis que entram voltam para quem atirou, criaturas hostis que encostam são empurradas para fora e o dano que
 * ainda chega cai em reduction (fração). Parâmetros: duration, shield_radius, reduction e push. A bolha é desenhada
 * no cliente (client/Mundo, efeito "escudo_tk").
 */
public final class EscudoTk implements TipoHabilidade {
    private static final Map<UUID, Ativo> ATIVOS = new ConcurrentHashMap<>();

    private record Ativo(ServerPlayer dono, long ate, double raio, float reducao, double empurra) {}

    @Override
    public boolean gratis(ServerPlayer player, Personagem.Habilidade h) {
        return ATIVOS.containsKey(player.getUUID());
    }

    @Override
    public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        if (ATIVOS.containsKey(player.getUUID())) {
            desligar(player);
            return true;
        }
        Params prm = h.params();
        int dur = Math.round(prm.f("duration", 10) * 20);
        double raio = prm.d("shield_radius", 2.6);
        ATIVOS.put(player.getUUID(), new Ativo(player, level.getGameTime() + dur, raio, prm.f("reduction", 0.75f), prm.d("push", 0.5)));
        Rede.efeito(level, player.getX(), player.getY(), player.getZ(), "escudo_tk", dur, (float) raio, player.getId());
        level.playSound(null, player.getX(), player.getY(), player.getZ(), Sons.ESCUDO_TK_LIGA.get(), SoundSource.PLAYERS, 1.3f, 1f);
        return true;
    }

    public static boolean ativo(Entity e) {
        return e instanceof ServerPlayer sp && ATIVOS.containsKey(sp.getUUID());
    }

    /** Fração do dano que o escudo de quem está com ele ligado corta (0 sem escudo). */
    public static float reducao(Entity e) {
        Ativo a = e instanceof ServerPlayer sp ? ATIVOS.get(sp.getUUID()) : null;
        return a == null ? 0 : a.reducao();
    }

    public static void registrar() {
        LivingHurtEvent.BUS.addListener(e -> {
            if (!(e.getEntity() instanceof ServerPlayer sp)) return;
            Ativo a = ATIVOS.get(sp.getUUID());
            if (a == null) return;
            e.setAmount(e.getAmount() * (1 - a.reducao()));
            impacto(sp, a);
        });
    }

    private static void impacto(ServerPlayer sp, Ativo a) {
        if (!(sp.level() instanceof ServerLevel level)) return;
        level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), Sons.ESCUDO_TK_IMPACTO.get(), SoundSource.PLAYERS, 1f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        Rede.efeito(level, sp.getX(), sp.getY(), sp.getZ(), "escudo_tk_golpe", 0, (float) a.raio(), sp.getId());
    }

    public static void tick() {
        ATIVOS.entrySet().removeIf(en -> {
            Ativo a = en.getValue();
            ServerPlayer p = a.dono();
            if (p.isRemoved() || !(p.level() instanceof ServerLevel level) || level.getGameTime() > a.ate()) {
                fim(p);
                return true;
            }
            Vec3 c = p.position().add(0, p.getBbHeight() / 2, 0);
            double r = a.raio();
            for (Entity e : level.getEntities(p, p.getBoundingBox().inflate(r + 1.5))) {
                Vec3 para = e.position().add(0, e.getBbHeight() / 2, 0).subtract(c);
                double d = para.length();
                if (e instanceof Projectile proj) {
                    if (d > r + 0.3 || proj.getOwner() == p) continue;
                    Vec3 v = proj.getDeltaMovement();
                    // Só o que vem para dentro: devolve na direção de onde veio, para quem atirou.
                    if (v.dot(para) >= 0) continue;
                    Entity atirador = proj.getOwner();
                    Vec3 volta = atirador != null ? atirador.getEyePosition().subtract(proj.position()).normalize().scale(Math.max(0.8, v.length()))
                        : v.scale(-0.8);
                    proj.setDeltaMovement(volta);
                    proj.setOwner(p);
                    proj.hurtMarked = true;
                    level.sendParticles(ParticleTypes.END_ROD, proj.getX(), proj.getY(), proj.getZ(), 6, 0.1, 0.1, 0.1, 0.05);
                    impacto(p, a);
                } else if (e instanceof Mob mob && mob.isAggressive() || e instanceof LivingEntity vivo && vivo instanceof net.minecraft.world.entity.monster.Enemy) {
                    if (d > r || d < 1e-3) continue;
                    Vec3 fora = new Vec3(para.x, 0, para.z).normalize().scale(a.empurra() * (1.2 - d / r));
                    e.setDeltaMovement(e.getDeltaMovement().add(fora.x, 0.12, fora.z));
                    e.hurtMarked = true;
                }
            }
            return false;
        });
    }

    private static void desligar(ServerPlayer p) {
        if (ATIVOS.remove(p.getUUID()) != null) fim(p);
    }

    private static void fim(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level)) return;
        Rede.efeito(level, p.getX(), p.getY(), p.getZ(), "escudo_tk_fim", 0, 0, p.getId());
        level.playSound(null, p.getX(), p.getY(), p.getZ(), Sons.ESCUDO_TK_DESLIGA.get(), SoundSource.PLAYERS, 1.1f, 1f);
    }

    public static void aoDestransformar(ServerPlayer player) {
        desligar(player);
    }

    public static void limpar() {
        ATIVOS.clear();
    }
}
