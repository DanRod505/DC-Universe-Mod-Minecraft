package com.danrod.dcuniverse.world.mundo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Fonte de biomas do tipo de mundo Mundo DC ({@code dcuniverse:mundo_dc}): a do mundo normal ({@code base}) com
 * uma região temática em volta de cada local do mod, sempre no mesmo lugar ({@code x}, {@code z}). Dentro do raio,
 * o bioma passa pelas {@code trocas} (a primeira que pega vale), fica igual se estiver em {@code manter} e, se
 * não, vira o {@code padrao}. Cavernas (tag {@code dcuniverse:mundo_dc/intocados}) nunca mudam.
 * <p>
 * O relevo não depende do bioma (vem só da semente), então a região muda a cara e as plantas, não a forma do
 * terreno. Os locais que ainda não existem no mod já ganham a região deles: quando chegarem numa versão nova, a
 * parte ainda não explorada do mundo já está pronta para eles.
 * <p>
 * As posições também decidem onde cada local nasce (ver mixin ChunkGeneratorStructureStateMixin): o local procura
 * o melhor terreno a até {@code busca} blocos do ponto marcado.
 */
public final class FonteMundoDC extends BiomeSource {
    /** Uma troca de bioma dentro da região: o que estiver em {@code de} vira {@code para}. */
    public record Troca(HolderSet<Biome> de, Holder<Biome> para) {
        static final Codec<Troca> CODEC = RecordCodecBuilder.create(i -> i.group(
            Biome.LIST_CODEC.fieldOf("de").forGetter(Troca::de),
            Biome.CODEC.fieldOf("para").forGetter(Troca::para)
        ).apply(i, Troca::new));
    }

    /** A região de um local (pronto ou reservado para uma versão futura). */
    public record Regiao(String local, int x, int z, int busca, int raio, HolderSet<Biome> manter, Holder<Biome> padrao, List<Troca> trocas) {
        static final Codec<Regiao> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("local").forGetter(Regiao::local),
            Codec.INT.fieldOf("x").forGetter(Regiao::x),
            Codec.INT.fieldOf("z").forGetter(Regiao::z),
            Codec.INT.optionalFieldOf("busca", 160).forGetter(Regiao::busca),
            Codec.INT.fieldOf("raio").forGetter(Regiao::raio),
            Biome.LIST_CODEC.fieldOf("manter").forGetter(Regiao::manter),
            Biome.CODEC.fieldOf("padrao").forGetter(Regiao::padrao),
            Troca.CODEC.listOf().optionalFieldOf("trocas", List.of()).forGetter(Regiao::trocas)
        ).apply(i, Regiao::new));

        /** Raio da região na direção do ponto (bx, bz): ondulado, para a borda não sair um círculo perfeito. */
        double raioNa(int bx, int bz) {
            double ang = Math.atan2(bz - z, bx - x);
            double fase = (local.hashCode() & 1023) / 163.0;
            return raio * (1 + 0.12 * Math.sin(ang * 3 + fase) + 0.07 * Math.sin(ang * 7 - fase * 2) + 0.04 * Math.sin(ang * 13 + fase * 3));
        }

        boolean contem(int bx, int bz) {
            long dx = bx - x, dz = bz - z;
            long max = (long) (raio * 1.25);
            if (Math.abs(dx) > max || Math.abs(dz) > max) return false;
            double r = raioNa(bx, bz);
            return dx * dx + dz * dz <= r * r;
        }
    }

    public static final MapCodec<FonteMundoDC> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        BiomeSource.CODEC.fieldOf("base").forGetter(f -> f.base),
        Biome.LIST_CODEC.optionalFieldOf("intocados").forGetter(f -> f.intocados),
        Regiao.CODEC.listOf().fieldOf("locais").forGetter(f -> f.regioes)
    ).apply(i, FonteMundoDC::new));

    private final BiomeSource base;
    private final Optional<HolderSet<Biome>> intocados;
    private final List<Regiao> regioes;

    public FonteMundoDC(BiomeSource base, Optional<HolderSet<Biome>> intocados, List<Regiao> regioes) {
        this.base = base;
        this.intocados = intocados;
        this.regioes = regioes;
    }

    public List<Regiao> regioes() {
        return regioes;
    }

    /** A região do local com esse id, se o Mundo DC marca um lugar para ele. */
    public Optional<Regiao> regiao(String local) {
        return regioes.stream().filter(r -> r.local.equals(local)).findFirst();
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        Stream<Holder<Biome>> extras = regioes.stream().flatMap(r -> Stream.concat(Stream.of(r.padrao), r.trocas.stream().map(Troca::para)));
        return Stream.concat(base.possibleBiomes().stream(), extras).distinct();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        Holder<Biome> b = base.getNoiseBiome(quartX, quartY, quartZ, sampler);
        if (intocados.isPresent() && intocados.get().contains(b)) return b;
        int bx = QuartPos.toBlock(quartX) + 2, bz = QuartPos.toBlock(quartZ) + 2;
        for (Regiao r : regioes) {
            if (!r.contem(bx, bz)) continue;
            for (Troca t : r.trocas) if (t.de.contains(b)) return t.para;
            return r.manter.contains(b) ? b : r.padrao;
        }
        return b;
    }

    @Override
    public void addDebugInfo(List<String> result, net.minecraft.core.BlockPos feetPos, Climate.Sampler sampler) {
        base.addDebugInfo(result, feetPos, sampler);
        for (Regiao r : regioes)
            if (r.contem(feetPos.getX(), feetPos.getZ())) result.add("Mundo DC: região de " + r.local);
    }
}
