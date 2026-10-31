package com.danrod.dcuniverse.ability.passive;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.ability.Passiva;
import com.danrod.dcuniverse.ability.Passivas;
import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.energy.Energia;
import com.danrod.dcuniverse.guia.Guia;
import com.danrod.dcuniverse.mixin.AbstractArrowAccessor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * deflect (Bloqueio e reflexo de projéteis): segurar o clique direito com a mão vazia ergue os braceletes (o cliente
 * manda Rede.Bloqueio; client/Bloqueador). Enquanto ergue, tudo que vem da frente (cone de angle graus) é bloqueado:
 * - projéteis (flechas, bolas de fogo, tridentes, espinhos de shulker, cuspe de lhama, os do mod...) ricocheteiam
 *   nos braceletes (Entity.deflection, via EntityDeflectionMixin) e não ferem;
 * - no bloqueio perfeito (o projétil chega até perfect_window s depois de erguer), o projétil volta direto em quem
 *   atirou, mais rápido e com reflect_mult vezes o dano, e passa a ser seu (a morte conta para você);
 * - golpes corpo a corpo de frente são anulados e o atacante é empurrado; no tempo perfeito ele fica atordoado;
 * - explosões de frente perdem explosion_reduction do dano.
 * Cada coração bloqueado gasta energy_per_heart de energia (no criativo, nada). Sem energia a guarda quebra: os braços
 * caem e só sobem de novo depois de stagger segundos. Erguido, anda a (1 - slow) da velocidade.
 * Serve para qualquer personagem que tenha a passiva (Moça-Maravilha depois) e para o escudo do lote.
 */
