package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Som contínuo de uma habilidade canalizada (zumbido da visão de calor, chiado do sopro congelante), preso ao
 * jogador que canaliza: entra suave, segue ele e some suave quando ele para. Todo mundo por perto ouve.
 */
public final class SomCanal extends AbstractTickableSoundInstance {
    private static final Map<Integer, SomCanal> TOCANDO = new HashMap<>();

    private final Player dono;
    private final String efeito;
    private final float alvo;
    private boolean saindo;

    private SomCanal(SoundEvent som, Player dono, String efeito, float volume) {
        super(som, SoundSource.PLAYERS, dono.getRandom());
        this.dono = dono;
        this.efeito = efeito;
        this.alvo = volume;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.01f;
        mover();
    }

    /** Liga o som de quem começou a canalizar (chamado a cada tick do cliente). */
    public static void tick(Minecraft mc, ClientLevel level) {
        for (Player pl : level.players()) {
            Rede.Aparencia a = EstadoCliente.APARENCIAS.get(pl.getId());
            SoundEvent som = a == null ? null : switch (a.efeito()) {
                case "projectile_beam" -> Sons.VISAO_CALOR.get();
                case "area_cone" -> Sons.SOPRO.get();
                default -> null;
            };
            SomCanal atual = TOCANDO.get(pl.getId());
            if (som == null || (atual != null && !atual.isStopped() && !atual.saindo)) continue;
            SomCanal novo = new SomCanal(som, pl, a.efeito(), a.efeito().equals("area_cone") ? 0.9f : 0.75f);
            TOCANDO.put(pl.getId(), novo);
            mc.getSoundManager().play(novo);
        }
        TOCANDO.values().removeIf(SomCanal::isStopped);
    }

    public static void limpar() {
        TOCANDO.clear();
    }

    @Override
    public void tick() {
        Rede.Aparencia a = EstadoCliente.APARENCIAS.get(dono.getId());
        saindo = dono.isRemoved() || a == null || !a.efeito().equals(efeito);
        volume = saindo ? volume - 0.15f : Math.min(alvo, volume + 0.12f);
        if (volume <= 0) {
            stop();
            return;
        }
        mover();
    }

    private void mover() {
        x = dono.getX();
        y = dono.getEyeY();
        z = dono.getZ();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
