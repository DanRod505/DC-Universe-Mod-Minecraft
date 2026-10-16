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
        return InteractionResult.SUCCESS;
    }
}
