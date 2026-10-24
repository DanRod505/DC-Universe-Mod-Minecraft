package com.danrod.dcuniverse.client.npc;

import com.danrod.dcuniverse.missao.DefMissao;
import com.danrod.dcuniverse.npc.DefNpc;
import com.danrod.dcuniverse.npc.ItemRef;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

/** Textos de missão gerados a partir do objetivo ("Pegue 6 Ovos (2/6)"), com o lang .obj.&lt;n&gt; tendo a palavra final. */
public final class TextoMissao {
    private TextoMissao() {}

    public static Component nome(DefMissao m) {
        return Component.translatable(m.chave());
    }

    public static MutableComponent objetivo(DefMissao m, int i, int prog) {
        DefMissao.Objetivo o = m.objetivos().get(i);
        String proprio = m.chave() + ".obj." + i;
        boolean conta = o.tipo() == DefMissao.Tipo.COLLECT || o.tipo() == DefMissao.Tipo.KILL || o.tipo() == DefMissao.Tipo.CRAFT;
        MutableComponent t;
        if (Language.getInstance().has(proprio)) {
            t = Component.translatable(proprio);
        } else {
            t = switch (o.tipo()) {
                case COLLECT -> Component.translatable("dcuniverse.missao.obj.collect", o.qtd(), item(o.item()));
                case CRAFT -> Component.translatable("dcuniverse.missao.obj.craft", o.qtd(), item(o.item()));
                case DELIVER -> Component.translatable("dcuniverse.missao.obj.deliver", npc(o.npc()));
                case TALK -> Component.translatable("dcuniverse.missao.obj.talk", npc(o.npc()));
                case GO -> Component.translatable("dcuniverse.missao.obj.go", lugar(o));
                case KILL -> o.alvos().size() == 1
                    ? Component.translatable("dcuniverse.missao.obj.kill", o.qtd(), entidade(o.alvos().get(0)))
                    : Component.translatable("dcuniverse.missao.obj.kill_monstros", o.qtd());
                case TRANSFORM -> o.alvos().isEmpty() ? Component.translatable("dcuniverse.missao.obj.transform_qualquer")
                    : Component.translatable("dcuniverse.missao.obj.transform", Component.translatable("personagem." + o.alvos().get(0).replace(':', '.')));
                case TAME -> Component.translatable("dcuniverse.missao.obj.tame", o.alvos().isEmpty() ? Component.literal("?") : entidade(o.alvos().get(0)));
            };
        }
        if (conta) t.append(Component.literal(" (" + Math.min(prog, o.qtd()) + "/" + o.qtd() + ")"));
        return t;
    }

    static Component item(ItemRef r) {
        return r.pilha().getHoverName();
    }

    static Component npc(Identifier id) {
        DefNpc d = NpcsCliente.npc(id);
        if (d != null && !d.profissao().isEmpty()) return Component.translatable("dcuniverse.npc.um", Component.translatable("dcuniverse.npc.profissao." + d.profissao()));
        return id == null ? Component.literal("?") : NpcsCliente.nome(id);
    }

    static Component entidade(String id) {
        var t = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(id));
        return t.<Component>map(x -> x.getDescription()).orElse(Component.literal(id));
    }

    static Component lugar(DefMissao.Objetivo o) {
        String k = "dcuniverse.ponto." + o.local() + "." + o.ponto();
        return Language.getInstance().has(k) ? Component.translatable(k) : Component.translatable("dcuniverse.local." + o.local());
    }
}
