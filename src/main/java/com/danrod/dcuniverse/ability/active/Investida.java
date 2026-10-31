package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * air_dash: investida aérea. O jogador dispara para onde olha (dá para virar no caminho) e atropela quem estiver
 * na frente. O movimento é do cliente (client/Voador, como o voo); o servidor manda o pedido, confere quem foi
 * atingido no caminho de cada tick e quebra o que é frágil (vidro, folhas, plantas, neve). Parâmetros: range
 * (blocos percorridos), speed (blocos por tick), damage_mult, knockback, hit_radius, breaks_soft e visual ("shield": a
 * investida com escudo da Mulher-Maravilha, com o escudo erguido na frente e, a pé, correndo rente ao chão).
 * Bater numa parede encerra a investida com um baque.
 */
public final class Investida implements TipoHabilidade {
    private static final List<Ativa> ATIVAS = new ArrayList<>();

    private static final class Ativa {
        final ServerPlayer dono;
        final int duracao;
        final float dano;
        final double empurra, raio;
        final boolean quebra;
        final Set<Integer> atingidos = new HashSet<>();
        int idade, parado;
        Vec3 antes;

        Ativa(ServerPlayer dono, int duracao, float dano, double empurra, double raio, boolean quebra) {
            this.dono = dono;
            this.duracao = duracao;
            this.dano = dano;
            this.empurra = empurra;
            this.raio = raio;
            this.quebra = quebra;
            this.antes = dono.position();
        }
    }

    @Override
    public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        if (!(player.level() instanceof ServerLevel level) || player.isPassenger() || emInvestida(player)) return false;
        Params prm = h.params();
        double vel = prm.d("speed", 3.2);
        int duracao = Math.max(4, (int) Math.ceil(prm.d("range", 24) / vel));
        ATIVAS.add(new Ativa(player, duracao, Alvos.dano(p, prm.d("damage_mult", 1.6)), prm.d("knockback", 2.0),
            prm.d("hit_radius", 1.6), prm.b("breaks_soft", true)));
        // Todos veem o rastro; o cliente do próprio jogador faz o movimento (cor = quem investe).
        boolean escudo = prm.s("visual", "").equals("shield");
        Rede.efeito(level, player.getX(), player.getY(), player.getZ(), escudo ? "investida_escudo" : "investida", duracao, (float) vel, player.getId());
        level.playSound(null, player.getX(), player.getY(), player.getZ(), escudo ? Sons.ESCUDO_INVESTIDA.get() : Sons.INVESTIDA.get(),
            SoundSource.PLAYERS, 1.6f, 1f);
        player.resetFallDistance();
        com.danrod.dcuniverse.guia.Guia.concluir(player, escudo ? "investida_escudo" : "investida");
        return true;
    }

    public static boolean emInvestida(ServerPlayer player) {
        for (Ativa a : ATIVAS) if (a.dono == player) return true;
        return false;
    }

    public static void tick() {
        ATIVAS.removeIf(Investida::avancar);
    }

    /** @return true quando a investida terminou */
    private static boolean avancar(Ativa a) {
        ServerPlayer p = a.dono;
        if (p.isRemoved() || !(p.level() instanceof ServerLevel level)) return true;
        a.idade++;
        Vec3 agora = p.position();
        Vec3 passo = agora.subtract(a.antes);
        // Os primeiros ticks esperam o cliente começar a se mover.
        if (a.idade > 3 && passo.lengthSqr() < 0.02) a.parado++;
        Vec3 dir = passo.lengthSqr() > 1e-4 ? passo.normalize() : p.getLookAngle();
        AABB caminho = p.getBoundingBox().minmax(p.getBoundingBox().move(a.antes.subtract(agora))).inflate(a.raio - 0.3);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, caminho, e -> Alvos.podeAtingir(p, e))) {
            if (!a.atingidos.add(e.getId())) continue;
            Alvos.ferir(p, e, level.damageSources().playerAttack(p), a.dano);
            e.invulnerableTime = 0;
            e.setDeltaMovement(e.getDeltaMovement().add(dir.x * a.empurra, 0.35 + Math.max(0, dir.y) * a.empurra, dir.z * a.empurra));
            e.hurtMarked = true;
            level.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), 14, 0.3, 0.3, 0.3, 0.35);
            level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.2f, 0.7f);
        }
        Alvos.cristais(p, caminho, level.damageSources().playerAttack(p));
        if (a.quebra && p.mayBuild()) quebrarFrageis(level, p, p.getBoundingBox().inflate(0.45).expandTowards(dir.scale(0.8)));
        a.antes = agora;
        if (a.parado >= 2) {
            // Bateu: baque e poeira.
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.5f, 1.4f);
            level.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 1, p.getZ(), 1, 0, 0, 0, 0);
            p.resetFallDistance();
            return true;
        }
        if (a.idade >= a.duracao + 2) {
            p.resetFallDistance();
            return true;
        }
        return false;
    }

    /** Vidro, folhas, plantas, neve e teias no caminho estouram (com os itens caindo, como quebrados à mão). */
    private static void quebrarFrageis(ServerLevel level, ServerPlayer p, AABB caixa) {
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(caixa.minX, caixa.minY, caixa.minZ),
            BlockPos.containing(caixa.maxX, caixa.maxY, caixa.maxZ))) {
            BlockState st = level.getBlockState(pos);
            if (st.isAir() || !st.getFluidState().isEmpty() || !fragil(st) || !level.mayInteract(p, pos)) continue;
            level.destroyBlock(pos, true, p);
        }
    }

    static boolean fragil(BlockState st) {
        return st.is(BlockTags.LEAVES) || st.is(BlockTags.IMPERMEABLE) || st.getBlock() instanceof IronBarsBlock && st.getSoundType() == net.minecraft.world.level.block.SoundType.GLASS
            || st.is(BlockTags.SNOW) || st.is(net.minecraft.world.level.block.Blocks.COBWEB) || st.canBeReplaced() && !st.is(BlockTags.FIRE);
    }

    public static void limpar() {
        ATIVAS.clear();
    }
}
