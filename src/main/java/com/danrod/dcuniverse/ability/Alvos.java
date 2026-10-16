package com.danrod.dcuniverse.ability;

import com.danrod.dcuniverse.character.Personagem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;

/** Utilidades comuns às habilidades: mira, dano pela ficha e regras de multiplayer (seção 2.12). */
public final class Alvos {
    /** Teto de dano por golpe, em corações (docs/BALANCEAMENTO.md). */
    public static final double TETO_CORACOES = 25;

    private Alvos() {}

    /** Dano em pontos de vida: dano base da ficha (corações) x multiplicador do tipo. */
    public static float dano(Personagem p, double mult) {
        return (float) (Math.min(TETO_CORACOES, p.ficha().dano() * mult) * 2);
    }

    /** Pode atingir? Nunca o próprio jogador; outros jogadores só com PvP permitido. */
    public static boolean podeAtingir(ServerPlayer atacante, Entity alvo) {
        if (alvo == atacante || !(alvo instanceof LivingEntity vivo) || !vivo.isAlive()) return false;
        if (alvo instanceof Player outro) return atacante.canHarmPlayer(outro);
        return !alvo.isAlliedTo(atacante);
    }

    public static void ferir(ServerPlayer atacante, Entity alvo, DamageSource fonte, float dano) {
        if (atacante.level() instanceof ServerLevel level && podeAtingir(atacante, alvo)) {
            ((LivingEntity) alvo).hurtServer(level, fonte, dano);
        }
    }

    /** Mira a partir dos olhos: o primeiro bloco ou entidade atingível dentro do alcance. */
    public static HitResult mirar(ServerPlayer player, double alcance) {
        Vec3 olhos = player.getEyePosition();
        Vec3 fim = olhos.add(player.getLookAngle().scale(alcance));
        BlockHitResult bloco = player.level().clip(new ClipContext(olhos, fim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 limite = bloco.getType() == HitResult.Type.MISS ? fim : bloco.getLocation();
        AABB area = new AABB(olhos, limite).inflate(1.0);
        EntityHitResult ent = ProjectileUtil.getEntityHitResult(player, olhos, limite, area,
            e -> podeAtingir(player, e), olhos.distanceToSqr(limite));
        return ent != null ? ent : bloco;
    }

    public static BlockHitResult mirarBloco(ServerPlayer player, double alcance, ClipContext.Fluid fluido) {
        Vec3 olhos = player.getEyePosition();
        Vec3 fim = olhos.add(player.getLookAngle().scale(alcance));
        return player.level().clip(new ClipContext(olhos, fim, ClipContext.Block.OUTLINE, fluido, player));
    }
}
