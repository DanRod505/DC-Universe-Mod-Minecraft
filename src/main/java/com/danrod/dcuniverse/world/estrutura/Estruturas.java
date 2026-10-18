package com.danrod.dcuniverse.world.estrutura;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.world.smallville.Smallville;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Sistema de estruturas geradas no mundo: um tipo de estrutura ({@code dcuniverse:local}) e um tipo de
 * peça ({@code dcuniverse:obra}) servem para todos os locais do mod. Cada local novo só registra o seu
 * traçado em {@link #LOCAIS} e as suas plantas em {@link #PLANTAS}.
 */
public final class Estruturas {
    public static final DeferredRegister<StructureType<?>> TIPOS = DeferredRegister.create(Registries.STRUCTURE_TYPE, DCUniverse.MODID);
    public static final DeferredRegister<StructurePieceType> PECAS = DeferredRegister.create(Registries.STRUCTURE_PIECE, DCUniverse.MODID);

    public static final RegistryObject<StructureType<LocalEstrutura>> LOCAL = TIPOS.register("local", () -> () -> LocalEstrutura.CODEC);
    public static final RegistryObject<StructurePieceType> PECA_OBRA = PECAS.register("obra",
        () -> (StructurePieceType.ContextlessType) PecaObra::new);

    public static final Map<String, Planta> PLANTAS = new HashMap<>();
    public static final Map<String, Local> LOCAIS = new HashMap<>();

    static {
        Smallville.registrar();
    }

    private Estruturas() {}

    public static Planta planta(String id) {
        Planta p = PLANTAS.get(id);
        if (p == null) throw new IllegalArgumentException("Planta desconhecida: " + id);
        return p;
    }
}
