package com.danrod.dcuniverse.ability.passive;

import com.danrod.dcuniverse.ability.Passiva;
import com.danrod.dcuniverse.ability.QuedaAmortecida;
import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.energy.Energia;
import com.danrod.dcuniverse.guia.Guia;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * flight: pulo duplo no ar liga e desliga o voo. O movimento (aceleração, inércia e curvas) é calculado no
 * cliente (client/Voador), como o do élitro; o servidor libera o voo, gasta energia, solta o estrondo sônico
 * e o impacto do pouso, e avisa quem está vendo que o jogador voa (para a pose).
 * Parâmetros: speed (blocos/s no máximo), drain e boost_drain (energia/s em cruzeiro e acelerando),
 * dash_cost (energia da arrancada) e trail_color/trail_color2 (as duas faixas do rastro).
 * No criativo vale o mesmo voo, sem gastar energia; ao destransformar volta o voo do criativo.
 */
public final class Voo implements Passiva {
    /** Pouso a partir desta velocidade (blocos/tick) faz impacto. */
    public static final double POUSO_FORTE = 0.7;
    /** Intervalo mínimo entre dois estrondos do mesmo jogador (ticks). */
    public static final int ESTRONDO_INTERVALO = 60;
    /** Manobras (Rede.Manobra): giro para a esquerda/direita, meia-volta e arrancada. */
    public static final int GIRO_ESQUERDA = 0, GIRO_DIREITA = 1, MEIA_VOLTA = 2, ARRANCADA = 3;
    /** Intervalo mínimo entre manobras (ticks), igual ao do cliente. */
    public static final int MANOBRA_INTERVALO = 8;

    private static final Map<UUID, Rastro> RASTROS = new ConcurrentHashMap<>();

    /** O que o servidor lembra do tick anterior: se voava, onde estava e a que velocidade. */
    private static final class Rastro {
        boolean voando;
        Vec3 pos;
        double vel;
        long estrondoEm = -ESTRONDO_INTERVALO;
        long manobraEm = -MANOBRA_INTERVALO;
    }

    @Override
    public void aoTransformar(ServerPlayer player, Params p) {
        if (modoLivre(player)) return;
        liberar(player);
    }

    @Override
    public void aoDestransformar(ServerPlayer player, Params p) {
        RASTROS.remove(player.getUUID());
        if (modoLivre(player)) return;
        boolean voava = player.getAbilities().flying;
        if (player.isCreative()) {
            // Volta o voo normal do criativo, sem tirar o jogador do ar.
            player.getAbilities().setFlyingSpeed(0.05f);
            avisar(player);
        } else {
            travar(player);
        }
        if (voava) Rede.aparencia(player);
    }

    @Override
    public void tick(ServerPlayer player, Params p) {
        if (modoLivre(player)) return;
        EstadoJogador e = EstadoJogador.de(player);
        Abilities a = player.getAbilities();
        Rastro r = RASTROS.computeIfAbsent(player.getUUID(), u -> new Rastro());
        Vec3 pos = player.position();
        double vel = r.pos == null ? 0 : pos.distanceTo(r.pos);
        boolean criativo = player.isCreative();
        if (e.esgotado() && !criativo) {
            // Decisão 6: sem energia o voo desliga e o jogador desce planando, sem dano.
            if (a.mayfly) {
                travar(player);
                QuedaAmortecida.iniciarSeNoAr(player);
            }
        } else if (!a.mayfly || a.getFlyingSpeed() != 0f) {
            // Também ao trocar de modo de jogo: o jogo devolve a velocidade do criativo.
            liberar(player);
        }
        if (a.flying) {
            player.resetFallDistance();
            Personagem pers = e.personagem();
            float dreno = player.isSprinting() ? p.f("boost_drain", p.f("drain", 0f)) : p.f("drain", 0f);
            if (pers != null && dreno > 0 && !criativo) Energia.adicionar(player, pers, -dreno / 20f);
            Guia.concluir(player, "voar");
        }
        if (r.voando != a.flying) {
            Rede.aparencia(player);
            if (!a.flying && player.onGround() && r.vel >= POUSO_FORTE) pouso(player, r.vel);
        }
        r.voando = a.flying;
        r.pos = pos;
        r.vel = vel;
    }

    public static boolean voando(ServerPlayer player) {
        EstadoJogador e = EstadoJogador.de(player);
        Personagem p = e.personagem();
        return p != null && p.passiva("flight") != null && player.getAbilities().flying;
    }

    /** Velocidade máxima de voo do personagem (blocos/s), 0 se ele não voa. */
    public static float velocidade(Personagem p) {
        Personagem.Tipo t = p.passiva("flight");
        return t == null ? 0f : t.params().f("speed", 12f);
    }

