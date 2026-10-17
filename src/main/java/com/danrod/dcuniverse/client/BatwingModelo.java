package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.content.BatwingEntidade;
import org.joml.Vector3f;

/**
 * Geometria do Batwing, em blocos, com o nariz para +z e a asa esquerda do piloto em +x. A asa é um corpo único
 * em forma de morcego: estações ao longo da envergadura, cada uma com um perfil de asa entre o bordo de ataque
 * (curva lisa até a ponta em garra) e o bordo de fuga recortado em três pontas, mais grosso no meio (a fuselagem)
 * e afinando até a ponta. Por cima: cabine de vidro, duas derivas, as "orelhas" no nariz e os dois bocais.
 * Embaixo: metralhadoras e os quatro mísseis nos pilones. Textura em textures/entity/batwing.png (256x128,
 * tools/arte/batwing.py): de cima em (0,0)-(128,92), de baixo em (128,0)-(256,92), peças na faixa de baixo.
 */
public final class BatwingModelo {
    /** Altura do plano da asa acima dos pés da entidade. */
    public static final float Y0 = BatwingEntidade.PIVO;
    public static final float MEIA_ENVERGADURA = 3.45f;
    private static final float TW = 256, TH = 128;

    public static final Malha CASCO = new Malha();
    public static final Malha VIDRO = new Malha();
    public static final Malha MOTOR = new Malha();
    public static final Malha LUZ_ESQ = new Malha(), LUZ_DIR = new Malha(), LUZ_CAUDA = new Malha(), PAINEL = new Malha();
    /** Os quatro mísseis nos pilones, na ordem em que sobram (o 1 é o último a sair). */
    public static final Malha[] MISSEIS = {new Malha(), new Malha(), new Malha(), new Malha()};
    /** Míssil sozinho, com o nariz em +z (para o míssil disparado). */
    public static final Malha MISSIL = new Malha();
    public static final Malha CHAMA = new Malha();

    // Bordo de ataque e de fuga (x, z), do meio para a ponta.
    private static final float[][] ATAQUE = {{0, 2.35f}, {0.18f, 2.05f}, {0.36f, 1.62f}, {0.55f, 1.05f}, {0.9f, 0.86f}, {1.5f, 0.74f},
        {2.2f, 0.6f}, {2.8f, 0.42f}, {3.2f, 0.26f}, {3.45f, 0.08f}};
    private static final float[][] FUGA = {{0, -1.2f}, {0.42f, -1.85f}, {0.82f, -0.98f}, {1.3f, -1.45f}, {1.72f, -0.62f},
        {2.2f, -1.08f}, {2.62f, -0.32f}, {3.05f, -0.52f}, {3.45f, 0.06f}};

    private BatwingModelo() {}

    static {
        asa();
        cabine();
        derivas();
        orelhas();
        bocais();
        armas();
        luzes();
        for (int k = 0; k < 4; k++) {
            float lado = k % 2 == 0 ? 1 : -1;  // 1 e 3 à esquerda (+x), 2 e 4 à direita
            float x = lado * (k < 2 ? 1.85f : 2.3f);
            missil(MISSEIS[k], x, Y0 - (k < 2 ? 0.17f : 0.2f), k < 2 ? 0.18f : 0.02f);
            pilone(x, k < 2 ? 0.18f : 0.02f);
        }
        missil(MISSIL, 0, 0, 0);
        chama();
    }

    private static float interp(float[][] pts, float x) {
        for (int i = 0; i + 1 < pts.length; i++) {
            if (x <= pts[i + 1][0]) {
                float t = (x - pts[i][0]) / (pts[i + 1][0] - pts[i][0]);
                return pts[i][1] + (pts[i + 1][1] - pts[i][1]) * t;
            }
        }
        return pts[pts.length - 1][1];
    }

    /** Meia espessura do perfil na estação x (fuselagem grossa no meio). */
    static float espessura(float x) {
        float t = Math.min(1, x / MEIA_ENVERGADURA);
        float e = 0.16f * (float) Math.pow(1 - t, 1.3) + 0.02f;
        if (x < 0.7f) e += 0.26f * (1 - (x / 0.7f) * (x / 0.7f));
        return e;
    }

    /** Altura do plano médio na estação x: as asas caem um pouco para as pontas. */
    static float meio(float x) {
        return Y0 - 0.055f * x;
    }

