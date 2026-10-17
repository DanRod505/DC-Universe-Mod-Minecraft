package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.content.Batcomputador;
import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tela do Batcomputador (content/Batcomputador decide tudo no servidor). Painel escuro com acentos ciano, quatro abas
 * (Gadgets, Fabricar, Análise, Mapa), uma linha de varredura descendo e o rodapé com o estado do sistema.
 */
public final class BatcomputadorScreen extends Screen {
    private static final int W = 340, H = 216;
    private static final int FUNDO = 0xF2080C14, FUNDO2 = 0xFF0E1522, CIANO = 0xFF39C8F0, CIANO_ESC = 0xFF15506A,
        TEXTO = 0xFFDDE8F0, SUAVE = 0xFF7F93A6, VERMELHO = 0xFFFF5548, VERDE = 0xFF5CE08A, OURO = 0xFFFFC857;

    enum Aba { GADGETS, FABRICAR, ANALISE, MAPA }

    private static Aba ultimaAba = Aba.GADGETS;
    private Rede.BatAbrir dados;
    private Aba aba = ultimaAba;
    private int x0, y0, rolagem, amostra, abriuEm;
    private final Map<EntityType<?>, LivingEntity> modelos = new HashMap<>();
    private int[] mapa;
    private long mapaEm = -1000;
    /** Hora do jogo em que os dados chegaram, para a contagem da recarga andar sem novo pacote. */
    private long recebidoEm;

    private BatcomputadorScreen(Rede.BatAbrir dados) {
        super(Component.translatable("block.dcuniverse.batcomputador"));
        trocar(dados);
    }

    private void trocar(Rede.BatAbrir m) {
        dados = m;
        Minecraft mc = Minecraft.getInstance();
        recebidoEm = mc.level == null ? 0 : mc.level.getGameTime();
    }

    /** Segundos que faltam para o Batcomputador recarregar de novo. */
    private int recargaFalta() {
        long passou = minecraft == null || minecraft.level == null ? 0 : (minecraft.level.getGameTime() - recebidoEm) / 20;
        return (int) Math.max(0, dados.recarga() - passou);
    }