    /**
     * O cliente avisa que passou da barreira do som. O servidor confere se o jogador voa mesmo e se já deu
     * tempo desde o último, e solta o estrondo para todos por perto.
     */
    public static void estrondo(ServerPlayer player) {
        if (!voando(player) || velocidade(EstadoJogador.de(player).personagem()) < 20f) return;
        Rastro r = RASTROS.computeIfAbsent(player.getUUID(), u -> new Rastro());
        long agora = player.level().getGameTime();
        if (agora - r.estrondoEm < ESTRONDO_INTERVALO) return;
        r.estrondoEm = agora;
        ServerLevel lvl = player.level();
        Vec3 dir = player.getLookAngle();
        Vec3 c = player.position().add(0, 0.9, 0).subtract(dir.scale(0.8));
        lvl.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 3.0f, 0.5f);
        lvl.playSound(null, c.x, c.y, c.z, SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 2.0f, 0.6f);
        // Anel de vapor em volta do corpo, abrindo para os lados e ficando para trás.
        Vec3 u = dir.cross(new Vec3(0, 1, 0));
        u = u.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : u.normalize();
        Vec3 v = u.cross(dir).normalize();
        for (int i = 0; i < 36; i++) {
            double ang = Math.PI * 2 * i / 36;
            Vec3 raio = u.scale(Math.cos(ang)).add(v.scale(Math.sin(ang)));
            Vec3 p = c.add(raio.scale(1.3));
            Vec3 m = raio.scale(0.45).subtract(dir.scale(0.12));
            particulas(lvl, ParticleTypes.CLOUD, p.x, p.y, p.z, 0, m.x, m.y, m.z, 1.0);
        }
        particulas(lvl, ParticleTypes.EXPLOSION, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        particulas(lvl, ParticleTypes.GUST_EMITTER_SMALL, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        Guia.concluir(player, "estrondo");
    }

    /**
     * Manobra pedida pelo cliente (que já fez o movimento): confere, cobra a arrancada, toca o som para todos
     * e manda quem está vendo animar a pose.
     */
    public static void manobra(ServerPlayer player, int tipo) {
        if (!voando(player) || tipo < GIRO_ESQUERDA || tipo > ARRANCADA) return;
        Rastro r = RASTROS.computeIfAbsent(player.getUUID(), u -> new Rastro());
        long agora = player.level().getGameTime();
        if (agora - r.manobraEm < MANOBRA_INTERVALO - 2) return;
        EstadoJogador e = EstadoJogador.de(player);
        Personagem pers = e.personagem();
        if (tipo == ARRANCADA && !player.isCreative()
            && !Energia.gastar(player, pers, pers.passiva("flight").params().f("dash_cost", 15f))) return;
        r.manobraEm = agora;
        ServerLevel lvl = player.level();
        Vec3 c = player.position().add(0, 0.9, 0);
        switch (tipo) {
            case ARRANCADA -> {
                lvl.playSound(null, c.x, c.y, c.z, SoundEvents.TRIDENT_RIPTIDE_3.value(), SoundSource.PLAYERS, 1.2f, 1.1f);
                lvl.playSound(null, c.x, c.y, c.z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0f, 0.7f);
            }
            case MEIA_VOLTA -> lvl.playSound(null, c.x, c.y, c.z, SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 1.0f, 0.8f);
            default -> lvl.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.6f);
        }
        Rede.manobraVista(player, tipo);
        Guia.concluir(player, "manobra");
    }

    /** Pouso forte: o chão racha em volta, com poeira e um baque proporcional à velocidade. */
    private static void pouso(ServerPlayer player, double vel) {
        ServerLevel lvl = player.level();
        Vec3 pes = player.position();
        BlockState chao = lvl.getBlockState(player.getBlockPosBelowThatAffectsMyMovement());
        float forca = (float) Math.min(1.0, (vel - POUSO_FORTE) / 0.9);
        lvl.playSound(null, pes.x, pes.y, pes.z, SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 0.8f + forca, 0.9f);
        if (!chao.isAir()) {
            particulas(lvl, new BlockParticleOption(ParticleTypes.BLOCK, chao), pes.x, pes.y + 0.1, pes.z,
                30 + Math.round(50 * forca), 1.2, 0.05, 1.2, 0.2);
        }
        for (int i = 0; i < 20; i++) {
            double ang = Math.PI * 2 * i / 20;
            particulas(lvl, ParticleTypes.POOF, pes.x, pes.y + 0.15, pes.z, 0, Math.cos(ang), 0.02, Math.sin(ang), 0.25 + 0.25 * forca);
        }
        if (forca > 0.5f) particulas(lvl, ParticleTypes.EXPLOSION, pes.x, pes.y + 0.3, pes.z, 1, 0, 0, 0, 0);
    }

    /** Partículas visíveis de longe para quem está por perto (jogadores de teste, sem conexão, ficam de fora). */
    private static void particulas(ServerLevel lvl, ParticleOptions tipo, double x, double y, double z, int qtd,
                                   double dx, double dy, double dz, double vel) {
        for (ServerPlayer quem : lvl.players()) {
            if (quem.connection != null) lvl.sendParticles(quem, tipo, true, true, x, y, z, qtd, dx, dy, dz, vel);
        }
    }

    /** Libera o pulo duplo para voar. A velocidade do criativo fica zerada: quem move é o cliente. */
    private static void liberar(ServerPlayer player) {
        Abilities a = player.getAbilities();
        a.mayfly = true;
        a.setFlyingSpeed(0f);
        avisar(player);
    }

    private static void travar(ServerPlayer player) {
        Abilities a = player.getAbilities();
        a.mayfly = false;
        a.flying = false;
        a.setFlyingSpeed(0.05f);
        avisar(player);
    }

    /** Manda as habilidades ao cliente (jogadores de teste não têm conexão). */
    private static void avisar(ServerPlayer player) {
        if (player.connection != null) player.onUpdateAbilities();
    }

    /** O espectador atravessa blocos voando do jeito dele: não mexemos nas habilidades. */
    private static boolean modoLivre(ServerPlayer player) {
        return player.isSpectator();
    }
}
