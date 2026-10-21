package com.danrod.dcuniverse.content;

import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.guia.Guia;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A Ilha Telecinética, suprema do Superboy (ability/active/Ilha): um disco de terreno arrancado do chão que flutua
 * como plataforma. O dono vai em pé no meio e pilota (o movimento é do cliente dele, como o Batwing: WASD anda,
 * pulo sobe, correr desce; a física fica em client/Ilhas). Qualquer um pode subir e é levado junto. A tecla de
 * usar habilidade pousa a ilha onde ela está, virando blocos de verdade: uma base aérea permanente. No fim do tempo
 * ela assenta sozinha do mesmo jeito.
 * <p>
 * Os blocos vão para o cliente pelo pacote Rede.IlhaBlocos. Para dar para andar em cima, cada coluna de blocos
 * sólidos é uma parte (PartEntity) com a caixa da coluna: no cliente ela colide com o jogador; no servidor só com
 * criaturas e itens (o movimento do jogador é do cliente, e quem está em cima não é chutado por "flutuar").
 */
public class IlhaTelecinetica extends Entity implements VeiculoDono {
    public static final int MAX_PARTES = 160;
    /** Ticks da subida depois de arrancada e quanto ela sobe. */
    public static final int SUBIDA = 50;
    public static final double ALTURA_SUBIDA = 5.5;

    private static final EntityDataAccessor<Integer> DONO = SynchedEntityData.defineId(IlhaTelecinetica.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ESTADO = SynchedEntityData.defineId(IlhaTelecinetica.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RESTANTE = SynchedEntityData.defineId(IlhaTelecinetica.class, EntityDataSerializers.INT);

    /** Física do piloto e o "levar junto" de quem está em cima, postos por client/Ilhas. */
    public static Consumer<IlhaTelecinetica> cliente = i -> {};

    private final InterpolationHandler interpolacao = new InterpolationHandler(this, 3);
    private final Parte[] partes = new Parte[MAX_PARTES];
    private int nPartes;

    // Blocos: posição local empacotada (ver pack) e estado; o mapa serve para o desenho e para conferir vizinhos.
    private int[] pos = new int[0], ids = new int[0];
    private final Long2ObjectOpenHashMap<BlockState> mapa = new Long2ObjectOpenHashMap<>();
    private final List<int[]> celulas = new ArrayList<>();
    private int minX, minY, minZ, maxX, maxY, maxZ;
    private boolean carregada;

    private Params prm = Params.EMPTY;
    private int duracao = 3000, tempoEstado;
    private double yInicial;
    private Vec3 ultima;
    /** Velocidade do piloto (blocos/tick), mantida no cliente dele. */
    public Vec3 velPiloto = Vec3.ZERO;

    public IlhaTelecinetica(EntityType<? extends IlhaTelecinetica> tipo, Level level) {
        super(tipo, level);
        noPhysics = true;
        setNoGravity(true);
        // As partes nascem logo depois da ilha, com ids seguidos (o cliente refaz os ids em recreateFromPacket).
        for (int i = 0; i < MAX_PARTES; i++) partes[i] = new Parte(this);
    }

    // ------------------------------------------------------------------ blocos

    public static int pack(int x, int y, int z) {
        return (x + 512) << 20 | (y + 512) << 10 | (z + 512);
    }

    public static int px(int p) {
        return (p >> 20 & 1023) - 512;
    }

    public static int py(int p) {
        return (p >> 10 & 1023) - 512;
    }

    public static int pz(int p) {
        return (p & 1023) - 512;
    }

    private static long chave(int x, int y, int z) {
        return BlockPos.asLong(x, y, z);
    }

    /** Monta a ilha com estes blocos (no servidor ao arrancar; no cliente quando o pacote chega). */
    public void definirBlocos(int[] pos, int[] ids) {
        this.pos = pos;
        this.ids = ids;
        mapa.clear();
        celulas.clear();
        minX = minY = minZ = Integer.MAX_VALUE;
        maxX = maxY = maxZ = Integer.MIN_VALUE;
        for (int i = 0; i < pos.length; i++) {
            int x = px(pos[i]), y = py(pos[i]), z = pz(pos[i]);
            BlockState st = Block.stateById(ids[i]);
            mapa.put(chave(x, y, z), st);
            celulas.add(new int[] {x, y, z});
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }
        montarPartes();
        carregada = pos.length > 0;
        atualizarPartes();
    }

    /** Cada trecho contínuo de blocos sólidos de uma coluna vira uma caixa de colisão (até MAX_PARTES). */
    private void montarPartes() {
        List<float[]> caixas = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int ini = Integer.MIN_VALUE;
                for (int y = minY; y <= maxY + 1; y++) {
                    BlockState st = mapa.get(chave(x, y, z));
                    boolean solido = st != null && st.blocksMotion();
                    if (solido && ini == Integer.MIN_VALUE) ini = y;
                    if (!solido && ini != Integer.MIN_VALUE) {
                        caixas.add(new float[] {x - 0.5f, ini, z - 0.5f, x + 0.5f, y, z + 0.5f});
                        ini = Integer.MIN_VALUE;
                    }
                }
            }
        }
        // Se passar do limite, fica com os trechos de cima (onde se pisa).
        caixas.sort((a, b) -> Float.compare(b[4], a[4]));
        nPartes = Math.min(MAX_PARTES, caixas.size());
        for (int i = 0; i < MAX_PARTES; i++) partes[i].local = i < nPartes ? caixas.get(i) : null;
    }

