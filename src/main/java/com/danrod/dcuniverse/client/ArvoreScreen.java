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
 * <p>Em cima: o herói, o nível, a experiência e os pontos livres. À esquerda, as habilidades da árvore (compradas com
 * moldura dourada, as que dá para comprar acesas, as outras apagadas com o custo). À direita, a habilidade em foco:
 * números, descrição e o botão de comprar. Embaixo, os 6 slots com as teclas Z X C V B R (os trancados mostram o nível
 * em que abrem). Para montar a build: arrastar uma habilidade comprada até um slot, ou clicar nela e depois no slot.
 * Arrastar de um slot para outro troca os dois; botão direito num slot esvazia.
 *
 * <p>O servidor confere tudo (ability/Arvore); a tela só pede e mostra o que voltou.
 */
public final class ArvoreScreen extends Screen {
    static final int OURO = 0xFFE8C468, TEXTO = 0xFFEDE6D6, SUAVE = 0xFF9C9584, VERDE = 0xFF7CE08A, VERMELHO = 0xFFFF7A6A;
    private static final int FUNDO = 0xE8101521, FUNDO2 = 0xF0171D2C, LINHA = 0x40E8C468;

    private int foco = -1;               // índice na lista de itens sob o mouse
    private String escolhido = "";       // habilidade em foco no painel (clicada)
    private String pegando = "";         // habilidade sendo arrastada ("" = nenhuma)
    private int pegouDoSlot = -1;        // slot de onde saiu o arrasto (-1 = da grade)
    private double pegouX, pegouY;
    private boolean arrastou;
    private float idade;

