package com.danrod.dcuniverse.client.anim;

import com.danrod.dcuniverse.DCUniverse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Animador do jogador: toca as animações de assets/dcuniverse/animacoes/*.json (formato do Blockbench, ver
 * {@link Animacao}) no modelo de qualquer jogador, por cima da pose normal (mixin PlayerModelMixin), e nos braços da
 * primeira pessoa (client/Combate). O relógio é a idade da entidade em ticks: a animação segue a câmera lenta do Modo
 * Diretor e o replay do Flashback. O hit-stop congela o relógio de uma entidade por alguns ticks.
 * <p>
 * Trocar de animação no meio mistura as duas (a anterior continua tocando enquanto a nova entra), então os golpes de
 * um combo emendam sem pulos.
 */
public final class Animador {
    private static final Map<String, Animacao> ANIMACOES = new HashMap<>();
    private static boolean carregado;
    private static final Map<Integer, Tocando> TOCANDO = new ConcurrentHashMap<>();

    /** Uma animação tocando numa entidade. inicio e congelamentos na idade da entidade, em ticks. */
    static final class Tocando {
        final Animacao a;
        final float inicio;
        Tocando anterior;
        float congeladoDe = -1, congeladoAte = -1, congeladoAntes;

        Tocando(Animacao a, float inicio) {
            this.a = a;
            this.inicio = inicio;
        }

        /** Segundos de animação na idade age, sem contar o tempo congelado. */
        float tempo(float age) {
            float parado = congeladoAntes;
            if (congeladoDe >= 0) parado += Mth.clamp(age, congeladoDe, congeladoAte) - congeladoDe;
            return Math.max(0, age - inicio - parado) / 20f;
        }

        float peso(float t) {
            float w = a.entrada <= 0 ? 1 : Math.min(1, t / a.entrada);
            if (!a.repete && a.saida > 0) w = Math.min(w, (a.duracao - t) / a.saida);
            return Mth.clamp(w, 0, 1);
        }

        boolean acabou(float t) {
            return !a.repete && t >= a.duracao;
        }
    }

    private Animador() {}

    /** F3+T (recarregar recursos) relê as animações: dá para ajustar um JSON do Blockbench com o jogo aberto. */
    public static void registrar() {
        DCUniverse.modBus().addListener(net.neoforged.neoforge.client.event.AddClientReloadListenersEvent.class, e ->
            e.addListener(net.minecraft.resources.Identifier.fromNamespaceAndPath(DCUniverse.MODID, "animacoes"),
                (net.minecraft.server.packs.resources.ResourceManagerReloadListener) rm -> {
                    ANIMACOES.clear();
                    carregado = false;
                }));
    }

    public static void limpar() {
        TOCANDO.clear();
        BASES.clear();
        Rastros.limpar();
        ANIMACOES.clear();
        carregado = false;
    }

    public static Animacao animacao(String nome) {
        if (!carregado) carregar();
        return ANIMACOES.get(nome);
    }

    private static void carregar() {
        carregado = true;
        var rm = Minecraft.getInstance().getResourceManager();
        var achados = rm.listResources("animacoes", id -> id.getNamespace().equals(DCUniverse.MODID) && id.getPath().endsWith(".json"));
        for (var en : achados.entrySet()) {
            try (Reader r = en.getValue().openAsReader()) {
                JsonObject j = JsonParser.parseReader(r).getAsJsonObject();
                ANIMACOES.putAll(Animacao.ler(j));
            } catch (Exception ex) {
                DCUniverse.LOGGER.error("Animação inválida em {}", en.getKey(), ex);
            }
        }
    }

    /** Idade da entidade agora (ticks + fração do quadro), o mesmo relógio que chega no ageInTicks do desenho. */
    private static float idade(Entity e) {
        return e.tickCount + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }

    /** Começa a animação na entidade (a que estava tocando sai misturando). */
    public static void tocar(Entity e, String nome) {
        Animacao a = animacao(nome);
        if (a == null) {
            DCUniverse.LOGGER.warn("Animação desconhecida: {}", nome);
            return;
        }
        Tocando novo = new Tocando(a, idade(e));
        Tocando velho = TOCANDO.get(e.getId());
        if (velho != null) {
            velho.anterior = null;
            novo.anterior = velho;
        }
        TOCANDO.put(e.getId(), novo);
    }

