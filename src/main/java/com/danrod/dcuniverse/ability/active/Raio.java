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
 * color, sets_fire, melts (derrete e funde blocos atingidos), lights_furnace (acende fornalhas, padrão sim) e
 * cooks_drops (o que o raio mata solta a comida assada, mesmo sem pegar fogo), knockback (empurra quem é atravessado),
 * digs (parado num bloco de chão natural, ele racha e se desfaz: cava buracos rasos) e dig_speed. O feixe é desenhado no cliente (client/Efeitos).
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
            if (prm.b("cooks_drops", false)) com.danrod.dcuniverse.ability.Assar.marcar(e);
            Alvos.ferir(player, e, level.damageSources().playerAttack(player), Alvos.dano(p, prm.d("damage_mult", 0.4)) / 2);
            if (e instanceof net.minecraft.world.entity.LivingEntity vivo) Congelamento.derreter(vivo);
        } else if (alvo instanceof BlockHitResult bh && alvo.getType() == HitResult.Type.BLOCK
            && player.mayBuild() && level.mayInteract(player, bh.getBlockPos())) {
            if (tick % 10 == 9) esquentar(level, player, bh, prm.b("melts", false), prm.b("sets_fire", false) && tick % 20 == 19, prm.b("lights_furnace", true));
            if (prm.b("digs", false)) cavar(level, player, bh, prm.f("dig_speed", 1f));
        } else {
            CALOR.remove(player.getUUID());
        }
        if (alvo instanceof EntityHitResult eh2 && tick % 10 == 0 && prm.f("knockback", 0) > 0 && Alvos.podeAtingir(player, eh2.getEntity())) {
            // Atravessado pelo raio: é empurrado para trás e sai um pouco do chão.
            Entity e = eh2.getEntity();
            var d = player.getLookAngle().multiply(1, 0, 1).normalize().scale(prm.f("knockback", 0));
            e.push(d.x, 0.18, d.z);
            e.hurtMarked = true;
        }
    }

    // ------------------------------------------------------------------ destruição (cavar)

    /** Bloco que o raio de cada jogador está esquentando agora e há quantos ticks. */
    private record Calor(BlockPos pos, int ticks) {}
    private static final Map<java.util.UUID, Calor> CALOR = new java.util.HashMap<>();
    /** O raio não cava mais fundo que isto abaixo dos pés de quem usa (buraco raso, como a cratera do Giro). */
    public static final int FUNDO_MAX = 3;

    /** Ticks de raio parado no mesmo bloco até ele se desfazer: terra e areia rápido, pedra mais devagar. */
    public static int ticksParaQuebrar(BlockState b, float velocidade) {
        float dureza = Math.max(0.3f, b.getBlock().defaultDestroyTime());
        return Math.max(4, Math.round((8 + dureza * 10) / Math.max(0.1f, velocidade)));
    }

    private static void cavar(ServerLevel level, ServerPlayer player, BlockHitResult bh, float velocidade) {
        BlockPos pos = bh.getBlockPos();
        Calor c = CALOR.get(player.getUUID());
        int t = c != null && c.pos().equals(pos) ? c.ticks() + 1 : 1;
        BlockState st = level.getBlockState(pos);
        // Por onde o raio passa, a grama torra na hora (o rastro de terra queimada); parado, ele vai cavando.
        if (t == 1 && torrar(level, player, pos)) st = level.getBlockState(pos);
        if (t < ticksParaQuebrar(st, velocidade)) {
            CALOR.put(player.getUUID(), new Calor(pos, t));
            // O bloco vai rachando enquanto esquenta (as mesmas rachaduras de quando se quebra na mão).
            level.destroyBlockProgress(-1000 - player.getId(), pos, Math.min(9, t * 10 / ticksParaQuebrar(st, velocidade)));
            return;
        }
        CALOR.remove(player.getUUID());
        level.destroyBlockProgress(-1000 - player.getId(), pos, -1);
        queimar(level, player, pos);
    }

    /**
     * O raio desfaz o bloco: só chão natural (terra, grama, areia, cascalho, pedra comum e afins, as mesmas regras da
     * cratera do Giro de Calor), nunca minério, baú, bloco com conteúdo nem perto de estrutura (Smallville, cidades),
     * nunca mais fundo que FUNDO_MAX abaixo dos pés e só com a destruição ligada na configuração. O que sobra embaixo
     * fica torrado (terra grossa) e às vezes pega fogo. Devolve se o bloco foi desfeito.
     */
    public static boolean queimar(ServerLevel level, ServerPlayer player, BlockPos pos) {
        if (!destruicaoLigada(level) || !player.mayBuild() || !level.mayInteract(player, pos)) return false;
        if (pos.getY() < player.getBlockY() - FUNDO_MAX || !GiroCalor.natural(level, pos) || GiroCalor.perto(level, pos, 4)) return false;
        BlockState st = level.getBlockState(pos);
        level.levelEvent(2001, pos, Block.getId(st));
        level.removeBlock(pos, false);
        BlockPos baixo = pos.below();
        BlockState b = level.getBlockState(baixo);
        if (b.is(net.minecraft.tags.BlockTags.DIRT) || b.is(net.minecraft.tags.BlockTags.GRASS_BLOCKS)) {
            level.setBlockAndUpdate(baixo, Blocks.COARSE_DIRT.defaultBlockState());
        }
        if (level.getRandom().nextFloat() < 0.3f && BaseFireBlock.canBePlacedAt(level, pos, net.minecraft.core.Direction.UP)) {
            level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
        }
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.5f, 0.7f + level.getRandom().nextFloat() * 0.3f);
        return true;
    }

    /** Grama, micélio e podzol atingidos viram terra grossa (queimada), com as mesmas regras de onde o raio pode cavar. */
    public static boolean torrar(ServerLevel level, ServerPlayer player, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        if (!(st.is(net.minecraft.tags.BlockTags.GRASS_BLOCKS) || st.is(Blocks.MYCELIUM) || st.is(Blocks.PODZOL))) return false;
        if (!destruicaoLigada(level) || !player.mayBuild() || !level.mayInteract(player, pos) || GiroCalor.perto(level, pos, 4)) return false;
        level.setBlockAndUpdate(pos, Blocks.COARSE_DIRT.defaultBlockState());
        BlockPos cima = pos.above();
        if (level.getBlockState(cima).canBeReplaced() && !level.getBlockState(cima).isAir() && level.getFluidState(cima).isEmpty()) level.removeBlock(cima, false);
        return true;
    }

    /** Destruição da Visão de Calor ligada (opção visaoDestroi). */
    public static boolean destruicaoLigada(ServerLevel level) {
        return com.danrod.dcuniverse.core.Config.VISAO_DESTROI.get();
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
        esquentar(level, player, bh, funde, fogo, true);
    }

    public static void esquentar(ServerLevel level, ServerPlayer player, BlockHitResult bh, boolean funde, boolean fogo, boolean forno) {
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
        if (level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity f) {
            if (forno) esquentarForno(level, pos, st, f);
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
