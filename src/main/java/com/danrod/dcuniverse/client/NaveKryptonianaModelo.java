package com.danrod.dcuniverse.client;

import org.joml.Vector3f;

import java.util.TreeSet;

/**
 * Geometria da nave kryptoniana da Supergirl, em blocos e já no tamanho final, com o nariz para +z e a esquerda do
 * piloto em +x (coordenadas a partir dos pés da entidade). O casco é uma gota alongada feita de estações ao longo
 * de z, cada uma uma superelipse (ângulo p: 0 em cima, pi/2 no lado esquerdo, pi embaixo); a cúpula de vidro é a
 * mesma superfície estufada para cima entre os dois aros, então vidro e casco dividem os vértices e não sobra
 * fresta entre eles. Dentro, a banheira da cabine com os dois assentos em tandem e o painel; atrás, as três
 * aletas de cristal (dorsal e duas laterais baixas, uma estrela de três pontas vista de trás) e o anel do motor;
 * embaixo do nariz, a lente dourada do canhão solar; em cima do nariz, o brasão da Casa de El.
 * Textura em textures/entity/nave_kryptoniana.png (1024x512, tools/arte/nave_kryptoniana.py): casco em
 * (0,0)-(512,512) com u = (3,3 - z) / 6,1 e v = p / 2pi; as outras áreas estão nas constantes abaixo.
 */
public final class NaveKryptonianaModelo {
    /** Altura do ponto em que o desenho gira (guinada, arfagem e rolagem). */
    public static final float PIVO_Y = 0.8f;
    /** Centro da lente do canhão solar (o feixe sai daqui para +z). */
    public static final Vector3f CANHAO = new Vector3f(0, 0.55f, 3.25f);
    /** Centro e semi-eixos do elipsoide do escudo. */
    public static final Vector3f ESCUDO_CENTRO = new Vector3f(0, 1.0f, -0.3f), ESCUDO_RAIO = new Vector3f(3.75f, 2.75f, 5.0f);
    /** Fundo do bocal (onde nasce a chama) e a altura do eixo do motor. */
    public static final float MOTOR_Z = -2.74f, MOTOR_Y = 0.96f;
    private static final float TW = 1024, TH = 512;

    /** Casco, aletas, cabine por dentro, assentos, brasão, canhão e motor (opacos). */
    public static final Malha CASCO = new Malha();
    /** A cúpula de cristal (translúcida). */
    public static final Malha VIDRO = new Malha();
    /** Partes que brilham devagar (veios de cristal, friso da cúpula, aletas, brasão, telas): mesma UV do casco. */
    public static final Malha BRILHO = new Malha();
    /** Anel e fundo do motor (brilho conforme a velocidade). */
    public static final Malha MOTOR = new Malha();
    /** Lente do canhão (brilha mais quando atira). */
    public static final Malha LENTE = new Malha();
    /** Chama do motor com 1 bloco de comprimento a partir de MOTOR_Z para -z (o renderer estica). */
    public static final Malha CHAMA = new Malha();
    /** Casca do escudo: geodésica sobre o elipsoide, cada triângulo numa célula da textura de hexágonos. */
    public static final Malha ESCUDO = new Malha();

    // ---------------------------------------------------------------- perfil do casco
    static final float Z_NARIZ = 3.3f, Z_MAX = 0.2f, Z_CAUDA = -2.8f;
    // ------------------------------------------- carenagem da cabine: gota vista de cima, mais alta em CAB_Z. O vidro
    // é a parte da frente (z >= Z_VIDRO); atrás vira a lombada de metal que leva à aleta dorsal.
    static final float CAB_Z = 0.6f, CAB_FRENTE = 1.9f, CAB_TRAS = 3.25f, CAB_L = 0.8f, CAB_H = 1.0f, Z_VIDRO = -1.1f;
    /** Espessura do casco na borda da cabine. */
    static final float PAREDE = 0.07f;
    static final int N_CASCO = 24, N_VIDRO = 12;

    /** Veios de cristal (z, p) do lado esquerdo; o direito é o espelho (2pi - p). A textura desenha os mesmos. */
    static final float[][][] VEIAS = {
        {{3.0f, 0.62f}, {2.25f, 1.02f}, {1.0f, 1.1f}, {-0.9f, 1.08f}, {-1.7f, 0.86f}, {-2.6f, 0.62f}},
        {{3.08f, 1.5f}, {1.6f, 1.58f}, {-2.7f, 1.6f}},
        {{2.45f, 2.3f}, {0.4f, 2.18f}, {-1.0f, 2.06f}, {-2.6f, 1.98f}},
    };
    /** Anéis de cristal (z) em volta do corpo. */
    static final float[] ANEIS = {-2.55f, 3.07f};

    // ---------------------------------------------------------------- áreas da textura (pixels)
    static final float[] ALETA = {512, 0, 768, 128}, DENTRO = {768, 0, 1024, 128};
    static final float[] ASSENTO = {512, 128, 576, 192}, ENCOSTO = {576, 128, 640, 192}, PAINEL = {640, 128, 704, 192},
        TELA = {640, 192, 704, 224}, BRASAO = {704, 128, 800, 224}, OURO = {800, 128, 864, 192}, METAL = {864, 128, 928, 192},
        BOCAL = {928, 128, 992, 160}, ANEL = {928, 160, 992, 176}, TORO = {992, 160, 1024, 192}, FUNDO = {928, 192, 992, 256},
        FRISO = {512, 192, 640, 200}, VIDRO_UV = {512, 256, 768, 384}, FOGO = {768, 256, 896, 320}, LUZ_FUNDO = {992, 192, 1024, 256},
        LUZ_ANEL = {800, 192, 864, 208}, LUZ_LENTE = {864, 192, 928, 256};

    private NaveKryptonianaModelo() {}

    static {
        casco();
        aletas();
        assentos();
        brasao();
        canhao();
        motor();
        escudo();
    }

    // ================================================================= formas

    private static float uNariz(float z) {
        return Math.min(1, (z - Z_MAX) / (Z_NARIZ - Z_MAX));
    }

    private static float uCauda(float z) {
        return Math.min(1, (Z_MAX - z) / (Z_MAX - Z_CAUDA));
    }

