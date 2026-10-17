package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.guia.Guia;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * smoke (bomba de fumaça): estoura uma nuvem nos pés de quem usa. Por duration segundos, quem está dentro fica
 * cego (blind segundos, renovado) e os mobs perdem o alvo; quem jogou ganha escape segundos de velocidade e
 * invisibilidade para fugir. A nuvem é desenhada nos clientes por perto (efeito "smoke" em client/Mundo).
 * Parâmetros: radius, duration, blind, escape.
 */
public final class Fumaca implements TipoHabilidade {
    private record Nuvem(ResourceKey<Level> mundo, Vec3 centro, double raio, long fim, int cegueira, UUID dono) {}

    private static final List<Nuvem> NUVENS = new ArrayList<>();

    @Override
    public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        Params prm = h.params();
        ServerLevel level = player.level();
        Vec3 c = player.position().add(0, 0.2, 0);
        float raio = prm.f("radius", 5), dur = prm.f("duration", 8);
        Nuvem n = new Nuvem(level.dimension(), c, raio, level.getGameTime() + Math.round(dur * 20),
            Math.round(prm.f("blind", 5) * 20), player.getUUID());
        synchronized (NUVENS) {
            NUVENS.add(n);
        }
        Rede.efeito(level, c.x, c.y, c.z, "smoke", dur, raio, prm.color("color", 0x8C9099));
        level.playSound(null, c.x, c.y, c.z, Sons.FUMACA.get(), SoundSource.PLAYERS, 1.2f, 1f);
        int fuga = Math.round(prm.f("escape", 3) * 20);
        // (Sem conexão só o jogador de teste: os efeitos mandam pacote para ele.)
        if (player.connection != null) {
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, fuga, 1, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, fuga, 0, false, false, true));
        }
        afetar(level, n);
        Guia.concluir(player, "fumaca");
        return true;
    }

    /** Nuvem de fumaça solta por outra coisa (a cortina da Batmoto): cega e tira o alvo como a bomba. */
    public static void nuvem(ServerPlayer dono, Vec3 c, float raio, float dur, float cegueiraSeg) {
        ServerLevel level = dono.level();
        Nuvem n = new Nuvem(level.dimension(), c, raio, level.getGameTime() + Math.round(dur * 20), Math.round(cegueiraSeg * 20), dono.getUUID());
        synchronized (NUVENS) {
            NUVENS.add(n);
        }
        Rede.efeito(level, c.x, c.y, c.z, "smoke", dur, raio, 0x5E636C);
        level.playSound(null, c.x, c.y, c.z, Sons.FUMACA.get(), SoundSource.PLAYERS, 1.2f, 0.9f);
        afetar(level, n);
    }

    /** Tick do servidor: a cada meio segundo, cega quem está nas nuvens e tira o alvo dos mobs. */
    public static void tick(net.minecraft.server.MinecraftServer server) {
        synchronized (NUVENS) {
            NUVENS.removeIf(n -> {
                ServerLevel level = server.getLevel(n.mundo());
                if (level == null || level.getGameTime() >= n.fim()) return true;
                if (level.getGameTime() % 10 == 0) afetar(level, n);
                return false;
            });
        }
    }

    private static void afetar(ServerLevel level, Nuvem n) {
        ServerPlayer dono = level.getServer().getPlayerList().getPlayer(n.dono());
        AABB area = new AABB(n.centro(), n.centro()).inflate(n.raio(), n.raio() * 0.7, n.raio());
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (e.getUUID().equals(n.dono()) || e.distanceToSqr(n.centro()) > n.raio() * n.raio()) continue;
            if (dono != null && !Alvos.podeAtingir(dono, e)) continue;
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, n.cegueira(), 0), dono);
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 1), dono);
            if (e instanceof Mob mob) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }
        }
        // Quem está fora mas mirava em quem está dentro da fumaça também perde de vista.
        if (dono != null && dono.distanceToSqr(n.centro()) <= n.raio() * n.raio()) {
            for (Mob mob : level.getEntitiesOfClass(Mob.class, area.inflate(16), m -> m.getTarget() == dono)) mob.setTarget(null);
        }
    }

    public static void limpar() {
        synchronized (NUVENS) {
            NUVENS.clear();
        }
    }
}
