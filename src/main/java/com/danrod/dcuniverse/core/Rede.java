package com.danrod.dcuniverse.core;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.character.Personagens;
import com.danrod.dcuniverse.character.Transformacao;
import com.danrod.dcuniverse.ability.DemoHud;
import com.danrod.dcuniverse.ability.Habilidades;
import com.danrod.dcuniverse.ability.passive.SuperSalto;
import com.danrod.dcuniverse.ability.passive.Voo;
import com.danrod.dcuniverse.energy.Recargas;
import com.danrod.dcuniverse.guia.Guia;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Pacotes do mod. O servidor decide tudo; o cliente só pede (Selecionar, Usar, Destransformar) e desenha (Sync, Aparencia, Catalogo, GuiaSync). */
public final class Rede {
    private static final int VERSAO = 14;
    public static final Canal CANAL = new Canal(VERSAO);

    /**
     * Uma habilidade como o cliente precisa ver: menu radial e HUD. Gadgets com munição mandam as cargas
     * (cargas = -1 sem munição), o máximo e quantos ticks faltam para a próxima de um total de cargaTotal.
     */
    public record HabInfo(String id, boolean hold, boolean suprema, float custo, int restante, int recargaTotal,
                          int cargas, int cargasMax, int cargaFalta, int cargaTotal) {
        public HabInfo(String id, boolean hold, boolean suprema, float custo, int restante, int recargaTotal) {
            this(id, hold, suprema, custo, restante, recargaTotal, -1, 0, 0, 0);
        }
    }

