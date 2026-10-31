package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.content.JatoInvisivel;
import org.joml.Vector3f;

/**
 * Geometria do Jato Invisível da Mulher-Maravilha, em blocos e já no tamanho final, com o nariz para +z e a esquerda
 * do piloto em +x (coordenadas a partir dos pés da entidade). Fuselagem fina e comprida de seção oval, com a
 * carenagem da cabine estufada em cima do meio; asas em delta bem varridas com diedro leve; duas derivas
 * inclinadas para fora atrás; dois bocais. Tudo é vidro: o renderer desenha o casco quase transparente, com as
 * arestas acesas (o contorno que deixa ver o jato) e um brilho que passa do nariz à cauda. Dentro, os quatro
 * assentos (ability/active/Veiculo, content/JatoInvisivel.ASSENTOS) e o painel, um pouco mais visíveis.
 * Textura em textures/entity/jato_invisivel.png (256x256, tools/arte/jato_invisivel.py): vidro em (0,0)-(128,128)
 * com u ao longo de z e v em volta; as outras áreas estão nas constantes abaixo.
 */
public final class JatoModelo {
    public static final float PIVO_Y = JatoInvisivel.PIVO_Y;
    static final float Z_NARIZ = 3.7f, Z_CAUDA = -3.3f;
    /** Bocais: centro em x = ±BOCAL_X, eixo em y = PIVO_Y, de BOCAL_Z0 para trás até BOCAL_Z1. */
    public static final float BOCAL_X = 0.34f, BOCAL_R = 0.27f, BOCAL_Z0 = -3.0f, BOCAL_Z1 = -3.42f;
    private static final float TW = 256, TH = 256;
    static final float[] VIDRO = {0, 0, 128, 128}, ASSENTO = {128, 0, 160, 32}, ENCOSTO = {160, 0, 192, 32},
        PAINEL = {192, 0, 224, 32}, OURO = {224, 0, 240, 16}, LUZ = {240, 0, 256, 16}, CHAMA = {128, 32, 192, 64},
        TELA = {224, 16, 256, 32}, BRASAO = {224, 32, 256, 64};

    /** Casco de vidro: fuselagem, carenagem, asas, derivas e bocais. */
    public static final Malha CASCO = new Malha();
    /** Assentos, painel e manche (mais visíveis que o casco). */
    public static final Malha CABINE = new Malha();
    /** Arestas acesas (tubos finos ao longo do contorno). */
    public static final Malha ARESTAS = new Malha();
    /** Telas do painel e o brasão dourado (brilham). */
    public static final Malha TELAS = new Malha();
    /** Fundo aceso dos bocais. */
    public static final Malha BOCAIS = new Malha();
    /** Chama de 1 bloco a partir de BOCAL_Z1 para -z (o renderer estica). */
    public static final Malha CHAMA_M = new Malha();

    static final int N_P = 22, N_Z = 30;

    private JatoModelo() {}

    static {
        fuselagem();
        asas();
        derivas();
        bocais();
        cabine();
        arestasFuselagem();
    }

    // ================================================================= fuselagem

    /** Meia largura da fuselagem na estação z. */
    static float largura(float z) {
        if (z > 1.0f) {
            float u = (z - 1.0f) / (Z_NARIZ - 1.0f);
            return 0.6f * (float) Math.pow(Math.max(0, 1 - u * u), 0.62);
        }
        if (z < -2.3f) {
            float u = (-2.3f - z) / (-2.3f - Z_CAUDA);
            return 0.6f - 0.12f * u * u;
        }
        return 0.6f;
    }

    /** A carenagem da cabine: quanto sobe o dorso na estação z (sino entre CAB_TRAS e CAB_FRENTE). */
    static float cabine(float z) {
        float frente = 2.45f, tras = -1.55f, meio = 0.75f;
        if (z >= frente || z <= tras) return 0;
        float u = z >= meio ? (z - meio) / (frente - meio) : (meio - z) / (meio - tras);
        return 0.5f * (float) Math.pow(Math.cos(Math.PI / 2 * u), z >= meio ? 1.1 : 0.8);
    }

