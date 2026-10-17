package com.danrod.dcuniverse.guia;

import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.content.Itens;
import com.danrod.dcuniverse.content.TransformacaoItem;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.energy.Recargas;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Progresso de cada jogador no Guia do Herói (docs/GUIA.md): tarefas de Primeiros Passos com visto e
 * personagens descobertos. Fica nos dados persistentes do jogador (sobrevive à morte) e vai para o cliente
 * pelo pacote GuiaSync. O livro é igual para todos; o conteúdo é de cada um.
 */
public final class Guia {
    /** Tarefas de Primeiros Passos, na ordem do livro. Cada etapa nova acrescenta as suas. */
    public static final List<String> TAREFAS = List.of("transformar", "sol", "radial", "habilidade", "voar", "estrondo", "manobra", "amortecida", "destransformar",
        "gancho", "batarangue", "batwing", "planar", "manobra_planar",
        "detetive", "fumaca", "gel", "gel_detonar", "contra_ataque", "furtivo",
        "capa_planador", "visor_tatico", "batcomputador");
    /** Segundos recarregando ao sol para ganhar o visto de "sol". */
    public static final int SEGUNDOS_SOL = 5;

    private static final String RAIZ = "dcuniverse_guia";

    private Guia() {}

    /** Todo jogador recebe um Guia do Herói ao entrar no mundo pela primeira vez. */
    public static boolean darGuia(ServerPlayer player) {
        CompoundTag g = dados(player);
        if (g.getBooleanOr("recebeu", false)) return false;
        g.putBoolean("recebeu", true);
        salvar(player, g);
        ItemStack guia = new ItemStack(Itens.GUIA_HEROI.get());
        if (!player.getInventory().add(guia)) player.drop(guia, false);
        return true;
    }

    public static boolean concluida(ServerPlayer player, String tarefa) {
        return lista(dados(player), "tarefas").contains(tarefa);
    }

    /** Marca a tarefa; na primeira vez o cliente mostra o aviso "Tarefa concluída". */
    public static void concluir(ServerPlayer player, String tarefa) {
        if (!TAREFAS.contains(tarefa) || adicionar(player, "tarefas", tarefa)) return;
        Rede.guia(player, tarefa);
    }

    public static boolean descoberto(ServerPlayer player, Identifier personagem) {
        return lista(dados(player), "descobertos").contains(personagem.toString());
    }

    /** Libera a página do personagem no capítulo Personagens. */
    public static void descobrir(ServerPlayer player, Identifier personagem) {
        if (adicionar(player, "descobertos", personagem.toString())) return;
        Rede.guia(player, "");
    }

    /** Uma vez por segundo: descobre personagens pelos itens de transformação e conta o tempo ao sol. */
    public static void tickSegundo(ServerPlayer player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).getItem() instanceof TransformacaoItem t) descobrir(player, t.personagem());
        }
        Personagem p = EstadoJogador.de(player).personagem();
        if (p == null || concluida(player, "sol")) return;
        boolean solar = p.energia().recargas().stream().anyMatch(r -> r.tipo().equals("sunlight"));
        if (!solar || !Recargas.sobOSol(player)) return;
        CompoundTag g = dados(player);
        int s = g.getIntOr("segundos_sol", 0) + 1;
        g.putInt("segundos_sol", s);
        salvar(player, g);
        if (s >= SEGUNDOS_SOL) concluir(player, "sol");
    }

    /** Apaga o progresso (tarefas e descobertas); o jogador continua com o livro. Comando de teste. */
    public static void resetar(ServerPlayer player) {
        salvar(player, new CompoundTag());
        CompoundTag g = dados(player);
        g.putBoolean("recebeu", true);
        salvar(player, g);
        Rede.guia(player, "");
    }

    public static Rede.GuiaSync montar(ServerPlayer player, String novaTarefa) {
        CompoundTag g = dados(player);
        return new Rede.GuiaSync(lista(g, "tarefas"), lista(g, "descobertos"), novaTarefa);
    }

    /** @return true se já estava na lista */
    private static boolean adicionar(ServerPlayer player, String chave, String valor) {
        CompoundTag g = dados(player);
        List<String> atual = lista(g, chave);
        if (atual.contains(valor)) return true;
        ListTag l = g.getListOrEmpty(chave);
        l.add(StringTag.valueOf(valor));
        g.put(chave, l);
        salvar(player, g);
        return false;
    }

    private static List<String> lista(CompoundTag g, String chave) {
        List<String> out = new ArrayList<>();
        for (Tag t : g.getListOrEmpty(chave)) t.asString().ifPresent(out::add);
        return List.copyOf(out);
    }

    /** Dados do guia dentro da parte persistente (copiada ao renascer). */
    private static CompoundTag dados(ServerPlayer player) {
        return player.getPersistentData().getCompoundOrEmpty(ServerPlayer.PERSISTED_NBT_TAG).getCompoundOrEmpty(RAIZ);
    }

    private static void salvar(ServerPlayer player, CompoundTag g) {
        CompoundTag dados = player.getPersistentData();
        CompoundTag persistente = dados.getCompoundOrEmpty(ServerPlayer.PERSISTED_NBT_TAG);
        persistente.put(RAIZ, g);
        dados.put(ServerPlayer.PERSISTED_NBT_TAG, persistente);
    }
}
