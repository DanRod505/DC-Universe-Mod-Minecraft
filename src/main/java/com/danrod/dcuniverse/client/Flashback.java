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
    private static final MethodHandle GRAVADOR = procurarGravador();
    private static boolean gravando;

    private static MethodHandle procurarGravador() {
        try {
            Class<?> c = Class.forName("com.moulberry.flashback.Flashback");
            return MethodHandles.publicLookup().findStaticGetter(c, "RECORDER", c.getField("RECORDER").getType())
                .asType(MethodType.methodType(Object.class));
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    /**
     * Gravação começando: o replay só guarda os pacotes a partir daqui, e o traje, o rastro e os efeitos de quem já
     * estava transformado chegaram antes. Pede tudo de novo ao servidor para ficar no replay.
     */
    static void tick() {
        if (GRAVADOR == null) return;
        boolean agora;
        try {
            agora = GRAVADOR.invokeExact() != null;
        } catch (Throwable e) {
            return;
        }
        if (agora && !gravando && net.minecraft.client.Minecraft.getInstance().getConnection() != null)
            com.danrod.dcuniverse.core.Rede.CANAL.send(new com.danrod.dcuniverse.core.Rede.AparenciasPedir(),
                com.danrod.dcuniverse.core.Canal.SERVIDOR);
        gravando = agora;
    }

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
