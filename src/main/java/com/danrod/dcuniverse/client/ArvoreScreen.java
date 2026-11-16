package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.ability.Arvore;
import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tela da árvore de habilidades e da build (tecla K, Plano 2: docs/plano/HABILIDADES.md).
 *
 * <p>Em cima: o herói, o nível, a experiência e os pontos livres. À esquerda, a árvore de verdade: o emblema do herói
 * no tronco, um galho para cada habilidade e, pendurados nela, os galhos das melhorias (uma melhoria que exige outra
 * fica embaixo dela). Comprado tem moldura e galho dourados, o que dá para comprar pulsa, o resto fica apagado com o
 * custo. Se a árvore não cabe, arrasta-se o fundo ou usa-se a rodinha para os lados. À direita, o nó em foco (sob o
 * mouse, ou o último clicado): números, descrição e o botão de comprar. Embaixo, os 6 slots com as teclas Z X C V B R (os trancados mostram o nível
 * em que abrem). Para montar a build: arrastar uma habilidade comprada até um slot, ou clicar nela e depois no slot.
 * Arrastar de um slot para outro troca os dois; botão direito num slot esvazia.
 *
 * <p>O servidor confere tudo (ability/Arvore); a tela só pede e mostra o que voltou.
 */
public final class ArvoreScreen extends Screen {
    static final int OURO = 0xFFE8C468, TEXTO = 0xFFEDE6D6, SUAVE = 0xFF9C9584, VERDE = 0xFF7CE08A, VERMELHO = 0xFFFF7A6A;
    private static final int FUNDO = 0xE8101521, FUNDO2 = 0xF0171D2C, LINHA = 0x40E8C468;

    private String foco = "";            // nó da árvore sob o mouse
    private String escolhido = "";       // habilidade em foco no painel (clicada)
    private String pegando = "";         // habilidade sendo arrastada ("" = nenhuma)
    private int pegouDoSlot = -1;        // slot de onde saiu o arrasto (-1 = da grade)
    private double pegouX, pegouY;
    private boolean arrastou;
    private float idade;

    // Layout.
    private int x0, y0, x1, y1, gx, gy, gw, gh, dx, dw, sy, sx0, slot;
    private int bx, by, bw, bh;          // botão comprar
    private int ux, uy, uw, uh;          // botão desfazer (criativo)

    /** Um nó da árvore já posicionado: coluna (pode ser fracionária, centrada sobre os filhos) e linha (0 = tronco). */
    private record No(Rede.ArvoreItem it, float col, int linha, String paiNo) {}

    private List<No> nos = List.of();
    private List<Rede.ArvoreItem> nosDe;
    private float colunas = 1;
    private int linhas = 1;
    private int colW, rowH, tam, tamM;
    private double pan;
    private boolean centrou;
    private boolean panando;

    public ArvoreScreen() {
        super(Component.translatable("dcuniverse.arvore.titulo"));
    }

