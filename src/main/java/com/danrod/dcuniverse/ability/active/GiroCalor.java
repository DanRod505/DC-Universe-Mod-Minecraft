package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.ability.Congelamento;
import com.danrod.dcuniverse.ability.Coreografia;
import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.core.Canal;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * spinning_beam (Giro de Calor): uma cena em três tempos, no mesmo compasso da animação "anim" (superman.giro).
 * <ol>
 * <li>Preparo ("charge", s): agacha, sobe "height" blocos e os olhos acendem (o cliente faz a subida, client/Voador).</li>
 * <li>Giro ("duration", s): parado no ar, o raio gira "turns" voltas acelerando, subindo e descendo entre "pitch_min" e
 * "pitch_max" graus abaixo do horizonte; o rastro dele forma os anéis em espiral (client/Efeitos). Quem o raio cruza
 * leva "damage_mult" (no máximo uma vez a cada "hit_interval" s), pega fogo, é empurrado ("knockback", "launch") e
 * fica com o contorno aceso; o chão onde ele bate funde, acende e pega fogo.</li>
 * <li>Queda: ele cai e bate no chão; a onda ("blast_damage_mult", "blast_knockback", "blast_launch") arremessa quem
 * está no alcance.</li>
 * </ol>
 * Os tempos e o ângulo do raio são funções puras (yaw, pitch), as mesmas no servidor e no cliente.
 */
public final class GiroCalor implements TipoHabilidade {
    private static final List<Ativo> ATIVOS = new ArrayList<>();
    /** Ticks entre o fim do giro e a batida no chão (a queda). */
    public static final int QUEDA = 3;
    /** Fração do giro em que ele ainda acelera. */
    private static final float ACELERA = 0.3f;

    private static final class Ativo {
        final ServerPlayer dono;
        final Params prm;
        final int preparo, ticks, intervalo;
        final float voltas, alcance, yaw0, pitchMin, pitchMax, dano;
        final Map<Integer, Integer> proximo = new HashMap<>();
        int idade;

        Ativo(ServerPlayer dono, Personagem p, Params prm) {
            this.dono = dono;
            this.prm = prm;
            this.preparo = Math.max(1, Math.round(prm.f("charge", 1f) * 20));
            this.ticks = Math.max(10, Math.round(prm.f("duration", 2.5f) * 20));
            this.intervalo = Math.max(1, Math.round(prm.f("hit_interval", 0.5f) * 20));
            this.voltas = prm.f("turns", 6f);
            this.alcance = prm.f("range", 12f);
            this.yaw0 = dono.getYRot();
            this.pitchMin = prm.f("pitch_min", 14f);
            this.pitchMax = prm.f("pitch_max", 42f);
            this.dano = Alvos.dano(p, prm.d("damage_mult", 0.35));
        }
    }

