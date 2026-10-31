package com.danrod.dcuniverse.client.npc;

import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.npc.Afinidade;
import com.danrod.dcuniverse.npc.ItemRef;
import com.danrod.dcuniverse.npc.NpcEntidade;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Conversa com um NPC: painel embaixo com o busto 3D do NPC (olhando para o mouse), nome, nível de afinidade com
 * a barra, a fala aparecendo letra por letra e as opções numeradas acima do painel (teclas 1 a 9). Quem decide o
 * conteúdo é o servidor (npc/Dialogo); a tela só mostra e devolve a escolha.
 */
public final class DialogoScreen extends Screen {
    private static final int OURO = 0xFFE8C468, TEXTO = 0xFFEDE6D6, SUAVE = 0xFF9C9584, FUNDO = 0xE0141821, FUNDO2 = 0xE01C2230,
        BORDA = 0xFF8A6B2E, LINHA = 0x40E8C468, OP = 0xC0161B26, OP_SOBRE = 0xE02A3448, BLOQ = 0xFF6A665E;
    private static final int H = 112, OP_H = 20, RETRATO = 76;
    /** Letras por milissegundo no efeito de digitação. */
    private static final float VELOCIDADE = 0.055f;

    private Rede.NpcAbrir m;
    private List<FormattedCharSequence> linhas = List.of();
    private int letras;
    private long inicio;
    private int x0, y0, w;

    private DialogoScreen(Rede.NpcAbrir m) {
        super(Component.empty());
        this.m = m;
    }

