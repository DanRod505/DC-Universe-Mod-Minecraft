package com.danrod.dcuniverse.combate;

import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.core.Canal;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.guia.Guia;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Combo corpo a corpo (campo "combo" do personagem): o clique esquerdo de mão vazia encadeia golpes animados. O cliente
 * toca a animação na hora e pede o golpe (Rede.Golpe); o servidor confere o ritmo, espera o quadro do impacto e acerta
 * quem estiver no arco à frente, avisando todo mundo por perto (Rede.Impacto) para o hit-stop, o tremor, as partículas
 * e o som. Quem bate prevê o próprio impacto (client/Combate), então não espera a resposta para sentir o golpe.
 * <p>
 * Campos de cada golpe: anim (animação em assets/dcuniverse/animacoes), impact (segundos até o impacto), recover
 * (segundos até poder emendar o próximo), window (segundos em que o próximo clique continua o combo), damage_mult
 * (vezes o dano de ataque do personagem), reach e arc (alcance em blocos e abertura em graus), knockback e launch
 * (empurrão e quanto joga para cima), radius (acima de 0: acerta todos nesse raio em volta do ponto do golpe),
 * stun (segundos lento e fraco) e fx (light, medium ou heavy). No combo: lunge, a distância que o personagem avança
 * até o alvo no começo de cada golpe.
 */
public final class Combo {
    public static final int LEVE = 0, MEDIO = 1, PESADO = 2;

    public record Golpe(String anim, float impacto, float livre, float janela, float dano, float alcance, float arco,
                        float empurrao, float lancar, float raio, float atordoar, int fx) {}

    public record Config(List<Golpe> golpes, float investida) {
        public static Config parse(JsonObject j) {
            List<Golpe> gs = new ArrayList<>();
            for (JsonElement el : j.getAsJsonArray("hits")) {
                JsonObject g = el.getAsJsonObject();
                float impacto = f(g, "impact", 0.12f);
                gs.add(new Golpe(g.get("anim").getAsString(), impacto, Math.max(impacto, f(g, "recover", impacto + 0.08f)),
                    f(g, "window", 0.6f), f(g, "damage_mult", 0.4f), f(g, "reach", 3.4f), f(g, "arc", 80f),
                    f(g, "knockback", 0.4f), f(g, "launch", 0f), f(g, "radius", 0f), f(g, "stun", 0f),
                    switch (g.has("fx") ? g.get("fx").getAsString() : "light") {
                        case "heavy" -> PESADO;
                        case "medium" -> MEDIO;
                        default -> LEVE;
                    }));
            }
            if (gs.isEmpty()) throw new IllegalArgumentException("combo sem golpes");
            return new Config(List.copyOf(gs), f(j, "lunge", 5f));
        }

        private static float f(JsonObject j, String k, float def) {
            return j.has(k) ? j.get(k).getAsFloat() : def;
        }
    }

    /** O combo usa a mão: com arma ou ferramenta na mão principal, o clique é o ataque normal do Minecraft. */
    public static boolean maoLivre(Player p) {
        ItemStack s = p.getMainHandItem();
        return s.isEmpty() || !s.has(DataComponents.WEAPON) && !s.has(DataComponents.TOOL);
    }

