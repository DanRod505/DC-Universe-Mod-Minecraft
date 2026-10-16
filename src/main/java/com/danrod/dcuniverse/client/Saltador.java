package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.network.PacketDistributor;

/**
 * Super-salto (passiva super_jump) no cliente: no chão, segurando agachar e pulo o salto carrega (o pulo
 * normal fica segurado); ao soltar o pulo ele sai com até "power" blocos/tick para cima, um pouco para a
 * frente se estiver andando. Barrinha de carga embaixo da mira, poeira nos pés e som subindo enquanto carrega.
 */
public final class Saltador {
    private static boolean carregando;
    private static float carga;
    private static SimpleSoundInstance som;

    private Saltador() {}

    public static void registrar() {
        MovementInputUpdateEvent.BUS.addListener(e -> {
            if (e.getEntity() instanceof LocalPlayer p) entrada(p, e.getInput().keyPresses, e.getInput());
        });
    }

    private static void entrada(LocalPlayer p, Input in, net.minecraft.client.player.ClientInput ci) {
        Params prm = EstadoCliente.passiva("super_jump");
        if (prm == null || Voador.voando(p) || p.isSpectator() || p.isPassenger() || p.isInWater()) {
            cancelar();
            return;
        }
        if (carregando) {
            if (!in.jump()) {
                soltar(p, prm);
            } else if (!in.shift() || !p.onGround()) {
                cancelar();
            } else {
                carga = Math.min(1, carga + 1f / Math.max(1, prm.f("charge", 1) * 20));
                ci.keyPresses = in.jump(false);
                poeira(p);
            }
        } else if (in.jump() && in.shift() && p.onGround()) {
            carregando = true;
            carga = 0;
            ci.keyPresses = in.jump(false);
            som = new SimpleSoundInstance(Sons.SALTO_CARGA.get(), SoundSource.PLAYERS, 0.8f, 1f, p.getRandom(), p.getX(), p.getY(), p.getZ());
            Minecraft.getInstance().getSoundManager().play(som);
        }
    }

    private static void soltar(LocalPlayer p, Params prm) {
        float f = carga;
        cancelar();
        if (f < 0.15f) return;
        double v = Math.max(0.42, prm.d("power", 2) * f);
        Vec3 olhar = p.getLookAngle();
        Vec3 frente = new Vec3(olhar.x, 0, olhar.z);
        double h = p.input.hasForwardImpulse() && frente.lengthSqr() > 1e-4 ? 0.7 * f : 0;
        frente = h > 0 ? frente.normalize().scale(h) : Vec3.ZERO;
        Vec3 dm = p.getDeltaMovement();
        p.setDeltaMovement(dm.x + frente.x, v, dm.z + frente.z);
        p.setOnGround(false);
        Rede.CANAL.send(new Rede.Salto(f), PacketDistributor.SERVER.noArg());
    }

    private static void cancelar() {
        if (som != null) Minecraft.getInstance().getSoundManager().stop(som);
        som = null;
        carregando = false;
        carga = 0;
    }

    public static void limpar() {
        cancelar();
    }

    private static void poeira(LocalPlayer p) {
        if (p.tickCount % 2 != 0) return;
        var rnd = p.getRandom();
        int n = 1 + Math.round(carga * 4);
        for (int i = 0; i < n; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2, r = 0.5 + rnd.nextDouble() * 0.4;
            p.level().addParticle(new DustParticleOptions(0xD9D2C4, 0.9f), p.getX() + Math.cos(ang) * r, p.getY() + 0.05,
                p.getZ() + Math.sin(ang) * r, -Math.cos(ang) * 0.04, 0.03 + carga * 0.05, -Math.sin(ang) * 0.04);
        }
    }

    /** Barrinha de carga embaixo da mira (fica dourada com a carga cheia). */
    public static void desenhar(GuiGraphicsExtractor g, DeltaTracker dt) {
        if (!carregando || carga <= 0) return;
        int w = 40, h = 3, x = g.guiWidth() / 2 - w / 2, y = g.guiHeight() / 2 + 12;
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, ARGB.color(150, 0x000000));
        int cheio = Mth.floor(w * carga);
        int cor = carga >= 1 ? 0xFFD54A : 0xFFFFFF;
        g.fill(x, y, x + cheio, y + h, ARGB.color(230, cor));
    }
}
