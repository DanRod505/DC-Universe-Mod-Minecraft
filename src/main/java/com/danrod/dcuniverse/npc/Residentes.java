package com.danrod.dcuniverse.npc;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.content.Entidades;
import com.danrod.dcuniverse.world.estrutura.Estruturas;
import com.danrod.dcuniverse.world.estrutura.Local;
import com.danrod.dcuniverse.world.estrutura.LocalEstrutura;
import com.danrod.dcuniverse.world.estrutura.PecaObra;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Quem mora em cada local gerado (Martha, Lana, cidadãos, Krypto e Streaky ainda sem dono): aparecem quando um
 * jogador chega perto da casa deles. Cada morador é lembrado pelo UUID; se some (morreu ou foi domado), outro
 * aparece no dia seguinte. Fica salvo no mundo (data/dcuniverse_residentes.dat).
 */
public final class Residentes extends SavedData {
    public static final SavedDataType<Residentes> TIPO = new SavedDataType<>(Identifier.fromNamespaceAndPath(DCUniverse.MODID, "residentes"),
        Residentes::new, CompoundTag.CODEC.xmap(Residentes::ler, Residentes::escrever), null);
    /** O morador só é conferido (e recriado) com um jogador a até essa distância da casa. */
    private static final double PERTO = 28;

    /** Um morador: quem é, onde mora, a entidade atual e o dia a partir do qual pode voltar. */
    record Morador(String quem, BlockPos casa, UUID uuid, long volta) {}

    private final Map<String, Morador> moradores = new LinkedHashMap<>();
    private final List<String> povoados = new ArrayList<>();
    /** Mascotes com dono que morreram com o dono longe (npc/Mascotes). */
    private final java.util.Set<UUID> mortos = new java.util.HashSet<>();

    public boolean morto(UUID id) {
        return mortos.contains(id);
    }

    public void marcarMorto(UUID id) {
        if (mortos.add(id)) setDirty();
    }

