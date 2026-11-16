package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.client.guia.GuiaScreen;
import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.List;

/**
 * Escolha da habilidade inicial (Plano 2, docs/plano/HABILIDADES.md): na primeira vez como um herói da árvore nova,
 * o jogador escolhe 1 entre as iniciais dele. Ela fica no slot 1; as outras se compram depois na árvore (tecla K).
 * À esquerda, o herói em 3D; à direita, as iniciais em cartões com ícone, nome, recarga e descrição.
 */
public final class EscolhaInicialScreen extends Screen {
    /** Herói para quem a tela já abriu sozinha (abre uma vez por herói; depois fica na dica do HUD e na tecla K). */
    private static String abertaPara = "";

    private int foco = -1, escolhido = -1;
    private float idade;
    // Cartões: x, largura, y do primeiro, altura e espaço entre eles.
    private int cx, cw, cy0, ch, gap, px0, px1;

    public EscolhaInicialScreen() {
        super(Component.translatable("dcuniverse.arvore.escolha.titulo"));
    }

    /** Tick do cliente: abre a escolha sozinha quando o herói atual ainda não tem a inicial. */
    static void tick(Minecraft mc, boolean livre) {
        Rede.ArvoreSync a = EstadoCliente.arvore;
        if (!EstadoCliente.temArvore() || !a.precisaEscolher()) {
            if (!EstadoCliente.temArvore()) abertaPara = "";
            return;
        }
        if (livre && !a.heroi().equals(abertaPara)) {
            abertaPara = a.heroi();
            mc.gui.setScreen(new EscolhaInicialScreen());
        }
    }

    private static List<Rede.ArvoreItem> iniciais() {
        return EstadoCliente.arvore.itens().stream().filter(Rede.ArvoreItem::inicial).toList();
    }

