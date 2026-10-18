package com.danrod.dcuniverse.content;

import com.danrod.dcuniverse.ability.passive.Planar;
import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.guia.Guia;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Equipamentos do lote que qualquer jogador sem transformação veste (transformado, os slots ficam com o traje):
 * - Capa Planadora (peito): o mesmo planar do Batman (ability/passive/Planar e client/Planador), a
 *   {@link #CAPA_VELOCIDADE} blocos/s de cruzeiro; o impulso da capa cansa (gasta fome) em vez de energia;
 * - Visor Tático (cabeça): visão noturna e mobs hostis a até {@link #VISOR_ALCANCE} blocos destacados com contorno
 *   (client/Visor); gasta 1 de durabilidade por segundo de uso e conserta com Placa de Circuito na bigorna.
 */
public final class Equipamentos {
    public static final float CAPA_VELOCIDADE = 9;
    /** Fome gasta por impulso da capa (sem energia de herói) e a fome mínima para dar o impulso. */
    public static final float CAPA_IMPULSO_FOME = 1.5f;
    public static final int CAPA_IMPULSO_MIN_FOME = 6;
    /** Segundos de uso do visor até quebrar (1 de durabilidade por segundo). */
    public static final int VISOR_DURACAO = 1800;
    public static final int VISOR_ALCANCE = 24;
    private static final int VISAO_TICKS = 400;
    private static final Planar PLANAR = new Planar();
    /** Quem estava de capa no último tick: vestir ou tirar manda o estado novo ao cliente (o planar liga e desliga). */
    private static final Set<UUID> COM_CAPA = ConcurrentHashMap.newKeySet();

    private Equipamentos() {}

    public static boolean vestindoCapa(Player p) {
        return p.getItemBySlot(EquipmentSlot.CHEST).is(Itens.CAPA_PLANADOR.get());
    }

    public static boolean vestindoVisor(Player p) {
        return p.getItemBySlot(EquipmentSlot.HEAD).is(Itens.VISOR_TATICO.get());
    }

    /** Tick de quem não está transformado (core/Eventos). */
    public static void tick(ServerPlayer player) {
        EstadoJogador e = EstadoJogador.de(player);
        boolean capa = vestindoCapa(player);
        if (capa != COM_CAPA.contains(player.getUUID())) {
            if (capa) {
                COM_CAPA.add(player.getUUID());
                Guia.concluir(player, "capa_planador");
            } else {
                COM_CAPA.remove(player.getUUID());
            }
            Rede.sincronizar(player);
        }
        if (capa) {
            PLANAR.tick(player, Params.EMPTY);
        } else if (e.planar() != Planar.FECHADA) {
            e.setPlanar(Planar.FECHADA);
            Rede.aparencia(player);
        }
        var visao = player.getEffect(MobEffects.NIGHT_VISION);
        if (vestindoVisor(player)) {
            if (visao == null || visao.getDuration() < 220) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, VISAO_TICKS, 0, true, false, false));
            }
            if (player.tickCount % 20 == 0) {
                ItemStack visor = player.getItemBySlot(EquipmentSlot.HEAD);
                if (!player.getAbilities().instabuild) visor.hurtAndBreak(1, player, EquipmentSlot.HEAD);
                if (player.tickCount % 200 == 0) Guia.concluir(player, "visor_tatico");
            }
        } else if (visao != null && visao.isAmbient() && visao.getDuration() <= VISAO_TICKS) {
            // A visão noturna do visor acaba junto com ele (a de poção não é "ambiente" e fica).
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }
}
