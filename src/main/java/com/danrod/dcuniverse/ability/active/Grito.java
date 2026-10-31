package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.Aliados;
import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.guia.Guia;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * war_cry: grito de guerra. Os aliados em volta (o próprio jogador, os outros jogadores, os
 * animais domados dele e os aliados do laço) ganham força e resistência; os monstros no raio ficam fracos e
 * hesitam (lentos por um instante). Parâmetros: radius, duration (s do bônus), strength (nível da força, 1 = I),
 * resistance (nível da resistência, 0 = nenhuma), fear (s de fraqueza e lentidão nos monstros) e color.
 */
public final class Grito implements TipoHabilidade {
    @Override
    public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        Params prm = h.params();
        double raio = prm.d("radius", 12);
        int dura = Math.round(prm.f("duration", 20) * 20);
        int forca = Math.max(1, prm.i("strength", 1)) - 1;
        int resiste = prm.i("resistance", 1) - 1;
        int medo = Math.round(prm.f("fear", 4) * 20);
        int cor = prm.color("color", 0xFFC93C);
        Vec3 c = player.position();
        int aliados = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(raio), LivingEntity::isAlive)) {
            if (e.distanceToSqr(c) > raio * raio) continue;
            if (aliado(player, e)) {
                e.addEffect(new MobEffectInstance(MobEffects.STRENGTH, dura, forca, false, true), player);
                if (resiste >= 0) e.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, dura, resiste, false, true), player);
                level.sendParticles(new DustParticleOptions(cor, 1.2f), e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 14,
                    e.getBbWidth() * 0.5, e.getBbHeight() * 0.4, e.getBbWidth() * 0.5, 0);
                // Cada aliado vê a coluna de luz subindo dele.
                Rede.efeito(level, e.getX(), e.getY(), e.getZ(), "grito_aliado", e.getBbHeight(), e.getBbWidth(), cor);
                if (e != player) aliados++;
            } else if (e instanceof Enemy && medo > 0) {
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, medo, 0), player);
                e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, Math.min(medo, 40), 2), player);
                if (e instanceof Mob m) m.getNavigation().stop();
            }
        }
        level.playSound(null, c.x, c.y, c.z, Sons.GRITO.get(), SoundSource.PLAYERS, 2.5f, 1f);
        Rede.efeito(level, c.x, c.y, c.z, "grito", (float) raio, player.getBbHeight(), cor);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        Guia.concluir(player, "grito");
        if (aliados > 0) Guia.concluir(player, "grito_aliados");
        return true;
    }

    /** Quem recebe o bônus: o próprio jogador, jogadores do mesmo lado, os bichos domados dele e os aliados do laço. */
    static boolean aliado(ServerPlayer player, LivingEntity e) {
        if (e == player) return true;
        if (e instanceof Player outro) return !outro.isSpectator();
        if (e instanceof OwnableEntity dono && dono.getOwner() == player) return true;
        if (e instanceof Mob m && player.getUUID().equals(Aliados.donoDe(m))) return true;
        return e.isAlliedTo(player);
    }
}
