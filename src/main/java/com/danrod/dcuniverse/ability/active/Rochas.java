package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.content.BlocoTelecinetico;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * rock_combo (Fúria de Rochas, Superboy): o soco no chão ergue três blocos de terra com a telecinese tátil, que sobem
 * à frente dele; um a um, eles descem até a altura do golpe e ele acerta cada um (soco de direita, chute giratório e
 * um direto com o corpo todo), arremessando-o contra um inimigo diferente. A rocha espatifa em quem acerta.
 * <p>
 * O "windup" do JSON é o tempo até o punho bater no chão (animação superboy.rochas); "hit1", "hit2" e "hit3" são os
 * segundos, contados da batida, em que cada golpe acerta. Parâmetros: damage_mult (os dois primeiros),
 * final_damage_mult (o último), knockback e final_knockback, throw_speed (blocos por tick) e range (alcance da busca
 * por alvos à frente). Sem alvo, a rocha vai para onde ele olha.
 */
public final class Rochas implements TipoHabilidade {
    private static final List<Ativa> ATIVAS = new ArrayList<>();
    /** Ticks antes do golpe em que a rocha desce até a altura dele. */
    private static final int DESCE = 7;

    private static final class Ativa {
        final ServerPlayer dono;
        final Params prm;
        final float dano, danoFinal;
        final int[] golpes = new int[3];
        final BlocoTelecinetico[] rochas = new BlocoTelecinetico[3];
        final Vec3[] posGolpe = new Vec3[3];
        final List<Integer> usados = new ArrayList<>();
        int idade;

        Ativa(ServerPlayer dono, Personagem p, Params prm) {
            this.dono = dono;
            this.prm = prm;
            this.dano = Alvos.dano(p, prm.d("damage_mult", 1.0));
            this.danoFinal = Alvos.dano(p, prm.d("final_damage_mult", 1.6));
            golpes[0] = Math.round(prm.f("hit1", 0.45f) * 20);
            golpes[1] = Math.round(prm.f("hit2", 0.85f) * 20);
            golpes[2] = Math.round(prm.f("hit3", 1.3f) * 20);
        }
    }

