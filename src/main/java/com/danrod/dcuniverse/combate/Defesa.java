package com.danrod.dcuniverse.combate;

import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.core.Canal;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.guia.Guia;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Defesa do modo combate (botão direito segurado): o herói cruza os braços na frente do rosto. O que vem pela frente
 * (golpe, flecha, explosão) perde boa parte do dano; quem defende anda devagar e não golpeia. Nos primeiros instantes
 * da guarda o golpe é aparado: não causa dano nenhum, o atacante é jogado para trás e fica atordoado, e quem aparou
 * ganha um momento de câmera (clarão, hit-stop e o sino do aparo para todo mundo por perto).
 * <p>
 * Campos no "guard" do combo: anim (guarda em laço), parry_anim, parry (segundos de aparo no começo da guarda),
 * reduction (fração do dano cortada fora do aparo), parry_stun (segundos), parry_knockback e arc (graus à frente que
 * a guarda cobre).
 */
public final class Defesa {
    public record Config(String anim, String animAparo, float aparo, float reducao, float atordoar, float empurrao, float arco) {
        static Config parse(com.google.gson.JsonObject j) {
            return new Config(j.get("anim").getAsString(), j.has("parry_anim") ? j.get("parry_anim").getAsString() : "",
                f(j, "parry", 0.3f), f(j, "reduction", 0.75f), f(j, "parry_stun", 1.5f), f(j, "parry_knockback", 1.4f), f(j, "arc", 200f));
        }

        private static float f(com.google.gson.JsonObject j, String k, float def) {
            return j.has(k) ? j.get(k).getAsFloat() : def;
        }
    }

    /** Quem está defendendo: desde quando (tick do mundo) e quando a animação volta para a guarda depois de um aparo. */
    private record Estado(ServerPlayer jogador, long desde, long voltaEm) {}

    private static final Map<UUID, Estado> DEFENDENDO = new ConcurrentHashMap<>();
    private static final Identifier LENTO = Identifier.fromNamespaceAndPath("dcuniverse", "defesa_lento");
    /** Quanto a defesa freia o andar (multiplica a velocidade toda). */
    private static final double FREIO = -0.55;

    private Defesa() {}

    public static boolean defendendo(ServerPlayer p) {
        return DEFENDENDO.containsKey(p.getUUID());
    }

    /** Pedido do cliente (Rede.Defender): levantou ou baixou a guarda. */
    public static void defender(ServerPlayer p, boolean ligar) {
        Combo.Config c = Combo.config(EstadoJogador.de(p).personagem());
        Config d = c == null ? null : c.defesa();
        if (ligar && (d == null || !Combo.emModo(p) || p.isSpectator() || p.isPassenger() || EstadoJogador.de(p).destransformarEm() != 0L)) ligar = false;
        boolean estava = DEFENDENDO.containsKey(p.getUUID());
        if (ligar == estava) return;
        if (ligar) {
            DEFENDENDO.put(p.getUUID(), new Estado(p, p.level().getGameTime(), 0));
            Combo.cancelarCarga(p);
            freio(p, true);
            Rede.CANAL.send(new Rede.GolpeVisto(p.getId(), d.anim()), Canal.rastreando(p));
            Guia.concluir(p, "defesa");
        } else {
            baixar(p);
        }
    }

    private static void baixar(ServerPlayer p) {
        if (DEFENDENDO.remove(p.getUUID()) == null) return;
        freio(p, false);
        if (p.connection != null) Rede.CANAL.send(new Rede.GolpeVisto(p.getId(), ""), Canal.rastreando(p));
    }

