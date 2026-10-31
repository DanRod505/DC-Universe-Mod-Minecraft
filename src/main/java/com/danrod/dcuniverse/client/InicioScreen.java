package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.client.guia.GuiaScreen;
import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.inicio.Inicio;
import com.danrod.dcuniverse.world.mundo.MapaMundo;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Tela do início guiado e do mapa do mundo (tecla M). À esquerda, o mapa dos biomas com os locais (os prontos em
 * vermelho, os reservados para versões futuras em cinza) e o jogador; roda do mouse aproxima e arrastar move. No
 * início guiado, à direita: onde começar (um local pronto ou o ponto de nascimento) e o personagem inicial, em 3D.
 */
public final class InicioScreen extends Screen {
    private static final Identifier TEXTURA = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "dynamic/mapa_mundo");
    private static final int OURO = 0xFFE8C468, TEXTO = 0xFFEDE6D6, SUAVE = 0xFF9C9584, FUNDO = 0xE8141821, FUNDO2 = 0xE81C2230,
        BORDA = 0xFF8A6B2E, LINHA = 0x40E8C468, SOBRE = 0xC02A3448, ESCOLHIDO = 0xE0384660, VERMELHO = 0xFFDC1E1E, CINZA = 0xFFB4B4B4;
    /** Menor trecho do mundo que cabe no mapa com o zoom todo (blocos). */
    private static final int ZOOM_MAX = 700;

    /** O mapa que está na textura agora (a textura só é refeita quando chega um mapa diferente). */
    private static MapaMundo.Mapa naTextura;

    private final Rede.InicioAbrir m;
    private final List<Personagem> personagens = new ArrayList<>();
    private final List<Rede.Marca> prontos = new ArrayList<>();
    private String local = Inicio.NASCIMENTO;
    private int escolhido;

    // Mapa na tela (quadrado de lado lado em mx0, my0) e a vista: centro e quantos blocos cabem no lado.
    private int mx0, my0, lado, cx0, cw, topo;
    private double vcx, vcz, vlado;
    private boolean arrastou, arrastando;
    private Button comecar;

    private InicioScreen(Rede.InicioAbrir m) {
        super(Component.translatable(m.escolher() ? "dcuniverse.inicio.titulo" : "dcuniverse.mapa.titulo"));
        this.m = m;
        for (Rede.Marca k : m.marcas()) if (k.pronto()) prontos.add(k);
        if (!prontos.isEmpty()) local = prontos.getFirst().local();
        for (Rede.Inicial i : m.personagens()) {
            String json = EstadoCliente.catalogo.get(i.personagem());
            if (json == null) continue;
            try {
                personagens.add(Personagem.parse(Identifier.parse(i.personagem()), JsonParser.parseString(json).getAsJsonObject()));
            } catch (RuntimeException e) {
                DCUniverse.LOGGER.warn("Início: personagem {} ignorado: {}", i.personagem(), e.toString());
            }
        }
        var a = m.mapa();
        vcx = a.x0() + a.w() * a.passo() / 2.0;
        vcz = a.z0() + a.h() * a.passo() / 2.0;
        vlado = Math.max(a.w(), a.h()) * a.passo();
    }

    /** O servidor mandou o mapa (pacote InicioAbrir). */
    public static void receber(Rede.InicioAbrir m) {
        Minecraft mc = Minecraft.getInstance();
        if (!m.escolher() && mc.gui.screen() != null) return;
        if (naTextura != m.mapa()) {
            var a = m.mapa();
            NativeImage img = new NativeImage(a.w(), a.h(), false);
            for (int j = 0; j < a.h(); j++)
                for (int i = 0; i < a.w(); i++) img.setPixel(i, j, 0xFF000000 | a.paleta()[a.pixels()[j * a.w() + i] & 255]);
            mc.getTextureManager().register(TEXTURA, new DynamicTexture(() -> "dcuniverse mapa do mundo", img));
            naTextura = m.mapa();
        }
        mc.gui.setScreen(new InicioScreen(m));
    }

    @Override
    protected void init() {
        int margem = 8;
        topo = margem + 28;
        if (m.escolher()) {
            lado = Math.max(80, Math.min(height - topo - margem - 8, (int) ((width - 2 * margem) * 0.56)));
            mx0 = margem + 8;
            cx0 = mx0 + lado + 12;
            cw = width - margem - 8 - cx0;
            comecar = addRenderableWidget(Button.builder(Component.translatable("dcuniverse.inicio.comecar").withStyle(ChatFormatting.BOLD), b -> comecar())
                .bounds(cx0, height - margem - 28, cw, 20).build());
        } else {
            lado = Math.max(80, Math.min(height - topo - margem - 20, width - 2 * margem - 16));
            mx0 = (width - lado) / 2;
        }
        my0 = topo;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        // No início guiado é preciso escolher (o "Onde estou agora" serve para quem não quer sair do lugar).
        return !m.escolher();
    }

    // ---------------------------------------------------------------- desenho

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float a) {
        int px0 = 8, py0 = 8, px1 = width - 8, py1 = height - 8;
        if (!m.escolher()) {
            px0 = mx0 - 8;
            px1 = mx0 + lado + 8;
        }
        g.fill(px0, py0, px1, py1, FUNDO);
        g.fill(px0 + 1, py0 + 1, px1 - 1, py0 + 26, FUNDO2);
        moldura(g, px0, py0, px1 - px0, py1 - py0, BORDA);
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), px0 + 8, py0 + 6, OURO);
        Component sub = m.escolher() ? Component.translatable("dcuniverse.inicio.subtitulo") : Component.translatable("dcuniverse.mapa.dica");
        g.text(font, sub, px0 + 8, py0 + 16, SUAVE);

        Rede.Marca sobreMarca = mapa(g, mx, my);
        if (m.escolher()) coluna(g, mx, my);
        else legenda(g);
        super.extractRenderState(g, mx, my, a);
        if (sobreMarca != null) {
            List<FormattedCharSequence> dica = new ArrayList<>();
            dica.add(Component.translatable("dcuniverse.local." + sobreMarca.local()).withStyle(ChatFormatting.GOLD).getVisualOrderText());
            String chave = !sobreMarca.pronto() ? "dcuniverse.mapa.reservado" : m.escolher() ? "dcuniverse.mapa.comecar_aqui" : "dcuniverse.mapa.pronto";
            dica.add(Component.translatable(chave).withStyle(ChatFormatting.GRAY).getVisualOrderText());
            g.setTooltipForNextFrame(dica, mx, my);
        }
    }

    /** O mapa com moldura, os locais e o jogador; devolve a marca debaixo do mouse. */
    private Rede.Marca mapa(GuiGraphicsExtractor g, int mx, int my) {
        var a = m.mapa();
        g.fill(mx0 - 3, my0 - 3, mx0 + lado + 3, my0 + lado + 3, 0xFF07090D);
        g.fill(mx0, my0, mx0 + lado, my0 + lado, 0xFF0B1B4A);
        g.enableScissor(mx0, my0, mx0 + lado, my0 + lado);
        // Só o pedaço da textura que aparece na vista.
        double ax1 = a.x0() + (double) a.w() * a.passo(), az1 = a.z0() + (double) a.h() * a.passo();
        double wx0 = Math.max(vcx - vlado / 2, a.x0()), wx1 = Math.min(vcx + vlado / 2, ax1);
        double wz0 = Math.max(vcz - vlado / 2, a.z0()), wz1 = Math.min(vcz + vlado / 2, az1);
        if (wx1 > wx0 && wz1 > wz0) {
            g.blit(TEXTURA, telaX(wx0), telaZ(wz0), telaX(wx1), telaZ(wz1),
                (float) ((wx0 - a.x0()) / (ax1 - a.x0())), (float) ((wx1 - a.x0()) / (ax1 - a.x0())),
                (float) ((wz0 - a.z0()) / (az1 - a.z0())), (float) ((wz1 - a.z0()) / (az1 - a.z0())));
        }
        Rede.Marca sobre = null;
        double melhor = 7 * 7;
        for (Rede.Marca k : m.marcas()) {
            double dx = telaXd(k.x()) - mx, dz = telaZd(k.z()) - my;
            if (dx * dx + dz * dz <= melhor && dentroMapa(mx, my)) {
                melhor = dx * dx + dz * dz;
                sobre = k;
            }
        }
        // Reservados primeiro (embaixo), depois os prontos, o escolhido e o jogador por cima.
        for (Rede.Marca k : m.marcas()) {
            if (k.pronto()) continue;
            int x = telaX(k.x()), z = telaZ(k.z());
            disco(g, x, z, 3, 0xFF202020);
            disco(g, x, z, 2, k == sobre ? TEXTO : CINZA);
            nome(g, Component.translatable("dcuniverse.local." + k.local()), x, z, 0xFFD0D0D0, 0.75f);
        }
        long t = System.currentTimeMillis();
        // Jogador: seta branca pulsando de leve. Em cima de um local (no Mundo DC se nasce em Smallville), vira um
        // anel branco em volta da marca do local, que fica por cima.
        int jx = telaX(m.jx()), jz = telaZ(m.jz());
        boolean noLocal = false;
        for (Rede.Marca k : prontos) noLocal |= Math.abs(telaX(k.x()) - jx) <= 6 && Math.abs(telaZ(k.z()) - jz) <= 6;
        if (m.escolher() && local.equals(Inicio.NASCIMENTO)) anel(g, jx, jz, 9 + (int) ((t / 120) % 4), OURO);
        int corJogador = (t / 400) % 2 == 0 ? 0xFFFFFFFF : 0xFFFFF2B0;
        if (noLocal) {
            anel(g, jx, jz, 7, corJogador);
            anel(g, jx, jz, 8, 0xFF101010);
        } else {
            seta(g, jx, jz, corJogador);
            Component voce = Component.translatable("dcuniverse.mapa.voce");
            nome(g, voce, jx - 7 - (int) (font.width(voce) * 0.375f), jz + 10, 0xFFFFF2B0, 0.75f);
        }
        for (Rede.Marca k : prontos) {
            int x = telaX(k.x()), z = telaZ(k.z());
            if (m.escolher() && k.local().equals(local)) anel(g, x, z, 10 + (int) ((t / 120) % 4), OURO);
            disco(g, x, z, 5, 0xFFFFFFFF);
            disco(g, x, z, 4, k == sobre ? 0xFFFF5050 : VERMELHO);
            disco(g, x, z, 1, 0xFFFFFFFF);
            nome(g, Component.translatable("dcuniverse.local." + k.local()).withStyle(ChatFormatting.BOLD), x + 2, z, 0xFFFFFFFF, 1f);
        }
        g.disableScissor();
        moldura(g, mx0 - 1, my0 - 1, lado + 2, lado + 2, BORDA);
        moldura(g, mx0 - 2, my0 - 2, lado + 4, lado + 4, 0xFF4A3A18);
        return sobre;
    }

    /** Coluna da direita do início guiado: onde começar e o personagem. */
    private void coluna(GuiGraphicsExtractor g, int mx, int my) {
        int y = topo;
        g.text(font, Component.translatable("dcuniverse.inicio.onde").withStyle(ChatFormatting.BOLD), cx0, y, OURO);
        y += 12;
        for (Rede.Marca k : prontos) {
            y = opcaoLocal(g, mx, my, y, k.local(), Component.translatable("dcuniverse.local." + k.local()),
                Component.translatable("dcuniverse.local." + k.local() + ".sub"));
        }
        y = opcaoLocal(g, mx, my, y, Inicio.NASCIMENTO, Component.translatable("dcuniverse.inicio.aqui"), Component.translatable("dcuniverse.inicio.aqui.desc"));
        if (personagens.isEmpty()) return;
        y += 6;
        g.fill(cx0, y, cx0 + cw, y + 1, LINHA);
        y += 6;
        g.text(font, Component.translatable("dcuniverse.inicio.com_quem").withStyle(ChatFormatting.BOLD), cx0, y, OURO);
        y += 12;
        Personagem p = personagens.get(escolhido);
        // Prévia 3D do tamanho que sobrar entre o título e os ícones.
        int fim = height - 8 - 28 - 6;
        int icones = fim - 20, nomeY = icones - 24;
        int previaH = nomeY - y - 4;
        if (previaH >= 36) {
            g.fill(cx0, y, cx0 + cw, y + previaH, 0x60000000);
            moldura(g, cx0, y, cw, previaH, 0x406A5A30);
            PlayerSkin skin = Skins.skinDoPersonagem(p.id().toString());
            float giro = 25f + (float) (System.currentTimeMillis() % 14400L) / 40f;
            if (skin != null) GuiaScreen.previa(minecraft, g, skin, cx0 + 2, y + 2, cx0 + cw - 2, y + previaH - 2, giro, 0f, previaH / 2.4f);
            if (sobre(mx, my, cx0, y, cw, previaH)) {
                var linhas = font.split(Component.translatable("personagem." + p.id().getNamespace() + "." + p.id().getPath() + ".exp"), 200);
                g.setTooltipForNextFrame(linhas, mx, my);
            }
            setas(g, mx, my, y, previaH);
        } else nomeY = y;
        g.centeredText(font, Component.translatable(p.chaveNome()).withStyle(ChatFormatting.BOLD), cx0 + cw / 2, nomeY + 2, TEXTO);
        ItemStack item = item(escolhido);
        g.centeredText(font, Component.translatable("dcuniverse.guia.personagem.nivel", p.nivel()).append(" • ").append(item.getHoverName()),
            cx0 + cw / 2, nomeY + 13, SUAVE);
        // Ícones dos itens de transformação para trocar rápido.
        int n = personagens.size(), ix = cx0 + (cw - n * 22) / 2;
        for (int i = 0; i < n; i++) {
            int x = ix + i * 22;
            boolean s = sobre(mx, my, x, icones, 20, 20);
            g.fill(x, icones, x + 20, icones + 20, i == escolhido ? ESCOLHIDO : s ? SOBRE : 0x80000000);
            moldura(g, x, icones, 20, 20, i == escolhido ? OURO : BORDA);
            g.item(item(i), x + 2, icones + 2);
            if (s && i != escolhido) g.setTooltipForNextFrame(Component.translatable(personagens.get(i).chaveNome()), mx, my);
        }
    }

    private int opcaoLocal(GuiGraphicsExtractor g, int mx, int my, int y, String id, Component nome, Component desc) {
        boolean s = sobre(mx, my, cx0, y, cw, 22), esc = local.equals(id);
        g.fill(cx0, y, cx0 + cw, y + 22, esc ? ESCOLHIDO : s ? SOBRE : 0x50000000);
        g.fill(cx0, y, cx0 + 2, y + 22, esc ? OURO : s ? BORDA : 0xFF3A3A3A);
        g.text(font, nome.copy().withStyle(ChatFormatting.BOLD), cx0 + 7, y + 3, esc ? OURO : TEXTO);
        g.text(font, font.substrByWidth(desc, cw - 12).getString(), cx0 + 7, y + 12, SUAVE);
        return y + 24;
    }

    private void setas(GuiGraphicsExtractor g, int mx, int my, int y, int h) {
        if (personagens.size() < 2) return;
        int sy = y + h / 2 - 8;
        for (int lado = 0; lado < 2; lado++) {
            int x = lado == 0 ? cx0 + 3 : cx0 + cw - 15;
            boolean s = sobre(mx, my, x, sy, 12, 16);
            g.fill(x, sy, x + 12, sy + 16, s ? SOBRE : 0x80000000);
            g.centeredText(font, lado == 0 ? "◀" : "▶", x + 6, sy + 4, s ? OURO : TEXTO);
        }
    }

    private void legenda(GuiGraphicsExtractor g) {
        int y = my0 + lado + 6, x = mx0;
        disco(g, x + 4, y + 4, 4, VERMELHO);
        g.text(font, Component.translatable("dcuniverse.mapa.legenda.pronto"), x + 11, y, TEXTO);
        x += 16 + font.width(Component.translatable("dcuniverse.mapa.legenda.pronto"));
        disco(g, x + 4, y + 4, 2, CINZA);
        g.text(font, Component.translatable("dcuniverse.mapa.legenda.reservado"), x + 11, y, TEXTO);
        x += 16 + font.width(Component.translatable("dcuniverse.mapa.legenda.reservado"));
        seta(g, x + 4, y + 4, 0xFFFFFFFF);
        g.text(font, Component.translatable("dcuniverse.mapa.voce"), x + 11, y, TEXTO);
    }

    private ItemStack item(int i) {
        String id = m.personagens().stream().filter(o -> o.personagem().equals(personagens.get(i).id().toString())).map(Rede.Inicial::item).findFirst().orElse("");
        return id.isEmpty() ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)));
    }

    // ---------------------------------------------------------------- formas

    private void nome(GuiGraphicsExtractor g, Component texto, int x, int z, int cor, float escala) {
        // À direita do ponto; perto da borda direita do mapa, à esquerda.
        float largura = font.width(texto) * escala;
        float tx = x + 7 + largura > mx0 + lado - 2 ? x - 7 - largura : x + 7;
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(tx, z - 4 * escala);
        pose.scale(escala, escala);
        // Contorno preto (como no mapa impresso) para ler em cima de qualquer bioma.
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                if (dx != 0 || dz != 0) g.text(font, texto, dx, dz, 0xFF000000, false);
        g.text(font, texto, 0, 0, cor, false);
        pose.popMatrix();
    }

    private static void disco(GuiGraphicsExtractor g, int cx, int cy, int r, int cor) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.round(Math.sqrt(r * r + r - dy * dy));
            g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, cor);
        }
    }

    private static void anel(GuiGraphicsExtractor g, int cx, int cy, int r, int cor) {
        for (int i = 0; i < 48; i++) {
            double ang = i * Math.PI * 2 / 48;
            int x = cx + (int) Math.round(Math.cos(ang) * r), y = cy + (int) Math.round(Math.sin(ang) * r);
            g.fill(x, y, x + 1, y + 1, cor);
        }
    }

    /** Seta para cima com contorno escuro (o jogador). */
    private static void seta(GuiGraphicsExtractor g, int cx, int cy, int cor) {
        for (int dy = -5; dy <= 4; dy++) {
            int meia = (dy + 6) / 2;
            g.fill(cx - meia - 1, cy + dy, cx + meia + 2, cy + dy + 1, 0xFF101010);
        }
        for (int dy = -4; dy <= 3; dy++) {
            int meia = (dy + 5) / 2;
            g.fill(cx - meia, cy + dy, cx + meia + 1, cy + dy + 1, cor);
        }
    }

    private static void moldura(GuiGraphicsExtractor g, int x, int y, int w, int h, int cor) {
        g.fill(x, y, x + w, y + 1, cor);
        g.fill(x, y + h - 1, x + w, y + h, cor);
        g.fill(x, y, x + 1, y + h, cor);
        g.fill(x + w - 1, y, x + w, y + h, cor);
    }

    // ---------------------------------------------------------------- vista do mapa

    private double telaXd(double wx) {
        return mx0 + (wx - (vcx - vlado / 2)) * lado / vlado;
    }

    private double telaZd(double wz) {
        return my0 + (wz - (vcz - vlado / 2)) * lado / vlado;
    }

    private int telaX(double wx) {
        return (int) Math.round(telaXd(wx));
    }

    private int telaZ(double wz) {
        return (int) Math.round(telaZd(wz));
    }

    private boolean dentroMapa(double x, double y) {
        return sobre(x, y, mx0, my0, lado, lado);
    }

    /** Mantém a vista dentro do mapa. */
    private void prender() {
        var a = m.mapa();
        double tam = Math.max(a.w(), a.h()) * (double) a.passo();
        vlado = Mth.clamp(vlado, Math.min(ZOOM_MAX, tam), tam);
        double ax1 = a.x0() + (double) a.w() * a.passo(), az1 = a.z0() + (double) a.h() * a.passo();
        vcx = ax1 - a.x0() <= vlado ? (a.x0() + ax1) / 2 : Mth.clamp(vcx, a.x0() + vlado / 2, ax1 - vlado / 2);
        vcz = az1 - a.z0() <= vlado ? (a.z0() + az1) / 2 : Mth.clamp(vcz, a.z0() + vlado / 2, az1 - vlado / 2);
    }

    // ---------------------------------------------------------------- entrada

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        if (!dentroMapa(x, y) || sy == 0) return super.mouseScrolled(x, y, sx, sy);
        // Aproxima em volta do ponto debaixo do mouse.
        double wx = vcx - vlado / 2 + (x - mx0) * vlado / lado, wz = vcz - vlado / 2 + (y - my0) * vlado / lado;
        double novo = vlado * (sy > 0 ? 0.8 : 1.25);
        vcx = wx + (vcx - wx) * novo / vlado;
        vcz = wz + (vcz - wz) * novo / vlado;
        vlado = novo;
        prender();
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean duplo) {
        if (e.button() == 0 && dentroMapa(e.x(), e.y())) {
            arrastando = true;
            arrastou = false;
            return true;
        }
        if (e.button() == 0 && m.escolher() && clicarColuna(e.x(), e.y())) return true;
        return super.mouseClicked(e, duplo);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (!arrastando) return super.mouseDragged(e, dx, dy);
        if (Math.abs(dx) + Math.abs(dy) > 0) arrastou = true;
        vcx -= dx * vlado / lado;
        vcz -= dy * vlado / lado;
        prender();
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        if (arrastando && e.button() == 0) {
            arrastando = false;
            if (!arrastou && m.escolher()) {
                // Clique sem arrastar num local pronto (ou em você) escolhe onde começar.
                double melhor = 8 * 8;
                String achado = null;
                for (Rede.Marca k : prontos) {
                    double ddx = telaXd(k.x()) - e.x(), ddz = telaZd(k.z()) - e.y();
                    if (ddx * ddx + ddz * ddz <= melhor) {
                        melhor = ddx * ddx + ddz * ddz;
                        achado = k.local();
                    }
                }
                double jdx = telaXd(m.jx()) - e.x(), jdz = telaZd(m.jz()) - e.y();
                if (jdx * jdx + jdz * jdz <= melhor) achado = Inicio.NASCIMENTO;
                if (achado != null) escolherLocal(achado);
            }
            return true;
        }
        return super.mouseReleased(e);
    }

    private boolean clicarColuna(double x, double y) {
        int yy = topo + 12;
        for (Rede.Marca k : prontos) {
            if (sobre(x, y, cx0, yy, cw, 22)) {
                escolherLocal(k.local());
                return true;
            }
            yy += 24;
        }
        if (sobre(x, y, cx0, yy, cw, 22)) {
            escolherLocal(Inicio.NASCIMENTO);
            return true;
        }
        if (personagens.isEmpty()) return false;
        int fim = height - 8 - 28 - 6, icones = fim - 20;
        int n = personagens.size(), ix = cx0 + (cw - n * 22) / 2;
        for (int i = 0; i < n; i++) {
            if (sobre(x, y, ix + i * 22, icones, 20, 20)) {
                escolherPersonagem(i);
                return true;
            }
        }
        int topoPrevia = yy + 24 + 6 + 6 + 12, previaH = icones - 24 - topoPrevia - 4;
        if (previaH >= 36 && personagens.size() > 1) {
            int sy = topoPrevia + previaH / 2 - 8;
            if (sobre(x, y, cx0 + 3, sy, 12, 16)) {
                escolherPersonagem((escolhido + n - 1) % n);
                return true;
            }
            if (sobre(x, y, cx0 + cw - 15, sy, 12, 16)) {
                escolherPersonagem((escolhido + 1) % n);
                return true;
            }
        }
        return false;
    }

    private void escolherLocal(String id) {
        if (local.equals(id)) return;
        local = id;
        clique();
        // Centraliza a vista no lugar escolhido, sem mudar o zoom.
        if (id.equals(Inicio.NASCIMENTO)) {
            vcx = m.jx();
            vcz = m.jz();
        } else for (Rede.Marca k : prontos) {
            if (k.local().equals(id)) {
                vcx = k.x();
                vcz = k.z();
            }
        }
        prender();
    }

    private void escolherPersonagem(int i) {
        if (i == escolhido) return;
        escolhido = i;
        clique();
    }

    private void clique() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1f, 0.35f));
    }

    private void comecar() {
        String p = personagens.isEmpty() ? "" : personagens.get(escolhido).id().toString();
        Rede.CANAL.send(new Rede.InicioEscolha(local, p), com.danrod.dcuniverse.core.Canal.SERVIDOR);
        minecraft.gui.setScreen(null);
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (!m.escolher() && Teclas.MAPA.matches(e)) {
            onClose();
            return true;
        }
        if (m.escolher() && !personagens.isEmpty() && (e.key() == 262 || e.key() == 263)) {
            int n = personagens.size();
            escolherPersonagem(e.key() == 262 ? (escolhido + 1) % n : (escolhido + n - 1) % n);
            return true;
        }
        return super.keyPressed(e);
    }

    private static boolean sobre(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