    /**
     * Quem o golpe acerta agora (servidor e previsão do cliente fazem a mesma conta): criaturas vivas no arco à frente,
     * dentro do alcance e à vista, sem os moradores e mascotes do mod. Sem raio, só a mais bem mirada; com raio, todas
     * em volta do ponto do golpe.
     */
    public static List<LivingEntity> alvos(Player p, Golpe g) {
        Vec3 olho = p.getEyePosition();
        Vec3 olhar = p.getLookAngle();
        double alcance = g.alcance() * Math.max(1, p.getScale());
        double cosArco = Math.cos(Math.toRadians(g.arco() / 2));
        AABB area = p.getBoundingBox().inflate(alcance + g.raio() + 1);
        List<LivingEntity> out = new ArrayList<>();
        List<LivingEntity> cands = p.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != p && e.isAlive() && !e.isSpectator()
            && !(e instanceof com.danrod.dcuniverse.npc.Residentes.Residente) && !e.isAlliedTo(p) && e.getVehicle() != p && p.getVehicle() != e);
        if (g.raio() > 0) {
            Vec3 ponto = olho.add(olhar.scale(Math.min(alcance, 2.2)));
            for (LivingEntity e : cands) {
                if (e.getBoundingBox().distanceToSqr(ponto) <= g.raio() * g.raio() && (p.hasLineOfSight(e) || e.distanceToSqr(p) < 4)) out.add(e);
            }
            return out;
        }
        LivingEntity melhor = null;
        double melhorNota = Double.MAX_VALUE;
        for (LivingEntity e : cands) {
            AABB bb = e.getBoundingBox();
            Vec3 perto = new Vec3(Mth.clamp(olho.x, bb.minX, bb.maxX), Mth.clamp(olho.y, bb.minY, bb.maxY), Mth.clamp(olho.z, bb.minZ, bb.maxZ));
            double dist = perto.distanceTo(olho);
            if (dist > alcance) continue;
            Vec3 para = bb.getCenter().subtract(olho);
            double cos = para.lengthSqr() < 1e-6 ? 1 : para.normalize().dot(olhar);
            // Colado no alvo, qualquer ângulo da frente vale (o centro dele fica "de lado" para o olho).
            if (cos < cosArco && dist > 0.6) continue;
            if (!p.hasLineOfSight(e) && dist > 0.6) continue;
            double nota = dist * (2 - cos);
            if (nota < melhorNota) {
                melhorNota = nota;
                melhor = e;
            }
        }
        if (melhor != null) out.add(melhor);
        return out;
    }

    /** Alvo da investida: o mais perto à frente, dentro da distância da investida. */
    public static LivingEntity alvoInvestida(Player p, Config c) {
        Vec3 olho = p.getEyePosition();
        Vec3 olhar = p.getLookAngle();
        double dist = c.investida() * Math.max(1, p.getScale());
        return p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(dist), e -> e != p && e.isAlive() && !e.isSpectator()
                && !(e instanceof com.danrod.dcuniverse.npc.Residentes.Residente) && !e.isAlliedTo(p) && p.hasLineOfSight(e))
            .stream()
            .filter(e -> {
                Vec3 para = e.getBoundingBox().getCenter().subtract(olho);
                return para.length() <= dist + e.getBbWidth() && para.normalize().dot(olhar) > 0.82;
            })
            .min(Comparator.comparingDouble(e -> e.distanceToSqr(p)))
            .orElse(null);
    }

    // ------------------------------------------------------------------ servidor

    /** Último golpe de cada jogador: passo e tick do jogo em que começou. */
    private record Ultimo(int passo, long tick) {}

    private record Pendente(ServerPlayer jogador, int passo, long tick) {}

    private static final Map<UUID, Ultimo> ULTIMOS = new ConcurrentHashMap<>();
    private static final List<Pendente> PENDENTES = new ArrayList<>();

    private Combo() {}

    /** Configuração do combo do personagem (null = sem combo, o clique é o ataque normal). */
    public static Config config(Personagem p) {
        return p == null ? null : p.combo();
    }

    /** Pedido de golpe do cliente: confere se pode e agenda o impacto. */
    public static void golpe(ServerPlayer player, int passo) {
        EstadoJogador e = EstadoJogador.de(player);
        Config c = config(e.personagem());
        if (c == null || player.isSpectator() || player.isPassenger() || passo < 0 || passo >= c.golpes().size()) return;
        if (e.destransformarEm() != 0L || !maoLivre(player)) return;
        long agora = player.level().getGameTime();
        Ultimo u = ULTIMOS.get(player.getUUID());
        if (u != null) {
            Golpe ant = c.golpes().get(Math.min(u.passo, c.golpes().size() - 1));
            long desde = agora - u.tick;
            // Folga de 3 ticks para o atraso da rede; mais rápido que isso é clique automático.
            if (desde < Math.round(ant.livre() * 20) - 3) return;
            if (passo > 0 && (passo != u.passo + 1 || desde > Math.round(ant.janela() * 20) + 6)) return;
        } else if (passo > 0) {
            return;
        }
        ULTIMOS.put(player.getUUID(), new Ultimo(passo, agora));
        Golpe g = c.golpes().get(passo);
        Rede.CANAL.send(new Rede.GolpeVisto(player.getId(), g.anim()), Canal.rastreandoEProprio(player));
        synchronized (PENDENTES) {
            PENDENTES.add(new Pendente(player, passo, agora + Math.max(0, Math.round(g.impacto() * 20))));
        }
        if (passo == c.golpes().size() - 1) Guia.concluir(player, "combo");
    }

    /** Tick do servidor: resolve os impactos que chegaram na hora. */
    public static void tick(net.minecraft.server.MinecraftServer server) {
        List<Pendente> agora = new ArrayList<>();
        synchronized (PENDENTES) {
            Iterator<Pendente> it = PENDENTES.iterator();
            while (it.hasNext()) {
                Pendente p = it.next();
                if (p.jogador.isRemoved() || p.jogador.hasDisconnected()) {
                    it.remove();
                } else if (p.jogador.level().getGameTime() >= p.tick) {
                    it.remove();
                    agora.add(p);
                }
            }
        }
        for (Pendente p : agora) {
            Config c = config(EstadoJogador.de(p.jogador).personagem());
            if (c == null || p.passo >= c.golpes().size() || !p.jogador.isAlive()) continue;
            acertar(p.jogador, c.golpes().get(p.passo));
        }
    }

    public static void esquecer(ServerPlayer player) {
        ULTIMOS.remove(player.getUUID());
    }

    private static void acertar(ServerPlayer player, Golpe g) {
        if (!(player.level() instanceof ServerLevel level)) return;
        List<LivingEntity> alvos = alvos(player, g);
        if (alvos.isEmpty()) return;
        float dano = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * g.dano());
        Vec3 olhar = player.getLookAngle();
        boolean noAr = !player.onGround();
        for (LivingEntity alvo : alvos) {
            if (alvo instanceof Player outro && !player.canHarmPlayer(outro)) continue;
            Vec3 ponto = pontoDoImpacto(player, alvo);
            // Cada golpe do combo conta: sem a invencibilidade de meio segundo depois de levar dano.
            alvo.invulnerableTime = 0;
            boolean feriu = alvo.hurtServer(level, player.damageSources().playerAttack(player), dano);
            if (!feriu) continue;
            empurrar(player, alvo, g, olhar, noAr);
            if (g.atordoar() > 0) {
                int t = Math.round(g.atordoar() * 20);
                alvo.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, t, 3, false, false), player);
                alvo.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, t, 1, false, false), player);
            }
            Rede.CANAL.send(new Rede.Impacto(player.getId(), alvo.getId(), g.fx(), ponto.x, ponto.y, ponto.z,
                (float) olhar.x, (float) olhar.y, (float) olhar.z), Canal.perto(level, ponto.x, ponto.y, ponto.z, 64));
        }
        player.causeFoodExhaustion(0.1f);
    }

    /** Onde o punho encontra o alvo: o ponto da caixa dele mais perto da linha do olhar, na altura do peito. */
    public static Vec3 pontoDoImpacto(Player p, LivingEntity alvo) {
        AABB bb = alvo.getBoundingBox();
        Vec3 olho = p.getEyePosition();
        Vec3 centro = bb.getCenter();
        double d = Math.max(0, centro.subtract(olho).dot(p.getLookAngle()) - alvo.getBbWidth() * 0.5);
        Vec3 naLinha = olho.add(p.getLookAngle().scale(d));
        return new Vec3(Mth.clamp(naLinha.x, bb.minX, bb.maxX), Mth.clamp(naLinha.y, bb.minY + bb.getYsize() * 0.35, bb.maxY - 0.1),
            Mth.clamp(naLinha.z, bb.minZ, bb.maxZ));
    }

    private static void empurrar(ServerPlayer player, LivingEntity alvo, Golpe g, Vec3 olhar, boolean noAr) {
        double resiste = 1 - Mth.clamp(alvo.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1) * 0.7;
        if (resiste <= 0) return;
        Vec3 dir = alvo.position().subtract(player.position());
        Vec3 plano = new Vec3(dir.x, 0, dir.z);
        plano = plano.lengthSqr() < 1e-4 ? new Vec3(olhar.x, 0, olhar.z).normalize() : plano.normalize();
        Vec3 v;
        if (noAr && g.fx() == PESADO) {
            // No ar o golpe final manda o alvo para onde o herói olha (até para baixo, contra o chão).
            v = olhar.scale(g.empurrao() * 1.2);
        } else {
            v = plano.scale(g.empurrao()).add(0, g.lancar() + (alvo.onGround() ? 0.12 : 0.04), 0);
        }
        Vec3 atual = alvo.getDeltaMovement();
        alvo.setDeltaMovement(atual.scale(0.3).add(v.scale(resiste)));
        alvo.hurtMarked = true;
    }
}
