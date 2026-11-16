package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.ability.Congelamento;
import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.core.Canal;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * spinning_beam: o herói gira e os raios de calor varrem tudo em volta (dano em área). Parâmetros: duration (s),
 * turns (voltas), range (blocos), pitch (graus para baixo, o raio risca o chão no fim), damage_mult (cada passada do
 * raio por um alvo, uma por volta), color, sets_fire, melts, cooks_drops e lights_furnace. O servidor varre o setor
 * entre o ângulo do tick anterior e o de agora; o cliente desenha os feixes girando (Rede.Giro, client/Efeitos).
 */
public final class GiroCalor implements TipoHabilidade {
    private static final List<Ativo> ATIVOS = new ArrayList<>();

    private static final class Ativo {
        final ServerPlayer dono;
        final Personagem p;
        final Params prm;
        final int ticks;
        final float voltas, alcance, yaw0, pitch, dano;
        final Set<Integer> atingidosNaVolta = new HashSet<>();
        int idade, volta;

        Ativo(ServerPlayer dono, Personagem p, Params prm, float dano) {
            this.dono = dono;
            this.p = p;
            this.prm = prm;
            this.ticks = Math.max(10, Math.round(prm.f("duration", 2f) * 20));
            this.voltas = prm.f("turns", 2f);
            this.alcance = prm.f("range", 12f);
            this.yaw0 = dono.getYRot();
            this.pitch = prm.f("pitch", 8f);
            this.dano = dano;
        }

        float yaw(int t) {
            return yaw0 + 360f * voltas * t / ticks;
        }
    }

    @Override
    public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        Params prm = h.params();
        ATIVOS.removeIf(a -> a.dono == player);
        Ativo a = new Ativo(player, p, prm, Alvos.dano(p, prm.d("damage_mult", 0.7)));
        ATIVOS.add(a);
        level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 0.6f);
        level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.9f, 0.7f);
        if (player.connection != null) {
            Rede.CANAL.send(new Rede.Giro(player.getId(), a.ticks, a.voltas, a.alcance, prm.color("color", 0xFF2A1A), a.yaw0,
                prm.f("beam_scale", 1f)), Canal.rastreandoEProprio(player));
        }
        return true;
    }

    /** Tick do servidor: varre o setor de cada giro, fere, incendeia e esquenta os blocos onde o raio bate. */
    public static void tick() {
        Iterator<Ativo> it = ATIVOS.iterator();
        while (it.hasNext()) {
            Ativo a = it.next();
            ServerPlayer pl = a.dono;
            if (pl.isRemoved() || !pl.isAlive() || EstadoJogador.de(pl).personagem() == null || a.idade >= a.ticks
                || !(pl.level() instanceof ServerLevel level)) {
                it.remove();
                continue;
            }
            float de = a.yaw(a.idade), ate = a.yaw(a.idade + 1);
            a.idade++;
            int volta = (int) Math.floor((ate - a.yaw0) / 360f);
            if (volta != a.volta) {
                a.volta = volta;
                a.atingidosNaVolta.clear();
            }
            Vec3 olhos = pl.getEyePosition();
            AABB area = pl.getBoundingBox().inflate(a.alcance, 3, a.alcance);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area, e -> Alvos.podeAtingir(pl, e))) {
                Vec3 para = e.position().subtract(pl.position());
                double dist = Math.sqrt(para.x * para.x + para.z * para.z);
                if (dist > a.alcance + e.getBbWidth() / 2 || a.atingidosNaVolta.contains(e.getId())) continue;
                // Ângulo do alvo no mesmo sistema do yaw do jogo, e se ele cai entre o raio de antes e o de agora.
                float ang = (float) (Mth.atan2(para.z, para.x) * Mth.RAD_TO_DEG) - 90f;
                float folga = (float) Math.toDegrees(Math.atan2(e.getBbWidth() / 2 + 0.4, Math.max(0.5, dist)));
                if (!entre(ang, de - folga, ate + folga)) continue;
                a.atingidosNaVolta.add(e.getId());
                if (a.prm.b("sets_fire", true)) e.igniteForSeconds(4);
                if (a.prm.b("cooks_drops", true)) com.danrod.dcuniverse.ability.Assar.marcar(e);
                Alvos.ferir(pl, e, level.damageSources().playerAttack(pl), a.dano);
                Congelamento.derreter(e);
            }
            // Onde o raio bate no chão: funde, acende e risca de fogo (a cada 2 ticks, para não pesar).
            if (a.idade % 2 == 0 && pl.mayBuild()) {
                Vec3 dir = Vec3.directionFromRotation(a.pitch, ate);
                BlockHitResult bh = level.clip(new ClipContext(olhos, olhos.add(dir.scale(a.alcance)), ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, pl));
                if (bh.getType() == HitResult.Type.BLOCK && level.mayInteract(pl, bh.getBlockPos())) {
                    Raio.esquentar(level, pl, bh, a.prm.b("melts", true), a.prm.b("sets_fire", true) && a.idade % 6 == 0,
                        a.prm.b("lights_furnace", true));
                }
            }
            if (a.idade % 5 == 0) level.playSound(null, pl.blockPosition(), SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 0.8f, 1.6f);
        }
    }

    /** O ângulo x (graus) está no arco de a até b (b > a, arco menor que uma volta)? */
    static boolean entre(float x, float a, float b) {
        float d = Mth.wrapDegrees(x - a);
        if (d < 0) d += 360f;
        return d <= b - a;
    }

    /** Está girando agora (usado pelos testes e para não sobrepor dois giros). */
    public static boolean girando(ServerPlayer player) {
        return ATIVOS.stream().anyMatch(a -> a.dono == player);
    }

    public static void limpar() {
        ATIVOS.clear();
    }
}
