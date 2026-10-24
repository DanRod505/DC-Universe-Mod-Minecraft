package com.danrod.dcuniverse.client.guia;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.client.Skins;
import com.danrod.dcuniverse.ability.passive.Planar;
import com.danrod.dcuniverse.client.Planador;
import com.danrod.dcuniverse.client.Voador;
import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.guia.Guia;
import com.danrod.dcuniverse.client.Teclas;
import com.danrod.dcuniverse.client.npc.NpcsCliente;
import com.danrod.dcuniverse.client.npc.TextoMissao;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.missao.DefMissao;
import com.danrod.dcuniverse.npc.DefNpc;
import com.danrod.dcuniverse.npc.ItemRef;
import net.minecraft.locale.Language;
import net.minecraftforge.network.PacketDistributor;
import com.google.gson.JsonParser;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Guia do Herói (docs/GUIA.md): livro aberto com marcadores de capítulo na borda direita.
 * Capítulos: Primeiros Passos (tarefas com visto), Personagens (índice com busca e filtros; ficha com prévia 3D
 * giratória e sumário clicável) e Itens e Receitas (busca, categorias, receita, como obter e "usado em", com links
 * entre os itens; dados em ItensGuia).
 * Todo texto sai do lang; fichas e habilidades vêm dos mesmos JSON que o servidor usa (pacote Catalogo).
 */
