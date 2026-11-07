package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Coreografia no cliente: os eventos que a animação de uma habilidade marca no tempo ("dcuniverse:eventos" em
 * assets/dcuniverse/animacoes) viram câmera, luz, som e partículas no quadro certo, para todo mundo que vê a cena.
 * O dano e os efeitos no mundo continuam no servidor (ability/Coreografia segura a habilidade até o momento certo).
 * <p>
 * Campos de um evento: "t" (segundos), "tremor" (graus) e "tremor_ticks", "zoom" (positivo fecha, negativo abre),
 * "flash" ("#RRGGBB" ou "cor" = cor do herói) e "clarao" (força), "som" (id), "vol" e "tom", "fx" (partículas:
 * olhos, inspira, agacha, estrondo, decola, aterrissa, brilho).
 */
public final class Cena {
    private Cena() {}

    static void evento(Entity e, JsonObject ev) {
        Minecraft mc = Minecraft.getInstance();
        if (!(e.level() instanceof ClientLevel level) || mc.player == null) return;
        boolean meu = e == mc.player;
        double dist = meu ? 0 : mc.player.distanceTo(e);
        // A câmera de quem está perto também sente (menos), a de quem está longe não.
        float perto = meu ? 1f : (float) Mth.clamp(1 - dist / 20, 0, 1) * 0.5f;
        int cor = cor(e.getId());
        if (perto > 0) {
            float tremor = f(ev, "tremor", 0) * perto;
            float zoom = meu ? f(ev, "zoom", 0) : 0;
            float clarao = f(ev, "clarao", 0) * (meu ? 1 : perto * 0.6f);
            int flash = ev.has("flash") ? (ev.get("flash").getAsString().equals("cor") ? Efeitos.misturar(cor, 0xFFFFFF, 0.6f)
                : Integer.parseInt(ev.get("flash").getAsString().substring(1), 16)) : 0xFFFFFF;
            if (tremor > 0 || zoom != 0 || clarao > 0) Combate.cena(tremor, Math.round(f(ev, "tremor_ticks", 4)), zoom, flash, clarao);
        }
        if (ev.has("som")) {
            SoundEvent som = SoundEvent.createVariableRangeEvent(Identifier.parse(ev.get("som").getAsString()));
            level.playLocalSound(e.getX(), e.getY() + 1, e.getZ(), som, SoundSource.PLAYERS, f(ev, "vol", 1), f(ev, "tom", 1), false);
        }
        if (ev.has("fx")) fx(level, e, ev.get("fx").getAsString(), cor, meu && mc.options.getCameraType().isFirstPerson());
    }