    /** Chegou uma fala nova: atualiza a tela aberta da mesma conversa ou abre uma. */
    public static void receber(Rede.NpcAbrir m) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof DialogoScreen d && d.m.entidade() == m.entidade()) {
            d.trocar(m);
            return;
        }
        mc.gui.setScreen(new DialogoScreen(m));
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.2f, 0.6f));
    }

    /** Fala solta de NPC no chat: "[Nome] texto". */
    public static void falar(Rede.NpcFala f) {
        Minecraft mc = Minecraft.getInstance();
        Component nome = mc.level != null && f.entidade() >= 0 && mc.level.getEntity(f.entidade()) instanceof NpcEntidade npc
            ? npc.nomeExibido() : f.npc().isEmpty() ? Component.literal("?") : NpcsCliente.nome(Identifier.parse(f.npc()));
        mc.gui.hud.getChat().addClientSystemMessage(Component.literal("[").append(nome).append("] ").withStyle(ChatFormatting.GOLD)
            .append(NpcsCliente.texto(f.texto(), f.args()).withStyle(ChatFormatting.WHITE)));
    }

    private void trocar(Rede.NpcAbrir novo) {
        m = novo;
        quebrar();
        inicio = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        w = Math.min(width - 16, 400);
        x0 = (width - w) / 2;
        y0 = height - H - 8;
        if (inicio == 0) inicio = System.currentTimeMillis();
        quebrar();
    }

    private void quebrar() {
        if (font == null) return;
        linhas = font.split(NpcsCliente.texto(m.texto(), m.args()), w - RETRATO - 22);
        letras = 0;
        for (FormattedCharSequence l : linhas) letras += comprimento(l);
    }

    private static int comprimento(FormattedCharSequence l) {
        int[] n = {0};
        l.accept((i, estilo, cp) -> {
            n[0]++;
            return true;
        });
        return n[0];
    }

    private boolean digitando() {
        return (System.currentTimeMillis() - inicio) * VELOCIDADE < letras;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    // ---------------------------------------------------------------- desenho

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float a) {
        Entity e = minecraft.level == null ? null : minecraft.level.getEntity(m.entidade());
        if (!(e instanceof NpcEntidade npc) || npc.distanceToSqr(minecraft.player) > 100) {
            onClose();
            return;
        }
        // Painel com moldura dourada e um degradê suave.
        g.fill(x0, y0, x0 + w, y0 + H, FUNDO);
        g.fill(x0 + 1, y0 + 1, x0 + w - 1, y0 + H / 2, FUNDO2);
        moldura(g, x0, y0, w, H, BORDA);
        // Retrato: o NPC em 3D do peito para cima.
        int rx0 = x0 + 6, ry0 = y0 + 6, rx1 = rx0 + RETRATO - 8, ry1 = y0 + H - 6;
        g.fill(rx0, ry0, rx1, ry1, 0xFF0C0F15);
        // Segue o mouse só de leve: olhar muito para cima inclinaria o busto para fora do retrato.
        float cx = (rx0 + rx1) / 2f, cy = (ry0 + ry1) / 2f;
        float ox = cx + Mth.clamp(mx - cx, -120, 120) * 0.25f, oy = cy + Mth.clamp(my - cy, -60, 60) * 0.12f;
        busto(g, rx0, ry0, rx1, ry1, ox, oy, npc);
        moldura(g, rx0, ry0, rx1 - rx0, ry1 - ry0, 0xFF4E3E1E);
        // Nome, nível e afinidade.
        int tx = x0 + RETRATO + 6, ty = y0 + 8;
        g.text(font, npc.nomeExibido().copy().withStyle(ChatFormatting.BOLD), tx, ty, OURO, true);
        int nivel = Afinidade.nivel(m.afinidade());
        Component nomeNivel = Component.translatable("dcuniverse.npc.nivel." + nivel);
        int bw = 74, bx = x0 + w - bw - 10;
        g.text(font, nomeNivel, bx - font.width(nomeNivel) - 6, ty, SUAVE, false);
        barra(g, bx, ty + 2, bw, m.afinidade());
        if (sobre(mx, my, bx, ty, bw, 8)) {
            g.setTooltipForNextFrame(Component.translatable("dcuniverse.npc.afinidade", m.afinidade(), Component.translatable("dcuniverse.npc.grupo." + m.grupo())), mx, my);
        }
        g.fill(tx, ty + 12, x0 + w - 10, ty + 13, LINHA);
        // Fala, letra por letra.
        int mostrar = (int) Math.min(letras, (System.currentTimeMillis() - inicio) * VELOCIDADE);
        int y = ty + 18;
        for (FormattedCharSequence l : linhas) {
            if (mostrar <= 0) break;
            int n = comprimento(l);
            g.text(font, n <= mostrar ? l : cortar(l, mostrar), tx, y, TEXTO, false);
            mostrar -= n;
            y += 10;
            if (y > y0 + H - 12) break;
        }
        if (!digitando() && (System.currentTimeMillis() / 400) % 2 == 0) g.text(font, "▼", x0 + w - 14, y0 + H - 12, OURO, false);
        opcoes(g, mx, my);
        super.extractRenderState(g, mx, my, a);
    }

    private static FormattedCharSequence cortar(FormattedCharSequence l, int n) {
        return out -> {
            int[] i = {0};
            return l.accept((pos, estilo, cp) -> i[0]++ < n && out.accept(pos, estilo, cp));
        };
    }

    /** Opções numeradas, empilhadas acima do painel e alinhadas à direita. */
    private void opcoes(GuiGraphicsExtractor g, int mx, int my) {
        if (digitando()) return;
        int ow = opW(), ox = x0 + w - ow;
        int n = m.opcoes().size();
        for (int i = 0; i < n; i++) {
            Rede.Opcao o = m.opcoes().get(i);
            int oy = y0 - 4 - (n - i) * (OP_H + 2);
            boolean sobre = o.ativa() && sobre(mx, my, ox, oy, ow, OP_H);
            g.fill(ox, oy, ox + ow, oy + OP_H, sobre ? OP_SOBRE : OP);
            g.fill(ox, oy, ox + 2, oy + OP_H, o.ativa() ? (sobre ? OURO : BORDA) : 0xFF3A3A3A);
            g.text(font, (i + 1) + ".", ox + 6, oy + 6, o.ativa() ? OURO : BLOQ, false);
            int cor = o.ativa() ? (sobre ? 0xFFFFFFFF : TEXTO) : BLOQ;
            int fim = ox + ow - 6 - larguraItens(o);
            Component rot = rotulo(o);
            String t = font.plainSubstrByWidth(rot.getString(), fim - (ox + 20) - 4);
            g.text(font, t.length() < rot.getString().length() ? Component.literal(t + "…") : rot, ox + 20, oy + 6, cor, false);
            itens(g, o, fim, oy + 2, mx, my);
        }
    }

    private int opW() {
        int max = 150;
        for (Rede.Opcao o : m.opcoes()) max = Math.max(max, 30 + font.width(rotulo(o)) + larguraItens(o));
        return Math.min(max, w - 40);
    }

    private static MutableComponent rotulo(Rede.Opcao o) {
        return NpcsCliente.texto(o.texto(), o.args());
    }

    /** Numa troca: entrada(s) → saída; numa missão: os itens de prêmio; numa entrega: o que vai ser entregue. */
    private static int larguraItens(Rede.Opcao o) {
        if (o.itens().isEmpty()) return 0;
        return o.itens().size() * 18 + (o.id().startsWith("troca:") ? 10 : 0);
    }

    private void itens(GuiGraphicsExtractor g, Rede.Opcao o, int x, int y, int mx, int my) {
        boolean troca = o.id().startsWith("troca:");
        for (int i = 0; i < o.itens().size(); i++) {
            ItemRef r = ItemRef.deTexto(o.itens().get(i));
            if (troca && i == o.itens().size() - 1) {
                g.text(font, "→", x + 1, y + 4, OURO, false);
                x += 10;
            }
            ItemStack s = r.pilha();
            g.item(s, x, y);
            g.itemDecorations(font, s, x, y);
            if (sobre(mx, my, x, y, 16, 16)) g.setTooltipForNextFrame(Component.literal(r.qtd() + " × ").append(s.getHoverName()), mx, my);
            x += 18;
        }
    }

    private void barra(GuiGraphicsExtractor g, int x, int y, int bw, int valor) {
        g.fill(x - 1, y - 1, x + bw + 1, y + 5, 0xFF000000);
        g.fill(x, y, x + bw, y + 4, 0xFF2A2A2A);
        int cheio = Math.round(bw * Mth.clamp(valor, 0, 100) / 100f);
        g.fill(x, y, x + cheio, y + 2, 0xFFF2D27A);
        g.fill(x, y + 2, x + cheio, y + 4, 0xFFC99A2E);
        for (int i = 1; i < 4; i++) g.fill(x + bw * i / 4, y, x + bw * i / 4 + 1, y + 4, 0x90000000);
    }

    private static void moldura(GuiGraphicsExtractor g, int x, int y, int w, int h, int cor) {
        g.fill(x, y, x + w, y + 1, cor);
        g.fill(x, y + h - 1, x + w, y + h, cor);
        g.fill(x, y, x + 1, y + h, cor);
        g.fill(x + w - 1, y, x + w, y + h, cor);
    }

    private static boolean sobre(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ---------------------------------------------------------------- entrada

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean duplo) {
        if (e.button() == 0) {
            if (digitando()) {
                inicio = 0;
                return true;
            }
            int ow = opW(), ox = x0 + w - ow, n = m.opcoes().size();
            for (int i = 0; i < n; i++) {
                int oy = y0 - 4 - (n - i) * (OP_H + 2);
                if (sobre(e.x(), e.y(), ox, oy, ow, OP_H)) {
                    escolher(i);
                    return true;
                }
            }
        }
        return super.mouseClicked(e, duplo);
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        int k = e.key();
        if (k >= 49 && k <= 57) {
            if (digitando()) inicio = 0;
            else escolher(k - 49);
            return true;
        }
        if (k == 32 || k == 257) {
            if (digitando()) inicio = 0;
            return true;
        }
        return super.keyPressed(e);
    }

    private void escolher(int i) {
        if (i < 0 || i >= m.opcoes().size() || !m.opcoes().get(i).ativa()) return;
        String id = m.opcoes().get(i).id();
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1f, 0.35f));
        Rede.CANAL.send(new Rede.NpcEscolha(m.entidade(), id), com.danrod.dcuniverse.core.Canal.SERVIDOR);
        if (id.equals("tchau")) minecraft.gui.setScreen(null);
    }

    @Override
    public void onClose() {
        Rede.CANAL.send(new Rede.NpcEscolha(m.entidade(), "tchau"), com.danrod.dcuniverse.core.Canal.SERVIDOR);
        super.onClose();
    }

    /** Uso dos testes: a fala e as opções na tela agora. */
    public static List<String> opcoesAbertas() {
        List<String> out = new ArrayList<>();
        if (Minecraft.getInstance().gui.screen() instanceof DialogoScreen d) for (Rede.Opcao o : d.m.opcoes()) out.add(o.id());
        return out;
    }

    /** O mesmo desenho do inventário (InventoryScreen), mas sem o nome flutuando sobre a cabeça dentro do retrato. */
    private static void busto(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, float mx, float my, NpcEntidade npc) {
        float xAng = (float) Math.atan(((x0 + x1) / 2f - mx) / 40f), yAng = (float) Math.atan(((y0 + y1) / 2f - my) / 40f);
        org.joml.Quaternionf rot = new org.joml.Quaternionf().rotateZ((float) Math.PI);
        org.joml.Quaternionf rotX = new org.joml.Quaternionf().rotateX(yAng * 20f * Mth.DEG_TO_RAD);
        rot.mul(rotX);
        var disp = Minecraft.getInstance().getEntityRenderDispatcher();
        var st = disp.getRenderer(npc).createRenderState(npc, 1f);
        st.shadowPieces.clear();
        st.outlineColor = 0;
        st.nameTag = null;
        if (st instanceof net.minecraft.client.renderer.entity.state.LivingEntityRenderState l) {
            l.bodyRot = 180f + xAng * 20f;
            l.yRot = xAng * 20f;
            l.xRot = -yAng * 20f;
            l.boundingBoxWidth /= l.scale;
            l.boundingBoxHeight /= l.scale;
            l.scale = 1f;
        }
        g.entity(st, 62, new org.joml.Vector3f(0, st.boundingBoxHeight / 2f + 0.62f, 0), rot, rotX, x0, y0, x1, y1);
    }
}
