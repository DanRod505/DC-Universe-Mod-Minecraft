package com.danrod.dcuniverse.client.anim;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Uma animação de corpo inteiro no formato de animação do Blockbench (Bedrock: "animations" com "bones", cada osso com
 * "rotation" e "position" em quadros-chave por segundo). Os eixos são os do modelo Java: rotação em graus somada ou
 * imposta à do osso (x negativo levanta o braço para a frente) e posição em pixels (y para baixo).
 * <p>
 * Ossos do corpo: root (tudo, gira em volta dos pés), body (o tronco, gira na cintura levando cabeça e braços), head,
 * right_arm, left_arm, right_leg, left_leg; e as dobras right_forearm, left_forearm, right_shin, left_shin (rotação da
 * metade de baixo do braço ou da perna em volta do cotovelo ou do joelho, ver Dobra). Ossos da primeira pessoa:
 * fp_right_arm e fp_left_arm (pixels e graus em volta da posição de descanso do braço na tela) e fp_right_forearm e
 * fp_left_forearm (cotovelo).
 * <p>
 * Campos nossos (opcionais): "dcuniverse:entrada" e "dcuniverse:saida" (segundos para entrar e sair da animação,
 * misturando com a pose normal) e "dcuniverse:aditivos" (ossos cuja rotação soma à pose normal em vez de substituir;
 * padrão: root, body e head, para a cabeça continuar olhando para onde o jogador olha) e "dcuniverse:rastros" (por
 * ponta, right_arm, left_arm, right_leg ou left_leg: lista de [de, até] em segundos com rastro de luz).
 */
public final class Animacao {
    public static final Set<String> ADITIVOS_PADRAO = Set.of("root", "body", "head");

    /** Quadro-chave: tempo em segundos, valor e se a curva até o próximo é suave (catmullrom) ou reta. */
    record Quadro(float t, float[] v, boolean suave) {}

    /** Uma curva de 3 valores no tempo. */
    static final class Curva {
        final Quadro[] q;

        Curva(Quadro[] q) {
            this.q = q;
        }

        void em(float t, float[] out) {
            if (t <= q[0].t) {
                copiar(q[0].v, out);
                return;
            }
            int n = q.length;
            if (t >= q[n - 1].t) {
                copiar(q[n - 1].v, out);
                return;
            }
            int i = 0;
            while (i < n - 2 && t >= q[i + 1].t) i++;
            Quadro a = q[i], b = q[i + 1];
            float f = (t - a.t) / Math.max(1e-5f, b.t - a.t);
            if (a.suave || b.suave) {
                float[] p0 = q[Math.max(0, i - 1)].v, p3 = q[Math.min(n - 1, i + 2)].v;
                for (int k = 0; k < 3; k++) out[k] = catmull(p0[k], a.v[k], b.v[k], p3[k], f);
            } else {
                for (int k = 0; k < 3; k++) out[k] = a.v[k] + (b.v[k] - a.v[k]) * f;
            }
        }

        private static float catmull(float p0, float p1, float p2, float p3, float t) {
            float t2 = t * t, t3 = t2 * t;
            return 0.5f * (2 * p1 + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (-p0 + 3 * p1 - 3 * p2 + p3) * t3);
        }

        private static void copiar(float[] v, float[] out) {
            out[0] = v[0];
            out[1] = v[1];
            out[2] = v[2];
        }
    }

    public final String nome;
    public final float duracao;
    public final boolean repete;
    public final float entrada, saida;
    /** Altura (pixels do modelo, 0 = cabeça, 24 = pés) em volta da qual o osso root gira: "dcuniverse:pivo_raiz". */
    float pivoRaiz = 24;
    final Map<String, Curva> rotacao = new HashMap<>();
    final Map<String, Curva> posicao = new HashMap<>();
    final Set<String> aditivos;
    /** Trechos (segundos de/até) em que cada ponta (Rastros.PONTAS) deixa rastro: os golpes de verdade. */
    final Map<String, float[][]> rastros = new HashMap<>();

    private Animacao(String nome, float duracao, boolean repete, float entrada, float saida, Set<String> aditivos) {
        this.nome = nome;
        this.duracao = duracao;
        this.repete = repete;
        this.entrada = entrada;
        this.saida = saida;
        this.aditivos = aditivos;
    }

    /** A ponta deixa rastro no tempo t. */
    boolean rastro(String ponta, float t) {
        float[][] r = rastros.get(ponta);
        if (r == null) return false;
        for (float[] tr : r) if (t >= tr[0] && t <= tr[1]) return true;
        return false;
    }