public final class Bloqueio implements Passiva {
    public static final int SOLTO = 0, ERGUIDO = 1, QUEBRADO = 2;
    private static final Identifier LENTO = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "bloqueio_lento");

    private static final class Estado {
        long inicio;
        long quebradoAte = Long.MIN_VALUE;
        boolean erguido;
    }

    private static final Map<UUID, Estado> ESTADOS = new ConcurrentHashMap<>();
    /** Projéteis refletidos esperando o novo dono (o Projectile.deflect volta o dono antigo depois do desvio). */
    private static final Map<Projectile, ServerPlayer> NOVO_DONO = new WeakHashMap<>();

    public static void registrar() {
        LivingAttackEvent.BUS.addListener((Predicate<LivingAttackEvent>) e -> e.getEntity() instanceof ServerPlayer sp
            && corpoACorpo(sp, e.getSource(), e.getAmount()));
        LivingHurtEvent.BUS.addListener(e -> {
            if (e.getEntity() instanceof ServerPlayer sp && e.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
                e.setAmount(explosao(sp, e.getSource(), e.getAmount()));
            }
        });
    }

    @Override
    public void tick(ServerPlayer player, Params p) {
        Estado e = ESTADOS.get(player.getUUID());
        if (e == null) return;
        long t = player.level().getGameTime();
        // A guarda quebrada volta sozinha; o cliente só ergue de novo se o botão ainda estiver segurado.
        if (!e.erguido && e.quebradoAte != Long.MIN_VALUE && t >= e.quebradoAte) {
            e.quebradoAte = Long.MIN_VALUE;
            mostrar(player, SOLTO);
        }
        if (e.erguido && player.tickCount % 4 == 0 && player.level() instanceof ServerLevel level) {
            // Brilho leve nos braceletes enquanto erguidos (os outros também veem).
            Vec3 m = maos(player);
            level.sendParticles(ParticleTypes.WAX_OFF, m.x, m.y, m.z, 1, 0.15, 0.1, 0.15, 0);
        }
    }

    @Override
    public void aoDestransformar(ServerPlayer player, Params p) {
        soltar(player);
        ESTADOS.remove(player.getUUID());
    }

    /** O cliente apertou ou soltou o bloqueio. */
    public static void definir(ServerPlayer player, boolean erguer) {
        Params prm = Passivas.params(player, "deflect");
        Estado e = ESTADOS.computeIfAbsent(player.getUUID(), k -> new Estado());
        long t = player.level().getGameTime();
        if (!erguer || prm == null || player.isSpectator()) {
            soltar(player);
            return;
        }
        if (e.erguido || t < e.quebradoAte) return;
        e.erguido = true;
        e.inicio = t;
        var vel = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (vel != null) {
            vel.addOrUpdateTransientModifier(new AttributeModifier(LENTO, -Math.clamp(prm.d("slow", 0.5), 0, 0.9),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), Sons.BRACELETES_ERGUE.get(), SoundSource.PLAYERS,
            0.5f, 0.95f + player.getRandom().nextFloat() * 0.1f);
        mostrar(player, ERGUIDO);
    }

    private static void soltar(ServerPlayer player) {
        Estado e = ESTADOS.get(player.getUUID());
        var vel = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (vel != null) vel.removeModifier(LENTO);
        if (e == null || !e.erguido) return;
        e.erguido = false;
        mostrar(player, player.level().getGameTime() < e.quebradoAte ? QUEBRADO : SOLTO);
    }

    public static boolean erguido(Player player) {
        Estado e = ESTADOS.get(player.getUUID());
        return e != null && e.erguido;
    }

    /** Erguido há pouco o bastante para o bloqueio perfeito? */
    public static boolean perfeito(ServerPlayer player, Params prm) {
        Estado e = ESTADOS.get(player.getUUID());
        return e != null && e.erguido && player.level().getGameTime() - e.inicio <= Math.round(prm.d("perfect_window", 0.3) * 20);
    }

    private static void mostrar(ServerPlayer player, int estado) {
        Rede.bloqueio(player, estado);
    }

    /** O golpe (ou projétil) que vem de "de" está dentro do cone da frente? */
    private static boolean deFrente(ServerPlayer player, Vec3 de, Params prm) {
        Vec3 para = de.subtract(player.getEyePosition());
        if (para.lengthSqr() < 1e-4) return true;
        double cos = Math.cos(Math.toRadians(Math.clamp(prm.d("angle", 140), 10, 359) / 2));
        return player.getLookAngle().dot(para.normalize()) >= cos;
    }

    /** Ponto entre os braceletes, um pouco à frente do rosto. */
    private static Vec3 maos(Player player) {
        return player.getEyePosition().add(player.getLookAngle().scale(0.55)).add(0, -0.25, 0);
    }

    /**
     * Gasta a energia do bloqueio. Sem energia, a guarda quebra (os braços caem por stagger segundos) e o golpe passa.
     * @return true se bloqueou
     */
    private static boolean pagar(ServerPlayer player, Params prm, float coracoes) {
        if (player.isCreative()) return true;
        Personagem p = EstadoJogador.de(player).personagem();
        if (p == null) return false;
        if (Energia.gastar(player, p, (float) (prm.d("energy_per_heart", 1.5) * Math.max(0.5f, coracoes)))) return true;
        Estado e = ESTADOS.get(player.getUUID());
        if (e != null) e.quebradoAte = player.level().getGameTime() + Math.round(prm.d("stagger", 1.5) * 20);
        soltar(player);
        Vec3 m = maos(player);
        player.level().playSound(null, m.x, m.y, m.z, Sons.BLOQUEIO_QUEBRA.get(), SoundSource.PLAYERS, 1f, 1f);
        if (player.level() instanceof ServerLevel level) level.sendParticles(ParticleTypes.SMOKE, m.x, m.y, m.z, 10, 0.2, 0.2, 0.2, 0.02);
        return false;
    }

    /**
     * Chamado pelo EntityDeflectionMixin quando um projétil acerta o jogador (Projectile.hitTargetOrDeflectSelf).
     * @return o desvio a aplicar, ou null para o projétil acertar normalmente
     */
    public static ProjectileDeflection desvio(Entity quem, Projectile proj) {
        if (!(quem instanceof ServerPlayer player) || !erguido(player) || !(player.level() instanceof ServerLevel level)) return null;
        Params prm = Passivas.params(player, "deflect");
        if (prm == null) return null;
        Vec3 v = proj.getDeltaMovement();
        Vec3 de = v.lengthSqr() < 1e-6 ? proj.position() : player.getEyePosition().subtract(v.normalize().scale(4));
        if (!deFrente(player, de, prm)) return null;
        double base = proj instanceof AbstractArrow a ? ((AbstractArrowAccessor) a).dcuniverse$dano() * v.length() / 2 : 3;
        if (!pagar(player, prm, (float) Math.min(10, base))) return null;
        boolean perfeito = perfeito(player, prm);
        Entity atirador = proj.getOwner();
        return (p, ent, rnd) -> {
            Vec3 vel = p.getDeltaMovement();
            double rapidez = Math.max(0.8, vel.length());
            Vec3 dir;
            if (perfeito) {
                // Volta direto em quem atirou (no peito), mais rápido e mais forte.
                if (atirador instanceof LivingEntity vivo && vivo.isAlive() && vivo.level() == p.level() && vivo.distanceToSqr(p) < 64 * 64) {
                    Vec3 alvo = vivo.getBoundingBox().getCenter();
                    double dist = alvo.distanceTo(p.position());
                    // Flechas caem: mira um pouco acima, como faria um arqueiro.
                    if (p instanceof AbstractArrow) alvo = alvo.add(0, dist * dist * 0.0012, 0);
                    dir = alvo.subtract(p.position()).normalize();
                } else {
                    dir = vel.lengthSqr() < 1e-6 ? player.getLookAngle() : vel.normalize().scale(-1);
                }
                rapidez = Math.max(1.6, rapidez * 1.25);
                if (p instanceof AbstractArrow a) {
                    a.setBaseDamage(((AbstractArrowAccessor) a).dcuniverse$dano() * prm.d("reflect_mult", 2.0));
                    a.setCritArrow(true);
                }
                synchronized (NOVO_DONO) {
                    NOVO_DONO.put(p, player);
                }
            } else {
                // Ricocheteia no bracelete: espelha no plano do olhar e espalha para o lado e para cima.
                Vec3 n = player.getLookAngle();
                Vec3 d = vel.lengthSqr() < 1e-6 ? n.scale(-1) : vel.normalize();
                dir = d.subtract(n.scale(2 * d.dot(n)));
                dir = dir.add((rnd.nextDouble() - 0.5) * 0.8, 0.25 + rnd.nextDouble() * 0.35, (rnd.nextDouble() - 0.5) * 0.8).normalize();
                rapidez *= 0.55;
                synchronized (NOVO_DONO) {
                    NOVO_DONO.put(p, player);
                }
            }
            p.setDeltaMovement(dir.scale(rapidez));
            double h = dir.horizontalDistance();
            p.setYRot((float) (Math.atan2(dir.x, dir.z) * (180 / Math.PI)));
            p.setXRot((float) (Math.atan2(dir.y, h) * (180 / Math.PI)));
            p.yRotO = p.getYRot();
            p.xRotO = p.getXRot();
            p.needsSync = true;
            efeito(player, level, p.position(), perfeito, false);
        };
    }

    /** Chamado no fim do Projectile.deflect: aplica o novo dono do projétil desviado. */
    public static void aposDesvio(Projectile p) {
        ServerPlayer dono;
        synchronized (NOVO_DONO) {
            dono = NOVO_DONO.remove(p);
        }
        if (dono == null) return;
        p.setOwner(dono);
        // O desvio comum corta a aceleração das bolas de fogo pela metade; refletida ela volta com tudo.
        if (p instanceof AbstractHurtingProjectile bola) bola.accelerationPower = 0.1;
    }

    /** Golpe corpo a corpo de frente: anula e empurra quem bateu (no tempo perfeito, atordoa). */
    public static boolean corpoACorpo(ServerPlayer player, DamageSource fonte, float dano) {
        if (!erguido(player) || fonte.is(DamageTypeTags.IS_EXPLOSION) || fonte.is(DamageTypeTags.BYPASSES_SHIELD)) return false;
        if (!(fonte.getEntity() instanceof LivingEntity atacante) || fonte.getDirectEntity() != atacante) return false;
        if (!(player.level() instanceof ServerLevel level) || atacante.distanceToSqr(player) > 36) return false;
        Params prm = Passivas.params(player, "deflect");
        if (prm == null || !deFrente(player, atacante.getEyePosition(), prm)) return false;
        if (!pagar(player, prm, dano / 2)) return false;
        boolean perfeito = perfeito(player, prm);
        Vec3 fora = atacante.position().subtract(player.position()).multiply(1, 0, 1);
        fora = fora.lengthSqr() < 1e-4 ? player.getLookAngle() : fora.normalize();
        atacante.knockback(perfeito ? 1.4 : 0.6, -fora.x, -fora.z, player.damageSources().playerAttack(player), 0);
        if (perfeito) {
            atacante.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 4), player);
            atacante.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 1), player);
            if (atacante instanceof net.minecraft.world.entity.Mob mob) mob.getNavigation().stop();
        }
        efeito(player, level, atacante.getEyePosition().add(player.getEyePosition()).scale(0.5), perfeito, true);
        return true;
    }

    /** Explosão de frente: os braceletes seguram explosion_reduction do dano. */
    private static float explosao(ServerPlayer player, DamageSource fonte, float dano) {
        if (!erguido(player) || !(player.level() instanceof ServerLevel level)) return dano;
        Params prm = Passivas.params(player, "deflect");
        Vec3 de = fonte.getSourcePosition();
        if (prm == null || de == null || !deFrente(player, de, prm)) return dano;
        float corta = (float) Math.clamp(prm.d("explosion_reduction", 0.8), 0, 1);
        if (!pagar(player, prm, dano * corta / 2)) return dano;
        efeito(player, level, maos(player), false, true);
        return dano * (1 - corta);
    }

    private static void efeito(ServerPlayer player, ServerLevel level, Vec3 onde, boolean perfeito, boolean golpe) {
        Vec3 m = maos(player).lerp(onde, golpe ? 0.3 : 0.15);
        // As faíscas saem um pouco mais à frente, para não taparem a visão de quem bloqueia.
        Vec3 f = m.add(player.getLookAngle().scale(0.6));
        level.sendParticles(ParticleTypes.CRIT, f.x, f.y, f.z, perfeito ? 14 : 6, 0.15, 0.15, 0.15, 0.35);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, f.x, f.y, f.z, perfeito ? 8 : 3, 0.1, 0.1, 0.1, 0.2);
        level.playSound(null, m.x, m.y, m.z, perfeito ? Sons.BLOQUEIO_PERFEITO.get() : Sons.BLOQUEIO.get(), SoundSource.PLAYERS,
            perfeito ? 1.2f : 0.9f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        Rede.efeito(level, m.x, m.y, m.z, "deflect", perfeito ? 1 : 0, 0, perfeito ? 0xFFE7A0 : 0xE8F2FF);
        Guia.concluir(player, "bloqueio");
        if (perfeito) Guia.concluir(player, "bloqueio_perfeito");
    }
}