    private void atualizarPartes() {
        Vec3 p = position();
        for (Parte parte : partes) {
            if (parte.local == null) {
                parte.setPos(p.x, -10000, p.z);
                parte.setBoundingBox(new AABB(p.x, -10000, p.z, p.x, -10000, p.z));
                continue;
            }
            float[] c = parte.local;
            parte.setPos(p.x + (c[0] + c[3]) / 2, p.y + c[1], p.z + (c[2] + c[5]) / 2);
            parte.setBoundingBox(new AABB(p.x + c[0], p.y + c[1], p.z + c[2], p.x + c[3], p.y + c[4], p.z + c[5]));
        }
    }

    public boolean carregada() {
        return carregada;
    }

    public List<int[]> celulas() {
        return celulas;
    }

    public BlockState bloco(int x, int y, int z) {
        BlockState st = mapa.get(chave(x, y, z));
        return st == null ? Blocks.AIR.defaultBlockState() : st;
    }

    /** Caixa de todos os blocos, no mundo (para o desenho e para achar quem está em cima). */
    public AABB caixaBlocos() {
        Vec3 p = position();
        if (!carregada) return getBoundingBox();
        return new AABB(p.x + minX - 0.5, p.y + minY, p.z + minZ - 0.5, p.x + maxX + 0.5, p.y + maxY + 1, p.z + maxZ + 0.5);
    }

    /** As caixas de colisão das partes, no mundo. */
    public List<AABB> caixasPartes() {
        List<AABB> out = new ArrayList<>(nPartes);
        for (int i = 0; i < nPartes; i++) out.add(partes[i].getBoundingBox());
        return out;
    }

