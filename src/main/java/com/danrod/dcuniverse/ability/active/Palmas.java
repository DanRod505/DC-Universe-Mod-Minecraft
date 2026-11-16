package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * sonic_clap: palmas sônicas. Uma bolha de ar sai das mãos e cresce até o raio em spread_ticks; quem ela alcança
 * leva dano, é empurrado para fora, fica atordoado (lento e fraco por stun segundos) e perde a defesa do escudo.
 * No mundo: estoura vidros e folhas, apaga tochas, velas e fogueiras e espalha os itens soltos. Parâmetros: radius,
 * spread_ticks, damage_mult, knockback, launch, stun e breaks_glass.
 */
public final class Palmas implements TipoHabilidade {
    /** Blocos que a onda pode mexer por tick (a casca cresce rápido; o resto fica para o próximo). */
    private static final int BLOCOS_POR_TICK = 400;
    private static final List<Ativa> ATIVAS = new ArrayList<>();

    private static final class Ativa {
        final ServerLevel level;
        final ServerPlayer dono;
        final Vec3 centro;
        final double raio, empurra, lanca;
        final int espalha, lento;
        final float dano;
        final boolean quebra;
        final Set<Integer> atingidos = new HashSet<>();
        int idade;
        double antes;

        Ativa(ServerLevel level, ServerPlayer dono, Vec3 centro, double raio, int espalha, float dano, double empurra, double lanca,
              int lento, boolean quebra) {
            this.level = level;
            this.dono = dono;
            this.centro = centro;
            this.raio = raio;
            this.espalha = espalha;
            this.dano = dano;
            this.empurra = empurra;
            this.lanca = lanca;
            this.lento = lento;
            this.quebra = quebra;
        }
    }

    @Override
    public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        Params prm = h.params();
        Vec3 olhar = player.getLookAngle();
        Vec3 centro = player.position().add(0, player.getBbHeight() * 0.62, 0).add(olhar.x * 0.5, 0, olhar.z * 0.5);
        double raio = prm.d("radius", 9);
        int espalha = Math.max(1, prm.i("spread_ticks", 6));
        ATIVAS.add(new Ativa(level, player, centro, raio, espalha, Alvos.dano(p, prm.d("damage_mult", 0.5)), prm.d("knockback", 1.6),
            prm.d("launch", 0.45), Math.round(Math.min(4f, prm.f("stun", 2f)) * 20), prm.b("breaks_glass", true)));
        level.playSound(null, centro.x, centro.y, centro.z, Sons.PALMAS.get(), SoundSource.PLAYERS, 3.0f, 1.0f);
        int cor = p.cor() & 0xFFFFFF;
        Rede.efeito(level, centro.x, centro.y, centro.z, "palmas", (float) raio, (float) (player.getY() - centro.y), cor);
        com.danrod.dcuniverse.guia.Guia.concluir(player, "habilidade");
        return true;
    }

    public static void tick() {
        ATIVAS.removeIf(Palmas::avancar);
    }

    /** @return true quando a onda terminou */
    private static boolean avancar(Ativa o) {
        o.idade++;
        ServerPlayer dono = o.dono;
        if (dono.isRemoved() || dono.level() != o.level) return true;
        double frente = o.raio * Math.min(1.0, o.idade / (double) o.espalha);
        Vec3 c = o.centro;
        AABB area = new AABB(c, c).inflate(frente);
        for (LivingEntity e : o.level.getEntitiesOfClass(LivingEntity.class, area, e -> Alvos.podeAtingir(dono, e))) {
            Vec3 d = e.position().add(0, e.getBbHeight() / 2, 0).subtract(c);
            double dist = d.length();
            if (dist > frente || !o.atingidos.add(e.getId())) continue;
            Alvos.ferir(dono, e, o.level.damageSources().playerAttack(dono), o.dano);
            e.invulnerableTime = 0;
            Vec3 dir = new Vec3(d.x, 0, d.z);
            dir = dir.lengthSqr() < 1e-4 ? dono.getLookAngle().multiply(1, 0, 1).normalize() : dir.normalize();
            double fora = o.empurra * (1 - 0.5 * dist / o.raio);
            e.setDeltaMovement(e.getDeltaMovement().add(dir.x * fora, o.lanca, dir.z * fora));
            e.hurtMarked = true;
            if (o.lento > 0) {
                e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, o.lento, 3), dono);
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, o.lento, 1), dono);
                e.addEffect(new MobEffectInstance(MobEffects.NAUSEA, o.lento + 40, 0), dono);
            }
            // Derruba o escudo: para de defender e o escudo fica fora de uso por 5 s.
            if (e.isBlocking()) {
                var escudo = e.getUseItem();
                e.stopUsingItem();
                if (e instanceof Player pl) pl.getCooldowns().addCooldown(escudo, 100);
            }
        }
        // Itens soltos no chão voam para fora.
        for (ItemEntity it : o.level.getEntitiesOfClass(ItemEntity.class, area)) {
            Vec3 d = it.position().subtract(c);
            double dist = d.length();
            if (dist > frente || dist < o.antes - 0.5) continue;
            Vec3 dir = dist < 0.01 ? new Vec3(0, 1, 0) : d.normalize();
            it.setDeltaMovement(it.getDeltaMovement().add(dir.x * 0.7, 0.35, dir.z * 0.7));
            it.hurtMarked = true;
        }
        if (o.quebra && dono.mayBuild()) mundo(o, frente);
        o.antes = frente;
        return o.idade >= o.espalha;
    }

    /** A casca entre o raio do tick anterior e o de agora: vidro e folhas estouram, o fogo pequeno apaga. */
    private static void mundo(Ativa o, double frente) {
        BlockPos cp = BlockPos.containing(o.centro);
        int r = (int) Math.ceil(frente);
        int mexidos = 0;
        double r0 = Math.max(0, o.antes - 0.5);
        for (BlockPos pos : BlockPos.betweenClosed(cp.offset(-r, -r, -r), cp.offset(r, r, r))) {
            double d = Math.sqrt(pos.distToCenterSqr(o.centro));
            if (d > frente || d < r0) continue;
            BlockState st = o.level.getBlockState(pos);
            if (st.isAir() || !o.level.mayInteract(o.dono, pos)) continue;
            if (st.is(Tags.Blocks.GLASS_BLOCKS) || st.is(Tags.Blocks.GLASS_PANES) || st.is(BlockTags.LEAVES)) {
                o.level.destroyBlock(pos, true, o.dono);
            } else if (st.is(Blocks.TORCH) || st.is(Blocks.WALL_TORCH) || st.is(Blocks.SOUL_TORCH) || st.is(Blocks.SOUL_WALL_TORCH)) {
                // Apaga a tocha: ela cai como item, com um fiapo de fumaça.
                o.level.destroyBlock(pos, true, o.dono);
                o.level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 6, 0.1, 0.1, 0.1, 0.01);
            } else if ((st.getBlock() instanceof CampfireBlock || st.getBlock() instanceof AbstractCandleBlock)
                && st.hasProperty(BlockStateProperties.LIT) && st.getValue(BlockStateProperties.LIT)) {
                o.level.setBlockAndUpdate(pos, st.setValue(BlockStateProperties.LIT, false));
                o.level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 8, 0.15, 0.1, 0.15, 0.02);
            } else {
                continue;
            }
            if (++mexidos >= BLOCOS_POR_TICK) return;
        }
    }

    public static void limpar() {
        ATIVAS.clear();
    }
}