    /** Medida da seção: ogiva no nariz (ponta arredondada) e afinando em cosseno até o anel do motor. */
    private static float medida(float z, float max, float fim) {
        if (z >= Z_MAX) {
            float u = uNariz(z);
            return max * (float) Math.pow(Math.max(0, 1 - u * u), 0.7);
        }
        float g = (float) Math.pow(Math.cos(Math.PI / 2 * uCauda(z)), 1.2);
        return fim + (max - fim) * g;
    }

    static float largura(float z) {
        return medida(z, 1.3f, 0.5f);
    }

    static float alto(float z) {
        return medida(z, 0.66f, 0.4f);
    }

    static float baixo(float z) {
        return medida(z, 0.62f, 0.4f);
    }

    /** Altura da quina lateral: o nariz baixa um pouco, a cauda sobe até o eixo do motor. */
    static float centro(float z) {
        if (z >= Z_MAX) {
            float u = uNariz(z);
            return 0.88f - 0.08f * u * u;
        }
        return 0.88f + 0.08f * (0.5f - 0.5f * (float) Math.cos(Math.PI * uCauda(z)));
    }

    /**
     * Ponto do casco no ângulo p da estação z (sem a carenagem). Seção de lente: dorso e barriga em arcos que se
     * encontram numa quina macia dos lados (a barriga mais chata).
     */
    static Vector3f secao(float p, float z) {
        float s = (float) Math.sin(p), c = (float) Math.cos(p);
        float x = largura(z) * s;
        float y = centro(z) + (c >= 0 ? alto(z) * (float) Math.pow(c, 1.1) : -baixo(z) * (float) Math.pow(-c, 1.5));
        return new Vector3f(x, y, z);
    }

    private static float uCab(float z) {
        return (z - CAB_Z) / (z >= CAB_Z ? CAB_FRENTE : CAB_TRAS);
    }

    /** Meia largura da carenagem na estação z (0 fora dela). */
    static float cabLargura(float z) {
        float u = uCab(z);
        return Math.abs(u) >= 1 ? 0 : CAB_L * (float) Math.sqrt(1 - u * u);
    }

    /** Quanto a carenagem sobe acima do casco no meio da estação z. */
    static float cabAltura(float z) {
        float u = uCab(z);
        if (Math.abs(u) >= 1) return 0;
        if (u >= 0) return CAB_H * (float) Math.pow(1 - u * u, 0.5);
        return CAB_H * (float) Math.pow(Math.cos(Math.PI / 2 * -u), 0.9);
    }

    /** Ângulo do aro da carenagem (onde o casco tem x = cabLargura). */
    static float aro(float z) {
        float w = cabLargura(z);
        if (w <= 0) return 0;
        return (float) Math.asin(Math.min(0.98f, w / largura(z)));
    }

    /** Ponto da carenagem: o casco estufado em elipse entre os aros (no aro a altura extra é zero). */
    static Vector3f vidro(float p, float z) {
        Vector3f q = secao(p, z);
        float w = cabLargura(z);
        if (w > 0) {
            float t = q.x / w;
            q.y += cabAltura(z) * (float) Math.pow(Math.max(0, 1 - t * t), 0.7);
        }
        return q;
    }

    /** Altura de cima do casco no ponto (x, z) (para apoiar peças). */
    static float topo(float x, float z) {
        float s = Math.min(1, Math.abs(x) / largura(z));
        return centro(z) + alto(z) * (float) Math.pow(Math.sqrt(1 - s * s), 1.1);
    }

    /** Normal lisa por diferenças: dP/dz x dP/dp aponta para fora. */
    private static Vector3f normal(float p, float z, boolean car) {
        float e = 1e-3f;
        float z0 = Math.max(Z_CAUDA, z - e), z1 = Math.min(Z_NARIZ, z + e);
        Vector3f dz = ponto(p, z1, car).sub(ponto(p, z0, car));
        Vector3f dp = ponto(p + e, z, car).sub(ponto(p - e, z, car));
        Vector3f n = dz.cross(dp);
        if (n.lengthSquared() < 1e-14f) return new Vector3f(0, 0, z > 0 ? 1 : -1);
        return n.normalize();
    }

    private static Vector3f ponto(float p, float z, boolean car) {
        return car ? vidro(p, z) : secao(p, z);
    }

    // ================================================================= utilidades de malha

    private static Malha.V vx(Vector3f p, float u, float v, Vector3f n) {
        return new Malha.V(p.x, p.y, p.z, u / TW, v / TH, n.x, n.y, n.z);
    }

    /** Triângulo com a frente para o lado das normais dos vértices (troca a ordem se precisar). */
    private static void tri(Malha m, Malha.V a, Malha.V b, Malha.V c) {
        float ex = b.x() - a.x(), ey = b.y() - a.y(), ez = b.z() - a.z();
        float fx = c.x() - a.x(), fy = c.y() - a.y(), fz = c.z() - a.z();
        float nx = ey * fz - ez * fy, ny = ez * fx - ex * fz, nz = ex * fy - ey * fx;
        if (nx * nx + ny * ny + nz * nz < 1e-12f) return;
        float d = nx * (a.nx() + b.nx() + c.nx()) + ny * (a.ny() + b.ny() + c.ny()) + nz * (a.nz() + b.nz() + c.nz());
        if (d < 0) m.tri(a, c, b);
        else m.tri(a, b, c);
    }

    private static void quad(Malha m, Malha.V a, Malha.V b, Malha.V c, Malha.V d) {
        tri(m, a, b, c);
        tri(m, a, c, d);
    }

