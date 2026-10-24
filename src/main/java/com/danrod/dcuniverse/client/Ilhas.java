package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.content.IlhaTelecinetica;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.content.VeiculoDono;
import com.danrod.dcuniverse.core.EstadoCliente;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Ilha Telecinética no cliente: a pilotagem do dono (WASD anda para onde ele olha, pulo sobe, correr desce, com
 * inércia de coisa pesada; a ilha para ao encostar em terreno), levar junto o jogador local que está em pé em cima,
 * o painel do piloto e o ronco grave de cada ilha por perto.
 */
public final class Ilhas {
    private static final Map<Integer, Ronco> RONCOS = new HashMap<>();
    private static final int AZUL = 0xFF8FD0FF;

    private Ilhas() {}

    public static void registrar() {
        IlhaTelecinetica.cliente = Ilhas::tick;
    }

    public static void limpar() {
        RONCOS.clear();
    }

    /** Parâmetros da suprema do personagem do jogador local. */
    static Params params() {
        try {
            var json = JsonParser.parseString(EstadoCliente.catalogo.getOrDefault(EstadoCliente.atual.personagem(), "{}")).getAsJsonObject();
            if (json.has("abilities")) {
                for (var e : json.getAsJsonArray("abilities")) {
                    var o = e.getAsJsonObject();
                    if ("floating_island".equals(o.get("type").getAsString())) return new Params(o);
                }
            }
        } catch (RuntimeException ignorado) {
            // catálogo ainda não chegou
        }
        return Params.EMPTY;
    }

