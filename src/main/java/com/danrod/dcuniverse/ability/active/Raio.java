package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.character.Params;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Map;

/**
 * projectile_beam: raio contínuo (visão de calor). Parâmetros: range, damage_mult (por segundo),
 * color, sets_fire, melts (derrete e funde blocos atingidos). O feixe é desenhado no cliente (client/Efeitos).
 */
public final class Raio implements TipoHabilidade {
    /** O que o raio de calor transforma depois de 1 s no mesmo bloco. */
    private static final Map<Block, Block> FUNDE = Map.of(
        Blocks.SAND, Blocks.GLASS,
        Blocks.RED_SAND, Blocks.GLASS,
        Blocks.ICE, Blocks.WATER,
        Blocks.SNOW_BLOCK, Blocks.AIR,
        Blocks.SNOW, Blocks.AIR,
        Blocks.COBBLESTONE, Blocks.STONE,
        Blocks.CLAY, Blocks.TERRACOTTA
    );

    @Override
    public void canalizar(ServerPlayer player, Personagem p, Personagem.Habilidade h, int tick) {
        Params prm = h.params();
        HitResult alvo = Alvos.mirar(player, prm.d("range", 24));
        if (!(player.level() instanceof ServerLevel level)) return;

        if (alvo instanceof EntityHitResult eh && tick % 10 == 0) {
            Entity e = eh.getEntity();
            Alvos.ferir(player, e, level.damageSources().playerAttack(player), Alvos.dano(p, prm.d("damage_mult", 0.4)) / 2);
            if (prm.b("sets_fire", false) && Alvos.podeAtingir(player, e)) e.igniteForSeconds(3);
        } else if (alvo instanceof BlockHitResult bh && alvo.getType() == HitResult.Type.BLOCK && prm.b("melts", false) && tick % 20 == 19) {
            BlockPos pos = bh.getBlockPos();
            Block novo = FUNDE.get(level.getBlockState(pos).getBlock());
            if (novo != null && player.mayBuild() && level.mayInteract(player, pos)) {
                level.setBlockAndUpdate(pos, novo.defaultBlockState());
            }
        }
    }
}
