package com.danrod.dcuniverse.character;

import com.danrod.dcuniverse.DCUniverse;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Estado do jogador transformado, guardado nos dados persistentes da entidade (sobrevive a logout e restart).
 * O servidor é a autoridade; o cliente recebe uma cópia pelo pacote de sincronização.
 */
public final class EstadoJogador {
    private static final String RAIZ = DCUniverse.MODID;
    private final Player player;
    private final CompoundTag tag;

    private EstadoJogador(Player player) {
        this.player = player;
        CompoundTag dados = player.getPersistentData();
        if (!(dados.get(RAIZ) instanceof CompoundTag)) dados.put(RAIZ, new CompoundTag());
        this.tag = dados.getCompoundOrEmpty(RAIZ);
    }

    public static EstadoJogador de(Player player) {
        return new EstadoJogador(player);
    }

    public Identifier personagemId() {
        String s = tag.getStringOr("personagem", "");
        return s.isEmpty() ? null : Identifier.tryParse(s);
    }

    public Personagem personagem() {
        return Personagens.get(personagemId());
    }

    public boolean transformado() {
        return personagemId() != null;
    }

    public void setPersonagem(Identifier id) {
        tag.putString("personagem", id == null ? "" : id.toString());
    }

    public float energia() {
        return tag.getFloatOr("energia", 0f);
    }

    public void setEnergia(float v) {
        tag.putFloat("energia", v);
    }

    /** Energia zerou e ainda não voltou a 10%: ativas bloqueadas e voo desligado. */
    public boolean esgotado() {
        return tag.getBooleanOr("esgotado", false);
    }

    public void setEsgotado(boolean v) {
        tag.putBoolean("esgotado", v);
    }

    public int selecionada() {
        return tag.getIntOr("selecionada", 0);
    }

    public void setSelecionada(int i) {
        tag.putInt("selecionada", i);
    }

    /** Id da habilidade canalizada (modo HOLD) no momento, ou "". */
    public String canalizando() {
        return tag.getStringOr("canalizando", "");
    }

    public void setCanalizando(String id) {
        tag.putString("canalizando", id);
    }

    public long prontaEm(String habilidade) {
        return tag.getCompoundOrEmpty("recargas").getLongOr(habilidade, 0L);
    }

    public void setProntaEm(String habilidade, long tick) {
        CompoundTag r = tag.getCompoundOrEmpty("recargas");
        r.putLong(habilidade, tick);
        tag.put("recargas", r);
    }

    public long ultimaTransformacao() {
        return tag.getLongOr("ultima_transformacao", Long.MIN_VALUE / 2);
    }

    public void setUltimaTransformacao(long tick) {
        tag.putLong("ultima_transformacao", tick);
    }

    /** Tick em que a destransformação pedida termina (0 = nenhuma em andamento). */
    public long destransformarEm() {
        return tag.getLongOr("destransformar_em", 0L);
    }

    public void setDestransformarEm(long tick) {
        tag.putLong("destransformar_em", tick);
    }

    public void salvarArmadura(EquipmentSlot slot, ItemStack stack) {
        CompoundTag salva = tag.getCompoundOrEmpty("armadura_salva");
        if (stack.isEmpty()) {
            salva.remove(slot.getName());
        } else {
            var ops = player.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            ItemStack.CODEC.encodeStart(ops, stack).result().ifPresent(t -> salva.put(slot.getName(), t));
        }
        tag.put("armadura_salva", salva);
    }

    /** Devolve e apaga a armadura guardada no slot. */
    public ItemStack retirarArmadura(EquipmentSlot slot) {
        CompoundTag salva = tag.getCompoundOrEmpty("armadura_salva");
        Tag t = salva.get(slot.getName());
        salva.remove(slot.getName());
        tag.put("armadura_salva", salva);
        if (t == null) return ItemStack.EMPTY;
        var ops = player.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        return ItemStack.CODEC.parse(ops, t).result().orElse(ItemStack.EMPTY);
    }

    /** Limpa o que só vale durante a transformação. */
    public void limparTransformacao() {
        setPersonagem(null);
        setCanalizando("");
        setEsgotado(false);
        setDestransformarEm(0L);
        tag.remove("recargas");
    }
}
