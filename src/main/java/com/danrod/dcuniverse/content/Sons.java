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

    private Sons() {}

    private static RegistryObject<SoundEvent> som(String nome) {
        return SONS.register(nome, () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(DCUniverse.MODID, nome)));
    }
}
