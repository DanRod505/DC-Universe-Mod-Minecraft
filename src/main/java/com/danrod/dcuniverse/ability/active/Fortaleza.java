package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.character.Transformacao;
import com.danrod.dcuniverse.content.Blocos;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.energy.Energia;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * structure: ergue uma construção temporária de cristal onde o jogador mira (suprema Fortaleza da Solidão).
 * Parâmetros: radius, duration (s), range (até onde dá para mirar), regen_level (nível da Regeneração de
 * quem está dentro) e energy_per_second (energia extra da câmara solar para quem está transformado).
 * O cristal só ocupa ar ou plantas, nunca onde há alguém, e some sozinho no fim (tick agendado no bloco).
 * Dentro, todos os jogadores se curam e os monstros são empurrados para fora.
 */
public final class Fortaleza implements TipoHabilidade {
    /** Ticks que a fortaleza leva para crescer do chão. */
    public static final int CRESCE = 40;
    private static final List<Ativa> ATIVAS = new ArrayList<>();

    private static final class Ativa {
        final ServerLevel level;
        final ServerPlayer dono;
        final BlockPos base;
        final int raio, altura, regen;
        final float energia;
        final long fim;
        final List<BlockPos> blocos;
        int colocados;

        Ativa(ServerLevel level, ServerPlayer dono, BlockPos base, int raio, int altura, int regen, float energia, long fim, List<BlockPos> blocos) {
            this.level = level;
            this.dono = dono;
            this.base = base;
            this.raio = raio;
            this.altura = altura;
            this.regen = regen;
            this.energia = energia;
            this.fim = fim;
            this.blocos = blocos;
        }
    }

