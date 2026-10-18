package com.danrod.dcuniverse.client.npc;

import com.danrod.dcuniverse.missao.DefMissao;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;


/** Aviso de missão aceita ou concluída no canto da tela, no estilo dos avisos de conquista. */
public final class MissaoToast implements Toast {
    private static final Identifier FUNDO = Identifier.withDefaultNamespace("toast/advancement");
    private static final long DURACAO = 5000;
    private final boolean concluida;
    private final Component nome;
    private Toast.Visibility vis = Toast.Visibility.SHOW;

    public MissaoToast(String tipo, String missao) {
        concluida = tipo.equals("concluida");
        DefMissao m = NpcsCliente.missao(Identifier.tryParse(missao));
        nome = m == null ? Component.literal(missao) : TextoMissao.nome(m);
    }

    @Override
    public Toast.Visibility getWantedVisibility() {
        return vis;
    }

    @Override
    public void update(ToastManager manager, long visivelMs) {
        vis = visivelMs >= DURACAO * manager.getNotificationDisplayTimeMultiplier() ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, Font font, long visivelMs) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, FUNDO, 0, 0, width(), height());
        g.item(new ItemStack(concluida ? Items.EMERALD : Items.WRITABLE_BOOK), 8, 8);
        g.text(font, Component.translatable(concluida ? "dcuniverse.missao.toast.concluida" : "dcuniverse.missao.toast.nova"), 30, 7,
            concluida ? 0xFF7CE07C : 0xFFFFD24A, false);
        String n = nome.getString();
        if (font.width(n) > 125) n = font.plainSubstrByWidth(n, 125 - font.width("...")).stripTrailing() + "...";
        g.text(font, n, 30, 18, 0xFFFFFFFF, false);
    }
}
