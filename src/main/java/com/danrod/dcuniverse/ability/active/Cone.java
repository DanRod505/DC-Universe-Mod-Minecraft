package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.character.Params;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * area_cone: sopro em cone (sopro congelante). Parâmetros: range, angle (graus), damage_mult (por segundo),
 * effect ("freeze"): deixa lentos e congela mobs, vira água em gelo e apaga fogo. O jato é desenhado no cliente.
 */
public final class Cone implements TipoHabilidade {
    @Override
    public void canalizar(ServerPlayer player, Personagem p, Personagem.Habilidade h, int tick) {
        if (!(player.level() instanceof ServerLevel level)) return;
        Params prm = h.params();
        double alcance = prm.d("range", 8);
        double cosMax = Math.cos(Math.toRadians(prm.d("angle", 30)));
        Vec3 olhos = player.getEyePosition();
        Vec3 dir = player.getLookAngle();
        boolean congela = "freeze".equals(prm.s("effect", "freeze"));

        if (tick % 8 == 0) {
            level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.POWDER_SNOW_BREAK, SoundSource.PLAYERS, 0.9f, 0.6f);
        }
        if (tick % 10 != 0) return;

        AABB area = player.getBoundingBox().expandTowards(dir.scale(alcance)).inflate(alcance * 0.5);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area, e -> Alvos.podeAtingir(player, e))) {
            Vec3 para = e.getBoundingBox().getCenter().subtract(olhos);
            double dist = para.length();
            if (dist > alcance || para.normalize().dot(dir) < cosMax) continue;
            Alvos.ferir(player, e, level.damageSources().freeze(), Alvos.dano(p, prm.d("damage_mult", 0.2)) / 2);
            if (congela) {
                // Controle limitado a 4 s em mobs comuns (BALANCEAMENTO.md).
                e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 2), player);
                e.setTicksFrozen(Math.min(e.getTicksRequiredToFreeze() + 80, e.getTicksFrozen() + 40));
                e.clearFire();
            }
        }
        if (congela && player.mayBuild()) congelarBlocos(level, player, olhos, dir, alcance);
    }

    private static void congelarBlocos(ServerLevel level, ServerPlayer player, Vec3 olhos, Vec3 dir, double alcance) {
        for (double d = 1; d <= alcance; d += 1) {
            BlockPos centro = BlockPos.containing(olhos.add(dir.scale(d)));
            int r = (int) (d * 0.35);
            for (BlockPos pos : BlockPos.betweenClosed(centro.offset(-r, -r - 1, -r), centro.offset(r, r, r))) {
                if (!level.mayInteract(player, pos)) continue;
                BlockState st = level.getBlockState(pos);
                if (st.is(Blocks.FIRE) || st.is(Blocks.SOUL_FIRE)) {
                    level.removeBlock(pos, false);
                } else if (st.getFluidState().is(Fluids.WATER) && st.is(Blocks.WATER) && level.getBlockState(pos.above()).isAir()) {
                    level.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState());
                    level.scheduleTick(pos, Blocks.FROSTED_ICE, 200 + level.getRandom().nextInt(100));
                }
            }
        }
    }
}