    @Override
    protected void init() {
        int margem = 10;
        int n = Math.max(1, iniciais().size());
        px0 = margem;
        px1 = Math.max(px0 + 70, Math.min(width / 3, 170));
        cx = px1 + 10;
        cw = Math.min(width - margem - cx, 300);
        cx = Math.max(px1 + 10, (px1 + width - margem - cw) / 2);
        gap = 6;
        int topo = 44, baixo = height - margem - 16;
        ch = Math.max(40, Math.min(76, (baixo - topo - gap * (n - 1)) / n));
        cy0 = topo + Math.max(0, (baixo - topo - (ch * n + gap * (n - 1))) / 2);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        idade++;
        if (!EstadoCliente.temArvore()) {
            onClose();
        } else if (!EstadoCliente.arvore.precisaEscolher()) {
            // O servidor confirmou a escolha: segue direto para a árvore, onde a inicial aparece no slot 1.
            minecraft.gui.setScreen(new ArvoreScreen());
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float a) {
        g.fillGradient(0, 0, width, height, 0xF4060912, 0xFA101626);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        super.extractRenderState(g, mx, my, pt);
        int cor = Hud.corPersonagem();
        float t = idade + pt;
        Rede.ArvoreSync a = EstadoCliente.arvore;
        // Título e subtítulo.
        Component nomeHeroi = Component.translatable(chaveHeroi(a.heroi()));
        Component titulo = Component.translatable("dcuniverse.arvore.escolha.titulo").withStyle(ChatFormatting.BOLD);
        g.centeredText(font, titulo, width / 2, 12, ArvoreScreen.OURO);
        Component sub = Component.translatable("dcuniverse.arvore.escolha.sub", nomeHeroi, Teclas.ARVORE.getTranslatedKeyMessage());
        centralizadoPequeno(g, sub, width / 2, 25, width - 20, ArvoreScreen.SUAVE);
        g.fill(width / 2 - 60, 41, width / 2 + 60, 42, 0x60E8C468);
        // O herói em 3D, girando devagar.
        PlayerSkin skin = Skins.skinDoPersonagem(a.heroi());
        int alto = height - 44 - 26;
        if (skin != null && alto > 60) {
            g.fillGradient(px0, 44, px1, 44 + alto, 0x00000000, ArvoreScreen.comAlfa(cor, 0x30));
            float giro = 20f + Mth.sin(t * 0.02f) * 25f;
            GuiaScreen.previa(minecraft, g, skin, px0, 44, px1, 44 + alto, giro, 0f, Math.min(alto / 2.4f, (px1 - px0) / 1.1f));
            g.centeredText(font, nomeHeroi.copy().withStyle(ChatFormatting.BOLD), (px0 + px1) / 2, 44 + alto + 4, ArvoreScreen.TEXTO);
        }
        // Os cartões.
        List<Rede.ArvoreItem> ini = iniciais();
        foco = -1;
        for (int i = 0; i < ini.size(); i++) {
            int y = cy0 + i * (ch + gap);
            boolean sobre = mx >= cx && mx < cx + cw && my >= y && my < y + ch;
            if (sobre) foco = i;
            cartao(g, ini.get(i), cx, y, cw, ch, sobre, i == escolhido, cor, t);
        }
        centralizadoPequeno(g, Component.translatable("dcuniverse.arvore.escolha.rodape"), cx + cw / 2, height - 20, cw, ArvoreScreen.SUAVE);
    }

    private void cartao(GuiGraphicsExtractor g, Rede.ArvoreItem item, int x, int y, int w, int h, boolean sobre, boolean marcado, int cor, float t) {
        int fundo = sobre ? 0xE8202A40 : 0xD8141A28;
        g.fill(x, y, x + w, y + h, fundo);
        // Faixa na cor do herói à esquerda, mais forte no foco.
        g.fill(x, y, x + 3, y + h, ArvoreScreen.comAlfa(cor, sobre ? 0xFF : 0x90));
        int borda = sobre ? ArvoreScreen.pulso(ArvoreScreen.OURO, t) : 0xFF3A4458;
        ArvoreScreen.moldura(g, x, y, w, h, marcado ? 0xFFFFFFFF : borda);
        int tam = Math.min(32, h - 12);
        int iy = y + (h - tam) / 2;
        ArvoreScreen.icone(g, item.id(), x + 10, iy, tam, sobre ? 0xFFFFFFFF : 0xFFE0E0E0);
        if (sobre) ArvoreScreen.brilhoIcone(g, x + 10, iy, tam, cor, t);
        int tx = x + 10 + tam + 10, tw = w - (tx - x) - 8;
        g.text(font, Component.translatable("habilidade.dcuniverse." + item.id()).withStyle(ChatFormatting.BOLD), tx, y + 6, sobre ? ArvoreScreen.OURO : ArvoreScreen.TEXTO, true);
        Component info = ArvoreScreen.resumo(item);
        ArvoreScreen.pequeno(g, font, info, tx, y + 17, ArvoreScreen.SUAVE);
        List<FormattedCharSequence> linhas = font.split(Component.translatable("habilidade.dcuniverse." + item.id() + ".desc"), Math.round(tw / 0.75f));
        int maxLinhas = Math.max(0, (h - 30) / 8);
        for (int i = 0; i < Math.min(maxLinhas, linhas.size()); i++) {
            g.pose().pushMatrix();
            g.pose().translate(tx, y + 27 + i * 8);
            g.pose().scale(0.75f, 0.75f);
            boolean ultima = i == maxLinhas - 1 && linhas.size() > maxLinhas;
            g.text(font, linhas.get(i), 0, 0, ultima ? 0xA0C8CCD8 : 0xFFC8CCD8, false);
            g.pose().popMatrix();
        }
        if (sobre) {
            Component escolher = Component.translatable("dcuniverse.arvore.escolha.botao").withStyle(ChatFormatting.BOLD);
            int bw = font.width(escolher) + 10;
            g.fill(x + w - bw - 4, y + 4, x + w - 4, y + 15, ArvoreScreen.comAlfa(cor, 0xE0));
            g.text(font, escolher, x + w - bw + 1, y + 6, 0xFFFFFFFF, true);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean duplo) {
        List<Rede.ArvoreItem> ini = iniciais();
        if (e.button() == 0 && foco >= 0 && foco < ini.size() && escolhido < 0) {
            escolhido = foco;
            Rede.CANAL.send(new Rede.ArvoreInicial(ini.get(foco).id()), com.danrod.dcuniverse.core.Canal.SERVIDOR);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.2f, 0.6f));
            return true;
        }
        return super.mouseClicked(e, duplo);
    }

    /** Texto centralizado; se não couber na largura, encolhe para 75% e quebra em até 2 linhas. */
    private void centralizadoPequeno(GuiGraphicsExtractor g, Component texto, int x, int y, int largura, int cor) {
        if (font.width(texto) <= largura) {
            g.centeredText(font, texto, x, y, cor);
            return;
        }
        List<FormattedCharSequence> linhas = font.split(texto, Math.round(largura / 0.75f));
        for (int i = 0; i < Math.min(2, linhas.size()); i++) {
            g.pose().pushMatrix();
            g.pose().translate(x, y + i * 8);
            g.pose().scale(0.75f, 0.75f);
            g.text(font, linhas.get(i), -font.width(linhas.get(i)) / 2, 0, cor, false);
            g.pose().popMatrix();
        }
    }

    static String chaveHeroi(String heroi) {
        var id = net.minecraft.resources.Identifier.tryParse(heroi);
        return id == null ? "" : "personagem." + id.getNamespace() + "." + id.getPath();
    }
}