    /** Altura de cima do corpo no ponto (x, z) aproximado (para apoiar as peças). */
    static float topo(float x, float z) {
        float le = interp(ATAQUE, Math.abs(x)), te = interp(FUGA, Math.abs(x));
        float u = Math.max(0, Math.min(1, (z - te) / (le - te)));
        return meio(Math.abs(x)) + espessura(Math.abs(x)) * perfil(u);
    }

    /** Forma do perfil ao longo da corda (0 = bordo de fuga, 1 = bordo de ataque); mais cheio na frente. */
    private static float perfil(float u) {
        float w = (float) Math.pow(u, 0.7);
        return (float) Math.pow(Math.sin(Math.PI * w), 0.65);
    }

    private static void asa() {
        // Estações: os cantos dos dois bordos mais uma divisão regular.
        java.util.TreeSet<Float> xs = new java.util.TreeSet<>();
        for (float[] p : ATAQUE) xs.add(p[0]);
        for (float[] p : FUGA) xs.add(p[0]);
        for (float x = 0; x < MEIA_ENVERGADURA; x += 0.12f) xs.add(x);
        Float[] est = xs.toArray(new Float[0]);
        int nc = 12;
        int ne = est.length;
        // Pontos de cima e de baixo [estação][corda].
        Vector3f[][] cima = new Vector3f[ne][nc + 1], baixo = new Vector3f[ne][nc + 1];
        for (int i = 0; i < ne; i++) {
            float x = est[i], le = interp(ATAQUE, x), te = interp(FUGA, x), e = espessura(x), m = meio(x);
            for (int j = 0; j <= nc; j++) {
                // mais pontos perto dos bordos (cosseno)
                float u = 0.5f - 0.5f * (float) Math.cos(Math.PI * j / nc);
                float z = te + (le - te) * u;
                float p = perfil(u);
                cima[i][j] = new Vector3f(x, m + e * p, z);
                baixo[i][j] = new Vector3f(x, m - e * 0.38f * p, z);
            }
        }
        for (int lado = -1; lado <= 1; lado += 2) {
            superficie(cima, ne, nc, lado, true);
            superficie(baixo, ne, nc, lado, false);
        }
    }

    private static Vector3f esp(Vector3f p, int lado) {
        return new Vector3f(p.x * lado, p.y, p.z);
    }

    private static float uCima(Vector3f p, boolean deCima) {
        return ((p.x + 3.6f) / 7.2f * 128f + (deCima ? 0 : 128)) / TW;
    }

    private static float vCima(Vector3f p) {
        return (2.45f - p.z) / 4.6f * 92f / TH;
    }

