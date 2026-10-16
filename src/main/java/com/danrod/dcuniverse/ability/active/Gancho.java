package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.content.BatwingEntidade;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.guia.Guia;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * grapple: o gancho de escalada (seção 2.8). Dispara na hora até range blocos:
 * - num bloco, puxa o jogador até lá (o movimento é calculado no cliente, client/Gancheador, como o voo);
 * - num mob pequeno (até small_size de volume), puxa o mob até o jogador e deixa ele lento;
 * - num mob grande ou num veículo do mod, puxa o jogador até ele (no Batwing, embarca ao chegar).
 * Usar de novo durante a puxada solta o cabo. Parâmetros: range, speed (blocos/s), small_size.
 * Quem está perto vê o cabo (pacote GanchoVisto); o servidor zera a queda enquanto o cabo está preso.
 */
public final class Gancho implements TipoHabilidade {
    public static final int SOLTO = 0, BLOCO = 1, ENTIDADE = 2, PUXA_MOB = 3, ERROU = 4;
    /** Velocidade com que o cabo sai (blocos/tick), só para saber quando ele chega. */
    public static final double CABO_VEL = 4.0;
    /** Quanto tempo dá para ficar pendurado numa parede (ticks). */
    public static final int PENDURADO_MAX = 30 * 20;

    private record Ativo(ServerPlayer dono, int modo, Vec3 ancora, Entity alvo, long inicio, int limite, double vel) {}

    private static final Map<UUID, Ativo> ATIVOS = new HashMap<>();

