package com.danrod.dcuniverse.content;

import com.danrod.dcuniverse.DCUniverse;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Efeitos sonoros das habilidades (sintetizados por tools/arte/sons.py, listados em sounds.json). */
public final class Sons {
    public static final DeferredRegister<SoundEvent> SONS = DeferredRegister.create(Registries.SOUND_EVENT, DCUniverse.MODID);

    public static final RegistryObject<SoundEvent> VISAO_CALOR = som("habilidade.visao_calor");
    public static final RegistryObject<SoundEvent> SOPRO = som("habilidade.sopro");
    public static final RegistryObject<SoundEvent> SOCO_SONICO = som("habilidade.soco_sonico");
    public static final RegistryObject<SoundEvent> PISAO = som("habilidade.pisao");
    public static final RegistryObject<SoundEvent> RAIO_X = som("habilidade.raio_x");
    public static final RegistryObject<SoundEvent> FORTALEZA_CRESCE = som("habilidade.fortaleza_cresce");
    public static final RegistryObject<SoundEvent> FORTALEZA_QUEBRA = som("habilidade.fortaleza_quebra");
    public static final RegistryObject<SoundEvent> SALTO_CARGA = som("habilidade.salto_carga");
    public static final RegistryObject<SoundEvent> SALTO = som("habilidade.salto");
    public static final RegistryObject<SoundEvent> RICOCHETE = som("habilidade.ricochete");
    // Batman (v0.2): gancho, batarangues e Batwing
    public static final RegistryObject<SoundEvent> GANCHO_DISPARO = som("habilidade.gancho_disparo");
    public static final RegistryObject<SoundEvent> GANCHO_PUXA = som("habilidade.gancho_puxa");
    public static final RegistryObject<SoundEvent> GANCHO_IMPULSO = som("habilidade.gancho_impulso");
    public static final RegistryObject<SoundEvent> BATARANGUE_ARREMESSO = som("habilidade.batarangue_arremesso");
    public static final RegistryObject<SoundEvent> BATARANGUE_GIRO = som("habilidade.batarangue_giro");
    public static final RegistryObject<SoundEvent> BATARANGUE_ACERTA = som("habilidade.batarangue_acerta");
    public static final RegistryObject<SoundEvent> BATARANGUE_PEGA = som("habilidade.batarangue_pega");
    public static final RegistryObject<SoundEvent> CHOQUE = som("habilidade.choque");
    public static final RegistryObject<SoundEvent> BATWING_CHEGA = som("habilidade.batwing_chega");
    public static final RegistryObject<SoundEvent> BATWING_MOTOR = som("habilidade.batwing_motor");
    public static final RegistryObject<SoundEvent> BATWING_TIRO = som("habilidade.batwing_tiro");
    public static final RegistryObject<SoundEvent> BATWING_MISSIL = som("habilidade.batwing_missil");
    public static final RegistryObject<SoundEvent> BATWING_FURTIVO = som("habilidade.batwing_furtivo");
    public static final RegistryObject<SoundEvent> BATWING_REVELA = som("habilidade.batwing_revela");
    public static final RegistryObject<SoundEvent> BATWING_SUPERAQUECE = som("habilidade.batwing_superaquece");
    public static final RegistryObject<SoundEvent> CAPA_ABRE = som("habilidade.capa_abre");
    public static final RegistryObject<SoundEvent> CAPA_FECHA = som("habilidade.capa_fecha");
    public static final RegistryObject<SoundEvent> CAPA_VENTO = som("habilidade.capa_vento");
    public static final RegistryObject<SoundEvent> CAPA_IMPULSO = som("habilidade.capa_impulso");
    public static final RegistryObject<SoundEvent> CAPA_ARREMETIDA = som("habilidade.capa_arremetida");
    public static final RegistryObject<SoundEvent> POUSO_ROLAMENTO = som("habilidade.pouso_rolamento");