    static float alto(float z) {
        float base = z > 1.0f ? 0.4f * largura(z) / 0.6f + 0.02f : 0.42f;
        return base + cabine(z);
    }

    static float baixo(float z) {
        return z > 1.0f ? 0.3f * largura(z) / 0.6f + 0.02f : 0.32f;
    }

    /** Altura do eixo: o nariz desce um pouco. */
    static float centro(float z) {
        if (z <= 1.0f) return PIVO_Y;
        float u = (z - 1.0f) / (Z_NARIZ - 1.0f);
        return PIVO_Y - 0.12f * u * u;
    }

    /** Ponto do casco no ângulo p (0 em cima, pi/2 na esquerda, pi embaixo) da estação z. */
    static Vector3f secao(float p, float z) {
        float s = (float) Math.sin(p), c = (float) Math.cos(p);
        float x = largura(z) * Math.signum(s) * (float) Math.pow(Math.abs(s), 0.85);
        float y = centro(z) + (c >= 0 ? alto(z) * (float) Math.pow(c, 0.9) : -baixo(z) * (float) Math.pow(-c, 1.2));
        return new Vector3f(x, y, z);
    }

    private static float zEst(int i) {
        // Mais estações no nariz e na cabine, onde a forma muda mais.
        float t = i / (float) N_Z;
        return Z_CAUDA + (Z_NARIZ - Z_CAUDA) * (float) (t + 0.08 * Math.sin(Math.PI * t));
    }

    private static void fuselagem() {
        for (int i = 0; i < N_Z; i++) {
            float z0 = zEst(i), z1 = zEst(i + 1);
            for (int j = 0; j < N_P; j++) {
                float p0 = (float) (2 * Math.PI * j / N_P), p1 = (float) (2 * Math.PI * (j + 1) / N_P);
                Vector3f a = secao(p0, z0), b = secao(p1, z0), c = secao(p1, z1), d = secao(p0, z1);
                if (i == N_Z - 1) {
                    // Ponta do nariz: fecha num ponto.
                    Vector3f ponta = new Vector3f(0, centro(Z_NARIZ), Z_NARIZ);
                    c = ponta;
                    d = ponta;
                }
                float u0 = uv(VIDRO, 0, (z0 - Z_CAUDA) / (Z_NARIZ - Z_CAUDA)), u1 = uv(VIDRO, 0, (z1 - Z_CAUDA) / (Z_NARIZ - Z_CAUDA));
                float v0 = uv(VIDRO, 1, j / (float) N_P), v1 = uv(VIDRO, 1, (j + 1) / (float) N_P);
                CASCO.tri(a, u0, v0, b, u0, v1, c, u1, v1);
                if (i != N_Z - 1) CASCO.tri(a, u0, v0, c, u1, v1, d, u1, v0);
            }
        }
        // Tampa de trás, entre os bocais.
        int n = N_P;
        Vector3f meio = new Vector3f(0, PIVO_Y, Z_CAUDA);
        for (int j = 0; j < n; j++) {
            Vector3f a = secao((float) (2 * Math.PI * j / n), Z_CAUDA), b = secao((float) (2 * Math.PI * (j + 1) / n), Z_CAUDA);
            CASCO.tri(meio, VIDRO[0] / TW, VIDRO[1] / TH, b, VIDRO[0] / TW + 0.02f, VIDRO[1] / TH, a, VIDRO[0] / TW, VIDRO[1] / TH + 0.02f);
        }
    }

    private static float uv(float[] area, int eixo, float t) {
        return eixo == 0 ? (area[0] + (area[2] - area[0]) * t) / TW : (area[1] + (area[3] - area[1]) * t) / TH;
    }

    // ================================================================= asas e derivas

