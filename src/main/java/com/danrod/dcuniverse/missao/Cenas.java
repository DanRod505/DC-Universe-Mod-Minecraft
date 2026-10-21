package com.danrod.dcuniverse.missao;

import com.danrod.dcuniverse.content.Blocos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Cenas de missão: acontecimentos no mundo quando um objetivo termina (campo "event" do objetivo). Por enquanto a
 * chuva de meteoros de "Meteoro em Smallville": três meteoros riscam o céu e caem em volta da cratera, cada um
 * deixando um Meteorito fumegante que solta Fragmentos Alienígenas.
 */
public final class Cenas {
    /** Cor do rastro (a mesma do brilho do Fragmento Alienígena). */
    private static final int COR = 0x5CF2D6;
    private static final List<Queda> QUEDAS = new ArrayList<>();

    private Cenas() {}

    /** Um meteoro em voo: de {@code de} até {@code ate} em {@code dur} ticks, a partir de {@code inicio}. */
    private record Queda(ServerLevel level, Vec3 de, BlockPos ate, long inicio, int dur) {
        Vec3 em(double t) {
            Vec3 fim = Vec3.atCenterOf(ate);
            // Desce acelerando, como quem cai de verdade.
            return de.lerp(fim, t * t);
        }
    }

    public static void iniciar(ServerPlayer p, String evento, BlockPos centro) {
        if (evento.isEmpty() || centro == null) return;
        if ("chuva_meteoros".equals(evento)) chuvaMeteoros(p, centro);
    }

    /** Pontos de queda em volta da cratera (na superfície) e o rastro de cada um vindo do mesmo lado do céu. */
    public static List<BlockPos> chuvaMeteoros(ServerPlayer p, BlockPos centro) {
        ServerLevel level = (ServerLevel) p.level();
        List<BlockPos> pontos = new ArrayList<>();
        // Os meteoros vêm do céu à frente de quem chegou, para a cena ser vista.
        double yaw = Math.toRadians(p.getYRot());
        double ceu = Math.atan2(Math.cos(yaw), -Math.sin(yaw)) + (p.getRandom().nextDouble() - 0.5) * 0.6;
        for (int i = 0; i < 3; i++) {
            double ang = ceu + Math.PI * 0.55 + i * (Math.PI * 2 / 3) + p.getRandom().nextDouble() * 0.6;
            int dist = 7 + p.getRandom().nextInt(6);
            BlockPos chao = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                centro.offset(Mth.floor(Math.cos(ang) * dist), 0, Mth.floor(Math.sin(ang) * dist)));
            pontos.add(chao);
            Vec3 de = Vec3.atCenterOf(chao).add(Math.cos(ceu) * 60, 75, Math.sin(ceu) * 60);
            QUEDAS.add(new Queda(level, de, chao, level.getGameTime() + 6 + i * 22L, 46 + i * 6));
        }
        level.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.AMBIENT, 1.5f, 0.5f);
        if (p.connection != null) p.sendOverlayMessage(Component.translatable("dcuniverse.cena.chuva_meteoros"));
        return pontos;
    }

    public static void tick() {
        QUEDAS.removeIf(q -> {
            long agora = q.level().getGameTime();
            if (agora < q.inicio()) return false;
            double t = (agora - q.inicio()) / (double) q.dur();
            if (t >= 1) {
                impacto(q.level(), q.ate());
                return true;
            }
            Vec3 v = q.em(t);
            var L = q.level();
            // Cabeça: bola de fogo verde-água com clarão; cauda: vários pontos entre o tick anterior e este, para o
            // rastro sair contínuo mesmo com o meteoro rápido lá no alto.
            L.sendParticles(ColorParticleOption.create(ParticleTypes.FLASH, 0xFF000000 | COR), true, true, v.x, v.y, v.z, 1, 0, 0, 0, 0);
            L.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, true, true, v.x, v.y, v.z, 10, 0.4, 0.4, 0.4, 0.03);
            L.sendParticles(ParticleTypes.FLAME, true, true, v.x, v.y, v.z, 8, 0.5, 0.5, 0.5, 0.04);
            Vec3 antes = q.em(Math.max(0, t - 1.0 / q.dur()));
            for (int k = 0; k < 6; k++) {
                Vec3 c = antes.lerp(v, k / 6.0);
                L.sendParticles(new DustParticleOptions(COR, 3.5f), true, true, c.x, c.y, c.z, 2, 0.3, 0.3, 0.3, 0);
                L.sendParticles(ParticleTypes.END_ROD, true, true, c.x, c.y, c.z, 1, 0.15, 0.15, 0.15, 0.005);
                L.sendParticles(ParticleTypes.LARGE_SMOKE, true, true, c.x, c.y, c.z, 2, 0.35, 0.35, 0.35, 0.01);
            }
            if (q.level().getRandom().nextInt(3) == 0) L.sendParticles(ParticleTypes.LAVA, true, true, v.x, v.y, v.z, 2, 0.3, 0.3, 0.3, 0);
            if ((agora - q.inicio()) % 10 == 0) {
                q.level().playSound(null, v.x, v.y, v.z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.AMBIENT, 4f, 0.5f);
            }
            return false;
        });
    }

    /** O meteoro chega: estrondo, chão chamuscado em volta e o Meteorito no meio, ainda soltando fumaça. */
    private static void impacto(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
        level.sendParticles(new DustParticleOptions(COR, 3f), pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 40, 1.2, 0.6, 1.2, 0);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 12, 0.4, 0.3, 0.4, 0.03);
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.AMBIENT, 3f, 0.7f);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx * dx + dz * dz > 5 || level.getRandom().nextInt(4) == 0) continue;
                BlockPos chao = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.offset(dx, 0, dz)).below();
                var s = level.getBlockState(chao);
                if (s.is(BlockTags.DIRT) || s.is(Blocks.SAND) || s.is(Blocks.GRAVEL)) {
                    level.setBlockAndUpdate(chao, (dx == 0 && dz == 0 || level.getRandom().nextBoolean()) ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.TUFF.defaultBlockState());
                }
                BlockPos acima = chao.above();
                if (level.getBlockState(acima).canBeReplaced() && !level.getBlockState(acima).liquid()) level.removeBlock(acima, false);
            }
        }
        if (level.getBlockState(pos).canBeReplaced()) level.setBlockAndUpdate(pos, Blocos.METEORITO.get().defaultBlockState());
        FUMACA.add(new Fumaca(level, pos.immutable(), level.getGameTime() + 20 * 40));
    }

    /** Fumaça que sobe do Meteorito por um tempo, para achar de longe onde ele caiu. */
    private record Fumaca(ServerLevel level, BlockPos pos, long ate) {}

    private static final List<Fumaca> FUMACA = new ArrayList<>();

    public static void tickFumaca() {
        FUMACA.removeIf(f -> {
            if (f.level().getGameTime() > f.ate() || !f.level().getBlockState(f.pos()).is(Blocos.METEORITO.get())) return true;
            if (f.level().getGameTime() % 4 == 0) {
                f.level().sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, false, f.pos().getX() + 0.5, f.pos().getY() + 1.1,
                    f.pos().getZ() + 0.5, 1, 0.15, 0.1, 0.15, 0.01);
            }
            return false;
        });
    }

    public static void limpar() {
        QUEDAS.clear();
        FUMACA.clear();
    }
}