    /** O jogador (ou criatura) está de pé em cima de alguma coluna da ilha? */
    public boolean emCima(Entity e) {
        AABB pes = e.getBoundingBox();
        AABB faixa = new AABB(pes.minX + 0.01, pes.minY - 0.12, pes.minZ + 0.01, pes.maxX - 0.01, pes.minY + 0.05, pes.maxZ - 0.01);
        for (int i = 0; i < nPartes; i++) {
            AABB c = partes[i].getBoundingBox();
            if (c.intersects(faixa) && pes.minY >= c.maxY - 0.3) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ invocação

    public static IlhaTelecinetica criar(ServerPlayer dono, BlockPos centro, int[] pos, int[] ids, Params prm) {
        ServerLevel level = dono.level();
        IlhaTelecinetica i = new IlhaTelecinetica(Entidades.ILHA.get(), level);
        i.prm = prm;
        i.duracao = Math.round(prm.f("duration", 150) * 20);
        i.entityData.set(DONO, dono.getId());
        i.entityData.set(RESTANTE, i.duracao);
        i.snapTo(centro.getX() + 0.5, centro.getY(), centro.getZ() + 0.5, dono.getYRot(), 0);
        i.yInicial = i.getY();
        i.definirBlocos(pos, ids);
        i.ultima = i.position();
        level.addFreshEntity(i);
        level.playSound(null, i.getX(), i.getY(), i.getZ(), Sons.ILHA_ARRANCA.get(), SoundSource.PLAYERS, 2.5f, 1f);
        return i;
    }

    /** Para quem passou a ver a ilha: os blocos dela. */
    public Rede.IlhaBlocos pacote() {
        return new Rede.IlhaBlocos(getId(), pos, ids);
    }

    @Override
    public void recreateFromPacket(net.minecraft.network.protocol.game.ClientboundAddEntityPacket pacote) {
        super.recreateFromPacket(pacote);
        for (int i = 0; i < MAX_PARTES; i++) partes[i].setId(pacote.getId() + i + 1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(DONO, -1);
        b.define(ESTADO, CHEGANDO);
        b.define(RESTANTE, 0);
    }

    @Override
    public int donoId() {
        return entityData.get(DONO);
    }

    public Entity dono() {
        return level().getEntity(donoId());
    }

    @Override
    public int estado() {
        return entityData.get(ESTADO);
    }

    public int restante() {
        return entityData.get(RESTANTE);
    }

    public Params params() {
        return prm;
    }

    // ------------------------------------------------------------------ partes, passageiros e regras de entidade

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return partes;
    }

    @Override
    protected AABB makeBoundingBox(Vec3 p) {
        // Caixa pequena no centro do topo: a ilha inteira não pode contar como caixa (o servidor recusaria o
        // movimento do piloto perto do chão). As colunas são as partes.
        return new AABB(p.x - 0.25, p.y, p.z - 0.25, p.x + 0.25, p.y + 0.5, p.z + 0.25);
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolacao;
    }

    @Override
    public LivingEntity getControllingPassenger() {
        Entity d = dono();
        return d instanceof LivingEntity vivo && hasPassenger(d) && estado() == PILOTADO ? vivo : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passageiro) {
        return getPassengers().isEmpty() && passageiro.getId() == donoId();
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passageiro, EntityDimensions dim, float escala) {
        // Em pé no meio da ilha (Avatar.DEFAULT_VEHICLE_ATTACHMENT sobe o assento 0,6).
        return new Vec3(0, 0.6, 0);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passageiro) {
        float yaw = passageiro.getYRot() * Mth.DEG_TO_RAD;
        return position().add(-Mth.sin(yaw) * 1.2, 0.02, Mth.cos(yaw) * 1.2);
    }

    @Override
    public boolean isFlyingVehicle() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource fonte, float dano) {
        return false;
    }

    @Override
    public boolean causeFallDamage(double distancia, float mult, DamageSource fonte) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 256 * 256;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {}

    @Override
    protected void readAdditionalSaveData(ValueInput in) {}

    @Override
    public void embarcar(ServerPlayer dono) {
        if (dono.getId() != donoId() || estado() == INDO || estado() == CHEGANDO || hasPassenger(dono)) return;
        dono.startRiding(this, true, true);
        entityData.set(ESTADO, PILOTADO);
        tempoEstado = 0;
        dono.resetFallDistance();
        Guia.concluir(dono, "ilha");
    }

    @Override
    public void chamar(ServerPlayer dono) {
        if (dono.distanceTo(this) < 14) embarcar(dono);
    }

    /** Pede para a ilha assentar onde está (fim do tempo, dono foi embora, destransformou). */
    @Override
    public void irEmbora() {
        if (level() instanceof ServerLevel level && estado() != INDO) assentar(level);
    }

    @Override
    public void comando(ServerPlayer piloto, int acao) {
        if (acao == ESPECIAL && piloto.getId() == donoId() && estado() == PILOTADO && level() instanceof ServerLevel level) assentar(level);
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (!carregada) {
                Rede.IlhaBlocos b = com.danrod.dcuniverse.core.EstadoCliente.ILHAS.get(getId());
                if (b != null) definirBlocos(b.pos(), b.estados());
            }
            if (ultima == null) ultima = position();
            if (isLocalInstanceAuthoritative()) interpolacao.cancel();
            else interpolacao.interpolate();
            cliente.accept(this);
            ultima = position();
            atualizarPartes();
            return;
        }
        tickServidor((ServerLevel) level());
    }