    @Override
    public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        if (ATIVOS.containsKey(player.getUUID())) {
            soltar(player);
            return false;
        }
        Params prm = h.params();
        return disparar(player, prm.d("range", 40), prm.d("speed", 30), prm.d("small_size", 1.2));
    }

    /** Dispara o gancho (também usado pelo Gancho de Escalada do lote, sem poderes). */
    public static boolean disparar(ServerPlayer player, double alcance, double velBlocosSeg, double pequeno) {
        Vec3 olhos = player.getEyePosition();
        Vec3 fim = olhos.add(player.getLookAngle().scale(alcance));
        BlockHitResult bloco = player.level().clip(new ClipContext(olhos, fim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 limite = bloco.getType() == HitResult.Type.MISS ? fim : bloco.getLocation();
        EntityHitResult ent = ProjectileUtil.getEntityHitResult(player, olhos, limite, new AABB(olhos, limite).inflate(1.0),
            e -> e != player && !e.isSpectator() && e.isPickable() && (e instanceof LivingEntity || e instanceof BatwingEntidade),
            olhos.distanceToSqr(limite));
        double vel = velBlocosSeg / 20.0;
        long agora = player.level().getGameTime();
        Ativo a;
        if (ent != null) {
            Entity alvo = ent.getEntity();
            double dist = ent.getLocation().distanceTo(olhos);
            boolean pequenoMob = alvo instanceof LivingEntity vivo && !(alvo instanceof Player)
                && vivo.getBbWidth() * vivo.getBbWidth() * vivo.getBbHeight() <= pequeno && vivo.getType().getCategory() != net.minecraft.world.entity.MobCategory.MISC;
            boolean pvp = !(alvo instanceof Player) || Alvos.podeAtingir(player, alvo);
            if (pequenoMob && Alvos.podeAtingir(player, alvo)) {
                a = new Ativo(player, PUXA_MOB, ent.getLocation(), alvo, agora, chega(dist) + 20, vel);
            } else if (pvp || alvo instanceof BatwingEntidade) {
                a = new Ativo(player, ENTIDADE, ent.getLocation(), alvo, agora, chega(dist) + (int) (dist / vel) + 40, vel);
            } else {
                a = new Ativo(player, ERROU, ent.getLocation(), null, agora, chega(dist) * 2, vel);
            }
        } else if (bloco.getType() != HitResult.Type.MISS) {
            double dist = bloco.getLocation().distanceTo(olhos);
            // Âncora um pouco para fora da face, para o jogador não entrar no bloco.
            Vec3 ancora = bloco.getLocation().add(Vec3.atLowerCornerOf(bloco.getDirection().getUnitVec3i()).scale(0.05));
            // Num bloco o cliente avisa quando termina (pode ficar pendurado até PENDURADO_MAX ticks).
            a = new Ativo(player, BLOCO, ancora, null, agora, chega(dist) + (int) (dist / vel) + 40 + PENDURADO_MAX, vel);
        } else {
            a = new Ativo(player, ERROU, fim, null, agora, chega(alcance) * 2, vel);
        }
        ATIVOS.put(player.getUUID(), a);
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), Sons.GANCHO_DISPARO.get(), SoundSource.PLAYERS, 1f,
            0.95f + player.getRandom().nextFloat() * 0.1f);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        mostrar(a);
        if (a.modo != ERROU) Guia.concluir(player, "gancho");
        return true;
    }

    /** Ticks até o cabo chegar ao alvo. */
    private static int chega(double dist) {
        return (int) Math.ceil(dist / CABO_VEL);
    }

    public static boolean ativo(Player player) {
        return ATIVOS.containsKey(player.getUUID());
    }

    /** O dono chegou (o cliente avisa) ou soltou o cabo. */
    public static void soltar(ServerPlayer player) {
        Ativo a = ATIVOS.remove(player.getUUID());
        if (a == null) return;
        player.resetFallDistance();
        if (a.modo == ENTIDADE && a.alvo instanceof BatwingEntidade bw && bw.isAlive() && player.distanceTo(bw) < 5) bw.embarcar(player);
        Rede.gancho(player, SOLTO, Vec3.ZERO, -1, 0);
    }

    private static void mostrar(Ativo a) {
        Rede.gancho(a.dono, a.modo, a.ancora, a.alvo == null ? -1 : a.alvo.getId(), (float) a.vel);
    }

    /** Tick do servidor. */
    public static void tick() {
        for (Iterator<Ativo> it = ATIVOS.values().iterator(); it.hasNext(); ) {
            Ativo a = it.next();
            ServerPlayer dono = a.dono;
            long t = dono.level().getGameTime() - a.inicio;
            boolean fim = dono.isRemoved() || !dono.isAlive() || t > a.limite || dono.isPassenger()
                || a.alvo != null && (a.alvo.isRemoved() || a.alvo.level() != dono.level());
            if (!fim && a.modo != ERROU) dono.resetFallDistance();
            if (!fim) {
                switch (a.modo) {
                    case ENTIDADE -> fim = t > 2 && dono.distanceTo(a.alvo) < (a.alvo instanceof BatwingEntidade ? 3.2 : a.alvo.getBbWidth() / 2 + 1.4);
                    case PUXA_MOB -> fim = puxarMob(a, t);
                    default -> {}
                }
            }
            if (fim) {
                it.remove();
                if (dono.isAlive()) {
                    dono.resetFallDistance();
                    if (a.modo == ENTIDADE && a.alvo instanceof BatwingEntidade bw && bw.isAlive() && dono.distanceTo(bw) < 5) bw.embarcar(dono);
                }
                Rede.gancho(dono, SOLTO, Vec3.ZERO, -1, 0);
            }
        }
    }

    /** Puxa o mob até o dono depois que o cabo chega. @return true quando terminou */
    private static boolean puxarMob(Ativo a, long t) {
        if (!(a.alvo instanceof LivingEntity mob) || !(mob.level() instanceof ServerLevel level)) return true;
        if (t < chega(a.ancora.distanceTo(a.dono.getEyePosition()))) return false;
        Vec3 para = a.dono.position().add(a.dono.getLookAngle().multiply(1.6, 0, 1.6)).subtract(mob.position());
        double d = para.length();
        if (d < 1.8) {
            mob.setDeltaMovement(mob.getDeltaMovement().scale(0.2));
            mob.hurtMarked = true;
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 3), a.dono);
            return true;
        }
        Vec3 v = para.normalize().scale(Math.min(d, 1.1)).add(0, 0.12, 0);
        mob.setDeltaMovement(v);
        mob.hurtMarked = true;
        mob.resetFallDistance();
        if (t % 3 == 0) level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), Sons.GANCHO_PUXA.get(), SoundSource.PLAYERS, 0.5f, 1.3f);
        return false;
    }

    public static void limpar() {
        ATIVOS.clear();
    }

    /** Destransformou: o cabo solta. */
    public static void aoDestransformar(ServerPlayer player) {
        if (ATIVOS.containsKey(player.getUUID())) soltar(player);
    }
}
