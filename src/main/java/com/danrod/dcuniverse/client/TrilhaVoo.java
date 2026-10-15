package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.DCUniverse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/**
 * Trilha do voo (música original, gerada por tools/arte/trilha_voo.py): quatro camadas do mesmo tamanho tocam
 * juntas em loop, em sincronia, e cada uma sobe ou desce de volume conforme a velocidade. Calma e espaçada devagar,
 * ganha um piano suave no cruzeiro, cordas, arpejo e tambor no boost e o tema épico com guitarras perto da máxima; ao
 * quebrar a barreira do som toca um impacto. Usa o volume de Música do jogo e cala a música normal enquanto toca.
 */
public final class TrilhaVoo {
    private static final SoundEvent[] CAMADAS = new SoundEvent[4];
    private static final SoundEvent ESTRONDO = evento("musica.voo_estrondo");
    /** Quanto o volume de cada camada anda por tick: sobe em ~4 s, desce em ~6 s. */
    private static final float SOBE = 0.0125f, DESCE = 0.008f;

    static {
        for (int i = 0; i < CAMADAS.length; i++) CAMADAS[i] = evento("musica.voo_" + i);
    }

    private static final Camada[] TOCANDO = new Camada[CAMADAS.length];
    private static final float[] VOLUME = new float[CAMADAS.length];

    private TrilhaVoo() {}

    private static SoundEvent evento(String nome) {
        return SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(DCUniverse.MODID, nome));
    }

    /** A cada tick: frac é a velocidade do voo em fração da máxima (0 quando não está voando). */
    static void tick(Minecraft mc, boolean voando, double frac) {
        float[] alvo = new float[CAMADAS.length];
        if (voando) {
            alvo[0] = 1f - 0.35f * suave(0.6, 1.0, frac);
            // No cruzeiro o piano entra baixinho; ele só cresce de vez junto com o boost.
            alvo[1] = 0.6f * suave(0.08, 0.26, frac) + 0.4f * suave(0.4, 0.66, frac);
            alvo[2] = suave(0.4, 0.66, frac);
            alvo[3] = suave(0.74, 0.93, frac);
        }
        boolean algum = false;
        for (int i = 0; i < CAMADAS.length; i++) {
            float d = alvo[i] - VOLUME[i];
            VOLUME[i] += Mth.clamp(d, -DESCE, SOBE);
            algum |= VOLUME[i] > 0.001f || alvo[i] > 0;
        }
        boolean tocando = TOCANDO[0] != null;
        if (tocando) {
            for (Camada c : TOCANDO) {
                // Se o motor de som parou alguma (recarregou os sons, por exemplo), recomeça todas juntas.
                if (c.isStopped() || !mc.getSoundManager().isActive(c)) {
                    parar();
                    tocando = false;
                    break;
                }
            }
        }
        if (algum && !tocando && voando) {
            for (int i = 0; i < CAMADAS.length; i++) {
                TOCANDO[i] = new Camada(CAMADAS[i], i);
                mc.getSoundManager().play(TOCANDO[i]);
            }
        } else if (!algum && tocando) {
            parar();
        }
        // A música normal do jogo fica calada enquanto a trilha toca.
        if (TOCANDO[0] != null) mc.getMusicManager().stopPlaying();
    }

    /** Impacto ao quebrar a barreira do som (só para quem voa). */
    static void estrondo(Minecraft mc) {
        if (TOCANDO[0] != null) mc.getSoundManager().play(SimpleSoundInstance.forMusic(ESTRONDO));
    }

    static void limpar() {
        parar();
        java.util.Arrays.fill(VOLUME, 0);
    }

    private static void parar() {
        for (int i = 0; i < TOCANDO.length; i++) {
            if (TOCANDO[i] != null) TOCANDO[i].parar = true;
            TOCANDO[i] = null;
        }
    }

    private static float suave(double de, double ate, double x) {
        float t = (float) Mth.clamp((x - de) / (ate - de), 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static final class Camada extends AbstractTickableSoundInstance {
        private final int indice;
        boolean parar;

        Camada(SoundEvent som, int indice) {
            super(som, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            this.indice = indice;
            this.looping = true;
            this.delay = 0;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
            this.volume = VOLUME[indice];
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            if (parar) {
                stop();
                return;
            }
            volume = VOLUME[indice];
        }
    }
}
