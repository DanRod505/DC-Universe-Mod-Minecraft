package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.character.Transformacao;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * area_blast: onda de choque que corre pelo chão a partir do jogador (pisão sísmico). Parâmetros: radius,
 * damage_mult, knockback (empurra para fora), launch (joga para cima), stun (s de lentidão, até 4) e
 * spread_ticks (quanto a onda leva para chegar na borda). Precisa de chão por perto; cada alvo é atingido
 * uma vez quando a frente da onda passa por ele. O anel, a poeira e o tremor são desenhados no cliente.
 */
public final class Onda implements TipoHabilidade {
    /** Até quantos blocos acima do chão o pisão ainda funciona. */
    private static final double ALTURA_MAX = 3.0;
    private static final List<Ativa> ATIVAS = new ArrayList<>();

    private record Ativa(ServerLevel level, ServerPlayer dono, Vec3 centro, double raio, int espalha, float dano, double empurra,
                         double lanca, int lento, Set<Integer> atingidos, int[] idade) {}

    @Override
    public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        Vec3 pe = player.position();
        BlockHitResult chao = level.clip(new ClipContext(pe.add(0, 0.1, 0), pe.add(0, -ALTURA_MAX, 0),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (chao.getType() == HitResult.Type.MISS) {
            Transformacao.avisar(player, Component.translatable("dcuniverse.msg.sem_chao"));
            return false;
        }
        Params prm = h.params();
        Vec3 centro = chao.getLocation();
        double raio = prm.d("radius", 7);
        int espalha = Math.max(1, prm.i("spread_ticks", 10));
        BlockState piso = level.getBlockState(chao.getBlockPos());
        ATIVAS.add(new Ativa(level, player, centro, raio, espalha, Alvos.dano(p, prm.d("damage_mult", 0.75)),
            prm.d("knockback", 1.2), prm.d("launch", 0.7), Math.round(Math.min(4f, prm.f("stun", 1.5f)) * 20),
            new HashSet<>(), new int[1]));
        level.playSound(null, centro.x, centro.y, centro.z, Sons.PISAO.get(), SoundSource.PLAYERS, 2.0f, 1.0f);
        level.playSound(null, centro.x, centro.y, centro.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7f, 0.55f);
        Rede.efeito(level, centro.x, centro.y, centro.z, "slam", (float) raio, espalha, Block.getId(piso));
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        if (player.mayBuild()) arrancarPlantas(level, player, BlockPos.containing(centro), (int) raio);
        return true;
    }

    /** A onda arranca grama alta, flores e mato no raio (cai o item, como se fosse quebrado à mão). */
    private static void arrancarPlantas(ServerLevel level, ServerPlayer player, BlockPos centro, int r) {
        for (BlockPos pos : BlockPos.betweenClosed(centro.offset(-r, -1, -r), centro.offset(r, 2, r))) {
            if (pos.distSqr(centro) > (double) r * r) continue;
            BlockState st = level.getBlockState(pos);
            if (st.isAir() || !st.getFluidState().isEmpty() || !st.canBeReplaced() || st.is(BlockTags.FIRE)) continue;
            if (level.mayInteract(player, pos)) level.destroyBlock(pos, true, player);
        }
    }

    /** Tick do servidor: avança a frente de cada onda e atinge quem ela alcançou. */
    public static void tick() {
        ATIVAS.removeIf(Onda::avancar);
    }

    /** @return true quando a onda terminou */
    private static boolean avancar(Ativa o) {
        int idade = ++o.idade()[0];
        ServerPlayer dono = o.dono();
        if (dono.isRemoved() || dono.level() != o.level()) return true;
        double frente = o.raio() * Math.min(1.0, idade / (double) o.espalha());
        Vec3 c = o.centro();
        AABB area = new AABB(c.x - frente, c.y - 2, c.z - frente, c.x + frente, c.y + 3, c.z + frente);
        for (LivingEntity e : o.level().getEntitiesOfClass(LivingEntity.class, area, e -> Alvos.podeAtingir(dono, e))) {
            double dx = e.getX() - c.x, dz = e.getZ() - c.z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > frente || !o.atingidos().add(e.getId())) continue;
            // Quem está no ar (voando, pulando alto) não sente a onda do chão.
            if (e.getY() - c.y > 2.5) continue;
            Alvos.ferir(dono, e, o.level().damageSources().playerAttack(dono), o.dano());
            double fora = o.empurra() * (1 - 0.5 * dist / o.raio());
            Vec3 dir = dist < 0.01 ? Vec3.ZERO : new Vec3(dx / dist, 0, dz / dist);
            e.setDeltaMovement(e.getDeltaMovement().add(dir.x * fora, o.lanca(), dir.z * fora));
            e.hurtMarked = true;
            // Controle limitado a 4 s em mobs comuns (BALANCEAMENTO.md).
            if (o.lento() > 0) e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, o.lento(), 3), dono);
        }
        return idade >= o.espalha();
    }

    public static void limpar() {
        ATIVAS.clear();
    }
}