    public static void parar(Entity e) {
        TOCANDO.remove(e.getId());
    }

    /** Nome da animação tocando na entidade agora ("" = nenhuma). */
    public static String tocando(Entity e) {
        Tocando t = TOCANDO.get(e.getId());
        return t == null || t.acabou(t.tempo(idade(e))) ? "" : t.a.nome;
    }

    /** A entidade tem uma animação tocando (pelo número dela, como no estado de desenho). */
    public static boolean ativo(int entidade) {
        return TOCANDO.containsKey(entidade);
    }

    /** Hit-stop: congela a animação da entidade por alguns ticks a partir de agora. */
    public static void congelar(Entity e, float ticks) {
        Tocando t = TOCANDO.get(e.getId());
        if (t == null) return;
        float agora = idade(e);
        if (t.congeladoDe >= 0) t.congeladoAntes += Mth.clamp(agora, t.congeladoDe, t.congeladoAte) - t.congeladoDe;
        t.congeladoDe = agora;
        t.congeladoAte = agora + ticks;
    }

    /** A entidade está num hit-stop agora. */
    public static boolean congelado(Entity e) {
        Tocando t = TOCANDO.get(e.getId());
        if (t == null) return false;
        float agora = idade(e);
        return agora >= t.congeladoDe && agora < t.congeladoAte;
    }

    // ------------------------------------------------------------------ postura (camada de baixo)

    /** Animação em laço por baixo dos golpes (a guarda do modo combate): entra e sai misturando. */
    static final class Base {
        final Tocando t;
        boolean ligada = true;
        float mudou;

        Base(Tocando t, float mudou) {
            this.t = t;
            this.mudou = mudou;
        }

        float peso(float age) {
            float d = (age - mudou) / 20f;
            float w = ligada ? d / Math.max(0.01f, t.a.entrada) : 1 - d / Math.max(0.01f, t.a.saida);
            return Mth.clamp(w, 0, 1);
        }
    }

    private static final Map<Integer, Base> BASES = new ConcurrentHashMap<>();

    /** Liga a postura (laço) na entidade, ou desliga com null. */
    public static void postura(Entity e, String nome) {
        Base b = BASES.get(e.getId());
        float agora = idade(e);
        if (nome == null) {
            if (b != null && b.ligada) {
                b.mudou = agora - (1 - b.peso(agora)) * b.t.a.saida * 20;
                b.ligada = false;
            }
            return;
        }
        if (b != null && b.ligada && b.t.a.nome.equals(nome)) return;
        Animacao a = animacao(nome);
        if (a == null) return;
        BASES.put(e.getId(), new Base(new Tocando(a, agora), agora));
    }

    /** Postura ligada na entidade agora. */
    public static String postura(Entity e) {
        Base b = BASES.get(e.getId());
        return b == null || !b.ligada ? "" : b.t.a.nome;
    }

    // ------------------------------------------------------------------ corpo (terceira pessoa)

    private static final float[] V = new float[3];
    private static final String[] DOBRAS = {"right_forearm", "left_forearm", "right_shin", "left_shin"};