public final class GuiaScreen extends Screen {
    static final Identifier TEX = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "textures/gui/guia.png");
    static final Identifier SILHUETA = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "textures/gui/silhueta.png");
    static final int TEX_W = 512, TEX_H = 256;
    static final int W = 300, H = 190;
    /** Área de texto de cada página, relativa ao canto do livro. */
    static final int ESQ_X = 18, DIR_X = 162, PAG_W = 120, TOPO = 15, BASE = 172;

    static final int TINTA = 0xFF3A2A1A, TITULO = 0xFF7A1F1F, SUAVE = 0xFF7D6646, VERDE = 0xFF2E7D32, OURO = 0xFFB0841E;

    enum Capitulo {
        PASSOS(304, 0, 336, 0), PERSONAGENS(304, 48, 336, 16), ITENS(360, 0, 336, 32), MISSOES(360, 48, 420, 0), NPCS(388, 0, 420, 16);

        /** Marcador na textura (u, v do fechado; o aberto fica 24 abaixo) e o ícone 12x12. */
        final int u, v, iconeU, icone;

        Capitulo(int u, int v, int iconeU, int icone) {
            this.u = u;
            this.v = v;
            this.iconeU = iconeU;
            this.icone = icone;
        }
    }

    /** Filtros do índice de personagens (campo "category" do JSON, o mesmo "cat" de docs/dados). */
    enum Filtro {
        TODOS(32), HEROI(16), VILAO(48), ANTI(64), AJUDANTE(80);

        /** Ícone 12x12 na coluna 336 da textura. */
        final int icone;

        Filtro(int icone) {
            this.icone = icone;
        }

        static Filtro de(String cat) {
            return switch (cat) {
                case "vilao" -> VILAO;
                case "anti" -> ANTI;
                case "ajudante" -> AJUDANTE;
                default -> HEROI;
            };
        }

        String chave() {
            return "dcuniverse.guia.personagens.filtro." + name().toLowerCase(Locale.ROOT);
        }
    }

    /** Título do sumário da ficha: aponta para o bloco onde o tópico começa. */
    private record Topico(Component nome, boolean sub, Bloco bloco) {}

    private static Capitulo ultimoCapitulo = Capitulo.PASSOS;
    /** Personagens: true mostra o índice (busca, filtros e grade de rostos); false, a ficha do personagem. */
    private static boolean naLista = true;
    private static Filtro ultimoFiltro = Filtro.TODOS;
    private static String ultimaBuscaPers = "";
    private static String ultimoPersonagem = "";
    private static String ultimoItem = "";
    private static ItensGuia.Grupo ultimoGrupo = ItensGuia.Grupo.TODOS;
    private static String ultimaBusca = "";

    private Capitulo capitulo;
    private List<Personagem> personagens = List.of();
    private final Map<String, Filtro> categorias = new HashMap<>();
    /** Páginas do sumário no começo da ficha (as do conteúdo vêm depois). */
    private int paginasSumario;
    /** Tópico escolhido no sumário: brilha por um instante na página de destino para o olho achar onde começa. */
    private int destaque = -1;
    private long destaqueAte;
    private Bloco blocoDestaque;
    private int rolagemPers;
    private int indice;
    private int pagina;
    private int x0, y0;
    private float giroBase = 25f, inclinacao = 0f;
    private boolean arrastando;
    private PageButton anterior, proximo, pagAnterior, pagProxima;
    private EditBox busca;
    private int rolagem;
    private final Deque<String> historico = new ArrayDeque<>();
    /** Itens clicáveis desenhados no último quadro (encaixes da grade, da receita e do "usado em"). */
    private final List<Alvo> alvos = new ArrayList<>();
    private int mouseX, mouseY;

    private record Alvo(int x, int y, int w, int h, ItemStack stack, Runnable acao) {}

    private GuiaScreen() {
        super(Component.translatable("item.dcuniverse.guia_heroi"));
    }

    /** Abre direto na página de um item do mod (usado pelos links e pelos testes). */
    public static void abrirItem(String id) {
        ultimoCapitulo = Capitulo.ITENS;
        ultimoItem = id;
        GuiaScreen s = new GuiaScreen();
        s.capitulo = Capitulo.ITENS;
        Minecraft.getInstance().gui.setScreen(s);
    }

    /** Abre direto no Diário de Missões (tecla J). */
    public static void abrirDiario() {
        ultimoCapitulo = Capitulo.MISSOES;
        GuiaScreen s = new GuiaScreen();
        s.capitulo = Capitulo.MISSOES;
        Minecraft.getInstance().gui.setScreen(s);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
    }

    /** Abre na página que faz sentido agora: o personagem ativo, se estiver transformado; senão a última lida. */
    public static void abrir() {
        GuiaScreen s = new GuiaScreen();
        String ativo = EstadoCliente.atual.personagem();
        if (!ativo.isEmpty()) {
            ultimoCapitulo = Capitulo.PERSONAGENS;
            ultimoPersonagem = ativo;
            naLista = false;
        }
        s.capitulo = ultimoCapitulo;
        Minecraft.getInstance().gui.setScreen(s);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
    }

    @Override
    protected void init() {
        x0 = (width - W - 18) / 2;
        y0 = (height - H) / 2;
        personagens = carregarPersonagens(categorias);
        indice = 0;
        for (int i = 0; i < personagens.size(); i++) {
            if (personagens.get(i).id().toString().equals(ultimoPersonagem)) indice = i;
        }
        anterior = addRenderableWidget(new PageButton(x0 + ESQ_X + 2, y0 + BASE - 10, false, b -> trocarPersonagem(-1), true));
        proximo = addRenderableWidget(new PageButton(x0 + ESQ_X + PAG_W - 25, y0 + BASE - 10, true, b -> trocarPersonagem(1), true));
        pagAnterior = addRenderableWidget(new PageButton(x0 + DIR_X + 2, y0 + BASE - 10, false, b -> pagina--, true));
        pagProxima = addRenderableWidget(new PageButton(x0 + DIR_X + PAG_W - 25, y0 + BASE - 10, true, b -> pagina++, true));
        ItensGuia.carregar();
        busca = addRenderableWidget(new EditBox(font, x0 + ESQ_X + 15, y0 + BUSCA_Y + 3, PAG_W - 19, 10,
            Component.translatable("dcuniverse.guia.itens.buscar")));
        busca.setBordered(false);
        busca.setTextShadow(false);
        busca.setTextColor(TINTA);
        busca.setMaxLength(40);
        busca.setHint(Component.translatable("dcuniverse.guia.itens.buscar").withColor(SUAVE));
        busca.setResponder(t -> {
            if (capitulo == Capitulo.ITENS) {
                ultimaBusca = t;
                rolagem = 0;
            } else {
                ultimaBuscaPers = t;
                rolagemPers = 0;
            }
        });
        sincronizarBusca();
        atualizarBotoes();
    }

    /** Personagens do servidor na ordem da lista do mod (campo "n" do JSON). */
    private static List<Personagem> carregarPersonagens(Map<String, Filtro> categorias) {
        List<Personagem> out = new ArrayList<>();
        EstadoCliente.catalogo.forEach((id, json) -> {
            try {
                var obj = JsonParser.parseString(json).getAsJsonObject();
                out.add(Personagem.parse(Identifier.parse(id), obj));
                categorias.put(id, Filtro.de(obj.has("category") ? obj.get("category").getAsString() : "heroi"));
            } catch (RuntimeException e) {
                DCUniverse.LOGGER.warn("Guia: personagem {} ignorado: {}", id, e.toString());
            }
        });
        out.sort(Comparator.comparingInt(p -> ordemDe(p)));
        return List.copyOf(out);
    }

    private static int ordemDe(Personagem p) {
        String json = EstadoCliente.catalogo.get(p.id().toString());
        try {
            return JsonParser.parseString(json).getAsJsonObject().get("n").getAsInt();
        } catch (RuntimeException e) {
            return Integer.MAX_VALUE;
        }
    }

    private void trocarPersonagem(int d) {
        if (personagens.isEmpty()) return;
        indice = Math.floorMod(indice + d, personagens.size());
        ultimoPersonagem = personagens.get(indice).id().toString();
        pagina = 0;
        atualizarBotoes();
    }

    private void irPara(Capitulo c) {
        if (c == capitulo) {
            // O marcador de Personagens com uma ficha aberta volta ao índice.
            if (c == Capitulo.PERSONAGENS && !naLista) abrirLista();
            return;
        }
        capitulo = c;
        ultimoCapitulo = c;
        pagina = 0;
        rolagemLista = 0;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
        sincronizarBusca();
        atualizarBotoes();
    }

    /** O campo de busca é um só: mostra o texto e a dica do capítulo aberto. */
    private void sincronizarBusca() {
        boolean itens = capitulo == Capitulo.ITENS;
        busca.setHint(Component.translatable(itens ? "dcuniverse.guia.itens.buscar" : "dcuniverse.guia.personagens.buscar").withColor(SUAVE));
        busca.setValue(itens ? ultimaBusca : ultimaBuscaPers);
        busca.setFocused(false);
    }

    private void abrirLista() {
        naLista = true;
        pagina = 0;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
        sincronizarBusca();
        atualizarBotoes();
    }

    private void abrirFicha(Personagem p) {
        indice = Math.max(0, personagens.indexOf(p));
        ultimoPersonagem = p.id().toString();
        naLista = false;
        pagina = 0;
        busca.setFocused(false);
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
        atualizarBotoes();
    }

    private void atualizarBotoes() {
        busca.visible = busca.active = capitulo == Capitulo.ITENS || (capitulo == Capitulo.PERSONAGENS && naLista);
        if (!busca.visible) busca.setFocused(false);
        boolean pers = capitulo == Capitulo.PERSONAGENS && !naLista && personagens.size() > 1;
        anterior.visible = proximo.visible = pers;
        int paginas = paginasDireita().size();
        pagina = Mth.clamp(pagina, 0, Math.max(0, paginas - 1));
        pagAnterior.visible = pagina > 0;
        pagProxima.visible = pagina < paginas - 1;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    @Override
    public void tick() {
        atualizarBotoes();
    }

    // ---------------------------------------------------------------- desenho

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float a) {
        mouseX = mx;
        mouseY = my;
        alvos.clear();
        marcadores(g, mx, my);
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, x0, y0, 0, 0, W, H, TEX_W, TEX_H);
        switch (capitulo) {
            case PASSOS -> passosEsquerda(g);
            case PERSONAGENS -> {
                if (naLista) listaEsquerda(g, mx, my);
                else personagemEsquerda(g, mx, my);
            }
            case ITENS -> itensEsquerda(g, mx, my);
            case MISSOES -> missoesEsquerda(g, mx, my);
            case NPCS -> npcsEsquerda(g, mx, my);
        }
        List<List<Bloco>> paginas = paginasDireita();
        if (!paginas.isEmpty()) {
            int y = y0 + TOPO;
            long agora = System.currentTimeMillis();
            for (Bloco b : paginas.get(Math.min(pagina, paginas.size() - 1))) {
                if (b == blocoDestaque && agora < destaqueAte) {
                    int alfa = (int) (0x70 * Math.min(1.0, (destaqueAte - agora) / 600.0));
                    g.fill(x0 + DIR_X - 3, y - 2, x0 + DIR_X + PAG_W + 2, y + b.altura - 1, alfa << 24 | 0xE0B040);
                }
                b.desenho.desenhar(g, x0 + DIR_X, y);
                y += b.altura;
            }
            if (paginas.size() > 1) {
                String cont = (pagina + 1) + " / " + paginas.size();
                g.centeredText(font, cont, x0 + DIR_X + PAG_W / 2, y0 + BASE - 7, SUAVE);
                if (capitulo == Capitulo.PERSONAGENS && !naLista && paginasSumario > 0 && pagina >= paginasSumario) {
                    // Atalho de volta ao sumário, ao lado do número da página.
                    botaoLista(g, x0 + DIR_X + PAG_W / 2 - font.width(cont) / 2 - 15, y0 + BASE - 9,
                        "dcuniverse.guia.sumario.voltar", () -> virar(0));
                }
            }
        }
        super.extractRenderState(g, mx, my, a);
    }

    private static final int MARC_Y0 = 18, MARC_PASSO = 26;

    /** Marcadores na borda direita: o do capítulo aberto fica mais comprido e claro. */
    private void marcadores(GuiGraphicsExtractor g, int mx, int my) {
        for (Capitulo c : Capitulo.values()) {
            int y = y0 + MARC_Y0 + c.ordinal() * MARC_PASSO;
            boolean ativo = c == capitulo;
            g.blit(RenderPipelines.GUI_TEXTURED, TEX, x0 + W - 4, y, c.u, c.v + (ativo ? 24 : 0), 24, 22, TEX_W, TEX_H);
            g.blit(RenderPipelines.GUI_TEXTURED, TEX, x0 + W + (ativo ? 4 : 2), y + 5, c.iconeU, c.icone, 12, 12, TEX_W, TEX_H);
            if (sobre(mx, my, x0 + W - 4, y, 24, 22)) {
                g.setTooltipForNextFrame(Component.translatable("dcuniverse.guia.capitulo." + c.name().toLowerCase(Locale.ROOT)), mx, my);
            }
        }
    }

    private void titulo(GuiGraphicsExtractor g, Component texto, int x, int y) {
        g.centeredText(font, texto.copy().withStyle(ChatFormatting.BOLD), x + PAG_W / 2, y, TITULO);
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, x + PAG_W / 2 - 40, y + 11, 304, 116, 80, 5, TEX_W, TEX_H);
    }

    private int paragrafo(GuiGraphicsExtractor g, Component texto, int x, int y, int largura, int cor) {
        for (FormattedCharSequence l : font.split(texto, largura)) {
            g.text(font, l, x, y, cor, false);
            y += 10;
        }
        return y;
    }

    // Primeiros Passos ------------------------------------------------------

    private void passosEsquerda(GuiGraphicsExtractor g) {
        int x = x0 + ESQ_X, y = y0 + TOPO;
        titulo(g, Component.translatable("dcuniverse.guia.capitulo.passos"), x, y);
        long feitas = Guia.TAREFAS.stream().filter(EstadoCliente.guia.tarefas()::contains).count();
        boolean tudo = feitas == Guia.TAREFAS.size();
        g.text(font, Component.translatable("dcuniverse.guia.passos.progresso", feitas, Guia.TAREFAS.size()), x, y + 22, tudo ? VERDE : TINTA, false);
        barra(g, x, y + 33, PAG_W, (float) feitas / Guia.TAREFAS.size(), tudo ? VERDE : OURO);
        paragrafo(g, Component.translatable(tudo ? "dcuniverse.guia.passos.fim" : "dcuniverse.guia.passos.intro"), x, y + 48, PAG_W, TINTA);
        List<FormattedCharSequence> dica = font.split(Component.translatable("dcuniverse.guia.passos.teclas"), PAG_W);
        int yd = y0 + BASE - dica.size() * 10;
        for (FormattedCharSequence l : dica) {
            g.text(font, l, x, yd, SUAVE, false);
            yd += 10;
        }
    }

    private List<Bloco> passosDireita() {
        List<Bloco> out = new ArrayList<>();
        var feitas = EstadoCliente.guia.tarefas();
        for (String t : Guia.TAREFAS) {
            boolean ok = feitas.contains(t);
            Component nome = Component.translatable("dcuniverse.guia.tarefa." + t);
            List<FormattedCharSequence> desc = font.split(Component.translatable("dcuniverse.guia.tarefa." + t + ".desc"), PAG_W - 15);
            int alt = 11 + desc.size() * 9 + 6;
            out.add(new Bloco(alt, (g, x, y) -> {
                g.blit(RenderPipelines.GUI_TEXTURED, TEX, x, y - 1, ok ? 316 : 304, 100, 11, 11, TEX_W, TEX_H);
                g.text(font, ok ? nome.copy().withStyle(ChatFormatting.BOLD) : nome, x + 15, y + 1, ok ? VERDE : TINTA, false);
                int yy = y + 11;
                for (FormattedCharSequence l : desc) {
                    g.text(font, l, x + 15, yy, SUAVE, false);
                    yy += 9;
                }
            }));
        }
        return out;
    }

    // Diário de Missões ------------------------------------------------------

    private static boolean abaConcluidas;
    private static String ultimaMissao = "";
    private static String ultimoNpc = "";
    private int rolagemLista;
    /** Abandonar pede um segundo clique; guarda até quando vale a confirmação. */
    private long confirmarAte;
    private static final int LISTA_Y = TOPO + 40, LINHA = 12, LISTA_LINHAS = 9;

    private List<String> missoesDaAba() {
        Rede.DiarioSync d = EstadoCliente.diario;
        List<String> out = new ArrayList<>();
        if (abaConcluidas) {
            for (String id : d.concluidas()) if (NpcsCliente.missao(Identifier.tryParse(id)) != null) out.add(id);
        } else {
            for (Rede.MissaoVista m : d.ativas()) if (NpcsCliente.missao(Identifier.tryParse(m.id())) != null) out.add(m.id());
        }
        return out;
    }

    private Rede.MissaoVista vista(String id) {
        for (Rede.MissaoVista m : EstadoCliente.diario.ativas()) if (m.id().equals(id)) return m;
        return null;
    }

    private String missaoAtual() {
        List<String> l = missoesDaAba();
        if (l.contains(ultimaMissao)) return ultimaMissao;
        String r = EstadoCliente.diario.rastreada();
        if (!abaConcluidas && l.contains(r)) return r;
        return l.isEmpty() ? "" : l.get(0);
    }

    private void clique() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    /** Aba de texto (Ativas/Concluídas, ou os grupos de NPCs): sublinhada quando aberta. */
    private void aba(GuiGraphicsExtractor g, Component c, int x, int y, int w, boolean ativa, Runnable acao) {
        boolean em = sobre(mouseX, mouseY, x, y - 2, w, 13);
        g.fill(x, y - 2, x + w, y + 11, ativa ? 0x50B0841E : em ? 0x28B0841E : 0x14000000);
        if (ativa) g.fill(x, y + 10, x + w, y + 11, OURO);
        g.centeredText(font, c, x + w / 2, y + 1, ativa ? TITULO : em ? TINTA : SUAVE);
        alvos.add(new Alvo(x, y - 2, w, 13, ItemStack.EMPTY, acao));
    }

    private void missoesEsquerda(GuiGraphicsExtractor g, int mx, int my) {
        int x = x0 + ESQ_X, y = y0 + TOPO;
        titulo(g, Component.translatable("dcuniverse.guia.capitulo.missoes"), x, y);
        Rede.DiarioSync d = EstadoCliente.diario;
        int metade = PAG_W / 2 - 1;
        aba(g, Component.translatable("dcuniverse.guia.missoes.ativas", d.ativas().size()), x, y + 21, metade, !abaConcluidas, () -> {
            if (abaConcluidas) { abaConcluidas = false; rolagemLista = 0; pagina = 0; clique(); }
        });
        aba(g, Component.translatable("dcuniverse.guia.missoes.concluidas", d.concluidas().size()), x + metade + 2, y + 21, metade, abaConcluidas, () -> {
            if (!abaConcluidas) { abaConcluidas = true; rolagemLista = 0; pagina = 0; clique(); }
        });
        List<String> lista = missoesDaAba();
        String aberta = missaoAtual();
        int ly = y0 + LISTA_Y;
        if (lista.isEmpty()) {
            paragrafo(g, Component.translatable(abaConcluidas ? "dcuniverse.guia.missoes.nenhuma_concluida" : "dcuniverse.guia.missoes.nenhuma"), x, ly + 4, PAG_W, SUAVE);
            return;
        }
        rolagemLista = Mth.clamp(rolagemLista, 0, Math.max(0, lista.size() - LISTA_LINHAS));
        for (int i = rolagemLista; i < Math.min(lista.size(), rolagemLista + LISTA_LINHAS); i++) {
            String id = lista.get(i);
            DefMissao m = NpcsCliente.missao(Identifier.parse(id));
            int ry = ly + (i - rolagemLista) * LINHA;
            boolean sel = id.equals(aberta), em = sobre(mx, my, x, ry - 2, PAG_W, LINHA);
            if (sel) g.fill(x - 2, ry - 2, x + PAG_W + 1, ry + LINHA - 2, 0x40B0841E);
            else if (em) g.fill(x - 2, ry - 2, x + PAG_W + 1, ry + LINHA - 2, 0x20B0841E);
            boolean rastreada = id.equals(d.rastreada());
            if (abaConcluidas) g.text(font, "✔", x, ry, VERDE, false);
            else if (rastreada) g.text(font, "◆", x, ry, OURO, false);
            else g.fill(x + 2, ry + 3, x + 5, ry + 6, SUAVE);
            String nome = cortar(TextoMissao.nome(m).getString(), PAG_W - 10);
            g.text(font, nome, x + 10, ry, sel ? TITULO : TINTA, false);
            if (em) {
                List<Component> dica = new ArrayList<>();
                dica.add(TextoMissao.nome(m));
                dica.add(Component.translatable("dcuniverse.guia.missoes.de", NpcsCliente.nome(m.npc())).withStyle(ChatFormatting.GRAY));
                if (rastreada) dica.add(Component.translatable("dcuniverse.guia.missoes.rastreando").withStyle(ChatFormatting.GOLD));
                g.setTooltipForNextFrame(font, dica, java.util.Optional.empty(), mx, my);
            }
            alvos.add(new Alvo(x - 2, ry - 2, PAG_W + 3, LINHA, ItemStack.EMPTY, () -> {
                if (!id.equals(ultimaMissao)) {
                    ultimaMissao = id;
                    pagina = 0;
                    confirmarAte = 0;
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
                }
            }));
        }
        if (lista.size() > LISTA_LINHAS) {
            int bx = x + PAG_W + 3, alt = LISTA_LINHAS * LINHA - 2;
            g.fill(bx, ly - 2, bx + 2, ly - 2 + alt, 0x40000000);
            int tam = Math.max(8, alt * LISTA_LINHAS / lista.size());
            int pos = (alt - tam) * rolagemLista / Math.max(1, lista.size() - LISTA_LINHAS);
            g.fill(bx, ly - 2 + pos, bx + 2, ly - 2 + pos + tam, OURO);
        }
        g.centeredText(font, Component.translatable("dcuniverse.guia.missoes.tecla", Teclas.DIARIO.getTranslatedKeyMessage()), x + PAG_W / 2, y0 + BASE - 7, SUAVE);
    }

    /** Botão de texto numa linha da página direita. */
    private Bloco botao(Component c, int cor, Runnable acao) {
        return new Bloco(16, (g, x, y) -> {
            int w = font.width(c) + 12;
            boolean em = sobre(mouseX, mouseY, x, y, w, 13);
            g.fill(x, y, x + w, y + 13, em ? 0xFF7A5C34 : 0xFF9C8460);
            g.fill(x + 1, y + 1, x + w - 1, y + 12, em ? 0xFFF3E2B8 : 0xFFF8EFD8);
            g.text(font, c, x + 6, y + 3, cor, false);
            alvos.add(new Alvo(x, y, w, 13, ItemStack.EMPTY, acao));
        });
    }

    /** Linha com o ícone de um item e o texto ao lado. */
    private Bloco linhaItem(ItemStack s, Component c) {
        return new Bloco(18, (g, x, y) -> {
            encaixe(g, x, y, s, false, link(s));
            g.text(font, cortar(c.getString(), PAG_W - 22), x + 21, y + 5, TINTA, false);
        });
    }

    private List<Bloco> missaoDireita() {
        List<Bloco> out = new ArrayList<>();
        String id = missaoAtual();
        DefMissao m = id.isEmpty() ? null : NpcsCliente.missao(Identifier.parse(id));
        if (m == null) {
            texto(out, Component.translatable("dcuniverse.guia.missoes.intro"), TINTA, 6);
            texto(out, Component.translatable("dcuniverse.guia.missoes.como"), SUAVE, 0);
            return out;
        }
        Rede.MissaoVista v = vista(id);
        boolean feita = v == null;
        if (!m.capitulo().isEmpty()) texto(out, Component.translatable("dcuniverse.capitulo." + m.capitulo()).withStyle(ChatFormatting.ITALIC), OURO, 2);
        titulos(out, TextoMissao.nome(m));
        texto(out, Component.translatable("dcuniverse.guia.missoes.de", NpcsCliente.nome(m.npc())), SUAVE, 4);
        if (Language.getInstance().has(m.chave() + ".desc")) texto(out, Component.translatable(m.chave() + ".desc"), TINTA, 4);
        out.add(secao("dcuniverse.guia.missoes.objetivos"));
        for (int i = 0; i < m.objetivos().size(); i++) {
            int estado = feita || i < v.objetivo() ? 0 : i == v.objetivo() ? 1 : 2;
            Component c = estado == 2 ? Component.literal("???") : TextoMissao.objetivo(m, i, estado == 1 ? v.progresso() : m.objetivos().get(i).qtd());
            String marca = estado == 0 ? "✔" : estado == 1 ? "▶" : "•";
            int cor = estado == 0 ? VERDE : estado == 1 ? TINTA : SUAVE;
            List<FormattedCharSequence> linhas = font.split(c, PAG_W - 10);
            out.add(new Bloco(linhas.size() * 9 + 2, (g, x, y) -> {
                g.text(font, marca, x, y, estado == 1 ? OURO : cor, false);
                int yy = y;
                for (FormattedCharSequence l : linhas) {
                    g.text(font, l, x + 10, yy, cor, false);
                    yy += 9;
                }
            }));
        }
        DefMissao.Recompensa r = m.recompensa();
        if (!r.itens().isEmpty() || r.xp() > 0 || r.afinidade() > 0 || !r.grupos().isEmpty()) {
            out.add(espaco(3));
            out.add(secao("dcuniverse.guia.missoes.recompensas"));
            for (var it : r.itens()) {
                ItemStack s = it.pilha();
                out.add(linhaItem(s, Component.literal(it.qtd() + "x ").append(s.getHoverName())));
            }
            if (r.xp() > 0) out.add(item(Component.translatable("dcuniverse.guia.missoes.xp", r.xp())));
            DefNpc dono = NpcsCliente.npc(m.npc());
            if (r.afinidade() > 0 && dono != null) {
                out.add(item(Component.translatable("dcuniverse.guia.missoes.afinidade", r.afinidade(), Component.translatable("dcuniverse.npc.grupo." + dono.grupo()))));
            }
            r.grupos().forEach((grupo, n) -> out.add(item(Component.translatable("dcuniverse.guia.missoes.afinidade", n,
                Component.translatable("dcuniverse.npc.grupo." + grupo)))));
        }
        if (m.repetivel()) {
            out.add(espaco(2));
            texto(out, Component.translatable("dcuniverse.guia.missoes.repetivel", m.intervalo()), SUAVE, 0);
        }
        if (!feita) {
            out.add(espaco(4));
            boolean rastreada = id.equals(EstadoCliente.diario.rastreada());
            out.add(botao(Component.translatable(rastreada ? "dcuniverse.guia.missoes.parar" : "dcuniverse.guia.missoes.rastrear"), TINTA, () -> {
                clique();
                Rede.CANAL.send(new Rede.DiarioAcao(0, rastreada ? "" : id), PacketDistributor.SERVER.noArg());
            }));
            boolean confirmar = System.currentTimeMillis() < confirmarAte;
            out.add(botao(Component.translatable(confirmar ? "dcuniverse.guia.missoes.confirmar" : "dcuniverse.guia.missoes.abandonar"), TITULO, () -> {
                clique();
                if (System.currentTimeMillis() < confirmarAte) {
                    confirmarAte = 0;
                    Rede.CANAL.send(new Rede.DiarioAcao(1, id), PacketDistributor.SERVER.noArg());
                } else {
                    confirmarAte = System.currentTimeMillis() + 3000;
                }
            }));
        }
        return out;
    }

    // Facções e NPCs -----------------------------------------------------------

    /** Um verbete: um NPC com nome próprio ou um grupo de cidadãos que dividem as falas. */
    private record Verbete(String chave, DefNpc def, List<DefNpc> membros) {}

    private List<Verbete> verbetes() {
        Map<String, Verbete> out = new java.util.LinkedHashMap<>();
        for (String id : EstadoCliente.diario.conhecidos()) {
            DefNpc d = NpcsCliente.npc(Identifier.tryParse(id));
            if (d == null) continue;
            String chave = d.nomes().isEmpty() ? d.id().toString() : "grupo:" + d.grupo();
            Verbete v = out.computeIfAbsent(chave, k -> new Verbete(k, d, new ArrayList<>()));
            v.membros().add(d);
        }
        return List.copyOf(out.values());
    }

    private Verbete verbeteAtual(List<Verbete> l) {
        for (Verbete v : l) if (v.chave().equals(ultimoNpc)) return v;
        return l.isEmpty() ? null : l.get(0);
    }

    private Component nomeVerbete(Verbete v) {
        return v.chave().startsWith("grupo:") ? Component.translatable("dcuniverse.npc.grupo." + v.def().grupo())
            : Component.translatable(NpcsCliente.chave(v.def().chaveNome()));
    }

    private int afinidade(DefNpc d) {
        return EstadoCliente.diario.afinidades().getOrDefault(d.grupo(), 0);
    }

    private static int nivelDe(int af) {
        int n = 0;
        for (int i = 0; i < NIVEIS.length; i++) if (af >= NIVEIS[i]) n = i;
        return n;
    }

    private static final int[] NIVEIS = {0, 25, 50, 75, 100};

    private void npcsEsquerda(GuiGraphicsExtractor g, int mx, int my) {
        int x = x0 + ESQ_X, y = y0 + TOPO;
        titulo(g, Component.translatable("dcuniverse.guia.capitulo.npcs"), x, y);
        List<Verbete> lista = verbetes();
        if (lista.isEmpty()) {
            paragrafo(g, Component.translatable("dcuniverse.guia.npcs.vazio"), x, y + 24, PAG_W, SUAVE);
            return;
        }
        Verbete aberto = verbeteAtual(lista);
        int linha = 26, linhas = 5;
        rolagemLista = Mth.clamp(rolagemLista, 0, Math.max(0, lista.size() - linhas));
        int ly = y + 22;
        for (int i = rolagemLista; i < Math.min(lista.size(), rolagemLista + linhas); i++) {
            Verbete v = lista.get(i);
            int ry = ly + (i - rolagemLista) * linha;
            boolean sel = v == aberto, em = sobre(mx, my, x - 2, ry - 2, PAG_W + 3, linha - 2);
            if (sel) g.fill(x - 2, ry - 2, x + PAG_W + 1, ry + linha - 4, 0x40B0841E);
            else if (em) g.fill(x - 2, ry - 2, x + PAG_W + 1, ry + linha - 4, 0x20B0841E);
            g.fill(x - 1, ry - 1, x + 21, ry + 21, 0xFF5A4630);
            PlayerFaceExtractor.extractRenderState(g, NpcsCliente.skin(v.def(), 0), x, ry, 20);
            g.text(font, cortar(nomeVerbete(v).getString(), PAG_W - 26), x + 25, ry + 1, sel ? TITULO : TINTA, false);
            int af = afinidade(v.def());
            barra(g, x + 25, ry + 12, PAG_W - 46, af / 100f, nivelDe(af) >= 3 ? VERDE : OURO);
            g.text(font, String.valueOf(af), x + PAG_W - font.width(String.valueOf(af)), ry + 12, SUAVE, false);
            if (em) {
                g.setTooltipForNextFrame(font, List.of(nomeVerbete(v),
                    Component.translatable("dcuniverse.npc.nivel." + nivelDe(af)).withStyle(ChatFormatting.GRAY)), java.util.Optional.empty(), mx, my);
            }
            alvos.add(new Alvo(x - 2, ry - 2, PAG_W + 3, linha - 2, ItemStack.EMPTY, () -> {
                if (!v.chave().equals(ultimoNpc)) {
                    ultimoNpc = v.chave();
                    pagina = 0;
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
                }
            }));
        }
        if (lista.size() > linhas) {
            g.centeredText(font, (rolagemLista + 1) + "-" + Math.min(lista.size(), rolagemLista + linhas) + " / " + lista.size(), x + PAG_W / 2, y0 + BASE - 7, SUAVE);
        }
    }

    private List<Bloco> npcDireita() {
        List<Bloco> out = new ArrayList<>();
        Verbete v = verbeteAtual(verbetes());
        if (v == null) {
            texto(out, Component.translatable("dcuniverse.guia.npcs.intro"), TINTA, 0);
            return out;
        }
        DefNpc d = v.def();
        int af = afinidade(d), nv = nivelDe(af);
        titulos(out, nomeVerbete(v));
        if (v.chave().startsWith("grupo:")) {
            List<String> profs = new ArrayList<>();
            for (DefNpc m : v.membros()) {
                String p = Component.translatable("dcuniverse.npc.profissao." + m.profissao()).getString();
                if (!m.profissao().isEmpty() && !profs.contains(p)) profs.add(p);
            }
            if (!profs.isEmpty()) texto(out, Component.literal(String.join(", ", profs)), SUAVE, 2);
        }
        String sobreK = NpcsCliente.chave(d.chave("sobre"));
        if (Language.getInstance().has(sobreK)) texto(out, Component.translatable(sobreK), TINTA, 4);
        out.add(secao("dcuniverse.guia.npcs.afinidade"));
        out.add(new Bloco(12, (g, x, y) -> {
            Component nome = Component.translatable("dcuniverse.npc.nivel." + nv);
            g.text(font, nome, x, y, nv >= 3 ? VERDE : TINTA, false);
            int bx = x + font.width(nome) + 5;
            barra(g, bx, y + 1, PAG_W - (bx - x) - 22, af / 100f, nv >= 3 ? VERDE : OURO);
            g.text(font, String.valueOf(af), x + PAG_W - font.width(String.valueOf(af)), y, SUAVE, false);
        }));
        if (nv < NIVEIS.length - 1) {
            texto(out, Component.translatable("dcuniverse.guia.npcs.proximo", NIVEIS[nv + 1],
                Component.translatable("dcuniverse.npc.beneficio." + (nv + 1))), SUAVE, 2);
        } else {
            texto(out, Component.translatable("dcuniverse.guia.npcs.maximo"), VERDE, 2);
        }
        texto(out, Component.translatable("dcuniverse.guia.npcs.ganhar"), SUAVE, 3);
        Map<String, Integer> gostos = new java.util.LinkedHashMap<>();
        for (DefNpc m : v.membros()) m.gostos().forEach(gostos::putIfAbsent);
        if (!gostos.isEmpty()) {
            out.add(secao("dcuniverse.guia.npcs.gostos"));
            List<ItemStack> pilhas = new ArrayList<>();
            for (String k : gostos.keySet()) {
                ItemStack s = new ItemRef(k, 1).pilha();
                if (!s.isEmpty()) pilhas.add(s);
            }
            for (int i = 0; i < pilhas.size(); i += 6) {
                List<ItemStack> fila = pilhas.subList(i, Math.min(pilhas.size(), i + 6));
                out.add(new Bloco(20, (g, x, y) -> {
                    for (int k = 0; k < fila.size(); k++) encaixe(g, x + k * 20, y, fila.get(k), false, link(fila.get(k)));
                }));
            }
        }
        List<DefMissao> dele = new ArrayList<>();
        for (DefMissao m : NpcsCliente.missoes().values()) {
            if (v.membros().stream().anyMatch(x -> x.id().equals(m.npc()))) dele.add(m);
        }
        dele.sort(Comparator.comparing(m -> m.id().toString()));
        if (!dele.isEmpty()) {
            out.add(espaco(2));
            out.add(secao("dcuniverse.guia.npcs.missoes"));
            Rede.DiarioSync dz = EstadoCliente.diario;
            for (DefMissao m : dele) {
                String id = m.id().toString();
                boolean ativa = vista(id) != null, feita = dz.concluidas().contains(id);
                boolean trancada = !ativa && !feita && af < m.min();
                Component nome = trancada ? Component.translatable("dcuniverse.guia.npcs.trancada", NIVEIS[nivelDe(m.min())] == m.min()
                    ? Component.translatable("dcuniverse.npc.nivel." + nivelDe(m.min())) : Component.literal(String.valueOf(m.min())))
                    : TextoMissao.nome(m);
                String marca = feita ? "✔" : ativa ? "▶" : trancada ? "•" : "✦";
                int cor = feita ? VERDE : ativa ? TINTA : SUAVE;
                out.add(new Bloco(10, (g, x, y) -> {
                    g.text(font, marca, x, y, ativa ? OURO : cor, false);
                    boolean em = !trancada && sobre(mouseX, mouseY, x, y - 1, PAG_W, 10);
                    Component c = Component.literal(cortar(nome.getString(), PAG_W - 10));
                    if (em) c = c.copy().withStyle(ChatFormatting.UNDERLINE);
                    g.text(font, c, x + 10, y, em ? TITULO : cor, false);
                    if (!trancada && (ativa || feita)) {
                        alvos.add(new Alvo(x, y - 1, PAG_W, 10, ItemStack.EMPTY, () -> {
                            abaConcluidas = feita && !ativa;
                            ultimaMissao = id;
                            irPara(Capitulo.MISSOES);
                        }));
                    }
                }));
            }
        }
        return out;
    }

    // Personagens ------------------------------------------------------------

    private Personagem atual() {
        return personagens.isEmpty() ? null : personagens.get(Mth.clamp(indice, 0, personagens.size() - 1));
    }

    private boolean descoberto(Personagem p) {
        return EstadoCliente.guia.descobertos().contains(p.id().toString());
    }

    private static final int PREVIA_Y = 36, PREVIA_H = 96;

    private void personagemEsquerda(GuiGraphicsExtractor g, int mx, int my) {
        int x = x0 + ESQ_X, y = y0 + TOPO;
        titulo(g, Component.translatable("dcuniverse.guia.capitulo.personagens"), x, y);
        botaoLista(g, x, y - 2, "dcuniverse.guia.personagens.indice", this::abrirLista);
        Personagem p = atual();
        if (p == null) {
            paragrafo(g, Component.translatable("dcuniverse.guia.personagens.vazio"), x, y + 30, PAG_W, SUAVE);
            return;
        }
        boolean visto = descoberto(p);
        int px0 = x + 10, py0 = y0 + PREVIA_Y, px1 = x + PAG_W - 10, py1 = py0 + PREVIA_H;
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, x + PAG_W / 2 - 32, py1 - 6, 304, 124, 64, 10, TEX_W, TEX_H);
        float giro = arrastando ? giroBase : giroBase + (float) (System.currentTimeMillis() % 14400L) / 40f;
        PlayerSkin skin = visto ? Skins.skinDoPersonagem(p.id().toString()) : null;
        previa(g, skin != null ? skin : SKIN_SILHUETA, px0, py0, px1, py1, giro);
        Component nome = visto ? Component.translatable(p.chaveNome()) : Component.literal("???");
        g.centeredText(font, nome.copy().withStyle(ChatFormatting.BOLD), x + PAG_W / 2, py1 + 7, TITULO);
        Component sub = visto
            ? Component.translatable("dcuniverse.guia.personagem.nivel", p.nivel())
            : Component.translatable("dcuniverse.guia.personagem.desconhecido");
        g.centeredText(font, sub, x + PAG_W / 2, py1 + 18, SUAVE);
        if (personagens.size() > 1) {
            g.centeredText(font, (indice + 1) + " / " + personagens.size(), x + PAG_W / 2, y0 + BASE - 7, SUAVE);
        }
        if (visto && sobre(mx, my, px0, py0, px1 - px0, py1 - py0) && !arrastando) {
            g.setTooltipForNextFrame(Component.translatable("dcuniverse.guia.personagem.girar"), mx, my);
        }
    }

    private static final PlayerSkin SKIN_SILHUETA = PlayerSkin.insecure(new ClientAsset.ResourceTexture(
        SILHUETA.withPath(p -> p.replace("textures/", "").replace(".png", ""))), null, null, PlayerModelType.WIDE);

    /**
     * Prévia 3D: o mesmo renderizador de jogador do jogo (com a capa), vestido com a skin do personagem e sem
     * armadura nem itens. O id falso impede que a troca de skin dos jogadores transformados (client/Skins) mexa nela.
     */
    private void previa(GuiGraphicsExtractor g, PlayerSkin skin, int ax0, int ay0, int ax1, int ay1, float giro) {
        if (minecraft.player == null) return;
        var renderer = minecraft.getEntityRenderDispatcher().getRenderer(minecraft.player);
        if (!(renderer.createRenderState(minecraft.player, 1f) instanceof AvatarRenderState s)) return;
        s.id = Integer.MIN_VALUE;
        s.shadowPieces.clear();
        s.outlineColor = 0;
        s.nameTag = null;
        s.skin = skin;
        s.showCape = skin.cape() != null;
        s.showHat = s.showJacket = s.showLeftPants = s.showRightPants = s.showLeftSleeve = s.showRightSleeve = true;
        s.headEquipment = s.chestEquipment = s.legsEquipment = s.feetEquipment = ItemStack.EMPTY;
        s.rightHandItemStack = s.leftHandItemStack = ItemStack.EMPTY;
        s.headItem.clear();
        s.rightHandItemState.clear();
        s.leftHandItemState.clear();
        s.rightArmPose = s.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        s.parrotOnLeftShoulder = s.parrotOnRightShoulder = null;
        s.arrowCount = s.stingerCount = 0;
        s.pose = Pose.STANDING;
        s.isCrouching = s.isFallFlying = s.isVisuallySwimming = s.isPassenger = s.isUsingItem = false;
        s.isInWater = s.hasRedOverlay = s.isInvisibleToPlayer = s.isSpectator = false;
        s.walkAnimationSpeed = s.attackTime = s.deathTime = s.swimAmount = 0;
        s.capeFlap = s.capeLean = s.capeLean2 = 0;
        s.bodyRot = 180f + giro;
        s.yRot = s.xRot = 0;
        s.boundingBoxWidth /= s.scale;
        s.boundingBoxHeight /= s.scale;
        s.scale = 1f;
        Quaternionf camera = new Quaternionf().rotateX(inclinacao * Mth.DEG_TO_RAD);
        Quaternionf rot = new Quaternionf().rotateZ(Mth.PI).mul(camera);
        g.entity(s, PREVIA_H / 2.2f, new Vector3f(0, s.boundingBoxHeight / 2f + 0.0625f, 0), rot, camera, ax0, ay0, ax1, ay1);
    }

    /** Liga o bloco a um título do sumário. */
    private Bloco topico(List<Topico> topicos, Bloco b, Component nome, boolean sub) {
        topicos.add(new Topico(nome, sub, b));
        return b;
    }

    private Bloco secao(List<Topico> topicos, String chave) {
        return topico(topicos, secao(chave), Component.translatable(chave), false);
    }

    private List<Bloco> personagemDireita(List<Topico> topicos) {
        Personagem p = atual();
        List<Bloco> out = new ArrayList<>();
        if (p == null) return out;
        String chave = "personagem." + p.id().getNamespace() + "." + p.id().getPath();
        if (!descoberto(p)) {
            texto(out, Component.translatable("dcuniverse.guia.personagem.como_descobrir"), SUAVE, 4);
            return out;
        }
        texto(out, Component.translatable(chave + ".exp").withStyle(ChatFormatting.ITALIC), SUAVE, 6);
        out.add(secao(topicos, "dcuniverse.guia.secao.ficha"));
        Personagem.Ficha f = p.ficha();
        out.add(atributo("vida", f.vida(), FichaEscala.VIDA, fmt(f.vida())));
        out.add(atributo("armadura", f.armadura(), FichaEscala.ARMADURA, fmt(f.armadura())));
        out.add(atributo("dano", f.dano(), FichaEscala.DANO, fmt(f.dano())));
        out.add(atributo("energia", p.energia().max(), FichaEscala.ENERGIA, fmt(p.energia().max())));
        out.add(atributo("velocidade", f.terra(), FichaEscala.VELOCIDADE, fmt(f.terra())));
        Personagem.Tipo voo = p.passiva("flight");
        if (voo != null) out.add(atributo("voo", voo.params().d("speed", 12), FichaEscala.VOO, fmt(voo.params().d("speed", 12))));
        Personagem.Tipo planar = p.passiva("glide");
        if (planar != null) out.add(atributo("planar", planar.params().d("speed", 10), FichaEscala.PLANAR, fmt(planar.params().d("speed", 10))));
        texto(out, Component.translatable("dcuniverse.guia.ficha.unidades"), SUAVE, 0);
        out.add(espaco(4));
        out.add(secao(topicos, "dcuniverse.guia.secao.energia"));
        out.add(item(Component.translatable("dcuniverse.guia.recarga.base", fmt(p.energia().regen()))));
        for (Personagem.Tipo r : p.energia().recargas()) {
            out.add(item(Component.translatable("dcuniverse.guia.recarga." + r.tipo(), fmt(r.params().d("per_second", 0)))));
        }
        if (!p.passivas().isEmpty()) {
            out.add(espaco(4));
            out.add(secao(topicos, "dcuniverse.guia.secao.passivas"));
            for (Personagem.Tipo t : p.passivas()) {
                out.add(item(Component.translatable("dcuniverse.guia.passiva." + t.tipo())));
                if (t.tipo().equals("flight")) {
                    double max = t.params().d("speed", 12);
                    texto(out, Component.translatable("dcuniverse.guia.voo.detalhe", fmt(max * Voador.CRUZEIRO), fmt(max),
                        fmt(t.params().d("dash_cost", 15))), SUAVE, 2);
                } else if (t.tipo().equals("glide")) {
                    double v = t.params().d("speed", 10);
                    texto(out, Component.translatable("dcuniverse.guia.planar.detalhe", fmt(v), fmt(v * Planador.MAX_ABERTA),
                        fmt(v * Planador.MAX_MERGULHO), fmt(t.params().d("boost_cost", Planar.IMPULSO_CUSTO))), SUAVE, 2);
                } else if (t.tipo().equals("super_jump")) {
                    texto(out, Component.translatable("dcuniverse.guia.salto.detalhe", fmt(alturaSalto(t.params().d("power", 2))),
                        fmt(t.params().d("charge", 1))), SUAVE, 2);
                } else if (t.tipo().equals("stealth")) {
                    texto(out, Component.translatable("dcuniverse.guia.furtividade.detalhe", t.params().i("max_light", 7),
                        Math.round((1 - t.params().d("visibility", 0.3)) * 100)), SUAVE, 2);
                } else if (t.tipo().equals("counter")) {
                    texto(out, Component.translatable("dcuniverse.guia.contra.detalhe", fmt(t.params().d("window", 0.4)),
                        fmt(Alvos.dano(p, t.params().d("damage_mult", 1.5)) / 2.0), fmt(t.params().d("stun", 1.5)), fmt(t.params().d("cooldown", 1))), SUAVE, 2);
                } else if (t.tipo().equals("projectile_resistance")) {
                    texto(out, Component.translatable("dcuniverse.guia.ricochete.detalhe", fmt(t.params().d("max_damage", 10) / 2)), SUAVE, 2);
                }
            }
        }
        List<Personagem.Habilidade> normais = p.habilidades().stream().filter(h -> !h.suprema()).toList();
        if (!normais.isEmpty()) {
            out.add(espaco(4));
            out.add(secao(topicos, "dcuniverse.guia.secao.habilidades"));
            for (Personagem.Habilidade h : normais) habilidade(out, topicos, p, h);
        }
        // Com cinto pode haver várias (os veículos do Batman, escolhidos na Garagem): mostra todas.
        List<Personagem.Habilidade> supremas = p.habilidades().stream().filter(Personagem.Habilidade::suprema).toList();
        if (!supremas.isEmpty()) {
            out.add(espaco(4));
            out.add(secao(topicos, "dcuniverse.guia.secao.suprema"));
        }
        for (Personagem.Habilidade sup : supremas) {
            if (sup != supremas.get(0)) out.add(espaco(3));
            int pct = Math.round(sup.custo() * 100f / p.energia().max());
            Component nomeSup = Component.translatable("habilidade.dcuniverse." + sup.id());
            out.add(topico(topicos, nome(nomeSup), nomeSup, true));
            texto(out, Component.translatable("habilidade.dcuniverse." + sup.id() + ".desc"), TINTA, 1);
            out.add(item(Component.translatable("dcuniverse.guia.suprema.custo", pct, fmt(sup.recargaSeg()))));
            detalhes(out, sup.params());
        }
        out.add(espaco(4));
        out.add(secao(topicos, "dcuniverse.guia.secao.historia"));
        texto(out, Component.translatable(chave + ".lore"), TINTA, 0);
        return out;
    }

    /** Nome, o que faz e os números: como usar e quanto gasta, recarga, alcance e dano em corações. */
    private void habilidade(List<Bloco> out, List<Topico> topicos, Personagem p, Personagem.Habilidade h) {
        Params prm = h.params();
        Component nomeHab = Component.translatable("habilidade.dcuniverse." + h.id());
        out.add(topico(topicos, nome(nomeHab), nomeHab, true));
        texto(out, Component.translatable("habilidade.dcuniverse." + h.id() + ".desc"), TINTA, 1);
        boolean segura = h.modo() == Personagem.Modo.HOLD;
        out.add(item(Component.translatable(segura ? "dcuniverse.guia.hab.segurar" : "dcuniverse.guia.hab.apertar", fmt(h.custo()))));
        Component recarga = Component.translatable("dcuniverse.guia.hab.recarga", fmt(h.recargaSeg()));
        out.add(item(prm.json().has("range")
            ? Component.translatable("dcuniverse.guia.hab.recarga_alcance", fmt(h.recargaSeg()), fmt(prm.d("range", 0)))
            : recarga));
        if (prm.json().has("damage_mult")) {
            // Raio e cone acertam a cada 10 ticks com metade do dano: o dano cheio por segundo.
            double coracoes = Alvos.dano(p, prm.d("damage_mult", 0.5)) / 2.0;
            out.add(item(Component.translatable(segura ? "dcuniverse.guia.hab.dano_seg" : "dcuniverse.guia.hab.dano", fmt(coracoes))));
        }
        if (prm.d("impact_radius", 0) > 0) {
            out.add(item(Component.translatable("dcuniverse.guia.hab.impacto", fmt(prm.d("impact_radius", 0)))));
        }
        if (h.tipo().equals("grapple")) out.add(item(Component.translatable("dcuniverse.guia.hab.gancho", fmt(prm.d("speed", 30)))));
        if (prm.i("ricochet", 0) > 0) out.add(item(Component.translatable("dcuniverse.guia.hab.ricochete", prm.i("ricochet", 0))));
        if (prm.s("variant", "").equals("electric")) out.add(item(Component.translatable("dcuniverse.guia.hab.choque")));
        Params ammo = com.danrod.dcuniverse.ability.Cargas.ammo(h);
        if (ammo != null) {
            out.add(item(Component.translatable("dcuniverse.guia.hab.cargas", ammo.i("max", 1), fmt(ammo.d("reload", 5)), fmt(ammo.d("reload_cost", 0)))));
        }
        detalhes(out, prm);
        out.add(espaco(3));
    }

    /** Raio, duração e efeitos de controle, quando a habilidade tem. */
    private void detalhes(List<Bloco> out, Params prm) {
        if (prm.json().has("radius")) out.add(item(Component.translatable("dcuniverse.guia.hab.raio", fmt(prm.d("radius", 0)))));
        if (prm.json().has("duration")) out.add(item(Component.translatable("dcuniverse.guia.hab.duracao", fmt(prm.d("duration", 0)))));
        if (prm.json().has("missiles")) {
            out.add(item(Component.translatable("dcuniverse.guia.hab.batwing.voo", fmt(prm.d("speed", 30)), fmt(prm.d("boost_speed", 60)))));
            out.add(item(Component.translatable("dcuniverse.guia.hab.batwing.armas", fmt(prm.d("gun_damage", 1)), fmt(prm.d("gun_range", 64)),
                prm.i("missiles", 4), fmt(prm.d("missile_damage", 7)), fmt(prm.d("missile_radius", 3.5)), fmt(prm.d("missile_reload", 5)))));
        }
        if (prm.s("model", "").equals("nave_kryptoniana")) {
            out.add(item(Component.translatable("dcuniverse.guia.hab.batwing.voo", fmt(prm.d("speed", 34)), fmt(prm.d("boost_speed", 72)))));
            out.add(item(Component.translatable("dcuniverse.guia.hab.nave.canhao", fmt(prm.d("beam_damage", 2.5)), fmt(prm.d("beam_range", 48)))));
            out.add(item(Component.translatable("dcuniverse.guia.hab.nave.orbes", prm.i("orbs", 3), fmt(prm.d("orb_damage", 8)),
                fmt(prm.d("orb_radius", 3.5)), fmt(prm.d("orb_reload", 6)))));
            out.add(item(Component.translatable("dcuniverse.guia.hab.nave.escudo", fmt(prm.d("shield_time", 12)), fmt(prm.d("shield_recharge", 18)))));
        } else if (prm.json().has("model") && !prm.s("model", "").equals("batwing")) {
            String m = prm.s("model", "");
            out.add(item(Component.translatable("dcuniverse.guia.hab.veiculo.velocidade", fmt(prm.d("speed", 26)), fmt(prm.d("boost_speed", 42)))));
            out.add(item(Component.translatable("dcuniverse.guia.hab.veiculo.armas." + m, fmt(prm.d("gun_damage", 1)), fmt(prm.d("gun_range", 48)),
                prm.i("rounds", 4), fmt(prm.d("rounds_damage", 6)), fmt(prm.d("rounds_reload", 4)))));
            out.add(item(Component.translatable("dcuniverse.guia.hab.veiculo.atropelo", fmt(prm.d("ram_min_speed", 8)), fmt(prm.d("ram_damage", 6)))));
            out.add(item(Component.translatable("dcuniverse.guia.hab.veiculo.especial." + m, fmt(prm.d("special_radius", 5)),
                fmt(prm.d("special_damage", 4)), fmt(prm.d("stun", 3)), fmt(prm.d("special_cooldown", 12)), fmt(prm.d("special_duration", 6)),
                fmt(prm.d("dive_speed", 14)))));
        }
        if (prm.json().has("reduction")) {
            out.add(item(Component.translatable("dcuniverse.guia.hab.escudo_tk", Math.round(prm.d("reduction", 0.75) * 100), fmt(prm.d("shield_radius", 2.6)))));
        }
        if (prm.json().has("throw_speed")) {
            out.add(item(Component.translatable("dcuniverse.guia.hab.telecinese", prm.i("max_blocks", 18), fmt(prm.d("hold", 30)))));
        }
        if (prm.json().has("max_blocks") && prm.json().has("depth") && prm.json().has("speed")) {
            out.add(item(Component.translatable("dcuniverse.guia.hab.ilha", prm.i("max_blocks", 420), fmt(prm.d("speed", 8)))));
        }
        if (prm.b("breaks_soft", false)) {
            out.add(item(Component.translatable("dcuniverse.guia.hab.investida", fmt(prm.d("speed", 3.2) * 20), fmt(prm.d("knockback", 2)))));
        }
        if (prm.json().has("drain")) {
            out.add(item(Component.translatable("dcuniverse.guia.hab.dreno", fmt(prm.d("drain", 1.5)),
                Math.round((prm.d("weak_point_mult", 1.25) - 1) * 100))));
        }
        if (prm.json().has("blind")) {
            out.add(item(Component.translatable("dcuniverse.guia.hab.fumaca", fmt(prm.d("blind", 5)), fmt(prm.d("escape", 3)))));
        }
        if (prm.json().has("max_placed")) out.add(item(Component.translatable("dcuniverse.guia.hab.gel", prm.i("max_placed", 3))));
        if (prm.d("stun", 0) > 0) out.add(item(Component.translatable("dcuniverse.guia.hab.lento", fmt(Math.min(4, prm.d("stun", 0))))));
        if (prm.d("regen_level", 0) > 0) {
            out.add(item(Component.translatable("dcuniverse.guia.hab.cura", (int) prm.d("regen_level", 0), fmt(prm.d("energy_per_second", 0)))));
        }
    }


    // Índice de personagens ----------------------------------------------------

    private Filtro filtro(Personagem p) {
        return categorias.getOrDefault(p.id().toString(), Filtro.HEROI);
    }

    /** Personagens do filtro escolhido; a busca só acha os já descobertos (o nome dos outros ainda é segredo). */
    private List<Personagem> filtradosPers() {
        String q = ItensGuia.normalizar(busca.getValue());
        return personagens.stream()
            .filter(p -> ultimoFiltro == Filtro.TODOS || filtro(p) == ultimoFiltro)
            .filter(p -> q.isEmpty() || (descoberto(p) && ItensGuia.normalizar(Component.translatable(p.chaveNome()).getString()).contains(q)))
            .toList();
    }

    private int maxRolagemPers() {
        int linhas = (filtradosPers().size() + COLUNAS - 1) / COLUNAS;
        return Math.max(0, linhas - GRADE_LINHAS);
    }

    /** Rosto do personagem (cabeça da skin, com a camada de fora); quem não foi descoberto aparece em silhueta. */
    private void rosto(GuiGraphicsExtractor g, Personagem p, int x, int y, int tam) {
        PlayerSkin skin = descoberto(p) ? Skins.skinDoPersonagem(p.id().toString()) : null;
        PlayerFaceExtractor.extractRenderState(g, skin != null ? skin : SKIN_SILHUETA, x, y, tam);
    }

    /** Ícone de lista (índice ou sumário) clicável, com dica. */
    private void botaoLista(GuiGraphicsExtractor g, int x, int y, String dica, Runnable acao) {
        boolean em = sobre(mouseX, mouseY, x - 1, y - 1, 14, 14);
        if (em) {
            g.fill(x - 1, y - 1, x + 13, y + 13, 0x40B0841E);
            g.setTooltipForNextFrame(Component.translatable(dica), mouseX, mouseY);
        }
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, x, y, 336, 96, 12, 12, TEX_W, TEX_H);
        alvos.add(new Alvo(x - 1, y - 1, 14, 14, ItemStack.EMPTY, acao));
    }

    /** Campo de busca: papel mais escuro com moldura de tinta e a lupa (o EditBox fica por cima). */
    private void campoBusca(GuiGraphicsExtractor g, int x) {
        int by = y0 + BUSCA_Y;
        boolean foco = busca.isFocused();
        g.fill(x, by, x + PAG_W, by + 15, foco ? 0xFF7A5C34 : 0xFF9C8460);
        g.fill(x + 1, by + 1, x + PAG_W - 1, by + 14, 0xFFF8EFD8);
        g.fill(x + 1, by + 1, x + PAG_W - 1, by + 2, 0x30000000);
        lupa(g, x + 3, by + 3);
    }

    /** Barra de rolagem fininha na margem da página, quando a grade tem mais linhas do que cabem. */
    private void barraRolagem(GuiGraphicsExtractor g, int x, int gy0, int linhas, int rol) {
        if (linhas <= GRADE_LINHAS) return;
        int bx = x + PAG_W + 3, alt = GRADE_LINHAS * PASSO - 2;
        g.fill(bx, gy0, bx + 2, gy0 + alt, 0x40000000);
        int tam = Math.max(8, alt * GRADE_LINHAS / linhas);
        int pos = (alt - tam) * rol / Math.max(1, linhas - GRADE_LINHAS);
        g.fill(bx, gy0 + pos, bx + 2, gy0 + pos + tam, OURO);
    }

    /** Página esquerda do índice: busca, filtros por categoria e a grade de rostos (clique abre a ficha). */
    private void listaEsquerda(GuiGraphicsExtractor g, int mx, int my) {
        int x = x0 + ESQ_X;
        titulo(g, Component.translatable("dcuniverse.guia.capitulo.personagens"), x, y0 + TOPO);
        campoBusca(g, x);
        Filtro[] filtros = Filtro.values();
        int passo = PAG_W / filtros.length;
        for (int i = 0; i < filtros.length; i++) {
            Filtro f = filtros[i];
            int fx = x + i * passo + (passo - 18) / 2, fy = y0 + GRUPOS_Y;
            boolean ativo = f == ultimoFiltro;
            g.blit(RenderPipelines.GUI_TEXTURED, TEX, fx, fy, ativo ? 324 : 304, 140, 18, 18, TEX_W, TEX_H);
            g.blit(RenderPipelines.GUI_TEXTURED, TEX, fx + 3, fy + 3, 336, f.icone, 12, 12, TEX_W, TEX_H);
            long n = f == Filtro.TODOS ? personagens.size() : personagens.stream().filter(p -> filtro(p) == f).count();
            if (n == 0 && !ativo) g.fill(fx + 1, fy + 1, fx + 17, fy + 17, 0x90E8DABA);
            if (sobre(mx, my, fx, fy, 18, 18)) {
                g.setTooltipForNextFrame(Component.translatable(f.chave()).append(Component.literal(" (" + n + ")").withStyle(ChatFormatting.GRAY)), mx, my);
            }
            alvos.add(new Alvo(fx, fy, 18, 18, ItemStack.EMPTY, () -> {
                if (ultimoFiltro != f) {
                    ultimoFiltro = f;
                    rolagemPers = 0;
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                }
            }));
        }
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, x + PAG_W / 2 - 40, y0 + GRUPOS_Y + 19, 304, 116, 80, 5, TEX_W, TEX_H);
        List<Personagem> lista = filtradosPers();
        rolagemPers = Mth.clamp(rolagemPers, 0, maxRolagemPers());
        int gx0 = x + (PAG_W - COLUNAS * PASSO) / 2 + 1, gy0 = y0 + GRADE_Y;
        Personagem aberto = atual();
        for (int i = rolagemPers * COLUNAS; i < Math.min(lista.size(), (rolagemPers + GRADE_LINHAS) * COLUNAS); i++) {
            Personagem p = lista.get(i);
            int k = i - rolagemPers * COLUNAS;
            int sx = gx0 + (k % COLUNAS) * PASSO, sy = gy0 + (k / COLUNAS) * PASSO;
            g.blit(RenderPipelines.GUI_TEXTURED, TEX, sx, sy, p == aberto ? 324 : 304, 140, 18, 18, TEX_W, TEX_H);
            rosto(g, p, sx + 1, sy + 1, 16);
            if (sobre(mx, my, sx, sy, 18, 18)) {
                g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0x40FFFFFF);
                g.setTooltipForNextFrame(font, dicaPersonagem(p), java.util.Optional.empty(), mx, my);
            }
            alvos.add(new Alvo(sx, sy, 18, 18, ItemStack.EMPTY, () -> abrirFicha(p)));
        }
        barraRolagem(g, x, gy0, (lista.size() + COLUNAS - 1) / COLUNAS, rolagemPers);
        long vistos = personagens.stream().filter(this::descoberto).count();
        Component rodape = lista.isEmpty()
            ? Component.translatable("dcuniverse.guia.personagens.nenhum")
            : Component.translatable("dcuniverse.guia.personagens.contagem", vistos, personagens.size());
        g.centeredText(font, rodape, x + PAG_W / 2, y0 + BASE - 7, SUAVE);
    }

    private List<Component> dicaPersonagem(Personagem p) {
        boolean visto = descoberto(p);
        List<Component> linhas = new ArrayList<>();
        linhas.add(visto ? Component.translatable(p.chaveNome()) : Component.literal("???"));
        linhas.add(Component.translatable("dcuniverse.guia.personagens.categoria." + filtro(p).name().toLowerCase(Locale.ROOT))
            .withStyle(ChatFormatting.GRAY));
        linhas.add(visto ? Component.translatable("dcuniverse.guia.personagem.nivel", p.nivel()).withStyle(ChatFormatting.GRAY)
            : Component.translatable("dcuniverse.guia.personagem.desconhecido").withStyle(ChatFormatting.GRAY));
        linhas.add(Component.translatable("dcuniverse.guia.personagens.abrir").withStyle(ChatFormatting.DARK_AQUA));
        return linhas;
    }

    /** Página direita do índice: os nomes por categoria, como o índice de um livro (clique abre a ficha). */
    private List<Bloco> listaDireita() {
        List<Bloco> out = new ArrayList<>();
        List<Personagem> lista = filtradosPers();
        if (lista.isEmpty()) {
            texto(out, Component.translatable("dcuniverse.guia.personagens.nenhum"), SUAVE, 0);
            return out;
        }
        for (Filtro f : Filtro.values()) {
            if (f == Filtro.TODOS) continue;
            List<Personagem> doGrupo = lista.stream().filter(p -> filtro(p) == f).toList();
            if (doGrupo.isEmpty()) continue;
            if (!out.isEmpty()) out.add(espaco(3));
            out.add(secao("dcuniverse.guia.personagens.filtro." + f.name().toLowerCase(Locale.ROOT)));
            for (Personagem p : doGrupo) {
                out.add(new Bloco(11, (g, x, y) -> {
                    boolean visto = descoberto(p);
                    boolean em = sobre(mouseX, mouseY, x, y - 1, PAG_W, 11);
                    rosto(g, p, x, y, 8);
                    String nv = visto ? String.valueOf(p.nivel()) : "";
                    int nvX = x + PAG_W - font.width(nv);
                    String nome = visto ? cortar(Component.translatable(p.chaveNome()).getString(), nvX - x - 16) : "???";
                    Component c = Component.literal(nome);
                    if (em) c = c.copy().withStyle(ChatFormatting.UNDERLINE);
                    g.text(font, c, x + 11, y, em ? TITULO : visto ? TINTA : SUAVE, false);
                    if (visto) g.text(font, nv, nvX, y, SUAVE, false);
                    if (em) g.setTooltipForNextFrame(font, dicaPersonagem(p), java.util.Optional.empty(), mouseX, mouseY);
                    alvos.add(new Alvo(x, y - 1, PAG_W, 11, ItemStack.EMPTY, () -> abrirFicha(p)));
                }));
            }
        }
        return out;
    }


    // Itens e Receitas ---------------------------------------------------------

    private static final int BUSCA_Y = TOPO + 21, GRUPOS_Y = TOPO + 39, GRADE_Y = TOPO + 64, PASSO = 20, COLUNAS = 6, GRADE_LINHAS = 4;

    /** Itens do grupo escolhido cujo nome ou texto tem a busca (sem diferença de acento ou maiúscula). */
    private List<ItensGuia.Entrada> filtrados() {
        String q = ItensGuia.normalizar(busca.getValue());
        return ItensGuia.itens.stream()
            .filter(e -> ultimoGrupo == ItensGuia.Grupo.TODOS || e.grupo() == ultimoGrupo)
            .filter(e -> q.isEmpty() || ItensGuia.normalizar(e.stack().getHoverName().getString()).contains(q)
                || ItensGuia.normalizar(Component.translatable("dcuniverse.guia.item." + e.id()).getString()).contains(q))
            .toList();
    }

    private int maxRolagem() {
        int linhas = (filtrados().size() + COLUNAS - 1) / COLUNAS;
        return Math.max(0, linhas - GRADE_LINHAS);
    }

    private ItensGuia.Entrada itemAtual() {
        ItensGuia.Entrada e = ItensGuia.itens.stream().filter(x -> x.id().equals(ultimoItem)).findFirst().orElse(null);
        if (e == null && !ItensGuia.itens.isEmpty()) {
            e = ItensGuia.itens.getFirst();
            ultimoItem = e.id();
        }
        return e;
    }

    private void abrir(String id, boolean guardar) {
        if (id.equals(ultimoItem)) return;
        if (guardar && !ultimoItem.isEmpty()) historico.push(ultimoItem);
        ultimoItem = id;
        pagina = 0;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
        // Rola a grade até o item, se ele está nela.
        List<ItensGuia.Entrada> lista = filtrados();
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).id().equals(id)) {
                int linha = i / COLUNAS;
                if (linha < rolagem) rolagem = linha;
                if (linha >= rolagem + GRADE_LINHAS) rolagem = linha - GRADE_LINHAS + 1;
            }
        }
        atualizarBotoes();
    }

    private void voltar() {
        if (!historico.isEmpty()) abrir(historico.pop(), false);
    }

    private void itensEsquerda(GuiGraphicsExtractor g, int mx, int my) {
        int x = x0 + ESQ_X;
        titulo(g, Component.translatable("dcuniverse.guia.capitulo.itens"), x, y0 + TOPO);
        campoBusca(g, x);
        // Marcadores de categoria: um ícone por grupo; o escolhido fica no encaixe dourado.
        ItensGuia.Grupo[] grupos = ItensGuia.Grupo.values();
        int passoGrupo = PAG_W / grupos.length;
        for (int i = 0; i < grupos.length; i++) {
            ItensGuia.Grupo gr = grupos[i];
            int gx = x + i * passoGrupo + (passoGrupo - 18) / 2, gy = y0 + GRUPOS_Y;
            boolean ativo = gr == ultimoGrupo;
            g.blit(RenderPipelines.GUI_TEXTURED, TEX, gx, gy, ativo ? 324 : 304, 140, 18, 18, TEX_W, TEX_H);
            if (gr == ItensGuia.Grupo.TODOS) {
                g.blit(RenderPipelines.GUI_TEXTURED, TEX, gx + 3, gy + 3, 336, 32, 12, 12, TEX_W, TEX_H);
            } else {
                ItemStack icone = icone(gr);
                if (!icone.isEmpty()) g.item(icone, gx + 1, gy + 1);
            }
            long n = gr == ItensGuia.Grupo.TODOS ? ItensGuia.itens.size() : ItensGuia.itens.stream().filter(e -> e.grupo() == gr).count();
            if (n == 0 && !ativo) g.fill(gx + 1, gy + 1, gx + 17, gy + 17, 0x90E8DABA);
            if (sobre(mx, my, gx, gy, 18, 18)) {
                g.setTooltipForNextFrame(Component.translatable(gr.chave()).append(Component.literal(" (" + n + ")").withStyle(ChatFormatting.GRAY)), mx, my);
            }
            final int fx = gx, fy = gy;
            alvos.add(new Alvo(fx, fy, 18, 18, ItemStack.EMPTY, () -> {
                if (ultimoGrupo != gr) {
                    ultimoGrupo = gr;
                    rolagem = 0;
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                }
            }));
        }
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, x + PAG_W / 2 - 40, y0 + GRUPOS_Y + 19, 304, 116, 80, 5, TEX_W, TEX_H);
        // Grade de itens com rolagem.
        List<ItensGuia.Entrada> lista = filtrados();
        rolagem = Mth.clamp(rolagem, 0, maxRolagem());
        int gx0 = x + (PAG_W - COLUNAS * PASSO) / 2 + 1, gy0 = y0 + GRADE_Y;
        for (int i = rolagem * COLUNAS; i < Math.min(lista.size(), (rolagem + GRADE_LINHAS) * COLUNAS); i++) {
            ItensGuia.Entrada e = lista.get(i);
            int k = i - rolagem * COLUNAS;
            encaixe(g, gx0 + (k % COLUNAS) * PASSO, gy0 + (k / COLUNAS) * PASSO, e.stack(), e.id().equals(ultimoItem),
                () -> {
                    historico.clear(); // escolher na grade começa uma leitura nova; só os links guardam o caminho
                    abrir(e.id(), false);
                });
        }
        barraRolagem(g, x, gy0, (lista.size() + COLUNAS - 1) / COLUNAS, rolagem);
        Component rodape = lista.isEmpty()
            ? Component.translatable("dcuniverse.guia.itens.vazio")
            : Component.translatable("dcuniverse.guia.itens.contagem", lista.size());
        g.centeredText(font, rodape, x + PAG_W / 2, y0 + BASE - 7, SUAVE);
    }

    private ItemStack icone(ItensGuia.Grupo gr) {
        return ItensGuia.itens.stream().filter(e -> e.id().equals(gr.icone)).map(ItensGuia.Entrada::stack).findFirst()
            .orElseGet(() -> ItensGuia.itens.stream().filter(e -> e.grupo() == gr).map(ItensGuia.Entrada::stack).findFirst().orElse(ItemStack.EMPTY));
    }

    /** Lupa de tinta (9x9) no campo de busca. */
    private void lupa(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x + 1, y, x + 5, y + 1, TINTA);
        g.fill(x + 1, y + 5, x + 5, y + 6, TINTA);
        g.fill(x, y + 1, x + 1, y + 5, TINTA);
        g.fill(x + 5, y + 1, x + 6, y + 5, TINTA);
        g.fill(x + 1, y + 1, x + 5, y + 5, 0x405FA8D8);
        g.fill(x + 5, y + 5, x + 7, y + 7, TINTA);
        g.fill(x + 6, y + 6, x + 9, y + 9, TINTA);
    }

    /**
     * Encaixe com item: clique abre a página dele (se for do mod) e o mouse por cima mostra a dica do jogo.
     * Ingrediente com várias opções (tag) alterna entre elas a cada segundo.
     */
    private void encaixe(GuiGraphicsExtractor g, int x, int y, ItemStack stack, boolean ativo, Runnable acao) {
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, x, y, ativo ? 324 : 304, 140, 18, 18, TEX_W, TEX_H);
        if (stack.isEmpty()) return;
        g.item(stack, x + 1, y + 1);
        if (stack.getCount() > 1) g.itemDecorations(font, stack, x + 1, y + 1);
        boolean doMod = ItensGuia.entrada(stack.getItem()) != null;
        if (sobre(mouseX, mouseY, x, y, 18, 18)) {
            g.fill(x + 1, y + 1, x + 17, y + 17, 0x40FFFFFF);
            List<Component> linhas = new ArrayList<>(Screen.getTooltipFromItem(minecraft, stack));
            if (doMod && acao != null && !stack.is(itemAtualOuVazio())) {
                linhas.add(Component.translatable("dcuniverse.guia.itens.abrir").withStyle(ChatFormatting.DARK_AQUA));
            }
            g.setTooltipForNextFrame(font, linhas, stack.getTooltipImage(), mouseX, mouseY);
        }
        if (doMod && acao != null) alvos.add(new Alvo(x, y, 18, 18, stack, acao));
    }

    private net.minecraft.world.item.Item itemAtualOuVazio() {
        ItensGuia.Entrada e = itemAtual();
        return e == null ? net.minecraft.world.item.Items.AIR : e.stack().getItem();
    }

    private boolean cliqueItens(double mx, double my) {
        for (Alvo a : List.copyOf(alvos)) {
            if (sobre(mx, my, a.x(), a.y(), a.w(), a.h())) {
                a.acao().run();
                return true;
            }
        }
        return false;
    }

    private Runnable link(ItemStack s) {
        ItensGuia.Entrada e = ItensGuia.entrada(s.getItem());
        return e == null ? null : () -> abrir(e.id(), true);
    }

    /** Página do item: cabeçalho, o que é, receitas, como obter e onde é usado. */
    private List<Bloco> itemDireita() {
        List<Bloco> out = new ArrayList<>();
        ItensGuia.Entrada e = itemAtual();
        if (e == null) {
            texto(out, Component.translatable("dcuniverse.guia.itens.vazio"), SUAVE, 0);
            return out;
        }
        ItemStack stack = e.stack();
        Component nome = stack.getHoverName().copy().withStyle(ChatFormatting.BOLD);
        List<FormattedCharSequence> linhasNome = font.split(nome, PAG_W - 40);
        Component grupo = Component.translatable(e.grupo().chave());
        boolean volta = !historico.isEmpty();
        out.add(new Bloco(Math.max(40, linhasNome.size() * 10 + (volta ? 26 : 16)), (g, x, y) -> {
            g.pose().pushMatrix();
            g.pose().translate(x, y);
            g.pose().scale(2f, 2f);
            g.blit(RenderPipelines.GUI_TEXTURED, TEX, 0, 0, 324, 140, 18, 18, TEX_W, TEX_H);
            g.item(stack, 1, 1);
            g.pose().popMatrix();
            int yy = y + 3;
            for (FormattedCharSequence l : linhasNome) {
                g.text(font, l, x + 40, yy, TITULO, false);
                yy += 10;
            }
            g.text(font, grupo, x + 40, yy + 1, SUAVE, false);
            if (volta) {
                // Link de volta ao item de onde o jogador veio (Backspace também volta).
                String curto = Component.translatable("dcuniverse.guia.itens.voltar_link").getString();
                int vy = yy + 11;
                boolean em = sobre(mouseX, mouseY, x + 40, vy - 1, font.width(curto), 10);
                g.text(font, Component.literal(curto).withStyle(em ? ChatFormatting.UNDERLINE : ChatFormatting.RESET), x + 40, vy, em ? TITULO : 0xFF2E5A8A, false);
                if (em) g.setTooltipForNextFrame(Component.translatable("dcuniverse.guia.itens.voltar"), mouseX, mouseY);
                alvos.add(new Alvo(x + 40, vy - 1, font.width(curto), 10, ItemStack.EMPTY, this::voltar));
            }
        }));
        // A receita vem logo depois do nome: é o que mais se procura aqui.
        List<ItensGuia.Receita> receitas = ItensGuia.comoFazer(stack.getItem());
        if (!receitas.isEmpty()) {
            out.add(secao(receitas.size() > 1 ? "dcuniverse.guia.itens.receitas" : "dcuniverse.guia.itens.receita"));
            for (ItensGuia.Receita r : receitas) out.add(receita(r));
        }
        out.add(secao("dcuniverse.guia.itens.sobre"));
        texto(out, Component.translatable("dcuniverse.guia.item." + e.id()), TINTA, 4);
        if (e.obter()) {
            out.add(secao("dcuniverse.guia.itens.obter"));
            texto(out, Component.translatable("dcuniverse.guia.item." + e.id() + ".obter"), TINTA, 4);
        }
        List<ItemStack> usos = ItensGuia.usadoEm(stack.getItem());
        out.add(secao("dcuniverse.guia.itens.usado_em"));
        if (usos.isEmpty()) {
            texto(out, Component.translatable("dcuniverse.guia.itens.sem_uso"), SUAVE, 0);
        } else {
            for (int i = 0; i < usos.size(); i += COLUNAS) {
                List<ItemStack> linha = usos.subList(i, Math.min(usos.size(), i + COLUNAS));
                out.add(new Bloco(PASSO, (g, x, y) -> {
                    for (int k = 0; k < linha.size(); k++) encaixe(g, x + k * PASSO, y, linha.get(k), false, link(linha.get(k)));
                }));
            }
        }
        return out;
    }

    /** Receita desenhada como na bancada (3x3, seta e resultado) ou na fornalha (ingrediente, chama, seta). */
    private Bloco receita(ItensGuia.Receita r) {
        Component rotulo = Component.translatable("dcuniverse.guia.itens.tipo." + r.tipo().name().toLowerCase(Locale.ROOT));
        if (r.tipo() == ItensGuia.Tipo.FORNALHA) {
            return new Bloco(50, (g, x, y) -> {
                ItemStack in = r.grade().getFirst().agora();
                encaixe(g, x + 18, y, in, false, link(in));
                g.blit(RenderPipelines.GUI_TEXTURED, TEX, x + 20, y + 20, 368, 141, 13, 14, TEX_W, TEX_H);
                g.blit(RenderPipelines.GUI_TEXTURED, TEX, x + 44, y + 10, 344, 141, 22, 15, TEX_W, TEX_H);
                encaixe(g, x + 72, y + 9, r.resultado(), true, link(r.resultado()));
                g.text(font, rotulo, x, y + 37, SUAVE, false);
            });
        }
        return new Bloco(68, (g, x, y) -> {
            for (int i = 0; i < 9; i++) {
                ItensGuia.Ingrediente ing = r.grade().get(i);
                ItemStack s = ing == null ? ItemStack.EMPTY : ing.agora();
                encaixe(g, x + (i % 3) * 18, y + (i / 3) * 18, s, false, link(s));
            }
            g.blit(RenderPipelines.GUI_TEXTURED, TEX, x + 59, y + 19, 344, 141, 22, 15, TEX_W, TEX_H);
            encaixe(g, x + 87, y + 18, r.resultado(), true, link(r.resultado()));
            g.text(font, font.plainSubstrByWidth(rotulo.getString(), PAG_W), x, y + 57, SUAVE, false);
        });
    }

    /** Altura aproximada do super-salto com a carga cheia (mesma física do pulo: gravidade 0,08 e arrasto 0,98). */
    static double alturaSalto(double vel) {
        double y = 0;
        for (int i = 0; i < 200 && vel > 0; i++) {
            y += vel;
            vel = (vel - 0.08) * 0.98;
        }
        return Math.round(y);
    }

    private List<List<Bloco>> paginasDireita() {
        return switch (capitulo) {
            case PASSOS -> paginar(passosDireita());
            case PERSONAGENS -> naLista ? paginar(listaDireita()) : fichaPaginas();
            case ITENS -> paginar(itemDireita());
            case MISSOES -> paginar(missaoDireita());
            case NPCS -> paginar(npcDireita());
        };
    }

    /**
     * Ficha com sumário: as primeiras páginas listam os tópicos (seções e cada habilidade) com o número da página
     * onde começam; clicar num deles vira direto para lá.
     */
    private List<List<Bloco>> fichaPaginas() {
        List<Topico> topicos = new ArrayList<>();
        List<List<Bloco>> conteudo = paginar(personagemDireita(topicos));
        paginasSumario = 0;
        blocoDestaque = destaque >= 0 && destaque < topicos.size() ? topicos.get(destaque).bloco() : null;
        if (topicos.isEmpty() || conteudo.size() < 2) return conteudo;
        int[] onde = new int[topicos.size()];
        for (int i = 0; i < topicos.size(); i++) {
            for (int k = 0; k < conteudo.size(); k++) {
                if (conteudo.get(k).contains(topicos.get(i).bloco())) onde[i] = k;
            }
        }
        // O sumário tem altura fixa por linha: conta as páginas dele antes de numerar.
        paginasSumario = paginar(sumario(topicos, onde, 0)).size();
        List<List<Bloco>> out = new ArrayList<>(paginar(sumario(topicos, onde, paginasSumario)));
        out.addAll(conteudo);
        return out;
    }

    private List<Bloco> sumario(List<Topico> topicos, int[] onde, int desloc) {
        List<Bloco> out = new ArrayList<>();
        out.add(secao("dcuniverse.guia.secao.sumario"));
        for (int i = 0; i < topicos.size(); i++) {
            Topico t = topicos.get(i);
            int destino = onde[i] + desloc, qual = i;
            String num = String.valueOf(destino + 1);
            boolean secaoNova = !t.sub() && i > 0;
            out.add(new Bloco(secaoNova ? 12 : 10, (g, x, y) -> {
                int yy = y + (secaoNova ? 2 : 0);
                int recuo = t.sub() ? 8 : 0;
                int numX = x + PAG_W - font.width(num);
                String nome = cortar(t.nome().getString(), numX - x - recuo - 8);
                boolean em = sobre(mouseX, mouseY, x, yy - 1, PAG_W, 10);
                int cor = em ? TITULO : t.sub() ? TINTA : TITULO;
                Component c = Component.literal(nome).withStyle(t.sub() ? ChatFormatting.RESET : ChatFormatting.BOLD);
                if (em) c = c.copy().withStyle(ChatFormatting.UNDERLINE);
                g.text(font, c, x + recuo, yy, cor, false);
                // Pontilhado até o número, como no sumário de um livro.
                for (int px = x + recuo + font.width(c) + 3; px < numX - 3; px += 3) g.fill(px, yy + 6, px + 1, yy + 7, 0x80B0841E);
                g.text(font, num, numX, yy, em ? TITULO : SUAVE, false);
                alvos.add(new Alvo(x, yy - 1, PAG_W, 10, ItemStack.EMPTY, () -> {
                    destaque = qual;
                    destaqueAte = System.currentTimeMillis() + 1800;
                    virar(destino);
                }));
            }));
        }
        return out;
    }

    private void virar(int p) {
        if (p == pagina) return;
        pagina = p;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
        atualizarBotoes();
    }

    private List<List<Bloco>> paginar(List<Bloco> blocos) {
        List<List<Bloco>> paginas = new ArrayList<>();
        List<Bloco> atual = new ArrayList<>();
        int livre = BASE - TOPO - 14, usado = 0;
        for (int i = 0; i < blocos.size(); i++) {
            Bloco b = blocos.get(i);
            // Títulos seguidos levam junto o primeiro bloco depois deles.
            int junto = b.altura;
            for (int j = i; j < blocos.size() && blocos.get(j).preso() && j + 1 < blocos.size(); j++) {
                junto += blocos.get(j + 1).altura;
            }
            if (usado + junto > livre && !atual.isEmpty()) {
                paginas.add(atual);
                atual = new ArrayList<>();
                usado = 0;
            }
            atual.add(b);
            usado += b.altura;
        }
        if (!atual.isEmpty()) paginas.add(atual);
        return paginas;
    }

    // Blocos da página direita ---------------------------------------------------

    interface Desenho {
        void desenhar(GuiGraphicsExtractor g, int x, int y);
    }

    /** preso: título que não pode ficar sozinho no fim da página (vai junto com o bloco seguinte). */
    record Bloco(int altura, Desenho desenho, boolean preso) {
        Bloco(int altura, Desenho desenho) {
            this(altura, desenho, false);
        }
    }

    /** Parágrafo: uma linha por bloco, para textos longos (como a história) continuarem na página seguinte. */
    private void texto(List<Bloco> out, Component c, int cor, int depois) {
        List<FormattedCharSequence> linhas = font.split(c, PAG_W);
        for (int i = 0; i < linhas.size(); i++) {
            FormattedCharSequence l = linhas.get(i);
            out.add(new Bloco(10 + (i == linhas.size() - 1 ? depois : 0), (g, x, y) -> g.text(font, l, x, y, cor, false)));
        }
    }

    /** Título em negrito que quebra em várias linhas quando não cabe na página. */
    private void titulos(List<Bloco> out, Component c) {
        List<FormattedCharSequence> linhas = font.split(c.copy().withStyle(ChatFormatting.BOLD), PAG_W);
        for (FormattedCharSequence l : linhas) out.add(new Bloco(10, (g, x, y) -> g.text(font, l, x, y, TITULO, false), true));
    }

    private Bloco nome(Component c) {
        Component negrito = c.copy().withStyle(ChatFormatting.BOLD);
        return new Bloco(10, (g, x, y) -> g.text(font, negrito, x, y, TITULO, false), true);
    }

    private Bloco secao(String chave) {
        Component c = Component.translatable(chave).withStyle(ChatFormatting.BOLD);
        return new Bloco(12, (g, x, y) -> {
            g.text(font, c, x, y, TITULO, false);
            int fim = x + font.width(c) + 4;
            g.fill(fim, y + 4, x + PAG_W, y + 5, 0x55B0841E);
        }, true);
    }

    private Bloco item(Component c) {
        List<FormattedCharSequence> linhas = font.split(c, PAG_W - 8);
        return new Bloco(linhas.size() * 9 + 1, (g, x, y) -> {
            g.fill(x + 1, y + 3, x + 4, y + 6, OURO);
            for (FormattedCharSequence l : linhas) {
                g.text(font, l, x + 8, y, TINTA, false);
                y += 9;
            }
        });
    }

    private Bloco espaco(int h) {
        return new Bloco(h, (g, x, y) -> {});
    }

    /** Linha da ficha: nome, barra em segmentos (escala = maior valor entre os 100 personagens) e o número. */
    private Bloco atributo(String nome, double valor, double escala, String numero) {
        Component rotulo = Component.translatable("dcuniverse.guia.atributo." + nome);
        return new Bloco(11, (g, x, y) -> {
            g.text(font, rotulo, x, y, TINTA, false);
            int bx = x + 52, bw = 46;
            barra(g, bx, y + 1, bw, (float) (valor / escala), OURO);
            g.text(font, numero, x + PAG_W - font.width(numero), y, TINTA, false);
        });
    }

    /** Barra com moldura de tinta, fundo de papel e 10 segmentos preenchidos. */
    private void barra(GuiGraphicsExtractor g, int x, int y, int w, float frac, int cor) {
        frac = Mth.clamp(frac, 0f, 1f);
        g.fill(x, y, x + w, y + 7, 0xFF5A4630);
        g.fill(x + 1, y + 1, x + w - 1, y + 6, 0xFFE8DABA);
        int cheio = Math.round((w - 2) * frac);
        if (cheio > 0) {
            g.fill(x + 1, y + 1, x + 1 + cheio, y + 6, cor);
            g.fill(x + 1, y + 1, x + 1 + cheio, y + 2, 0x60FFFFFF);
            g.fill(x + 1, y + 5, x + 1 + cheio, y + 6, 0x40000000);
        }
        for (int i = 1; i < 10; i++) {
            int sx = x + 1 + Math.round((w - 2) * i / 10f);
            g.fill(sx, y + 1, sx + 1, y + 6, 0x30000000);
        }
    }

    /** Texto que não cabe na largura termina em reticências. */
    private String cortar(String s, int largura) {
        if (font.width(s) <= largura) return s;
        return font.plainSubstrByWidth(s, largura - font.width("...")).stripTrailing() + "...";
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.1f", v).replace('.', ',');
    }

    private static boolean sobre(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ---------------------------------------------------------------- mouse

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean duplo) {
        for (Capitulo c : Capitulo.values()) {
            if (sobre(e.x(), e.y(), x0 + W - 4, y0 + MARC_Y0 + c.ordinal() * MARC_PASSO, 26, 22)) {
                irPara(c);
                return true;
            }
        }
        if (e.button() == 0 && cliqueItens(e.x(), e.y())) {
            return true;
        }
        if (capitulo == Capitulo.PERSONAGENS && !naLista && e.button() == 0
            && sobre(e.x(), e.y(), x0 + ESQ_X + 10, y0 + PREVIA_Y, PAG_W - 20, PREVIA_H)) {
            giroBase += (float) (System.currentTimeMillis() % 14400L) / 40f;
            arrastando = true;
            return true;
        }
        return super.mouseClicked(e, duplo);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (arrastando) {
            giroBase += (float) dx * 2.5f;
            inclinacao = Mth.clamp(inclinacao - (float) dy * 1.5f, -30f, 30f);
            return true;
        }
        return super.mouseDragged(e, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        if (arrastando) {
            arrastando = false;
            giroBase -= (float) (System.currentTimeMillis() % 14400L) / 40f;
            return true;
        }
        return super.mouseReleased(e);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        if (sy != 0 && capitulo == Capitulo.ITENS && sobre(x, y, x0, y0 + GRADE_Y, W / 2, GRADE_LINHAS * PASSO)) {
            rolagem = Mth.clamp(rolagem + (sy < 0 ? 1 : -1), 0, maxRolagem());
            return true;
        }
        if (sy != 0 && (capitulo == Capitulo.MISSOES || capitulo == Capitulo.NPCS) && sobre(x, y, x0, y0, W / 2, H)) {
            rolagemLista = Math.max(0, rolagemLista + (sy < 0 ? 1 : -1));
            return true;
        }
        if (sy != 0 && capitulo == Capitulo.PERSONAGENS && naLista && sobre(x, y, x0, y0 + GRADE_Y, W / 2, GRADE_LINHAS * PASSO)) {
            rolagemPers = Mth.clamp(rolagemPers + (sy < 0 ? 1 : -1), 0, maxRolagemPers());
            return true;
        }
        if (sy != 0 && capitulo == Capitulo.PERSONAGENS && !naLista && sobre(x, y, x0, y0, W / 2, H) && personagens.size() > 1) {
            trocarPersonagem(sy < 0 ? 1 : -1);
            return true;
        }
        // Na página direita a roda vira as páginas.
        if (sy != 0 && sobre(x, y, x0 + W / 2, y0, W / 2, H)) {
            int total = paginasDireita().size();
            int nova = Mth.clamp(pagina + (sy < 0 ? 1 : -1), 0, Math.max(0, total - 1));
            virar(nova);
            return true;
        }
        return super.mouseScrolled(x, y, sx, sy);
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        // Backspace fora da busca volta ao item anterior (o mesmo do botão de voltar).
        if (capitulo == Capitulo.ITENS && e.key() == 259 && !busca.isFocused() && !historico.isEmpty()) {
            voltar();
            return true;
        }
        // Na ficha de um personagem, Backspace volta ao índice.
        if (capitulo == Capitulo.PERSONAGENS && !naLista && e.key() == 259) {
            abrirLista();
            return true;
        }
        return super.keyPressed(e);
    }

    /** Maiores valores da ficha entre os 100 personagens (docs/dados/personagens.json): a barra cheia. */
    static final class FichaEscala {
        static final double VIDA = 40, ARMADURA = 24, DANO = 12, ENERGIA = 320, VELOCIDADE = 7.5, VOO = 60, PLANAR = 14;

        private FichaEscala() {}
    }
}
