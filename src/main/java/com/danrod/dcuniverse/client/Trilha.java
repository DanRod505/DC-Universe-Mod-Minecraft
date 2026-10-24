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
 * Trilha dinâmica (música original, gerada por tools/arte/trilha_voo.py e trilha_planar.py): as camadas, do mesmo
 * tamanho, tocam juntas em loop e em sincronia, e cada uma sobe ou desce de volume conforme o alvo que quem usa
 * passa a cada tick (o voo e o planar decidem pela velocidade). O impacto toca por cima, uma vez. Usa o volume de
 * Música do jogo e cala a música normal enquanto toca.
 */
public final class Trilha {
    /** Voo (client/Voador): calma, piano, crescendo e o épico; o estrondo é a barreira do som. */
    static final Trilha VOO = new Trilha("voo", 4, "voo_estrondo");
    /** Planar (client/Planador): noite, piano gótico, tensão e o ápice nos metais; o golpe é a arremetida. */
    static final Trilha PLANAR = new Trilha("planar", 4, "planar_apice");
    /** Quanto o volume de cada camada anda por tick: sobe em ~4 s, desce em ~6 s. */
    private static final float SOBE = 0.0125f, DESCE = 0.008f;

    private static final Trilha[] TODAS = {VOO, PLANAR};

    private final SoundEvent[] camadas;
    private final SoundEvent impacto;
    private final Camada[] tocando;
    private final float[] volume;

    private Trilha(String nome, int n, String impacto) {
        camadas = new SoundEvent[n];
        for (int i = 0; i < n; i++) camadas[i] = evento("musica." + nome + "_" + i);
        this.impacto = evento("musica." + impacto);
        tocando = new Camada[n];
        volume = new float[n];
    }

    private static SoundEvent evento(String nome) {
        return SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(DCUniverse.MODID, nome));
    }

    /** A cada tick: o volume alvo de cada camada (null quando a trilha não deve tocar). */
    void tick(Minecraft mc, float[] alvo) {
        boolean ativa = alvo != null;
        boolean algum = false;
        for (int i = 0; i < camadas.length; i++) {
            float d = (ativa ? alvo[i] : 0) - volume[i];
            volume[i] += Mth.clamp(d, -DESCE, SOBE);
            algum |= volume[i] > 0.001f || ativa && alvo[i] > 0;
        }
        boolean toca = tocando[0] != null;
        if (toca) {
            for (Camada c : tocando) {
                // Se o motor de som parou alguma (recarregou os sons, por exemplo), recomeça todas juntas.
                if (c.isStopped() || !mc.getSoundManager().isActive(c)) {
                    parar();
                    toca = false;
                    break;
                }
            }
        }
        if (algum && !toca && ativa) {
            for (int i = 0; i < camadas.length; i++) {
                tocando[i] = new Camada(camadas[i], volume, i);
                mc.getSoundManager().play(tocando[i]);
            }
        } else if (!algum && toca) {
            parar();
        }
        // A música normal do jogo para quando a trilha começa (e o MusicManagerMixin não deixa voltar).
        if (tocando[0] != null && !toca) mc.getMusicManager().stopPlaying();
    }

    /** Alguma trilha está tocando (o MusicManagerMixin segura a música normal). */
    public static boolean tocando() {
        for (Trilha t : TODAS) if (t.tocando[0] != null) return true;
        return false;
    }

    /** O impacto por cima das camadas (só se a trilha está tocando). */
    void impacto(Minecraft mc) {
        if (tocando[0] != null) mc.getSoundManager().play(SimpleSoundInstance.forMusic(impacto));
    }

    void limpar() {
        parar();
        java.util.Arrays.fill(volume, 0);
    }

    private void parar() {
        for (int i = 0; i < tocando.length; i++) {
            if (tocando[i] != null) tocando[i].parar = true;
            tocando[i] = null;
        }
    }

    static float suave(double de, double ate, double x) {
        float t = (float) Mth.clamp((x - de) / (ate - de), 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static final class Camada extends AbstractTickableSoundInstance {
        private final float[] volumes;
        private final int indice;
        boolean parar;

        Camada(SoundEvent som, float[] volumes, int indice) {
            super(som, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            this.volumes = volumes;
            this.indice = indice;
            this.looping = true;
            this.delay = 0;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
            this.volume = volumes[indice];
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
            volume = volumes[indice];
        }
    }
}