    /** Quinas de uma asa (lado +1 esquerda, -1 direita): raiz frente, ponta frente, ponta trás, raiz trás. */
    static Vector3f[] asa(int lado) {
        float y = PIVO_Y - 0.12f;
        return new Vector3f[] {
            new Vector3f(lado * 0.5f, y, 1.3f),
            new Vector3f(lado * 3.75f, y + 0.18f, -2.15f),
            new Vector3f(lado * 3.75f, y + 0.18f, -2.75f),
            new Vector3f(lado * 0.5f, y, -2.95f),
        };
    }

    private static void asas() {
        for (int lado = -1; lado <= 1; lado += 2) {
            Vector3f[] q = asa(lado);
            float esp = 0.07f;
            Vector3f[] cima = new Vector3f[4], baixo = new Vector3f[4];
            for (int i = 0; i < 4; i++) {
                float e = i == 1 || i == 2 ? esp * 0.35f : esp;
                cima[i] = new Vector3f(q[i]).add(0, e, 0);
                baixo[i] = new Vector3f(q[i]).add(0, -e, 0);
            }
            placa(CASCO, cima, baixo);
            // Arestas: bordo de ataque, ponta e bordo de fuga.
            tubo(ARESTAS, q[0], q[1], 0.03f);
            tubo(ARESTAS, q[1], q[2], 0.03f);
            tubo(ARESTAS, q[2], q[3], 0.022f);
            // Linha do flap, mais fraca, um pouco antes do bordo de fuga.
            tubo(ARESTAS, new Vector3f(q[3]).lerp(q[0], 0.12f).add(0, esp, 0), new Vector3f(q[2]).lerp(q[1], 0.25f).add(0, esp * 0.4f, 0), 0.012f);
        }
    }

    /** Quinas de uma deriva (lado +1 esquerda): base frente, topo frente, topo trás, base trás. */
    static Vector3f[] deriva(int lado) {
        float x = lado * 0.42f, y0 = PIVO_Y + 0.3f, h = 1.15f, inclina = 0.32f;
        return new Vector3f[] {
            new Vector3f(x, y0, -1.75f),
            new Vector3f(x + lado * h * inclina, y0 + h, -2.75f),
            new Vector3f(x + lado * h * inclina, y0 + h, -3.2f),
            new Vector3f(x, y0, -3.25f),
        };
    }

    private static void derivas() {
        for (int lado = -1; lado <= 1; lado += 2) {
            Vector3f[] q = deriva(lado);
            Vector3f n = new Vector3f(q[1]).sub(q[0]).cross(new Vector3f(q[3]).sub(q[0])).normalize(0.035f);
            Vector3f[] a = new Vector3f[4], b = new Vector3f[4];
            for (int i = 0; i < 4; i++) {
                a[i] = new Vector3f(q[i]).add(n);
                b[i] = new Vector3f(q[i]).sub(n);
            }
            placa(CASCO, a, b);
            tubo(ARESTAS, q[0], q[1], 0.026f);
            tubo(ARESTAS, q[1], q[2], 0.026f);
            tubo(ARESTAS, q[2], q[3], 0.02f);
        }
    }

    /** Placa grossa: face de cima (a), de baixo (b) e as quatro bordas. */
    private static void placa(Malha m, Vector3f[] a, Vector3f[] b) {
        float u0 = VIDRO[0] / TW, v0 = VIDRO[1] / TH, u1 = VIDRO[2] / TW * 0.5f, v1 = VIDRO[3] / TH * 0.5f;
        m.quad(a[0], a[1], a[2], a[3], u0, v0, u1, v1);
        m.quad(b[3], b[2], b[1], b[0], u0, v0, u1, v1);
        for (int i = 0; i < 4; i++) {
            int k = (i + 1) % 4;
            m.quad(a[k], a[i], b[i], b[k], u0, v0, u1, v0 + 0.02f);
        }
    }