    @Override
    public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        if (!(player.level() instanceof ServerLevel)) return false;
        Params prm = h.params();
        ATIVOS.removeIf(a -> a.dono == player);
        Ativo a = new Ativo(player, p, prm);
        ATIVOS.add(a);
        String anim = prm.s("anim", "");
        if (!anim.isEmpty()) Coreografia.animar(player, anim);
        if (player.connection != null) {
            Rede.CANAL.send(new Rede.Giro(player.getId(), a.preparo, a.ticks, a.voltas, a.alcance, prm.color("color", 0xFF2A1A), a.yaw0,
                prm.f("beam_scale", 1f), prm.f("height", 3f), a.pitchMin, a.pitchMax), Canal.rastreandoEProprio(player));
        }
        return true;
    }

    // ------------------------------------------------------------------ o tempo e o ângulo do raio (servidor e cliente)

    /** Quanto do giro já passou (0 a 1) em voltas: acelera no começo e depois segue rápido. */
    public static float avanco(float u) {
        u = Mth.clamp(u, 0f, 1f);
        float e = u < ACELERA ? u * u / (2 * ACELERA) : u - ACELERA / 2;
        return e / (1 - ACELERA / 2);
    }

    /** Direção do raio (yaw do jogo, graus) na fração u do giro. */
    public static float yaw(float yaw0, float voltas, float u) {
        return yaw0 + 360f * voltas * avanco(u);
    }

    /** Inclinação do raio (graus para baixo) na fração u: sobe e desce uma vez e meia, riscando anéis em alturas diferentes. */
    public static float pitch(float min, float max, float u) {
        return min + (max - min) * (0.5f - 0.5f * Mth.cos(u * Mth.TWO_PI * 1.5f));
    }

    /** Tick do servidor: preparo, varredura do giro e a batida no chão no fim. */
    public static void tick() {
        Iterator<Ativo> it = ATIVOS.iterator();
        while (it.hasNext()) {
            Ativo a = it.next();
            ServerPlayer pl = a.dono;
            if (pl.isRemoved() || !pl.isAlive() || EstadoJogador.de(pl).personagem() == null
                || !(pl.level() instanceof ServerLevel level)) {
                it.remove();
                continue;
            }
            pl.resetFallDistance();
            int t = a.idade++ - a.preparo;
            if (t < 0) continue;
            if (t < a.ticks) varrer(level, pl, a, t);
            else if (t == a.ticks + QUEDA) {
                batida(level, pl, a);
                it.remove();
            }
        }
    }

    /** Um tick do giro: o setor entre o raio de antes e o de agora fere quem está nele; o chão onde ele bate esquenta. */
    private static void varrer(ServerLevel level, ServerPlayer pl, Ativo a, int t) {
        float de = yaw(a.yaw0, a.voltas, t / (float) a.ticks), ate = yaw(a.yaw0, a.voltas, (t + 1) / (float) a.ticks);
        AABB area = pl.getBoundingBox().inflate(a.alcance, a.alcance * 0.6, a.alcance);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area, e -> Alvos.podeAtingir(pl, e))) {
            Vec3 para = e.position().subtract(pl.position());
            double dist = Math.sqrt(para.x * para.x + para.z * para.z);
            if (dist > a.alcance + e.getBbWidth() / 2 || a.proximo.getOrDefault(e.getId(), 0) > t) continue;
            // Ângulo do alvo no mesmo sistema do yaw do jogo, e se ele cai entre o raio de antes e o de agora.
            float ang = (float) (Mth.atan2(para.z, para.x) * Mth.RAD_TO_DEG) - 90f;
            float folga = (float) Math.toDegrees(Math.atan2(e.getBbWidth() / 2 + 0.4, Math.max(0.5, dist)));
            if (ate - de >= 360f || !entre(ang, de - folga, ate + folga)) continue;
            a.proximo.put(e.getId(), t + a.intervalo);
            atingir(level, pl, a.prm, e, a.dano, a.prm.f("knockback", 0.7f), a.prm.f("launch", 0.35f));
        }
        // Onde o raio bate: funde, acende e risca de fogo (a cada 2 ticks, para não pesar).
        if (t % 2 == 0 && pl.mayBuild()) {
            Vec3 olhos = pl.getEyePosition();
            Vec3 dir = Vec3.directionFromRotation(pitch(a.pitchMin, a.pitchMax, t / (float) a.ticks), ate);
            BlockHitResult bh = level.clip(new ClipContext(olhos, olhos.add(dir.scale(a.alcance)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, pl));
            if (bh.getType() == HitResult.Type.BLOCK && level.mayInteract(pl, bh.getBlockPos())) {
                Raio.esquentar(level, pl, bh, a.prm.b("melts", true), a.prm.b("sets_fire", true) && t % 6 == 0,
                    a.prm.b("lights_furnace", true));
            }
        }
    }

    /** O fim: a batida no chão arremessa e queima quem está no alcance. */
    private static void batida(ServerLevel level, ServerPlayer pl, Ativo a) {
        float raio = a.alcance * 0.8f;
        float dano = Alvos.dano(EstadoJogador.de(pl).personagem(), a.prm.d("blast_damage_mult", 1.2));
        AABB area = pl.getBoundingBox().inflate(raio, 4, raio);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area, e -> Alvos.podeAtingir(pl, e))) {
            double dist = e.position().subtract(pl.position()).horizontalDistance();
            if (dist > raio) continue;
            float perto = (float) (1 - 0.5 * dist / raio);
            atingir(level, pl, a.prm, e, dano * perto, a.prm.f("blast_knockback", 2.2f) * perto, a.prm.f("blast_launch", 0.8f) * perto);
        }
        Alvos.cristais(pl, area, level.damageSources().playerAttack(pl));
        level.playSound(null, pl.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8f, 0.7f);
        if (crateraLigada(level) && pl.mayBuild()) {
            cratera(level, pl, pl.blockPosition(), a.prm.f("crater_radius", 4.5f), a.prm.f("crater_depth", 2.5f));
        }
    }

    /** A cratera está ligada: opção crateraGiro, e em servidor dedicado também crateraGiroServidor. */
    public static boolean crateraLigada(ServerLevel level) {
        boolean dedicado = level.getServer() != null && level.getServer().isDedicatedServer();
        return com.danrod.dcuniverse.core.Config.CRATERA_GIRO.get() && (!dedicado || com.danrod.dcuniverse.core.Config.CRATERA_GIRO_SERVIDOR.get());
    }

    /**
     * Cratera rasa onde ele bateu: uma tigela de até "crater_depth" blocos no meio, sumindo até a borda. Só tira o que é
     * chão natural (terra, grama, areia, cascalho, argila, neve e pedra comum, mais as plantas em cima); minério, bloco
     * com conteúdo (baú, fornalha...) e qualquer outro bloco ficam onde estão. Perto de qualquer estrutura (Smallville,
     * cidades, vilas) não abre nada. O fundo fica em brasa (magma no meio, basalto e terra queimada em volta).
     */
    public static int cratera(ServerLevel level, ServerPlayer pl, BlockPos centro, float raio, float fundo) {
        int r = (int) Math.ceil(raio);
        // Achar o chão embaixo de quem bateu.
        BlockPos.MutableBlockPos m = centro.mutable();
        for (int i = 0; i < 6 && level.getBlockState(m).isAir(); i++) m.move(0, -1, 0);
        BlockPos chao = m.immutable();
        if (perto(level, chao, r + 4)) return 0;
        int tirados = 0;
        net.minecraft.util.RandomSource rnd = level.getRandom();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz) / raio;
                if (d > 1.25) continue;
                // Topo do terreno nessa coluna (até 3 acima e 4 abaixo do chão do centro).
                BlockPos topo = null;
                for (int dy = 3; dy >= -4; dy--) {
                    BlockPos p = chao.offset(dx, dy, dz);
                    if (!level.getBlockState(p).isAir() && !level.getBlockState(p).canBeReplaced()) {
                        topo = p;
                        break;
                    }
                }
                if (topo == null || !level.mayInteract(pl, topo)) continue;
                // Plantas por cima saem junto.
                BlockPos acima = topo.above();
                if (level.getBlockState(acima).canBeReplaced() && !level.getBlockState(acima).isAir() && level.getBlockEntity(acima) == null) {
                    level.setBlock(acima, Blocks.AIR.defaultBlockState(), 3);
                }
                int prof = d >= 1 ? 0 : (int) Math.round(fundo * (1 - d * d) + (rnd.nextFloat() - 0.5f) * 0.6f);
                BlockPos p = topo;
                boolean parou = false;
                for (int i = 0; i < prof; i++, p = p.below()) {
                    if (!natural(level, p)) {
                        parou = true;
                        break;
                    }
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    tirados++;
                }
                // O fundo (ou a borda) fica queimado, se for chão natural.
                if (!parou && natural(level, p)) {
                    BlockState queimado = d < 0.45 ? (rnd.nextFloat() < 0.7f ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState())
                        : d < 0.95 ? (rnd.nextFloat() < 0.5f ? Blocks.BASALT.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState())
                        : (rnd.nextFloat() < 0.55f ? Blocks.COARSE_DIRT.defaultBlockState() : null);
                    if (queimado != null) level.setBlock(p, queimado, 3);
                    if (d < 0.95 && rnd.nextFloat() < 0.18f && level.getBlockState(p.above()).isAir()) {
                        level.setBlock(p.above(), net.minecraft.world.level.block.BaseFireBlock.getState(level, p.above()), 3);
                    }
                }
            }
        }
        return tirados;
    }

    /** Chão natural que a cratera pode tirar. */
    public static boolean natural(ServerLevel level, BlockPos p) {
        BlockState b = level.getBlockState(p);
        if (b.isAir() || level.getBlockEntity(p) != null) return false;
        return b.is(net.minecraft.tags.BlockTags.DIRT) || b.is(net.minecraft.tags.BlockTags.GRASS_BLOCKS) || b.is(net.minecraft.tags.BlockTags.SAND)
            || b.is(Blocks.MYCELIUM) || b.is(Blocks.PODZOL) || b.is(Blocks.DIRT_PATH) || b.is(Blocks.GRAVEL) || b.is(Blocks.CLAY)
            || b.is(Blocks.SNOW_BLOCK) || b.is(Blocks.SNOW) || b.is(Blocks.STONE) || b.is(Blocks.GRANITE) || b.is(Blocks.DIORITE)
            || b.is(Blocks.ANDESITE) || b.is(Blocks.TUFF) || b.is(Blocks.DEEPSLATE) || b.is(Blocks.SANDSTONE) || b.is(Blocks.RED_SANDSTONE)
            || b.is(Blocks.NETHERRACK) || b.is(Blocks.END_STONE);
    }

    /** Há alguma estrutura (do jogo ou do mod) a menos de `margem` blocos? Aí a cratera não abre. */
    public static boolean perto(ServerLevel level, BlockPos p, int margem) {
        var sm = level.structureManager();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos q = p.offset(dx * 16, 0, dz * 16);
                for (var en : sm.getAllStructuresAt(q).entrySet()) {
                    StructureStart st = sm.getStructureAt(q, en.getKey());
                    if (st.isValid() && st.getBoundingBox().inflatedBy(margem).isInside(p)) return true;
                    for (long ref : en.getValue()) {
                        net.minecraft.world.level.ChunkPos cp = net.minecraft.world.level.ChunkPos.unpack(ref);
                        StructureStart s2 = sm.getStartForStructure(net.minecraft.core.SectionPos.of(cp, 0), en.getKey(),
                            level.getChunk(cp.x(), cp.z()));
                        if (s2 != null && s2.isValid() && s2.getBoundingBox().inflatedBy(margem).isInside(p)) return true;
                    }
                }
            }
        }
        return false;
    }

    private static void atingir(ServerLevel level, ServerPlayer pl, Params prm, LivingEntity e, float dano, float empurrao, float lancar) {
        if (prm.b("sets_fire", true)) e.igniteForSeconds(4);
        if (prm.b("cooks_drops", true)) com.danrod.dcuniverse.ability.Assar.marcar(e);
        Alvos.ferir(pl, e, level.damageSources().playerAttack(pl), dano);
        Congelamento.derreter(e);
        // Contorno aceso: o cliente pinta de vermelho o brilho de quem está perto de um giro (client/Efeitos).
        e.addEffect(new MobEffectInstance(MobEffects.GLOWING, Math.round(prm.f("glow", 3f) * 20), 0, false, false));
        Vec3 fora = e.position().subtract(pl.position()).multiply(1, 0, 1);
        fora = fora.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : fora.normalize();
        e.push(fora.x * empurrao, lancar, fora.z * empurrao);
        e.hurtMarked = true;
    }

    /** O ângulo x (graus) está no arco de a até b (b > a, arco menor que uma volta)? */
    static boolean entre(float x, float a, float b) {
        float d = Mth.wrapDegrees(x - a);
        if (d < 0) d += 360f;
        return d <= b - a;
    }

    /** Está no meio do giro (preparo, giro ou queda): as outras habilidades esperam. */
    public static boolean girando(ServerPlayer player) {
        return ATIVOS.stream().anyMatch(a -> a.dono == player);
    }

    public static void limpar() {
        ATIVOS.clear();
    }
}
