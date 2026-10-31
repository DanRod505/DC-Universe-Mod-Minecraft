package com.danrod.dcuniverse.ability;

import com.danrod.dcuniverse.character.Personagem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
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
        // No End: os cristais (que curam o dragão) quebram com qualquer poder, e as partes do dragão contam como ele.
        if (alvo instanceof EndCrystal cristal) return cristal.isAlive();
        if (alvo instanceof EnderDragonPart parte) alvo = parte.parentMob;
        if (alvo == atacante || !(alvo instanceof LivingEntity vivo) || !vivo.isAlive()) return false;
        if (alvo instanceof Player outro) return atacante.canHarmPlayer(outro);
        // Os aliados do laço não levam os golpes do próprio dono.
        if (alvo instanceof net.minecraft.world.entity.Mob m && atacante.getUUID().equals(Aliados.donoDe(m))) return false;
        return !alvo.isAlliedTo(atacante);
    }

    /** Chefes (dragão, Wither e a tag de chefes): não viram aliados nem saem do lugar com o laço. */
    public static boolean chefe(Entity e) {
        return e instanceof EnderDragon || e instanceof net.minecraft.world.entity.boss.wither.WitherBoss
            || e.getType().builtInRegistryHolder().is(net.minecraftforge.common.Tags.EntityTypes.BOSSES);
    }

    /** Pode levar golpe de poder: criaturas vivas, os cristais do End e as partes do Ender Dragon. */
    public static boolean atingivel(Entity e) {
        return e instanceof LivingEntity || e instanceof EndCrystal || e instanceof EnderDragonPart;
    }

    public static void ferir(ServerPlayer atacante, Entity alvo, DamageSource fonte, float dano) {
        if (atacante.level() instanceof ServerLevel level && podeAtingir(atacante, alvo)) {
            // O dragão só aceita dano que venha de um jogador: gelo, fogo e afins sem autor viram ataque do jogador.
            boolean dragao = alvo instanceof EnderDragon || alvo instanceof EnderDragonPart || alvo instanceof EndCrystal;
            if (dragao && fonte.getEntity() == null) fonte = level.damageSources().playerAttack(atacante);
            alvo.hurtServer(level, fonte, dano);
        }
    }

    /**
     * Cristais do End dentro de uma área de efeito (as áreas procuram só criaturas vivas): cada um é atingido pelo
     * atacante e explode como em um golpe normal.
     */
    public static void cristais(ServerPlayer atacante, AABB area, DamageSource fonte) {
        if (atacante == null || !(atacante.level() instanceof ServerLevel level)) return;
        for (EndCrystal c : level.getEntitiesOfClass(EndCrystal.class, area, EndCrystal::isAlive)) {
            ferir(atacante, c, fonte, 1);
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