    /**
     * Depois da animação normal do modelo: a postura por baixo, a animação anterior (se ainda misturando) e a atual.
     * pontas: anotar o rastro dos golpes (só o corpo do jogador; a armadura passa por aqui também, sem rastro).
     */
    public static void aplicar(HumanoidModel<?> m, AvatarRenderState s, boolean pontas) {
        ModelPart[] membros = {m.rightArm, m.leftArm, m.rightLeg, m.leftLeg};
        for (ModelPart p : membros) Dobra.zerar(p);
        Base b = BASES.get(s.id);
        Tocando tc = TOCANDO.get(s.id);
        float wAcao = 0;
        if (tc != null) {
            float t = tc.tempo(s.ageInTicks);
            if (tc.acabou(t) || s.ageInTicks + 1 < tc.inicio) {
                // Acabou, ou a entidade voltou no tempo (replay rebobinado): some.
                TOCANDO.remove(s.id, tc);
                tc = null;
            } else {
                wAcao = tc.peso(t);
            }
        }
        if (b != null) {
            float wb = b.peso(s.ageInTicks);
            if (!b.ligada && wb <= 0 || s.ageInTicks + 1 < b.mudou) {
                BASES.remove(s.id, b);
            } else if (wb > 0.001f) {
                float tb = b.t.tempo(s.ageInTicks);
                // Andando, as pernas são as do Minecraft; parado, as da guarda.
                float pernas = 1 - Mth.clamp(s.walkAnimationSpeed * 3f, 0, 1);
                aplicar(m, b.t.a, tb % b.t.a.duracao, wb * (1 - 0.85f * wAcao), pernas, false, s.id);
            }
        }
        if (tc == null) return;
        float t = tc.tempo(s.ageInTicks);
        Tocando ant = tc.anterior;
        if (ant != null) {
            float ta = ant.tempo(s.ageInTicks);
            if (wAcao >= 1 || ant.acabou(ta)) tc.anterior = null;
            else aplicar(m, ant.a, ant.a.repete ? ta % ant.a.duracao : ta, ant.peso(ta), 1, false, s.id);
        }
        aplicar(m, tc.a, tc.a.repete ? t % tc.a.duracao : t, wAcao, 1, pontas, s.id);
    }

    private static void aplicar(HumanoidModel<?> m, Animacao a, float t, float w, float pernas, boolean pontas, int id) {
        if (w <= 0.001f) return;
        osso(m.head, a, "head", t, w);
        osso(m.rightArm, a, "right_arm", t, w);
        osso(m.leftArm, a, "left_arm", t, w);
        osso(m.rightLeg, a, "right_leg", t, w * pernas);
        osso(m.leftLeg, a, "left_leg", t, w * pernas);
        ModelPart[] membros = {m.rightArm, m.leftArm, m.rightLeg, m.leftLeg};
        for (int i = 0; i < 4; i++) {
            if (!a.rotacao(DOBRAS[i], t, V)) continue;
            float wi = i >= 2 ? w * pernas : w;
            float[] d = Dobra.de(membros[i]);
            Dobra.definir(membros[i], Mth.lerp(wi, d[0], V[0] * Mth.DEG_TO_RAD), Mth.lerp(wi, d[1], V[1] * Mth.DEG_TO_RAD),
                Mth.lerp(wi, d[2], V[2] * Mth.DEG_TO_RAD));
        }
        if (pontas) {
            // Anota a ponta durante toda a animação que tem rastro (o trecho do golpe acende; o quadro anterior liga a fita).
            for (int i = 0; i < 4; i++) {
                if (a.rastros.containsKey(Rastros.PONTAS.get(i))) Dobra.rastrear(membros[i], id, i + (a.rastro(Rastros.PONTAS.get(i), t) ? 8 : 0));
            }
        }
        // Tronco: gira na cintura levando a cabeça e os braços junto.
        ModelPart[] cima = {m.body, m.head, m.rightArm, m.leftArm};
        Vector3f cintura = new Vector3f(m.body.x, m.body.y + 12, m.body.z);
        if (a.posicao("body", t, V)) mover(cima, V, w);
        if (a.rotacao("body", t, V)) {
            // A cabeça vai junto com o tronco, mas continua olhando para o alvo: desfaz o giro horizontal nela.
            m.head.yRot -= V[1] * w * Mth.DEG_TO_RAD;
            girar(cima, V, w, cintura);
        }
        // Raiz: o corpo todo, girando em volta da cintura (giros e cambalhotas) ou dos pés (inclinar).
        ModelPart[] tudo = {m.body, m.head, m.rightArm, m.leftArm, m.rightLeg, m.leftLeg};
        if (a.posicao("root", t, V)) mover(tudo, V, w);
        if (a.rotacao("root", t, V)) girar(tudo, V, w, new Vector3f(0, a.pivoRaiz, 0));
    }

    private static void osso(ModelPart p, Animacao a, String nome, float t, float w) {
        if (a.rotacao(nome, t, V)) {
            float x = V[0] * Mth.DEG_TO_RAD, y = V[1] * Mth.DEG_TO_RAD, z = V[2] * Mth.DEG_TO_RAD;
            if (a.aditivos.contains(nome)) {
                p.xRot += x * w;
                p.yRot += y * w;
                p.zRot += z * w;
            } else {
                p.xRot = Mth.lerp(w, p.xRot, x);
                p.yRot = Mth.lerp(w, p.yRot, y);
                p.zRot = Mth.lerp(w, p.zRot, z);
            }
        }
        if (a.posicao(nome, t, V)) {
            p.x += V[0] * w;
            p.y += V[1] * w;
            p.z += V[2] * w;
        }
    }