    /** Superfície em grade com normais lisas (média das faces vizinhas). */
    private static void superficie(Vector3f[][] g, int ne, int nc, int lado, boolean deCima) {
        Vector3f[][] n = new Vector3f[ne][nc + 1];
        for (int i = 0; i < ne; i++) for (int j = 0; j <= nc; j++) n[i][j] = new Vector3f();
        for (int i = 0; i + 1 < ne; i++) {
            for (int j = 0; j < nc; j++) {
                Vector3f a = esp(g[i][j], lado), b = esp(g[i + 1][j], lado), c = esp(g[i + 1][j + 1], lado);
                Vector3f f = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a));
                if ((f.y < 0) == deCima) f.negate();
                for (int[] k : new int[][] {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}}) n[k[0]][k[1]].add(f);
            }
        }
        for (int i = 0; i + 1 < ne; i++) {
            for (int j = 0; j < nc; j++) {
                Malha.V a = v(g[i][j], n[i][j], lado, deCima), b = v(g[i + 1][j], n[i + 1][j], lado, deCima);
                Malha.V c = v(g[i + 1][j + 1], n[i + 1][j + 1], lado, deCima), d = v(g[i][j + 1], n[i][j + 1], lado, deCima);
                CASCO.tri(a, b, c).tri(a, c, d);
            }
        }
    }

    private static Malha.V v(Vector3f p, Vector3f n, int lado, boolean deCima) {
        Vector3f q = esp(p, lado);
        Vector3f nn = new Vector3f(n);
        if (nn.lengthSquared() < 1e-10f) nn.set(0, deCima ? 1 : -1, 0);
        nn.normalize();
        return new Malha.V(q.x, q.y, q.z, uCima(q, deCima), vCima(q), nn.x, nn.y, nn.z);
    }

    // ------------------------------------------------------------------ peças

    /** UV de uma área da faixa de peças (em pixels da textura). */
    private static float pu(float px) {
        return px / TW;
    }

    private static float pv(float py) {
        return py / TH;
    }

    private static void cabine() {
        // Meio elipsoide de vidro afundado no dorso, um pouco à frente do centro.
        float cx = 0, cz = 0.8f, rx = 0.38f, ry = 0.44f, rz = 0.9f;
        float cy = topo(0, cz) - 0.06f;
        int nl = 10, na = 16;
        Vector3f[][] p = new Vector3f[nl + 1][na + 1];
        for (int i = 0; i <= nl; i++) {
            double lat = Math.PI / 2 * i / nl;  // 0 = base, pi/2 = topo
            for (int j = 0; j <= na; j++) {
                double lon = Math.PI * 2 * j / na;
                p[i][j] = new Vector3f(cx + (float) (rx * Math.cos(lat) * Math.sin(lon)), cy + (float) (ry * Math.sin(lat)),
                    cz + (float) (rz * Math.cos(lat) * Math.cos(lon)));
            }
        }
        for (int i = 0; i < nl; i++) {
            for (int j = 0; j < na; j++) {
                Vector3f a = p[i][j], b = p[i][j + 1], c = p[i + 1][j + 1], d = p[i + 1][j];
                float u0 = pu(96 + 32f * j / na), u1 = pu(96 + 32f * (j + 1) / na), v0 = pv(92 + 36f * i / nl), v1 = pv(92 + 36f * (i + 1) / nl);
                VIDRO.tri(a, u0, v0, d, u0, v1, c, u1, v1).tri(a, u0, v0, c, u1, v1, b, u1, v0);
            }
        }
        // Painel aceso dentro da cabine (visto pelo vidro): uma placa inclinada à frente do piloto.
        float yp = cy + 0.06f;
        PAINEL.quad(new Vector3f(-0.22f, yp, 1.25f), new Vector3f(0.22f, yp, 1.25f), new Vector3f(0.22f, yp + 0.12f, 1.12f),
            new Vector3f(-0.22f, yp + 0.12f, 1.12f), pu(40), pv(0), pu(56), pv(8));
    }

    private static void derivas() {
        for (int lado = -1; lado <= 1; lado += 2) {
            float x = 0.5f * lado;
            float yb = topo(0.5f, -0.9f) - 0.04f;
            Vector3f bf = new Vector3f(x, yb, -0.5f), bt = new Vector3f(x, yb, -1.45f);
            Vector3f tt = new Vector3f(x + 0.14f * lado, yb + 0.62f, -1.78f), tf = new Vector3f(x + 0.12f * lado, yb + 0.56f, -1.3f);
            placa(bf, bt, tt, tf, 0.035f, 0, 92, 40, 128);
        }
    }

    /** Placa fina (dois lados e as bordas) a partir de um quadrilátero no plano yz deslocado em x. */
    private static void placa(Vector3f a, Vector3f b, Vector3f c, Vector3f d, float meia, float px0, float py0, float px1, float py1) {
        Vector3f off = new Vector3f(b).sub(a).cross(new Vector3f(d).sub(a)).normalize().mul(meia);
        Vector3f a1 = new Vector3f(a).add(off), b1 = new Vector3f(b).add(off), c1 = new Vector3f(c).add(off), d1 = new Vector3f(d).add(off);
        Vector3f a2 = new Vector3f(a).sub(off), b2 = new Vector3f(b).sub(off), c2 = new Vector3f(c).sub(off), d2 = new Vector3f(d).sub(off);
        float u0 = pu(px0), u1 = pu(px1), v0 = pv(py0), v1 = pv(py1);
        CASCO.quad(a1, b1, c1, d1, u0, v0, u1, v1);
        CASCO.quad(b2, a2, d2, c2, u0, v0, u1, v1);
        float ue = pu(px0 + 2);
        CASCO.quad(d1, c1, c2, d2, u0, v0, ue, v1);
        CASCO.quad(c1, b1, b2, c2, u0, v0, ue, v1);
        CASCO.quad(a1, d1, d2, a2, u0, v0, ue, v1);
    }

    private static void orelhas() {
        for (int lado = -1; lado <= 1; lado += 2) {
            float x = 0.17f * lado;
            float y = topo(0.17f, 1.9f) - 0.02f;
            Vector3f f = new Vector3f(x, y, 2.02f), t = new Vector3f(x, y, 1.62f), pico = new Vector3f(x + 0.03f * lado, y + 0.2f, 1.72f);
            Vector3f fl = new Vector3f(x - 0.06f, y, 1.85f), fr = new Vector3f(x + 0.06f, y, 1.85f);
            float u0 = pu(56), v0 = pv(0), u1 = pu(64), v1 = pv(8);
            CASCO.tri(f, u0, v1, fr, u1, v1, pico, u1, v0).tri(fr, u0, v1, t, u1, v1, pico, u1, v0)
                .tri(t, u0, v1, fl, u1, v1, pico, u1, v0).tri(fl, u0, v1, f, u1, v1, pico, u1, v0);
        }
    }

    private static void bocais() {
        for (int lado = -1; lado <= 1; lado += 2) {
            float x = 0.24f * lado, y = Y0 + 0.14f;
            tubo(CASCO, x, y, -0.9f, -1.38f, 0.14f, 0.12f, 64, 92, 96, 128);
            // Fundo aceso do bocal.
            disco(MOTOR, x, y, -1.36f, 0.1f, 192, 92, 224, 124);
        }
    }

    /** Tubo octogonal ao longo de z, de z0 (frente) a z1 (trás), raio r0 na frente e r1 atrás. */
    private static void tubo(Malha m, float x, float y, float z0, float z1, float r0, float r1, float px0, float py0, float px1, float py1) {
        int n = 10;
        for (int k = 0; k < n; k++) {
            double a0 = Math.PI * 2 * k / n, a1 = Math.PI * 2 * (k + 1) / n;
            Vector3f p0 = new Vector3f(x + r0 * (float) Math.cos(a0), y + r0 * (float) Math.sin(a0), z0);
            Vector3f p1 = new Vector3f(x + r0 * (float) Math.cos(a1), y + r0 * (float) Math.sin(a1), z0);
            Vector3f q1 = new Vector3f(x + r1 * (float) Math.cos(a1), y + r1 * (float) Math.sin(a1), z1);
            Vector3f q0 = new Vector3f(x + r1 * (float) Math.cos(a0), y + r1 * (float) Math.sin(a0), z1);
            float u0 = pu(px0 + (px1 - px0) * k / n), u1 = pu(px0 + (px1 - px0) * (k + 1) / n);
            m.quad(p0, q0, q1, p1, u0, pv(py0), u1, pv(py1));
        }
    }

    private static void disco(Malha m, float x, float y, float z, float r, float px0, float py0, float px1, float py1) {
        int n = 10;
        Vector3f c = new Vector3f(x, y, z);
        float cu = pu((px0 + px1) / 2), cv = pv((py0 + py1) / 2), ru = pu((px1 - px0) / 2), rv = pv((py1 - py0) / 2);
        for (int k = 0; k < n; k++) {
            double a0 = Math.PI * 2 * k / n, a1 = Math.PI * 2 * (k + 1) / n;
            Vector3f p0 = new Vector3f(x + r * (float) Math.cos(a0), y + r * (float) Math.sin(a0), z);
            Vector3f p1 = new Vector3f(x + r * (float) Math.cos(a1), y + r * (float) Math.sin(a1), z);
            m.tri(c, cu, cv, p1, cu + ru * (float) Math.cos(a1), cv + rv * (float) Math.sin(a1),
                p0, cu + ru * (float) Math.cos(a0), cv + rv * (float) Math.sin(a0));
        }
    }

    private static void armas() {
        for (int lado = -1; lado <= 1; lado += 2) {
            float x = 1.15f * lado, y = meio(1.15f) - 0.1f;
            CASCO.caixa(x - 0.07f, y - 0.06f, 0.45f, x + 0.07f, y + 0.04f, 1.05f, pu(64), pv(92), pu(80), pv(100));
            tubo(CASCO, x - 0.03f, y - 0.01f, 1.7f, 0.95f, 0.025f, 0.025f, 80, 92, 96, 100);
            tubo(CASCO, x + 0.03f, y - 0.01f, 1.7f, 0.95f, 0.025f, 0.025f, 80, 92, 96, 100);
        }
    }

    private static void pilone(float x, float z) {
        float y = meio(Math.abs(x)) - 0.02f;
        CASCO.caixa(x - 0.02f, y - 0.12f, z - 0.25f, x + 0.02f, y, z + 0.2f, pu(64), pv(100), pu(72), pv(108));
    }

    private static void luzes() {
        float yp = meio(MEIA_ENVERGADURA - 0.08f) + 0.02f;
        LUZ_ESQ.caixa(3.3f, yp - 0.04f, 0.08f, 3.42f, yp + 0.04f, 0.2f, pu(224), pv(92), pu(232), pv(100));
        LUZ_DIR.caixa(-3.42f, yp - 0.04f, 0.08f, -3.3f, yp + 0.04f, 0.2f, pu(232), pv(92), pu(240), pv(100));
        float yt = topo(0.5f, -0.9f) + 0.6f;
        LUZ_CAUDA.caixa(0.6f, yt - 0.04f, -1.8f, 0.7f, yt + 0.04f, -1.7f, pu(240), pv(92), pu(248), pv(100));
        LUZ_CAUDA.caixa(-0.7f, yt - 0.04f, -1.8f, -0.6f, yt + 0.04f, -1.7f, pu(240), pv(92), pu(248), pv(100));
        // Filete azul no bordo de ataque, perto da raiz (só de cima).
        for (int lado = -1; lado <= 1; lado += 2) {
            for (float x = 0.7f; x < 2.6f; x += 0.3f) {
                float z = interp(ATAQUE, x) - 0.07f, z2 = interp(ATAQUE, x + 0.3f) - 0.07f;
                float y = topo(x, z) + 0.006f, y2 = topo(x + 0.3f, z2) + 0.006f;
                Vector3f a = new Vector3f(x * lado, y, z), b = new Vector3f((x + 0.3f) * lado, y2, z2);
                Vector3f c = new Vector3f((x + 0.3f) * lado, y2, z2 - 0.035f), d = new Vector3f(x * lado, y, z - 0.035f);
                if (lado < 0) MOTOR.quad(b, a, d, c, pu(248), pv(92), pu(256), pv(100));
                else MOTOR.quad(a, b, c, d, pu(248), pv(92), pu(256), pv(100));
            }
        }
    }

    private static void missil(Malha m, float x, float y, float z) {
        float r = 0.07f;
        tubo(m, x, y, z + 0.3f, z - 0.42f, r, r, 128, 92, 176, 104);
        // ogiva
        int n = 10;
        Vector3f ponta = new Vector3f(x, y, z + 0.52f);
        for (int k = 0; k < n; k++) {
            double a0 = Math.PI * 2 * k / n, a1 = Math.PI * 2 * (k + 1) / n;
            Vector3f p0 = new Vector3f(x + r * (float) Math.cos(a0), y + r * (float) Math.sin(a0), z + 0.3f);
            Vector3f p1 = new Vector3f(x + r * (float) Math.cos(a1), y + r * (float) Math.sin(a1), z + 0.3f);
            m.tri(p1, pu(176), pv(104), p0, pu(184), pv(104), ponta, pu(180), pv(92));
        }
        // aletas em cruz
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 4 + Math.PI / 2 * k;
            float dx = (float) Math.cos(a), dy = (float) Math.sin(a);
            Vector3f a0 = new Vector3f(x + dx * r, y + dy * r, z - 0.22f), b0 = new Vector3f(x + dx * r, y + dy * r, z - 0.42f);
            Vector3f c0 = new Vector3f(x + dx * (r + 0.1f), y + dy * (r + 0.1f), z - 0.44f);
            m.tri(a0, pu(184), pv(92), b0, pu(192), pv(92), c0, pu(192), pv(100));
            m.tri(a0, pu(184), pv(92), c0, pu(192), pv(100), b0, pu(192), pv(92));
        }
    }

    private static void chama() {
        // Chama do míssil: dois planos cruzados atrás do motor (desenhados com brilho).
        float z0 = -0.42f, z1 = -1.1f;
        CHAMA.quad(new Vector3f(-0.08f, 0, z0), new Vector3f(0.08f, 0, z0), new Vector3f(0.02f, 0, z1), new Vector3f(-0.02f, 0, z1),
            pu(224), pv(100), pu(240), pv(124));
        CHAMA.quad(new Vector3f(0, -0.08f, z0), new Vector3f(0, 0.08f, z0), new Vector3f(0, 0.02f, z1), new Vector3f(0, -0.02f, z1),
            pu(224), pv(100), pu(240), pv(124));
    }
}