    public static Residentes de(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TIPO);
    }

    private static Residentes ler(CompoundTag t) {
        Residentes r = new Residentes();
        CompoundTag m = t.getCompoundOrEmpty("moradores");
        for (String k : m.keySet()) {
            CompoundTag c = m.getCompoundOrEmpty(k);
            String u = c.getStringOr("uuid", "");
            r.moradores.put(k, new Morador(c.getStringOr("quem", ""), new BlockPos(c.getIntOr("x", 0), c.getIntOr("y", 0), c.getIntOr("z", 0)),
                u.isEmpty() ? null : UUID.fromString(u), c.getLongOr("volta", 0)));
        }
        for (String k : t.getCompoundOrEmpty("povoados").keySet()) r.povoados.add(k);
        for (String k : t.getCompoundOrEmpty("mortos").keySet()) r.mortos.add(UUID.fromString(k));
        return r;
    }

    private static CompoundTag escrever(Residentes r) {
        CompoundTag t = new CompoundTag();
        CompoundTag m = new CompoundTag();
        r.moradores.forEach((k, v) -> {
            CompoundTag c = new CompoundTag();
            c.putString("quem", v.quem());
            c.putInt("x", v.casa().getX());
            c.putInt("y", v.casa().getY());
            c.putInt("z", v.casa().getZ());
            if (v.uuid() != null) c.putString("uuid", v.uuid().toString());
            c.putLong("volta", v.volta());
            m.put(k, c);
        });
        t.put("moradores", m);
        CompoundTag p = new CompoundTag();
        for (String k : r.povoados) p.putBoolean(k, true);
        t.put("povoados", p);
        CompoundTag mo = new CompoundTag();
        for (UUID u : r.mortos) mo.putBoolean(u.toString(), true);
        t.put("mortos", mo);
        return t;
    }

    // ------------------------------------------------------------ ticker

    /** A cada 5 segundos para cada jogador: registra os moradores do local onde ele está e recria quem sumiu. */
    public static void tick(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        var gerente = level.structureManager();
        for (Structure s : gerente.getAllStructuresAt(player.blockPosition()).keySet()) {
            if (!(s instanceof LocalEstrutura le)) continue;
            StructureStart st = gerente.getStructureAt(player.blockPosition(), s);
            if (!st.isValid()) continue;
            Local local = Estruturas.LOCAIS.get(le.local());
            if (local == null) continue;
            Residentes r = de(level);
            String chave = le.local() + "@" + st.getChunkPos().x() + "," + st.getChunkPos().z();
            if (!r.povoados.contains(chave)) r.povoar(chave, local, st);
            r.conferir(level, player, chave);
        }
    }

    private void povoar(String chave, Local local, StructureStart st) {
        for (Local.Morador m : local.moradores()) {
            String[] quem = m.quem().split(",");
            int n = 0;
            for (StructurePiece pc : st.getPieces()) {
                if (!(pc instanceof PecaObra po) || !po.planta().equals(m.planta())) continue;
                moradores.put(chave + "/" + m.papel() + (m.todas() ? "_" + n : ""), new Morador(quem[n % quem.length].trim(), po.mundo(m.x(), m.y(), m.z()), null, 0));
                n++;
                if (!m.todas()) break;
            }
        }
        povoados.add(chave);
        setDirty();
    }

    private void conferir(ServerLevel level, ServerPlayer player, String chave) {
        long dia = level.getOverworldClockTime() / 24000L;
        for (var e : new ArrayList<>(moradores.entrySet())) {
            if (!e.getKey().startsWith(chave + "/")) continue;
            Morador m = e.getValue();
            if (player.blockPosition().distSqr(m.casa()) > PERTO * PERTO || !level.isPositionEntityTicking(m.casa())) continue;
            if (m.uuid() != null && level.getEntity(m.uuid()) != null) continue;
            if (dia < m.volta()) continue;
            Entity novo = criar(level, m, e.getKey());
            if (novo != null) {
                moradores.put(e.getKey(), new Morador(m.quem(), m.casa(), novo.getUUID(), 0));
                setDirty();
            }
        }
    }

    private static Entity criar(ServerLevel level, Morador m, String chave) {
        BlockPos pos = livre(level, m.casa());
        Mob mob;
        if (m.quem().startsWith("entity:")) {
            EntityType<?> tipo = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(m.quem().substring(7)));
            if (!(tipo.create(level, EntitySpawnReason.STRUCTURE) instanceof Mob x)) return null;
            mob = x;
            mob.setPersistenceRequired();
            mob.setHomeTo(m.casa(), 10);
        } else {
            DefNpc d = Npcs.npc(Identifier.parse(m.quem()));
            if (d == null) return null;
            NpcEntidade npc = (d.slim() ? Entidades.NPC_SLIM : Entidades.NPC).get().create(level, EntitySpawnReason.STRUCTURE);
            if (npc == null) return null;
            npc.configurar(d, m.casa(), chave);
            mob = npc;
        }
        mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0f);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
        if (mob instanceof Residente r) r.morador(chave);
        level.addFreshEntity(mob);
        return mob;
    }

    /** Lugar livre para alguém em pé perto de {@code alvo}: chão firme e dois blocos de ar. */
    public static BlockPos livre(ServerLevel level, BlockPos alvo) {
        for (int r = 0; r <= 4; r++) {
            for (int dy : new int[]{0, 1, -1, 2, -2, 3, -3, 4}) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                        BlockPos p = alvo.offset(dx, dy, dz);
                        if (level.getBlockState(p.below()).isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP)
                            && level.getBlockState(p).getCollisionShape(level, p).isEmpty()
                            && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()
                            && level.getFluidState(p).isEmpty()) return p;
                    }
                }
            }
        }
        return alvo;
    }

    // ------------------------------------------------------------ eventos

    /** O morador morreu ou ganhou dono: outro aparece no dia seguinte. */
    public static void saiu(ServerLevel level, String chave) {
        if (chave == null || chave.isEmpty()) return;
        Residentes r = de(level);
        Morador m = r.moradores.get(chave);
        if (m == null) return;
        r.moradores.put(chave, new Morador(m.quem(), m.casa(), null, level.getOverworldClockTime() / 24000L + 1));
        r.setDirty();
    }

    /** Casa do morador mais perto com esse id (para a seta do Diário), ou null. */
    public static BlockPos casa(ServerLevel level, String quem, BlockPos perto) {
        BlockPos melhor = null;
        for (Morador m : de(level).moradores.values()) {
            if (!m.quem().equals(quem) && !m.quem().equals("entity:" + quem)) continue;
            if (melhor == null || m.casa().distSqr(perto) < melhor.distSqr(perto)) melhor = m.casa();
        }
        return melhor;
    }

    /** Entidade que mora num local (NPC ou mascote sem dono) e sabe a própria chave em Residentes. */
    public interface Residente {
        String morador();

        void morador(String chave);
    }
}