    @Override
    public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        Params prm = h.params();
        BlockPos base = chao(level, player, prm.d("range", 24));
        if (base == null) {
            Transformacao.avisar(player, Component.translatable("dcuniverse.msg.sem_chao"));
            return false;
        }
        int raio = Math.max(3, prm.i("radius", 6));
        Vec3 para = player.position().subtract(Vec3.atBottomCenterOf(base));
        Direction porta = para.horizontalDistanceSqr() < 1 ? player.getDirection().getOpposite() : Direction.getApproximateNearest(para.x, 0, para.z);
        RandomSource rnd = RandomSource.create(level.getGameTime() ^ base.asLong());
        List<BlockPos> forma = forma(raio, porta, rnd);
        List<BlockPos> blocos = new ArrayList<>(forma.size());
        for (BlockPos rel : forma) blocos.add(base.offset(rel));
        int altura = forma.stream().mapToInt(BlockPos::getY).max().orElse(0);
        long fim = level.getGameTime() + CRESCE + Math.round(prm.f("duration", 120) * 20);
        ATIVAS.add(new Ativa(level, player, base, raio, altura, Math.max(0, prm.i("regen_level", 2) - 1),
            prm.f("energy_per_second", 8), fim, blocos));
        level.playSound(null, base, Sons.FORTALEZA_CRESCE.get(), SoundSource.PLAYERS, 2.5f, 1f);
        Rede.efeito(level, base.getX() + 0.5, base.getY(), base.getZ() + 0.5, "fortress", raio, (float) (fim - level.getGameTime()), altura);
        return true;
    }

    /** Onde a fortaleza nasce: o chão no ponto mirado (ou sob o jogador), até 8 blocos abaixo. */
    private static BlockPos chao(ServerLevel level, ServerPlayer player, double alcance) {
        BlockHitResult hit = Alvos.mirarBloco(player, alcance, ClipContext.Fluid.NONE);
        BlockPos pos = hit.getType() == HitResult.Type.MISS ? player.blockPosition() : hit.getBlockPos().relative(hit.getDirection());
        for (int i = 0; i < 8; i++, pos = pos.below()) {
            BlockPos abaixo = pos.below();
            if (level.getBlockState(pos).canBeReplaced() && level.getBlockState(abaixo).isFaceSturdy(level, abaixo, Direction.UP)) return pos;
        }
        return null;
    }

    /**
     * Forma da fortaleza, em posições relativas à base, na ordem em que crescem (de baixo para cima):
     * cúpula alta de cristal com a porta virada para quem usou, torres pontudas inclinadas para fora em volta
     * (menos na frente da porta) e o pedestal no centro.
     */
    public static List<BlockPos> forma(int r, Direction porta, RandomSource rnd) {
        Set<BlockPos> s = new LinkedHashSet<>();
        double alto = r * 1.2;
        for (int y = 0; y <= Math.ceil(alto) + 1; y++) {
            for (int x = -r - 1; x <= r + 1; x++) {
                for (int z = -r - 1; z <= r + 1; z++) {
                    double e = Math.sqrt((x * x + z * z) / (double) (r * r) + (y * y) / (alto * alto)) * r;
                    if (e >= r - 0.65 && e < r + 0.35) s.add(new BlockPos(x, y, z));
                }
            }
        }
        // Porta: 3 de largura e 4 de altura atravessando a parede.
        s.removeIf(b -> {
            int frente = b.getX() * porta.getStepX() + b.getZ() * porta.getStepZ();
            int lado = Math.abs(b.getX() * porta.getStepZ() - b.getZ() * porta.getStepX());
            return frente > 0 && lado <= 1 && b.getY() <= 3;
        });
        double angPorta = Math.atan2(porta.getStepZ(), porta.getStepX());
        // Anel principal de torres grandes, um anel externo de torres menores e mais inclinadas, e o
        // feixe de cristais que sai do alto da cúpula: o desenho clássico da Fortaleza.
        int torres = 11 + r / 2;
        for (int i = 0; i < torres; i++) {
            double ang = (i + rnd.nextDouble() * 0.6) * Math.PI * 2 / torres;
            if (pertoDaPorta(ang, angPorta)) continue;
            torre(s, ang, r - 1.2 + rnd.nextDouble() * 1.2, 0, r * (1.3 + rnd.nextDouble() * 1.3),
                0.18 + rnd.nextDouble() * 0.25, 1.5 + rnd.nextDouble() * 0.9);
        }
        int menores = 8 + r / 2;
        for (int i = 0; i < menores; i++) {
            double ang = (i + 0.5 + rnd.nextDouble() * 0.4) * Math.PI * 2 / menores;
            if (pertoDaPorta(ang, angPorta)) continue;
            torre(s, ang, r + 0.8 + rnd.nextDouble(), 0, r * (0.55 + rnd.nextDouble() * 0.5),
                0.45 + rnd.nextDouble() * 0.35, 1.0 + rnd.nextDouble() * 0.6);
        }
        int topo = (int) Math.floor(alto) - 1;
        torre(s, 0, 0, topo, r * 1.1, 0, 1.9);
        for (int i = 0; i < 5; i++) {
            double ang = i * Math.PI * 2 / 5 + rnd.nextDouble() * 0.5;
            torre(s, ang, 1.2, topo - 1, r * (0.6 + rnd.nextDouble() * 0.35), 0.25 + rnd.nextDouble() * 0.2, 1.2);
        }
        // Pedestal do centro, onde fica o cristal-mestre.
        s.add(BlockPos.ZERO);
        List<BlockPos> lista = new ArrayList<>(s);
        lista.sort(Comparator.<BlockPos>comparingInt(BlockPos::getY).thenComparingInt(b -> b.getX() * b.getX() + b.getZ() * b.getZ()));
        return lista;
    }

    private static boolean pertoDaPorta(double ang, double angPorta) {
        return Math.abs(Math.atan2(Math.sin(ang - angPorta), Math.cos(ang - angPorta))) < 0.42;
    }

    /**
     * Uma torre de cristal: nasce no ângulo ang, a raio0 do centro e na altura y0, sobe até h blocos
     * afinando até a ponta e inclinando para fora (inclina blocos por bloco de altura).
     */
    private static void torre(Set<BlockPos> s, double ang, double raio0, int y0, double h, double inclina, double grossura) {
        for (double t = 0; t <= h; t += 0.5) {
            double raioT = raio0 + inclina * t;
            double cx = Math.cos(ang) * raioT, cz = Math.sin(ang) * raioT;
            double g = grossura * (1 - t / h) + 0.3;
            int gi = (int) Math.ceil(g);
            for (int dx = -gi; dx <= gi; dx++) {
                for (int dz = -gi; dz <= gi; dz++) {
                    int bx = (int) Math.floor(cx) + dx, bz = (int) Math.floor(cz) + dz;
                    double px = bx + 0.5 - cx, pz = bz + 0.5 - cz;
                    if (px * px + pz * pz <= g * g) s.add(new BlockPos(bx, y0 + (int) t, bz));
                }
            }
        }
    }

    /** Tick do servidor: faz a fortaleza crescer e cuida de quem está dentro. */
    public static void tick() {
        ATIVAS.removeIf(Fortaleza::avancar);
    }

    /** @return true quando a fortaleza acabou */
    private static boolean avancar(Ativa f) {
        long agora = f.level.getGameTime();
        if (f.colocados < f.blocos.size()) crescer(f, agora);
        if (agora >= f.fim) {
            Vec3 c = Vec3.atBottomCenterOf(f.base);
            f.level.playSound(null, c.x, c.y + 3, c.z, Sons.FORTALEZA_QUEBRA.get(), SoundSource.PLAYERS, 2.5f, 1f);
            Rede.efeito(f.level, c.x, c.y, c.z, "fortress_end", f.raio, f.altura, 0);
            return true;
        }
        if (agora % 10 == 0) cuidar(f);
        return false;
    }

    private static void crescer(Ativa f, long agora) {
        int porTick = Math.max(1, (int) Math.ceil(f.blocos.size() / (double) CRESCE));
        ServerPlayer dono = f.dono.isRemoved() ? null : f.dono;
        BlockState cristal = Blocos.CRISTAL_FORTALEZA.get().defaultBlockState();
        Block bloco = cristal.getBlock();
        for (int i = 0; i < porTick && f.colocados < f.blocos.size(); i++) {
            BlockPos pos = f.blocos.get(f.colocados++);
            if (!f.level.isLoaded(pos) || !f.level.getBlockState(pos).canBeReplaced() || !f.level.getFluidState(pos).isEmpty()) continue;
            if (dono != null && !f.level.mayInteract(dono, pos)) continue;
            if (!f.level.getEntitiesOfClass(LivingEntity.class, new AABB(pos)).isEmpty()) continue;
            f.level.setBlock(pos, cristal, 3);
            // Some de cima para baixo: o topo estilhaça primeiro.
            int espera = (int) (f.fim - agora) + (f.altura + f.base.getY() - pos.getY()) * 2 + f.level.getRandom().nextInt(4);
            f.level.scheduleTick(pos, bloco, Math.max(1, espera));
            if (f.colocados % 12 == 0) {
                f.level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.2f, 0.6f + f.level.getRandom().nextFloat() * 0.9f);
            }
        }
    }

    /** Cura e energia para os jogadores de dentro; monstros são empurrados para fora. */
    private static void cuidar(Ativa f) {
        Vec3 c = Vec3.atBottomCenterOf(f.base);
        AABB area = new AABB(c.x - f.raio - 2, c.y - 1, c.z - f.raio - 2, c.x + f.raio + 2, c.y + f.altura + 1, c.z + f.raio + 2);
        Set<ServerPlayer> jogadores = new LinkedHashSet<>(f.level.players());
        if (!f.dono.isRemoved() && f.dono.level() == f.level) jogadores.add(f.dono);
        for (ServerPlayer pl : jogadores) {
            double dx = pl.getX() - c.x, dz = pl.getZ() - c.z;
            if (pl.isSpectator() || !area.contains(pl.position()) || dx * dx + dz * dz >= f.raio * f.raio) continue;
            // Jogadores sem conexão (os de teste) não recebem efeito: o servidor tentaria avisar o cliente.
            if (pl.connection != null) pl.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 50, f.regen, true, true));
            EstadoJogador est = EstadoJogador.de(pl);
            if (est.personagem() != null && f.energia > 0) Energia.adicionar(pl, est.personagem(), f.energia / 2f);
        }
        for (Entity e : f.level.getEntities((Entity) null, area, x -> x.isAlive() && x instanceof Enemy && x instanceof LivingEntity)) {
            double dx = e.getX() - c.x, dz = e.getZ() - c.z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < f.raio + 1.5) {
                Vec3 fora = dist < 0.01 ? new Vec3(1, 0, 0) : new Vec3(dx / dist, 0, dz / dist);
                e.setDeltaMovement(fora.x * 0.8, 0.3, fora.z * 0.8);
                e.hurtMarked = true;
            }
        }
    }

    /** Fortalezas de pé agora (para os testes e o /reload). */
    public static int ativas() {
        return ATIVAS.size();
    }

    public static void limpar() {
        ATIVAS.clear();
    }
}