    @Override
    protected void init() {
        x0 = 8;
        y0 = 8;
        x1 = width - 8;
        y1 = height - 8;
        int corpoY = y0 + 30;
        slot = 26;
        sy = y1 - slot - 16;
        int largura = x1 - x0;
        dw = Math.max(120, Math.min(170, largura * 3 / 10));
        dx = x1 - dw - 6;
        gx = x0 + 8;
        gy = corpoY + 14;
        gw = dx - gx - 10;
        gh = sy - 18 - gy;
        nosDe = null;
        sx0 = (x0 + x1) / 2 - (Arvore.SLOTS * (slot + 6) - 6) / 2;
        if (escolhido.isEmpty() && !EstadoCliente.arvore.itens().isEmpty()) {
            List<String> s = EstadoCliente.arvore.slots();
            escolhido = !s.isEmpty() && !s.get(0).isEmpty() ? s.get(0) : EstadoCliente.arvore.itens().get(0).id();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        idade++;
        if (!EstadoCliente.temArvore()) onClose();
        else if (EstadoCliente.arvore.precisaEscolher()) minecraft.gui.setScreen(new EscolhaInicialScreen());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float a) {
        g.fillGradient(0, 0, width, height, 0xF0060912, 0xF8101626);
    }

    // ------------------------------------------------------------------ desenho

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        super.extractRenderState(g, mx, my, pt);
        Rede.ArvoreSync a = EstadoCliente.arvore;
        int cor = Hud.corPersonagem();
        float t = idade + pt;
        g.fill(x0, y0, x1, y1, FUNDO);
        moldura(g, x0, y0, x1 - x0, y1 - y0, 0xFF3A4458);
        g.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 27, FUNDO2);
        g.fill(x0 + 1, y0 + 27, x1 - 1, y0 + 28, LINHA);
        cabecalho(g, a, cor, t);
        arvore(g, a, mx, my, cor, t);
        painel(g, a, cor, t);
        slots(g, a, mx, my, cor, t);
        // A habilidade arrastada segue o mouse.
        if (!pegando.isEmpty() && arrastou) {
            g.fill(mx - 11, my - 11, mx + 11, my + 11, 0xC0101521);
            moldura(g, mx - 11, my - 11, 22, 22, OURO);
            icone(g, pegando, mx - 9, my - 9, 18, 0xFFFFFFFF);
        }
    }

    private void cabecalho(GuiGraphicsExtractor g, Rede.ArvoreSync a, int cor, float t) {
        Component heroi = Component.translatable(EscolhaInicialScreen.chaveHeroi(a.heroi()));
        String nome = heroi.getString();
        int p = nome.indexOf(" (");
        if (p > 0) nome = nome.substring(0, p);
        g.fill(x0 + 6, y0 + 6, x0 + 8, y0 + 22, comAlfa(cor, 0xFF));
        g.text(font, Component.literal(nome).withStyle(ChatFormatting.BOLD), x0 + 12, y0 + 6, TEXTO, true);
        pequeno(g, font, Component.translatable("dcuniverse.arvore.subtitulo"), x0 + 12, y0 + 17, SUAVE);
        // Nível e barra de experiência no meio.
        int bw = Math.min(140, (x1 - x0) / 3), bx = (x0 + x1) / 2 - bw / 2;
        Component nivel = Component.translatable("dcuniverse.hud.nivel", a.nivel()).withStyle(ChatFormatting.BOLD);
        g.centeredText(font, nivel, bx + bw / 2, y0 + 5, OURO);
        int faixa = Math.max(1, a.xpProximo() - a.xpNivel());
        float k = a.nivel() >= Arvore.NIVEL_MAX ? 1f : Mth.clamp((a.xp() - a.xpNivel()) / (float) faixa, 0f, 1f);
        g.fill(bx, y0 + 16, bx + bw, y0 + 21, 0xFF0B0F18);
        g.fillGradient(bx, y0 + 16, bx + Math.round(bw * k), y0 + 21, comAlfa(clarear(cor, 0.35f), 0xFF), comAlfa(cor, 0xFF));
        moldura(g, bx - 1, y0 + 15, bw + 2, 7, 0xFF5A4A2A);
        String xp = a.nivel() >= Arvore.NIVEL_MAX ? "MAX" : (a.xp() - a.xpNivel()) + " / " + faixa + " XP";
        pequeno(g, font, Component.literal(xp), bx + bw + 4, y0 + 16, SUAVE);
        // Pontos à direita.
        Component pts = a.criativo() ? Component.translatable("dcuniverse.arvore.criativo")
            : Component.translatable("dcuniverse.arvore.pontos", a.pontos());
        int pw = font.width(pts) + 12, px = x1 - 8 - pw;
        boolean tem = a.pontos() > 0;
        g.fill(px, y0 + 6, px + pw, y0 + 21, tem ? 0xE04A3A12 : 0xC0202838);
        moldura(g, px, y0 + 6, pw, 15, tem ? pulso(OURO, t) : 0xFF3A4458);
        g.text(font, pts, px + 6, y0 + 10, tem ? OURO : TEXTO, true);
    }

    // ------------------------------------------------------------------ a árvore

    /** Posiciona os nós: folhas lado a lado, cada pai centrado sobre os filhos, melhoria exigida em cima da seguinte. */
    private void montarNos(Rede.ArvoreSync a) {
        if (a.itens() == nosDe) return;
        nosDe = a.itens();
        List<No> out = new ArrayList<>();
        float[] prox = {0};
        int[] fundo = {1};
        List<Float> raizes = new ArrayList<>();
        for (Rede.ArvoreItem it : a.itens()) {
            if (it.melhoria()) continue;
            raizes.add(posicionar(a, it, 1, "", out, prox, fundo));
            prox[0] += 0.35f;
        }
        colunas = Math.max(1, prox[0] - 0.35f);
        linhas = fundo[0] + 1;
        nos = List.copyOf(out);
        // Tamanhos: o maior que couber, e rolagem para os lados se nem o menor couber.
        colW = Mth.clamp(Math.round(gw / colunas), 26, 44);
        rowH = Mth.clamp((gh - 4) / linhas, 26, 44);
        tam = Math.min(colW, rowH) - 6;
        tamM = Math.max(18, tam - 6);
        // Abre centrada no tronco (o emblema do herói), com as primeiras habilidades em volta.
        if (!centrou) {
            float meio = colRaiz();
            pan = meio * colW + colW / 2f - gw / 2f;
            centrou = true;
        }
        pan = Mth.clamp(pan, 0, maxPan());
    }

    private float posicionar(Rede.ArvoreSync a, Rede.ArvoreItem it, int linha, String pai, List<No> out, float[] prox, int[] fundo) {
        fundo[0] = Math.max(fundo[0], linha);
        List<Rede.ArvoreItem> filhos = new ArrayList<>();
        for (Rede.ArvoreItem f : a.itens()) {
            if (!f.melhoria()) continue;
            boolean dele = it.melhoria() ? f.requer().equals(it.id()) : f.pai().equals(it.id()) && f.requer().isEmpty();
            if (dele) filhos.add(f);
        }
        float col;
        if (filhos.isEmpty()) {
            col = prox[0]++;
        } else {
            float primeiro = 0, ultimo = 0;
            for (int i = 0; i < filhos.size(); i++) {
                float c = posicionar(a, filhos.get(i), linha + 1, it.id(), out, prox, fundo);
                if (i == 0) primeiro = c;
                ultimo = c;
            }
            col = (primeiro + ultimo) / 2;
        }
        out.add(new No(it, col, linha, pai));
        return col;
    }

    private int larguraConteudo() {
        return Math.round(colunas * colW);
    }

    private double maxPan() {
        return Math.max(0, larguraConteudo() - gw + 8);
    }

    private int noX(float col) {
        int w = larguraConteudo();
        int base = w <= gw ? gx + (gw - w) / 2 : gx + 4 - (int) Math.round(pan);
        return base + Math.round(col * colW + colW / 2f);
    }

    private int noY(int linha) {
        return gy + 2 + linha * rowH + rowH / 2;
    }

    private int tamanho(No n) {
        return n.linha() == 0 ? tam : n.it().melhoria() ? tamM : tam;
    }

    private void arvore(GuiGraphicsExtractor g, Rede.ArvoreSync a, int mx, int my, int cor, float t) {
        montarNos(a);
        g.text(font, Component.translatable("dcuniverse.arvore.habilidades").withStyle(ChatFormatting.BOLD), gx, gy - 12, SUAVE, false);
        g.fill(gx - 2, gy - 2, gx + gw + 2, gy + gh, 0x60080B12);
        moldura(g, gx - 2, gy - 2, gw + 4, gh + 2, 0xFF222838);
        foco = "";
        boolean dentroArea = dentro(mx, my, gx, gy, gw, gh);
        g.enableScissor(gx - 1, gy - 1, gx + gw + 1, gy + gh - 1);
        // O tronco: o emblema do herói, centrado sobre as habilidades.
        int rx = noX(colRaiz()), ry = noY(0);
        // Galhos primeiro (embaixo dos nós).
        for (No n : nos) {
            int px, py, pt;
            if (n.paiNo().isEmpty()) {
                px = rx;
                py = ry;
                pt = tam;
            } else {
                No pai = no(n.paiNo());
                if (pai == null) continue;
                px = noX(pai.col());
                py = noY(pai.linha());
                pt = tamanho(pai);
            }
            boolean tem = a.tem(n.it().id());
            boolean pode = a.podeComprar(n.it().id());
            int c = tem ? OURO : pode ? pulso(0xFFFFFFFF, t) : 0xFF3A4152;
            galho(g, px, py + pt / 2, noX(n.col()), noY(n.linha()) - tamanho(n) / 2, c, tem ? 2 : 1);
        }
        raiz(g, a, rx, ry, cor, t);
        for (No n : nos) {
            int x = noX(n.col()), y = noY(n.linha()), s = tamanho(n);
            boolean sobre = dentroArea && dentro(mx, my, x - s / 2, y - s / 2, s, s);
            if (sobre) foco = n.it().id();
            if (n.it().melhoria()) noMelhoria(g, a, n.it(), x, y, s, sobre, cor, t);
            else noHabilidade(g, a, n.it(), x, y, s, sobre, cor, t);
        }
        g.disableScissor();
        // Setas de rolagem quando a árvore não cabe.
        if (maxPan() > 0) {
            if (pan > 0) g.text(font, "<", gx + 1, gy + gh / 2 - 4, pulso(OURO, t), true);
            if (pan < maxPan()) g.text(font, ">", gx + gw - 6, gy + gh / 2 - 4, pulso(OURO, t), true);
        }
    }

    /** O tronco fica em cima das habilidades iniciais (o começo de todo herói); sem elas, no meio. */
    private float colRaiz() {
        float soma = 0;
        int n = 0;
        for (No x : nos) {
            if (x.linha() == 1 && x.it().inicial()) {
                soma += x.col();
                n++;
            }
        }
        if (n == 0) for (No x : nos) if (x.linha() == 1) { soma += x.col(); n++; }
        return n == 0 ? 0 : soma / n;
    }

    private No no(String id) {
        for (No n : nos) if (n.it().id().equals(id)) return n;
        return null;
    }

    /** Galho em cotovelo: desce do pai, corre na horizontal e desce até o filho. */
    private static void galho(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int cor, int esp) {
        int meio = (y0 + y1) / 2;
        int e0 = esp / 2, e1 = esp - e0;
        g.fill(x0 - e0, y0, x0 + e1, meio + e1, cor);
        g.fill(Math.min(x0, x1) - e0, meio - e0, Math.max(x0, x1) + e1, meio + e1, cor);
        g.fill(x1 - e0, meio - e0, x1 + e1, y1, cor);
    }

    private void raiz(GuiGraphicsExtractor g, Rede.ArvoreSync a, int x, int y, int cor, float t) {
        int s = tam + 2;
        int x0 = x - s / 2, y0 = y - s / 2;
        g.fill(x0 + 2, y0, x0 + s - 2, y0 + s, 0xF0141A28);
        g.fill(x0, y0 + 2, x0 + s, y0 + s - 2, 0xF0141A28);
        g.fill(x0 + 1, y0 + 1, x0 + s - 1, y0 + s - 1, 0xF0141A28);
        moldura(g, x0 + 1, y0 + 1, s - 2, s - 2, OURO);
        Identifier id = Identifier.tryParse(a.heroi());
        if (id != null) {
            Identifier emblema = id.withPath("textures/gui/emblema/" + id.getPath() + ".png");
            int e = s - 6;
            if (minecraft.getResourceManager().getResource(emblema).isPresent()) {
                g.blit(RenderPipelines.GUI_TEXTURED, emblema, x0 + 3, y0 + 3, 0, 0, e, e, 48, 48, 48, 48);
            }
        }
    }

    private void noHabilidade(GuiGraphicsExtractor g, Rede.ArvoreSync a, Rede.ArvoreItem it, int cx, int cy, int s, boolean sobre, int cor, float t) {
        int x = cx - s / 2, y = cy - s / 2;
        boolean tem = a.tem(it.id());
        boolean pode = a.podeComprar(it.id());
        int fundo = it.id().equals(escolhido) ? 0xF02A3450 : sobre ? 0xF0222C42 : 0xF0141A28;
        g.fill(x, y, x + s, y + s, fundo);
        if (tem) g.fillGradient(x, y + s / 2, x + s, y + s, 0x00000000, comAlfa(cor, 0x50));
        moldura(g, x, y, s, s, tem ? OURO : pode ? pulso(0xFFFFFFFF, t) : 0xFF2E3546);
        if (it.id().equals(escolhido)) moldura(g, x - 1, y - 1, s + 2, s + 2, 0x80FFFFFF);
        int ic = s - 6;
        icone(g, it.id(), x + 3, y + 3, ic, tem ? 0xFFFFFFFF : pode ? 0xFFB8B8B8 : 0xFF5A5A5A);
        if (sobre && tem) brilhoIcone(g, x + 3, y + 3, ic, cor, t);
        int s0 = a.slots().indexOf(it.id());
        if (tem && s0 >= 0 && s0 < a.slotsAbertos()) {
            // A tecla do slot num selo no canto.
            String tecla = teclaDoSlot(s0);
            int w = Math.round(font.width(tecla) * 0.75f) + 3;
            g.fill(x + s - w - 1, y + s - 8, x + s - 1, y + s - 1, 0xE0101521);
            pequeno(g, font, Component.literal(tecla), x + s - w + 1, y + s - 7, OURO);
        } else if (!tem) {
            custo(g, it.custo(), cx, y + s + 2, pode);
        }
    }

    private void noMelhoria(GuiGraphicsExtractor g, Rede.ArvoreSync a, Rede.ArvoreItem it, int cx, int cy, int s, boolean sobre, int cor, float t) {
        int x = cx - s / 2, y = cy - s / 2;
        boolean tem = a.tem(it.id());
        boolean pode = a.podeComprar(it.id());
        int borda = tem ? OURO : pode ? pulso(0xFFFFFFFF, t) : 0xFF2E3546;
        int fundo = it.id().equals(escolhido) ? 0xF02A3450 : sobre ? 0xF0222C42 : tem ? 0xF0262014 : 0xF0141A28;
        // Losango de cantos cortados: a melhoria tem outra forma que a habilidade.
        g.fill(x + 2, y, x + s - 2, y + s, borda);
        g.fill(x, y + 2, x + s, y + s - 2, borda);
        g.fill(x + 1, y + 1, x + s - 1, y + s - 1, borda);
        g.fill(x + 3, y + 1, x + s - 3, y + s - 1, fundo);
        g.fill(x + 1, y + 3, x + s - 1, y + s - 3, fundo);
        g.fill(x + 2, y + 2, x + s - 2, y + s - 2, fundo);
        if (it.id().equals(escolhido)) moldura(g, x - 1, y - 1, s + 2, s + 2, 0x80FFFFFF);
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(Identifier.tryParse(it.icone().isEmpty() ? "minecraft:book" : it.icone()));
        g.item(new net.minecraft.world.item.ItemStack(item.orElse(net.minecraft.world.item.Items.BOOK)), cx - 8, cy - 8);
        if (!tem) g.fill(x + 2, y + 2, x + s - 2, y + s - 2, pode ? 0x50101521 : 0xA0101521);
        if (sobre && tem) brilhoIcone(g, x + 1, y + 1, s - 2, cor, t);
        String chave = "melhoria.dcuniverse." + it.id() + ".rotulo";
        String r = net.minecraft.client.resources.language.I18n.get(chave);
        if (!r.equals(chave)) {
            int w = Math.round(font.width(r) * 0.75f) + 2;
            g.fill(x + s - w, y + s - 7, x + s + 1, y + s + 1, 0xE0101521);
            pequeno(g, font, Component.literal(r), x + s - w + 1, y + s - 6, tem ? OURO : TEXTO);
        }
        if (!tem) custo(g, it.custo(), cx, y + s + 2, pode);
    }

    /** Custo em losangos embaixo do nó. */
    private static void custo(GuiGraphicsExtractor g, int n, int cx, int y, boolean pode) {
        for (int c = 0; c < n; c++) losango(g, cx - n * 3 + c * 6 + 1, y, pode ? OURO : 0xFF6A6A6A);
    }

    /** Nome de um nó: a habilidade, ou "Habilidade: melhoria". */
    static Component nome(Rede.ArvoreItem it) {
        return it.melhoria() ? Component.translatable("melhoria.dcuniverse." + it.id()) : Component.translatable("habilidade.dcuniverse." + it.id());
    }

    static Component descricao(Rede.ArvoreItem it) {
        return Component.translatable((it.melhoria() ? "melhoria.dcuniverse." : "habilidade.dcuniverse.") + it.id() + ".desc");
    }

    private void painel(GuiGraphicsExtractor g, Rede.ArvoreSync a, int cor, float t) {
        int px = dx, py = gy - 12, ph = sy - 20 - py;
        g.fill(px, py, px + dw, py + ph, 0xC0121826);
        moldura(g, px, py, dw, ph, 0xFF2E3546);
        bw = 0;
        uw = 0;
        Rede.ArvoreItem it = a.item(foco.isEmpty() ? escolhido : foco);
        if (it == null) return;
        boolean tem = a.tem(it.id());
        boolean pode = a.podeComprar(it.id());
        int tx = px + 36;
        if (it.melhoria()) {
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(Identifier.tryParse(it.icone().isEmpty() ? "minecraft:book" : it.icone()));
            g.item(new net.minecraft.world.item.ItemStack(item.orElse(net.minecraft.world.item.Items.BOOK)), px + 10, py + 10);
            pequeno(g, font, Component.translatable("dcuniverse.arvore.melhoria_de", Component.translatable("habilidade.dcuniverse." + it.pai())), tx, py + 19, SUAVE);
        } else {
            icone(g, it.id(), px + 6, py + 6, 24, tem ? 0xFFFFFFFF : 0xFF8A8A8A);
            pequeno(g, font, resumo(it), tx, py + 19, SUAVE);
        }
        Component titulo = nome(it).copy().withStyle(ChatFormatting.BOLD);
        // Nome comprido (algumas melhorias): menor, para caber no painel.
        if (font.width(titulo) > px + dw - 4 - tx) pequeno(g, font, titulo, tx, py + 8, tem ? OURO : TEXTO);
        else g.text(font, titulo, tx, py + 7, tem ? OURO : TEXTO, true);
        // Estado e ação.
        Component estado;
        int corEstado;
        Rede.ArvoreItem falta = falta(a, it);
        if (tem) {
            int s = a.slots().indexOf(it.id());
            estado = it.melhoria() ? Component.translatable("dcuniverse.arvore.melhoria_ativa")
                : s >= 0 && s < a.slotsAbertos() ? Component.translatable("dcuniverse.arvore.no_slot", teclaDoSlot(s))
                : Component.translatable("dcuniverse.arvore.comprada");
            corEstado = VERDE;
        } else if (falta != null) {
            estado = Component.translatable("dcuniverse.arvore.exige", nome(falta));
            corEstado = VERMELHO;
        } else if (pode) {
            estado = a.criativo() ? Component.translatable("dcuniverse.arvore.pode_criativo")
                : Component.translatable("dcuniverse.arvore.pode_comprar", it.custo());
            corEstado = OURO;
        } else {
            estado = Component.translatable("dcuniverse.arvore.faltam", it.custo() - Math.max(0, a.pontos()));
            corEstado = VERMELHO;
        }
        int ty = py + 34;
        g.fill(px + 6, ty - 3, px + dw - 6, ty - 2, LINHA);
        pequeno(g, font, estado, px + 6, ty, corEstado);
        ty += 10;
        boolean desfaz = tem && a.criativo() && !it.id().equals(a.inicial());
        // Descrição.
        List<FormattedCharSequence> linhas = font.split(descricao(it), Math.round((dw - 12) / 0.75f));
        int fundoTexto = py + ph - (!tem || desfaz ? 26 : 18);
        for (FormattedCharSequence l : linhas) {
            if (ty + 7 > fundoTexto) break;
            g.pose().pushMatrix();
            g.pose().translate(px + 6, ty);
            g.pose().scale(0.75f, 0.75f);
            g.text(font, l, 0, 0, 0xFFC8CCD8, false);
            g.pose().popMatrix();
            ty += 8;
        }
        // Botão de comprar (o painel segue o nó clicado, então o botão só vale para ele).
        boolean doEscolhido = it.id().equals(escolhido);
        if (!tem) {
            bw = dw - 12;
            bh = 16;
            bx = px + 6;
            by = py + ph - bh - 6;
            boolean ok = pode && doEscolhido;
            g.fill(bx, by, bx + bw, by + bh, ok ? comAlfa(cor, 0xE0) : 0xC0262C38);
            moldura(g, bx, by, bw, bh, ok ? pulso(OURO, t) : 0xFF3A4458);
            Component c = doEscolhido ? Component.translatable("dcuniverse.arvore.comprar", it.custo()).withStyle(ChatFormatting.BOLD)
                : Component.translatable("dcuniverse.arvore.clique_comprar");
            g.centeredText(font, c, bx + bw / 2, by + 4, ok ? 0xFFFFFFFF : 0xFF8A8A8A);
            if (!doEscolhido) bw = 0;
        } else if (desfaz && doEscolhido) {
            uw = dw - 12;
            uh = 16;
            ux = px + 6;
            uy = py + ph - uh - 6;
            g.fill(ux, uy, ux + uw, uy + uh, 0xC0302024);
            moldura(g, ux, uy, uw, uh, 0xFF6A3A3A);
            g.centeredText(font, Component.translatable("dcuniverse.arvore.desfazer"), ux + uw / 2, uy + 4, 0xFFFFB0A0);
        } else if (!it.melhoria()) {
            pequeno(g, font, Component.translatable("dcuniverse.arvore.dica_equipar"), px + 6, py + ph - 12, SUAVE);
        }
    }

    /** O que falta comprar antes deste nó (a habilidade ou a melhoria exigida), ou null. */
    private static Rede.ArvoreItem falta(Rede.ArvoreSync a, Rede.ArvoreItem it) {
        if (!it.melhoria()) return null;
        if (!a.tem(it.pai())) return a.item(it.pai());
        if (!it.requer().isEmpty() && !a.tem(it.requer())) return a.item(it.requer());
        return null;
    }

    private void slots(GuiGraphicsExtractor g, Rede.ArvoreSync a, int mx, int my, int cor, float t) {
        Component titulo = Component.translatable("dcuniverse.arvore.build").withStyle(ChatFormatting.BOLD);
        g.text(font, titulo, sx0, sy - 12, SUAVE, false);
        for (int i = 0; i < Arvore.SLOTS; i++) {
            int x = sx0 + i * (slot + 6);
            boolean aberto = i < a.slotsAbertos();
            String id = i < a.slots().size() ? a.slots().get(i) : "";
            boolean sobre = dentro(mx, my, x, sy, slot, slot);
            g.fill(x, sy, x + slot, sy + slot, !aberto ? 0xB00A0D14 : sobre ? 0xE0283450 : 0xD0161C2A);
            int borda = !aberto ? 0xFF22262E : sobre && !pegando.isEmpty() ? pulso(OURO, t) : !id.isEmpty() ? comAlfa(cor, 0xFF) : 0xFF3A4458;
            moldura(g, x, sy, slot, slot, borda);
            if (aberto && !id.isEmpty() && !(id.equals(pegando) && pegouDoSlot == i && arrastou)) {
                icone(g, id, x + 3, sy + 3, slot - 6, 0xFFFFFFFF);
            }
            if (!aberto) {
                cadeado(g, x + slot / 2, sy + 7);
                String nv = Component.translatable("dcuniverse.arvore.nivel_curto", Arvore.SLOT_NIVEL[i]).getString();
                pequeno(g, font, Component.literal(nv), x + slot / 2 - Math.round(font.width(nv) * 0.75f / 2), sy + slot - 9, 0xFF6A6A6A);
            }
            String tecla = teclaDoSlot(i);
            pequeno(g, font, Component.literal(tecla), x + slot / 2 - Math.round(font.width(tecla) * 0.75f / 2), sy + slot + 3, aberto ? TEXTO : 0xFF5A5A5A);
        }
    }

    // ------------------------------------------------------------------ mouse

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean duplo) {
        Rede.ArvoreSync a = EstadoCliente.arvore;
        double mx = e.x(), my = e.y();
        int s = slotEm(mx, my);
        if (e.button() == 1 && s >= 0 && s < a.slotsAbertos()) {
            // Botão direito esvazia o slot.
            mudarSlot(s, "");
            return true;
        }
        if (e.button() != 0) return super.mouseClicked(e, duplo);
        if (bw > 0 && dentro(mx, my, bx, by, bw, bh)) {
            if (a.podeComprar(escolhido)) {
                Rede.CANAL.send(new Rede.ArvoreComprar(escolhido, false), com.danrod.dcuniverse.core.Canal.SERVIDOR);
                som(1.0f);
            } else {
                som(0.5f);
            }
            return true;
        }
        if (uw > 0 && dentro(mx, my, ux, uy, uw, uh)) {
            Rede.CANAL.send(new Rede.ArvoreComprar(escolhido, true), com.danrod.dcuniverse.core.Canal.SERVIDOR);
            som(0.7f);
            return true;
        }
        if (!foco.isEmpty()) {
            String id = foco;
            Rede.ArvoreItem it = a.item(id);
            // Duplo clique num nó que dá para comprar: compra direto.
            if (duplo && id.equals(escolhido) && a.podeComprar(id)) {
                Rede.CANAL.send(new Rede.ArvoreComprar(id, false), com.danrod.dcuniverse.core.Canal.SERVIDOR);
                som(1.0f);
                return true;
            }
            escolhido = id;
            if (a.tem(id) && it != null && !it.melhoria()) comecarArrasto(id, -1, mx, my);
            som(1.4f);
            return true;
        }
        if (dentro(mx, my, gx, gy, gw, gh) && maxPan() > 0) {
            panando = true;
            return true;
        }
        if (s >= 0 && s < a.slotsAbertos()) {
            String id = s < a.slots().size() ? a.slots().get(s) : "";
            if (!id.isEmpty()) {
                escolhido = id;
                comecarArrasto(id, s, mx, my);
            }
            return true;
        }
        return super.mouseClicked(e, duplo);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double ddx, double ddy) {
        if (panando) {
            pan = Mth.clamp(pan - ddx, 0, maxPan());
            return true;
        }
        if (!pegando.isEmpty() && Math.abs(e.x() - pegouX) + Math.abs(e.y() - pegouY) > 3) arrastou = true;
        return !pegando.isEmpty() || super.mouseDragged(e, ddx, ddy);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        if (maxPan() > 0 && dentro(x, y, gx, gy, gw, gh)) {
            pan = Mth.clamp(pan - (sy + sx) * 24, 0, maxPan());
            return true;
        }
        return super.mouseScrolled(x, y, sx, sy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        panando = false;
        if (pegando.isEmpty() || e.button() != 0) return super.mouseReleased(e);
        Rede.ArvoreSync a = EstadoCliente.arvore;
        int s = slotEm(e.x(), e.y());
        if (arrastou && s >= 0 && s < a.slotsAbertos()) {
            if (pegouDoSlot >= 0) trocarSlots(pegouDoSlot, s);
            else mudarSlot(s, pegando);
        } else if (!arrastou && pegouDoSlot < 0) {
            // Clique simples numa comprada: vai para o primeiro slot vazio (se tiver).
            int vazio = a.slots().indexOf("");
            if (!a.slots().contains(pegando) && vazio >= 0 && vazio < a.slotsAbertos()) mudarSlot(vazio, pegando);
        }
        pegando = "";
        pegouDoSlot = -1;
        arrastou = false;
        return true;
    }

    private void comecarArrasto(String id, int doSlot, double mx, double my) {
        pegando = id;
        pegouDoSlot = doSlot;
        pegouX = mx;
        pegouY = my;
        arrastou = false;
    }

    private void mudarSlot(int s, String id) {
        List<String> novos = new ArrayList<>(slotsAtuais());
        if (!id.isEmpty()) {
            int antes = novos.indexOf(id);
            // Já estava em outro slot: troca de lugar com o que estiver no destino.
            if (antes >= 0) novos.set(antes, novos.get(s));
        }
        novos.set(s, id);
        enviar(novos);
    }

    private void trocarSlots(int de, int para) {
        if (de == para) return;
        List<String> novos = new ArrayList<>(slotsAtuais());
        String x = novos.get(de);
        novos.set(de, novos.get(para));
        novos.set(para, x);
        enviar(novos);
    }

    private static List<String> slotsAtuais() {
        List<String> s = new ArrayList<>(EstadoCliente.arvore.slots());
        while (s.size() < Arvore.SLOTS) s.add("");
        return s.subList(0, Arvore.SLOTS);
    }

    private void enviar(List<String> novos) {
        Rede.CANAL.send(new Rede.ArvoreSlots(List.copyOf(novos)), com.danrod.dcuniverse.core.Canal.SERVIDOR);
        // Mostra já; o servidor confirma em seguida.
        Rede.ArvoreSync a = EstadoCliente.arvore;
        EstadoCliente.arvore = new Rede.ArvoreSync(a.heroi(), a.nivel(), a.xp(), a.xpNivel(), a.xpProximo(), a.pontos(), a.slotsAbertos(),
            a.criativo(), a.inicial(), a.compradas(), List.copyOf(novos), a.itens(), a.salto());
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.2f, 0.7f));
    }

    private void som(float tom) {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), tom, 0.5f));
    }

    private int slotEm(double mx, double my) {
        for (int i = 0; i < Arvore.SLOTS; i++) {
            if (dentro(mx, my, sx0 + i * (slot + 6), sy, slot, slot)) return i;
        }
        return -1;
    }

    // ------------------------------------------------------------------ peças compartilhadas

    static String teclaDoSlot(int s) {
        String t = Atalhos.tecla(s).getTranslatedKeyMessage().getString();
        return t.length() > 3 ? t.substring(0, 3) : t;
    }

    /** Recarga e jeito de usar: "Recarga 6 s • apertar". */
    static Component resumo(Rede.ArvoreItem it) {
        String rec = it.recarga() <= 0 ? "" : String.format(Locale.ROOT, it.recarga() % 1 == 0 ? "%.0f" : "%.1f", it.recarga());
        Component modo = it.rajada() > 0 ? Component.translatable("dcuniverse.arvore.rajada", String.format(Locale.ROOT, "%.0f", it.rajada()))
            : Component.translatable(it.hold() ? "dcuniverse.arvore.segurar" : "dcuniverse.arvore.apertar");
        return rec.isEmpty() ? modo : Component.translatable("dcuniverse.arvore.recarga", rec).append(" • ").append(modo);
    }

    static void icone(GuiGraphicsExtractor g, String id, int x, int y, int tam, int tinta) {
        Identifier tex = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "textures/gui/ability/" + id + ".png");
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0, 0, tam, tam, 32, 32, 32, 32, tinta);
    }

    /** Halo suave na cor do herói em volta do ícone em foco. */
    static void brilhoIcone(GuiGraphicsExtractor g, int x, int y, int tam, int cor, float t) {
        int a = 0x30 + Math.round(0x30 * (0.5f + 0.5f * Mth.sin(t * 0.25f)));
        g.fill(x - 2, y - 2, x + tam + 2, y, comAlfa(cor, a));
        g.fill(x - 2, y + tam, x + tam + 2, y + tam + 2, comAlfa(cor, a));
        g.fill(x - 2, y, x, y + tam, comAlfa(cor, a));
        g.fill(x + tam, y, x + tam + 2, y + tam, comAlfa(cor, a));
    }

    static void pequeno(GuiGraphicsExtractor g, Font font, Component c, int x, int y, int cor) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(0.75f, 0.75f);
        g.text(font, c, 0, 0, cor, true);
        g.pose().popMatrix();
    }

    static void moldura(GuiGraphicsExtractor g, int x, int y, int w, int h, int cor) {
        g.fill(x, y, x + w, y + 1, cor);
        g.fill(x, y + h - 1, x + w, y + h, cor);
        g.fill(x, y, x + 1, y + h, cor);
        g.fill(x + w - 1, y, x + w, y + h, cor);
    }

    private static void losango(GuiGraphicsExtractor g, int x, int y, int cor) {
        g.fill(x + 1, y, x + 2, y + 1, cor);
        g.fill(x, y + 1, x + 3, y + 2, cor);
        g.fill(x + 1, y + 2, x + 2, y + 3, cor);
    }

    private static void cadeado(GuiGraphicsExtractor g, int cx, int y) {
        int c = 0xFF5A5E68;
        moldura(g, cx - 2, y, 5, 4, c);
        g.fill(cx - 4, y + 3, cx + 5, y + 10, c);
        g.fill(cx, y + 5, cx + 1, y + 8, 0xFF0A0D14);
    }

    static int comAlfa(int rgb, int alfa) {
        return (alfa << 24) | (rgb & 0xFFFFFF);
    }

    static int clarear(int rgb, float t) {
        int r = (rgb >> 16) & 0xFF, gg = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        r += Math.round((255 - r) * t);
        gg += Math.round((255 - gg) * t);
        b += Math.round((255 - b) * t);
        return (r << 16) | (gg << 8) | b;
    }

    /** A cor pulsando devagar entre 55% e 100% do brilho. */
    static int pulso(int argb, float t) {
        float k = 0.55f + 0.45f * (0.5f + 0.5f * Mth.sin(t * 0.2f));
        int r = Math.round(((argb >> 16) & 0xFF) * k), gg = Math.round(((argb >> 8) & 0xFF) * k), b = Math.round((argb & 0xFF) * k);
        return (argb & 0xFF000000) | (r << 16) | (gg << 8) | b;
    }

    private static boolean dentro(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
