package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.content.BatwingEntidade;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.network.PacketDistributor;

import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Pilotar o Batwing no cliente. Física (chamada pelo tick da própria entidade quando o piloto é o jogador local):
 * o jato aponta aos poucos para onde o piloto olha; W acelera até a velocidade de cruzeiro, correr (Ctrl) liga o
 * pós-combustor até a velocidade máxima, S freia até pairar e, devagar, A e D andam de lado; pulo sobe e agachar
 * salta do Batwing. O jato inclina nas curvas (a câmera inclina junto só em primeira pessoa). Armas: botão de
 * atacar segura as metralhadoras, botão de usar dispara um míssil e a tecla de usar habilidade liga o modo
 * furtivo. Também desenha o painel do Batwing na tela e toca o motor de cada Batwing por perto.
 */
public final class Piloto {
    private static boolean atirava;
    private static float rolagemCam;
    private static int batida;
    private static final Map<Integer, Motor> MOTORES = new HashMap<>();
    /** Jogadores desenhados agora girados junto com o Batwing (o Post desfaz o giro do Pre). */
    private static final Set<Integer> GIRADOS = new HashSet<>();
    /** Altura do assento acima dos pés de quem está montado (Avatar.DEFAULT_VEHICLE_ATTACHMENT). */
    private static final float ASSENTO = 0.6f;

    private Piloto() {}