    private static void tick(IlhaTelecinetica i) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        if (i.isLocalInstanceAuthoritative() && p.getVehicle() == i) pilotar(mc, p, i);
        // Quem está em pé em cima anda junto (as partes ainda estão onde a ilha estava no tick anterior).
        Vec3 d = i.andou();
        if (!p.isPassenger() && d.lengthSqr() > 1e-8 && i.emCima(p)) {
            p.setPos(p.position().add(d));
            if (d.y > 0) p.setDeltaMovement(p.getDeltaMovement().multiply(1, 0, 1));
        }
        Ronco r = RONCOS.get(i.getId());
        if ((r == null || r.isStopped()) && i.carregada()) {
            r = new Ronco(i);
            RONCOS.put(i.getId(), r);
            mc.getSoundManager().play(r);
        }
        RONCOS.values().removeIf(Ronco::isStopped);
    }

    private static void pilotar(Minecraft mc, LocalPlayer p, IlhaTelecinetica i) {
        if (i.estado() != VeiculoDono.PILOTADO) return;
        Params prm = params();
        double max = prm.d("speed", 8) / 20.0;
        boolean livre = mc.gui.screen() == null;
        int frente = 0, lado = 0, vert = 0;
        if (livre) {
            frente = (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0);
            lado = (mc.options.keyLeft.isDown() ? 1 : 0) - (mc.options.keyRight.isDown() ? 1 : 0);
            vert = (mc.options.keyJump.isDown() ? 1 : 0) - (mc.options.keySprint.isDown() ? 1 : 0);
        }
        float yaw = p.getYRot() * Mth.DEG_TO_RAD;
        Vec3 f = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw)), e = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
        Vec3 alvo = f.scale(frente).add(e.scale(lado));
        if (alvo.lengthSqr() > 1e-6) alvo = alvo.normalize().scale(max);
        alvo = alvo.add(0, vert * max * 0.7, 0);
        // Coisa pesada: ganha e perde velocidade devagar.
        Vec3 v = i.velPiloto.lerp(alvo, alvo.lengthSqr() > i.velPiloto.lengthSqr() ? 0.06 : 0.1);
        double mx = livre(i, v.x, 0, 0) ? v.x : 0;
        double my = livre(i, mx, v.y, 0) ? v.y : 0;
        double mz = livre(i, mx, my, v.z) ? v.z : 0;
        v = new Vec3(mx, my, mz);
        i.velPiloto = v;
        i.setDeltaMovement(v);
        i.setPos(i.position().add(v));
    }

    /** A ilha cabe deslocada de (dx, dy, dz)? Confere só as faces de fora dos blocos na direção do movimento. */
    private static boolean livre(IlhaTelecinetica i, double dx, double dy, double dz) {
        if (dx == 0 && dy == 0 && dz == 0) return true;
        var level = i.level();
        Vec3 o = i.position().add(dx, dy, dz);
        int sx = (int) Math.signum(dx), sy = (int) Math.signum(dy), sz = (int) Math.signum(dz);
        for (int[] c : i.celulas()) {
            if (!i.bloco(c[0], c[1], c[2]).blocksMotion()) continue;
            boolean exposta = sx != 0 && !i.bloco(c[0] + sx, c[1], c[2]).blocksMotion()
                || sy != 0 && !i.bloco(c[0], c[1] + sy, c[2]).blocksMotion()
                || sz != 0 && !i.bloco(c[0], c[1], c[2] + sz).blocksMotion();
            if (!exposta) continue;
            double x0 = o.x + c[0] - 0.5 + 0.02, y0 = o.y + c[1] + 0.02, z0 = o.z + c[2] - 0.5 + 0.02;
            for (BlockPos b : BlockPos.betweenClosed(BlockPos.containing(x0, y0, z0), BlockPos.containing(x0 + 0.96, y0 + 0.96, z0 + 0.96))) {
                if (!level.getBlockState(b).getCollisionShape(level, b).isEmpty()) return false;
            }
        }
        return true;
    }

    /** Até onde a câmera de terceira pessoa pode recuar sem entrar nos blocos de uma ilha por perto. */
    public static float limitarCamera(Vec3 de, Vec3 frente, float dist) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || dist <= 0) return dist;
        Vec3 ate = de.subtract(frente.scale(dist));
        net.minecraft.world.phys.AABB raio = new net.minecraft.world.phys.AABB(de, ate).inflate(1);
        float melhor = dist;
        for (IlhaTelecinetica i : mc.level.getEntitiesOfClass(IlhaTelecinetica.class, raio.inflate(24))) {
            if (!i.caixaBlocos().intersects(raio)) continue;
            for (var caixa : i.caixasPartes()) {
                var acerto = caixa.inflate(0.15).clip(de, ate);
                // Já dentro da caixa (o jogador em pé encostado): não conta.
                if (acerto.isPresent() && !caixa.contains(de)) melhor = Math.min(melhor, (float) acerto.get().distanceTo(de));
            }
        }
        return melhor;
    }

    /** Painel do piloto no canto de baixo à direita: tempo que falta e os comandos. */
    public static void desenhar(GuiGraphicsExtractor g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.hud.isHidden() || !(mc.player.getVehicle() instanceof IlhaTelecinetica i)) return;
        Font f = mc.font;
        int w = 168, h = 58, x = g.guiWidth() - w - 6, y = g.guiHeight() - h - 30;
        g.fill(x, y, x + w, y + h, 0xB0080A12);
        g.fill(x, y, x + w, y + 1, AZUL);
        g.text(f, Component.translatable("habilidade.dcuniverse.ilha_telecinetica").withStyle(ChatFormatting.BOLD), x + 5, y + 4, AZUL, true);
        int total = Math.round(params().f("duration", 150) * 20);
        float frac = Mth.clamp(i.restante() / (float) Math.max(1, total), 0, 1);
        String seg = (i.restante() + 19) / 20 + " s";
        g.text(f, seg, x + w - 5 - f.width(seg), y + 4, frac < 0.1f ? 0xFFFF5A4A : 0xFFE8ECF4, true);
        g.fill(x + 5, y + 14, x + w - 5, y + 16, 0xFF20242E);
        g.fill(x + 5, y + 14, x + 5 + Math.round((w - 10) * frac), y + 16, frac < 0.1f ? 0xFFFF5A4A : AZUL);
        g.text(f, Component.translatable("dcuniverse.ilha.altitude", (int) Math.floor(i.getY())), x + 5, y + 21, 0xFFE8ECF4, true);
        g.text(f, Component.translatable("dcuniverse.ilha.dica1"), x + 5, y + 34, 0xFF7F8898, false);
        g.text(f, Component.translatable("dcuniverse.ilha.dica2", Teclas.USAR.getTranslatedKeyMessage()), x + 5, y + 45, 0xFF7F8898, false);
    }

    /** Zumbido grave da ilha flutuando: sobe de tom e de volume quando ela anda. */
    private static final class Ronco extends AbstractTickableSoundInstance {
        private final IlhaTelecinetica i;

        Ronco(IlhaTelecinetica i) {
            super(Sons.ILHA_MOTOR.get(), SoundSource.NEUTRAL, i.getRandom());
            this.i = i;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01f;
            this.x = i.getX();
            this.y = i.getY();
            this.z = i.getZ();
        }

        @Override
        public void tick() {
            if (i.isRemoved()) {
                volume -= 0.08f;
                if (volume <= 0) stop();
                return;
            }
            double v = i.position().subtract(i.xo, i.yo, i.zo).length();
            float alvo = (float) Mth.clamp(0.45 + v * 2.2, 0.45, 1.0);
            volume += (alvo - volume) * 0.1f;
            pitch = (float) Mth.clamp(0.8 + v * 0.8, 0.8, 1.2);
            Entity cam = Minecraft.getInstance().getCameraEntity();
            // Fica no ponto da ilha mais perto de quem ouve (ela é grande).
            Vec3 perto = cam == null ? i.position() : clamp(cam.position(), i.caixaBlocos());
            x = perto.x;
            y = perto.y;
            z = perto.z;
        }

        private static Vec3 clamp(Vec3 p, net.minecraft.world.phys.AABB c) {
            return new Vec3(Mth.clamp(p.x, c.minX, c.maxX), Mth.clamp(p.y, c.minY, c.maxY), Mth.clamp(p.z, c.minZ, c.maxZ));
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }
}
