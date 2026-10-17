package com.danrod.dcuniverse.ability.passive;

import com.danrod.dcuniverse.ability.Passiva;
import com.danrod.dcuniverse.ability.Passivas;
import com.danrod.dcuniverse.character.Params;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;

/**
 * stealth (Furtividade): agachado num lugar escuro (luz até max_light), os mobs só percebem o jogador bem mais
 * perto (visibilidade x visibility). Vale para a mira de todos os mobs que caçam pela distância. No cliente, a tela
 * escurece nas bordas enquanto está escondido (client/Furtivo).
 */
public final class Furtividade implements Passiva {
    public static void registrar() {
        LivingEvent.LivingVisibilityEvent.BUS.addListener(e -> {
            if (!(e.getEntity() instanceof Player player)) return;
            Params prm = Passivas.params(player, "stealth");
            if (prm != null && escondido(player, prm)) e.modifyVisibility(prm.f("visibility", 0.3f));
        });
    }

    /** Guia: ficar escondido a poucos blocos de um monstro que não percebeu você. */
    @Override
    public void tick(net.minecraft.server.level.ServerPlayer player, Params p) {
        if (player.tickCount % 20 != 0 || !escondido(player, p)) return;
        boolean perto = !player.level().getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, player.getBoundingBox().inflate(8),
            m -> m.getTarget() != player).isEmpty();
        if (perto) com.danrod.dcuniverse.guia.Guia.concluir(player, "furtivo");
    }

    /** Agachado e no escuro? Funciona dos dois lados (o cliente usa para o efeito na tela). */
    public static boolean escondido(LivingEntity quem, Params prm) {
        if (!quem.isShiftKeyDown() || quem.isSpectator()) return false;
        return quem.level().getMaxLocalRawBrightness(quem.blockPosition()) <= prm.i("max_light", 7);
    }
}