    private static void freio(ServerPlayer p, boolean ligar) {
        AttributeInstance a = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (a == null) return;
        a.removeModifier(LENTO);
        if (ligar) a.addTransientModifier(new AttributeModifier(LENTO, FREIO, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    public static void registrar() {
        NeoForge.EVENT_BUS.addListener(LivingIncomingDamageEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer sp && DEFENDENDO.containsKey(sp.getUUID())) {
                float resta = receber(sp, e.getSource(), e.getAmount());
                if (resta <= 0) e.setCanceled(true);
                else e.setAmount(resta);
            }
        });
    }

    /**
     * O golpe que chega em quem está defendendo: quanto dano passa (0 = aparado ou bloqueado por inteiro). Público
     * para os testes (o jogador de teste não leva dano de verdade).
     */
    public static float receber(ServerPlayer p, DamageSource fonte, float dano) {
        Estado st = DEFENDENDO.get(p.getUUID());
        Combo.Config c = Combo.config(EstadoJogador.de(p).personagem());
        if (st == null || c == null || c.defesa() == null || !(p.level() instanceof ServerLevel level)) return dano;
        if (fonte.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR) && !fonte.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) return dano;
        Vec3 de = fonte.getSourcePosition();
        if (de == null) return dano;
        Config d = c.defesa();
        Vec3 para = de.subtract(p.position());
        Vec3 olhar = p.getLookAngle();
        Vec3 plano = new Vec3(para.x, 0, para.z);
        Vec3 olharPlano = new Vec3(olhar.x, 0, olhar.z);
        if (plano.lengthSqr() > 1e-4 && olharPlano.lengthSqr() > 1e-4) {
            double cos = plano.normalize().dot(olharPlano.normalize());
            if (cos < Math.cos(Math.toRadians(d.arco() / 2))) return dano;
        }
        long agora = level.getGameTime();
        Entity atacante = fonte.getEntity();
        Entity direto = fonte.getDirectEntity();
        Vec3 ponto = p.getEyePosition().add(olhar.scale(0.6)).add(0, -0.3, 0);
        if (agora - st.desde() <= Math.round(d.aparo() * 20)) {
            // Aparo: nada passa; quem bateu de perto volta tonto, o que foi atirado volta para quem atirou.
            if (direto instanceof Projectile proj) {
                Entity dono = proj.getOwner();
                Vec3 volta = dono != null ? dono.getEyePosition().subtract(proj.position()).normalize().scale(Math.max(1.0, proj.getDeltaMovement().length()))
                    : proj.getDeltaMovement().scale(-1);
                proj.setDeltaMovement(volta);
                proj.setOwner(p);
                proj.hurtMarked = true;
            }
            if (atacante instanceof LivingEntity vivo && vivo != p && vivo.distanceToSqr(p) < 8 * 8) {
                Vec3 fora = vivo.position().subtract(p.position()).multiply(1, 0, 1);
                fora = fora.lengthSqr() < 1e-4 ? olharPlano.normalize() : fora.normalize();
                vivo.setDeltaMovement(vivo.getDeltaMovement().scale(0.2).add(fora.scale(d.empurrao())).add(0, 0.35, 0));
                vivo.hurtMarked = true;
                int t = Math.round(d.atordoar() * 20);
                vivo.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, t, 4, false, false), p);
                vivo.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, t, 2, false, false), p);
                if (vivo instanceof net.minecraft.world.entity.Mob mob) mob.getNavigation().stop();
            }
            level.playSound(null, p.getX(), p.getEyeY(), p.getZ(), Sons.APARO.get(), SoundSource.PLAYERS, 1.4f, 0.95f + level.getRandom().nextFloat() * 0.1f);
            int alvo = atacante instanceof LivingEntity vivo ? vivo.getId() : -1;
            Rede.CANAL.send(new Rede.Impacto(p.getId(), alvo, Combo.APARO, ponto.x, ponto.y, ponto.z, (float) olhar.x, (float) olhar.y, (float) olhar.z),
                Canal.perto(level, ponto.x, ponto.y, ponto.z, 64));
            if (!d.animAparo().isEmpty()) {
                Rede.CANAL.send(new Rede.GolpeVisto(p.getId(), d.animAparo(), true), Canal.rastreandoEProprio(p));
                DEFENDENDO.put(p.getUUID(), new Estado(p, st.desde(), agora + 9));
            }
            Guia.concluir(p, "aparo");
            return 0;
        }
        level.playSound(null, p.getX(), p.getEyeY(), p.getZ(), Sons.DEFESA.get(), SoundSource.PLAYERS, 1.0f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        Rede.CANAL.send(new Rede.Impacto(p.getId(), -1, Combo.BLOQUEIO, ponto.x, ponto.y, ponto.z, (float) -olhar.x, (float) -olhar.y, (float) -olhar.z),
            Canal.perto(level, ponto.x, ponto.y, ponto.z, 48));
        // Um empurrãozinho para trás: o golpe pegou, só não machucou.
        Vec3 tras = olharPlano.lengthSqr() < 1e-4 ? Vec3.ZERO : olharPlano.normalize().scale(-0.25);
        p.setDeltaMovement(p.getDeltaMovement().add(tras));
        p.hurtMarked = true;
        return dano * (1 - d.reducao());
    }

    public static void tick() {
        for (var it = DEFENDENDO.values().iterator(); it.hasNext(); ) {
            Estado st = it.next();
            ServerPlayer p = st.jogador();
            boolean fora = p.isRemoved() || p.hasDisconnected();
            Combo.Config c = fora ? null : Combo.config(EstadoJogador.de(p).personagem());
            if (fora || c == null || c.defesa() == null || !Combo.emModo(p) || p.isSpectator() || !p.isAlive()) {
                it.remove();
                if (!fora) {
                    freio(p, false);
                    Rede.CANAL.send(new Rede.GolpeVisto(p.getId(), ""), Canal.rastreando(p));
                }
                continue;
            }
            if (st.voltaEm() > 0 && p.level().getGameTime() >= st.voltaEm()) {
                DEFENDENDO.put(p.getUUID(), new Estado(p, st.desde(), 0));
                Rede.CANAL.send(new Rede.GolpeVisto(p.getId(), c.defesa().anim(), true), Canal.rastreandoEProprio(p));
            }
        }
    }

    public static void esquecer(ServerPlayer p) {
        DEFENDENDO.remove(p.getUUID());
    }

    public static void limpar() {
        DEFENDENDO.clear();
    }
}
