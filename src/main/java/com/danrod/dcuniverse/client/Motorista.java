package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.content.VeiculoBat;
import com.danrod.dcuniverse.content.VeiculoDono;
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
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Dirigir o Batmóvel, a Batmoto e o Batbarco no cliente (content/VeiculoBat). Física de quem pilota:
 * W acelera, S freia e dá ré, A e D esterçam (a curva fecha menos em alta velocidade), Ctrl gasta o turbo, o pulo
 * puxa o freio de mão (o carro derrapa) ou faz a moto saltar, e no barco mergulhado sobe. O mouse só mira: as
 * metralhadoras e os mísseis vão para onde o piloto olha. O barco boia na linha d'água e, mergulhado, segue o
 * nariz para onde o piloto olha, sem passar da superfície. Também desenha o painel, toca os motores e os pneus
 * e solta as partículas (fumaça na derrapagem, chama do turbo, esteira e bolhas) de todos os veículos por perto.
 */
public final class Motorista {
    private static boolean atirava;
    private static int batida;
    private static final Map<Integer, Motor> MOTORES = new HashMap<>();
    private static Pneu pneu;
    private static final Set<Integer> GIRADOS = new HashSet<>();
    private static final float ASSENTO = 0.6f;

    private Motorista() {}

    public static void registrar() {
        VeiculoBat.controleCliente = Motorista::controlar;
        InputEvent.InteractionKeyMappingTriggered.BUS.addListener((Predicate<InputEvent.InteractionKeyMappingTriggered>) e -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || VeiculoBat.pilotando(mc.player) == null) return false;
            e.setSwingHand(false);
            if (e.isUseItem() && e.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) enviar(VeiculoDono.SECUNDARIA);
            return true;
        });
        ComputeFovModifierEvent.BUS.addListener(e -> {
            Minecraft mc = Minecraft.getInstance();
            if (e.getPlayer() != mc.player) return;
            VeiculoBat v = VeiculoBat.pilotando(mc.player);
            if (v == null) return;
            float frac = Mth.clamp(Math.abs(v.velocidade) * 20 / Math.max(1, params(v).f("boost_speed", 40)), 0, 1.2f);
            e.setNewFovModifier(e.getNewFovModifier() * (1 + (v.turboLigado ? 0.2f : 0.12f) * frac * frac));
        });
        // Quem está montado inclina junto (a moto deita nas curvas), girando em volta do próprio assento.
        RenderLivingEvent.Pre.BUS.addListener((Predicate<RenderLivingEvent.Pre>) e -> {
            if (!(e.getState() instanceof AvatarRenderState s)) return false;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || !(mc.level.getEntity(s.id) instanceof Entity p) || !(p.getVehicle() instanceof VeiculoBat v)) return false;
            var ps = e.getPoseStack();
            ps.pushPose();
            ps.translate(0, ASSENTO, 0);
            ps.mulPose(v.inclinacao(mc.getDeltaTracker().getGameTimeDeltaPartialTick(false)));
            // Na moto o corpo vai deitado para a frente, em cima do tanque.
            if (v.forma() == VeiculoBat.BATMOTO) ps.mulPose(com.mojang.math.Axis.XP.rotationDegrees(22));
            ps.translate(0, -ASSENTO, 0);
            GIRADOS.add(s.id);
            return false;
        });
        RenderLivingEvent.Post.BUS.addListener(e -> {
            if (e.getState() instanceof AvatarRenderState s && GIRADOS.remove(s.id)) e.getPoseStack().popPose();
        });
    }

    /**
     * Pose na Batmoto: corpo deitado para a frente (o giro vem do RenderLivingEvent), mãos no guidão, pernas
     * dobradas nas pedaleiras e a cabeça erguida para olhar a estrada. O passageiro só abraça o piloto.
     */
    public static void poseMoto(net.minecraft.client.model.player.PlayerModel m, AvatarRenderState s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getEntity(s.id) instanceof Entity p) || !(p.getVehicle() instanceof VeiculoBat v)
            || v.forma() != VeiculoBat.BATMOTO) return;
        boolean piloto = v.donoId() == p.getId();
        // Pernas abertas por fora do tanque, para a frente até as pedaleiras.
        m.rightLeg.xRot = -1.2f;
        m.leftLeg.xRot = -1.2f;
        m.rightLeg.yRot = 0.1f;
        m.leftLeg.yRot = -0.1f;
        m.rightLeg.zRot = 0.32f;
        m.leftLeg.zRot = -0.32f;
        m.rightArm.xRot = piloto ? -1.2f : -0.9f;
        m.leftArm.xRot = piloto ? -1.2f : -0.9f;
        m.rightArm.yRot = piloto ? -0.12f : 0.35f;
        m.leftArm.yRot = piloto ? 0.12f : -0.35f;
        m.rightArm.zRot = 0.05f;
        m.leftArm.zRot = -0.05f;
        m.head.xRot -= 22 * Mth.DEG_TO_RAD;
    }

    public static void limpar() {
        atirava = false;
        MOTORES.clear();
        pneu = null;
    }

    private static void enviar(int acao) {
        Rede.CANAL.send(new Rede.Veiculo(acao), PacketDistributor.SERVER.noArg());
    }

    /** Parâmetros da habilidade do veículo (a do mesmo "model"), do catálogo do personagem do jogador local. */
    public static Params params(VeiculoBat v) {
        try {
            var json = JsonParser.parseString(EstadoCliente.catalogo.getOrDefault(EstadoCliente.atual.personagem(), "{}")).getAsJsonObject();
            if (json.has("abilities")) {
                for (var e : json.getAsJsonArray("abilities")) {
                    var o = e.getAsJsonObject();
                    if ("vehicle".equals(o.get("type").getAsString()) && o.has("model") && v.modelo().equals(o.get("model").getAsString())) {
                        return new Params(o);
                    }
                }
            }
        } catch (RuntimeException ignorado) {
            // catálogo ainda não chegou
        }
        return Params.EMPTY;
    }

    /** Em primeira pessoa o olho fica na cabeça inclinada junto com o veículo (como no Batwing). */
    public static Vec3 olhoNoBanco(Entity e, float pt) {
        if (e == null || !(e.getVehicle() instanceof VeiculoBat v)) return null;
        float h = e.getEyeHeight() - ASSENTO;
        var q = v.inclinacao(pt);
        if (v.forma() == VeiculoBat.BATMOTO) q.rotateX(22 * Mth.DEG_TO_RAD);
        Vector3f d = q.transform(new Vector3f(0, h, 0));
        return new Vec3(d.x, d.y - h, d.z);
    }

    /** Tick do cliente: metralhadoras seguradas, sons dos motores e dos pneus, partículas de todos os veículos. */
    public static void tick(Minecraft mc) {
        if (mc.level == null || mc.player == null) return;
        VeiculoBat meu = VeiculoBat.pilotando(mc.player);
        boolean atira = meu != null && mc.gui.screen() == null && mc.options.keyAttack.isDown();
        if (atira != atirava) {
            atirava = atira;
            enviar(atira ? VeiculoDono.TIRO_LIGA : VeiculoDono.TIRO_DESLIGA);
        }
        if (batida > 0) batida--;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof VeiculoBat v)) continue;
            particulas(mc, v);
            Motor m = MOTORES.get(e.getId());
            boolean dentro = mc.player.getVehicle() == e;
            if (m != null && !m.isStopped() && m.dentro != dentro) {
                mc.getSoundManager().stop(m);
                m = null;
            }
            if (m == null || m.isStopped()) {
                m = new Motor(v, dentro);
                MOTORES.put(e.getId(), m);
                mc.getSoundManager().play(m);
            }
        }
        MOTORES.values().removeIf(Motor::isStopped);
        if (meu != null && (pneu == null || pneu.isStopped() || pneu.v != meu)) {
            pneu = new Pneu(meu);
            mc.getSoundManager().play(pneu);
        }
    }

    // ------------------------------------------------------------------ física

    /** A câmera vira junto com o veículo (como no barco), e o mouse continua livre para olhar em volta. */
    private static void virarJunto(float taxa) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        p.setYRot(p.getYRot() - taxa);
        p.setYHeadRot(p.getYHeadRot() - taxa);
    }

    private static void controlar(VeiculoBat v) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || p.getVehicle() != v || v.estado() != VeiculoDono.PILOTADO) return;
        Params prm = params(v);
        boolean livre = mc.gui.screen() == null;
        boolean frente = livre && mc.options.keyUp.isDown(), tras = livre && mc.options.keyDown.isDown();
        boolean esq = livre && mc.options.keyLeft.isDown(), dir = livre && mc.options.keyRight.isDown();
        boolean pulo = livre && mc.options.keyJump.isDown(), turbo = livre && mc.options.keySprint.isDown();
        if (v.forma() == VeiculoBat.BATBARCO) barco(v, p, prm, frente, tras, esq, dir, pulo, turbo);
        else chao(v, prm, frente, tras, esq, dir, pulo, turbo);
    }

    /** Turbo: dura uns 3 s cheio e recarrega em uns 10 s; precisa de um pouco para ligar. */
    private static boolean turbo(VeiculoBat v, boolean quer) {
        boolean liga = quer && (v.turboLigado ? v.turbo > 0 : v.turbo > 0.2f);
        if (liga) v.turbo = Math.max(0, v.turbo - 1 / 60f);
        else v.turbo = Math.min(1, v.turbo + 1 / 200f);
        if (liga && !v.turboLigado) Minecraft.getInstance().player.playSound(Sons.VEICULO_TURBO.get(), 1f, 1f);
        v.turboLigado = liga;
        return liga;
    }

    private static void chao(VeiculoBat v, Params prm, boolean frente, boolean tras, boolean esq, boolean dir, boolean pulo, boolean querTurbo) {
        boolean moto = v.forma() == VeiculoBat.BATMOTO;
        float max = prm.f("speed", moto ? 30 : 26) / 20f, maxTurbo = prm.f("boost_speed", moto ? 50 : 42) / 20f;
        float acel = prm.f("accel", moto ? 0.042f : 0.032f);
        boolean chao = v.onGround();
        boolean turbo = turbo(v, querTurbo && frente);
        float vel = v.velocidade;
        boolean agua = v.isInWater();
        if (agua) max = maxTurbo = 0.12f;
        if (chao || agua) {
            if (frente) {
                float teto = turbo ? maxTurbo : max;
                if (vel < teto) vel = Math.min(teto, vel + acel * (vel < 0 ? 3 : turbo ? 1.8f : 1));
                else vel = Math.max(teto, vel - 0.02f);
            } else if (tras) {
                vel = vel > 0.02f ? vel - 0.085f : Math.max(-max * 0.35f, vel - acel * 0.7f);
            } else {
                vel *= 0.975f;
                if (Math.abs(vel) < 0.01f) vel = 0;
            }
        }
        // Esterço: anda para o alvo; a curva fecha menos quanto mais rápido e precisa de velocidade.
        float alvoEst = (esq ? 1 : 0) - (dir ? 1 : 0);
        v.esterco += (alvoEst - v.esterco) * 0.35f;
        float rapidez = Mth.clamp(Math.abs(vel) / 0.3f, 0, 1);
        float taxa = prm.f("steer", moto ? 5f : 4f) * v.esterco * rapidez * Math.signum(vel) * (1 - 0.45f * Mth.clamp(Math.abs(vel) / maxTurbo, 0, 1));
        boolean freioMao = !moto && pulo && chao && Math.abs(vel) > 0.35f;
        if (freioMao) {
            taxa *= 1.9f;
            vel *= 0.982f;
        }
        if (!chao && !agua) taxa *= 0.35f;
        float yaw = v.getYRot() - taxa;
        v.setYRot(yaw);
        virarJunto(taxa);
        // Aderência: o rumo do movimento segue o nariz; no freio de mão quase não segue (derrapa).
        if (!Float.isFinite(v.rumo) || v.tickCount < 2) v.rumo = yaw;
        float aderencia = freioMao ? 0.05f : moto ? 0.5f : 0.32f;
        if (!chao) aderencia = 0.02f;
        v.rumo += Mth.wrapDegrees(yaw - v.rumo) * aderencia;
        float desvio = Mth.wrapDegrees(yaw - v.rumo);
        v.derrapando = chao && Math.abs(vel) > 0.35f && Math.abs(desvio) > 9;
        if (v.derrapando) vel *= 0.993f;
        // Salto da moto.
        Vec3 antes = v.getDeltaMovement();
        double vy = chao ? -0.08 : (antes.y - 0.08) * 0.98;
        if (v.puloEspera > 0) v.puloEspera--;
        if (moto && pulo && chao && v.puloEspera == 0) {
            vy = prm.f("jump", 0.7f);
            v.puloEspera = 12;
            Minecraft.getInstance().player.playSound(Sons.VEICULO_TURBO.get(), 0.6f, 1.4f);
        }
        if (agua) vy = Math.max(vy, -0.04);
        Vec3 mov = Vec3.directionFromRotation(0, v.rumo).scale(vel);
        Vec3 dlt = new Vec3(mov.x, vy, mov.z);
        v.setDeltaMovement(dlt);
        double y0 = v.getY();
        v.move(MoverType.SELF, dlt);
        if (v.horizontalCollision && Math.abs(vel) > 0.25f) {
            // Bateu de frente: perde a velocidade e a câmera sacode.
            if (Math.abs(vel) > 0.6f && batida == 0) {
                Voador.tremer(Math.min(12, Math.round(Math.abs(vel) * 9)));
                Minecraft.getInstance().player.playSound(net.minecraft.sounds.SoundEvents.ANVIL_LAND, 0.7f, 0.55f);
                batida = 15;
            }
            vel *= 0.3f;
        }
        v.velocidade = vel;
        // Inclinações: o nariz acompanha a subida (e no ar cai aos poucos); o carro rola pouco e a moto deita.
        double sub = v.getY() - y0;
        float alvoArf = chao ? Mth.clamp((float) (-Mth.atan2(sub, Math.max(0.15, Math.abs(vel))) * Mth.RAD_TO_DEG), -32, 32)
            : Mth.clamp((float) (-vy * 28), -25, 25);
        v.arfagem += (alvoArf - v.arfagem) * (chao ? 0.35f : 0.08f);
        float alvoRol = moto ? Mth.clamp(-taxa * Math.abs(vel) * 9f, -42, 42) : Mth.clamp(taxa * Math.abs(vel) * 0.9f, -5, 5);
        v.rolagem += (alvoRol - v.rolagem) * (moto ? 0.2f : 0.3f);
    }

    private static void barco(VeiculoBat v, LocalPlayer p, Params prm, boolean frente, boolean tras, boolean esq, boolean dir, boolean pulo, boolean querTurbo) {
        float max = prm.f("speed", 22) / 20f, maxTurbo = prm.f("boost_speed", 36) / 20f, acel = prm.f("accel", 0.028f);
        boolean agua = v.isInWater();
        boolean sub = v.submerso() && agua;
        boolean turbo = turbo(v, querTurbo && frente && agua);
        if (sub) {
            max = prm.f("dive_speed", 14) / 20f;
            maxTurbo = max * 1.4f;
        }
        if (!agua) max = maxTurbo = 0.07f;
        float vel = v.velocidade;
        if (frente) {
            float teto = turbo ? maxTurbo : max;
            vel = vel < teto ? Math.min(teto, vel + acel * (turbo ? 1.6f : 1)) : Math.max(teto, vel - 0.02f);
        } else if (tras) {
            vel = vel > 0.02f ? vel - 0.05f : Math.max(-max * 0.3f, vel - acel * 0.5f);
        } else {
            vel *= agua ? 0.985f : 0.8f;
            if (Math.abs(vel) < 0.005f) vel = 0;
        }
        float alvoEst = (esq ? 1 : 0) - (dir ? 1 : 0);
        v.esterco += (alvoEst - v.esterco) * 0.3f;
        float rapidez = Mth.clamp(Math.abs(vel) / 0.2f, 0.25f, 1);
        float taxa = prm.f("steer", 3.4f) * v.esterco * rapidez * (vel < 0 ? -1 : 1) * (1 - 0.35f * Mth.clamp(Math.abs(vel) / maxTurbo, 0, 1));
        float yaw = v.getYRot() - taxa;
        v.setYRot(yaw);
        virarJunto(taxa);
        if (!Float.isFinite(v.rumo) || v.tickCount < 2) v.rumo = yaw;
        // Na água a lancha escorrega um pouco nas curvas.
        v.rumo += Mth.wrapDegrees(yaw - v.rumo) * (sub ? 0.4f : 0.16f);
        Vec3 antes = v.getDeltaMovement();
        double vy;
        float alvoArf;
        if (sub) {
            // Mergulhado: o nariz vai para onde o piloto olha (sem passar de 50°); o pulo sobe.
            float alvo = Mth.clamp(p.getXRot(), -50, 50);
            v.arfagem += Mth.clamp(alvo - v.arfagem, -3, 3);
            alvoArf = v.arfagem;
            vy = -Mth.sin(v.arfagem * Mth.DEG_TO_RAD) * vel + (pulo ? 0.12 : 0);
            // Não fura a superfície: com o casco quase todo fora, só desce.
            double fh = v.getFluidHeight(FluidTags.WATER);
            if (fh < v.getBbHeight() + 0.25 && vy > 0) vy = Math.min(vy, (fh - v.getBbHeight() - 0.25) * 0.2);
        } else if (agua) {
            // Boia: mola até a linha d'água, mais alta quanto mais rápido (a proa sobe para planar).
            double fh = v.getFluidHeight(FluidTags.WATER);
            double linha = 0.45 - Mth.clamp(Math.abs(vel) * 0.25, 0, 0.25);
            vy = (antes.y + (fh - linha) * 0.12) * 0.82;
            alvoArf = -Mth.clamp(Math.abs(vel) * 9, 0, 9) + Mth.sin(v.tickCount * 0.12f) * 1.5f;
        } else {
            vy = v.onGround() ? -0.08 : (antes.y - 0.08) * 0.98;
            alvoArf = 0;
        }
        if (!sub) v.arfagem += (alvoArf - v.arfagem) * 0.15f;
        float horiz = sub ? Mth.cos(v.arfagem * Mth.DEG_TO_RAD) : 1;
        Vec3 mov = Vec3.directionFromRotation(0, v.rumo).scale(vel * horiz);
        Vec3 dlt = new Vec3(mov.x, vy, mov.z);
        v.setDeltaMovement(dlt);
        v.move(MoverType.SELF, dlt);
        if (v.horizontalCollision && Math.abs(vel) > 0.3f) {
            if (batida == 0) {
                Voador.tremer(8);
                p.playSound(net.minecraft.sounds.SoundEvents.WOOD_BREAK, 0.8f, 0.6f);
                batida = 15;
            }
            vel *= 0.3f;
        }
        v.velocidade = vel;
        float alvoRol = Mth.clamp(taxa * Math.abs(vel) * (sub ? 6 : 4.5f), -18, 18);
        v.rolagem += (alvoRol - v.rolagem) * 0.15f;
    }

    // ------------------------------------------------------------------ partículas

    private static void particulas(Minecraft mc, VeiculoBat v) {
        var level = mc.level;
        Vec3 d = v.position().subtract(v.xo, v.yo, v.zo);
        double vel = d.horizontalDistance();
        float yaw = v.getYRot();
        Vec3 frente = Vec3.directionFromRotation(0, yaw), lado = Vec3.directionFromRotation(0, yaw - 90);
        boolean moto = v.forma() == VeiculoBat.BATMOTO;
        if (v.forma() == VeiculoBat.BATBARCO) {
            if (!v.isInWater()) return;
            if (v.submerso()) {
                if (v.tickCount % 2 == 0) {
                    Vec3 h = v.position().add(frente.scale(-2.3)).add(0, 0.4, 0);
                    level.addParticle(ParticleTypes.BUBBLE, h.x, h.y, h.z, -frente.x * 0.2, 0.05, -frente.z * 0.2);
                }
                return;
            }
            if (vel > 0.25) {
                // Proa abrindo água e a esteira atrás.
                for (int s = -1; s <= 1; s += 2) {
                    Vec3 pr = v.position().add(frente.scale(1.6)).add(lado.scale(s * 0.9)).add(0, 0.35, 0);
                    level.addParticle(ParticleTypes.SPLASH, pr.x, pr.y, pr.z, lado.x * s * 0.2, 0.15, lado.z * s * 0.2);
                }
                Vec3 t = v.position().add(frente.scale(-2.6)).add(0, 0.3, 0);
                level.addParticle(ParticleTypes.CLOUD, t.x + (v.getRandom().nextDouble() - 0.5), t.y, t.z + (v.getRandom().nextDouble() - 0.5), 0, 0.02, 0);
                level.addParticle(ParticleTypes.SPLASH, t.x, t.y, t.z, -frente.x * 0.1, 0.25, -frente.z * 0.1);
            }
            return;
        }
        // Derrapagem: rumo (pelo movimento) longe do nariz.
        boolean derrapa = vel > 0.35 && v.onGround() && Math.abs(Mth.wrapDegrees((float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90 - yaw)) > 9;
        if (v.isLocalInstanceAuthoritative()) derrapa = v.derrapando;
        if (derrapa) {
            for (int s = -1; s <= 1; s += 2) {
                if (moto && s > 0) continue;
                Vec3 r = v.position().add(frente.scale(moto ? -0.9 : -1.7)).add(lado.scale(moto ? 0 : s * 0.95)).add(0, 0.15, 0);
                level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, r.x, r.y, r.z, 0, 0.01, 0);
                level.addParticle(ParticleTypes.SMOKE, r.x, r.y, r.z, (v.getRandom().nextDouble() - 0.5) * 0.1, 0.05, (v.getRandom().nextDouble() - 0.5) * 0.1);
            }
        } else if (vel > 0.5 && v.onGround() && v.tickCount % 2 == 0) {
            // Poeira do chão atrás das rodas.
            BlockPos baixo = v.blockPosition().below();
            BlockState st = level.getBlockState(baixo);
            if (!st.isAir()) {
                Vec3 r = v.position().add(frente.scale(moto ? -1 : -1.9));
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, st), r.x, r.y + 0.1, r.z, -frente.x * 0.2, 0.15, -frente.z * 0.2);
            }
        }
        // Turbo: chama azul e laranja do escapamento (para quem vê de fora, pela velocidade).
        boolean turbo = v.isLocalInstanceAuthoritative() ? v.turboLigado : vel > (moto ? 1.55 : 1.35);
        if (turbo) {
            Vec3 esc = v.position().add(frente.scale(moto ? -1.25 : -2.55)).add(0, moto ? 0.45 : 0.55, 0);
            for (int i = 0; i < 2; i++) {
                level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, esc.x, esc.y, esc.z, -frente.x * 0.25 + d.x, 0.01, -frente.z * 0.25 + d.z);
                level.addParticle(ParticleTypes.FLAME, esc.x, esc.y, esc.z, -frente.x * 0.15 + d.x, 0.02, -frente.z * 0.15 + d.z);
            }
        } else if (v.tickCount % 4 == 0 && vel < 0.4) {
            Vec3 esc = v.position().add(frente.scale(moto ? -1.25 : -2.55)).add(0, moto ? 0.45 : 0.55, 0);
            level.addParticle(ParticleTypes.SMOKE, esc.x, esc.y, esc.z, -frente.x * 0.05, 0.02, -frente.z * 0.05);
        }
    }

    // ------------------------------------------------------------------ painel

    private static final int AMBAR = 0xFFFFC23A, VERDE = 0xFF7CF0A0, CINZA = 0xFF9AA4B8, VERMELHO = 0xFFFF5A4A, AZUL = 0xFF5AC8FF;

    /** Painel do veículo no canto de baixo à direita: tempo, velocidade, turbo, armas e o especial. */
    public static void desenhar(GuiGraphicsExtractor g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.hud.isHidden()) return;
        VeiculoBat v = VeiculoBat.pilotando(mc.player);
        if (v == null) return;
        Font f = mc.font;
        Params prm = params(v);
        String id = v.modelo();
        int w = 176, h = 107, x = g.guiWidth() - w - 6, y = g.guiHeight() - h - 30;
        g.fill(x, y, x + w, y + h, 0xB0080A12);
        g.fill(x, y, x + w, y + 1, AMBAR);
        g.fill(x, y + h - 1, x + w, y + h, 0x80FFC23A);
        g.text(f, Component.translatable("dcuniverse.veiculo." + id).withStyle(ChatFormatting.BOLD), x + 5, y + 4, AMBAR, true);
        int total = Math.round(prm.f("duration", 90) * 20);
        float frac = Mth.clamp(v.restante() / (float) Math.max(1, total), 0, 1);
        String seg = (v.restante() + 19) / 20 + " s";
        g.text(f, seg, x + w - 5 - f.width(seg), y + 4, frac < 0.2f ? VERMELHO : 0xFFE8ECF4, true);
        g.fill(x + 5, y + 14, x + w - 5, y + 16, 0xFF20242E);
        g.fill(x + 5, y + 14, x + 5 + Math.round((w - 10) * frac), y + 16, frac < 0.2f ? VERMELHO : AMBAR);
        int vel = Math.round(Math.abs(v.velocidade) * 20);
        g.text(f, Component.translatable("dcuniverse.batwing.velocidade", vel), x + 5, y + 21, 0xFFE8ECF4, true);
        if (v.forma() == VeiculoBat.BATBARCO && v.submerso()) {
            int prof = (int) Math.max(0, Math.round(v.getFluidHeight(FluidTags.WATER)));
            g.text(f, Component.translatable("dcuniverse.veiculo.profundidade", prof), x + 82, y + 21, AZUL, true);
        } else if (v.velocidade < -0.01f) {
            g.text(f, Component.translatable("dcuniverse.veiculo.re"), x + 82, y + 21, CINZA, true);
        }
        // Turbo
        g.text(f, Component.translatable("dcuniverse.veiculo.turbo"), x + 5, y + 33, CINZA, true);
        int bx = x + 68, bw = w - 73;
        g.fill(bx, y + 35, bx + bw, y + 39, 0xFF20242E);
        g.fill(bx, y + 35, bx + Math.round(bw * v.turbo), y + 39, v.turboLigado ? AZUL : v.turbo < 0.2f ? 0xFF5A6070 : 0xFF3A8FD0);
        // Metralhadoras: calor
        float calor = v.calor();
        g.text(f, Component.translatable("dcuniverse.batwing.metralhadoras"), x + 5, y + 44, CINZA, true);
        g.fill(bx, y + 46, bx + bw, y + 50, 0xFF20242E);
        int corCalor = calor > 0.8f ? VERMELHO : calor > 0.5f ? 0xFFFF9A3A : AMBAR;
        g.fill(bx, y + 46, bx + Math.round(bw * calor), y + 50, corCalor);
        // Munição da secundária
        g.text(f, Component.translatable("dcuniverse.veiculo.municao." + id), x + 5, y + 55, CINZA, true);
        int max = prm.i("rounds", 4);
        for (int i = 0; i < max; i++) {
            int mx = bx + i * Math.min(12, bw / Math.max(1, max)), my = y + 55;
            boolean tem = i < v.municao();
            g.fill(mx, my + 2, mx + 8, my + 6, tem ? 0xFFE8ECF4 : 0xFF2A2F3A);
            g.fill(mx + 8, my + 3, mx + 10, my + 5, tem ? (id.equals("batmoto") ? AMBAR : VERMELHO) : 0xFF2A2F3A);
            if (i == v.municao() && v.municao() < max) g.fill(mx, my + 7, mx + Math.round(8 * v.recarga()), my + 8, AMBAR);
        }
        // Especial e mira travada
        Component esp;
        int corEsp;
        if (id.equals("batbarco")) {
            esp = Component.translatable(v.submerso() ? "dcuniverse.veiculo.especial.batbarco.on" : "dcuniverse.veiculo.especial.batbarco.off");
            corEsp = v.submerso() ? AZUL : CINZA;
        } else if (v.especialEspera() > 0) {
            esp = Component.translatable("dcuniverse.veiculo.especial." + id + ".espera", (v.especialEspera() + 19) / 20);
            corEsp = 0xFF7F8898;
        } else {
            esp = Component.translatable("dcuniverse.veiculo.especial." + id + ".pronto");
            corEsp = VERDE;
        }
        g.text(f, esp, x + 5, y + 67, corEsp, true);
        if (v.alvoTravado() >= 0 && v.municao() > 0) {
            // Na linha da velocidade, à direita (o especial ocupa a linha de baixo inteira).
            boolean pisca = (mc.player.tickCount / 4) % 2 == 0;
            Component c = Component.translatable("dcuniverse.batwing.travado");
            g.fill(x + w - 7 - f.width(c), y + 20, x + w - 3, y + 30, 0xF0080A12);
            g.text(f, c, x + w - 5 - f.width(c), y + 21, pisca ? VERMELHO : 0xFFFFB0A0, true);
        }
        g.text(f, Component.translatable("dcuniverse.veiculo.dica1." + id), x + 5, y + 82, 0xFF7F8898, false);
        g.text(f, Component.translatable("dcuniverse.veiculo.dica2." + id), x + 5, y + 92, 0xFF7F8898, false);
    }

    // ------------------------------------------------------------------ sons

    private static SoundEvent somMotor(VeiculoBat v) {
        return switch (v.modelo()) {
            case "batmoto" -> Sons.BATMOTO_MOTOR.get();
            case "batbarco" -> Sons.BATBARCO_MOTOR.get();
            default -> Sons.BATMOVEL_MOTOR.get();
        };
    }

    /** Ronco do motor de um veículo: tom e volume sobem com a velocidade; dentro, fica com o ouvinte. */
    private static final class Motor extends AbstractTickableSoundInstance {
        private final VeiculoBat v;
        final boolean dentro;
        private float rpm;

        Motor(VeiculoBat v, boolean dentro) {
            super(somMotor(v), SoundSource.NEUTRAL, v.getRandom());
            this.v = v;
            this.dentro = dentro;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01f;
            if (dentro) {
                this.relative = true;
                this.attenuation = Attenuation.NONE;
            } else {
                this.x = v.getX();
                this.y = v.getY();
                this.z = v.getZ();
            }
        }

        @Override
        public void tick() {
            if (v.isRemoved()) {
                volume -= 0.1f;
                if (volume <= 0) stop();
                return;
            }
            double vel = v.position().subtract(v.xo, v.yo, v.zo).length();
            boolean moto = v.forma() == VeiculoBat.BATMOTO;
            // Rotação: sobe com a velocidade e "corta" um pouco a cada marcha (a moto tem mais marchas).
            float marcha = moto ? 0.42f : 0.5f;
            float alvoRpm = (float) (vel % marcha / marcha) * 0.35f + (float) Math.min(1, vel / 2.2) * 0.65f;
            rpm += (alvoRpm - rpm) * 0.25f;
            boolean sub = v.submerso();
            float alvo = (dentro ? 0.75f : 1f) * (0.45f + 0.55f * Math.min(1, (float) vel / 1.2f)) * (sub ? 0.5f : 1);
            if (v.turboLigado) alvo *= 1.2f;
            volume += (Math.min(1.2f, alvo) - volume) * 0.15f;
            pitch = Mth.clamp((moto ? 0.8f : 0.65f) + rpm * (moto ? 1.1f : 0.9f) + (sub ? -0.15f : 0), 0.5f, 2f);
            if (dentro) return;
            x = v.getX();
            y = v.getY();
            z = v.getZ();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }

    /** Cantada dos pneus do veículo pilotado, só enquanto derrapa. */
    private static final class Pneu extends AbstractTickableSoundInstance {
        final VeiculoBat v;

        Pneu(VeiculoBat v) {
            super(Sons.VEICULO_PNEU.get(), SoundSource.NEUTRAL, v.getRandom());
            this.v = v;
            this.looping = true;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
            this.volume = 0.01f;
        }

        @Override
        public void tick() {
            Minecraft mc = Minecraft.getInstance();
            if (v.isRemoved() || mc.player == null || mc.player.getVehicle() != v) {
                stop();
                return;
            }
            float alvo = v.derrapando ? 0.9f : 0f;
            volume += (alvo - volume) * 0.3f;
            pitch = 0.9f + Math.abs(v.velocidade) * 0.15f;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }
}
