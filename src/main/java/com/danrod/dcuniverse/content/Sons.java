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

    private Sons() {}

    private static RegistryObject<SoundEvent> som(String nome) {
        return SONS.register(nome, () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(DCUniverse.MODID, nome)));
    }
}