    // Layout.
    private int x0, y0, x1, y1, gx, gy, gw, colunas, tile, dx, dw, sy, sx0, slot;
    private int bx, by, bw, bh;          // botão comprar

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
        dw = Math.max(130, Math.min(200, largura * 2 / 5));
        dx = x1 - dw - 6;
        gx = x0 + 8;
        gy = corpoY + 14;
        gw = dx - gx - 10;
        tile = 38;
        colunas = Math.max(2, (gw + 4) / (tile + 4));
        int itens = Math.max(1, EstadoCliente.arvore.itens().size());
        int linhas = (itens + colunas - 1) / colunas;
        int cabe = sy - 18 - gy;
        // Se não cabe, diminui os quadros.
        while (linhas * (tile + 4) > cabe && tile > 26) {
            tile -= 2;
            colunas = Math.max(2, (gw + 4) / (tile + 4));
            linhas = (itens + colunas - 1) / colunas;
        }
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
        grade(g, a, mx, my, cor, t);
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
        boolean tem = a.pontos() > 0 && !a.criativo();
        g.fill(px, y0 + 6, px + pw, y0 + 21, tem ? 0xE04A3A12 : 0xC0202838);
        moldura(g, px, y0 + 6, pw, 15, tem ? pulso(OURO, t) : 0xFF3A4458);
        g.text(font, pts, px + 6, y0 + 10, tem ? OURO : TEXTO, true);
    }

    private void grade(GuiGraphicsExtractor g, Rede.ArvoreSync a, int mx, int my, int cor, float t) {
        g.text(font, Component.translatable("dcuniverse.arvore.habilidades").withStyle(ChatFormatting.BOLD), gx, gy - 12, SUAVE, false);
        List<Rede.ArvoreItem> itens = a.itens();
        foco = -1;
        for (int i = 0; i < itens.size(); i++) {
            Rede.ArvoreItem it = itens.get(i);
            int x = gx + (i % colunas) * (tile + 4), y = gy + (i / colunas) * (tile + 4);
            boolean sobre = dentro(mx, my, x, y, tile, tile);
            if (sobre) foco = i;
            boolean tem = a.tem(it.id());
            boolean podeComprar = !tem && a.pontos() >= it.custo() && !a.precisaEscolher();
            boolean noSlot = a.slots().contains(it.id()) && a.slots().indexOf(it.id()) < a.slotsAbertos();
            int fundo = it.id().equals(escolhido) ? 0xE02A3450 : sobre ? 0xD0222C42 : 0xC0141A28;
            g.fill(x, y, x + tile, y + tile, fundo);
            if (tem) g.fillGradient(x, y + tile / 2, x + tile, y + tile, 0x00000000, comAlfa(cor, 0x40));
            int borda = tem ? OURO : podeComprar ? pulso(0xFFFFFFFF, t) : 0xFF2E3546;
            moldura(g, x, y, tile, tile, borda);
            if (it.id().equals(escolhido)) moldura(g, x - 1, y - 1, tile + 2, tile + 2, 0x80FFFFFF);
            int tam = tile - 14;
            icone(g, it.id(), x + (tile - tam) / 2, y + 3, tam, tem ? 0xFFFFFFFF : podeComprar ? 0xFFB8B8B8 : 0xFF5A5A5A);
            if (sobre && tem) brilhoIcone(g, x + (tile - tam) / 2, y + 3, tam, cor, t);
            // Custo em pontos (losangos) embaixo, ou o selo de equipada.
            int baseY = y + tile - 8;
            if (tem) {
                if (noSlot) {
                    String tecla = teclaDoSlot(a.slots().indexOf(it.id()));
                    pequeno(g, font, Component.literal(tecla), x + tile / 2 - Math.round(font.width(tecla) * 0.75f / 2), baseY, OURO);
                }
            } else {
                for (int c = 0; c < it.custo(); c++) {
                    int lx = x + tile / 2 - it.custo() * 3 + c * 6 + 1;
                    losango(g, lx, baseY + 1, podeComprar ? OURO : 0xFF6A6A6A);
                }
            }
        }
    }

    private void painel(GuiGraphicsExtractor g, Rede.ArvoreSync a, int cor, float t) {
        int px = dx, py = gy - 12, ph = sy - 20 - py;
        g.fill(px, py, px + dw, py + ph, 0xC0121826);
        moldura(g, px, py, dw, ph, 0xFF2E3546);
        bw = 0;
        Rede.ArvoreItem it = a.item(escolhido);
        if (it == null) return;
        boolean tem = a.tem(it.id());
        icone(g, it.id(), px + 6, py + 6, 24, tem ? 0xFFFFFFFF : 0xFF8A8A8A);
        g.text(font, Component.translatable("habilidade.dcuniverse." + it.id()).withStyle(ChatFormatting.BOLD), px + 36, py + 7, tem ? OURO : TEXTO, true);
        pequeno(g, font, resumo(it), px + 36, py + 19, SUAVE);
        // Estado e ação.
        Component estado;
        int corEstado;
        if (tem) {
            int s = a.slots().indexOf(it.id());
            estado = s >= 0 && s < a.slotsAbertos() ? Component.translatable("dcuniverse.arvore.no_slot", teclaDoSlot(s))
                : Component.translatable("dcuniverse.arvore.comprada");
            corEstado = VERDE;
        } else if (a.pontos() >= it.custo()) {
            estado = Component.translatable("dcuniverse.arvore.pode_comprar", it.custo());
            corEstado = OURO;
        } else {
            estado = Component.translatable("dcuniverse.arvore.faltam", it.custo() - Math.max(0, a.pontos()));
            corEstado = VERMELHO;
        }
        int ty = py + 34;
        g.fill(px + 6, ty - 3, px + dw - 6, ty - 2, LINHA);
        pequeno(g, font, estado, px + 6, ty, corEstado);
        ty += 10;
        // Descrição.
        List<FormattedCharSequence> linhas = font.split(Component.translatable("habilidade.dcuniverse." + it.id() + ".desc"), Math.round((dw - 12) / 0.75f));
        int fundoTexto = py + ph - (tem ? 18 : 26);
        for (FormattedCharSequence l : linhas) {
            if (ty + 7 > fundoTexto) break;
            g.pose().pushMatrix();
            g.pose().translate(px + 6, ty);
            g.pose().scale(0.75f, 0.75f);
            g.text(font, l, 0, 0, 0xFFC8CCD8, false);
            g.pose().popMatrix();
            ty += 8;
        }
        // Botão de comprar (ou a dica de como equipar).
        if (!tem && !a.criativo()) {
            bw = dw - 12;
            bh = 16;
            bx = px + 6;
            by = py + ph - bh - 6;
            boolean ok = a.pontos() >= it.custo();
            g.fill(bx, by, bx + bw, by + bh, ok ? comAlfa(cor, 0xE0) : 0xC0262C38);
            moldura(g, bx, by, bw, bh, ok ? pulso(OURO, t) : 0xFF3A4458);
            Component c = Component.translatable("dcuniverse.arvore.comprar", it.custo()).withStyle(ChatFormatting.BOLD);
            g.centeredText(font, c, bx + bw / 2, by + 4, ok ? 0xFFFFFFFF : 0xFF8A8A8A);
        } else {
            pequeno(g, font, Component.translatable("dcuniverse.arvore.dica_equipar"), px + 6, py + ph - 12, SUAVE);
        }
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
            Rede.ArvoreItem it = a.item(escolhido);
            if (it != null && a.pontos() >= it.custo()) {
                Rede.CANAL.send(new Rede.ArvoreComprar(it.id()), com.danrod.dcuniverse.core.Canal.SERVIDOR);
                som(1.0f);
            } else {
                som(0.5f);
            }
            return true;
        }
        if (foco >= 0 && foco < a.itens().size()) {
            String id = a.itens().get(foco).id();
            escolhido = id;
            if (a.tem(id)) comecarArrasto(id, -1, mx, my);
            som(1.4f);
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
        if (!pegando.isEmpty() && Math.abs(e.x() - pegouX) + Math.abs(e.y() - pegouY) > 3) arrastou = true;
        return !pegando.isEmpty() || super.mouseDragged(e, ddx, ddy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
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
            a.criativo(), a.inicial(), a.compradas(), List.copyOf(novos), a.itens());
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
        Component modo = Component.translatable(it.hold() ? "dcuniverse.arvore.segurar" : "dcuniverse.arvore.apertar");
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