    // ================================================================= bocais

    private static void bocais() {
        int n = 16;
        for (int lado = -1; lado <= 1; lado += 2) {
            float cx = lado * BOCAL_X;
            Vector3f antP = null, antA = null;
            for (int i = 0; i <= n; i++) {
                double a = 2 * Math.PI * i / n;
                float x = cx + BOCAL_R * (float) Math.cos(a), y = PIVO_Y + BOCAL_R * (float) Math.sin(a);
                Vector3f f = new Vector3f(x, y, BOCAL_Z0), t = new Vector3f(cx + (x - cx) * 1.12f, PIVO_Y + (y - PIVO_Y) * 1.12f, BOCAL_Z1);
                if (antP != null) {
                    CASCO.quad(antP, f, t, antA, VIDRO[0] / TW, VIDRO[1] / TH, VIDRO[0] / TW + 0.05f, VIDRO[1] / TH + 0.05f);
                    tubo(ARESTAS, antA, t, 0.018f);
                    // Fundo aceso.
                    Vector3f fundoC = new Vector3f(cx, PIVO_Y, BOCAL_Z0 - 0.05f);
                    Vector3f fa = new Vector3f(antP).add(0, 0, -0.05f), fb = new Vector3f(f).add(0, 0, -0.05f);
                    BOCAIS.tri(fundoC, (LUZ[0] + 8) / TW, (LUZ[1] + 8) / TH, fb, LUZ[0] / TW, LUZ[1] / TH, fa, LUZ[2] / TW, LUZ[3] / TH);
                    BOCAIS.tri(fundoC, (LUZ[0] + 8) / TW, (LUZ[1] + 8) / TH, fa, LUZ[0] / TW, LUZ[1] / TH, fb, LUZ[2] / TW, LUZ[3] / TH);
                    // Chama: cone de 1 bloco para trás.
                    Vector3f ponta = new Vector3f(cx, PIVO_Y, BOCAL_Z1 - 1f);
                    Vector3f ca = new Vector3f(antA.x, antA.y, BOCAL_Z1), cb = new Vector3f(t.x, t.y, BOCAL_Z1);
                    CHAMA_M.tri(ca, CHAMA[0] / TW, CHAMA[1] / TH, cb, CHAMA[0] / TW, CHAMA[3] / TH, ponta, CHAMA[2] / TW, (CHAMA[1] + CHAMA[3]) / 2 / TH);
                    CHAMA_M.tri(cb, CHAMA[0] / TW, CHAMA[3] / TH, ca, CHAMA[0] / TW, CHAMA[1] / TH, ponta, CHAMA[2] / TW, (CHAMA[1] + CHAMA[3]) / 2 / TH);
                }
                antP = f;
                antA = t;
            }
        }
    }

    // ================================================================= cabine

