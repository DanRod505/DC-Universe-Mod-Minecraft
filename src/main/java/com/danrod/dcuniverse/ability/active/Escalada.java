package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.content.BlocoTelecinetico;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.core.Canal;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * platform_leap (Escalada Sísmica, Superboy): o pisão ergue com a telecinese tátil três plataformas de terra (3x3) em
 * escada à frente dele; ele salta de uma em uma e, da última, mergulha até o chão com um super soco que solta uma onda
 * de choque em área. No fim as plataformas caem de volta (a terra real volta para os buracos de onde saiu).
 * <p>
 * O "windup" do JSON é o tempo até o pé bater no chão (animação superboy.escalada). Depois dele, em ticks: as
 * plataformas sobem durante "rise" segundos; cada salto leva "jump" s com uma pausa de "pause" s na plataforma; o
 * mergulho leva "dive" s. O movimento é do cliente (client/Escalada), que segue o percurso que o servidor manda
 * (Rede.Escalada); o servidor faz as plataformas, a batida e a onda. Parâmetros da escada: spacing (distância entre
 * plataformas), first_height (altura da primeira) e step (quanto cada uma sobe). A batida: radius, damage_mult,
 * knockback, launch e stun; mira o inimigo mais perto da descida ("dive_reach" blocos além da última plataforma).
 */
public final class Escalada implements TipoHabilidade {
    private static final List<Ativa> ATIVAS = new ArrayList<>();

    /** O percurso, em ticks contados do pisão (os mesmos no servidor e no cliente). */
    public record Tempos(int sobe, int salto, int pausa, int mergulho) {
        /** Começo do movimento i (0, 1 e 2 = saltos; 3 = mergulho). */
        public int inicio(int i) {
            return sobe + i * (salto + pausa);
        }

        public int duracao(int i) {
            return i == 3 ? mergulho : salto;
        }

        public int batida() {
            return inicio(3) + mergulho;
        }
    }

    private static final class Ativa {
        final ServerPlayer dono;
        final Params prm;
        final Tempos tempos;
        final List<List<BlocoTelecinetico>> plataformas = new ArrayList<>();
        final Vec3[] pontos = new Vec3[4];
        final float dano;
        int idade;

        Ativa(ServerPlayer dono, Personagem p, Params prm, Tempos tempos) {
            this.dono = dono;
            this.prm = prm;
            this.tempos = tempos;
            this.dano = Alvos.dano(p, prm.d("damage_mult", 2.0));
        }

        void soltar() {
            for (List<BlocoTelecinetico> pl : plataformas) {
                for (BlocoTelecinetico b : pl) if (b.isAlive() && b.fase() == BlocoTelecinetico.PAIRANDO) b.soltar();
            }
        }
    }

    public static Tempos tempos(Params prm) {
        return new Tempos(Math.max(4, Math.round(prm.f("rise", 0.7f) * 20)), Math.max(4, Math.round(prm.f("jump", 0.5f) * 20)),
            Math.max(0, Math.round(prm.f("pause", 0.2f) * 20)), Math.max(4, Math.round(prm.f("dive", 0.6f) * 20)));
    }