    /** Quanto a ilha andou desde o fim do tick anterior (para levar junto quem está em cima). */
    public Vec3 andou() {
        return ultima == null ? Vec3.ZERO : position().subtract(ultima);
    }

    private void tickServidor(ServerLevel level) {
        tempoEstado++;
        Entity dono = dono();
        int restante = Math.max(0, restante() - 1);
        entityData.set(RESTANTE, restante);
        if (estado() == PILOTADO && (dono == null || !hasPassenger(dono))) {
            entityData.set(ESTADO, PARADO);
            tempoEstado = 0;
        }
        if (estado() == CHEGANDO) {
            float t = Math.min(1f, tempoEstado / (float) SUBIDA);
            float e = t * t * (3 - 2 * t);
            setPos(getX(), yInicial + ALTURA_SUBIDA * e + Math.sin(tempoEstado * 0.9) * 0.03 * (1 - t), getZ());
            if (tempoEstado % 2 == 0) poeira(level, 1 - t);
            if (t >= 1) {
                entityData.set(ESTADO, PARADO);
                tempoEstado = 0;
                if (dono instanceof ServerPlayer sp && sp.distanceTo(this) < 14) embarcar(sp);
            }
        } else if (estado() == PARADO) {
            setPos(getX(), getY() + Math.sin(tickCount * 0.05) * 0.004, getZ());
        }
        if (restante == 10 * 20 && dono instanceof ServerPlayer sp && sp.connection != null) {
            sp.sendOverlayMessage(Component.translatable("dcuniverse.ilha.aviso_fim"));
        }
        if (dono == null || !dono.isAlive() || restante <= 0
            || !(dono instanceof ServerPlayer sp && com.danrod.dcuniverse.character.EstadoJogador.de(sp).transformado())) {
            assentar(level);
            return;
        }
        levarJunto(level);
        if (tickCount % 3 == 0 && estado() != CHEGANDO) poeira(level, 0.15f);
        ultima = position();
        atualizarPartes();
    }

    /** Criaturas e itens em cima andam com a ilha; jogadores em cima não são chutados por "flutuar". */
    private void levarJunto(ServerLevel level) {
        Vec3 d = andou();
        AABB acima = caixaBlocos().inflate(0.5, 1.5, 0.5);
        for (Entity e : level.getEntities(this, acima, e -> !(e instanceof Parte) && !e.isPassenger())) {
            if (!emCima(e)) continue;
            if (e instanceof ServerPlayer sp) {
                if (sp.connection != null) ((com.danrod.dcuniverse.mixin.ServerGamePacketListenerImplAccessor) sp.connection).dcuniverse$setAboveGroundTickCount(0);
                sp.resetFallDistance();
            } else if (d.lengthSqr() > 1e-8) {
                e.setPos(e.position().add(d));
                e.resetFallDistance();
            }
        }
    }

