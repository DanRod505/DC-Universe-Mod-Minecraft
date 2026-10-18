package com.danrod.dcuniverse.npc;

import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Personagens;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.EnumSet;
import java.util.UUID;

/**
 * NPC humano (Martha, Lana, cidadãos): usa o modelo de jogador com a skin do JSON dele, passeia em volta de
 * casa, abre portas e para para conversar. Não morre por dano comum; quem bate leva bronca e perde afinidade.
 */
public class NpcEntidade extends PathfinderMob implements Residentes.Residente {
    private static final EntityDataAccessor<String> DEF = SynchedEntityData.defineId(NpcEntidade.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(NpcEntidade.class, EntityDataSerializers.INT);

    /** Jogador conversando agora (o NPC para e olha para ele) e até quando. */
    private UUID conversando;
    private long conversaAte;
    private long broncaEm;
    /** Nome próprio sorteado (cidadãos); vazio = o nome do lang. */
    private String nome = "";
    /** Chave do morador em Residentes ("" = colocado por comando). */
    private String morador = "";

    public NpcEntidade(EntityType<? extends NpcEntidade> tipo, Level level) {
        super(tipo, level);
        if (getNavigation() instanceof GroundPathNavigation n) {
            n.setCanOpenDoors(true);
        }
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder atributos() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(DEF, "");
        b.define(SKIN, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Conversa());
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 10f, 0.7, 0.9, e -> e instanceof Player p && vilao(p)));
        goalSelector.addGoal(3, new OpenDoorGoal(this, true));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.6));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.55, 0.002f) {
            @Override
            public boolean canUse() {
                // À noite fica em casa, quase parado.
                return !(level().isDarkOutside() && random.nextInt(4) != 0) && super.canUse();
            }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    /** Jogador transformado num vilão: os cidadãos fogem (docs/MUNDO.md). */
    private static boolean vilao(Player p) {
        if (!(p instanceof ServerPlayer sp)) return false;
        Identifier id = EstadoJogador.de(sp).personagemId();
        return id != null && "vilao".equals(Personagens.categoria(id));
    }

    public DefNpc def() {
        return Npcs.npc(defId());
    }

    public Identifier defId() {
        String s = entityData.get(DEF);
        return s.isEmpty() ? null : Identifier.tryParse(s);
    }

    public int skin() {
        return entityData.get(SKIN);
    }

    @Override
    public String morador() {
        return morador;
    }

    @Override
    public void morador(String chave) {
        morador = chave;
    }

    @Override
    public void die(DamageSource fonte) {
        super.die(fonte);
        if (level() instanceof ServerLevel s) Residentes.saiu(s, morador);
    }

    /** Define quem é o NPC, onde mora e até onde passeia. */
    public void configurar(DefNpc d, BlockPos casa, String morador) {
        entityData.set(DEF, d.id().toString());
        entityData.set(SKIN, random.nextInt(d.skins().size()));
        this.morador = morador;
        nome = d.nomes().isEmpty() ? "" : d.nomes().get(random.nextInt(d.nomes().size()));
        setHomeTo(casa, d.raio());
        atualizarNome(d);
    }

    private void atualizarNome(DefNpc d) {
        Component n = nome.isEmpty() ? Component.translatable(d.chaveNome())
            : Component.literal(nome + " · ").append(Component.translatable("dcuniverse.npc.profissao." + d.profissao()));
        setCustomName(n);
    }

    public Component nomeExibido() {
        return getCustomName() != null ? getCustomName() : getName();
    }

    public void conversar(ServerPlayer p) {
        conversando = p.getUUID();
        conversaAte = level().getGameTime() + 20 * 30;
        getNavigation().stop();
    }

    public void encerrar(ServerPlayer p) {
        if (p.getUUID().equals(conversando)) conversando = null;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp && def() != null) Dialogo.abrir(sp, this);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource fonte, float dano) {
        if (fonte.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurtServer(level, fonte, dano);
        if (fonte.getEntity() instanceof ServerPlayer p && def() != null) {
            long agora = level.getGameTime();
            if (agora - broncaEm > 40) {
                broncaEm = agora;
                Afinidade.mudar(p, def().grupo(), -5);
                Dialogo.dizer(p, this, def().chave("bronca"));
                playSound(SoundEvents.VILLAGER_NO, 1f, getVoicePitch());
            }
        }
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return conversando == null && super.isPushable();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putString("npc", entityData.get(DEF));
        out.putInt("skin", skin());
        out.putString("nome", nome);
        out.putString("morador", morador);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        entityData.set(DEF, in.getStringOr("npc", ""));
        entityData.set(SKIN, in.getIntOr("skin", 0));
        nome = in.getStringOr("nome", "");
        morador = in.getStringOr("morador", "");
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && conversando != null && level().getGameTime() > conversaAte) conversando = null;
    }

    /** Para e olha para quem está conversando. */
    private final class Conversa extends Goal {
        Conversa() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        private Player alvo() {
            return conversando == null ? null : level().getPlayerByUUID(conversando);
        }

        @Override
        public boolean canUse() {
            Player p = alvo();
            if (p != null && p.distanceToSqr(NpcEntidade.this) > 100) conversando = null;
            return alvo() != null;
        }

        @Override
        public void start() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            Player p = alvo();
            if (p != null) getLookControl().setLookAt(p, 30f, 30f);
        }
    }
}