    private static void cabine() {
        float y = 0.55f - 0.08f;
        for (float[] a : JatoInvisivel.ASSENTOS) {
            float x = a[0], z = a[1];
            // Assento (almofada), encosto inclinado e apoio de cabeça; bordas douradas.
            caixa(CABINE, x - 0.24f, y - 0.1f, z - 0.24f, x + 0.24f, y + 0.02f, z + 0.22f, ASSENTO);
            Vector3f e0 = new Vector3f(x - 0.24f, y, z - 0.28f), e1 = new Vector3f(x + 0.24f, y, z - 0.28f);
            Vector3f e2 = new Vector3f(x + 0.22f, y + 0.62f, z - 0.42f), e3 = new Vector3f(x - 0.22f, y + 0.62f, z - 0.42f);
            Vector3f t = new Vector3f(0, 0, -0.08f);
            prisma(CABINE, e0, e1, e2, e3, t, ENCOSTO);
            caixa(CABINE, x - 0.1f, y + 0.62f, z - 0.48f, x + 0.1f, y + 0.8f, z - 0.4f, ENCOSTO);
            caixa(CABINE, x - 0.26f, y - 0.12f, z - 0.26f, x - 0.22f, y + 0.04f, z + 0.24f, OURO);
            caixa(CABINE, x + 0.22f, y - 0.12f, z - 0.26f, x + 0.26f, y + 0.04f, z + 0.24f, OURO);
        }
        // Painel na frente do piloto, inclinado, com duas telas e o brasão; o manche entre os joelhos.
        float zp = JatoInvisivel.ASSENTOS[0][1] + 0.62f;
        Vector3f p0 = new Vector3f(-0.36f, 0.88f, zp), p1 = new Vector3f(0.36f, 0.88f, zp);
        Vector3f p2 = new Vector3f(0.32f, 1.12f, zp + 0.22f), p3 = new Vector3f(-0.32f, 1.12f, zp + 0.22f);
        prisma(CABINE, p0, p1, p2, p3, new Vector3f(0, -0.06f, 0.06f), PAINEL);
        Vector3f lev = new Vector3f(0, 0.004f, -0.004f);
        for (int lado = -1; lado <= 1; lado += 2) {
            float xa = lado * 0.05f, xb = lado * 0.3f;
            Vector3f a = new Vector3f(xa, 0.91f, zp + 0.03f).add(lev), b = new Vector3f(xb, 0.91f, zp + 0.03f).add(lev);
            Vector3f c = new Vector3f(xb * 0.96f, 1.08f, zp + 0.19f).add(lev), d = new Vector3f(xa, 1.08f, zp + 0.19f).add(lev);
            if (lado > 0) TELAS.quad(b, a, d, c, TELA[0] / TW, TELA[1] / TH, TELA[2] / TW, TELA[3] / TH);
            else TELAS.quad(a, b, c, d, TELA[0] / TW, TELA[1] / TH, TELA[2] / TW, TELA[3] / TH);
        }
        caixa(CABINE, -0.025f, 0.5f, zp - 0.42f, 0.025f, 0.78f, zp - 0.37f, OURO);
        caixa(CABINE, -0.11f, 0.76f, zp - 0.42f, 0.11f, 0.81f, zp - 0.37f, OURO);
        // Brasão (o "W" dourado) no nariz, por cima.
        float zb = 2.75f;
        Vector3f c0 = secao(-0.35f, zb).add(0, 0.01f, 0), c1 = secao(0.35f, zb).add(0, 0.01f, 0);
        Vector3f c2 = secao(0.32f, zb + 0.45f).add(0, 0.01f, 0), c3 = secao(-0.32f, zb + 0.45f).add(0, 0.01f, 0);
        TELAS.quad(c1, c0, c3, c2, BRASAO[0] / TW, BRASAO[1] / TH, BRASAO[2] / TW, BRASAO[3] / TH);
    }

    private static void caixa(Malha m, float x0, float y0, float z0, float x1, float y1, float z1, float[] area) {
        m.caixa(x0, y0, z0, x1, y1, z1, area[0] / TW, area[1] / TH, area[2] / TW, area[3] / TH);
    }

    /** Placa de quatro cantos (a frente) com espessura t: frente, verso e bordas. */
    private static void prisma(Malha m, Vector3f a, Vector3f b, Vector3f c, Vector3f d, Vector3f t, float[] area) {
        float u0 = area[0] / TW, v0 = area[1] / TH, u1 = area[2] / TW, v1 = area[3] / TH;
        Vector3f a2 = new Vector3f(a).add(t), b2 = new Vector3f(b).add(t), c2 = new Vector3f(c).add(t), d2 = new Vector3f(d).add(t);
        m.quad(a, b, c, d, u0, v0, u1, v1);
        m.quad(b2, a2, d2, c2, u0, v0, u1, v1);
        m.quad(a2, b2, b, a, u0, v0, u1, v0 + 0.01f);
        m.quad(d, c, c2, d2, u0, v0, u1, v0 + 0.01f);
        m.quad(a2, a, d, d2, u0, v0, u0 + 0.01f, v1);
        m.quad(b, b2, c2, c, u0, v0, u0 + 0.01f, v1);
    }