    private static void mover(ModelPart[] ps, float[] v, float w) {
        for (ModelPart p : ps) {
            p.x += v[0] * w;
            p.y += v[1] * w;
            p.z += v[2] * w;
        }
    }

    /** Gira as peças juntas em volta do pivô (graus x, y, z vezes o peso), como filhas de um mesmo osso. */
    private static void girar(ModelPart[] ps, float[] graus, float w, Vector3f pivo) {
        Quaternionf d = new Quaternionf().rotationZYX(graus[2] * w * Mth.DEG_TO_RAD, graus[1] * w * Mth.DEG_TO_RAD, graus[0] * w * Mth.DEG_TO_RAD);
        Vector3f off = new Vector3f(), e = new Vector3f();
        Quaternionf q = new Quaternionf();
        for (ModelPart p : ps) {
            off.set(p.x - pivo.x, p.y - pivo.y, p.z - pivo.z);
            d.transform(off);
            p.x = pivo.x + off.x;
            p.y = pivo.y + off.y;
            p.z = pivo.z + off.z;
            q.set(d).mul(new Quaternionf().rotationZYX(p.zRot, p.yRot, p.xRot));
            q.getEulerAnglesZYX(e);
            p.xRot = e.x;
            p.yRot = e.y;
            p.zRot = e.z;
        }
    }

    // ------------------------------------------------------------------ primeira pessoa

    /**
     * Pose de um braço da primeira pessoa (osso fp_right_arm ou fp_left_arm) do jogador local agora: posição em
     * pixels e rotação em graus, já misturadas pelo peso. Devolve o peso (0 = a animação não mexe nesse braço).
     */
    public static float primeiraPessoa(Entity e, String osso, float[] pos, float[] rot) {
        pos[0] = pos[1] = pos[2] = rot[0] = rot[1] = rot[2] = 0;
        float age = idade(e);
        float peso = 0, w = 0;
        Tocando tc = TOCANDO.get(e.getId());
        if (tc != null && tc.acabou(tc.tempo(age))) tc = null;
        if (tc != null) w = tc.peso(tc.tempo(age));
        // A guarda por baixo (modo combate), sumindo enquanto um golpe toca.
        Base b = BASES.get(e.getId());
        if (b != null && b.t.a.tem(osso)) {
            float wb = b.peso(age) * (1 - w);
            if (wb > 0.001f) peso = Math.max(peso, somar(b.t.a, osso, b.t.tempo(age) % b.t.a.duracao, wb, pos, rot));
        }
        if (tc == null) return peso;
        float t = tc.tempo(age);
        Tocando ant = tc.anterior;
        if (ant != null && w < 1) {
            float ta = ant.tempo(age);
            if (!ant.acabou(ta) && ant.a.tem(osso)) peso = Math.max(peso, somar(ant.a, osso, ant.a.repete ? ta % ant.a.duracao : ta, ant.peso(ta) * (1 - w), pos, rot));
        }
        if (tc.a.tem(osso)) peso = Math.max(peso, somar(tc.a, osso, tc.a.repete ? t % tc.a.duracao : t, w, pos, rot));
        return peso;
    }

    /** Dobra do cotovelo da primeira pessoa (osso fp_right_forearm ou fp_left_forearm) em graus; false = reto. */
    public static boolean cotoveloPrimeiraPessoa(Entity e, String osso, float[] rot) {
        float[] pos = new float[3];
        return primeiraPessoa(e, osso, pos, rot) > 0.001f;
    }

    private static float somar(Animacao a, String osso, float t, float w, float[] pos, float[] rot) {
        if (a.posicao(osso, t, V)) for (int i = 0; i < 3; i++) pos[i] += V[i] * w;
        if (a.rotacao(osso, t, V)) for (int i = 0; i < 3; i++) rot[i] += V[i] * w;
        return w;
    }
}
