package com.danrod.dcuniverse.client.guia;

import com.danrod.dcuniverse.content.Itens;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Aviso no canto quando uma tarefa de Primeiros Passos ganha o visto, no estilo dos avisos de conquista. */
public final class GuiaToast implements Toast {
    private static final Identifier FUNDO = Identifier.withDefaultNamespace("toast/advancement");
    private static final long DURACAO = 5000;
    private final String tarefa;
    private Toast.Visibility visibilidade = Toast.Visibility.SHOW;

    public GuiaToast(String tarefa) {
        this.tarefa = tarefa;
    }

    @Override
    public Toast.Visibility getWantedVisibility() {
        return visibilidade;
    }

    @Override
    public void update(ToastManager manager, long visivelMs) {
        visibilidade = visivelMs >= DURACAO * manager.getNotificationDisplayTimeMultiplier() ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, Font font, long visivelMs) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, FUNDO, 0, 0, width(), height());
        g.item(new ItemStack(Itens.GUIA_HEROI.get()), 8, 8);
        g.text(font, Component.translatable("dcuniverse.guia.toast"), 30, 7, 0xFFFFD24A, false);
        List<FormattedCharSequence> linhas = font.split(Component.translatable("dcuniverse.guia.tarefa." + tarefa), 125);
        if (!linhas.isEmpty()) g.text(font, linhas.get(0), 30, 18, 0xFFFFFFFF, false);
    }
}
