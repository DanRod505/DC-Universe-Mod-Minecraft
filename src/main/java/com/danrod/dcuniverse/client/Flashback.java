package com.danrod.dcuniverse.client;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Integração com o Flashback (gravação de replays, mod Fabric via Sinytra Connector). Durante um replay os pacotes
 * gravados são repetidos: nossas telas (início, diálogo, Batcomputador) não podem abrir por cima do editor, e as
 * nossas teclas não fazem nada. Sem o Flashback, tudo responde "não está em replay".
 */
public final class Flashback {
    private Flashback() {}

    private static final MethodHandle EM_REPLAY = procurar();

    private static MethodHandle procurar() {
        try {
            return MethodHandles.publicLookup().findStatic(Class.forName("com.moulberry.flashback.Flashback"),
                "isInReplay", MethodType.methodType(boolean.class));
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    /** Assistindo/editando um replay do Flashback. */
    public static boolean emReplay() {
        if (EM_REPLAY == null) return false;
        try {
            return (boolean) EM_REPLAY.invokeExact();
        } catch (Throwable e) {
            return false;
        }
    }
}
