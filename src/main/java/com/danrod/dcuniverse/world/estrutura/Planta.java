package com.danrod.dcuniverse.world.estrutura;

/**
 * Projeto de uma construção desenhada em código. Ocupa x em [0, largura), z em [0, fundo) e y em
 * [abaixo, altura); o chão fica em y = 0 e a frente em z = 0. Tudo que a planta sortear deve sair de
 * {@link Obra#rand} (na mesma ordem sempre) ou de {@link Obra#ruido}, para os chunks combinarem.
 */
public interface Planta {
    int largura();

    int fundo();

    int altura();

    /** Quanto a obra desce abaixo do chão (porões, fundações). */
    default int abaixo() {
        return 4;
    }

    void construir(Obra o);
}