    // ================================================================= arestas da fuselagem

    private static void arestasFuselagem() {
        // Lombada, quinas dos lados e a quilha embaixo.
        for (float p : new float[] {0, (float) Math.PI / 2, (float) -Math.PI / 2, (float) Math.PI}) {
            float larg = p == 0 ? 0.03f : 0.022f;
            Vector3f ant = null;
            for (int i = 0; i <= N_Z; i++) {
                float z = i == N_Z ? Z_NARIZ : zEst(i);
                Vector3f q = i == N_Z ? new Vector3f(0, centro(Z_NARIZ), Z_NARIZ) : secao(p, z);
                // A lombada não passa por cima da cabine: de dentro ela virava uma faixa no meio da vista.
                boolean cabine = p == 0 && ant != null && Math.max(ant.z, q.z) > -1.5f && Math.min(ant.z, q.z) < 2.35f;
                if (ant != null && !cabine) tubo(ARESTAS, ant, q, larg);
                ant = q;
            }
        }
        // Moldura da cabine: os dois trilhos laterais da carenagem e os arcos da frente e de trás.
        for (int lado = -1; lado <= 1; lado += 2) {
            Vector3f ant = null;
            for (int i = 0; i <= 16; i++) {
                float z = -1.45f + (2.35f + 1.45f) * i / 16f;
                Vector3f q = secao(lado * 0.78f, z);
                if (ant != null) tubo(ARESTAS, ant, q, 0.02f);
                ant = q;
            }
        }
        for (float z : new float[] {2.3f, 0.95f, -1.4f}) {
            Vector3f ant = null;
            for (int i = 0; i <= 12; i++) {
                float p = -0.8f + 1.6f * i / 12f;
                Vector3f q = secao(p, z);
                if (ant != null) tubo(ARESTAS, ant, q, z == 0.95f ? 0.014f : 0.02f);
                ant = q;
            }
        }
        // Anel do nariz e anel de trás.
        for (float z : new float[] {3.1f, Z_CAUDA + 0.02f}) {
            Vector3f ant = null;
            for (int j = 0; j <= N_P; j++) {
                Vector3f q = secao((float) (2 * Math.PI * j / N_P), z);
                if (ant != null) tubo(ARESTAS, ant, q, 0.016f);
                ant = q;
            }
        }
    }

    /** Tubo quadrado fino de a até b (as quatro faces, nas duas voltas, para aparecer de qualquer lado). */
    static void tubo(Malha m, Vector3f a, Vector3f b, float larg) {
        Vector3f d = new Vector3f(b).sub(a);
        if (d.lengthSquared() < 1e-8f) return;
        d.normalize();
        Vector3f ref = Math.abs(d.y) < 0.9f ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
        Vector3f u = new Vector3f(d).cross(ref).normalize(larg / 2), v = new Vector3f(d).cross(u).normalize(larg / 2);
        Vector3f[] c = {new Vector3f(u).add(v), new Vector3f(u).sub(v), new Vector3f(u).negate().sub(v), new Vector3f(v).sub(u)};
        float u0 = LUZ[0] / TW, v0 = LUZ[1] / TH, u1 = LUZ[2] / TW, v1 = LUZ[3] / TH;
        for (int i = 0; i < 4; i++) {
            Vector3f p0 = new Vector3f(a).add(c[i]), p1 = new Vector3f(b).add(c[i]);
            Vector3f p2 = new Vector3f(b).add(c[(i + 1) % 4]), p3 = new Vector3f(a).add(c[(i + 1) % 4]);
            m.quad(p0, p1, p2, p3, u0, v0, u1, v1);
            m.quad(p3, p2, p1, p0, u0, v0, u1, v1);
        }
    }
}
