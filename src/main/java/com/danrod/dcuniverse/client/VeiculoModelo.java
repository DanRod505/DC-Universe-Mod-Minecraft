package com.danrod.dcuniverse.client;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Geometria do Batmóvel, da Batmoto e do Batbarco, em blocos, com o nariz para +z e o lado esquerdo do piloto em
 * +x. O corpo de cada um é uma "loft": seções transversais (superelipses, com fundo em V no barco) ao longo do
 * comprimento, ligadas com normais lisas. Rodas, vidros, aletas, armas e bocais são peças à parte.
 * Texturas 256x256 (tools/arte/veiculos.py), uma por veículo, com a mesma divisão: o corpo desenrolado em
 * (0,0)-(256,128), com u = do nariz (0) à cauda (256) e v = da quilha (0) ao topo (128) passando pela lateral
 * (64); na metade de baixo as peças (ver as constantes P_*).
 */
public final class VeiculoModelo {
    private static final float T = 256;

    /** Peças na metade de baixo da textura: {u0, v0, u1, v1} em pixels. */
    static final float[] P_PNEU = {0, 128, 128, 160}, P_ARO = {0, 160, 64, 224}, P_VIDRO = {128, 128, 160, 160},
        P_FAROL = {160, 128, 176, 136}, P_LANTERNA = {176, 128, 192, 136}, P_FILETE = {192, 128, 208, 136},
        P_BOCAL = {208, 128, 224, 136}, P_ALETA = {128, 160, 192, 192}, P_METAL = {192, 160, 256, 192},
        P_CHAPA = {128, 192, 256, 224}, P_BANCO = {128, 224, 256, 256}, P_ARO_MOTO = {64, 160, 128, 224};

    /** Um veículo montado: corpo, vidro, peças que brilham, rodas (eixo em x, centro na origem) e onde ficam. */
    public static final class Conjunto {
        public final Malha corpo = new Malha(), vidro = new Malha(), luzes = new Malha(), farois = new Malha();
        public final List<Roda> rodas = new ArrayList<>();
        /** Pontos de saída do farol (x, y, z) para o facho de luz. */
        public final List<Vector3f> fachos = new ArrayList<>();
    }

    /** Roda: malha centrada na origem, posição no veículo e se esterça. */
    public record Roda(Malha malha, float x, float y, float z, float raio, boolean dianteira) {}

    public static final Conjunto BATMOVEL = new Conjunto(), BATMOTO = new Conjunto(), BATBARCO = new Conjunto();

    private VeiculoModelo() {}

    static {
        batmovel(BATMOVEL);
        batmoto(BATMOTO);
        batbarco(BATBARCO);
    }

    // ------------------------------------------------------------------ loft

    /** Seção do corpo: posição z, meia largura, fundo e topo, e expoentes da superelipse em baixo e em cima. */
    record Secao(float z, float w, float yb, float yt, float nb, float nt) {
        Secao(float z, float w, float yb, float yt) {
            this(z, w, yb, yt, 4f, 3.2f);
        }
    }

