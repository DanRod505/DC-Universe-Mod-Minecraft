package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.ability.Habilidades;
import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.guia.Guia;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * bracelet_blast: golpe de braceletes cruzados (modo hold). Segurar cruza os braceletes na frente do peito e
 * junta energia (os braços sobem como no bloqueio, os braceletes acendem); soltar bate um no outro e solta uma
 * onda de choque em esfera, no chão ou no ar. Quanto mais carga, maior o raio, o dano e o empurrão; carregado
 * até o fim, também atordoa. A onda devolve flechas e projéteis que estiverem dentro dela e o vento apaga o fogo
 * em volta. Parâmetros: charge (s até a carga cheia), min_radius, radius, damage_mult, knockback, stun (s, só
 * com a carga cheia) e color. Segurar além de 1 s depois da carga cheia solta sozinho.
 */
public final class Choque implements TipoHabilidade {
    /** Início da carga e último tick visto, por jogador. */
    private static final Map<UUID, long[]> CARGA = new ConcurrentHashMap<>();

    @Override
    public void canalizar(ServerPlayer player, Personagem p, Personagem.Habilidade h, int tick) {
        long agora = player.level().getGameTime();
        long[] c = CARGA.get(player.getUUID());
        if (c == null || agora - c[1] > 2) {
            // Começou agora (ou sobrou uma carga de antes de trocar de habilidade).
            c = new long[] {agora, agora};
            CARGA.put(player.getUUID(), c);
            player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), Sons.BRACELETES_CARGA.get(),
                SoundSource.PLAYERS, 1f, 1f);
        }
        c[1] = agora;
        int total = Math.max(1, Math.round(h.params().f("charge", 1.5f) * 20));
        if (agora - c[0] >= total + 20) Habilidades.parar(player);
    }

    /** Quanto já carregou (0 a 1), ou -1 se não está carregando. */
    public static float carga(ServerPlayer player, Personagem.Habilidade h) {
        long[] c = CARGA.get(player.getUUID());
        if (c == null) return -1;
        int total = Math.max(1, Math.round(h.params().f("charge", 1.5f) * 20));
        return Mth.clamp((player.level().getGameTime() - c[0]) / (float) total, 0, 1);
    }

    @Override
    public void soltar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        float c = carga(player, h);
        CARGA.remove(player.getUUID());
        if (c < 0 || !(player.level() instanceof ServerLevel level)) return;
        explodir(level, player, p, h.params(), c);
    }

    /** A onda de choque com a carga c (0 a 1), saindo do peito do jogador. */
    public static void explodir(ServerLevel level, ServerPlayer player, Personagem p, Params prm, float c) {
        Vec3 centro = player.position().add(0, player.getBbHeight() * 0.62, 0);
        double raio = Mth.lerp(c, prm.d("min_radius", 3), prm.d("radius", 8));
        float dano = Alvos.dano(p, prm.d("damage_mult", 1.2) * (0.35 + 0.65 * c));
        double empurra = prm.d("knockback", 2.0) * (0.4 + 0.6 * c);
        int atordoa = c >= 0.999f ? Math.round(Math.min(4f, prm.f("stun", 2f)) * 20) : 0;
        int cor = prm.color("color", 0xFFD86A);
        AABB area = new AABB(centro, centro).inflate(raio);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area, e -> Alvos.podeAtingir(player, e))) {
            Vec3 rel = e.position().add(0, e.getBbHeight() / 2, 0).subtract(centro);
            double d = rel.length();
            if (d > raio + e.getBbWidth() / 2) continue;
            Alvos.ferir(player, e, level.damageSources().playerAttack(player), dano);
            e.invulnerableTime = 0;
            Vec3 dir = d < 0.01 ? player.getLookAngle() : rel.scale(1 / d);
            double f = empurra * (1 - 0.5 * Math.min(1, d / raio));
            e.setDeltaMovement(e.getDeltaMovement().add(dir.x * f, 0.25 + Math.max(0, dir.y) * f * 0.6, dir.z * f));
            e.hurtMarked = true;
            if (atordoa > 0) e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, atordoa, 4), player);
        }
        Alvos.cristais(player, area, level.damageSources().playerAttack(player));
        // Flechas, bolas de fogo e afins dentro da onda voltam para fora (e passam a ser do jogador).
        for (Projectile pr : level.getEntitiesOfClass(Projectile.class, area)) {
            if (pr.getOwner() == player) continue;
            Vec3 rel = pr.position().subtract(centro);
            if (rel.length() > raio) continue;
            Vec3 dir = rel.lengthSqr() < 1e-4 ? player.getLookAngle() : rel.normalize();
            pr.setDeltaMovement(dir.scale(Math.max(1.2, pr.getDeltaMovement().length())));
            pr.setOwner(player);
            pr.hurtMarked = true;
        }
        if (player.mayBuild()) apagarFogo(level, player, BlockPos.containing(centro), (int) Math.ceil(raio));
        level.playSound(null, centro.x, centro.y, centro.z, Sons.BRACELETES_CHOQUE.get(), SoundSource.PLAYERS,
            1.2f + c, 1.15f - 0.25f * c);
        level.sendParticles(new DustParticleOptions(cor, 1.6f), centro.x, centro.y, centro.z, 30 + Math.round(40 * c), raio * 0.3, raio * 0.2, raio * 0.3, 0);
        Rede.efeito(level, centro.x, centro.y, centro.z, "braceletes", (float) raio, c, cor);
        player.swing(InteractionHand.MAIN_HAND, true);
        Guia.concluir(player, "braceletes");
    }

    /** O vento da onda apaga o fogo em volta. */
    private static void apagarFogo(ServerLevel level, ServerPlayer player, BlockPos c, int r) {
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-r, -r, -r), c.offset(r, r, r))) {
            if (pos.distSqr(c) > (double) r * r || !level.getBlockState(pos).is(BlockTags.FIRE) || !level.mayInteract(player, pos)) continue;
            level.removeBlock(pos, false);
        }
    }

    public static void limpar() {
        CARGA.clear();
    }
}