    @Override
    public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        if (!(player.level() instanceof ServerLevel level) || player.isPassenger() || ativa(player)) return false;
        Params prm = h.params();
        Ativa a = new Ativa(player, p, prm, tempos(prm));
        Vec3 pe = player.position(), f = Rochas.frente(player.getYRot()), d = Rochas.direita(f);
        int y = Mth.floor(pe.y) - 1;
        double passo = prm.d("spacing", 3.6), alto = prm.d("first_height", 2.5), sobe = prm.d("step", 1.8);
        for (int k = 0; k < 3; k++) {
            Vec3 c = pe.add(f.scale(passo * (k + 1)));
            double topo = pe.y + alto + sobe * k;
            a.pontos[k] = new Vec3(c.x, topo, c.z);
            List<BlocoTelecinetico> tiles = new ArrayList<>();
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    Vec3 q = c.add(f.scale(i)).add(d.scale(j));
                    Vec3 ancora = new Vec3(q.x, topo - 1, q.z);
                    BlocoTelecinetico b = Terra.erguer(level, player, q.x, y, q.z, ancora, BlocoTelecinetico.PLATAFORMA);
                    // Cada plataforma sobe um pouco depois da anterior: a escada se forma da mais perto para a mais longe.
                    b.pairar(ancora, 0.09f + 0.04f * (2 - k) + 0.01f * (i + 1));
                    tiles.add(b);
                }
            }
            a.plataformas.add(tiles);
        }
        a.pontos[3] = destino(level, player, a.pontos[2], f, prm.d("dive_reach", 4.0), pe.y);
        ATIVAS.add(a);
        // O pisão: a terra treme em volta do pé.
        BlockPos piso = Terra.topo(level, pe.x, y, pe.z);
        BlockState chao = piso == null ? Blocks.DIRT.defaultBlockState() : level.getBlockState(piso);
        Rede.efeito(level, pe.x, pe.y, pe.z, "slam", 4.0f, 8, Block.getId(chao));
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, chao), pe.x, pe.y + 0.1, pe.z, 60, 1.2, 0.1, 1.2, 0.3);
        level.playSound(null, pe.x, pe.y, pe.z, Sons.PISAO.get(), SoundSource.PLAYERS, 1.8f, 0.85f);
        level.playSound(null, pe.x, pe.y, pe.z, Sons.TELECINESE_ERGUE.get(), SoundSource.PLAYERS, 1.8f, 0.7f);
        if (player.connection != null) {
            Rede.CANAL.send(new Rede.Escalada(player.getId(), a.pontos, a.tempos.sobe(), a.tempos.salto(), a.tempos.pausa(),
                a.tempos.mergulho()), Canal.rastreandoEProprio(player));
        }
        com.danrod.dcuniverse.guia.Guia.concluir(player, "telecinese");
        return true;
    }

    /** O ponto do chão onde o mergulho termina: no inimigo mais perto da descida ou alcance blocos além da última plataforma. */
    private static Vec3 destino(ServerLevel level, ServerPlayer pl, Vec3 ultima, Vec3 f, double alcance, double yPe) {
        Vec3 q = ultima.add(f.scale(alcance));
        q = new Vec3(q.x, yPe, q.z);
        Vec3 base = q;
        List<LivingEntity> perto = level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(base, base).inflate(alcance + 2, 6, alcance + 2),
            e -> Alvos.podeAtingir(pl, e) && e != pl && e.isAlive());
        perto.sort(Comparator.comparingDouble(e -> e.distanceToSqr(base)));
        if (!perto.isEmpty()) q = perto.get(0).position();
        BlockPos topo = Terra.topo(level, q.x, Mth.floor(q.y) - 1, q.z);
        return new Vec3(q.x, topo == null ? q.y : topo.getY() + 1, q.z);
    }

    /** Tick do servidor: segura o herói sem dano de queda, solta poeira nos saltos e, na hora, a batida. */
    public static void tick() {
        Iterator<Ativa> it = ATIVAS.iterator();
        while (it.hasNext()) {
            Ativa a = it.next();
            ServerPlayer pl = a.dono;
            if (pl.isRemoved() || !pl.isAlive() || EstadoJogador.de(pl).personagem() == null || !(pl.level() instanceof ServerLevel level)) {
                a.soltar();
                it.remove();
                continue;
            }
            int t = a.idade++;
            pl.resetFallDistance();
            for (int i = 0; i < 3; i++) {
                if (t == a.tempos.inicio(i)) {
                    Vec3 de = i == 0 ? pl.position() : a.pontos[i - 1];
                    level.playSound(null, de.x, de.y, de.z, Sons.SALTO.get(), SoundSource.PLAYERS, 1.0f, 1.1f + 0.1f * i);
                    if (i > 0) poeira(level, a.plataformas.get(i - 1), 6);
                }
                if (t == a.tempos.inicio(i) + a.tempos.salto()) {
                    Vec3 c = a.pontos[i];
                    level.playSound(null, c.x, c.y, c.z, Sons.TELECINESE_POUSA.get(), SoundSource.PLAYERS, 1.2f, 0.9f);
                    poeira(level, a.plataformas.get(i), 10);
                }
            }
            if (t == a.tempos.inicio(3)) {
                Vec3 c = a.pontos[2];
                level.playSound(null, c.x, c.y, c.z, Sons.GOLPE_AR.get(), SoundSource.PLAYERS, 1.6f, 0.7f);
            }
            if (t == a.tempos.batida()) {
                batida(level, pl, a);
                it.remove();
            }
        }
    }

    private static void poeira(ServerLevel level, List<BlocoTelecinetico> tiles, int n) {
        BlocoTelecinetico meio = tiles.get(4);
        if (!meio.isAlive()) return;
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, meio.bloco()), meio.getX(), meio.getY() + 1.05, meio.getZ(),
            n * 3, 0.9, 0.05, 0.9, 0.15);
    }

    /** O super soco no chão: a onda de choque em área, estilhaços, clarão, e as plataformas desabam. */
    private static void batida(ServerLevel level, ServerPlayer pl, Ativa a) {
        Vec3 c = pl.position().distanceToSqr(a.pontos[3]) < 16 ? pl.position() : a.pontos[3];
        BlockPos piso = Terra.topo(level, c.x, Mth.floor(c.y) - 1, c.z);
        BlockState chao = piso == null ? Blocks.DIRT.defaultBlockState() : level.getBlockState(piso);
        Params prm = a.prm;
        Onda.disparar(level, pl, c, chao, prm.d("radius", 6.5), Math.max(1, prm.i("spread_ticks", 7)), a.dano,
            prm.d("knockback", 2.2), prm.d("launch", 0.9), prm.f("stun", 1.5f));
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, chao), c.x, c.y + 0.2, c.z, 160, 2.2, 0.3, 2.2, 0.45);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 0.3, c.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.CLOUD, c.x, c.y + 0.2, c.z, 40, 1.8, 0.2, 1.8, 0.12);
        level.playSound(null, c.x, c.y, c.z, Sons.GOLPE_ONDA.get(), SoundSource.PLAYERS, 2.4f, 0.8f);
        level.playSound(null, c.x, c.y, c.z, Sons.GOLPE_PESADO.get(), SoundSource.PLAYERS, 2.0f, 0.7f);
        a.soltar();
    }

    public static boolean ativa(ServerPlayer player) {
        return ATIVAS.stream().anyMatch(a -> a.dono == player);
    }

    public static void aoDestransformar(ServerPlayer player) {
        ATIVAS.removeIf(a -> {
            if (a.dono != player) return false;
            a.soltar();
            return true;
        });
    }

    public static void limpar() {
        ATIVAS.clear();
    }
}