    // Batman (v0.2, etapa 3): modo detetive, fumaça, gel explosivo e contra-ataque
    public static final RegistryObject<SoundEvent> DETETIVE_LIGA = som("habilidade.detetive_liga");
    public static final RegistryObject<SoundEvent> DETETIVE_DESLIGA = som("habilidade.detetive_desliga");
    public static final RegistryObject<SoundEvent> FUMACA = som("habilidade.fumaca");
    public static final RegistryObject<SoundEvent> GEL_APLICA = som("habilidade.gel_aplica");
    public static final RegistryObject<SoundEvent> GEL_BIPE = som("habilidade.gel_bipe");
    public static final RegistryObject<SoundEvent> GEL_DETONA = som("habilidade.gel_detona");
    public static final RegistryObject<SoundEvent> CONTRA_ATAQUE = som("habilidade.contra_ataque");

    // Batman (v0.2): veículos da garagem (Batmóvel, Batmoto, Batbarco)
    public static final RegistryObject<SoundEvent> BATMOVEL_MOTOR = som("habilidade.batmovel_motor");
    public static final RegistryObject<SoundEvent> BATMOVEL_CHEGA = som("habilidade.batmovel_chega");
    public static final RegistryObject<SoundEvent> BATMOTO_MOTOR = som("habilidade.batmoto_motor");
    public static final RegistryObject<SoundEvent> BATMOTO_CHEGA = som("habilidade.batmoto_chega");
    public static final RegistryObject<SoundEvent> BATBARCO_MOTOR = som("habilidade.batbarco_motor");
    public static final RegistryObject<SoundEvent> BATBARCO_EMERGE = som("habilidade.batbarco_emerge");
    public static final RegistryObject<SoundEvent> BATBARCO_MERGULHA = som("habilidade.batbarco_mergulha");
    public static final RegistryObject<SoundEvent> BATBARCO_TORPEDO = som("habilidade.batbarco_torpedo");
    public static final RegistryObject<SoundEvent> VEICULO_PNEU = som("habilidade.veiculo_pneu");
    public static final RegistryObject<SoundEvent> VEICULO_TURBO = som("habilidade.veiculo_turbo");
    public static final RegistryObject<SoundEvent> VEICULO_ATROPELA = som("habilidade.veiculo_atropela");

    // v0.3 (etapa 4): Supergirl e Superboy
    public static final RegistryObject<SoundEvent> INVESTIDA = som("habilidade.investida");
    public static final RegistryObject<SoundEvent> TELECINESE_ERGUE = som("habilidade.telecinese_ergue");
    public static final RegistryObject<SoundEvent> TELECINESE_LANCA = som("habilidade.telecinese_lanca");
    public static final RegistryObject<SoundEvent> TELECINESE_POUSA = som("habilidade.telecinese_pousa");
    public static final RegistryObject<SoundEvent> ESCUDO_TK_LIGA = som("habilidade.escudo_tk_liga");
    public static final RegistryObject<SoundEvent> ESCUDO_TK_DESLIGA = som("habilidade.escudo_tk_desliga");
    public static final RegistryObject<SoundEvent> ESCUDO_TK_IMPACTO = som("habilidade.escudo_tk_impacto");
    public static final RegistryObject<SoundEvent> ILHA_ARRANCA = som("habilidade.ilha_arranca");
    public static final RegistryObject<SoundEvent> ILHA_MOTOR = som("habilidade.ilha_motor");
    public static final RegistryObject<SoundEvent> ILHA_POUSA = som("habilidade.ilha_pousa");
    public static final RegistryObject<SoundEvent> NAVE_CHEGA = som("habilidade.nave_chega");
    public static final RegistryObject<SoundEvent> NAVE_MOTOR = som("habilidade.nave_motor");
    public static final RegistryObject<SoundEvent> NAVE_CANHAO = som("habilidade.nave_canhao");
    public static final RegistryObject<SoundEvent> NAVE_ORBE = som("habilidade.nave_orbe");
    public static final RegistryObject<SoundEvent> NAVE_ESCUDO_LIGA = som("habilidade.nave_escudo_liga");
    public static final RegistryObject<SoundEvent> NAVE_ESCUDO_DESLIGA = som("habilidade.nave_escudo_desliga");