    /** Frente e direita no plano, pelo yaw (graus) do jogador. */
    static Vec3 frente(float yaw) {
        float r = yaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(r), 0, Mth.cos(r));
    }

    static Vec3 direita(Vec3 f) {
        return new Vec3(-f.z, 0, f.x);
    }

    @Override
    public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        if (!(player.level() instanceof ServerLevel level) || player.isPassenger()) return false;
        Params prm = h.params();
        ATIVAS.removeIf(a -> {
            if (a.dono != player) return false;
            for (BlocoTelecinetico b : a.rochas) if (b != null && b.isAlive() && b.fase() == BlocoTelecinetico.PAIRANDO) b.soltar();
            return true;
        });
        Ativa a = new Ativa(player, p, prm);
        Vec3 pe = player.position(), f = frente(player.getYRot()), d = direita(f);
        int y = Mth.floor(pe.y) - 1;
        // De onde cada bloco sai (no chão, à frente) e onde ele fica no ar até a vez dele: bem alto, escalonados.
        Vec3[] chao = {pe.add(f.scale(2.0)).add(d.scale(1.1)), pe.add(f.scale(2.2)).add(d.scale(-1.1)), pe.add(f.scale(3.1))};
        Vec3[] alto = {pe.add(f.scale(2.0)).add(d.scale(1.0)).add(0, 2.3, 0), pe.add(f.scale(2.3)).add(d.scale(-1.0)).add(0, 2.9, 0),
            pe.add(f.scale(2.7)).add(0, 3.5, 0)};
        // Onde cada um é golpeado: na altura do punho direito, do chute e do direto final.
        a.posGolpe[0] = pe.add(f.scale(1.6)).add(d.scale(0.5)).add(0, 0.75, 0);
        a.posGolpe[1] = pe.add(f.scale(1.7)).add(d.scale(-0.3)).add(0, 0.45, 0);
        a.posGolpe[2] = pe.add(f.scale(1.7)).add(0, 1.0, 0);
        for (int i = 0; i < 3; i++) {
            a.rochas[i] = Terra.erguer(level, player, chao[i].x, y, chao[i].z, alto[i], BlocoTelecinetico.ROCHA);
            a.rochas[i].pairar(alto[i], 0.22f - 0.03f * i);
        }
        ATIVAS.add(a);
        // O soco no chão: a terra racha em volta do punho e um anel de poeira corre pelo chão.
        Vec3 punho = pe.add(f.scale(0.9));
        BlockPos piso = Terra.topo(level, punho.x, y, punho.z);
        int idPiso = piso == null ? Block.getId(net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState()) : Block.getId(level.getBlockState(piso));
        Rede.efeito(level, punho.x, pe.y, punho.z, "slam", 1.8f, 4, idPiso);
        if (piso != null) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(piso)), punho.x, pe.y + 0.1, punho.z,
                50, 0.6, 0.1, 0.6, 0.25);
        }
        level.playSound(null, punho.x, pe.y, punho.z, Sons.PISAO.get(), SoundSource.PLAYERS, 1.3f, 1.25f);
        level.playSound(null, punho.x, pe.y, punho.z, Sons.TELECINESE_ERGUE.get(), SoundSource.PLAYERS, 1.5f, 0.9f);
        player.swing(InteractionHand.MAIN_HAND, true);
        com.danrod.dcuniverse.guia.Guia.concluir(player, "telecinese");
        return true;
    }

    /** Tick do servidor: cada rocha desce à altura do golpe e, na hora, sai arremessada. */
    public static void tick() {
        Iterator<Ativa> it = ATIVAS.iterator();
        while (it.hasNext()) {
            Ativa a = it.next();
            ServerPlayer pl = a.dono;
            if (pl.isRemoved() || !pl.isAlive() || EstadoJogador.de(pl).personagem() == null || !(pl.level() instanceof ServerLevel level)) {
                for (BlocoTelecinetico b : a.rochas) if (b != null && b.isAlive() && b.fase() == BlocoTelecinetico.PAIRANDO) b.soltar();
                it.remove();
                continue;
            }
            int t = a.idade++;
            for (int i = 0; i < 3; i++) {
                BlocoTelecinetico b = a.rochas[i];
                if (b == null || !b.isAlive() || b.fase() != BlocoTelecinetico.PAIRANDO) continue;
                if (t == a.golpes[i] - DESCE) b.pairar(a.posGolpe[i], 0.4f);
                if (t == a.golpes[i]) golpear(level, pl, a, i);
            }
            if (t > a.golpes[2]) it.remove();
        }
    }

    /** O golpe i acerta a rocha i e a arremessa no próximo inimigo à frente (um diferente para cada rocha, se houver). */
    private static void golpear(ServerLevel level, ServerPlayer pl, Ativa a, int i) {
        BlocoTelecinetico b = a.rochas[i];
        boolean fim = i == 2;
        Vec3 de = b.position().add(0, 0.5, 0);
        LivingEntity alvo = alvo(level, pl, a, de);
        Vec3 mira = alvo != null ? alvo.position().add(0, alvo.getBbHeight() * 0.55, 0)
            : de.add(pl.getLookAngle().scale(24));
        if (alvo != null) a.usados.add(alvo.getId());
        double vel = a.prm.d("throw_speed", 2.4) * (fim ? 1.15 : 1.0);
        Vec3 dif = mira.subtract(de);
        double dist = dif.length();
        // Compensa a queda do arco (gravidade do bloco, 0,045 por tick²), como na telecinese comum.
        Vec3 dir = dif.add(0, 0.0225 * dist * dist / (vel * vel), 0).normalize();
        b.estilhacar(true);
        b.empurrar(a.prm.d(fim ? "final_knockback" : "knockback", fim ? 1.8 : 1.0));
        b.lancar(dir.scale(vel), fim ? a.danoFinal : a.dano, new HashMap<>());
        // O impacto do golpe na rocha: estalo, estilhaços e o ar cortado.
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, b.bloco()), de.x, de.y, de.z, 22, 0.3, 0.3, 0.3, 0.2);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, de.x, de.y, de.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.CRIT, de.x, de.y, de.z, 10, 0.2, 0.2, 0.2, 0.5);
        level.playSound(null, de.x, de.y, de.z, (fim ? Sons.GOLPE_PESADO : Sons.GOLPE_MEDIO).get(), SoundSource.PLAYERS, 1.4f, fim ? 0.9f : 1.05f);
        level.playSound(null, de.x, de.y, de.z, Sons.TELECINESE_LANCA.get(), SoundSource.PLAYERS, 1.2f, 1.1f + 0.1f * i);
        pl.swing(i == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, true);
    }

    /** O inimigo mais perto à frente que ainda não levou rocha (se todos já levaram, o mais perto). */
    private static LivingEntity alvo(ServerLevel level, ServerPlayer pl, Ativa a, Vec3 de) {
        double r = a.prm.d("range", 18);
        Vec3 olhar = frente(pl.getYRot());
        List<LivingEntity> todos = level.getEntitiesOfClass(LivingEntity.class, pl.getBoundingBox().inflate(r, r * 0.5, r),
            e -> Alvos.podeAtingir(pl, e) && e != pl && e.isAlive()
                && new Vec3(e.getX() - pl.getX(), 0, e.getZ() - pl.getZ()).normalize().dot(olhar) > 0.25
                && pl.hasLineOfSight(e));
        todos.sort(Comparator.comparingDouble(e -> e.distanceToSqr(de)));
        for (LivingEntity e : todos) if (!a.usados.contains(e.getId())) return e;
        return todos.isEmpty() ? null : todos.get(0);
    }

    public static boolean ativa(ServerPlayer player) {
        return ATIVAS.stream().anyMatch(a -> a.dono == player);
    }

    public static void limpar() {
        ATIVAS.clear();
    }
}