    /** Terra e pedrinhas caindo de baixo da ilha (forca = quanto cai, de 0 a 1). */
    private void poeira(ServerLevel level, float forca) {
        if (celulas.isEmpty()) return;
        int n = 1 + Math.round(forca * 10);
        for (int k = 0; k < n; k++) {
            int[] c = celulas.get(random.nextInt(celulas.size()));
            if (mapa.containsKey(chave(c[0], c[1] - 1, c[2]))) continue;
            BlockState st = bloco(c[0], c[1], c[2]);
            double x = getX() + c[0] + (random.nextDouble() - 0.5), y = getY() + c[1] - 0.05, z = getZ() + c[2] + (random.nextDouble() - 0.5);
            level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, st), x, y, z, 1, 0.1, 0, 0.1, 0);
            if (forca > 0.5f && random.nextInt(3) == 0) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), x, y, z, 3, 0.2, 0.1, 0.2, 0.05);
            }
        }
    }

    /**
     * A ilha vira blocos de verdade onde está (base aérea permanente). O que não couber (já tem bloco ali) cai
     * como item. Quem estava em cima fica em cima.
     */
    public void assentar(ServerLevel level) {
        if (estado() == INDO) return;
        entityData.set(ESTADO, INDO);
        for (Entity p : getPassengers()) p.stopRiding();
        int bx = (int) Math.round(getX() - 0.5), by = (int) Math.round(getY()), bz = (int) Math.round(getZ() - 0.5);
        Entity dono = dono();
        for (int i = 0; i < pos.length; i++) {
            BlockPos alvo = new BlockPos(bx + px(pos[i]), by + py(pos[i]), bz + pz(pos[i]));
            BlockState st = Block.stateById(ids[i]);
            BlockState la = level.getBlockState(alvo);
            boolean pode = !(dono instanceof ServerPlayer sp) || level.mayInteract(sp, alvo);
            if (pode && level.isLoaded(alvo) && la.canBeReplaced() && la.getFluidState().isEmpty()) {
                level.setBlock(alvo, st, Block.UPDATE_CLIENTS);
            } else {
                ItemStack it = new ItemStack(st.getBlock().asItem());
                if (!it.isEmpty()) Block.popResource(level, alvo, it);
            }
        }
        // Todos em cima (inclusive o piloto) acompanham o arredondamento da posição.
        Vec3 ajuste = new Vec3(bx + 0.5 - getX(), by - getY(), bz + 0.5 - getZ());
        for (Entity e : level.getEntities(this, caixaBlocos().inflate(0.5, 2, 0.5), e -> !(e instanceof Parte))) {
            if (emCima(e)) e.teleportTo(e.getX() + ajuste.x, e.getY() + ajuste.y + 0.01, e.getZ() + ajuste.z);
        }
        level.playSound(null, getX(), getY(), getZ(), Sons.ILHA_POUSA.get(), SoundSource.PLAYERS, 2.2f, 1f);
        poeira(level, 1f);
        if (dono instanceof ServerPlayer sp && sp.connection != null) sp.sendOverlayMessage(Component.translatable("dcuniverse.ilha.assentou"));
        discard();
    }

    // ------------------------------------------------------------------ parte

    /** Uma coluna de blocos sólidos da ilha, como caixa de colisão. */
    public static final class Parte extends PartEntity<IlhaTelecinetica> {
        float[] local;

        Parte(IlhaTelecinetica ilha) {
            super(ilha);
            noPhysics = true;
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder b) {}

        @Override
        protected void readAdditionalSaveData(ValueInput in) {}

        @Override
        protected void addAdditionalSaveData(ValueOutput out) {}

        @Override
        public boolean canBeCollidedWith(Entity outro) {
            // No servidor, o jogador não colide (o movimento dele é do cliente, que já colidiu com a ilha que vê).
            return local != null && (level().isClientSide() || !(outro instanceof Player));
        }

        @Override
        public boolean isPickable() {
            return false;
        }

        @Override
        public boolean hurtServer(ServerLevel level, DamageSource fonte, float dano) {
            return false;
        }

        @Override
        public boolean shouldBeSaved() {
            return false;
        }

        @Override
        public boolean is(Entity outro) {
            return this == outro || getParent() == outro;
        }
    }
}