    /**
     * Corpo liso a partir das seções (do nariz para a cauda): metade esquerda e espelho, normais médias e tampas
     * no nariz e na cauda. zNariz e zCauda definem o u da textura.
     */
    static void loft(Malha m, Secao[] s, float zNariz, float zCauda) {
        int nv = 16;
        int ns = s.length;
        Vector3f[][] p = new Vector3f[ns][nv + 1];
        for (int i = 0; i < ns; i++) {
            Secao c = s[i];
            float yc = (c.yb() + c.yt()) / 2, h = (c.yt() - c.yb()) / 2;
            for (int j = 0; j <= nv; j++) {
                double th = -Math.PI / 2 + Math.PI * j / nv;
                double co = Math.cos(th), si = Math.sin(th);
                float n = si < 0 ? c.nb() : c.nt();
                float x = (float) (c.w() * Math.pow(Math.abs(co), 2 / n));
                float y = yc + (float) (h * Math.signum(si) * Math.pow(Math.abs(si), 2 / n));
                p[i][j] = new Vector3f(x, y, c.z());
            }
        }
        for (int lado = -1; lado <= 1; lado += 2) {
            Vector3f[][] n = new Vector3f[ns][nv + 1];
            for (int i = 0; i < ns; i++) for (int j = 0; j <= nv; j++) n[i][j] = new Vector3f();
            for (int i = 0; i + 1 < ns; i++) {
                for (int j = 0; j < nv; j++) {
                    Vector3f a = esp(p[i][j], lado), b = esp(p[i + 1][j], lado), c = esp(p[i + 1][j + 1], lado);
                    Vector3f f = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a));
                    // Normal para fora (do eixo do corpo para o ponto).
                    Vector3f fora = new Vector3f(a.x, a.y - (s[i].yb() + s[i].yt()) / 2, 0);
                    if (f.dot(fora) < 0) f.negate();
                    for (int[] k : new int[][] {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}}) n[k[0]][k[1]].add(f);
                }
            }
            for (int i = 0; i + 1 < ns; i++) {
                for (int j = 0; j < nv; j++) {
                    Malha.V a = v(p[i][j], n[i][j], lado, j, nv, zNariz, zCauda), b = v(p[i + 1][j], n[i + 1][j], lado, j, nv, zNariz, zCauda);
                    Malha.V c = v(p[i + 1][j + 1], n[i + 1][j + 1], lado, j + 1, nv, zNariz, zCauda), d = v(p[i][j + 1], n[i][j + 1], lado, j + 1, nv, zNariz, zCauda);
                    // Sentido anti-horário visto de fora em cada lado (o espelho inverte).
                    if (lado > 0) m.tri(a, b, c).tri(a, c, d);
                    else m.tri(a, c, b).tri(a, d, c);
                }
            }
            // Tampas: leque do centro da seção.
            tampa(m, p[0], s[0], lado, true);
            tampa(m, p[ns - 1], s[ns - 1], lado, false);
        }
    }

    private static void tampa(Malha m, Vector3f[] anel, Secao s, int lado, boolean nariz) {
        Vector3f c = new Vector3f(0, (s.yb() + s.yt()) / 2, s.z());
        float u = pu(P_CHAPA[0] + 8), v = pv(P_CHAPA[1] + 8);
        for (int j = 0; j + 1 < anel.length; j++) {
            Vector3f a = esp(anel[j], lado), b = esp(anel[j + 1], lado);
            boolean horario = (lado > 0) == nariz;
            if (horario) m.tri(c, u, v, a, u, v, b, u, v);
            else m.tri(c, u, v, b, u, v, a, u, v);
        }
    }

    private static Vector3f esp(Vector3f p, int lado) {
        return new Vector3f(p.x * lado, p.y, p.z);
    }

    private static Malha.V v(Vector3f p, Vector3f n, int lado, int j, int nv, float zNariz, float zCauda) {
        Vector3f q = esp(p, lado);
        Vector3f nn = new Vector3f(n);
        if (nn.lengthSquared() < 1e-10f) nn.set(lado, 0, 0);
        nn.normalize();
        float u = (zNariz - p.z) / (zNariz - zCauda) * 256f / T;
        float vv = (j / (float) nv) * 128f / T;
        return new Malha.V(q.x, q.y, q.z, u, vv, nn.x, nn.y, nn.z);
    }

    static float pu(float px) {
        return px / T;
    }

    static float pv(float py) {
        return py / T;
    }

    // ------------------------------------------------------------------ peças

    /** Caixa com uma área da textura. */
    static void caixa(Malha m, float x0, float y0, float z0, float x1, float y1, float z1, float[] a) {
        m.caixa(x0, y0, z0, x1, y1, z1, pu(a[0]), pv(a[1]), pu(a[2]), pv(a[3]));
    }

    /** Tubo de n lados ao longo de z (de z0 a z1), raios r0 e r1. */
    static void tubo(Malha m, float x, float y, float z0, float z1, float r0, float r1, float[] a) {
        int n = 12;
        for (int k = 0; k < n; k++) {
            double a0 = Math.PI * 2 * k / n, a1 = Math.PI * 2 * (k + 1) / n;
            Vector3f p0 = new Vector3f(x + r0 * (float) Math.cos(a0), y + r0 * (float) Math.sin(a0), z0);
            Vector3f p1 = new Vector3f(x + r0 * (float) Math.cos(a1), y + r0 * (float) Math.sin(a1), z0);
            Vector3f q1 = new Vector3f(x + r1 * (float) Math.cos(a1), y + r1 * (float) Math.sin(a1), z1);
            Vector3f q0 = new Vector3f(x + r1 * (float) Math.cos(a0), y + r1 * (float) Math.sin(a0), z1);
            float u0 = pu(a[0] + (a[2] - a[0]) * k / n), u1 = pu(a[0] + (a[2] - a[0]) * (k + 1) / n);
            if (z1 < z0) m.quad(p0, q0, q1, p1, u0, pv(a[1]), u1, pv(a[3]));
            else m.quad(p1, q1, q0, p0, u1, pv(a[1]), u0, pv(a[3]));
        }
    }

    /** Disco virado para +z (frente=true) ou -z, com a área da textura mapeada em círculo. */
    static void disco(Malha m, float x, float y, float z, float r, boolean frente, float[] a) {
        int n = 14;
        Vector3f c = new Vector3f(x, y, z);
        float cu = pu((a[0] + a[2]) / 2), cv = pv((a[1] + a[3]) / 2), ru = pu((a[2] - a[0]) / 2), rv = pv((a[3] - a[1]) / 2);
        for (int k = 0; k < n; k++) {
            double a0 = Math.PI * 2 * k / n, a1 = Math.PI * 2 * (k + 1) / n;
            Vector3f p0 = new Vector3f(x + r * (float) Math.cos(a0), y + r * (float) Math.sin(a0), z);
            Vector3f p1 = new Vector3f(x + r * (float) Math.cos(a1), y + r * (float) Math.sin(a1), z);
            float u0 = cu + ru * (float) Math.cos(a0), v0 = cv - rv * (float) Math.sin(a0), u1 = cu + ru * (float) Math.cos(a1), v1 = cv - rv * (float) Math.sin(a1);
            if (frente) m.tri(c, cu, cv, p0, u0, v0, p1, u1, v1);
            else m.tri(c, cu, cv, p1, u1, v1, p0, u0, v0);
        }
    }

    /** Placa fina: quadrilátero (anti-horário visto do lado +normal) com espessura, nas duas faces e bordas. */
    static void placa(Malha m, Vector3f a, Vector3f b, Vector3f c, Vector3f d, float meia, float[] t) {
        Vector3f off = new Vector3f(b).sub(a).cross(new Vector3f(d).sub(a)).normalize().mul(meia);
        Vector3f a1 = new Vector3f(a).add(off), b1 = new Vector3f(b).add(off), c1 = new Vector3f(c).add(off), d1 = new Vector3f(d).add(off);
        Vector3f a2 = new Vector3f(a).sub(off), b2 = new Vector3f(b).sub(off), c2 = new Vector3f(c).sub(off), d2 = new Vector3f(d).sub(off);
        float u0 = pu(t[0]), u1 = pu(t[2]), v0 = pv(t[1]), v1 = pv(t[3]), ue = pu(t[0] + 3);
        m.quad(a1, b1, c1, d1, u0, v0, u1, v1);
        m.quad(b2, a2, d2, c2, u0, v0, u1, v1);
        m.quad(d1, c1, c2, d2, u0, v0, ue, v1);
        m.quad(c1, b1, b2, c2, u0, v0, ue, v1);
        m.quad(a1, d1, d2, a2, u0, v0, ue, v1);
        m.quad(b1, a1, a2, b2, u0, v0, ue, v1);
    }

    /** Roda com eixo em x: banda de rodagem (pneu), flancos e aro afundado com raios. */
    static Malha roda(float r, float largura, float[] aro) {
        Malha m = new Malha();
        int n = 20;
        float h = largura / 2, ri = r * 0.68f;
        for (int k = 0; k < n; k++) {
            double a0 = Math.PI * 2 * k / n, a1 = Math.PI * 2 * (k + 1) / n;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            // Banda: bordas arredondadas (um chanfro de cada lado).
            float rr = r * 0.94f, hh = h * 0.86f;
            Vector3f[] anel0 = {new Vector3f(-h, s0 * rr, c0 * rr), new Vector3f(-hh, s0 * r, c0 * r), new Vector3f(hh, s0 * r, c0 * r), new Vector3f(h, s0 * rr, c0 * rr)};
            Vector3f[] anel1 = {new Vector3f(-h, s1 * rr, c1 * rr), new Vector3f(-hh, s1 * r, c1 * r), new Vector3f(hh, s1 * r, c1 * r), new Vector3f(h, s1 * rr, c1 * rr)};
            float u0 = pu(P_PNEU[0] + (P_PNEU[2] - P_PNEU[0]) * k / n), u1 = pu(P_PNEU[0] + (P_PNEU[2] - P_PNEU[0]) * (k + 1) / n);
            for (int i = 0; i < 3; i++) {
                float v0 = pv(P_PNEU[1] + (P_PNEU[3] - P_PNEU[1]) * i / 3f), v1 = pv(P_PNEU[1] + (P_PNEU[3] - P_PNEU[1]) * (i + 1) / 3f);
                m.quad(anel0[i], anel0[i + 1], anel1[i + 1], anel1[i], u0, v0, u1, v1);
            }
            // Flancos: do pneu até o aro, de cada lado.
            float ut = pu(P_PNEU[0] + 4), vt = pv(P_PNEU[1] + 2);
            for (int lado = -1; lado <= 1; lado += 2) {
                Vector3f o0 = new Vector3f(lado * h, s0 * rr, c0 * rr), o1 = new Vector3f(lado * h, s1 * rr, c1 * rr);
                Vector3f i0 = new Vector3f(lado * h * 0.8f, s0 * ri, c0 * ri), i1 = new Vector3f(lado * h * 0.8f, s1 * ri, c1 * ri);
                if (lado > 0) m.quad(o1, o0, i0, i1, ut, vt, ut, vt);
                else m.quad(o0, o1, i1, i0, ut, vt, ut, vt);
                // Aro: disco afundado com a textura dos raios.
                Vector3f c = new Vector3f(lado * h * 0.62f, 0, 0);
                float cu = pu((aro[0] + aro[2]) / 2), cv = pv((aro[1] + aro[3]) / 2), ru = pu((aro[2] - aro[0]) / 2), rv = pv((aro[3] - aro[1]) / 2);
                float ua = cu + ru * c0, va = cv - rv * s0, ub = cu + ru * c1, vb = cv - rv * s1;
                // Visto de fora (+x no lado +), o anel anda no sentido horário: a ordem inverte.
                if (lado > 0) m.tri(c, cu, cv, i1, ub, vb, i0, ua, va);
                else m.tri(c, cu, cv, i0, ua, va, i1, ub, vb);
            }
        }
        return m;
    }

    /** Vidro em meio elipsoide alongado sobre o corpo: centro (cx, cz), base y0, raios e altura. */
    static void cupula(Malha m, float cz, float y0, float rx, float rz, float alto, float bico) {
        int nl = 8, na = 18;
        Vector3f[][] p = new Vector3f[nl + 1][na + 1];
        for (int i = 0; i <= nl; i++) {
            double lat = Math.PI / 2 * i / nl;
            for (int j = 0; j <= na; j++) {
                double lon = Math.PI * 2 * j / na;
                double cz2 = Math.cos(lon);
                // Na frente o vidro é mais comprido (bico) e desce suave como um para-brisa.
                float rzz = cz2 > 0 ? rz * bico : rz;
                float x = (float) (rx * Math.cos(lat) * Math.sin(lon)), z = cz + (float) (rzz * Math.cos(lat) * cz2);
                p[i][j] = new Vector3f(x, y0 + alto * (float) Math.sin(lat), z);
            }
        }
        for (int i = 0; i < nl; i++) {
            for (int j = 0; j < na; j++) {
                Vector3f a = p[i][j], b = p[i][j + 1], c = p[i + 1][j + 1], d = p[i + 1][j];
                float u0 = pu(P_VIDRO[0] + 32f * j / na), u1 = pu(P_VIDRO[0] + 32f * (j + 1) / na);
                float v0 = pv(P_VIDRO[1] + 32f * i / nl), v1 = pv(P_VIDRO[1] + 32f * (i + 1) / nl);
                m.tri(a, u0, v0, c, u1, v1, d, u0, v1).tri(a, u0, v0, b, u1, v0, c, u1, v1);
            }
        }
    }

    // ------------------------------------------------------------------ Batmóvel

    private static void batmovel(Conjunto k) {
        Secao[] s = {
            new Secao(2.78f, 0.5f, 0.42f, 0.6f, 3, 2.5f),
            new Secao(2.55f, 0.92f, 0.3f, 0.7f),
            new Secao(2.15f, 1.04f, 0.26f, 0.8f),
            new Secao(1.6f, 0.98f, 0.24f, 0.84f),
            new Secao(1.0f, 0.92f, 0.24f, 0.98f),
            new Secao(0.3f, 0.9f, 0.24f, 1.12f),
            new Secao(-0.5f, 0.92f, 0.24f, 1.12f),
            new Secao(-1.2f, 1.06f, 0.26f, 1.0f),
            new Secao(-1.9f, 1.12f, 0.28f, 0.94f),
            new Secao(-2.5f, 1.08f, 0.34f, 0.9f),
            new Secao(-2.8f, 0.85f, 0.42f, 0.84f, 3, 2.5f),
        };
        loft(k.corpo, s, 2.8f, -2.8f);
        // Cabine de vidro escuro e as aletas de morcego atrás.
        cupula(k.vidro, 0.15f, 1.02f, 0.6f, 0.95f, 0.55f, 1.5f);
        for (int lado = -1; lado <= 1; lado += 2) {
            float x = 0.78f * lado;
            Vector3f bf = new Vector3f(x, 0.92f, -1.3f), bt = new Vector3f(x, 0.88f, -2.6f);
            Vector3f tt = new Vector3f(x + 0.22f * lado, 1.75f, -3.15f), tf = new Vector3f(x + 0.12f * lado, 1.3f, -2.1f);
            if (lado > 0) placa(k.corpo, bf, bt, tt, tf, 0.035f, P_ALETA);
            else placa(k.corpo, bt, bf, tf, tt, 0.035f, P_ALETA);
            // Para-lamas sobre as rodas de trás (as rodas ficam para fora do corpo).
            caixa(k.corpo, lado > 0 ? 0.95f : -1.45f, 0.95f, -2.35f, lado > 0 ? 1.45f : -0.95f, 1.02f, -1.05f, P_CHAPA);
            caixa(k.corpo, lado > 0 ? 0.92f : -1.32f, 0.86f, 1.35f, lado > 0 ? 1.32f : -0.92f, 0.92f, 2.2f, P_CHAPA);
            // Metralhadoras saindo do capô e o lançador de mísseis no teto.
            tubo(k.corpo, 0.62f * lado, 0.55f, 2.2f, 2.95f, 0.05f, 0.04f, P_METAL);
            caixa(k.corpo, 0.62f * lado - 0.11f, 0.47f, 1.9f, 0.62f * lado + 0.11f, 0.63f, 2.35f, P_METAL);
            caixa(k.corpo, 0.55f * lado - 0.14f, 1.1f, -1.25f, 0.55f * lado + 0.14f, 1.3f, -0.55f, P_METAL);
            // Faróis: uma faixa fina e afiada de cada lado do nariz.
            Vector3f f0 = new Vector3f(0.32f * lado, 0.6f, 2.66f), f1 = new Vector3f(0.88f * lado, 0.64f, 2.4f);
            Vector3f f2 = new Vector3f(0.86f * lado, 0.58f, 2.42f), f3 = new Vector3f(0.3f * lado, 0.55f, 2.68f);
            luz(k.farois, f0, f1, f2, f3, lado, P_FAROL);
            k.fachos.add(new Vector3f(0.6f * lado, 0.6f, 2.6f));
            // Lanternas atrás.
            luz(k.luzes, new Vector3f(1.0f * lado, 0.8f, -2.62f), new Vector3f(0.45f * lado, 0.78f, -2.8f),
                new Vector3f(0.45f * lado, 0.72f, -2.8f), new Vector3f(1.0f * lado, 0.72f, -2.62f), -lado, P_LANTERNA);
            // Rodas: grandes atrás, menores na frente.
            k.rodas.add(new Roda(roda(0.56f, 0.55f, P_ARO), 1.2f * lado, 0.56f, -1.7f, 0.56f, false));
            k.rodas.add(new Roda(roda(0.46f, 0.4f, P_ARO), 1.1f * lado, 0.46f, 1.78f, 0.46f, true));
        }
        // Turbina atrás: bocal com o fundo aceso.
        tubo(k.corpo, 0, 0.58f, -2.6f, -3.05f, 0.25f, 0.21f, P_METAL);
        disco(k.luzes, 0, 0.58f, -2.98f, 0.18f, false, P_BOCAL);
    }

    /** Quad de luz (farol, lanterna) com uma camada para cada lado, para não sumir de nenhum ângulo. */
    private static void luz(Malha m, Vector3f a, Vector3f b, Vector3f c, Vector3f d, int lado, float[] t) {
        float u0 = pu(t[0]), v0 = pv(t[1]), u1 = pu(t[2]), v1 = pv(t[3]);
        m.quad(a, b, c, d, u0, v0, u1, v1);
        m.quad(b, a, d, c, u0, v0, u1, v1);
    }

    // ------------------------------------------------------------------ Batmoto

    private static void batmoto(Conjunto k) {
        Secao[] s = {
            new Secao(1.32f, 0.08f, 0.62f, 0.74f, 3, 2.5f),
            new Secao(1.12f, 0.2f, 0.5f, 0.92f),
            new Secao(0.8f, 0.27f, 0.42f, 1.02f),
            new Secao(0.4f, 0.29f, 0.36f, 0.95f),
            new Secao(0.05f, 0.25f, 0.36f, 0.8f),
            new Secao(-0.35f, 0.22f, 0.4f, 0.84f),
            new Secao(-0.8f, 0.2f, 0.5f, 0.9f),
            new Secao(-1.2f, 0.14f, 0.6f, 0.92f),
            new Secao(-1.42f, 0.06f, 0.7f, 0.88f, 3, 2.5f),
        };
        loft(k.corpo, s, 1.35f, -1.45f);
        // Para-brisa com as orelhas de morcego.
        Vector3f a = new Vector3f(-0.24f, 0.98f, 0.92f), b = new Vector3f(0.24f, 0.98f, 0.92f);
        Vector3f c = new Vector3f(0.2f, 1.26f, 0.72f), d = new Vector3f(-0.2f, 1.26f, 0.72f);
        k.vidro.quad(b, a, d, c, pu(P_VIDRO[0]), pv(P_VIDRO[1]), pu(P_VIDRO[2]), pv(P_VIDRO[3]));
        k.vidro.quad(a, b, c, d, pu(P_VIDRO[0]), pv(P_VIDRO[1]), pu(P_VIDRO[2]), pv(P_VIDRO[3]));
        for (int lado = -1; lado <= 1; lado += 2) {
            Vector3f o0 = new Vector3f(0.13f * lado, 1.0f, 0.86f), o1 = new Vector3f(0.21f * lado, 1.0f, 0.82f);
            Vector3f o2 = new Vector3f(0.22f * lado, 1.38f, 0.66f), o3 = new Vector3f(0.17f * lado, 1.2f, 0.72f);
            if (lado > 0) placa(k.corpo, o0, o1, o2, o3, 0.015f, P_ALETA);
            else placa(k.corpo, o1, o0, o3, o2, 0.015f, P_ALETA);
            // Garfo, guidão e balança.
            caixa(k.corpo, 0.13f * lado - 0.03f, 0.42f, 0.9f, 0.13f * lado + 0.03f, 0.9f, 1.0f, P_METAL);
            caixa(k.corpo, lado > 0 ? 0.2f : -0.46f, 0.98f, 0.56f, lado > 0 ? 0.46f : -0.2f, 1.03f, 0.62f, P_METAL);
            caixa(k.corpo, lado > 0 ? 0.42f : -0.5f, 0.95f, 0.52f, lado > 0 ? 0.5f : -0.42f, 1.06f, 0.66f, P_BANCO);
            caixa(k.corpo, 0.12f * lado - 0.03f, 0.4f, -0.98f, 0.12f * lado + 0.03f, 0.5f, -0.2f, P_METAL);
            // Metralhadoras leves no bico, pedaleiras e o farol duplo.
            tubo(k.corpo, 0.24f * lado, 0.62f, 1.05f, 1.42f, 0.03f, 0.025f, P_METAL);
            caixa(k.corpo, lado > 0 ? 0.24f : -0.42f, 0.42f, -0.15f, lado > 0 ? 0.42f : -0.24f, 0.46f, 0.05f, P_METAL);
            luz(k.farois, new Vector3f(0.04f * lado, 0.76f, 1.27f), new Vector3f(0.15f * lado, 0.82f, 1.15f),
                new Vector3f(0.16f * lado, 0.74f, 1.16f), new Vector3f(0.05f * lado, 0.68f, 1.28f), lado, P_FAROL);
            // Aletas pequenas na rabeta.
            Vector3f r0 = new Vector3f(0.1f * lado, 0.88f, -0.9f), r1 = new Vector3f(0.1f * lado, 0.86f, -1.35f);
            Vector3f r2 = new Vector3f(0.2f * lado, 1.18f, -1.62f), r3 = new Vector3f(0.15f * lado, 1.0f, -1.15f);
            if (lado > 0) placa(k.corpo, r0, r1, r2, r3, 0.015f, P_ALETA);
            else placa(k.corpo, r1, r0, r3, r2, 0.015f, P_ALETA);
        }
        k.fachos.add(new Vector3f(0, 0.76f, 1.3f));
        // Banco de couro.
        caixa(k.corpo, -0.2f, 0.8f, -0.75f, 0.2f, 0.88f, 0.05f, P_BANCO);
        // Lanterna e o bocal do turbo embaixo da rabeta.
        luz(k.luzes, new Vector3f(0.1f, 0.84f, -1.42f), new Vector3f(-0.1f, 0.84f, -1.42f), new Vector3f(-0.1f, 0.78f, -1.4f),
            new Vector3f(0.1f, 0.78f, -1.4f), -1, P_LANTERNA);
        tubo(k.corpo, 0, 0.5f, -1.0f, -1.38f, 0.1f, 0.085f, P_METAL);
        disco(k.luzes, 0, 0.5f, -1.34f, 0.07f, false, P_BOCAL);
        k.rodas.add(new Roda(roda(0.42f, 0.2f, P_ARO_MOTO), 0, 0.42f, 1.0f, 0.42f, true));
        k.rodas.add(new Roda(roda(0.45f, 0.28f, P_ARO_MOTO), 0, 0.45f, -0.98f, 0.45f, false));
    }

    // ------------------------------------------------------------------ Batbarco

    private static void batbarco(Conjunto k) {
        Secao[] s = {
            new Secao(2.75f, 0.12f, 0.6f, 0.86f, 1.6f, 2.5f),
            new Secao(2.35f, 0.62f, 0.3f, 0.92f, 1.3f, 3),
            new Secao(1.6f, 1.02f, 0.12f, 0.92f, 1.25f, 3.5f),
            new Secao(0.6f, 1.16f, 0.06f, 0.9f, 1.3f, 3.5f),
            new Secao(-0.6f, 1.18f, 0.06f, 0.88f, 1.4f, 3.5f),
            new Secao(-1.8f, 1.14f, 0.12f, 0.86f, 1.6f, 3.5f),
            new Secao(-2.5f, 1.06f, 0.2f, 0.85f, 2.2f, 3.5f),
        };
        loft(k.corpo, s, 2.8f, -2.55f);
        cupula(k.vidro, 0.35f, 0.82f, 0.82f, 0.9f, 0.78f, 1.6f);
        for (int lado = -1; lado <= 1; lado += 2) {
            // Asas de morcego nas laterais, subindo para trás.
            Vector3f bf = new Vector3f(1.08f * lado, 0.86f, -0.5f), bt = new Vector3f(1.04f * lado, 0.84f, -2.45f);
            Vector3f tt = new Vector3f(1.55f * lado, 1.55f, -2.95f), tf = new Vector3f(1.3f * lado, 1.1f, -1.4f);
            if (lado > 0) placa(k.corpo, bf, bt, tt, tf, 0.035f, P_ALETA);
            else placa(k.corpo, bt, bf, tf, tt, 0.035f, P_ALETA);
            // Jatos d'água na popa.
            tubo(k.corpo, 0.48f * lado, 0.38f, -2.35f, -2.82f, 0.17f, 0.15f, P_METAL);
            disco(k.luzes, 0.48f * lado, 0.38f, -2.77f, 0.12f, false, P_BOCAL);
            // Metralhadoras no convés da proa e os tubos de torpedo embaixo.
            tubo(k.corpo, 0.55f * lado, 0.98f, 1.75f, 2.35f, 0.045f, 0.04f, P_METAL);
            caixa(k.corpo, 0.55f * lado - 0.1f, 0.9f, 1.55f, 0.55f * lado + 0.1f, 1.04f, 1.9f, P_METAL);
            tubo(k.corpo, 0.5f * lado, 0.2f, 1.2f, 1.75f, 0.08f, 0.08f, P_METAL);
            luz(k.farois, new Vector3f(0.18f * lado, 0.86f, 2.52f), new Vector3f(0.5f * lado, 0.9f, 2.24f),
                new Vector3f(0.5f * lado, 0.83f, 2.25f), new Vector3f(0.18f * lado, 0.8f, 2.53f), lado, P_FAROL);
            k.fachos.add(new Vector3f(0.35f * lado, 0.86f, 2.4f));
            luz(k.luzes, new Vector3f(1.0f * lado, 0.8f, -2.48f), new Vector3f(0.75f * lado, 0.82f, -2.53f),
                new Vector3f(0.75f * lado, 0.76f, -2.53f), new Vector3f(1.0f * lado, 0.74f, -2.48f), -lado, P_LANTERNA);
        }
        // Barbatana dorsal atrás da cabine.
        placa(k.corpo, new Vector3f(0, 0.88f, -1.0f), new Vector3f(0, 0.86f, -2.3f), new Vector3f(0, 1.65f, -2.75f),
            new Vector3f(0, 1.2f, -1.6f), 0.04f, P_ALETA);
    }
}