    /** Pacote do servidor: abre a tela ou, se ela já está aberta neste bloco, só troca os dados. */
    public static void receber(Rede.BatAbrir m) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof BatcomputadorScreen s && s.dados.pos().equals(m.pos())) {
            s.trocar(m);
            return;
        }
        mc.gui.setScreen(new BatcomputadorScreen(m));
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BEACON_ACTIVATE, 2.0f, 0.25f));
    }

    @Override
    protected void init() {
        x0 = (width - W) / 2;
        y0 = (height - H) / 2;
        if (minecraft.player != null) abriuEm = minecraft.player.tickCount;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (minecraft.player == null || minecraft.player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(dados.pos()))
            > Batcomputador.ALCANCE_USO * Batcomputador.ALCANCE_USO) onClose();
    }

    @Override
    public void removed() {
        ultimaAba = aba;
    }

    private void enviar(int tipo, String arg) {
        Rede.CANAL.send(new Rede.BatAcao(dados.pos(), tipo, arg), PacketDistributor.SERVER.noArg());
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.6f, 0.5f));
    }

    private float tempo(float pt) {
        return minecraft.player == null ? 0 : minecraft.player.tickCount + pt;
    }

    // ---------------------------------------------------------------- moldura

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        super.extractRenderState(g, mx, my, pt);
        float t = tempo(pt);
        float entra = Mth.clamp((t - abriuEm) / 6f, 0, 1);
        int a = (int) (entra * 255);
        // brilho em volta, painel e grade fina
        g.fill(x0 - 3, y0 - 3, x0 + W + 3, y0 + H + 3, ((a / 6) << 24) | 0x39C8F0);
        g.fill(x0, y0, x0 + W, y0 + H, FUNDO);
        for (int x = x0 + 10; x < x0 + W; x += 10) g.fill(x, y0 + 1, x + 1, y0 + H - 1, 0x0A39C8F0);
        for (int y = y0 + 10; y < y0 + H; y += 10) g.fill(x0 + 1, y, x0 + W - 1, y + 1, 0x0A39C8F0);
        borda(g, x0, y0, W, H, CIANO_ESC);
        cantos(g, x0, y0, W, H, CIANO);
        // cabeçalho: morcego, título e o "sistema"
        g.fill(x0 + 1, y0 + 1, x0 + W - 1, y0 + 22, 0xFF0B1320);
        g.fill(x0 + 1, y0 + 22, x0 + W - 1, y0 + 23, CIANO_ESC);
        // emblema: o batarangue sobre uma placa ciano acesa
        g.fill(x0 + 6, y0 + 3, x0 + 26, y0 + 20, 0xFF39C8F0);
        g.fill(x0 + 7, y0 + 4, x0 + 25, y0 + 19, 0xFF8BE4FF);
        g.item(new ItemStack(com.danrod.dcuniverse.content.Itens.BATARANGUE_SIMPLES.get()), x0 + 8, y0 + 3);
        g.text(font, Component.translatable("dcuniverse.batcomputador.titulo").withStyle(ChatFormatting.BOLD), x0 + 30, y0 + 8, CIANO, false);
        Component sis = Component.translatable("dcuniverse.batcomputador.sistema");
        g.text(font, sis, x0 + W - 8 - font.width(sis), y0 + 8, SUAVE, false);
        abas(g, mx, my);
        int cx = x0 + 8, cy = y0 + 44, cw = W - 16, ch = H - 60;
        g.fill(cx, cy, cx + cw, cy + ch, 0x60050910);
        borda(g, cx, cy, cw, ch, 0xFF13324A);
        switch (aba) {
            case GADGETS -> gadgets(g, cx, cy, cw, ch, mx, my);
            case FABRICAR -> fabricar(g, cx, cy, cw, ch, mx, my);
            case ANALISE -> analise(g, cx, cy, cw, ch, mx, my, pt);
            case MAPA -> mapa(g, cx, cy, cw, ch, t);
        }
        // linha de varredura e rodapé
        int vy = y0 + 24 + (int) (t * 1.2f % (H - 26));
        g.fill(x0 + 1, vy, x0 + W - 1, vy + 1, 0x1839C8F0);
        String pos = dados.pos().getX() + " " + dados.pos().getY() + " " + dados.pos().getZ();
        g.text(font, Component.translatable("dcuniverse.batcomputador.rodape", pos), x0 + 8, y0 + H - 12, SUAVE, false);
        boolean pisca = ((int) t / 10) % 2 == 0;
        g.fill(x0 + W - 12, y0 + H - 11, x0 + W - 8, y0 + H - 7, pisca ? VERDE : 0xFF1E5A36);
    }

    private static void borda(GuiGraphicsExtractor g, int x, int y, int w, int h, int cor) {
        g.fill(x, y, x + w, y + 1, cor);
        g.fill(x, y + h - 1, x + w, y + h, cor);
        g.fill(x, y, x + 1, y + h, cor);
        g.fill(x + w - 1, y, x + w, y + h, cor);
    }

    private static void cantos(GuiGraphicsExtractor g, int x, int y, int w, int h, int cor) {
        int l = 10;
        g.fill(x - 1, y - 1, x + l, y + 1, cor);
        g.fill(x - 1, y - 1, x + 1, y + l, cor);
        g.fill(x + w - l, y - 1, x + w + 1, y + 1, cor);
        g.fill(x + w - 1, y - 1, x + w + 1, y + l, cor);
        g.fill(x - 1, y + h - 1, x + l, y + h + 1, cor);
        g.fill(x - 1, y + h - l, x + 1, y + h + 1, cor);
        g.fill(x + w - l, y + h - 1, x + w + 1, y + h + 1, cor);
        g.fill(x + w - 1, y + h - l, x + w + 1, y + h + 1, cor);
    }

    private void abas(GuiGraphicsExtractor g, int mx, int my) {
        int w = (W - 16) / Aba.values().length;
        for (Aba b : Aba.values()) {
            int x = x0 + 8 + b.ordinal() * w, y = y0 + 27;
            boolean ativa = b == aba, sobre = sobre(mx, my, x, y, w - 2, 14);
            g.fill(x, y, x + w - 2, y + 14, ativa ? 0xFF103A50 : sobre ? 0xFF0F2433 : 0xFF0A1622);
            borda(g, x, y, w - 2, 14, ativa ? CIANO : CIANO_ESC);
            if (ativa) g.fill(x + 1, y + 12, x + w - 3, y + 13, CIANO);
            Component n = Component.translatable("dcuniverse.batcomputador.aba." + b.name().toLowerCase(Locale.ROOT));
            g.centeredText(font, n, x + (w - 2) / 2, y + 3, ativa ? TEXTO : SUAVE);
        }
    }

    /** Botão do painel; devolve se o mouse está em cima. */
    private boolean botao(GuiGraphicsExtractor g, Component texto, int x, int y, int w, boolean ativo, int mx, int my) {
        boolean sobre = ativo && sobre(mx, my, x, y, w, 14);
        g.fill(x, y, x + w, y + 14, !ativo ? 0xFF101820 : sobre ? 0xFF1C6A88 : 0xFF124A60);
        borda(g, x, y, w, 14, ativo ? CIANO : 0xFF2A3440);
        g.centeredText(font, texto, x + w / 2, y + 3, ativo ? TEXTO : 0xFF56626E);
        return sobre;
    }

    private void paragrafo(GuiGraphicsExtractor g, Component texto, int x, int y, int w, int cor) {
        for (FormattedCharSequence l : font.split(texto, w)) {
            g.text(font, l, x, y, cor, false);
            y += 10;
        }
    }

    // ---------------------------------------------------------------- gadgets

    private void gadgets(GuiGraphicsExtractor g, int x, int y, int w, int h, int mx, int my) {
        Rede.Sync s = EstadoCliente.atual;
        if (!dados.heroi()) {
            g.text(font, Component.translatable("dcuniverse.batcomputador.gadgets.titulo").withStyle(ChatFormatting.BOLD), x + 8, y + 8, CIANO, false);
            paragrafo(g, Component.translatable("dcuniverse.batcomputador.gadgets.sem_heroi"), x + 8, y + 24, w - 16, SUAVE);
            return;
        }
        g.text(font, Component.translatable("dcuniverse.batcomputador.gadgets.cinto").withStyle(ChatFormatting.BOLD), x + 8, y + 8, CIANO, false);
        int ly = y + 22;
        for (Rede.HabInfo hab : s.habilidades()) {
            if (hab.cargasMax() <= 0) continue;
            g.text(font, Component.translatable("habilidade.dcuniverse." + hab.id()), x + 8, ly, TEXTO, false);
            int bx = x + 150, bw = 10;
            for (int i = 0; i < hab.cargasMax(); i++) {
                boolean cheia = i < hab.cargas();
                g.fill(bx + i * (bw + 2), ly, bx + i * (bw + 2) + bw, ly + 7, cheia ? CIANO : 0xFF1A2A36);
            }
            g.text(font, hab.cargas() + "/" + hab.cargasMax(), bx + hab.cargasMax() * (bw + 2) + 4, ly, SUAVE, false);
            ly += 12;
            if (ly > y + h - 46) break;
        }
        // energia
        float frac = s.max() <= 0 ? 0 : s.energia() / s.max();
        g.text(font, Component.translatable("dcuniverse.batcomputador.gadgets.energia", Math.round(s.energia()), Math.round(s.max())), x + 8, y + h - 40, TEXTO, false);
        g.fill(x + 8, y + h - 29, x + w - 8, y + h - 25, 0xFF1A2A36);
        g.fill(x + 8, y + h - 29, x + 8 + (int) ((w - 16) * frac), y + h - 25, CIANO);
        boolean pronto = recargaFalta() <= 0;
        Component rot = pronto ? Component.translatable("dcuniverse.batcomputador.gadgets.recarregar")
            : Component.translatable("dcuniverse.batcomputador.gadgets.aguarde", recargaFalta());
        botao(g, rot, x + w / 2 - 80, y + h - 20, 160, pronto, mx, my);
    }

    // ---------------------------------------------------------------- fabricar

    private static final int LINHA = 24;

    private int linhasVisiveis(int h) {
        return (h - 6) / LINHA;
    }

    private void fabricar(GuiGraphicsExtractor g, int x, int y, int w, int h, int mx, int my) {
        List<Rede.BatFab> fab = dados.fabricaveis();
        int vis = linhasVisiveis(h);
        rolagem = Mth.clamp(rolagem, 0, Math.max(0, fab.size() - vis));
        for (int i = 0; i < vis && i + rolagem < fab.size(); i++) {
            Rede.BatFab f = fab.get(i + rolagem);
            int ly = y + 4 + i * LINHA;
            g.fill(x + 4, ly, x + w - 4, ly + LINHA - 2, i % 2 == 0 ? 0x30163048 : 0x18163048);
            ItemStack res = stack(f.item(), f.qtd());
            g.item(res, x + 8, ly + 3);
            if (res.getCount() > 1) g.itemDecorations(font, res, x + 8, ly + 3);
            g.text(font, res.getHoverName(), x + 30, ly + 3, f.pode() ? TEXTO : SUAVE, false);
            int cx = x + 30;
            for (Rede.BatCusto c : f.custos()) {
                ItemStack ing = stack(c.item(), 1);
                // ícone pequeno com "tem/precisa" ao lado
                g.item(ing, cx, ly + 11);
                String n = c.tem() + "/" + c.qtd();
                g.text(font, n, cx + 17, ly + 15, c.tem() >= c.qtd() ? VERDE : VERMELHO, false);
                if (sobre(mx, my, cx, ly + 11, 16, 12)) g.setTooltipForNextFrame(ing.getHoverName(), mx, my);
                cx += 22 + font.width(n);
                if (cx > x + w - 90) break;
            }
            botao(g, Component.translatable("dcuniverse.batcomputador.fabricar.botao"), x + w - 74, ly + 4, 66, f.pode(), mx, my);
            if (sobre(mx, my, x + 8, ly + 3, 16, 16)) g.setTooltipForNextFrame(res.getHoverName(), mx, my);
        }
        if (fab.size() > vis) {
            int barra = Math.max(10, (h - 8) * vis / fab.size());
            int by = y + 4 + (h - 8 - barra) * rolagem / Math.max(1, fab.size() - vis);
            g.fill(x + w - 3, by, x + w - 1, by + barra, CIANO_ESC);
        }
    }

    private static ItemStack stack(String id, int qtd) {
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.tryParse(id));
        return new ItemStack(item, qtd);
    }

    // ---------------------------------------------------------------- análise

    private List<Map.Entry<Item, EntityType<?>>> amostras() {
        return new ArrayList<>(Batcomputador.amostras().entrySet());
    }

    private int temNoInventario(Item item) {
        if (minecraft.player == null) return 0;
        var inv = minecraft.player.getInventory();
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(item)) n += inv.getItem(i).getCount();
        return n;
    }

    private boolean analisado(EntityType<?> t) {
        return dados.analisados().contains(Batcomputador.id(t));
    }

    private void analise(GuiGraphicsExtractor g, int x, int y, int w, int h, int mx, int my, float pt) {
        var lista = amostras();
        amostra = Mth.clamp(amostra, 0, lista.size() - 1);
        g.text(font, Component.translatable("dcuniverse.batcomputador.analise.amostras", dados.analisados().size(), lista.size()), x + 6, y + 5, CIANO, false);
        for (int i = 0; i < lista.size(); i++) {
            int sx = x + 6 + (i % 5) * 20, sy = y + 17 + (i / 5) * 20;
            var en = lista.get(i);
            boolean feito = analisado(en.getValue()), tem = temNoInventario(en.getKey()) > 0;
            g.fill(sx, sy, sx + 18, sy + 18, i == amostra ? 0xFF16506A : 0xFF0C1824);
            borda(g, sx, sy, 18, 18, feito ? OURO : i == amostra ? CIANO : 0xFF1E3446);
            g.item(new ItemStack(en.getKey()), sx + 1, sy + 1);
            if (!feito && !tem) g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xA0080C14);
            if (feito) g.fill(sx + 13, sy + 2, sx + 16, sy + 5, OURO);
            if (sobre(mx, my, sx, sy, 18, 18)) {
                g.setTooltipForNextFrame(Component.translatable("dcuniverse.batcomputador.analise.amostra_de",
                    new ItemStack(en.getKey()).getHoverName(), en.getValue().getDescription()), mx, my);
            }
        }
        // dossiê do selecionado
        var sel = lista.get(amostra);
        EntityType<?> tipo = sel.getValue();
        boolean feito = analisado(tipo);
        int dx = x + 108, dw = w - 112;
        g.fill(dx, y + 16, dx + dw, y + h - 4, 0x40102434);
        borda(g, dx, y + 16, dw, h - 20, feito ? 0xFF5A4A20 : 0xFF13324A);
        int px0 = dx + 4, py0 = y + 20, px1 = dx + 70, py1 = y + h - 8;
        if (feito) {
            LivingEntity m = modelo(tipo);
            if (m != null) {
                float alto = Math.max(m.getBbHeight(), m.getBbWidth());
                int tam = (int) Mth.clamp(48 / alto, 8, 34);
                InventoryScreen.extractEntityInInventoryFollowsMouse(g, px0, py0, px1, py1, tam, 0.0625f, mx, my, m);
            }
        } else {
            g.centeredText(font, Component.literal("?").withStyle(ChatFormatting.BOLD), (px0 + px1) / 2, (py0 + py1) / 2 - 4, CIANO_ESC);
        }
        int tx = dx + 74, tw = dw - 78, ty = y + 20;
        g.text(font, feito ? tipo.getDescription().copy().withStyle(ChatFormatting.BOLD) : Component.literal("???"), tx, ty, feito ? OURO : SUAVE, false);
        ty += 12;
        if (feito) {
            AttributeSupplier atr = atributos(tipo);
            if (atr != null) {
                ty = estat(g, "vida", fmt(atr.getValue(Attributes.MAX_HEALTH)), tx, ty);
                if (atr.hasAttribute(Attributes.ATTACK_DAMAGE)) ty = estat(g, "ataque", fmt(atr.getValue(Attributes.ATTACK_DAMAGE)), tx, ty);
                ty = estat(g, "armadura", fmt(atr.getValue(Attributes.ARMOR)), tx, ty);
                ty = estat(g, "velocidade", fmt(atr.getValue(Attributes.MOVEMENT_SPEED) * 10), tx, ty);
            }
            g.text(font, Component.translatable("dcuniverse.batcomputador.analise.bonus", Batcomputador.BONUS_PCT), tx, ty, VERDE, false);
            ty += 12;
            paragrafo(g, Component.translatable("dcuniverse.batcomputador.dossie." + BuiltInRegistries.ENTITY_TYPE.getKey(tipo).getPath()), tx, ty, tw, TEXTO);
        } else {
            paragrafo(g, Component.translatable("dcuniverse.batcomputador.analise.falta", new ItemStack(sel.getKey()).getHoverName()), tx, ty, tw, SUAVE);
            boolean pode = temNoInventario(sel.getKey()) > 0 || (minecraft.player != null && minecraft.player.getAbilities().instabuild);
            botao(g, Component.translatable("dcuniverse.batcomputador.analise.botao"), tx, y + h - 24, Math.min(110, tw), pode, mx, my);
        }
    }

    private int estat(GuiGraphicsExtractor g, String chave, String valor, int x, int y) {
        g.text(font, Component.translatable("dcuniverse.batcomputador.analise." + chave), x, y, SUAVE, false);
        g.text(font, valor, x + 62, y, TEXTO, false);
        return y + 10;
    }

    private static String fmt(double v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.format(Locale.ROOT, "%.1f", v);
    }

    @SuppressWarnings("unchecked")
    private static AttributeSupplier atributos(EntityType<?> tipo) {
        if (!DefaultAttributes.hasSupplier(tipo)) return null;
        return DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) tipo);
    }

    private LivingEntity modelo(EntityType<?> tipo) {
        if (minecraft.level == null) return null;
        return modelos.computeIfAbsent(tipo, t -> {
            Entity e = t.create(minecraft.level, EntitySpawnReason.LOAD);
            if (!(e instanceof LivingEntity l)) return null;
            // id próprio (negativo, fora do mundo): o desenho do modelo lê o id da entidade
            l.setId(-1_000_000 - modelos.size());
            return l;
        });
    }

    // ---------------------------------------------------------------- mapa

    private void mapa(GuiGraphicsExtractor g, int x, int y, int w, int h, float t) {
        int r = Batcomputador.MAPA_RAIO, lado = 2 * r + 1, px = 2;
        int mx0 = x + 6, my0 = y + (h - lado * px) / 2;
        if (minecraft.level == null) return;
        long agora = minecraft.level.getGameTime();
        if (mapa == null || agora - mapaEm > 40) {
            mapa = gerarMapa(dados.pos(), r);
            mapaEm = agora;
        }
        // o terreno, em faixas da mesma cor para desenhar menos retângulos
        for (int z = 0; z < lado; z++) {
            int ini = 0;
            for (int xx = 1; xx <= lado; xx++) {
                if (xx == lado || mapa[z * lado + xx] != mapa[z * lado + ini]) {
                    int c = mapa[z * lado + ini];
                    if (c != 0) g.fill(mx0 + ini * px, my0 + z * px, mx0 + xx * px, my0 + (z + 1) * px, c);
                    ini = xx;
                }
            }
        }
        // grade de 8 em 8 blocos e anéis de distância, como numa tela de vigilância
        for (int i = 0; i <= lado; i += 8) {
            g.fill(mx0 + i * px, my0, mx0 + i * px + 1, my0 + lado * px, 0x2839C8F0);
            g.fill(mx0, my0 + i * px, mx0 + lado * px, my0 + i * px + 1, 0x2839C8F0);
        }
        for (int anel = 12; anel <= r; anel += 12) {
            for (int k = 0; k < 96; k++) {
                double an = k * Math.PI * 2 / 96;
                int ax = mx0 + r * px + 1 + (int) (Math.cos(an) * anel * px), ay = my0 + r * px + 1 + (int) (Math.sin(an) * anel * px);
                g.fill(ax, ay, ax + 1, ay + 1, 0x5039C8F0);
            }
        }
        borda(g, mx0 - 1, my0 - 1, lado * px + 2, lado * px + 2, CIANO_ESC);
        // varredura de radar girando a partir do Batcomputador
        int cx = mx0 + r * px + 1, cy = my0 + r * px + 1;
        float ang = t * 0.08f;
        for (int i = 4; i < r * px; i += 2) {
            g.fill(cx + (int) (Math.cos(ang) * i), cy + (int) (Math.sin(ang) * i), cx + (int) (Math.cos(ang) * i) + 1,
                cy + (int) (Math.sin(ang) * i) + 1, 0x8039C8F0);
        }
        g.fill(cx - 2, cy - 2, cx + 2, cy + 2, CIANO);
        int ameacas = 0, jogadores = 0;
        BlockPos c0 = dados.pos();
        for (Entity e : minecraft.level.entitiesForRendering()) {
            double dx = e.getX() - (c0.getX() + 0.5), dz = e.getZ() - (c0.getZ() + 0.5);
            if (Math.abs(dx) > r || Math.abs(dz) > r || !(e instanceof LivingEntity)) continue;
            int ex = cx + (int) (dx * px), ez = cy + (int) (dz * px);
            int cor;
            if (e == minecraft.player) {
                cor = 0xFFFFFFFF;
                float yaw = e.getYRot() * Mth.DEG_TO_RAD;
                for (int i = 2; i < 7; i++) g.fill(ex - (int) (Mth.sin(yaw) * i), ez + (int) (Mth.cos(yaw) * i),
                    ex - (int) (Mth.sin(yaw) * i) + 1, ez + (int) (Mth.cos(yaw) * i) + 1, 0xFFFFFFFF);
            } else if (e instanceof Player) {
                cor = 0xFFFFA640;
                jogadores++;
            } else if (e instanceof Enemy) {
                cor = analisado(Batcomputador.familia(e.getType())) ? OURO : VERMELHO;
                ameacas++;
            } else {
                cor = 0xFF7CE07C;
            }
            g.fill(ex - 2, ez - 2, ex + 3, ez + 3, 0xFF000000);
            g.fill(ex - 1, ez - 1, ex + 2, ez + 2, cor);
        }
        // legenda
        int lx = mx0 + lado * px + 12, ly = y + 8;
        g.text(font, Component.translatable("dcuniverse.batcomputador.mapa.titulo").withStyle(ChatFormatting.BOLD), lx, ly, CIANO, false);
        ly += 14;
        ly = legenda(g, lx, ly, CIANO, Component.translatable("dcuniverse.batcomputador.mapa.base"));
        ly = legenda(g, lx, ly, 0xFFFFFFFF, Component.translatable("dcuniverse.batcomputador.mapa.voce"));
        ly = legenda(g, lx, ly, 0xFFFFA640, Component.translatable("dcuniverse.batcomputador.mapa.jogadores", jogadores));
        ly = legenda(g, lx, ly, VERMELHO, Component.translatable("dcuniverse.batcomputador.mapa.ameacas", ameacas));
        ly = legenda(g, lx, ly, OURO, Component.translatable("dcuniverse.batcomputador.mapa.analisados"));
        ly = legenda(g, lx, ly, 0xFF7CE07C, Component.translatable("dcuniverse.batcomputador.mapa.outros"));
        paragrafo(g, Component.translatable("dcuniverse.batcomputador.mapa.alcance", r), lx, ly + 6, x + w - lx - 6, SUAVE);
    }

    private int legenda(GuiGraphicsExtractor g, int x, int y, int cor, Component texto) {
        g.fill(x, y + 2, x + 4, y + 6, cor);
        g.text(font, texto, x + 8, y, TEXTO, false);
        return y + 11;
    }

    /** Cor (ARGB) do topo de cada coluna em volta, com sombra pela diferença de altura (como os mapas do jogo). */
    private int[] gerarMapa(BlockPos c, int r) {
        var level = minecraft.level;
        int lado = 2 * r + 1;
        int[] out = new int[lado * lado];
        int[] alt = new int[lado * lado];
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int z = 0; z < lado; z++) {
            for (int x = 0; x < lado; x++) {
                int wx = c.getX() - r + x, wz = c.getZ() - r + z;
                if (!level.hasChunk(wx >> 4, wz >> 4)) continue;
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz) - 1;
                MapColor cor = MapColor.NONE;
                for (int k = 0; k < 6 && cor == MapColor.NONE && y > level.getMinY(); k++, y--) {
                    p.set(wx, y, wz);
                    cor = level.getBlockState(p).getMapColor(level, p);
                }
                alt[z * lado + x] = y;
                if (cor == MapColor.NONE) continue;
                int norte = z > 0 ? alt[(z - 1) * lado + x] : y;
                MapColor.Brightness b = y > norte ? MapColor.Brightness.HIGH : y < norte ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
                int argb = cor.calculateARGBColor(b);
                // tom frio de tela: escurece e puxa para o azul
                int rr = (argb >> 16 & 255) * 9 / 20 + 6, gg = (argb >> 8 & 255) * 9 / 20 + 16, bb = (argb & 255) * 9 / 20 + 36;
                out[z * lado + x] = 0xFF000000 | rr << 16 | gg << 8 | bb;
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- mouse

    private static boolean sobre(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean duplo) {
        if (e.button() != 0) return super.mouseClicked(e, duplo);
        double mx = e.x(), my = e.y();
        int w = (W - 16) / Aba.values().length;
        for (Aba b : Aba.values()) {
            if (sobre(mx, my, x0 + 8 + b.ordinal() * w, y0 + 27, w - 2, 14)) {
                if (aba != b) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.4f, 0.4f));
                aba = b;
                rolagem = 0;
                return true;
            }
        }
        int x = x0 + 8, y = y0 + 44, cw = W - 16, ch = H - 60;
        switch (aba) {
            case GADGETS -> {
                if (dados.heroi() && recargaFalta() <= 0 && sobre(mx, my, x + cw / 2 - 80, y + ch - 20, 160, 14)) {
                    enviar(Batcomputador.RECARREGAR, "");
                    return true;
                }
            }
            case FABRICAR -> {
                List<Rede.BatFab> fab = dados.fabricaveis();
                for (int i = 0; i < linhasVisiveis(ch) && i + rolagem < fab.size(); i++) {
                    Rede.BatFab f = fab.get(i + rolagem);
                    if (f.pode() && sobre(mx, my, x + cw - 74, y + 4 + i * LINHA + 4, 66, 14)) {
                        enviar(Batcomputador.FABRICAR, f.receita());
                        return true;
                    }
                }
            }
            case ANALISE -> {
                var lista = amostras();
                for (int i = 0; i < lista.size(); i++) {
                    if (sobre(mx, my, x + 6 + (i % 5) * 20, y + 17 + (i / 5) * 20, 18, 18)) {
                        amostra = i;
                        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.8f, 0.3f));
                        return true;
                    }
                }
                var sel = lista.get(Mth.clamp(amostra, 0, lista.size() - 1));
                int dx = x + 108, tx = dx + 74, tw = cw - 112 - 78;
                if (!analisado(sel.getValue()) && sobre(mx, my, tx, y + ch - 24, Math.min(110, tw), 14)) {
                    enviar(Batcomputador.ANALISAR, BuiltInRegistries.ITEM.getKey(sel.getKey()).toString());
                    return true;
                }
            }
            default -> { }
        }
        return super.mouseClicked(e, duplo);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        if (aba == Aba.FABRICAR && sy != 0) {
            rolagem += sy < 0 ? 1 : -1;
            return true;
        }
        return super.mouseScrolled(x, y, sx, sy);
    }
}