    /** Faceta chapada (normal da própria face) virada para o lado de "fora". */
    private static void faceta(Malha m, Vector3f a, float ua, float va, Vector3f b, float ub, float vb, Vector3f c, float uc, float vc,
                               Vector3f fora) {
        Vector3f n = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a));
        if (n.lengthSquared() < 1e-12f) return;
        n.normalize();
        if (n.dot(fora) < 0) {
            n.negate();
            m.tri(vx(a, ua, va, n), vx(c, uc, vc, n), vx(b, ub, vb, n));
        } else {
            m.tri(vx(a, ua, va, n), vx(b, ub, vb, n), vx(c, uc, vc, n));
        }
    }

    private static float lerp(float[] r, int i, float t) {
        return r[i] + (r[i + 2] - r[i]) * t;
    }

    // ================================================================= casco, cúpula e cabine

    private static float[] estacoes() {
        TreeSet<Float> s = new TreeSet<>();
        for (float z = Z_CAUDA; z < 2.85f; z += 0.28f) s.add(z);
        float zf = CAB_Z + CAB_FRENTE, zt = CAB_Z - CAB_TRAS;
        float[] fixas = {2.9f, 3.04f, 3.15f, 3.23f, 3.28f, Z_NARIZ, CAB_Z, Z_VIDRO,
            zf, CAB_Z + 0.97f * CAB_FRENTE, CAB_Z + 0.9f * CAB_FRENTE, CAB_Z + 0.75f * CAB_FRENTE, CAB_Z + 0.45f * CAB_FRENTE,
            zt, CAB_Z - 0.97f * CAB_TRAS, CAB_Z - 0.9f * CAB_TRAS};
        for (float f : fixas) s.removeIf(z -> z > Z_CAUDA + 0.01f && Math.abs(z - f) < 0.1f);
        for (float f : fixas) s.add(f);
        float[] r = new float[s.size()];
        int i = 0;
        for (float z : s) r[i++] = z;
        return r;
    }

    private static boolean temVeia(float z0, float z1, float p0, float p1) {
        float mz = 0.07f, mp = 0.07f;
        for (float a : ANEIS) if (a > z0 - mz && a < z1 + mz) return true;
        for (float[][] veia : VEIAS) {
            for (int k = 0; k + 1 < veia.length; k++) {
                for (int lado = 0; lado < 2; lado++) {
                    float za = veia[k][0], zb = veia[k + 1][0];
                    float pa = lado == 0 ? veia[k][1] : (float) (2 * Math.PI) - veia[k][1];
                    float pb = lado == 0 ? veia[k + 1][1] : (float) (2 * Math.PI) - veia[k + 1][1];
                    if (Math.max(za, zb) < z0 - mz || Math.min(za, zb) > z1 + mz) continue;
                    float zl = Math.max(Math.min(za, zb), z0 - mz), zh = Math.min(Math.max(za, zb), z1 + mz);
                    float ta = (zl - za) / (zb - za), tb = (zh - za) / (zb - za);
                    float qa = pa + (pb - pa) * ta, qb = pa + (pb - pa) * tb;
                    if (Math.max(qa, qb) >= p0 - mp && Math.min(qa, qb) <= p1 + mp) return true;
                }
            }
        }
        return false;
    }

    /** UV do casco: u ao longo de z, v pelo ângulo (0 e 512 em cima, 256 embaixo). */
    private static float uCasco(float z) {
        return (Z_NARIZ - z) / (Z_NARIZ - Z_CAUDA) * 512;
    }

    private static void casco() {
        float[] zs = estacoes();
        int ne = zs.length;
        float dois = (float) (2 * Math.PI);
        Malha.V[][] fora = new Malha.V[ne][N_CASCO + 1];
        Vector3f[][] pos = new Vector3f[ne][N_CASCO + 1], nor = new Vector3f[ne][N_CASCO + 1];
        Vector3f[][] cpos = new Vector3f[ne][N_VIDRO + 1], cnor = new Vector3f[ne][N_VIDRO + 1];
        float[][] ps = new float[ne][N_CASCO + 1], cps = new float[ne][N_VIDRO + 1];
        for (int i = 0; i < ne; i++) {
            float z = zs[i], pr = aro(z);
            for (int j = 0; j <= N_CASCO; j++) {
                float p = pr + (dois - 2 * pr) * j / N_CASCO;
                ps[i][j] = p;
                pos[i][j] = secao(p, z);
                nor[i][j] = normal(p, z, false);
                fora[i][j] = vx(pos[i][j], uCasco(z), p / dois * 512, nor[i][j]);
            }
            for (int k = 0; k <= N_VIDRO; k++) {
                float p = -pr + 2 * pr * k / N_VIDRO;
                cps[i][k] = p;
                cpos[i][k] = vidro(p, z);
                cnor[i][k] = normal(p, z, true);
            }
        }
        float zf = CAB_Z + CAB_FRENTE;
        for (int i = 0; i + 1 < ne; i++) {
            for (int j = 0; j < N_CASCO; j++) {
                quad(CASCO, fora[i][j], fora[i + 1][j], fora[i + 1][j + 1], fora[i][j + 1]);
                float p0 = Math.min(ps[i][j], ps[i + 1][j]), p1 = Math.max(ps[i][j + 1], ps[i + 1][j + 1]);
                if (temVeia(zs[i], zs[i + 1], p0, p1)) quad(BRILHO, fora[i][j], fora[i + 1][j], fora[i + 1][j + 1], fora[i][j + 1]);
            }
            boolean deVidro = zs[i] >= Z_VIDRO - 1e-4f;
            for (int k = 0; k < N_VIDRO; k++) {
                Malha.V[] q = new Malha.V[4];
                int[][] id = {{i, k}, {i + 1, k}, {i + 1, k + 1}, {i, k + 1}};
                // a metade de p < 0 usa v perto de 512, a outra perto de 0 (a costura fica no meio, em p = 0)
                boolean neg = k < N_VIDRO / 2;
                for (int w = 0; w < 4; w++) {
                    int a = id[w][0], b = id[w][1];
                    Vector3f pt = cpos[a][b];
                    float u, v;
                    if (deVidro) {
                        u = lerp(VIDRO_UV, 0, (zf - zs[a]) / (zf - Z_VIDRO));
                        v = lerp(VIDRO_UV, 1, (float) b / N_VIDRO);
                    } else {
                        u = uCasco(zs[a]);
                        v = (neg ? cps[a][b] + dois : cps[a][b]) / dois * 512;
                    }
                    q[w] = vx(pt, u, v, cnor[a][b]);
                }
                if (deVidro) {
                    quad(VIDRO, q[0], q[1], q[2], q[3]);
                } else {
                    quad(CASCO, q[0], q[1], q[2], q[3]);
                    // friso aceso logo atrás do vidro e o veio da lombada
                    float pa = neg ? cps[i][k] + dois : cps[i][k];
                    if (Math.abs(zs[i + 1] - Z_VIDRO) < 1e-4f || temVeia(zs[i], zs[i + 1], pa - 0.05f, pa + 0.1f)) {
                        quad(BRILHO, q[0], q[1], q[2], q[3]);
                    }
                }
            }
        }

        // Banheira da cabine: o casco por dentro (afastado PAREDE para dentro), o friso que fecha a borda e as
        // duas anteparas nas pontas. Só aparece pela cúpula.
        int ia = -1, ib = -1;
        for (int i = 0; i < ne; i++) {
            if (Math.abs(zs[i] - Z_VIDRO) < 1e-4f) ia = i;
            if (Math.abs(zs[i] - zf) < 1e-4f) ib = i;
        }
        int passo = 2, nd = N_CASCO / passo;
        Malha.V[][] den = new Malha.V[ne][nd + 1];
        Vector3f[][] dpos = new Vector3f[ne][nd + 1];
        for (int i = ia; i <= ib; i++) {
            for (int j = 0; j <= nd; j++) {
                Vector3f n = nor[i][j * passo];
                Vector3f q = new Vector3f(pos[i][j * passo]).sub(new Vector3f(n).mul(PAREDE));
                dpos[i][j] = q;
                den[i][j] = vx(q, lerp(DENTRO, 0, (zf - zs[i]) / (zf - Z_VIDRO)), lerp(DENTRO, 1, ps[i][j * passo] / dois),
                    new Vector3f(n).negate());
            }
        }
        for (int i = ia; i < ib; i++) for (int j = 0; j < nd; j++) quad(CASCO, den[i][j], den[i + 1][j], den[i + 1][j + 1], den[i][j + 1]);
        Vector3f up = new Vector3f(0, 1, 0);
        for (int i = ia; i < ib; i++) {
            for (int lado = 0; lado < 2; lado++) {
                int jf = lado == 0 ? 0 : N_CASCO, jd = lado == 0 ? 0 : nd;
                float fu0 = lerp(FRISO, 0, (float) (i - ia) / (ib - ia)), fu1 = lerp(FRISO, 0, (float) (i + 1 - ia) / (ib - ia));
                Malha.V a = vx(pos[i][jf], fu0, FRISO[1], up), b = vx(pos[i + 1][jf], fu1, FRISO[1], up);
                Malha.V c = vx(dpos[i + 1][jd], fu1, FRISO[3], up), d = vx(dpos[i][jd], fu0, FRISO[3], up);
                quad(CASCO, a, b, c, d);
                quad(BRILHO, a, b, c, d);
            }
        }
        // Antepara da frente (o anel de dentro inteiro) e a de trás (o anel de dentro mais o arco da carenagem).
        float cu = (DENTRO[0] + DENTRO[2]) / 2, cv = (DENTRO[1] + DENTRO[3]) / 2;
        for (int lado = 0; lado < 2; lado++) {
            int i = lado == 0 ? ia : ib;
            Vector3f n = new Vector3f(0, 0, lado == 0 ? 1 : -1);
            java.util.List<Vector3f> anel = new java.util.ArrayList<>();
            for (int j = 0; j <= nd; j++) anel.add(dpos[i][j]);
            if (lado == 0) for (int k = 0; k <= N_VIDRO; k++) anel.add(cpos[i][k]);
            Vector3f c = new Vector3f();
            for (Vector3f q : anel) c.add(q);
            c.div(anel.size());
            for (int j = 0; j < anel.size(); j++) {
                Vector3f a = anel.get(j), b = anel.get((j + 1) % anel.size());
                tri(CASCO, vx(c, cu, cv, n), vx(a, cu - 6, cv - 20, n), vx(b, cu + 6, cv - 20, n));
            }
        }
        painel();
    }

    /** Painel na frente do piloto: placa inclinada com a tela de cristal acesa. */
    private static void painel() {
        float z0 = 1.52f, z1 = 1.86f, yb = 0.98f, yt = 1.3f, w = 0.5f;
        Vector3f a = new Vector3f(w, yt, z0), b = new Vector3f(-w, yt, z0), c = new Vector3f(-w * 1.1f, yb + 0.1f, z1), d = new Vector3f(w * 1.1f, yb + 0.1f, z1);
        Vector3f e = new Vector3f(w, yb, z0 + 0.05f), f = new Vector3f(-w, yb, z0 + 0.05f);
        Vector3f tras = new Vector3f(0, 0.7f, -1);
        faceta(CASCO, a, PAINEL[0], PAINEL[1], b, PAINEL[2], PAINEL[1], f, PAINEL[2], PAINEL[3], new Vector3f(0, -0.2f, -1));
        faceta(CASCO, a, PAINEL[0], PAINEL[1], f, PAINEL[2], PAINEL[3], e, PAINEL[0], PAINEL[3], new Vector3f(0, -0.2f, -1));
        faceta(CASCO, a, PAINEL[0], PAINEL[1], d, PAINEL[0], PAINEL[3], c, PAINEL[2], PAINEL[3], new Vector3f(0, 1, 0.3f));
        faceta(CASCO, a, PAINEL[0], PAINEL[1], c, PAINEL[2], PAINEL[3], b, PAINEL[2], PAINEL[1], new Vector3f(0, 1, 0.3f));
        // Tela acesa, um pouco acima da face de cima.
        float o = 0.008f;
        Vector3f ta = new Vector3f(w * 0.8f, yt - 0.02f + o, z0 + 0.04f), tb = new Vector3f(-w * 0.8f, yt - 0.02f + o, z0 + 0.04f);
        Vector3f tc = new Vector3f(-w * 0.85f, yb + 0.13f + o, z1 - 0.05f), td = new Vector3f(w * 0.85f, yb + 0.13f + o, z1 - 0.05f);
        for (Malha m : new Malha[] {CASCO, BRILHO}) {
            faceta(m, ta, TELA[0], TELA[1], td, TELA[0], TELA[3], tc, TELA[2], TELA[3], new Vector3f(0, 1, 0.3f));
            faceta(m, ta, TELA[0], TELA[1], tc, TELA[2], TELA[3], tb, TELA[2], TELA[1], new Vector3f(0, 1, 0.3f));
        }
        // Telinha do passageiro nas costas do assento do piloto.
        float zt = 0.9f - 0.47f, y0 = 1.05f, y1 = 1.35f;
        Vector3f pa = new Vector3f(0.18f, y1, zt - 0.03f), pb = new Vector3f(-0.18f, y1, zt - 0.03f);
        Vector3f pc = new Vector3f(-0.2f, y0, zt + 0.02f), pd = new Vector3f(0.2f, y0, zt + 0.02f);
        for (Malha m : new Malha[] {CASCO, BRILHO}) {
            faceta(m, pa, TELA[0], TELA[1], pb, TELA[2], TELA[1], pc, TELA[2], TELA[3], tras);
            faceta(m, pa, TELA[0], TELA[1], pc, TELA[2], TELA[3], pd, TELA[0], TELA[3], tras);
        }
    }

    /** Caixa de 8 cantos (a-d embaixo, e-h em cima, mesma ordem), com a mesma área da textura nas faces. */
    private static void bloco(Malha m, Vector3f[] k, float[] r) {
        int[][] faces = {{0, 1, 2, 3}, {4, 5, 6, 7}, {0, 1, 5, 4}, {1, 2, 6, 5}, {2, 3, 7, 6}, {3, 0, 4, 7}};
        Vector3f c = new Vector3f();
        for (Vector3f p : k) c.add(p);
        c.div(8);
        for (int[] f : faces) {
            Vector3f fc = new Vector3f(k[f[0]]).add(k[f[1]]).add(k[f[2]]).add(k[f[3]]).div(4);
            Vector3f fora = fc.sub(c);
            faceta(m, k[f[0]], r[0], r[3], k[f[1]], r[2], r[3], k[f[2]], r[2], r[1], fora);
            faceta(m, k[f[0]], r[0], r[3], k[f[2]], r[2], r[1], k[f[3]], r[0], r[1], fora);
        }
    }

    private static Vector3f[] caixa(float x0, float y0, float z0, float x1, float y1, float z1) {
        return new Vector3f[] {new Vector3f(x0, y0, z0), new Vector3f(x1, y0, z0), new Vector3f(x1, y0, z1), new Vector3f(x0, y0, z1),
            new Vector3f(x0, y1, z0), new Vector3f(x1, y1, z0), new Vector3f(x1, y1, z1), new Vector3f(x0, y1, z1)};
    }

    private static void assentos() {
        for (float z : new float[] {0.9f, -0.5f}) {
            bloco(CASCO, caixa(-0.16f, 0.4f, z - 0.2f, 0.16f, 0.52f, z + 0.12f), METAL);
            // Almofada (topo em 0,65) com a frente arredondada por um chanfro.
            bloco(CASCO, new Vector3f[] {new Vector3f(-0.3f, 0.5f, z - 0.32f), new Vector3f(0.3f, 0.5f, z - 0.32f),
                new Vector3f(0.3f, 0.5f, z + 0.22f), new Vector3f(-0.3f, 0.5f, z + 0.22f),
                new Vector3f(-0.3f, 0.65f, z - 0.32f), new Vector3f(0.3f, 0.65f, z - 0.32f),
                new Vector3f(0.28f, 0.63f, z + 0.3f), new Vector3f(-0.28f, 0.63f, z + 0.3f)}, ASSENTO);
            // Encosto inclinado para trás, mais estreito em cima, e o apoio de cabeça.
            bloco(CASCO, new Vector3f[] {new Vector3f(-0.3f, 0.6f, z - 0.42f), new Vector3f(0.3f, 0.6f, z - 0.42f),
                new Vector3f(0.3f, 0.6f, z - 0.3f), new Vector3f(-0.3f, 0.6f, z - 0.3f),
                new Vector3f(-0.25f, 1.55f, z - 0.55f), new Vector3f(0.25f, 1.55f, z - 0.55f),
                new Vector3f(0.25f, 1.55f, z - 0.43f), new Vector3f(-0.25f, 1.55f, z - 0.43f)}, ENCOSTO);
            bloco(CASCO, new Vector3f[] {new Vector3f(-0.15f, 1.58f, z - 0.6f), new Vector3f(0.15f, 1.58f, z - 0.6f),
                new Vector3f(0.15f, 1.58f, z - 0.48f), new Vector3f(-0.15f, 1.58f, z - 0.48f),
                new Vector3f(-0.13f, 1.86f, z - 0.64f), new Vector3f(0.13f, 1.86f, z - 0.64f),
                new Vector3f(0.13f, 1.86f, z - 0.53f), new Vector3f(-0.13f, 1.86f, z - 0.53f)}, ENCOSTO);
            bloco(CASCO, caixa(-0.05f, 1.52f, z - 0.56f, 0.05f, 1.6f, z - 0.5f), METAL);
        }
    }

    // ================================================================= aletas

    /** Raio do casco a partir do eixo (0, yA) na direção phi, na estação z. */
    private static float raio(float z, float phi, float yA) {
        float melhor = 1e9f, r = 0;
        for (int k = 0; k < 720; k++) {
            float p = (float) (2 * Math.PI * k / 720);
            Vector3f q = vidro(p, z);
            float a = (float) Math.atan2(q.y - yA, q.x);
            float d = (float) Math.abs(Math.atan2(Math.sin(a - phi), Math.cos(a - phi)));
            if (d < melhor) {
                melhor = d;
                r = (float) Math.hypot(q.x, q.y - yA);
            }
        }
        return r;
    }

    private static float bez(float a, float b, float c, float s) {
        return (1 - s) * (1 - s) * a + 2 * s * (1 - s) * b + s * s * c;
    }

    private static void aletas() {
        float lat = (float) Math.toRadians(25);
        aleta((float) (Math.PI / 2), -1.45f, -2.62f, 1.72f, -2.95f, -3.25f);
        aleta(-lat, -0.7f, -2.62f, 2.3f, -2.78f, -3.2f);
        aleta((float) Math.PI + lat, -0.7f, -2.62f, 2.3f, -2.78f, -3.2f);
    }

    /**
     * Aleta de cristal no plano que contém o eixo do motor e a direção phi: seção em losango (facetas chapadas),
     * bordo de ataque convexo e varrido para trás. A raiz entra no casco.
     */
    private static void aleta(float phi, float zA0, float zF0, float r1, float zA1, float zF1) {
        float yA = MOTOR_Y;
        Vector3f d = new Vector3f((float) Math.cos(phi), (float) Math.sin(phi), 0), t = new Vector3f(-d.y, d.x, 0);
        float rA0 = raio(zA0, phi, yA) - 0.07f, rF0 = raio(zF0, phi, yA) - 0.07f;
        int ns = 7;
        Vector3f[][] k = new Vector3f[ns + 1][4];
        float[] sv = new float[ns + 1];
        for (int i = 0; i <= ns; i++) {
            float s = (float) i / ns;
            sv[i] = s;
            float ra = bez(rA0, rA0 + 0.72f * (r1 - rA0), r1, s), za = bez(zA0, zA0 - 0.28f * (zA0 - zA1), zA1, s);
            float rf = bez(rF0, rF0 + 0.45f * (r1 - rF0), r1, s), zf = bez(zF0, zF0 - 0.12f, zF1, s);
            float rm = ra + (rf - ra) * 0.42f, zm = za + (zf - za) * 0.42f;
            float th = 0.085f * (1 - s) + 0.012f * s;
            Vector3f base = new Vector3f(0, yA, 0);
            k[i][0] = new Vector3f(base).add(new Vector3f(d).mul(ra)).add(0, 0, za);
            k[i][1] = new Vector3f(base).add(new Vector3f(d).mul(rm)).add(new Vector3f(t).mul(th)).add(0, 0, zm);
            k[i][2] = new Vector3f(base).add(new Vector3f(d).mul(rf)).add(0, 0, zf);
            k[i][3] = new Vector3f(base).add(new Vector3f(d).mul(rm)).add(new Vector3f(t).mul(-th)).add(0, 0, zm);
        }
        // UV: u pela corda (0 bordo de ataque, 0,42 a quina, 1 bordo de fuga), v pela envergadura.
        float[] cu = {0, 0.42f, 1, 0.42f};
        Vector3f tn = new Vector3f(t).negate();
        for (int i = 0; i < ns; i++) {
            float v0 = lerp(ALETA, 1, sv[i]), v1 = lerp(ALETA, 1, sv[i + 1]);
            for (int f = 0; f < 4; f++) {
                int g = (f + 1) % 4;
                Vector3f fora = (f == 0 || f == 1) ? t : tn;
                float ua = lerp(ALETA, 0, cu[f]), ub = lerp(ALETA, 0, cu[g]);
                for (Malha m : new Malha[] {CASCO, BRILHO}) {
                    faceta(m, k[i][f], ua, v0, k[i + 1][f], ua, v1, k[i + 1][g], ub, v1, fora);
                    faceta(m, k[i][f], ua, v0, k[i + 1][g], ub, v1, k[i][g], ub, v0, fora);
                }
            }
        }
        // Ponta.
        float vt = ALETA[3] - 1;
        for (Malha m : new Malha[] {CASCO, BRILHO}) {
            faceta(m, k[ns][0], ALETA[0], vt, k[ns][1], lerp(ALETA, 0, 0.42f), vt, k[ns][2], ALETA[2], vt, d);
            faceta(m, k[ns][0], ALETA[0], vt, k[ns][2], ALETA[2], vt, k[ns][3], lerp(ALETA, 0, 0.42f), vt, d);
        }
    }

    // ================================================================= brasão, canhão e motor

    /** Brasão da Casa de El em placa elevada no dorso do nariz (a ponta do escudo para a frente). */
    private static void brasao() {
        float zc = 2.84f, alt = 0.028f, fundo = 0.02f;
        float[][] contorno = {{-0.104f, -0.136f}, {0.104f, -0.136f}, {0.172f, -0.056f}, {0, 0.152f}, {-0.172f, -0.056f}};
        int sub = 3, n = contorno.length * sub;
        float[][] borda = new float[n][2];
        for (int i = 0; i < contorno.length; i++) {
            float[] a = contorno[i], b = contorno[(i + 1) % contorno.length];
            for (int k = 0; k < sub; k++) {
                float t = (float) k / sub;
                borda[i * sub + k] = new float[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t};
            }
        }
        Vector3f c = new Vector3f(0, topo(0, zc) + alt, zc);
        float cu = lerp(BRASAO, 0, 0.5f), cv = lerp(BRASAO, 1, 0.5f);
        Vector3f cima = new Vector3f(0, 1, 0.25f);
        for (int i = 0; i < n; i++) {
            float[] a = borda[i], b = borda[(i + 1) % n];
            Vector3f pa = new Vector3f(a[0], topo(a[0], zc + a[1]) + alt, zc + a[1]), pb = new Vector3f(b[0], topo(b[0], zc + b[1]) + alt, zc + b[1]);
            float ua = lerp(BRASAO, 0, 0.5f + a[0] / 0.4f), va = lerp(BRASAO, 1, 0.5f + a[1] / 0.4f);
            float ub = lerp(BRASAO, 0, 0.5f + b[0] / 0.4f), vb = lerp(BRASAO, 1, 0.5f + b[1] / 0.4f);
            for (Malha m : new Malha[] {CASCO, BRILHO}) faceta(m, c, cu, cv, pa, ua, va, pb, ub, vb, cima);
            // Lateral da placa, até dentro do casco.
            Vector3f qa = new Vector3f(pa.x, pa.y - alt - fundo, pa.z), qb = new Vector3f(pb.x, pb.y - alt - fundo, pb.z);
            Vector3f fora = new Vector3f((a[0] + b[0]) / 2, 0, (a[1] + b[1]) / 2);
            float gu = BRASAO[0] + 1, gv = BRASAO[1] + 1;
            faceta(CASCO, pa, gu, gv, qa, gu, gv + 1, qb, gu + 1, gv + 1, fora);
            faceta(CASCO, pa, gu, gv, qb, gu + 1, gv + 1, pb, gu + 1, gv, fora);
        }
    }

    /** Corpo de revolução em volta do eixo (0, y) ao longo de z: perfil (z, raio), do fundo para a frente. */
    private static void revolucao(Malha m, float y, float[][] perfil, float[] r, int n, boolean liso) {
        for (int i = 0; i + 1 < perfil.length; i++) {
            float z0 = perfil[i][0], r0 = perfil[i][1], z1 = perfil[i + 1][0], r1 = perfil[i + 1][1];
            float v0 = lerp(r, 1, (float) i / (perfil.length - 1)), v1 = lerp(r, 1, (float) (i + 1) / (perfil.length - 1));
            // normal do perfil (para fora e um pouco para a frente quando o raio diminui)
            float dz = z1 - z0, dr = r1 - r0;
            for (int k = 0; k < n; k++) {
                double a0 = 2 * Math.PI * k / n, a1 = 2 * Math.PI * (k + 1) / n;
                Vector3f p00 = new Vector3f(r0 * (float) Math.cos(a0), y + r0 * (float) Math.sin(a0), z0);
                Vector3f p01 = new Vector3f(r0 * (float) Math.cos(a1), y + r0 * (float) Math.sin(a1), z0);
                Vector3f p10 = new Vector3f(r1 * (float) Math.cos(a0), y + r1 * (float) Math.sin(a0), z1);
                Vector3f p11 = new Vector3f(r1 * (float) Math.cos(a1), y + r1 * (float) Math.sin(a1), z1);
                Vector3f n0 = new Vector3f(dz * (float) Math.cos(a0), dz * (float) Math.sin(a0), -dr).normalize();
                Vector3f n1 = new Vector3f(dz * (float) Math.cos(a1), dz * (float) Math.sin(a1), -dr).normalize();
                float u0 = lerp(r, 0, (float) k / n), u1 = lerp(r, 0, (float) (k + 1) / n);
                if (liso) {
                    quad(m, vx(p00, u0, v0, n0), vx(p10, u0, v1, n0), vx(p11, u1, v1, n1), vx(p01, u1, v0, n1));
                } else {
                    Vector3f nf = new Vector3f(n0).add(n1);
                    faceta(m, p00, u0, v0, p10, u0, v1, p11, u1, v1, nf);
                    faceta(m, p00, u0, v0, p11, u1, v1, p01, u1, v0, nf);
                }
            }
        }
    }

    private static void canhao() {
        float y = CANHAO.y;
        // Casulo prateado embaixo do nariz (a parte de trás fica dentro do casco).
        revolucao(CASCO, y, new float[][] {{2.45f, 0.06f}, {2.6f, 0.13f}, {2.85f, 0.165f}, {3.05f, 0.17f}, {3.15f, 0.16f}, {3.18f, 0.148f}},
            METAL, 12, true);
        // Aro dourado e a lente abaulada.
        revolucao(CASCO, y, new float[][] {{3.18f, 0.148f}, {3.2f, 0.135f}, {3.19f, 0.118f}}, OURO, 12, false);
        float[][] lente = {{3.19f, 0.118f}, {3.225f, 0.105f}, {3.25f, 0.075f}, {3.268f, 0.035f}, {3.272f, 0.001f}};
        revolucao(CASCO, y, lente, OURO, 12, true);
        revolucao(LENTE, y, lente, LUZ_LENTE, 12, true);
        revolucao(LENTE, y, new float[][] {{3.18f, 0.148f}, {3.2f, 0.135f}, {3.19f, 0.118f}}, LUZ_LENTE, 12, false);
    }

    private static Vector3f anel(float p, float escala, float z) {
        Vector3f q = secao(p, Z_CAUDA);
        return new Vector3f(q.x * escala, MOTOR_Y + (q.y - MOTOR_Y) * escala, z);
    }

    private static void motor() {
        int n = N_CASCO;
        float dois = (float) (2 * Math.PI);
        // (escala, z) do contorno: borda do casco, chanfro, anel aceso, parede do bocal e o fundo.
        float[][] passos = {{1.0f, Z_CAUDA}, {0.93f, -2.87f}, {0.8f, -2.87f}, {0.7f, MOTOR_Z}};
        float[][] areas = {BOCAL, ANEL, BOCAL};
        for (int s = 0; s + 1 < passos.length; s++) {
            for (int j = 0; j < n; j++) {
                float pa = dois * j / n, pb = dois * (j + 1) / n;
                Vector3f a = anel(pa, passos[s][0], passos[s][1]), b = anel(pb, passos[s][0], passos[s][1]);
                Vector3f c = anel(pb, passos[s + 1][0], passos[s + 1][1]), d = anel(pa, passos[s + 1][0], passos[s + 1][1]);
                float[] r = areas[s];
                float u0 = lerp(r, 0, (float) j / n), u1 = lerp(r, 0, (float) (j + 1) / n);
                Vector3f fora = s == 2 ? new Vector3f(new Vector3f(a).add(b).mul(-0.5f)).add(0, MOTOR_Y, 0).add(0, 0, -0.3f)
                    : new Vector3f(0, 0, -1);
                faceta(CASCO, a, u0, r[1], b, u1, r[1], c, u1, r[3], fora);
                faceta(CASCO, a, u0, r[1], c, u1, r[3], d, u0, r[3], fora);
                if (s == 1) {
                    faceta(MOTOR, a, lerp(LUZ_ANEL, 0, (float) j / n), LUZ_ANEL[1], b, lerp(LUZ_ANEL, 0, (float) (j + 1) / n), LUZ_ANEL[1],
                        c, lerp(LUZ_ANEL, 0, (float) (j + 1) / n), LUZ_ANEL[3], fora);
                    faceta(MOTOR, a, lerp(LUZ_ANEL, 0, (float) j / n), LUZ_ANEL[1], c, lerp(LUZ_ANEL, 0, (float) (j + 1) / n), LUZ_ANEL[3],
                        d, lerp(LUZ_ANEL, 0, (float) j / n), LUZ_ANEL[3], fora);
                }
            }
        }
        // Fundo do bocal: leque com UV radial.
        Vector3f c = new Vector3f(0, MOTOR_Y, MOTOR_Z), tras = new Vector3f(0, 0, -1);
        for (int j = 0; j < n; j++) {
            float pa = dois * j / n, pb = dois * (j + 1) / n;
            Vector3f a = anel(pa, 0.7f, MOTOR_Z), b = anel(pb, 0.7f, MOTOR_Z);
            for (int k = 0; k < 2; k++) {
                Malha m = k == 0 ? CASCO : MOTOR;
                float[] r = k == 0 ? FUNDO : LUZ_FUNDO;
                float cu = lerp(r, 0, 0.5f), cv = lerp(r, 1, 0.5f), ru = (r[2] - r[0]) / 2, rv = (r[3] - r[1]) / 2;
                faceta(m, c, cu, cv, a, cu + ru * (float) Math.sin(pa), cv - rv * (float) Math.cos(pa),
                    b, cu + ru * (float) Math.sin(pb), cv - rv * (float) Math.cos(pb), tras);
            }
        }
        // Toro de cristal sobre o anel aceso.
        int nt = 5;
        float rt = 0.05f, zt = -2.885f;
        for (int j = 0; j < n; j++) {
            float pa = dois * j / n, pb = dois * (j + 1) / n;
            Vector3f ca = anel(pa, 0.865f, zt), cb = anel(pb, 0.865f, zt);
            Vector3f ra = new Vector3f(ca.x, ca.y - MOTOR_Y, 0).normalize(), rb = new Vector3f(cb.x, cb.y - MOTOR_Y, 0).normalize();
            for (int k = 0; k < nt; k++) {
                double t0 = 2 * Math.PI * k / nt, t1 = 2 * Math.PI * (k + 1) / nt;
                Vector3f[] q = new Vector3f[4], nn = new Vector3f[4];
                Vector3f[] cs = {ca, cb, cb, ca};
                Vector3f[] rs = {ra, rb, rb, ra};
                double[] ts = {t0, t0, t1, t1};
                for (int w = 0; w < 4; w++) {
                    nn[w] = new Vector3f(rs[w]).mul((float) Math.cos(ts[w])).add(0, 0, (float) Math.sin(ts[w]));
                    q[w] = new Vector3f(cs[w]).add(new Vector3f(nn[w]).mul(rt));
                }
                for (int m = 0; m < 2; m++) {
                    Malha mm = m == 0 ? CASCO : MOTOR;
                    float[] r = m == 0 ? TORO : LUZ_ANEL;
                    float u0 = lerp(r, 0, (float) j / n), u1 = lerp(r, 0, (float) (j + 1) / n);
                    float v0 = lerp(r, 1, (float) k / nt), v1 = lerp(r, 1, (float) (k + 1) / nt);
                    quad(mm, vx(q[0], u0, v0, nn[0]), vx(q[1], u1, v0, nn[1]), vx(q[2], u1, v1, nn[2]), vx(q[3], u0, v1, nn[3]));
                }
            }
        }
        // Chama: três planos cruzados pelo eixo, de MOTOR_Z até 1 bloco para trás.
        for (int k = 0; k < 3; k++) {
            double a = Math.PI * k / 3;
            float cx = (float) Math.cos(a), cy = (float) Math.sin(a);
            float w0 = 0.34f, w1 = 0.06f;
            Vector3f p0 = new Vector3f(cx * w0, MOTOR_Y + cy * w0 * 0.85f, MOTOR_Z), p1 = new Vector3f(-cx * w0, MOTOR_Y - cy * w0 * 0.85f, MOTOR_Z);
            Vector3f p2 = new Vector3f(-cx * w1, MOTOR_Y - cy * w1, MOTOR_Z - 1), p3 = new Vector3f(cx * w1, MOTOR_Y + cy * w1, MOTOR_Z - 1);
            Vector3f nf = new Vector3f(-cy, cx, 0);
            for (int lado = 0; lado < 2; lado++) {
                Vector3f f = lado == 0 ? nf : new Vector3f(nf).negate();
                faceta(CHAMA, p0, FOGO[0], FOGO[3], p1, FOGO[0], FOGO[1], p2, FOGO[2], FOGO[1], f);
                faceta(CHAMA, p0, FOGO[0], FOGO[3], p2, FOGO[2], FOGO[1], p3, FOGO[2], FOGO[3], f);
            }
        }
    }

    // ================================================================= escudo

    private static void escudo() {
        float t = (float) ((1 + Math.sqrt(5)) / 2);
        float[][] v = {{-1, t, 0}, {1, t, 0}, {-1, -t, 0}, {1, -t, 0}, {0, -1, t}, {0, 1, t}, {0, -1, -t}, {0, 1, -t},
            {t, 0, -1}, {t, 0, 1}, {-t, 0, -1}, {-t, 0, 1}};
        int[][] f = {{0, 11, 5}, {0, 5, 1}, {0, 1, 7}, {0, 7, 10}, {0, 10, 11}, {1, 5, 9}, {5, 11, 4}, {11, 10, 2}, {10, 7, 6},
            {7, 1, 8}, {3, 9, 4}, {3, 4, 2}, {3, 2, 6}, {3, 6, 8}, {3, 8, 9}, {4, 9, 5}, {2, 4, 11}, {6, 2, 10}, {8, 6, 7}, {9, 8, 1}};
        int n = 6;
        // Célula da textura: triângulo (0,0)-(64,0)-(32,64) da trama de 256x256 (os hexágonos têm o centro nos cantos).
        float[][] uv = {{0, 0}, {0.25f, 0}, {0.125f, 0.25f}};
        for (int[] face : f) {
            Vector3f a = new Vector3f(v[face[0]][0], v[face[0]][1], v[face[0]][2]);
            Vector3f b = new Vector3f(v[face[1]][0], v[face[1]][1], v[face[1]][2]);
            Vector3f c = new Vector3f(v[face[2]][0], v[face[2]][1], v[face[2]][2]);
            for (int i = 0; i < n; i++) {
                for (int j = 0; i + j < n; j++) {
                    escudoTri(a, b, c, n, i, j, i + 1, j, i, j + 1, uv);
                    if (i + j + 1 < n) escudoTri(a, b, c, n, i + 1, j, i + 1, j + 1, i, j + 1, uv);
                }
            }
        }
    }

    private static Vector3f esfera(Vector3f a, Vector3f b, Vector3f c, int n, int i, int j) {
        Vector3f p = new Vector3f(a).mul((float) (n - i - j) / n).add(new Vector3f(b).mul((float) i / n)).add(new Vector3f(c).mul((float) j / n));
        p.normalize();
        return p;
    }

    private static void escudoTri(Vector3f a, Vector3f b, Vector3f c, int n, int i0, int j0, int i1, int j1, int i2, int j2, float[][] uv) {
        Vector3f[] s = {esfera(a, b, c, n, i0, j0), esfera(a, b, c, n, i1, j1), esfera(a, b, c, n, i2, j2)};
        Malha.V[] q = new Malha.V[3];
        for (int k = 0; k < 3; k++) {
            Vector3f p = new Vector3f(s[k]).mul(ESCUDO_RAIO).add(ESCUDO_CENTRO);
            Vector3f nn = new Vector3f(s[k].x / ESCUDO_RAIO.x, s[k].y / ESCUDO_RAIO.y, s[k].z / ESCUDO_RAIO.z).normalize();
            q[k] = new Malha.V(p.x, p.y, p.z, uv[k][0], uv[k][1], nn.x, nn.y, nn.z);
        }
        tri(ESCUDO, q[0], q[1], q[2]);
    }
}
