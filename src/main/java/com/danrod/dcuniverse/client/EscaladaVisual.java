package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.ability.active.Escalada;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Escalada Sísmica (ability/active/Escalada) no cliente do Superboy: ele mesmo faz o percurso que o servidor mandou,
 * como no voo. Até as plataformas subirem ele fica no chão; depois, três saltos em arco (agacha, sobe, pousa no topo
 * de cada uma e fica firme nela na pausa) e o mergulho: um impulso para cima e a descida acelerando até o chão, a
 * tempo da batida do servidor. Chamado no começo da física do jogador (client/Voador).
 */
public final class EscaladaVisual {
    private static Rede.Escalada atual;
    private static Escalada.Tempos tempos;
    private static int t;
    private static Vec3 de;
    private static int segmento = -1;

    private EscaladaVisual() {}

    public static void receber(Rede.Escalada m) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.getId() != m.entidade()) return;
        atual = m;
        tempos = new Escalada.Tempos(m.sobe(), m.salto(), m.pausa(), m.mergulho());
        t = 0;
        segmento = -1;
    }

    /** @return true quando o percurso está no comando do movimento neste tick */
    public static boolean mover(LocalPlayer p) {
        if (atual == null) return false;
        int agora = t++;
        if (agora > tempos.batida() + 2 || p.isSpectator() || p.isPassenger()) {
            atual = null;
            return false;
        }
        if (agora < tempos.inicio(0)) return false;
        p.resetFallDistance();
        for (int i = 0; i < 4; i++) {
            int ini = tempos.inicio(i), dur = tempos.duracao(i);
            if (agora < ini || agora >= ini + dur) continue;
            if (segmento != i) {
                segmento = i;
                de = p.position();
            }
            float u = (agora - ini + 1) / (float) dur;
            Vec3 alvo = i == 3 ? mergulho(de, atual.pontos()[3], u) : arco(de, atual.pontos()[i], u, 1.6 + 0.3 * i);
            p.setDeltaMovement(alvo.subtract(p.position()));
            if (i == 3 && agora == ini + dur - 1) Voador.tremer(12);
            return true;
        }
        // Na pausa entre um salto e outro: firme no topo da plataforma (a terra flutuante não tem colisão).
        int ultimo = -1;
        for (int i = 0; i < 3; i++) if (agora >= tempos.inicio(i) + tempos.salto()) ultimo = i;
        if (ultimo < 0) return false;
        if (ultimo == 0 && agora == tempos.inicio(0) + tempos.salto()) Voador.tremer(3);
        Vec3 topo = atual.pontos()[ultimo];
        p.setDeltaMovement(topo.subtract(p.position()).scale(0.6));
        return true;
    }

    /** Salto em arco de a até b: sobe acima da reta e desce no topo (u de 0 a 1). */
    static Vec3 arco(Vec3 a, Vec3 b, float u, double altura) {
        double s = u * u * (3 - 2 * u);
        return a.lerp(b, s).add(0, altura * 4 * u * (1 - u), 0);
    }

    /** O mergulho: um pequeno impulso para cima no começo e a descida acelerando até o ponto da batida. */
    static Vec3 mergulho(Vec3 a, Vec3 b, float u) {
        double s = Math.pow(u, 1.8);
        return a.lerp(b, s).add(0, 2.2 * Mth.sin(Mth.PI * Math.min(1f, u * 1.3f)) * (1 - u), 0);
    }

    public static void limpar() {
        atual = null;
    }
}
