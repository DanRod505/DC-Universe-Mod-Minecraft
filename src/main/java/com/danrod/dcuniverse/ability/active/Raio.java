package com.danrod.dcuniverse.ability.active;

import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import com.danrod.dcuniverse.ability.Congelamento;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.BaseFireBlock;
import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.character.Params;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
    /** O que o raio de calor funde a cada meio segundo no bloco atingido (gelo e neve viram água ou somem). */
    private static final Map<Block, Block> FUNDE = Map.ofEntries(
        Map.entry(Blocks.SAND, Blocks.GLASS),
        Map.entry(Blocks.RED_SAND, Blocks.GLASS),
        Map.entry(Blocks.ICE, Blocks.WATER),
        Map.entry(Blocks.FROSTED_ICE, Blocks.WATER),
        Map.entry(Blocks.PACKED_ICE, Blocks.WATER),
        Map.entry(Blocks.BLUE_ICE, Blocks.WATER),
        Map.entry(Blocks.SNOW_BLOCK, Blocks.AIR),
        Map.entry(Blocks.SNOW, Blocks.AIR),
        Map.entry(Blocks.POWDER_SNOW, Blocks.AIR),
        Map.entry(Blocks.COBBLESTONE, Blocks.STONE),
        Map.entry(Blocks.CLAY, Blocks.TERRACOTTA)
    );

    @Override
    public void canalizar(ServerPlayer player, Personagem p, Personagem.Habilidade h, int tick) {
        Params prm = h.params();
        HitResult alvo = Alvos.mirar(player, prm.d("range", 24));
        if (!(player.level() instanceof ServerLevel level)) return;
        if (tick % 10 == 0) {
            // O zumbido do feixe é contínuo no cliente (client/SomCanal); aqui só o chiado de queimado onde bate.
            var onde = alvo.getLocation();
            level.playSound(null, onde.x, onde.y, onde.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.25f, 1.6f);
        }

        if (alvo instanceof EntityHitResult eh && tick % 10 == 0) {
            Entity e = eh.getEntity();
            // Pega fogo antes do dano: se morrer com o golpe, já está em chamas e a comida cai assada (loot do jogo).
            if (prm.b("sets_fire", false) && Alvos.podeAtingir(player, e)) e.igniteForSeconds(4);
            Alvos.ferir(player, e, level.damageSources().playerAttack(player), Alvos.dano(p, prm.d("damage_mult", 0.4)) / 2);
            if (e instanceof net.minecraft.world.entity.LivingEntity vivo) Congelamento.derreter(vivo);
        } else if (alvo instanceof BlockHitResult bh && alvo.getType() == HitResult.Type.BLOCK && tick % 10 == 9
            && player.mayBuild() && level.mayInteract(player, bh.getBlockPos())) {
            esquentar(level, player, bh, prm.b("melts", false), prm.b("sets_fire", false) && tick % 20 == 19);
        }
    }

    private static java.lang.reflect.Field dadosForno;

    /**
     * Fornalha, defumador e alto-forno funcionam com o calor dos olhos, sem combustível: cada meio segundo de
     * raio vale 1,5 s de fogo (até 10 s guardados), só quando há algo para assar.
     */
    private static void esquentarForno(ServerLevel level, BlockPos pos, BlockState st, AbstractFurnaceBlockEntity forno) {
        if (forno.getItem(0).isEmpty()) return;
        try {
            if (dadosForno == null) {
                dadosForno = AbstractFurnaceBlockEntity.class.getDeclaredField("dataAccess");
                dadosForno.setAccessible(true);
            }
            ContainerData d = (ContainerData) dadosForno.get(forno);
            int aceso = Math.min(200, d.get(0) + 30);
            d.set(0, aceso);
            d.set(1, Math.max(d.get(1), aceso));
        } catch (ReflectiveOperationException e) {
            return;
        }
        if (st.hasProperty(BlockStateProperties.LIT) && !st.getValue(BlockStateProperties.LIT)) {
            level.setBlock(pos, st.setValue(BlockStateProperties.LIT, true), 3);
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.6f, 1.4f);
        }
        forno.setChanged();
    }

    /** Funde o bloco atingido (e a neve em cima dele); com fogo, acende fogueiras, velas e TNT ou põe fogo na face. */
    public static void esquentar(ServerLevel level, ServerPlayer player, BlockHitResult bh, boolean funde, boolean fogo) {
        BlockPos pos = bh.getBlockPos();
        BlockState st = level.getBlockState(pos);
        if (funde) {
            BlockPos cima = pos.above();
            if (FUNDE.get(level.getBlockState(cima).getBlock()) == Blocks.AIR) level.removeBlock(cima, false);
            Block novo = FUNDE.get(st.getBlock());
            if (novo != null) {
                boolean evapora = novo == Blocks.WATER && level.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, pos);
                level.setBlockAndUpdate(pos, (evapora ? Blocks.AIR : novo).defaultBlockState());
                level.levelEvent(1501, pos, 0);
                return;
            }
        }
        if (level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity forno) {
            esquentarForno(level, pos, st, forno);
            return;
        }
        if (!fogo) return;
        if (CampfireBlock.canLight(st) || CandleBlock.canLight(st) || CandleCakeBlock.canLight(st)) {
            level.setBlock(pos, st.setValue(BlockStateProperties.LIT, true), 11);
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8f, 1.2f);
        } else if (st.getBlock() instanceof TntBlock) {
            st.onCaughtFire(level, pos, bh.getDirection(), player);
            level.removeBlock(pos, false);
        } else {
            BlockPos face = pos.relative(bh.getDirection());
            if (BaseFireBlock.canBePlacedAt(level, face, bh.getDirection().getOpposite())) {
                level.setBlockAndUpdate(face, BaseFireBlock.getState(level, face));
                level.playSound(null, face, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8f, 1.2f);
            }
        }
    }
}
