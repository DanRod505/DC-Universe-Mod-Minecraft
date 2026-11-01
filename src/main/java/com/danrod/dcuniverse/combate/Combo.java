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
    /** Força do golpe (hit-stop, tremor, partículas, som); ONDA é o pesado que racha o chão em volta. */
    public static final int LEVE = 0, MEDIO = 1, PESADO = 2, ONDA = 3;
    /** Passo do golpe carregado (botão direito no modo combate) nos pacotes e nas contas do combo. */
    public static final int PASSO_PESADO = 99;

    public record Golpe(String anim, float impacto, float livre, float janela, float dano, float alcance, float arco,
                        float empurrao, float lancar, float raio, float atordoar, int fx) {
        public boolean pesado() {
            return fx >= PESADO;
        }

        static Golpe parse(JsonObject g) {
            float impacto = f(g, "impact", 0.12f);
            return new Golpe(g.get("anim").getAsString(), impacto, Math.max(impacto, f(g, "recover", impacto + 0.08f)),
                f(g, "window", 0.6f), f(g, "damage_mult", 0.4f), f(g, "reach", 3.4f), f(g, "arc", 80f),
                f(g, "knockback", 0.4f), f(g, "launch", 0f), f(g, "radius", 0f), f(g, "stun", 0f),
                switch (g.has("fx") ? g.get("fx").getAsString() : "light") {
                    case "shockwave" -> ONDA;
                    case "heavy" -> PESADO;
                    case "medium" -> MEDIO;
                    default -> LEVE;
                });
        }
    }

    /**
     * Golpe carregado: segurar o botão direito no modo combate toca charge_anim (em laço) por até charge segundos; ao
     * soltar sai o golpe (os campos de um golpe comum), com dano e empurrão de min_power (soltou na hora) a 1 (carga
     * cheia).
     */
    public record Carregado(String animCarga, float carga, float minimo, Golpe golpe) {}

    /** stance: a guarda (animação em laço) do modo combate. heavy: o golpe carregado (pode faltar). */
    public record Config(List<Golpe> golpes, float investida, String postura, Carregado pesado) {
        public static Config parse(JsonObject j) {
            List<Golpe> gs = new ArrayList<>();
            for (JsonElement el : j.getAsJsonArray("hits")) gs.add(Golpe.parse(el.getAsJsonObject()));
            if (gs.isEmpty()) throw new IllegalArgumentException("combo sem golpes");
            Carregado pesado = null;
            if (j.has("heavy")) {
                JsonObject h = j.getAsJsonObject("heavy");
                pesado = new Carregado(h.get("charge_anim").getAsString(), f(h, "charge", 1f), f(h, "min_power", 0.4f), Golpe.parse(h));
            }
            return new Config(List.copyOf(gs), f(j, "lunge", 5f), j.has("stance") ? j.get("stance").getAsString() : "", pesado);
        }

        /** O golpe do passo (PASSO_PESADO = o carregado). */
        public Golpe golpe(int passo) {
            if (passo == PASSO_PESADO) return pesado == null ? null : pesado.golpe();
            return passo >= 0 && passo < golpes.size() ? golpes.get(passo) : null;
        }

    }

    private static float f(JsonObject j, String k, float def) {
        return j.has(k) ? j.get(k).getAsFloat() : def;
    }

    /**
     * O combo usa a mão: com arma ou ferramenta na mão principal, o clique é o ataque normal do Minecraft. No modo
     * combate a mão é sempre do combo.
     */
    public static boolean maoLivre(Player p) {
        ItemStack s = p.getMainHandItem();
        return s.isEmpty() || !s.has(DataComponents.WEAPON) && !s.has(DataComponents.TOOL) || emModo(p);
    }

    // ------------------------------------------------------------------ modo combate

    /** Quem está no modo combate (servidor). No cliente vale EstadoCliente.MODO_COMBATE (ids das entidades). */
    private static final Map<UUID, ServerPlayer> MODO = new ConcurrentHashMap<>();

    /**
     * Modo combate: os botões do mouse são só do combate (esquerdo combo, direito golpe carregado); nada de quebrar,
     * construir ou usar itens. A tecla liga e desliga; destransformar desliga.
     */
    public static boolean emModo(Player p) {
        if (p.level().isClientSide()) return com.danrod.dcuniverse.core.EstadoCliente.MODO_COMBATE.contains(p.getId());
        return MODO.containsKey(p.getUUID());
    }

    public static void modo(ServerPlayer p, boolean ligar) {
        Config c = config(EstadoJogador.de(p).personagem());
        if (c == null || p.isSpectator() || EstadoJogador.de(p).destransformarEm() != 0L) ligar = false;
        boolean estava = MODO.containsKey(p.getUUID());
        if (ligar) MODO.put(p.getUUID(), p);
        else MODO.remove(p.getUUID());
        if (!ligar) CARREGANDO.remove(p.getUUID());
        // Sempre responde (mesmo sem mudar): o cliente que previu errado volta ao estado certo.
        Rede.CANAL.send(new Rede.ModoVisto(p.getId(), ligar ? c.postura() : ""), Canal.rastreandoEProprio(p));
        if (ligar && !estava) Guia.concluir(p, "modo_combate");
    }

    /** Modo combate: o mouse não quebra, não constrói e não usa nada (nos dois lados, para a previsão do cliente bater). */
    public static void registrar() {
        var bus = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener(net.neoforged.neoforge.event.level.block.BreakBlockEvent.class,
            com.danrod.dcuniverse.core.Eventos.cancela(e -> {
                if (!emModo(e.getPlayer())) return false;
                if (e.getPlayer() instanceof ServerPlayer sp && sp.connection != null) e.setNotifyClient(true);
                return true;
            }));
        bus.addListener(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.class,
            com.danrod.dcuniverse.core.Eventos.cancela(e -> emModo(e.getEntity())));
        bus.addListener(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock.class,
            com.danrod.dcuniverse.core.Eventos.cancela(e -> emModo(e.getEntity())));
        bus.addListener(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem.class,
            com.danrod.dcuniverse.core.Eventos.cancela(e -> emModo(e.getEntity())));
        bus.addListener(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract.class,
            com.danrod.dcuniverse.core.Eventos.cancela(e -> emModo(e.getEntity())));
    }

    /** Quem começa a ver o jogador fica sabendo da guarda dele. */
    public static void modoPara(ServerPlayer quem, ServerPlayer alvo) {
        if (!MODO.containsKey(alvo.getUUID())) return;
        Config c = config(EstadoJogador.de(alvo).personagem());
        if (c != null) Rede.CANAL.send(new Rede.ModoVisto(alvo.getId(), c.postura()), Canal.jogador(quem));
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

    /** Último golpe de cada jogador: passo, o golpe e o tick do jogo em que começou. */
    private record Ultimo(int passo, Golpe golpe, long tick) {}

    /** Impacto agendado: força 0 a 1 multiplica dano e empurrão (golpe carregado). */
    private record Pendente(ServerPlayer jogador, Golpe golpe, float forca, long tick) {}

    private static final Map<UUID, Ultimo> ULTIMOS = new ConcurrentHashMap<>();
    /** Tick em que cada jogador começou a carregar o golpe pesado. */
    private static final Map<UUID, Long> CARREGANDO = new ConcurrentHashMap<>();
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
            Golpe ant = u.golpe;
            long desde = agora - u.tick;
            // Folga de 3 ticks para o atraso da rede (mas nunca dois golpes a menos de 2 ticks): mais rápido que isso é
            // clique automático.
            if (desde < Math.max(2, Math.round(ant.livre() * 20) - 3)) return;
            if (passo > 0 && (passo != u.passo + 1 || desde > Math.round(ant.janela() * 20) + 6)) return;
        } else if (passo > 0) {
            return;
        }
        Golpe g = c.golpes().get(passo);
        CARREGANDO.remove(player.getUUID());
        agendar(player, passo, g, 1, agora);
        if (passo == c.golpes().size() - 1) Guia.concluir(player, "combo");
    }

    private static void agendar(ServerPlayer player, int passo, Golpe g, float forca, long agora) {
        ULTIMOS.put(player.getUUID(), new Ultimo(passo, g, agora));
        Rede.CANAL.send(new Rede.GolpeVisto(player.getId(), g.anim()), Canal.rastreandoEProprio(player));
        synchronized (PENDENTES) {
            PENDENTES.add(new Pendente(player, g, forca, agora + Math.max(0, Math.round(g.impacto() * 20))));
        }
    }

    /** Golpe carregado (só no modo combate): começar toca a carga para quem vê; soltar agenda o golpe. */
    public static void carregar(ServerPlayer player, boolean comecar) {
        EstadoJogador e = EstadoJogador.de(player);
        Config c = config(e.personagem());
        if (c == null || c.pesado() == null || !emModo(player) || player.isSpectator() || player.isPassenger() || e.destransformarEm() != 0L) {
            CARREGANDO.remove(player.getUUID());
            return;
        }
        long agora = player.level().getGameTime();
        if (comecar) {
            Ultimo u = ULTIMOS.get(player.getUUID());
            if (u != null && agora - u.tick < Math.round(u.golpe.livre() * 20) - 3) return;
            CARREGANDO.put(player.getUUID(), agora);
            Rede.CANAL.send(new Rede.GolpeVisto(player.getId(), c.pesado().animCarga()), Canal.rastreandoEProprio(player));
            return;
        }
        Long desde = CARREGANDO.remove(player.getUUID());
        if (desde == null) return;
        Carregado k = c.pesado();
        float forca = forca(k, (agora - desde) / 20f);
        agendar(player, PASSO_PESADO, k.golpe(), forca, agora);
        if (forca >= 0.99f) Guia.concluir(player, "golpe_carregado");
    }

    /** Força do golpe carregado depois de segundos carregando (a mesma conta no cliente). */
    public static float forca(Carregado k, float segundos) {
        return Mth.clamp(k.minimo() + (1 - k.minimo()) * segundos / Math.max(0.05f, k.carga()), k.minimo(), 1);
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
            if (config(EstadoJogador.de(p.jogador).personagem()) == null || !p.jogador.isAlive()) continue;
            acertar(p.jogador, p.golpe, p.forca);
        }
        // Modo combate: sai quem destransformou, morreu ou saiu.
        for (var it = MODO.values().iterator(); it.hasNext(); ) {
            ServerPlayer p = it.next();
            boolean fora = p.isRemoved() || p.hasDisconnected();
            if (fora || config(EstadoJogador.de(p).personagem()) == null || EstadoJogador.de(p).destransformarEm() != 0L || p.isSpectator()) {
                it.remove();
                CARREGANDO.remove(p.getUUID());
                if (!fora) Rede.CANAL.send(new Rede.ModoVisto(p.getId(), ""), Canal.rastreandoEProprio(p));
            }
        }
    }

    public static void esquecer(ServerPlayer player) {
        ULTIMOS.remove(player.getUUID());
        CARREGANDO.remove(player.getUUID());
        MODO.remove(player.getUUID());
    }

    private static void acertar(ServerPlayer player, Golpe g, float forca) {
        if (!(player.level() instanceof ServerLevel level)) return;
        List<LivingEntity> alvos = alvos(player, g);
        if (alvos.isEmpty()) return;
        float dano = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * g.dano() * forca);
        Vec3 olhar = player.getLookAngle();
        boolean noAr = !player.onGround();
        for (LivingEntity alvo : alvos) {
            if (alvo instanceof Player outro && !player.canHarmPlayer(outro)) continue;
            Vec3 ponto = pontoDoImpacto(player, alvo);
            // Cada golpe do combo conta: sem a invencibilidade de meio segundo depois de levar dano.
            alvo.invulnerableTime = 0;
            boolean feriu = alvo.hurtServer(level, player.damageSources().playerAttack(player), dano);
            if (!feriu) continue;
            empurrar(player, alvo, g, olhar, noAr, forca);
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

    private static void empurrar(ServerPlayer player, LivingEntity alvo, Golpe g, Vec3 olhar, boolean noAr, float forca) {
        double resiste = 1 - Mth.clamp(alvo.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1) * 0.7;
        if (resiste <= 0) return;
        Vec3 dir = alvo.position().subtract(player.position());
        Vec3 plano = new Vec3(dir.x, 0, dir.z);
        plano = plano.lengthSqr() < 1e-4 ? new Vec3(olhar.x, 0, olhar.z).normalize() : plano.normalize();
        Vec3 v;
        if (noAr && g.pesado()) {
            // No ar o golpe final manda o alvo para onde o herói olha (até para baixo, contra o chão).
            v = olhar.scale(g.empurrao() * 1.2 * forca);
        } else {
            v = plano.scale(g.empurrao() * forca).add(0, g.lancar() * forca + (alvo.onGround() ? 0.12 : 0.04), 0);
        }
        Vec3 atual = alvo.getDeltaMovement();
        alvo.setDeltaMovement(atual.scale(0.3).add(v.scale(resiste)));
        alvo.hurtMarked = true;
    }
}
