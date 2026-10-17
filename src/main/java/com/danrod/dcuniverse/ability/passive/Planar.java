package com.danrod.dcuniverse.ability.passive;

import com.danrod.dcuniverse.ability.Passiva;
import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.energy.Energia;
import com.danrod.dcuniverse.guia.Guia;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * glide: no ar, o pulo abre a capa e o jogador plana até pousar (ou apertar o pulo de novo). O movimento é
 * calculado no cliente (client/Planador, como o élitro): mergulhar ganha velocidade, subir troca velocidade por
 * altura, Shift fecha a capa num mergulho e S freia. O cliente avisa o estado da capa (aberta, mergulho, freio) e
 * as manobras; o servidor zera a queda, repassa tudo a quem vê, cobra o impulso e solta o pouso com rolamento.
 * Parâmetros: speed (velocidade de cruzeiro, blocos/s) e boost_cost (energia do impulso da capa).
 */
public final class Planar implements Passiva {
    /** Estado da capa. */
    public static final int FECHADA = 0, ABERTA = 1, MERGULHO = 2, FREIO = 3;
    /** Manobras do planar (o mesmo pacote das manobras de voo, com números próprios). */
    public static final int GIRO_ESQUERDA = 10, GIRO_DIREITA = 11, IMPULSO = 12, ARREMETIDA = 13;
    /** Energia padrão do impulso da capa (toque duplo em W). */
    public static final float IMPULSO_CUSTO = 10;
    /** Ticks mínimos entre duas manobras. */
    public static final int MANOBRA_INTERVALO = 8;
    /** Pouso acima desta velocidade (blocos/tick) vira rolamento, com poeira. */
    static final double POUSO_ROLAMENTO = 0.8;

    private static final class Rastro {
        long manobraEm = -100;
        Vec3 ultima;
        double vel;
    }

    private static final Map<UUID, Rastro> RASTROS = new ConcurrentHashMap<>();

    @Override
    public void tick(ServerPlayer player, Params p) {
        EstadoJogador e = EstadoJogador.de(player);
        Rastro r = RASTROS.computeIfAbsent(player.getUUID(), u -> new Rastro());
        Vec3 agora = player.position();
        if (r.ultima != null) r.vel = agora.distanceTo(r.ultima);
        r.ultima = agora;
        if (e.planar() == FECHADA) return;
        if (!noAr(player)) {
            if (player.onGround() && r.vel > POUSO_ROLAMENTO) pouso(player, r.vel);
            e.setPlanar(FECHADA);
            Rede.aparencia(player);
            return;
        }
        player.resetFallDistance();
    }

    public static boolean planando(ServerPlayer player) {
        return EstadoJogador.de(player).planar() != FECHADA;
    }

    /** O cliente abriu, fechou ou mudou a capa. */
    public static void definir(ServerPlayer player, int estado) {
        if (estado < FECHADA || estado > FREIO) return;
        EstadoJogador e = EstadoJogador.de(player);
        Personagem pers = e.personagem();
        if (estado != FECHADA && (pers == null || pers.passiva("glide") == null || !noAr(player))) estado = FECHADA;
        if (e.planar() == estado) return;
        boolean abriu = e.planar() == FECHADA;
        e.setPlanar(estado);
        if (estado != FECHADA) player.resetFallDistance();
        if (abriu && estado != FECHADA) Guia.concluir(player, "planar");
        Rede.aparencia(player);
    }

    public static void manobra(ServerPlayer player, int tipo) {
        EstadoJogador e = EstadoJogador.de(player);
        if (e.planar() == FECHADA || tipo < GIRO_ESQUERDA || tipo > ARREMETIDA) return;
        Rastro r = RASTROS.computeIfAbsent(player.getUUID(), u -> new Rastro());
        long agora = player.level().getGameTime();
        if (tipo != ARREMETIDA && agora - r.manobraEm < MANOBRA_INTERVALO - 2) return;
        Personagem pers = e.personagem();
        if (tipo == IMPULSO && !player.isCreative()
            && !Energia.gastar(player, pers, pers.passiva("glide").params().f("boost_cost", IMPULSO_CUSTO))) return;
        if (tipo != ARREMETIDA) r.manobraEm = agora;
        ServerLevel lvl = player.level();
        Vec3 c = player.position().add(0, 0.9, 0);
        switch (tipo) {
            case IMPULSO -> lvl.playSound(null, c.x, c.y, c.z, Sons.CAPA_IMPULSO.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
            case ARREMETIDA -> lvl.playSound(null, c.x, c.y, c.z, Sons.CAPA_ARREMETIDA.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
            default -> lvl.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 0.7f);
        }
        Rede.manobraVista(player, tipo);
        Guia.concluir(player, "manobra_planar");
    }

    /** Pouso rápido planando: rola no chão, levantando poeira do bloco embaixo. */
    private static void pouso(ServerPlayer player, double vel) {
        ServerLevel lvl = player.level();
        Vec3 pes = player.position();
        BlockState chao = lvl.getBlockState(player.getBlockPosBelowThatAffectsMyMovement());
        float forca = (float) Math.min(1.0, (vel - POUSO_ROLAMENTO) / 1.0);
        lvl.playSound(null, pes.x, pes.y, pes.z, Sons.POUSO_ROLAMENTO.get(), SoundSource.PLAYERS, 0.8f + 0.4f * forca, 1.0f);
        if (!chao.isAir()) {
            lvl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, chao), pes.x, pes.y + 0.1, pes.z,
                12 + Math.round(16 * forca), 0.5, 0.05, 0.5, 0.15);
        }
        lvl.sendParticles(ParticleTypes.POOF, pes.x, pes.y + 0.2, pes.z, 4 + Math.round(6 * forca), 0.4, 0.05, 0.4, 0.04);
    }

    private static boolean noAr(ServerPlayer player) {
        return !player.onGround() && !player.isInWater() && !player.isInLava() && !player.isPassenger()
            && !player.onClimbable() && !player.getAbilities().flying && !player.isSpectator();
    }

    public static void limpar(UUID id) {
        RASTROS.remove(id);
    }
}