    public static void registrar() {
        BatwingEntidade.controleCliente = Piloto::controlar;
        // Os botões do mouse viram as armas: nada de quebrar blocos ou usar itens pilotando.
        InputEvent.InteractionKeyMappingTriggered.BUS.addListener((Predicate<InputEvent.InteractionKeyMappingTriggered>) e -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || BatwingEntidade.pilotando(mc.player) == null) return false;
            e.setSwingHand(false);
            if (e.isUseItem() && e.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
                enviar(BatwingEntidade.MISSIL);
            }
            return true;
        });
        ComputeFovModifierEvent.BUS.addListener(e -> {
            Minecraft mc = Minecraft.getInstance();
            if (e.getPlayer() != mc.player) return;
            BatwingEntidade b = BatwingEntidade.pilotando(mc.player);
            if (b == null) return;
            float frac = Mth.clamp(b.velocidade * 20 / Math.max(1, params().f("boost_speed", 60)), 0, 1.2f);
            e.setNewFovModifier(e.getNewFovModifier() * (1 + 0.22f * frac * frac));
        });
        ViewportEvent.ComputeCameraAngles.BUS.addListener(Piloto::camera);
        // Quem está sentado no Batwing inclina com ele, girando em volta do próprio assento.
        RenderLivingEvent.Pre.BUS.addListener((Predicate<RenderLivingEvent.Pre>) e -> {
            if (!(e.getState() instanceof AvatarRenderState s)) return false;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || !(mc.level.getEntity(s.id) instanceof Entity p) || !(p.getVehicle() instanceof BatwingEntidade b)) return false;
            var ps = e.getPoseStack();
            ps.pushPose();
            ps.translate(0, ASSENTO, 0);
            ps.mulPose(b.inclinacao(mc.getDeltaTracker().getGameTimeDeltaPartialTick(false)));
            ps.translate(0, -ASSENTO, 0);
            GIRADOS.add(s.id);
            return false;
        });
        RenderLivingEvent.Post.BUS.addListener(e -> {
            if (e.getState() instanceof AvatarRenderState s && GIRADOS.remove(s.id)) e.getPoseStack().popPose();
        });
    }

    /**
     * Em primeira pessoa o olho fica na cabeça girada junto com o jato (mixin/CameraMixin), e não reto acima do
     * assento: senão, inclinado numa curva, a câmera sairia da cabine.
     * @return o quanto mover a câmera, ou null se a entidade não está num Batwing
     */
    public static Vec3 olhoNaCabine(Entity e, float pt) {
        if (e == null || !(e.getVehicle() instanceof BatwingEntidade b)) return null;
        float h = e.getEyeHeight() - ASSENTO;
        Vector3f v = b.inclinacao(pt).transform(new Vector3f(0, h, 0));
        return new Vec3(v.x, v.y - h, v.z);
    }

    public static void limpar() {
        atirava = false;
        MOTORES.clear();
    }

    private static void enviar(int acao) {
        Rede.CANAL.send(new Rede.Veiculo(acao), PacketDistributor.SERVER.noArg());
    }

    /** Parâmetros do veículo do personagem do jogador local (do catálogo de personagens). */
    public static Params params() {
        try {
            var json = JsonParser.parseString(EstadoCliente.catalogo.getOrDefault(EstadoCliente.atual.personagem(), "{}")).getAsJsonObject();
            if (json.has("abilities")) {
                for (var e : json.getAsJsonArray("abilities")) {
                    var o = e.getAsJsonObject();
                    if ("vehicle".equals(o.get("type").getAsString())) return new Params(o);
                }
            }
        } catch (RuntimeException ignorado) {
            // catálogo ainda não chegou
        }
        return Params.EMPTY;
    }

    /** Tick do cliente (Cliente.tick): metralhadora segurada e o som dos motores. */
    public static void tick(Minecraft mc) {
        if (mc.level == null || mc.player == null) return;
        BatwingEntidade b = BatwingEntidade.pilotando(mc.player);
        boolean atira = b != null && mc.gui.screen() == null && mc.options.keyAttack.isDown();
        if (atira != atirava) {
            atirava = atira;
            enviar(atira ? BatwingEntidade.TIRO_LIGA : BatwingEntidade.TIRO_DESLIGA);
        }
        if (batida > 0) batida--;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof BatwingEntidade bw && bw.estado() != BatwingEntidade.CHEGANDO || e instanceof BatwingEntidade bw2 && bw2.tickCount > 8) {
                Motor m = MOTORES.get(e.getId());
                // Dentro do jato o motor toca junto do ouvinte; quem embarca ou salta troca de som.
                boolean dentro = mc.player.getVehicle() == e;
                if (m != null && !m.isStopped() && m.dentro != dentro) {
                    mc.getSoundManager().stop(m);
                    m = null;
                }
                if (m == null || m.isStopped()) {
                    m = new Motor((BatwingEntidade) e, dentro);
                    MOTORES.put(e.getId(), m);
                    mc.getSoundManager().play(m);
                }
            }
        }
        MOTORES.values().removeIf(Motor::isStopped);
    }

    // ------------------------------------------------------------------ física

    private static void controlar(BatwingEntidade b) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || p.getVehicle() != b || b.estado() != BatwingEntidade.PILOTADO) return;
        Params prm = params();
        float cruzeiro = prm.f("speed", 30) / 20f, maxima = prm.f("boost_speed", 60) / 20f;
        boolean livre = mc.gui.screen() == null;
        boolean frente = livre && mc.options.keyUp.isDown(), tras = livre && mc.options.keyDown.isDown();
        boolean esq = livre && mc.options.keyLeft.isDown(), dir = livre && mc.options.keyRight.isDown();
        boolean sobe = livre && mc.options.keyJump.isDown(), boost = livre && mc.options.keySprint.isDown();
        // Acelerador: mantém a velocidade sem tecla; acima do cruzeiro sem pós-combustor, volta devagar a ele.
        float v = b.velocidade;
        if (boost && (frente || v > cruzeiro * 0.8f)) v = Math.min(maxima, v + 0.045f);
        else if (frente) v = v < cruzeiro ? Math.min(cruzeiro, v + 0.05f) : Math.max(cruzeiro, v - 0.03f);
        else if (v > cruzeiro) v = Math.max(cruzeiro, v - 0.03f);
        if (tras) v = Math.max(0, v - 0.07f);
        // Aponta para onde o piloto olha, virando mais devagar quanto mais rápido.
        float giroMax = Mth.lerp(Mth.clamp(v / maxima, 0, 1), 7.5f, 3.2f);
        float yaw0 = b.getYRot();
        float dy = Mth.clamp(Mth.wrapDegrees(p.getYRot() - yaw0), -giroMax, giroMax);
        float alvoPitch = Mth.clamp(p.getXRot(), -65, 65);
        float dp = Mth.clamp(alvoPitch - b.getXRot(), -giroMax * 0.8f, giroMax * 0.8f);
        b.setYRot(yaw0 + dy);
        b.setXRot(b.getXRot() + dp);
        // Devagar o jato paira: o nariz não manda subir nem descer, só o pulo.
        float lento = 1 - Mth.clamp(v / (cruzeiro * 0.6f), 0, 1);
        Vec3 nariz = Vec3.directionFromRotation(b.getXRot() * (1 - lento), b.getYRot());
        Vec3 lado = Vec3.directionFromRotation(0, b.getYRot() - 90);
        double lateral = (esq ? 1 : 0) - (dir ? 1 : 0);
        Vec3 mov = nariz.scale(v).add(lado.scale(lateral * 0.28 * Math.max(0.25, lento)))
            .add(0, sobe ? 0.32 : 0, 0);
        // Pairando, balança de leve.
        if (v < 0.05 && !sobe) mov = mov.add(0, Math.sin(b.tickCount * 0.08) * 0.01, 0);
        b.setDeltaMovement(mov);
        b.move(MoverType.SELF, mov);
        if (b.horizontalCollision && v > 0.7f) {
            // Bateu: perde quase toda a velocidade e a câmera sacode.
            v *= 0.25f;
            if (batida == 0) {
                Voador.tremer(10);
                p.playSound(net.minecraft.sounds.SoundEvents.ANVIL_LAND, 0.8f, 0.6f);
                batida = 20;
            }
        }
        b.velocidade = v;
        float alvoRol = Mth.clamp(-dy * 7.5f, -60, 60) - (float) lateral * 18 * lento;
        b.rolagem += (alvoRol - b.rolagem) * 0.18f;
    }

    private static void camera(ViewportEvent.ComputeCameraAngles e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        BatwingEntidade b = BatwingEntidade.pilotando(mc.player);
        float alvo = b != null && mc.options.getCameraType().isFirstPerson()
            ? Mth.lerp((float) e.getPartialTick(), b.rolagemO, b.rolagem) * 0.45f : 0;
        rolagemCam += (alvo - rolagemCam) * 0.5f;
        if (Math.abs(rolagemCam) < 0.01f) return;
        e.setRoll(e.getRoll() + rolagemCam);
        e.getCamera().setRotation(e.getYaw(), e.getPitch(), e.getRoll());
        CameraRenderState cs = mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
        cs.orientation.set(e.getCamera().rotation());
        e.getCamera().getViewRotationMatrix(cs.viewRotationMatrix);
    }

    // ------------------------------------------------------------------ painel

    private static final int AMBAR = 0xFFFFC23A, VERDE = 0xFF7CF0A0, CINZA = 0xFF9AA4B8, VERMELHO = 0xFFFF5A4A;

    /** Painel do Batwing no canto de baixo à direita: tempo, velocidade, altitude, armas e furtivo. */
    public static void desenhar(GuiGraphicsExtractor g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.hud.isHidden()) return;
        BatwingEntidade b = BatwingEntidade.pilotando(mc.player);
        if (b == null) return;
        Font f = mc.font;
        Params prm = params();
        int w = 150, h = 97, x = g.guiWidth() - w - 6, y = g.guiHeight() - h - 30;
        g.fill(x, y, x + w, y + h, 0xB0080A12);
        g.fill(x, y, x + w, y + 1, AMBAR);
        g.fill(x, y + h - 1, x + w, y + h, 0x80FFC23A);
        g.text(f, Component.translatable("dcuniverse.batwing.titulo").withStyle(ChatFormatting.BOLD), x + 5, y + 4, AMBAR, true);
        // Tempo restante
        int total = Math.round(prm.f("duration", 90) * 20);
        float frac = Mth.clamp(b.restante() / (float) Math.max(1, total), 0, 1);
        String seg = (b.restante() + 19) / 20 + " s";
        g.text(f, seg, x + w - 5 - f.width(seg), y + 4, frac < 0.2f ? VERMELHO : 0xFFE8ECF4, true);
        g.fill(x + 5, y + 14, x + w - 5, y + 16, 0xFF20242E);
        g.fill(x + 5, y + 14, x + 5 + Math.round((w - 10) * frac), y + 16, frac < 0.2f ? VERMELHO : AMBAR);
        // Velocidade e altitude
        int vel = Math.round(b.velocidade * 20);
        g.text(f, Component.translatable("dcuniverse.batwing.velocidade", vel), x + 5, y + 21, 0xFFE8ECF4, true);
        g.text(f, Component.translatable("dcuniverse.batwing.altitude", (int) Math.floor(b.getY())), x + 82, y + 21, 0xFFE8ECF4, true);
        // Metralhadoras: calor
        float calor = b.calor();
        g.text(f, Component.translatable("dcuniverse.batwing.metralhadoras"), x + 5, y + 34, CINZA, true);
        int bx = x + 5, by = y + 44, bw = w - 10;
        g.fill(bx, by, bx + bw, by + 4, 0xFF20242E);
        int corCalor = calor > 0.8f ? VERMELHO : calor > 0.5f ? 0xFFFF9A3A : AMBAR;
        g.fill(bx, by, bx + Math.round(bw * calor), by + 4, corCalor);
        // Mísseis
        g.text(f, Component.translatable("dcuniverse.batwing.misseis"), x + 5, y + 52, CINZA, true);
        int max = prm.i("missiles", 4);
        for (int i = 0; i < max; i++) {
            int mx = x + 60 + i * 12, my = y + 52;
            boolean tem = i < b.misseis();
            g.fill(mx, my + 2, mx + 8, my + 6, tem ? 0xFFE8ECF4 : 0xFF2A2F3A);
            g.fill(mx + 8, my + 3, mx + 10, my + 5, tem ? VERMELHO : 0xFF2A2F3A);
            if (i == b.misseis() && b.misseis() < max) {
                g.fill(mx, my + 7, mx + Math.round(8 * b.recargaMissil()), my + 8, AMBAR);
            }
        }
        // Mira travada e furtivo
        boolean travado = b.alvoTravado() >= 0 && b.misseis() > 0;
        if (travado) {
            boolean pisca = (mc.player.tickCount / 4) % 2 == 0;
            g.text(f, Component.translatable("dcuniverse.batwing.travado"), x + 5, y + 64, pisca ? VERMELHO : 0xFFFFB0A0, true);
        }
        if (b.oculto()) {
            Component c = Component.translatable("dcuniverse.batwing.furtivo");
            g.text(f, c, x + w - 5 - f.width(c), y + 64, VERDE, true);
        }
        // Dicas
        g.text(f, Component.translatable("dcuniverse.batwing.dica1"), x + 5, y + 75, 0xFF7F8898, false);
        g.text(f, Component.translatable("dcuniverse.batwing.dica2"), x + 5, y + 85, 0xFF7F8898, false);
    }

    // ------------------------------------------------------------------ motor

    /**
     * Ronco do motor preso a um Batwing: o tom sobe com a velocidade; no modo furtivo quase some. Para quem está
     * dentro, o som não tem posição (fica com o ouvinte): com a câmera de terceira pessoa a uns 11 blocos, um som
     * preso ao jato chegava baixo, de lado e aos trancos, porque o jato pula de posição a cada tick e a câmera não.
     */
    private static final class Motor extends AbstractTickableSoundInstance {
        private final BatwingEntidade b;
        final boolean dentro;

        Motor(BatwingEntidade b, boolean dentro) {
            super(Sons.BATWING_MOTOR.get(), SoundSource.NEUTRAL, b.getRandom());
            this.b = b;
            this.dentro = dentro;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01f;
            if (dentro) {
                this.relative = true;
                this.attenuation = Attenuation.NONE;
            } else {
                this.x = b.getX();
                this.y = b.getY();
                this.z = b.getZ();
            }
        }

        @Override
        public void tick() {
            if (b.isRemoved()) {
                volume -= 0.1f;
                if (volume <= 0) stop();
                return;
            }
            double v = Math.sqrt((b.getX() - b.xo) * (b.getX() - b.xo) + (b.getY() - b.yo) * (b.getY() - b.yo) + (b.getZ() - b.zo) * (b.getZ() - b.zo));
            float alvo = b.oculto() ? 0.18f : 0.9f;
            volume += (alvo - volume) * 0.15f;
            pitch = (float) Mth.clamp(0.7 + v * 0.32, 0.6, 1.8);
            if (dentro) return;
            x = b.getX();
            y = b.getY();
            z = b.getZ();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }

}
