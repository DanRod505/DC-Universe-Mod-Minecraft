package com.danrod.dcuniverse.client.guia;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.client.Skins;
import com.danrod.dcuniverse.client.Voador;
import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.guia.Guia;
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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Guia do Herói (docs/GUIA.md): livro aberto com marcadores de capítulo na borda direita.
 * Capítulos desta etapa: Primeiros Passos (tarefas com visto) e Personagens (prévia 3D giratória e ficha).
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

    enum Capitulo { PASSOS, PERSONAGENS }

    private static Capitulo ultimoCapitulo = Capitulo.PASSOS;
    private static String ultimoPersonagem = "";

    private Capitulo capitulo;
    private List<Personagem> personagens = List.of();
    private int indice;
    private int pagina;
    private int x0, y0;
    private float giroBase = 25f, inclinacao = 0f;
    private boolean arrastando;
    private PageButton anterior, proximo, pagAnterior, pagProxima;

    private GuiaScreen() {
        super(Component.translatable("item.dcuniverse.guia_heroi"));
    }

    /** Abre na página que faz sentido agora: o personagem ativo, se estiver transformado; senão a última lida. */
    public static void abrir() {
        GuiaScreen s = new GuiaScreen();
        String ativo = EstadoCliente.atual.personagem();
        if (!ativo.isEmpty()) {
            ultimoCapitulo = Capitulo.PERSONAGENS;
            ultimoPersonagem = ativo;
        }
        s.capitulo = ultimoCapitulo;
        Minecraft.getInstance().gui.setScreen(s);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
    }

    @Override
    protected void init() {
        x0 = (width - W - 18) / 2;
        y0 = (height - H) / 2;
        personagens = carregarPersonagens();
        indice = 0;
        for (int i = 0; i < personagens.size(); i++) {
            if (personagens.get(i).id().toString().equals(ultimoPersonagem)) indice = i;
        }
        anterior = addRenderableWidget(new PageButton(x0 + ESQ_X + 2, y0 + BASE - 10, false, b -> trocarPersonagem(-1), true));
        proximo = addRenderableWidget(new PageButton(x0 + ESQ_X + PAG_W - 25, y0 + BASE - 10, true, b -> trocarPersonagem(1), true));
        pagAnterior = addRenderableWidget(new PageButton(x0 + DIR_X + 2, y0 + BASE - 10, false, b -> pagina--, true));
        pagProxima = addRenderableWidget(new PageButton(x0 + DIR_X + PAG_W - 25, y0 + BASE - 10, true, b -> pagina++, true));
        atualizarBotoes();
    }

    /** Personagens do servidor na ordem da lista do mod (campo "n" do JSON). */
    private static List<Personagem> carregarPersonagens() {
        List<Personagem> out = new ArrayList<>();
        List<Integer> ordem = new ArrayList<>();
        EstadoCliente.catalogo.forEach((id, json) -> {
            try {
                var obj = JsonParser.parseString(json).getAsJsonObject();
                out.add(Personagem.parse(Identifier.parse(id), obj));
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
        if (c == capitulo) return;
        capitulo = c;
        ultimoCapitulo = c;
        pagina = 0;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
        atualizarBotoes();
    }

    private void atualizarBotoes() {
        boolean pers = capitulo == Capitulo.PERSONAGENS && personagens.size() > 1;
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
        marcadores(g, mx, my);
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, x0, y0, 0, 0, W, H, TEX_W, TEX_H);
        if (capitulo == Capitulo.PASSOS) {
            passosEsquerda(g);
        } else {
            personagemEsquerda(g, mx, my);
        }
        List<List<Bloco>> paginas = paginasDireita();
        if (!paginas.isEmpty()) {
            int y = y0 + TOPO;
            for (Bloco b : paginas.get(Math.min(pagina, paginas.size() - 1))) {
                b.desenho.desenhar(g, x0 + DIR_X, y);
                y += b.altura;
            }
            if (paginas.size() > 1) {
                g.centeredText(font, (pagina + 1) + " / " + paginas.size(), x0 + DIR_X + PAG_W / 2, y0 + BASE - 7, SUAVE);
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
            int v = (c == Capitulo.PASSOS ? 0 : 48) + (ativo ? 24 : 0);
            g.blit(RenderPipelines.GUI_TEXTURED, TEX, x0 + W - 4, y, 304, v, 24, 22, TEX_W, TEX_H);
            g.blit(RenderPipelines.GUI_TEXTURED, TEX, x0 + W + (ativo ? 4 : 2), y + 5, 336, c == Capitulo.PASSOS ? 0 : 16, 12, 12, TEX_W, TEX_H);
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

    private List<Bloco> personagemDireita() {
        Personagem p = atual();
        List<Bloco> out = new ArrayList<>();
        if (p == null) return out;
        String chave = "personagem." + p.id().getNamespace() + "." + p.id().getPath();
        if (!descoberto(p)) {
            texto(out, Component.translatable("dcuniverse.guia.personagem.como_descobrir"), SUAVE, 4);
            return out;
        }
        texto(out, Component.translatable(chave + ".exp").withStyle(ChatFormatting.ITALIC), SUAVE, 6);
        out.add(secao("dcuniverse.guia.secao.ficha"));
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
        out.add(secao("dcuniverse.guia.secao.energia"));
        out.add(item(Component.translatable("dcuniverse.guia.recarga.base", fmt(p.energia().regen()))));
        for (Personagem.Tipo r : p.energia().recargas()) {
            out.add(item(Component.translatable("dcuniverse.guia.recarga." + r.tipo(), fmt(r.params().d("per_second", 0)))));
        }
        if (!p.passivas().isEmpty()) {
            out.add(espaco(4));
            out.add(secao("dcuniverse.guia.secao.passivas"));
            for (Personagem.Tipo t : p.passivas()) {
                out.add(item(Component.translatable("dcuniverse.guia.passiva." + t.tipo())));
                if (t.tipo().equals("flight")) {
                    double max = t.params().d("speed", 12);
                    texto(out, Component.translatable("dcuniverse.guia.voo.detalhe", fmt(max * Voador.CRUZEIRO), fmt(max),
                        fmt(t.params().d("dash_cost", 15))), SUAVE, 2);
                } else if (t.tipo().equals("super_jump")) {
                    texto(out, Component.translatable("dcuniverse.guia.salto.detalhe", fmt(alturaSalto(t.params().d("power", 2))),
                        fmt(t.params().d("charge", 1))), SUAVE, 2);
                } else if (t.tipo().equals("projectile_resistance")) {
                    texto(out, Component.translatable("dcuniverse.guia.ricochete.detalhe", fmt(t.params().d("max_damage", 10) / 2)), SUAVE, 2);
                }
            }
        }
        List<Personagem.Habilidade> normais = p.habilidades().stream().filter(h -> !h.suprema()).toList();
        if (!normais.isEmpty()) {
            out.add(espaco(4));
            out.add(secao("dcuniverse.guia.secao.habilidades"));
            for (Personagem.Habilidade h : normais) habilidade(out, p, h);
        }
        Personagem.Habilidade sup = p.suprema();
        if (sup != null) {
            out.add(espaco(4));
            out.add(secao("dcuniverse.guia.secao.suprema"));
            int pct = Math.round(sup.custo() * 100f / p.energia().max());
            out.add(nome(Component.translatable("habilidade.dcuniverse." + sup.id())));
            texto(out, Component.translatable("habilidade.dcuniverse." + sup.id() + ".desc"), TINTA, 1);
            out.add(item(Component.translatable("dcuniverse.guia.suprema.custo", pct, fmt(sup.recargaSeg()))));
            detalhes(out, sup.params());
        }
        out.add(espaco(4));
        out.add(secao("dcuniverse.guia.secao.historia"));
        texto(out, Component.translatable(chave + ".lore"), TINTA, 0);
        return out;
    }

    /** Nome, o que faz e os números: como usar e quanto gasta, recarga, alcance e dano em corações. */
    private void habilidade(List<Bloco> out, Personagem p, Personagem.Habilidade h) {
        Params prm = h.params();
        out.add(nome(Component.translatable("habilidade.dcuniverse." + h.id())));
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
        detalhes(out, prm);
        out.add(espaco(3));
    }

    /** Raio, duração e efeitos de controle, quando a habilidade tem. */
    private void detalhes(List<Bloco> out, Params prm) {
        if (prm.json().has("radius")) out.add(item(Component.translatable("dcuniverse.guia.hab.raio", fmt(prm.d("radius", 0)))));
        if (prm.json().has("duration")) out.add(item(Component.translatable("dcuniverse.guia.hab.duracao", fmt(prm.d("duration", 0)))));
        if (prm.d("stun", 0) > 0) out.add(item(Component.translatable("dcuniverse.guia.hab.lento", fmt(Math.min(4, prm.d("stun", 0))))));
        if (prm.d("regen_level", 0) > 0) {
            out.add(item(Component.translatable("dcuniverse.guia.hab.cura", (int) prm.d("regen_level", 0), fmt(prm.d("energy_per_second", 0)))));
        }
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
        List<Bloco> blocos = capitulo == Capitulo.PASSOS ? passosDireita() : personagemDireita();
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
        if (capitulo == Capitulo.PERSONAGENS && e.button() == 0
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
        if (sy != 0 && capitulo == Capitulo.PERSONAGENS && sobre(x, y, x0, y0, W / 2, H) && personagens.size() > 1) {
            trocarPersonagem(sy < 0 ? 1 : -1);
            return true;
        }
        return super.mouseScrolled(x, y, sx, sy);
    }

    /** Maiores valores da ficha entre os 100 personagens (docs/dados/personagens.json): a barra cheia. */
    static final class FichaEscala {
        static final double VIDA = 40, ARMADURA = 24, DANO = 12, ENERGIA = 320, VELOCIDADE = 7.5, VOO = 60, PLANAR = 14;

        private FichaEscala() {}
    }
}