    private Sons() {}

    // Mulher-Maravilha (v0.5): braceletes, laço e arma arremessável que volta
    public static final RegistryObject<SoundEvent> BRACELETES_ERGUE = som("habilidade.braceletes_ergue");
    public static final RegistryObject<SoundEvent> BLOQUEIO = som("habilidade.bloqueio");
    public static final RegistryObject<SoundEvent> BLOQUEIO_PERFEITO = som("habilidade.bloqueio_perfeito");
    public static final RegistryObject<SoundEvent> BLOQUEIO_QUEBRA = som("habilidade.bloqueio_quebra");
    public static final RegistryObject<SoundEvent> LACO_ARREMESSO = som("habilidade.laco_arremesso");
    public static final RegistryObject<SoundEvent> LACO_GIRO = som("habilidade.laco_giro");
    public static final RegistryObject<SoundEvent> LACO_PRENDE = som("habilidade.laco_prende");
    public static final RegistryObject<SoundEvent> LACO_PUXA = som("habilidade.laco_puxa");
    public static final RegistryObject<SoundEvent> LACO_SOLTA = som("habilidade.laco_solta");
    public static final RegistryObject<SoundEvent> ARMA_ARREMESSO = som("habilidade.arma_arremesso");
    public static final RegistryObject<SoundEvent> ARMA_GIRO = som("habilidade.arma_giro");
    public static final RegistryObject<SoundEvent> ARMA_ACERTA = som("habilidade.arma_acerta");
    public static final RegistryObject<SoundEvent> ARMA_PEGA = som("habilidade.arma_pega");
    // Mulher-Maravilha, etapa 3: aliado do laço, braceletes, grito, escudo, espada e Jato Invisível
    public static final RegistryObject<SoundEvent> ALIADO = som("habilidade.aliado");
    public static final RegistryObject<SoundEvent> BRACELETES_CARGA = som("habilidade.braceletes_carga");
    public static final RegistryObject<SoundEvent> BRACELETES_CHOQUE = som("habilidade.braceletes_choque");
    public static final RegistryObject<SoundEvent> GRITO = som("habilidade.grito");
    public static final RegistryObject<SoundEvent> ESCUDO_INVESTIDA = som("habilidade.escudo_investida");
    public static final RegistryObject<SoundEvent> ESPADA_SACA = som("habilidade.espada_saca");
    public static final RegistryObject<SoundEvent> ESPADA_GUARDA = som("habilidade.espada_guarda");
    public static final RegistryObject<SoundEvent> ESPADA_GOLPE = som("habilidade.espada_golpe");
    public static final RegistryObject<SoundEvent> ESPADA_GIRO = som("habilidade.espada_giro");
    public static final RegistryObject<SoundEvent> JATO_CHEGA = som("habilidade.jato_chega");
    public static final RegistryObject<SoundEvent> JATO_MOTOR = som("habilidade.jato_motor");
    public static final RegistryObject<SoundEvent> JATO_RAMPA = som("habilidade.jato_rampa");
    public static final RegistryObject<SoundEvent> JATO_EMBARQUE = som("habilidade.jato_embarque");
    public static final RegistryObject<SoundEvent> JATO_MOSTRA = som("habilidade.jato_mostra");
    public static final RegistryObject<SoundEvent> MERGULHO_POUSO = som("habilidade.mergulho_pouso");

    private static RegistryObject<SoundEvent> som(String nome) {
        return SONS.register(nome, () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(DCUniverse.MODID, nome)));
    }
}
