package com.danrod.dcuniverse.content;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.character.Transformacao;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Item de transformação: usar transforma no personagem; usar de novo destransforma (seção 2.4). */
public final class TransformacaoItem extends Item {
    /** Ticks até o item poder ser usado de novo. */
    public static final int RECARGA = 30;

    private final Identifier personagem;

    public TransformacaoItem(String personagem, Properties properties) {
        super(properties.stacksTo(1));
        this.personagem = Identifier.fromNamespaceAndPath(DCUniverse.MODID, personagem);
    }

    public Identifier personagem() {
        return personagem;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) Transformacao.usarItem(sp, personagem);
        // Segurar o botão repete o uso a cada 4 ticks: um clique um pouco mais longo transformava e já pedia para
        // destransformar. A recarga curta do item segura essa repetição.
        player.getCooldowns().addCooldown(player.getItemInHand(hand), RECARGA);
        return InteractionResult.SUCCESS;
    }
}