    private static void fx(ClientLevel level, Entity e, String fx, int cor, boolean primeiraPessoa) {
        RandomSource rnd = level.getRandom();
        Vec3 olhar = e.getViewVector(1f);
        Vec3 lado = olhar.cross(new Vec3(0, 1, 0));
        lado = lado.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : lado.normalize();
        Vec3 olhos = e.getEyePosition();
        switch (fx) {
            case "olhos" -> {
                // Os olhos acendem: dois pontos de luz vermelha e um bafo de calor subindo.
                if (primeiraPessoa) break;
                for (int s = -1; s <= 1; s += 2) {
                    Vec3 o = olhos.add(olhar.scale(0.3)).add(lado.scale(0.12 * s));
                    level.addParticle(new DustParticleOptions(0xFFF0C0, 0.5f), o.x, o.y, o.z, 0, 0, 0);
                    for (int i = 0; i < 6; i++) {
                        level.addParticle(new DustParticleOptions(i % 2 == 0 ? 0xFF2A1A : 0xFFB070, 0.6f), o.x, o.y, o.z,
                            olhar.x * 0.05 + rnd.nextGaussian() * 0.02, 0.02, olhar.z * 0.05 + rnd.nextGaussian() * 0.02);
                    }
                }
                for (int i = 0; i < 4; i++) level.addParticle(ParticleTypes.WHITE_SMOKE, olhos.x + rnd.nextGaussian() * 0.15,
                    olhos.y + 0.3, olhos.z + rnd.nextGaussian() * 0.15, 0, 0.03, 0);
            }
            case "inspira" -> {
                // O ar entra: flocos e névoa correndo de longe para a boca.
                Vec3 boca = olhos.add(olhar.scale(0.3)).add(0, -0.15, 0);
                for (int i = 0; i < 26; i++) {
                    Vec3 de = boca.add(olhar.scale(1.5 + rnd.nextDouble() * 1.5)).add(rnd.nextGaussian() * 0.8, rnd.nextGaussian() * 0.5, rnd.nextGaussian() * 0.8);
                    Vec3 v = boca.subtract(de).scale(0.12);
                    level.addParticle(i % 3 == 0 ? ParticleTypes.SNOWFLAKE : ParticleTypes.CLOUD, de.x, de.y, de.z, v.x, v.y, v.z);
                }
            }
            case "agacha" -> poeira(level, e, 10, 0.12);
            case "estrondo" -> {
                // O arranque rompe o ar: anel em pé virado para onde ele vai e um clarão no punho.
                Vec3 c = e.position().add(0, e.getBbHeight() * 0.55, 0).add(olhar.scale(primeiraPessoa ? 1.6 : 0.8));
                Vec3 cima = lado.cross(olhar).normalize();
                level.addParticle(ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF), c.x, c.y, c.z, 0, 0, 0);
                for (int i = 0; i < 40; i++) {
                    double a = i * Math.PI * 2 / 40;
                    Vec3 d = lado.scale(Math.cos(a)).add(cima.scale(Math.sin(a)));
                    level.addParticle(new DustParticleOptions(i % 2 == 0 ? 0xFFFFFF : Efeitos.misturar(cor, 0xFFFFFF, 0.4f), 1.2f),
                        c.x, c.y, c.z, d.x * 0.45 - olhar.x * 0.2, d.y * 0.45 - olhar.y * 0.2, d.z * 0.45 - olhar.z * 0.2);
                }
                for (int i = 0; i < 10; i++) level.addParticle(ParticleTypes.POOF, c.x, c.y, c.z, -olhar.x * 0.3 + rnd.nextGaussian() * 0.1,
                    -olhar.y * 0.3 + rnd.nextGaussian() * 0.1, -olhar.z * 0.3 + rnd.nextGaussian() * 0.1);
            }
            case "decola" -> {
                poeira(level, e, 40, 0.4);
                level.addParticle(ParticleTypes.EXPLOSION, e.getX(), e.getY() + 0.2, e.getZ(), 0, 0, 0);
                for (int i = 0; i < 24; i++) level.addParticle(ParticleTypes.CLOUD, e.getX(), e.getY() + 0.1, e.getZ(),
                    rnd.nextGaussian() * 0.25, 0.05, rnd.nextGaussian() * 0.25);
            }
            case "aterrissa" -> {
                poeira(level, e, 60, 0.55);
                Combate.onda(level, e.position(), cor, 1.5f);
            }
            case "brilho" -> {
                Vec3 c = e.position().add(0, e.getBbHeight() * 0.6, 0);
                level.addParticle(ColorParticleOption.create(ParticleTypes.FLASH, 0xFF000000 | Efeitos.misturar(cor, 0xFFFFFF, 0.5f)), c.x, c.y, c.z, 0, 0, 0);
                for (int i = 0; i < 20; i++) level.addParticle(ParticleTypes.END_ROD, c.x, c.y, c.z,
                    rnd.nextGaussian() * 0.15, rnd.nextGaussian() * 0.15, rnd.nextGaussian() * 0.15);
            }
            default -> {}
        }
    }

    /** Poeira e pedaços do chão espirrando em anel nos pés. */
    private static void poeira(ClientLevel level, Entity e, int n, double v) {
        RandomSource rnd = level.getRandom();
        BlockState piso = level.getBlockState(BlockPos.containing(e.getX(), e.getY() - 0.2, e.getZ()));
        for (int i = 0; i < n; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, s = v * (0.6 + rnd.nextDouble() * 0.6);
            double vx = Math.cos(a) * s, vz = Math.sin(a) * s;
            if (!piso.isAir()) level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, piso), e.getX(), e.getY() + 0.1, e.getZ(), vx, 0.15 + rnd.nextDouble() * 0.2, vz);
            if (i % 3 == 0) level.addParticle(ParticleTypes.POOF, e.getX(), e.getY() + 0.1, e.getZ(), vx * 0.6, 0.02, vz * 0.6);
        }
    }

    private static float f(JsonObject o, String k, float def) {
        return o.has(k) ? o.get(k).getAsFloat() : def;
    }

    private static int cor(int entidade) {
        Rede.Aparencia a = EstadoCliente.APARENCIAS.get(entidade);
        return a == null ? 0x3A7BFF : (a.rastro() != 0 ? a.rastro() : a.cor()) & 0xFFFFFF;
    }
}