    public boolean tem(String osso) {
        return rotacao.containsKey(osso) || posicao.containsKey(osso);
    }

    /** Rotação do osso em graus no tempo t; false se a animação não mexe na rotação dele. */
    boolean rotacao(String osso, float t, float[] out) {
        Curva c = rotacao.get(osso);
        if (c == null) return false;
        c.em(t, out);
        return true;
    }

    boolean posicao(String osso, float t, float[] out) {
        Curva c = posicao.get(osso);
        if (c == null) return false;
        c.em(t, out);
        return true;
    }

    /** Lê todas as animações de um arquivo (o objeto "animations" do Blockbench). */
    static Map<String, Animacao> ler(JsonObject arquivo) {
        Map<String, Animacao> out = new HashMap<>();
        JsonObject anims = arquivo.getAsJsonObject("animations");
        for (var en : anims.entrySet()) {
            JsonObject j = en.getValue().getAsJsonObject();
            float dur = j.has("animation_length") ? j.get("animation_length").getAsFloat() : 1f;
            boolean repete = j.has("loop") && j.get("loop").isJsonPrimitive() && j.get("loop").getAsJsonPrimitive().isBoolean() && j.get("loop").getAsBoolean();
            float entrada = j.has("dcuniverse:entrada") ? j.get("dcuniverse:entrada").getAsFloat() : 0.05f;
            float saida = j.has("dcuniverse:saida") ? j.get("dcuniverse:saida").getAsFloat() : 0.15f;
            Set<String> aditivos = ADITIVOS_PADRAO;
            if (j.has("dcuniverse:aditivos")) {
                List<String> l = new ArrayList<>();
                for (JsonElement e : j.getAsJsonArray("dcuniverse:aditivos")) l.add(e.getAsString());
                aditivos = Set.copyOf(l);
            }
            Animacao a = new Animacao(en.getKey(), dur, repete, entrada, saida, aditivos);
            if (j.has("dcuniverse:pivo_raiz")) a.pivoRaiz = j.get("dcuniverse:pivo_raiz").getAsFloat();
            if (j.has("dcuniverse:rastros")) {
                for (var r : j.getAsJsonObject("dcuniverse:rastros").entrySet()) {
                    JsonArray l = r.getValue().getAsJsonArray();
                    float[][] trechos = new float[l.size()][];
                    for (int i = 0; i < l.size(); i++) trechos[i] = new float[] {l.get(i).getAsJsonArray().get(0).getAsFloat(), l.get(i).getAsJsonArray().get(1).getAsFloat()};
                    a.rastros.put(r.getKey(), trechos);
                }
            }
            JsonObject ossos = j.has("bones") ? j.getAsJsonObject("bones") : new JsonObject();
            for (var o : ossos.entrySet()) {
                JsonObject osso = o.getValue().getAsJsonObject();
                if (osso.has("rotation")) a.rotacao.put(o.getKey(), curva(osso.get("rotation")));
                if (osso.has("position")) a.posicao.put(o.getKey(), curva(osso.get("position")));
            }
            out.put(en.getKey(), a);
        }
        return out;
    }

    /** Valor fixo ([x,y,z]) ou quadros-chave {"0.1": [x,y,z] | {"post": [x,y,z], "lerp_mode": "catmullrom"}}. */
    private static Curva curva(JsonElement e) {
        List<Quadro> qs = new ArrayList<>();
        if (e.isJsonArray()) {
            qs.add(new Quadro(0, vetor(e.getAsJsonArray()), false));
        } else {
            for (var en : e.getAsJsonObject().entrySet()) {
                float t = Float.parseFloat(en.getKey());
                JsonElement v = en.getValue();
                if (v.isJsonArray()) {
                    qs.add(new Quadro(t, vetor(v.getAsJsonArray()), false));
                } else {
                    JsonObject o = v.getAsJsonObject();
                    JsonArray arr = o.has("post") ? o.getAsJsonArray("post") : o.getAsJsonArray("pre");
                    boolean suave = o.has("lerp_mode") && o.get("lerp_mode").getAsString().equals("catmullrom");
                    qs.add(new Quadro(t, vetor(arr), suave));
                }
            }
        }
        qs.sort((a, b) -> Float.compare(a.t, b.t));
        return new Curva(qs.toArray(new Quadro[0]));
    }

    private static float[] vetor(JsonArray a) {
        return new float[] {a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
    }
}