    /**
     * Estado do próprio jogador. amortecendo vale também fora da transformação (queda amortecida depois de
     * destransformar no ar); planar é a velocidade do planar do personagem (0 = não plana). recarga é quanto
     * a energia sobe por segundo agora; solar diz se o personagem recarrega no sol e sol se ele está no sol.
     */
    public record Sync(String personagem, float energia, float max, boolean esgotado, int selecionada,
                       String canalizando, List<HabInfo> habilidades, float planar, boolean amortecendo,
                       float recarga, boolean solar, boolean sol, float voo, float arrancada, boolean tecnologica, boolean estacao) {
        public static final Sync VAZIO = new Sync("", 0, 0, false, 0, "", List.of(), 0, false, 0, false, false, 0, 0, false, false);

        /** A habilidade pode ser usada agora: sem recarga pendente, com energia para o custo e munição. */
        public boolean pronta(HabInfo h) {
            return h.restante() <= 0 && !esgotado && energia >= h.custo() && h.cargas() != 0;
        }

        public static void encode(Sync m, FriendlyByteBuf b) {
            b.writeUtf(m.personagem);
            b.writeFloat(m.energia);
            b.writeFloat(m.max);
            b.writeBoolean(m.esgotado);
            b.writeVarInt(m.selecionada);
            b.writeUtf(m.canalizando);
            b.writeVarInt(m.habilidades.size());
            for (HabInfo h : m.habilidades) {
                b.writeUtf(h.id);
                b.writeBoolean(h.hold);
                b.writeBoolean(h.suprema);
                b.writeFloat(h.custo);
                b.writeVarInt(h.restante);
                b.writeVarInt(h.recargaTotal);
                b.writeVarInt(h.cargas + 1);
                b.writeVarInt(h.cargasMax);
                b.writeVarInt(h.cargaFalta);
                b.writeVarInt(h.cargaTotal);
            }
            b.writeFloat(m.planar);
            b.writeBoolean(m.amortecendo);
            b.writeFloat(m.recarga);
            b.writeBoolean(m.solar);
            b.writeBoolean(m.sol);
            b.writeFloat(m.voo);
            b.writeFloat(m.arrancada);
            b.writeBoolean(m.tecnologica);
            b.writeBoolean(m.estacao);
        }

        public static Sync decode(FriendlyByteBuf b) {
            String p = b.readUtf();
            float e = b.readFloat(), max = b.readFloat();
            boolean esg = b.readBoolean();
            int sel = b.readVarInt();
            String can = b.readUtf();
            int n = b.readVarInt();
            List<HabInfo> habs = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                habs.add(new HabInfo(b.readUtf(), b.readBoolean(), b.readBoolean(), b.readFloat(), b.readVarInt(), b.readVarInt(),
                    b.readVarInt() - 1, b.readVarInt(), b.readVarInt(), b.readVarInt()));
            }
            return new Sync(p, e, max, esg, sel, can, List.copyOf(habs), b.readFloat(), b.readBoolean(),
                b.readFloat(), b.readBoolean(), b.readBoolean(), b.readFloat(), b.readFloat(), b.readBoolean(), b.readBoolean());
        }
    }

    /**
     * Aparência de um jogador para quem está vendo ele: personagem ("" = normal), a cor do personagem e o
     * efeito da habilidade canalizada agora (tipo, "" = nenhuma), com cor, alcance e ângulo, para todo mundo
     * ver o raio ou o sopro, se ele está voando (para a pose de voo) e o estado da capa do planar
     * (ability/passive/Planar: 0 fechada, 1 aberta, 2 mergulho, 3 freio).
     */
    public record Aparencia(int entidade, String personagem, int cor, String efeito, int efeitoCor, float alcance, float angulo, boolean voando,
                            int rastro, int rastro2, int rastro3, int planar) {
        public static void encode(Aparencia m, FriendlyByteBuf b) {
            b.writeVarInt(m.entidade);
            b.writeUtf(m.personagem);
            b.writeInt(m.cor);
            b.writeUtf(m.efeito);
            b.writeInt(m.efeitoCor);
            b.writeFloat(m.alcance);
            b.writeFloat(m.angulo);
            b.writeBoolean(m.voando);
            b.writeInt(m.rastro);
            b.writeInt(m.rastro2);
            b.writeInt(m.rastro3);
            b.writeVarInt(m.planar);
        }

        public static Aparencia decode(FriendlyByteBuf b) {
            return new Aparencia(b.readVarInt(), b.readUtf(), b.readInt(), b.readUtf(), b.readInt(), b.readFloat(), b.readFloat(), b.readBoolean(),
                b.readInt(), b.readInt(), b.readInt(), b.readVarInt());
        }
    }

    /** Os personagens do datapack (JSON cru), para o Guia mostrar fichas e habilidades iguais às do servidor. */
    public record Catalogo(Map<String, String> personagens) {
        public static void encode(Catalogo m, FriendlyByteBuf b) {
            b.writeMap(m.personagens, FriendlyByteBuf::writeUtf, (buf, s) -> buf.writeUtf(s, 262144));
        }

        public static Catalogo decode(FriendlyByteBuf b) {
            return new Catalogo(Map.copyOf(b.readMap(FriendlyByteBuf::readUtf, buf -> buf.readUtf(262144))));
        }
    }

    /** Progresso do jogador no Guia: tarefas concluídas de Primeiros Passos e personagens descobertos. */
    public record GuiaSync(List<String> tarefas, List<String> descobertos, String novaTarefa) {
        public static final GuiaSync VAZIO = new GuiaSync(List.of(), List.of(), "");

        public static void encode(GuiaSync m, FriendlyByteBuf b) {
            b.writeCollection(m.tarefas, FriendlyByteBuf::writeUtf);
            b.writeCollection(m.descobertos, FriendlyByteBuf::writeUtf);
            b.writeUtf(m.novaTarefa);
        }

        public static GuiaSync decode(FriendlyByteBuf b) {
            return new GuiaSync(List.copyOf(b.readList(FriendlyByteBuf::readUtf)), List.copyOf(b.readList(FriendlyByteBuf::readUtf)), b.readUtf());
        }
    }

    /** Escolha no menu radial (índice na lista de habilidades). */
    public record Selecionar(int indice) {}

    /** Tecla de uso apertada (true) ou solta (false). */
    /** Tecla de habilidade apertada ou solta: indice na lista de habilidades do Sync (-1 = a escolhida no radial). */
    public record Usar(int indice, boolean pressionado) {}

    /** Golpe do combo corpo a corpo (combate/Combo): passo 0 começa o combo. */
    public record Golpe(int passo) {}

    /** Alguém começou um golpe: a animação para quem vê. */
    public record GolpeVisto(int entidade, String anim) {}

    /** Um golpe acertou: hit-stop, tremor, partículas e som no ponto (x, y, z), com a direção do golpe (dx, dy, dz). */
    public record Impacto(int atacante, int alvo, int fx, double x, double y, double z, float dx, float dy, float dz) {}

    /** O cliente passou da barreira do som voando (o servidor confere e solta o estrondo). */
    public record Estrondo() {}

    /** Manobra de voo feita pelo cliente (Voo.GIRO_ESQUERDA... ARRANCADA). */
    public record Manobra(int tipo) {}

    /** O cliente abriu, fechou ou mudou a capa do planar (ability/passive/Planar.FECHADA...FREIO). */
    public record Planar(int estado) {}

    /** Outro jogador fez uma manobra: quem vê anima a pose dele. */
    public record ManobraVista(int entidade, int tipo) {}

    public record Destransformar() {}

    /** O animal passou a usar (ou tirou) a Coleira do S. */
    public record Coleira(int entidade, boolean usa) {}

    /** Custo de uma receita no Batcomputador: item que representa o ingrediente, quantos pede e quantos o jogador tem. */
    public record BatCusto(String item, int qtd, int tem) {}

    /** Receita que o Batcomputador fabrica: id da receita, resultado e se dá para fazer agora. */
    public record BatFab(String receita, String item, int qtd, List<BatCusto> custos, boolean pode) {}

    /** Abre (ou atualiza) a tela do Batcomputador: heroi = tem recarga tecnológica; recarga = segundos até recarregar de novo. */
    /**
     * Cinto do Batcomputador: de quem é ("" = nenhum herói com cinto), quantos espaços, os gadgets que dá para levar,
     * o que está em cada espaço ("" vazio), as supremas que dá para escolher e a escolhida.
     */
    public record BatCinto(String personagem, int slots, List<String> gadgets, List<String> equipados, List<String> veiculos, String veiculo) {
        public static final BatCinto VAZIO = new BatCinto("", 0, List.of(), List.of(), List.of(), "");

        static void encode(BatCinto c, FriendlyByteBuf b) {
            b.writeUtf(c.personagem);
            b.writeVarInt(c.slots);
            for (List<String> l : List.of(c.gadgets, c.equipados, c.veiculos)) {
                b.writeVarInt(l.size());
                for (String s : l) b.writeUtf(s);
            }
            b.writeUtf(c.veiculo);
        }

        static BatCinto decode(FriendlyByteBuf b) {
            String p = b.readUtf();
            int slots = Math.min(b.readVarInt(), 16);
            List<List<String>> ls = new ArrayList<>();
            for (int k = 0; k < 3; k++) {
                int n = Math.min(b.readVarInt(), 64);
                List<String> l = new ArrayList<>();
                for (int i = 0; i < n; i++) l.add(b.readUtf());
                ls.add(List.copyOf(l));
            }
            return new BatCinto(p, slots, ls.get(0), ls.get(1), ls.get(2), b.readUtf());
        }
    }

    public record BatAbrir(net.minecraft.core.BlockPos pos, boolean heroi, int recarga, List<BatFab> fabricaveis, List<String> analisados,
                           BatCinto cinto) {
        public static void encode(BatAbrir m, FriendlyByteBuf b) {
            b.writeBlockPos(m.pos);
            b.writeBoolean(m.heroi);
            b.writeVarInt(m.recarga);
            b.writeVarInt(m.fabricaveis.size());
            for (BatFab f : m.fabricaveis) {
                b.writeUtf(f.receita());
                b.writeUtf(f.item());
                b.writeVarInt(f.qtd());
                b.writeBoolean(f.pode());
                b.writeVarInt(f.custos().size());
                for (BatCusto c : f.custos()) {
                    b.writeUtf(c.item());
                    b.writeVarInt(c.qtd());
                    b.writeVarInt(c.tem());
                }
            }
            b.writeVarInt(m.analisados.size());
            for (String a : m.analisados) b.writeUtf(a);
            BatCinto.encode(m.cinto, b);
        }

        public static BatAbrir decode(FriendlyByteBuf b) {
            var pos = b.readBlockPos();
            boolean heroi = b.readBoolean();
            int recarga = b.readVarInt();
            int n = Math.min(b.readVarInt(), 256);
            List<BatFab> fab = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                String receita = b.readUtf(), item = b.readUtf();
                int qtd = b.readVarInt();
                boolean pode = b.readBoolean();
                int nc = Math.min(b.readVarInt(), 9);
                List<BatCusto> cs = new ArrayList<>();
                for (int j = 0; j < nc; j++) cs.add(new BatCusto(b.readUtf(), b.readVarInt(), b.readVarInt()));
                fab.add(new BatFab(receita, item, qtd, List.copyOf(cs), pode));
            }
            int na = Math.min(b.readVarInt(), 256);
            List<String> an = new ArrayList<>();
            for (int i = 0; i < na; i++) an.add(b.readUtf());
            return new BatAbrir(pos, heroi, recarga, List.copyOf(fab), List.copyOf(an), BatCinto.decode(b));
        }
    }

    /** Ação na tela do Batcomputador (content/Batcomputador.RECARREGAR, FABRICAR com o id da receita, ANALISAR com o item da amostra). */
    public record BatAcao(net.minecraft.core.BlockPos pos, int tipo, String arg) {}

    /**
     * Efeito pontual no mundo para o cliente desenhar (onda do pisão, raio X, fortaleza, super-salto...):
     * tipo, posição e dois valores e uma cor livres, que cada tipo interpreta (client/Mundo).
     */
    public record Efeito(String tipo, double x, double y, double z, float a, float b, int cor) {
        public static void encode(Efeito m, FriendlyByteBuf b) {
            b.writeUtf(m.tipo);
            b.writeDouble(m.x);
            b.writeDouble(m.y);
            b.writeDouble(m.z);
            b.writeFloat(m.a);
            b.writeFloat(m.b);
            b.writeInt(m.cor);
        }

        public static Efeito decode(FriendlyByteBuf b) {
            return new Efeito(b.readUtf(), b.readDouble(), b.readDouble(), b.readDouble(), b.readFloat(), b.readFloat(), b.readInt());
        }
    }

    /**
     * Cabo do gancho de um jogador (dono = id da entidade): modo (ability/active/Gancho: 0 solto, 1 bloco,
     * 2 entidade, 3 puxando mob, 4 errou), ponto de ancoragem, entidade presa (-1 = nenhuma) e velocidade da puxada.
     */
    public record GanchoVisto(int dono, int modo, double x, double y, double z, int alvo, float vel) {
        public static void encode(GanchoVisto m, FriendlyByteBuf b) {
            b.writeVarInt(m.dono);
            b.writeVarInt(m.modo);
            b.writeDouble(m.x);
            b.writeDouble(m.y);
            b.writeDouble(m.z);
            b.writeVarInt(m.alvo + 1);
            b.writeFloat(m.vel);
        }

        public static GanchoVisto decode(FriendlyByteBuf b) {
            return new GanchoVisto(b.readVarInt(), b.readVarInt(), b.readDouble(), b.readDouble(), b.readDouble(), b.readVarInt() - 1, b.readFloat());
        }
    }

    /** O dono do gancho chegou ao ponto ou soltou o cabo. */
    public record GanchoFim() {}

    /** Comando do piloto do veículo (content/BatwingEntidade: metralhadora, míssil, furtivo). */
    public record Veiculo(int acao) {}

    /** O cliente soltou um super-salto carregado (força de 0 a 1); o servidor confere e mostra para todos. */
    public record Salto(float forca) {}

    /** NPCs e missões do datapack (JSON cru), para o Diário e o capítulo de NPCs do Guia. */
    public record NpcCatalogo(Map<String, String> npcs, Map<String, String> missoes) {
        public static void encode(NpcCatalogo m, FriendlyByteBuf b) {
            b.writeMap(m.npcs, FriendlyByteBuf::writeUtf, (buf, s) -> buf.writeUtf(s, 65536));
            b.writeMap(m.missoes, FriendlyByteBuf::writeUtf, (buf, s) -> buf.writeUtf(s, 65536));
        }

        public static NpcCatalogo decode(FriendlyByteBuf b) {
            return new NpcCatalogo(Map.copyOf(b.readMap(FriendlyByteBuf::readUtf, buf -> buf.readUtf(65536))),
                Map.copyOf(b.readMap(FriendlyByteBuf::readUtf, buf -> buf.readUtf(65536))));
        }
    }

    /**
     * Opção do diálogo: id devolvido ao servidor, texto (chave do lang, argumentos com "@" também são chaves),
     * itens desenhados ao lado ("id*qtd": numa troca, o último é o que se ganha) e se dá para escolher agora.
     */
    public record Opcao(String id, String texto, List<String> args, List<String> itens, boolean ativa) {
        static void encode(Opcao o, FriendlyByteBuf b) {
            b.writeUtf(o.id);
            b.writeUtf(o.texto);
            b.writeCollection(o.args, FriendlyByteBuf::writeUtf);
            b.writeCollection(o.itens, FriendlyByteBuf::writeUtf);
            b.writeBoolean(o.ativa);
        }

        static Opcao decode(FriendlyByteBuf b) {
            return new Opcao(b.readUtf(), b.readUtf(), List.copyOf(b.readList(FriendlyByteBuf::readUtf)), List.copyOf(b.readList(FriendlyByteBuf::readUtf)), b.readBoolean());
        }
    }

    /** Abre (ou atualiza) a conversa com um NPC: quem é, a afinidade do jogador com o grupo, a fala e as opções. */
    public record NpcAbrir(int entidade, String npc, int skin, int afinidade, String grupo, String texto, List<String> args, List<Opcao> opcoes) {
        public static void encode(NpcAbrir m, FriendlyByteBuf b) {
            b.writeVarInt(m.entidade);
            b.writeUtf(m.npc);
            b.writeVarInt(m.skin);
            b.writeVarInt(m.afinidade);
            b.writeUtf(m.grupo);
            b.writeUtf(m.texto);
            b.writeCollection(m.args, FriendlyByteBuf::writeUtf);
            b.writeCollection(m.opcoes, (buf, o) -> Opcao.encode(o, buf));
        }

        public static NpcAbrir decode(FriendlyByteBuf b) {
            return new NpcAbrir(b.readVarInt(), b.readUtf(), b.readVarInt(), b.readVarInt(), b.readUtf(), b.readUtf(),
                List.copyOf(b.readList(FriendlyByteBuf::readUtf)), List.copyOf(b.readList(Opcao::decode)));
        }
    }

    /** Opção escolhida na conversa ("tchau" fecha). */
    public record NpcEscolha(int entidade, String opcao) {}

    /** Fala de NPC no chat (entidade -1 = só pelo id do NPC). */
    public record NpcFala(int entidade, String npc, String texto, List<String> args) {}

    /** Uma missão ativa como o cliente vê: objetivo atual, progresso nele e para onde a seta aponta. */
    public record MissaoVista(String id, int objetivo, int progresso, boolean temAlvo, int x, int y, int z) {}

    /** Diário do jogador: ativas, concluídas, a rastreada, afinidade por grupo, NPCs conhecidos e missões que já podem ser aceitas. */
    public record DiarioSync(List<MissaoVista> ativas, List<String> concluidas, String rastreada, Map<String, Integer> afinidades, List<String> conhecidos,
                             List<String> disponiveis) {
        public static final DiarioSync VAZIO = new DiarioSync(List.of(), List.of(), "", Map.of(), List.of(), List.of());

        public static void encode(DiarioSync m, FriendlyByteBuf b) {
            b.writeCollection(m.ativas, (buf, a) -> {
                buf.writeUtf(a.id());
                buf.writeVarInt(a.objetivo());
                buf.writeVarInt(a.progresso());
                buf.writeBoolean(a.temAlvo());
                buf.writeInt(a.x());
                buf.writeInt(a.y());
                buf.writeInt(a.z());
            });
            b.writeCollection(m.concluidas, FriendlyByteBuf::writeUtf);
            b.writeUtf(m.rastreada);
            b.writeMap(m.afinidades, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeVarInt);
            b.writeCollection(m.conhecidos, FriendlyByteBuf::writeUtf);
            b.writeCollection(m.disponiveis, FriendlyByteBuf::writeUtf);
        }

        public static DiarioSync decode(FriendlyByteBuf b) {
            return new DiarioSync(List.copyOf(b.readList(buf -> new MissaoVista(buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(),
                buf.readInt(), buf.readInt(), buf.readInt()))), List.copyOf(b.readList(FriendlyByteBuf::readUtf)), b.readUtf(),
                Map.copyOf(b.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readVarInt)), List.copyOf(b.readList(FriendlyByteBuf::readUtf)),
                List.copyOf(b.readList(FriendlyByteBuf::readUtf)));
        }
    }

    /** Aliado vinculado pelo Comunicador da Liga, na mesma dimensão. */
    public record Aliado(java.util.UUID id, String nome, double x, double y, double z) {}

    /** Comunicador da Liga: aliados vinculados que também carregam um (lista vazia = esconder). */
    public record LigaSync(List<Aliado> aliados) {}

    /** Ponto de encontro marcado por um aliado (ou por você). */
    public record LigaPonto(String quem, int x, int y, int z) {}

    /** Aviso de missão para o cliente mostrar (tipo "nova" ou "concluida"). */
    public record MissaoAviso(String tipo, String missao) {}

    /** Pedido do Diário: 0 rastrear (arg = id, "" para nenhuma), 1 abandonar. */
    public record DiarioAcao(int tipo, String arg) {}

    /** Os blocos de uma Ilha Telecinética (content/IlhaTelecinetica): posição local empacotada e estado de cada um. */
    public record IlhaBlocos(int entidade, int[] pos, int[] estados) {}

    /** Um local no mapa do mundo: os prontos ficam onde nasceram; os reservados, no ponto marcado no Mundo DC. */
    public record Marca(String local, int x, int z, boolean pronto) {}

    /** Personagem que dá para escolher no início guiado e o item de transformação dele. */
    public record Inicial(String personagem, String item) {}

    /**
     * Mapa do mundo para o cliente: {@code escolher} abre a tela do início guiado (com os personagens, se o servidor
     * dá o item inicial); senão é só o mapa da tecla M. (jx, jz) é onde o jogador está.
     */
    public record InicioAbrir(boolean escolher, com.danrod.dcuniverse.world.mundo.MapaMundo.Mapa mapa, List<Marca> marcas, int jx, int jz,
                              List<Inicial> personagens) {
        public static void encode(InicioAbrir m, FriendlyByteBuf b) {
            b.writeBoolean(m.escolher);
            var a = m.mapa;
            b.writeVarInt(a.x0());
            b.writeVarInt(a.z0());
            b.writeVarInt(a.passo());
            b.writeVarInt(a.w());
            b.writeVarInt(a.h());
            b.writeVarIntArray(a.paleta());
            b.writeByteArray(a.pixels());
            b.writeVarInt(m.marcas.size());
            for (Marca k : m.marcas) {
                b.writeUtf(k.local());
                b.writeVarInt(k.x());
                b.writeVarInt(k.z());
                b.writeBoolean(k.pronto());
            }
            b.writeVarInt(m.jx);
            b.writeVarInt(m.jz);
            b.writeVarInt(m.personagens.size());
            for (Inicial i : m.personagens) {
                b.writeUtf(i.personagem());
                b.writeUtf(i.item());
            }
        }

        public static InicioAbrir decode(FriendlyByteBuf b) {
            boolean escolher = b.readBoolean();
            int x0 = b.readVarInt(), z0 = b.readVarInt(), passo = b.readVarInt(), w = b.readVarInt(), h = b.readVarInt();
            int[] paleta = b.readVarIntArray(256);
            byte[] px = b.readByteArray(com.danrod.dcuniverse.world.mundo.MapaMundo.LADO * com.danrod.dcuniverse.world.mundo.MapaMundo.LADO);
            var mapa = new com.danrod.dcuniverse.world.mundo.MapaMundo.Mapa(x0, z0, passo, w, h, paleta, px);
            int n = Math.min(b.readVarInt(), 256);
            List<Marca> marcas = new ArrayList<>();
            for (int i = 0; i < n; i++) marcas.add(new Marca(b.readUtf(), b.readVarInt(), b.readVarInt(), b.readBoolean()));
            int jx = b.readVarInt(), jz = b.readVarInt();
            int np = Math.min(b.readVarInt(), 256);
            List<Inicial> ps = new ArrayList<>();
            for (int i = 0; i < np; i++) ps.add(new Inicial(b.readUtf(), b.readUtf()));
            return new InicioAbrir(escolher, mapa, List.copyOf(marcas), jx, jz, List.copyOf(ps));
        }
    }

    /** Escolha do início guiado: local ("" = ponto de nascimento) e personagem ("" sem item inicial). */
    public record InicioEscolha(String local, String personagem) {}

    /** Pedido do mapa do mundo (tecla M). */
    public record MapaPedir() {}

    /** Modo Diretor: câmera lenta (fração da velocidade normal do mundo). */
    public record DiretorTempo(float fator) {}

    /** O cliente começou a gravar um replay (Flashback): pede de novo a aparência de quem está por perto. */
    public record AparenciasPedir() {}

    private Rede() {}

    public static void registrar() {
        CANAL.messageBuilder(InicioAbrir.class)
            .direction(PacketFlow.CLIENTBOUND)
            .encoder(InicioAbrir::encode).decoder(InicioAbrir::decode)
            .consumerMainThread((m, ctx) -> EstadoCliente.inicio.accept(m))
            .add();
        CANAL.messageBuilder(InicioEscolha.class)
            .direction(PacketFlow.SERVERBOUND)
            .encoder((m, b) -> {
                b.writeUtf(m.local());
                b.writeUtf(m.personagem());
            }).decoder(b -> new InicioEscolha(b.readUtf(64), b.readUtf(128)))
            .consumerMainThread((m, ctx) -> comJogador(ctx, p -> com.danrod.dcuniverse.inicio.Inicio.escolher(p, m.local(), m.personagem())))
            .add();
        CANAL.messageBuilder(MapaPedir.class)
            .direction(PacketFlow.SERVERBOUND)
            .encoder((m, b) -> {}).decoder(b -> new MapaPedir())
            .consumerMainThread((m, ctx) -> comJogador(ctx, p -> com.danrod.dcuniverse.inicio.Inicio.abrir(p, false)))
            .add();
        CANAL.messageBuilder(DiretorTempo.class)
            .direction(PacketFlow.SERVERBOUND)
            .encoder((m, b) -> b.writeFloat(m.fator())).decoder(b -> new DiretorTempo(b.readFloat()))
            .consumerMainThread((m, ctx) -> comJogador(ctx, p -> Diretor.tempo(p, m.fator())))
            .add();
        CANAL.messageBuilder(AparenciasPedir.class)
            .direction(PacketFlow.SERVERBOUND)
            .encoder((m, b) -> {}).decoder(b -> new AparenciasPedir())
            .consumerMainThread((m, ctx) -> comJogador(ctx, p -> {
                for (ServerPlayer q : p.level().players())
                    if (q == p || q.distanceToSqr(p) < 160 * 160) aparenciaPara(p, q);
            }))
            .add();
        CANAL.messageBuilder(Sync.class)
            .direction(PacketFlow.CLIENTBOUND)
            .encoder(Sync::encode).decoder(Sync::decode)
            .consumerMainThread((m, ctx) -> EstadoCliente.atual = EstadoCliente.emReplay.getAsBoolean() ? Sync.VAZIO : m)
            .add();
        CANAL.messageBuilder(Aparencia.class)
            .direction(PacketFlow.CLIENTBOUND)
            .encoder(Aparencia::encode).decoder(Aparencia::decode)
            .consumerMainThread((m, ctx) -> EstadoCliente.aparencia(m))
            .add();
        CANAL.messageBuilder(Catalogo.class)
            .direction(PacketFlow.CLIENTBOUND)
            .encoder(Catalogo::encode).decoder(Catalogo::decode)
            .consumerMainThread((m, ctx) -> EstadoCliente.catalogo(m))
            .add();
        CANAL.messageBuilder(GuiaSync.class)
            .direction(PacketFlow.CLIENTBOUND)
            .encoder(GuiaSync::encode).decoder(GuiaSync::decode)
            .consumerMainThread((m, ctx) -> EstadoCliente.guia(m))
            .add();
        CANAL.messageBuilder(Selecionar.class)
            .direction(PacketFlow.SERVERBOUND)
            .encoder((m, b) -> b.writeVarInt(m.indice())).decoder(b -> new Selecionar(b.readVarInt()))
            .consumerMainThread((m, ctx) -> comJogador(ctx, p -> Habilidades.selecionar(p, m.indice())))
            .add();
        CANAL.messageBuilder(Usar.class)
            .direction(PacketFlow.SERVERBOUND)
            .encoder((m, b) -> b.writeVarInt(m.indice() + 1).writeBoolean(m.pressionado())).decoder(b -> new Usar(b.readVarInt() - 1, b.readBoolean()))
            .consumerMainThread((m, ctx) -> comJogador(ctx, p -> Habilidades.usar(p, m.indice(), m.pressionado())))
            .add();
        CANAL.messageBuilder(Golpe.class)
            .direction(PacketFlow.SERVERBOUND)
            .encoder((m, b) -> b.writeVarInt(m.passo())).decoder(b -> new Golpe(b.readVarInt()))
            .consumerMainThread((m, ctx) -> comJogador(ctx, p -> com.danrod.dcuniverse.combate.Combo.golpe(p, m.passo())))
            .add();
        CANAL.messageBuilder(GolpeVisto.class)
            .direction(PacketFlow.CLIENTBOUND)
            .encoder((m, b) -> b.writeVarInt(m.entidade()).writeUtf(m.anim()))
            .decoder(b -> new GolpeVisto(b.readVarInt(), b.readUtf()))
            .consumerMainThread((m, ctx) -> EstadoCliente.GOLPES.add(m))
            .add();
        CANAL.messageBuilder(Impacto.class)
            .direction(PacketFlow.CLIENTBOUND)
            .encoder((m, b) -> {
                b.writeVarInt(m.atacante()).writeVarInt(m.alvo()).writeVarInt(m.fx());
                b.writeDouble(m.x()).writeDouble(m.y()).writeDouble(m.z());
                b.writeFloat(m.dx()).writeFloat(m.dy()).writeFloat(m.dz());
            })
            .decoder(b -> new Impacto(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readDouble(), b.readDouble(), b.readDouble(),
                b.readFloat(), b.readFloat(), b.readFloat()))
            .consumerMainThread((m, ctx) -> EstadoCliente.IMPACTOS.add(m))
            .add();
        CANAL.messageBuilder(Estrondo.class)
            .direction(PacketFlow.SERVERBOUND)
            .encoder((m, b) -> {}).decoder(b -> new Estrondo())
            .consumerMainThread((m, ctx) -> comJogador(ctx, Voo::estrondo))
            .add();
        CANAL.messageBuilder(Manobra.class)
            .direction(PacketFlow.SERVERBOUND)
            .encoder((m, b) -> b.writeVarInt(m.tipo())).decoder(b -> new Manobra(b.readVarInt()))
            .consumerMainThread((m, ctx) -> comJogador(ctx, p -> {
                if (m.tipo() >= com.danrod.dcuniverse.ability.passive.Planar.GIRO_ESQUERDA) {
                    com.danrod.dcuniverse.ability.passive.Planar.manobra(p, m.tipo());
                } else {
                    Voo.manobra(p, m.tipo());
                }
            }))
            .add();
        CANAL.messageBuilder(Planar.class)
            .direction(PacketFlow.SERVERBOUND)
            .encoder((m, b) -> b.writeVarInt(m.estado())).decoder(b -> new Planar(b.readVarInt()))
            .consumerMainThread((m, ctx) -> comJogador(ctx, p -> com.danrod.dcuniverse.ability.passive.Planar.definir(p, m.estado())))
            .add();
        CANAL.messageBuilder(ManobraVista.class)
            .direction(PacketFlow.CLIENTBOUND)
            .encoder((m, b) -> b.writeVarInt(m.entidade()).writeVarInt(m.tipo()))
            .decoder(b -> new ManobraVista(b.readVarInt(), b.readVarInt()))
            .consumerMainThread((m, ctx) -> EstadoCliente.MANOBRAS.add(m))
            .add();
        CANAL.messageBuilder(Coleira.class)
            .direction(PacketFlow.CLIENTBOUND)
            .encoder((m, b) -> b.writeVarInt(m.entidade()).writeBoolean(m.usa()))
            .decoder(b -> new Coleira(b.readVarInt(), b.readBoolean()))
            .consumerMainThread((m, ctx) -> {
                if (m.usa()) EstadoCliente.COLEIRAS.add(m.entidade());
                else EstadoCliente.COLEIRAS.remove(m.entidade());
            })
            .add();
        CANAL.messageBuilder(Destransformar.class)
            .direction(PacketFlow.SERVERBOUND)
            .encoder((m, b) -> {}).decoder(b -> new Destransformar())
            .consumerMainThread((m, ctx) -> comJogador(ctx, Transformacao::pedirDestransformar))
            .add();
        CANAL.messageBuilder(Efeito.class)
            .direction(PacketFlow.CLIENTBOUND)
            .encoder(Efeito::encode).decoder(Efeito::decode)
            .consumerMainThread((m, ctx) -> EstadoCliente.EFEITOS.add(m))
            .add();
        CANAL.messageBuilder(GanchoVisto.class)
            .direction(PacketFlow.CLIENTBOUND)
            .encoder(GanchoVisto::encode).decoder(GanchoVisto::decode)
            .consumerMainThread((m, ctx) -> EstadoCliente.GANCHOS.add(m))
            .add();
        CANAL.messageBuilder(GanchoFim.class)
            .direction(PacketFlow.SERVERBOUND)
            .encoder((m, b) -> {}).decoder(b -> new GanchoFim())
            .consumerMainThread((m, ctx) -> comJogador(ctx, com.danrod.dcuniverse.ability.active.Gancho::soltar))
            .add();
        CANAL.messageBuilder(Veiculo.class)
            .direction(PacketFlow.SERVERBOUND)
            .encoder((m, b) -> b.writeVarInt(m.acao())).decoder(b -> new Veiculo(b.readVarInt()))
            .consumerMainThread((m, ctx) -> comJogador(ctx, p -> {
                var v = com.danrod.dcuniverse.content.VeiculoDono.pilotando(p);
                if (v != null) v.comando(p, m.acao());
            }))
            .add();
        CANAL.messageBuilder(BatAbrir.class)
            .direction(PacketFlow.CLIENTBOUND)
            .encoder(BatAbrir::encode).decoder(BatAbrir::decode)
            .consumerMainThread((m, ctx) -> EstadoCliente.batcomputador.accept(m))
            .add();
        CANAL.messageBuilder(BatAcao.class)
            .direction(PacketFlow.SERVERBOUND)
            .encoder((m, b) -> {
                b.writeBlockPos(m.pos());
                b.writeVarInt(m.tipo());
                b.writeUtf(m.arg());
            }).decoder(b -> new BatAcao(b.readBlockPos(), b.readVarInt(), b.readUtf(256)))
            .consumerMainThread((m, ctx) -> comJogador(ctx, p -> com.danrod.dcuniverse.content.Batcomputador.acao(p, m)))
            .add();
        CANAL.messageBuilder(Salto.class)
            .direction(PacketFlow.SERVERBOUND)
            .encoder((m, b) -> b.writeFloat(m.forca())).decoder(b -> new Salto(b.readFloat()))
            .consumerMainThread((m, ctx) -> comJogador(ctx, p -> SuperSalto.saltar(p, m.forca())))
            .add();
        CANAL.messageBuilder(NpcCatalogo.class)
            .direction(PacketFlow.CLIENTBOUND)
            .encoder(NpcCatalogo::encode).decoder(NpcCatalogo::decode)
            .consumerMainThread((m, ctx) -> EstadoCliente.npcCatalogo(m))
            .add();
        CANAL.messageBuilder(NpcAbrir.class)
            .direction(PacketFlow.CLIENTBOUND)
            .encoder(NpcAbrir::encode).decoder(NpcAbrir::decode)
            .consumerMainThread((m, ctx) -> EstadoCliente.npcAbrir.accept(m))
            .add();
        CANAL.messageBuilder(NpcEscolha.class)
            .direction(PacketFlow.SERVERBOUND)
            .encoder((m, b) -> {
                b.writeVarInt(m.entidade());
                b.writeUtf(m.opcao());
            }).decoder(b -> new NpcEscolha(b.readVarInt(), b.readUtf(256)))
            .consumerMainThread((m, ctx) -> comJogador(ctx, p -> com.danrod.dcuniverse.npc.Dialogo.escolher(p, m.entidade(), m.opcao())))
            .add();
        CANAL.messageBuilder(NpcFala.class)
            .direction(PacketFlow.CLIENTBOUND)
            .encoder((m, b) -> {
                b.writeVarInt(m.entidade() + 1);
                b.writeUtf(m.npc());
                b.writeUtf(m.texto());
                b.writeCollection(m.args(), FriendlyByteBuf::writeUtf);
            }).decoder(b -> new NpcFala(b.readVarInt() - 1, b.readUtf(), b.readUtf(), List.copyOf(b.readList(FriendlyByteBuf::readUtf))))
            .consumerMainThread((m, ctx) -> EstadoCliente.npcFala.accept(m))
            .add();
        CANAL.messageBuilder(DiarioSync.class)
            .direction(PacketFlow.CLIENTBOUND)
            .encoder(DiarioSync::encode).decoder(DiarioSync::decode)
            .consumerMainThread((m, ctx) -> EstadoCliente.diario = m)
            .add();
        CANAL.messageBuilder(LigaSync.class)
            .direction(PacketFlow.CLIENTBOUND)
            .encoder((m, b) -> b.writeCollection(m.aliados(), (buf, a) -> {
                buf.writeUUID(a.id());
                buf.writeUtf(a.nome());
                buf.writeDouble(a.x());
                buf.writeDouble(a.y());
                buf.writeDouble(a.z());
            })).decoder(b -> new LigaSync(List.copyOf(b.readList(buf -> new Aliado(buf.readUUID(), buf.readUtf(), buf.readDouble(), buf.readDouble(), buf.readDouble())))))
            .consumerMainThread((m, ctx) -> EstadoCliente.liga = m)
            .add();
        CANAL.messageBuilder(LigaPonto.class)
            .direction(PacketFlow.CLIENTBOUND)
            .encoder((m, b) -> {
                b.writeUtf(m.quem());
                b.writeInt(m.x());
                b.writeInt(m.y());
                b.writeInt(m.z());
            }).decoder(b -> new LigaPonto(b.readUtf(), b.readInt(), b.readInt(), b.readInt()))
            .consumerMainThread((m, ctx) -> EstadoCliente.pontoLiga(m))
            .add();
        CANAL.messageBuilder(IlhaBlocos.class)
            .direction(PacketFlow.CLIENTBOUND)
            .encoder((m, b) -> {
                b.writeVarInt(m.entidade());
                b.writeVarIntArray(m.pos());
                b.writeVarIntArray(m.estados());
            }).decoder(b -> new IlhaBlocos(b.readVarInt(), b.readVarIntArray(4096), b.readVarIntArray(4096)))
            .consumerMainThread((m, ctx) -> EstadoCliente.ILHAS.put(m.entidade(), m))
            .add();
        CANAL.messageBuilder(MissaoAviso.class)
            .direction(PacketFlow.CLIENTBOUND)
            .encoder((m, b) -> {
                b.writeUtf(m.tipo());
                b.writeUtf(m.missao());
            }).decoder(b -> new MissaoAviso(b.readUtf(), b.readUtf()))
            .consumerMainThread((m, ctx) -> EstadoCliente.AVISOS.add(m))
            .add();
        CANAL.messageBuilder(DiarioAcao.class)
            .direction(PacketFlow.SERVERBOUND)
            .encoder((m, b) -> {
                b.writeVarInt(m.tipo());
                b.writeUtf(m.arg());
            }).decoder(b -> new DiarioAcao(b.readVarInt(), b.readUtf(256)))
            .consumerMainThread((m, ctx) -> comJogador(ctx, p -> {
                if (m.tipo() == 0) com.danrod.dcuniverse.missao.Diario.rastrear(p, m.arg());
                else if (m.tipo() == 1 && !m.arg().isEmpty()) com.danrod.dcuniverse.missao.Diario.abandonar(p, Identifier.parse(m.arg()));
            }))
            .add();
    }

    /** Mostra um efeito para todos perto do ponto (até 96 blocos). */
    public static void efeito(ServerLevel level, double x, double y, double z, String tipo, float a, float b, int cor) {
        CANAL.send(new Efeito(tipo, x, y, z, a, b, cor),
            Canal.perto(level, x, y, z, 96));
    }

    /** Cabo do gancho do jogador, para ele e para quem está vendo. */
    public static void gancho(ServerPlayer player, int modo, net.minecraft.world.phys.Vec3 ancora, int alvo, float vel) {
        if (player.connection == null) return;
        CANAL.send(new GanchoVisto(player.getId(), modo, ancora.x, ancora.y, ancora.z, alvo, vel), Canal.rastreandoEProprio(player));
    }

    /** Mostra um efeito só para um jogador (ex.: raio X, que só quem usa enxerga). */
    public static void efeitoPara(ServerPlayer player, String tipo, double x, double y, double z, float a, float b, int cor) {
        if (player.connection == null) return;
        CANAL.send(new Efeito(tipo, x, y, z, a, b, cor), Canal.jogador(player));
    }

    private static void comJogador(Canal.Contexto ctx, Consumer<ServerPlayer> acao) {
        ServerPlayer p = ctx.getSender();
        if (p != null) acao.accept(p);
    }

    /** Avisa o próprio jogador e quem está vendo ele que a skin mudou. Jogadores sem conexão (testes) são ignorados. */
    public static void aparencia(ServerPlayer player) {
        if (player.connection == null) return;
        CANAL.send(montarAparencia(player), Canal.rastreandoEProprio(player));
    }

    /** Avisa só quem está vendo o jogador (ele mesmo já animou a manobra). */
    public static void manobraVista(ServerPlayer player, int tipo) {
        if (player.connection == null) return;
        CANAL.send(new ManobraVista(player.getId(), tipo), Canal.rastreando(player));
    }

    /** Avisa quem está vendo o animal que ele pôs ou tirou a Coleira do S. */
    public static void coleira(net.minecraft.world.entity.Entity e, boolean usa) {
        if (e.level().isClientSide()) return;
        CANAL.send(new Coleira(e.getId(), usa), Canal.rastreando(e));
    }

    public static void coleiraPara(ServerPlayer quemVe, net.minecraft.world.entity.Entity e) {
        if (quemVe.connection == null) return;
        CANAL.send(new Coleira(e.getId(), true), Canal.jogador(quemVe));
    }

    /** Manda a aparência de alvo só para quem começou a ver ele. */
    public static void aparenciaPara(ServerPlayer quemVe, ServerPlayer alvo) {
        if (quemVe.connection == null) return;
        CANAL.send(montarAparencia(alvo), Canal.jogador(quemVe));
    }

    /** Terceira cor padrão do rastro: a principal quase branca. */
    private static int clarear(int c) {
        int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        return ((r + (255 - r) * 4 / 5) << 16) | ((g + (255 - g) * 4 / 5) << 8) | (b + (255 - b) * 4 / 5);
    }

    public static Aparencia montarAparencia(ServerPlayer player) {
        EstadoJogador e = EstadoJogador.de(player);
        Personagem p = e.personagem();
        if (p == null) return new Aparencia(player.getId(), "", 0, "", 0, 0, 0, false, 0, 0, 0, e.planar());
        Personagem.Habilidade h = e.canalizando().isEmpty() ? null : p.habilidade(e.canalizando());
        boolean voando = Voo.voando(player);
        Personagem.Tipo voo = p.passiva("flight");
        int rastro = voo == null ? 0 : voo.params().color("trail_color", p.cor());
        int rastro2 = voo == null ? 0 : voo.params().color("trail_color2", rastro);
        int rastro3 = voo == null ? 0 : voo.params().color("trail_color3", clarear(rastro));
        int planar = e.planar();
        if (h == null) return new Aparencia(player.getId(), p.id().toString(), p.cor(), e.detetive() ? "detective_vision" : "", 0, 0, 0, voando, rastro, rastro2, rastro3, planar);
        return new Aparencia(player.getId(), p.id().toString(), p.cor(), h.tipo(), h.params().color("color", 0xFFFFFF),
            h.params().f("range", 8), h.params().f("angle", 30), voando, rastro, rastro2, rastro3, planar);
    }

    /** Envia o estado completo ao próprio jogador. */
    public static void sincronizar(ServerPlayer player) {
        if (player.connection == null) return;
        CANAL.send(montar(player), Canal.jogador(player));
    }

    public static Sync montar(ServerPlayer player) {
        EstadoJogador e = EstadoJogador.de(player);
        Personagem p = e.personagem();
        if (p == null) {
            // Sem herói, só a Capa Planadora (content/Equipamentos) dá o planar.
            float capa = com.danrod.dcuniverse.content.Equipamentos.vestindoCapa(player) ? com.danrod.dcuniverse.content.Equipamentos.CAPA_VELOCIDADE : 0;
            return e.amortecendo() || capa > 0
                ? new Sync("", 0, 0, false, 0, "", List.of(), capa, e.amortecendo(), 0, false, false, 0, 0, false, false) : Sync.VAZIO;
        }
        long agora = player.level().getGameTime();
        List<HabInfo> habs = new ArrayList<>();
        for (Personagem.Habilidade h : com.danrod.dcuniverse.character.Cinto.equipadas(player, p)) {
            int restante = (int) Math.max(0, e.prontaEm(h.id()) - agora);
            int max = com.danrod.dcuniverse.ability.Cargas.max(h);
            habs.add(new HabInfo(h.id(), h.modo() == Personagem.Modo.HOLD, h.suprema(), player.isCreative() ? 0 : h.custo(), restante,
                Math.round(h.recargaSeg() * 20), max == 0 ? -1 : player.isCreative() ? max : com.danrod.dcuniverse.ability.Cargas.atuais(player, h), max,
                max == 0 ? 0 : com.danrod.dcuniverse.ability.Cargas.faltam(player, h),
                max == 0 ? 0 : com.danrod.dcuniverse.ability.Cargas.ticksPorCarga(player, p, h)));
        }
        int selecionada = e.selecionada();
        if (DemoHud.ativa(player)) {
            selecionada = DemoHud.adicionar(player, p, habs);
        }
        Personagem.Tipo planar = p.passiva("glide");
        boolean solar = p.energia().recargas().stream().anyMatch(r -> r.tipo().equals("sunlight"));
        return new Sync(p.id().toString(), e.energia(), p.energia().max(), e.esgotado(), selecionada,
            e.canalizando(), List.copyOf(habs), planar == null ? 0f : planar.params().f("speed", 10f), e.amortecendo(),
            Recargas.porSegundo(player, p), solar, solar && Recargas.sobOSol(player), Voo.velocidade(p),
            p.passiva("flight") == null ? 0f : p.passiva("flight").params().f("dash_cost", 15f),
            Recargas.tecnologica(p), Recargas.tecnologica(p) && Recargas.naEstacao(player, p));
    }

    public static void catalogo(ServerPlayer player) {
        if (player.connection == null) return;
        CANAL.send(new Catalogo(Personagens.jsons()), Canal.jogador(player));
    }

    public static void guia(ServerPlayer player, String novaTarefa) {
        if (player.connection == null) return;
        CANAL.send(Guia.montar(player, novaTarefa), Canal.jogador(player));
    }

    public static void inicio(ServerPlayer player, InicioAbrir m) {
        if (player.connection != null) CANAL.send(m, Canal.jogador(player));
    }

    public static void npcCatalogo(ServerPlayer player) {
        if (player.connection == null) return;
        CANAL.send(new NpcCatalogo(com.danrod.dcuniverse.npc.Npcs.jsonNpcs(), com.danrod.dcuniverse.npc.Npcs.jsonMissoes()), Canal.jogador(player));
    }

    public static void npcAbrir(ServerPlayer player, NpcAbrir m) {
        if (player.connection == null) return;
        CANAL.send(m, Canal.jogador(player));
    }

    public static void npcFala(ServerPlayer player, int entidade, String npc, String texto, List<String> args) {
        if (player.connection == null) return;
        CANAL.send(new NpcFala(entidade, npc, texto, args), Canal.jogador(player));
    }

    public static void diario(ServerPlayer player) {
        if (player.connection == null) return;
        CANAL.send(com.danrod.dcuniverse.missao.Diario.montar(player), Canal.jogador(player));
    }

    public static void missaoAviso(ServerPlayer player, String tipo, String missao) {
        if (player.connection == null) return;
        CANAL.send(new MissaoAviso(tipo, missao), Canal.jogador(player));
    }

    public static void liga(ServerPlayer player, LigaSync m) {
        if (player.connection == null) return;
        CANAL.send(m, Canal.jogador(player));
    }

    public static void ligaPonto(ServerPlayer player, LigaPonto m) {
        if (player.connection == null) return;
        CANAL.send(m, Canal.jogador(player));
    }

    /** Os blocos da ilha para quem passou a ver a entidade. */
    public static void ilha(ServerPlayer player, IlhaBlocos m) {
        if (player.connection == null) return;
        CANAL.send(m, Canal.jogador(player));
    }
}
