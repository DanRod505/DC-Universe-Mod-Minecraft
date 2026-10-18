package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.core.EstadoCliente;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import com.danrod.dcuniverse.suit.TrajeItem;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.client.event.RenderLivingEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Troca a skin de quem está transformado: textures/skin/<personagem>.png (128x128) e, se existir,
 * textures/skin/<personagem>_capa.png, desenhada pela camada de capa do próprio jogo.
 * Vale para todos os jogadores visíveis (o servidor manda o personagem de cada um).
 */
public final class Skins {
    private static final Map<String, PlayerSkin> CACHE = new HashMap<>();
    /** renderRightHand/renderLeftHand disparam o RenderArmEvent de novo; sem isto a chamada se repete até estourar a pilha. */
    private static boolean desenhandoBraco;

    private Skins() {}

    public static void registrar() {
        RenderLivingEvent.Pre.BUS.addListener((Predicate<RenderLivingEvent.Pre>) e -> {
            if (e.getState() instanceof AvatarRenderState s) {
                PlayerSkin skin = skinDe(s.id);
                if (skin != null) {
                    s.skin = skin;
                    s.showCape = skin.cape() != null;
                    // As peças do traje travado não aparecem: a skin já mostra o traje.
                    var level = Minecraft.getInstance().level;
                    if (level != null && level.getEntity(s.id) instanceof net.minecraft.world.entity.LivingEntity vivo
                        && vivo.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).getItem() instanceof TrajeItem) {
                        s.headEquipment = ItemStack.EMPTY;
                        s.headItem.clear();
                    }
                    if (s.chestEquipment.getItem() instanceof TrajeItem) s.chestEquipment = ItemStack.EMPTY;
                    if (s.legsEquipment.getItem() instanceof TrajeItem) s.legsEquipment = ItemStack.EMPTY;
                    if (s.feetEquipment.getItem() instanceof TrajeItem) s.feetEquipment = ItemStack.EMPTY;
                }
            }
            return false;
        });
        // Braço em primeira pessoa com a skin do personagem.
        RenderArmEvent.BUS.addListener((Predicate<RenderArmEvent>) e -> {
            if (desenhandoBraco) return false;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return false;
            PlayerSkin skin = skinDe(mc.player.getId());
            if (skin == null) return false;
            var renderer = mc.getEntityRenderDispatcher().getPlayerRenderer(mc.player);
            Identifier tex = skin.body().texturePath();
            desenhandoBraco = true;
            try {
                if (e.getArm() == HumanoidArm.RIGHT) {
                    renderer.renderRightHand(e.getPoseStack(), e.getNodeCollector(), e.getPackedLight(), tex,
                        mc.player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE));
                } else {
                    renderer.renderLeftHand(e.getPoseStack(), e.getNodeCollector(), e.getPackedLight(), tex,
                        mc.player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE));
                }
            } finally {
                desenhandoBraco = false;
            }
            return true;
        });
    }

    public static void limpar() {
        CACHE.clear();
    }

    public static PlayerSkin skinDe(int entidade) {
        var a = EstadoCliente.APARENCIAS.get(entidade);
        return a == null ? null : skinDoPersonagem(a.personagem());
    }

    /** Skin (e capa, se houver) do personagem, ou null se ele ainda não tem skin. */
    public static PlayerSkin skinDoPersonagem(String personagem) {
        return CACHE.computeIfAbsent(personagem, Skins::criar);
    }

    private static PlayerSkin criar(String personagem) {
        Identifier id = Identifier.tryParse(personagem);
        if (id == null) return null;
        Identifier corpo = id.withPath("skin/" + id.getPath());
        Identifier capa = id.withPath("skin/" + id.getPath() + "_capa");
        var recursos = Minecraft.getInstance().getResourceManager();
        if (recursos.getResource(corpo.withPath(p -> "textures/" + p + ".png")).isEmpty()) return null;
        // Com a peça capa (client/Pecas) a capa comum do jogo não aparece.
        boolean temCapa = recursos.getResource(capa.withPath(p -> "textures/" + p + ".png")).isPresent()
            && !Pecas.temCapa(personagem);
        return PlayerSkin.insecure(new ClientAsset.ResourceTexture(corpo), temCapa ? new ClientAsset.ResourceTexture(capa) : null,
            null, PlayerModelType.WIDE);
    }
}
