package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.core.Rede;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * A bolha do escudo telecinético (ability/active/EscudoTk) em volta de quem a ergueu: uma esfera de luz azulada
 * mais forte na borda (como uma bolha de sabão vista de lado), com faixas de força correndo devagar pela superfície.
 * Um golpe na bolha acende uma onda que se espalha a partir do lado atingido. Some encolhendo no fim.
 */
public final class Bolha {
    private static final class Ativa {
        long ate, golpe = -100, fim = -1;
        float raio;
        Vec3 deOnde = Vec3.ZERO;
    }

    private static final Map<Integer, Ativa> ATIVAS = new HashMap<>();
    private static final int LADOS = 28, ANEIS = 16;

    private Bolha() {}

    public static void efeito(Minecraft mc, Rede.Efeito e) {
        if (mc.level == null) return;
        long agora = mc.level.getGameTime();
        switch (e.tipo()) {
            case "escudo_tk" -> {
                Ativa a = new Ativa();
                a.ate = agora + Math.round(e.a());
                a.raio = e.b();
                ATIVAS.put(e.cor(), a);
            }
            case "escudo_tk_golpe" -> {
                Ativa a = ATIVAS.get(e.cor());
                if (a != null) {
                    a.golpe = agora;
                    // O ponto de onde veio o golpe não chega no pacote: a onda nasce do lado da câmera.
                    a.deOnde = mc.gameRenderer.mainCamera().position().subtract(e.x(), e.y() + 1, e.z()).normalize();
                }
            }
            case "escudo_tk_fim" -> {
                Ativa a = ATIVAS.get(e.cor());
                if (a != null && a.fim < 0) a.fim = agora;
            }
            default -> {}
        }
    }

    public static void tick(ClientLevel level) {
        long agora = level.getGameTime();
        ATIVAS.entrySet().removeIf(en -> {
            Ativa a = en.getValue();
            if (a.fim < 0 && agora > a.ate) a.fim = agora;
            if (a.fim >= 0 && agora - a.fim > 8) return true;
            Entity e = level.getEntity(en.getKey());
            if (e == null) return true;
            var rnd = level.getRandom();
            if (rnd.nextInt(3) == 0) {
                Vec3 d = new Vec3(rnd.nextGaussian(), rnd.nextGaussian(), rnd.nextGaussian()).normalize().scale(a.raio);
                level.addParticle(new DustParticleOptions(0xA8DCFF, 0.6f), e.getX() + d.x, e.getY() + e.getBbHeight() / 2 + d.y, e.getZ() + d.z, 0, 0, 0);
            }
            return false;
        });
    }

    public static void limpar() {
        ATIVAS.clear();
    }

    public static void desenhar(PoseStack ps, SubmitNodeCollector col, Vec3 cam, float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || ATIVAS.isEmpty()) return;
        long agora = mc.level.getGameTime();
        float tempo = agora + pt;
        for (var en : ATIVAS.entrySet()) {
            Entity e = mc.level.getEntity(en.getKey());
            if (e == null) continue;
            Ativa a = en.getValue();
            float nasce = Mth.clamp((tempo - (a.ate - 99999)) / 6f, 0, 1);
            float some = a.fim < 0 ? 1 : Mth.clamp(1 - (tempo - a.fim) / 8f, 0, 1);
            float golpe = Mth.clamp(1 - (tempo - a.golpe) / 10f, 0, 1);
            float r = a.raio * (a.fim < 0 ? 1 : 0.6f + 0.4f * some);
            Vec3 pos = e.getPosition(pt).add(0, e.getBbHeight() / 2, 0);
            Vec3 c = pos.subtract(cam);
            boolean dentro = cam.distanceTo(pos) < r;
            float forca = some * nasce;
            Vec3 olho = cam.subtract(pos);
            col.submitCustomGeometry(ps, Luz.tipo(), (pose, buf) -> esfera(pose.pose(), buf, c, r, olho, tempo, forca, golpe, a.deOnde, dentro));
        }
    }

    private static void esfera(Matrix4f m, VertexConsumer buf, Vec3 c, float r, Vec3 olho, float t, float forca, float golpe, Vec3 deOnde, boolean dentro) {
        for (int i = 0; i < ANEIS; i++) {
            for (int j = 0; j < LADOS; j++) {
                float[][] q = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
                for (int k = 0; k < 4; k++) {
                    float lat = Mth.PI * q[k][0] / ANEIS - Mth.HALF_PI, lon = Mth.TWO_PI * q[k][1] / LADOS;
                    float x = Mth.cos(lat) * Mth.cos(lon), y = Mth.sin(lat), z = Mth.cos(lat) * Mth.sin(lon);
                    Vec3 n = new Vec3(x, y, z);
                    Vec3 v = olho.subtract(n.scale(r)).normalize();
                    // Borda mais forte (fresnel), faixas de força correndo e a onda do golpe.
                    double borda = Math.pow(1 - Math.abs(n.dot(v)), 2.2);
                    double faixas = 0.5 + 0.5 * Math.sin(lat * 9 + t * 0.12) * Math.sin(lon * 7 - t * 0.08);
                    double onda = golpe > 0 ? golpe * Math.max(0, Math.cos((Math.acos(Mth.clamp((float) n.dot(deOnde), -1, 1)) - (1 - golpe) * 2.6) * 3)) : 0;
                    float alfa = (float) ((0.05 + 0.32 * borda + 0.08 * faixas * faixas + 0.5 * onda) * forca * (dentro ? 0.5 : 1));
                    buf.addVertex(m, (float) (c.x + x * r), (float) (c.y + y * r), (float) (c.z + z * r))
                        .setColor(0.55f + 0.3f * (float) onda, 0.8f, 1f, Mth.clamp(alfa, 0, 1));
                }
            }
        }
    }
}
