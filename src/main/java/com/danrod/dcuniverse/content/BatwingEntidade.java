package com.danrod.dcuniverse.content;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.ability.QuedaAmortecida;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.guia.Guia;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * O Batwing, veículo da suprema do Batman (tipo vehicle). Chega voando, embarca o dono como piloto e tem um
 * assento para mais um jogador. Pilotado, o movimento é do cliente do piloto (como o barco do jogo; a física
 * fica em client/Piloto) e o servidor confere pelo pacote de veículo do próprio jogo. Armas (pacote Rede.Veiculo):
 * metralhadoras nas asas (botão de atacar, esquentam), mísseis teleguiados (botão de usar) e modo furtivo
 * (tecla de usar habilidade): mobs perdem o piloto de vista. Sem piloto, fica pairando no lugar até o dono voltar
 * (clique direito ou o gancho nele). No fim da duração, os passageiros descem planando e ele vai embora.
 */
public class BatwingEntidade extends Entity implements VeiculoDono {
    public static final int PAIRANDO = PARADO;
    /** Escala do modelo (client/BatwingModelo é desenhado em blocos e aumentado por ela): o Batman cabe na cabine. */
    public static final float ESCALA = 1.9f;
    /** Altura do assento acima dos pés da entidade (o piloto senta afundado na fuselagem, a cabeça sob o vidro). */
    static final double ASSENTO_Y = 0.7;
    /** Altura do plano da asa no modelo, onde o desenho gira (client/BatwingModelo.Y0), antes da escala. */
    public static final float PIVO = 0.42f;
    public static final int MISSIL = SECUNDARIA, FURTIVO = ESPECIAL;
    private static final Identifier CAMERA = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "batwing_camera");

    private static final EntityDataAccessor<Integer> DONO = SynchedEntityData.defineId(BatwingEntidade.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ESTADO = SynchedEntityData.defineId(BatwingEntidade.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> OCULTO = SynchedEntityData.defineId(BatwingEntidade.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> RESTANTE = SynchedEntityData.defineId(BatwingEntidade.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MISSEIS = SynchedEntityData.defineId(BatwingEntidade.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> CALOR = SynchedEntityData.defineId(BatwingEntidade.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> RECARGA_MISSIL = SynchedEntityData.defineId(BatwingEntidade.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ALVO = SynchedEntityData.defineId(BatwingEntidade.class, EntityDataSerializers.INT);

    /** Física do piloto, posta por client/Piloto (o código comum não conhece as classes de cliente). */
    public static Consumer<BatwingEntidade> controleCliente = b -> {};

    private final InterpolationHandler interpolacao = new InterpolationHandler(this, 3);

    // Parâmetros (do JSON da habilidade) e estado do servidor.
    private Params prm = Params.EMPTY;
    private int duracao = 1800;
    private boolean atirando, superaquecido;
    private int tiros;
    private Vec3 chegada = Vec3.ZERO, origemChegada = Vec3.ZERO;
    private int tempoEstado;
    private boolean donoABordo;

    /** Velocidade (blocos/tick) e inclinação lateral visual, mantidas no cliente. */
    public float velocidade, rolagem, rolagemO;

    public BatwingEntidade(EntityType<? extends BatwingEntidade> tipo, Level level) {
        super(tipo, level);
        setNoGravity(true);
    }

    /** Invoca o Batwing para o dono: desce do céu atrás dele, ou aparece no lugar se não houver céu aberto. */
    public static BatwingEntidade invocar(ServerPlayer dono, Params prm) {
        ServerLevel level = dono.level();
        BatwingEntidade b = new BatwingEntidade(Entidades.BATWING.get(), level);
        b.prm = prm;
        b.duracao = Math.round(prm.f("duration", 90) * 20);
        b.entityData.set(DONO, dono.getId());
        b.entityData.set(RESTANTE, b.duracao);
        b.entityData.set(MISSEIS, prm.i("missiles", 4));
        Vec3 alvo = dono.position().add(0, 0.6, 0);
        float yaw = dono.getYRot();
        Vec3 atras = new Vec3(Mth.sin(yaw * Mth.DEG_TO_RAD), 0, -Mth.cos(yaw * Mth.DEG_TO_RAD));
        Vec3 de = alvo.add(atras.scale(26)).add(0, 20, 0);
        boolean ceu = level.clip(new ClipContext(alvo.add(0, 1, 0), de, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, b)).getType() == HitResult.Type.MISS;
        b.chegada = alvo;
        b.origemChegada = ceu ? de : alvo;
        b.snapTo(b.origemChegada.x, b.origemChegada.y, b.origemChegada.z, yaw, ceu ? 20 : 0);
        b.noPhysics = true;
        level.addFreshEntity(b);
        if (!ceu) b.embarcar(dono);
        level.playSound(null, dono.getX(), dono.getY(), dono.getZ(), Sons.BATWING_CHEGA.get(), SoundSource.PLAYERS, 1.6f, 1f);
        return b;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(DONO, -1);
        b.define(ESTADO, CHEGANDO);
        b.define(OCULTO, false);
        b.define(RESTANTE, 0);
        b.define(MISSEIS, 0);
        b.define(CALOR, 0f);
        b.define(RECARGA_MISSIL, 0);
        b.define(ALVO, -1);
    }

    // ------------------------------------------------------------------ consultas

    @Override
    public int estado() {
        return entityData.get(ESTADO);
    }

    public boolean oculto() {
        return entityData.get(OCULTO);
    }

    /** Ticks até o Batwing ir embora. */
    public int restante() {
        return entityData.get(RESTANTE);
    }

    public int misseis() {
        return entityData.get(MISSEIS);
    }

    public float calor() {
        return entityData.get(CALOR);
    }

    /** Fração da recarga do próximo míssil (0 = acabou de disparar, 1 = pronto). */
    public float recargaMissil() {
        int total = Math.round(prm.f("missile_reload", 5) * 20);
        return 1f - entityData.get(RECARGA_MISSIL) / (float) Math.max(1, total);
    }

    /** Entidade que o míssil vai perseguir (-1 = nenhuma), para o piloto ver a mira travada. */
    public int alvoTravado() {
        return entityData.get(ALVO);
    }

    @Override
    public int donoId() {
        return entityData.get(DONO);
    }

    public Entity dono() {
        return level().getEntity(donoId());
    }

    /**
     * Quem está dentro do Batwing não se machuca com o próprio jato: batida em parede (a cabeça entra no bloco e
     * sufoca), afogamento (a cabine é fechada), queda do jato, aperto e os próprios tiros e mísseis.
     */
    public static void registrar() {
        LivingAttackEvent.BUS.addListener((Predicate<LivingAttackEvent>) e -> {
            // Vale para todos os veículos da suprema (content/VeiculoDono).
            if (!(e.getEntity().getVehicle() instanceof VeiculoDono) || !(e.getEntity().getVehicle() instanceof Entity b)) return false;
            DamageSource f = e.getSource();
            return f.is(DamageTypes.IN_WALL) || f.is(DamageTypes.DROWN) || f.is(DamageTypes.FALL) || f.is(DamageTypes.FLY_INTO_WALL) || f.is(DamageTypes.CRAMMING)
                || f.getEntity() != null && (f.getEntity() == b || b.hasPassenger(f.getEntity()))
                || f.getDirectEntity() == b;
        });
    }

    @Override
    public boolean causeFallDamage(double distancia, float mult, DamageSource fonte) {
        return false;
    }

    /** O Batwing que o jogador pilota agora, ou null. */
    public static BatwingEntidade pilotando(Player player) {
        return player.getVehicle() instanceof BatwingEntidade b && b.donoId() == player.getId() ? b : null;
    }

    public Params params() {
        return prm;
    }

    // ------------------------------------------------------------------ passageiros

    @Override
    public LivingEntity getControllingPassenger() {
        Entity d = dono();
        return d instanceof LivingEntity vivo && hasPassenger(d) ? vivo : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passageiro) {
        return getPassengers().size() < 2 && estado() != INDO;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passageiro, EntityDimensions dim, float escala) {
        boolean piloto = passageiro.getId() == donoId();
        // O assento gira com o jato inteiro (rumo, nariz e inclinação), em volta do mesmo pivô do desenho:
        // assim quem está sentado acompanha a cabine nas curvas em vez de ficar parado onde ela estava.
        float pivo = PIVO * ESCALA;
        Vector3f v = rotacao(1).transform(new Vector3f(0, (float) ASSENTO_Y - pivo, (piloto ? 0.75f : 0.2f) * ESCALA));
        return new Vec3(v.x, v.y + pivo, v.z);
    }

    /** A rotação com que o Batwing é desenhado (client/render/BatwingRenderer): rumo, depois nariz, depois inclinação. */
    public Quaternionf rotacao(float pt) {
        float yaw = Mth.rotLerp(pt, yRotO, getYRot()), pitch = Mth.lerp(pt, xRotO, getXRot()), roll = Mth.lerp(pt, rolagemO, rolagem);
        return new Quaternionf().rotationY(-yaw * Mth.DEG_TO_RAD).rotateX(pitch * Mth.DEG_TO_RAD).rotateZ(roll * Mth.DEG_TO_RAD);
    }

    /** Só a inclinação do jato (nariz e asas) vista de fora, para girar o corpo de quem está sentado nele. */
    public Quaternionf inclinacao(float pt) {
        float yaw = Mth.rotLerp(pt, yRotO, getYRot());
        return rotacao(pt).rotateY(yaw * Mth.DEG_TO_RAD);
    }

    @Override
    protected void positionRider(Entity passageiro, MoveFunction mover) {
        super.positionRider(passageiro, mover);
        // Sentado de frente para o painel, como num barco: o corpo segue o jato, a cabeça olha livre.
        if (passageiro instanceof LivingEntity vivo) {
            vivo.setYBodyRot(getYRot());
            vivo.yBodyRotO = yRotO;
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passageiro) {
        // Salta por baixo da asa, já no ar; perto do chão, desce pelo lado.
        Vec3 baixo = position().add(0, -passageiro.getBbHeight() - 0.1, 0);
        if (level().noCollision(passageiro, passageiro.getDimensions(passageiro.getPose()).makeBoundingBox(baixo))) return baixo;
        Vec3 lado = position().add(new Vec3(-3.6 * ESCALA, 0.2, 0).yRot(-getYRot() * Mth.DEG_TO_RAD));
        if (level().noCollision(passageiro, passageiro.getDimensions(passageiro.getPose()).makeBoundingBox(lado))) return lado;
        return super.getDismountLocationForPassenger(passageiro);
    }

    @Override
    public boolean isFlyingVehicle() {
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 onde) {
        if (player.isSecondaryUseActive() || hasPassenger(player)) return InteractionResult.PASS;
        if (!level().isClientSide()) {
            if (player.getId() == donoId()) embarcar((ServerPlayer) player);
            else if (getPassengers().size() < (hasPassenger(dono()) ? 2 : 1)) player.startRiding(this);
        }
        return InteractionResult.SUCCESS;
    }

    /** O dono entra no assento do piloto (pelo clique, pelo gancho ou na chegada). */
    @Override
    public void embarcar(ServerPlayer dono) {
        if (dono.getId() != donoId() || estado() == INDO || hasPassenger(dono)) return;
        // O assento do piloto é o primeiro: quem estava sozinho atrás continua atrás.
        dono.startRiding(this, true, true);
        entityData.set(ESTADO, PILOTADO);
        tempoEstado = 0;
        noPhysics = false;
        velocidade = 0.5f;
        dono.resetFallDistance();
        var cam = dono.getAttribute(Attributes.CAMERA_DISTANCE);
        if (cam != null) cam.addOrUpdateTransientModifier(new AttributeModifier(CAMERA, 6 * ESCALA, AttributeModifier.Operation.ADD_VALUE));
        Guia.concluir(dono, "batwing");
    }

    private static void tirarCamera(Entity e) {
        if (e instanceof LivingEntity vivo && vivo.getAttribute(Attributes.CAMERA_DISTANCE) != null) {
            vivo.getAttribute(Attributes.CAMERA_DISTANCE).removeModifier(CAMERA);
        }
    }

    @Override
    protected void removePassenger(Entity passageiro) {
        super.removePassenger(passageiro);
        tirarCamera(passageiro);
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolacao;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith(Entity outro) {
        return true;
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
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 256 * 256;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {}

    @Override
    protected void readAdditionalSaveData(ValueInput in) {}

    // ------------------------------------------------------------------ tick

    @Override
    public void tick() {
        super.tick();
        rolagemO = rolagem;
        if (level().isClientSide()) {
            if (isLocalInstanceAuthoritative()) {
                // Uma interpolação que sobrou de antes de embarcar faria o cliente mandar ao servidor a posição velha.
                interpolacao.cancel();
                controleCliente.accept(this);
            } else {
                interpolacao.interpolate();
                // Quem vê de fora estima a inclinação pela curva.
                float giro = Mth.wrapDegrees(getYRot() - yRotO);
                rolagem += (Mth.clamp(-giro * 7, -55, 55) - rolagem) * 0.2f;
            }
            particulas();
            return;
        }
        tickServidor((ServerLevel) level());
    }

    private void particulas() {
        if (oculto() || estado() == CHEGANDO && tickCount < 3) return;
        Vec3 tras = Vec3.directionFromRotation(getXRot(), getYRot()).scale(-1);
        Vec3 bocal = position().add(0, 0.45 * ESCALA, 0).add(tras.scale(1.6 * ESCALA));
        if (random.nextInt(2) == 0) level().addParticle(ParticleTypes.SMOKE, bocal.x, bocal.y, bocal.z, tras.x * 0.1, 0.01, tras.z * 0.1);
    }

    private void tickServidor(ServerLevel level) {
        tempoEstado++;
        Entity dono = dono();
        int restante = Math.max(0, restante() - 1);
        entityData.set(RESTANTE, restante);
        boolean aBordo = dono != null && hasPassenger(dono);
        if (donoABordo && !aBordo && estado() == PILOTADO) {
            // O dono saltou: o Batwing fica pairando e o Batman desce planando.
            entityData.set(ESTADO, PAIRANDO);
            tempoEstado = 0;
            entityData.set(OCULTO, false);
            atirando = false;
            if (dono instanceof ServerPlayer sp) {
                tirarCamera(sp);
                sp.resetFallDistance();
                QuedaAmortecida.iniciarSeNoAr(sp);
            }
        }
        donoABordo = aBordo;
        if (estado() != INDO && (dono == null || !dono.isAlive() || restante <= 0 || !(dono instanceof ServerPlayer sp && com.danrod.dcuniverse.character.EstadoJogador.de(sp).transformado()))) {
            irEmbora();
        }
        switch (estado()) {
            case CHEGANDO -> chegar(dono);
            case PAIRANDO -> pairar();
            case INDO -> {
                Vec3 frente = Vec3.directionFromRotation(-25, getYRot());
                float v = Math.min(3f, 0.6f + tempoEstado * 0.08f);
                setPos(position().add(frente.scale(v)));
                if (tempoEstado > 60 || level.getNearestPlayer(this, 128) == null) discard();
            }
            default -> {}
        }
        armas(level, dono);
    }

    private void chegar(Entity dono) {
        int total = 36;
        float t = Math.min(1f, tempoEstado / (float) total);
        // Mergulha do céu freando no fim; o alvo segue o dono enquanto ele anda.
        if (dono != null) chegada = dono.position().add(0, 0.6, 0);
        float e = 1 - (1 - t) * (1 - t) * (1 - t);
        Vec3 p = origemChegada.lerp(chegada, e);
        Vec3 dir = chegada.subtract(origemChegada);
        float yaw = (float) (Mth.atan2(dir.z, dir.x) * Mth.RAD_TO_DEG) - 90;
        setPos(p);
        setYRot(yaw);
        setXRot(20 * (1 - t));
        if (t >= 1) {
            noPhysics = false;
            if (dono instanceof ServerPlayer sp && sp.distanceTo(this) < 6) embarcar(sp);
            else {
                entityData.set(ESTADO, PAIRANDO);
                tempoEstado = 0;
            }
        }
    }

    private void pairar() {
        // Paira balançando devagar e girando um pouco, à espera do dono.
        setXRot(getXRot() * 0.9f);
        setYRot(getYRot() + 0.35f);
        double sobe = Math.sin(tempoEstado * 0.08) * 0.02;
        move(MoverType.SELF, new Vec3(0, sobe, 0));
    }

    /** Os passageiros descem planando e o Batwing sobe e vai embora. */
    @Override
    public void irEmbora() {
        if (estado() == INDO) return;
        Entity dono = dono();
        for (Entity p : getPassengers()) {
            p.stopRiding();
            tirarCamera(p);
            if (p instanceof ServerPlayer sp) {
                sp.resetFallDistance();
                QuedaAmortecida.iniciarSeNoAr(sp);
            }
        }
        if (dono instanceof ServerPlayer sp) tirarCamera(sp);
        entityData.set(ESTADO, INDO);
        entityData.set(OCULTO, false);
        tempoEstado = 0;
        noPhysics = true;
        atirando = false;
        level().playSound(null, getX(), getY(), getZ(), Sons.BATWING_CHEGA.get(), SoundSource.PLAYERS, 1.4f, 1.2f);
    }

    // ------------------------------------------------------------------ armas

    /** Comando do piloto (pacote Rede.Veiculo ou a tecla de usar habilidade). */
    @Override
    public void comando(ServerPlayer piloto, int acao) {
        if (pilotando(piloto) != this || estado() != PILOTADO) return;
        switch (acao) {
            case TIRO_LIGA -> {
                atirando = true;
                revelar();
            }
            case TIRO_DESLIGA -> atirando = false;
            case MISSIL -> missil(piloto);
            case FURTIVO -> {
                boolean novo = !oculto();
                entityData.set(OCULTO, novo);
                level().playSound(null, getX(), getY(), getZ(), novo ? Sons.BATWING_FURTIVO.get() : Sons.BATWING_REVELA.get(),
                    SoundSource.PLAYERS, 1f, 1f);
                if (novo) esquecerAlvos();
            }
            default -> {}
        }
    }

    private void revelar() {
        if (!oculto()) return;
        entityData.set(OCULTO, false);
        level().playSound(null, getX(), getY(), getZ(), Sons.BATWING_REVELA.get(), SoundSource.PLAYERS, 1f, 1f);
    }

    /** Mobs que miravam o piloto, o passageiro ou o Batwing perdem o alvo. */
    private void esquecerAlvos() {
        for (Mob mob : level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(64))) {
            LivingEntity alvo = mob.getTarget();
            if (alvo != null && (hasPassenger(alvo) || alvo == dono())) mob.setTarget(null);
        }
    }

    private void armas(ServerLevel level, Entity dono) {
        float calor = calor();
        boolean pode = estado() == PILOTADO && dono instanceof ServerPlayer;
        if (pode && atirando && !superaquecido) {
            if (tickCount % 2 == 0) tiro(level, (ServerPlayer) dono);
            calor += 0.022f;
            if (calor >= 1) {
                superaquecido = true;
                level.playSound(null, getX(), getY(), getZ(), Sons.BATWING_SUPERAQUECE.get(), SoundSource.PLAYERS, 0.9f, 1f);
            }
        } else {
            calor -= superaquecido ? 0.012f : 0.02f;
            if (calor <= 0.25f) superaquecido = false;
        }
        entityData.set(CALOR, Mth.clamp(calor, 0, 1));
        // Mísseis: recarregam um por vez.
        int max = prm.i("missiles", 4);
        int rec = entityData.get(RECARGA_MISSIL);
        if (misseis() < max) {
            if (rec <= 0) rec = Math.round(prm.f("missile_reload", 5) * 20);
            else if (--rec <= 0) entityData.set(MISSEIS, misseis() + 1);
            entityData.set(RECARGA_MISSIL, Math.max(0, rec));
        }
        if (oculto() && tickCount % 10 == 0) esquecerAlvos();
        if (pode && tickCount % 4 == 0) {
            Entity alvo = procurarAlvo((ServerPlayer) dono);
            entityData.set(ALVO, alvo == null ? -1 : alvo.getId());
        }
    }

    /** Ponto de saída das armas na asa (lado -1 esquerda, 1 direita), no mundo. */
    public Vec3 bocal(int lado, float frente) {
        Vec3 v = new Vec3(lado * 1.15, 0.32, 1.4 + frente).scale(ESCALA);
        return position().add(v.xRot(-getXRot() * Mth.DEG_TO_RAD).yRot(-getYRot() * Mth.DEG_TO_RAD));
    }

    private void tiro(ServerLevel level, ServerPlayer piloto) {
        tiros++;
        int lado = tiros % 2 == 0 ? 1 : -1;
        Vec3 de = bocal(lado, 0.3f);
        double alcance = prm.d("gun_range", 64);
        // As duas metralhadoras convergem no ponto da mira (senão os tiros passam dos dois lados de um alvo no centro).
        Vec3 dir = mira(piloto, alcance).subtract(de).normalize();
        dir = dir.add((random.nextDouble() - 0.5) * 0.02, (random.nextDouble() - 0.5) * 0.02, (random.nextDouble() - 0.5) * 0.02).normalize();
        Vec3 fim = de.add(dir.scale(alcance));
        BlockHitResult bloco = level.clip(new ClipContext(de, fim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 limite = bloco.getType() == HitResult.Type.MISS ? fim : bloco.getLocation();
        EntityHitResult ent = ProjectileUtil.getEntityHitResult(level, this, de, limite, new AABB(de, limite).inflate(1),
            e -> e != this && !hasPassenger(e) && Alvos.podeAtingir(piloto, e), 0.3f);
        Vec3 acerto = limite;
        if (ent != null) {
            acerto = ent.getLocation();
            Alvos.ferir(piloto, ent.getEntity(), damageSources().thrown(this, piloto), (float) prm.d("gun_damage", 1) * 2);
            ent.getEntity().invulnerableTime = 0;
        }
        Rede.efeito(level, de.x, de.y, de.z, "tiro", (float) (acerto.x - de.x), (float) (acerto.z - de.z),
            Float.floatToIntBits((float) (acerto.y - de.y)) );
        if (tiros % 2 == 0) level.playSound(null, de.x, de.y, de.z, Sons.BATWING_TIRO.get(), SoundSource.PLAYERS, 1.2f, 0.95f + random.nextFloat() * 0.1f);
    }

    /** Onde a mira do piloto aponta: o primeiro bloco ou criatura na linha do olhar, até alcance blocos. */
    public Vec3 mira(ServerPlayer piloto, double alcance) {
        Vec3 olhos = piloto.getEyePosition();
        Vec3 fim = olhos.add(piloto.getLookAngle().scale(alcance));
        BlockHitResult bloco = level().clip(new ClipContext(olhos, fim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 limite = bloco.getType() == HitResult.Type.MISS ? fim : bloco.getLocation();
        EntityHitResult ent = ProjectileUtil.getEntityHitResult(level(), this, olhos, limite, new AABB(olhos, limite).inflate(1),
            e -> e != this && !hasPassenger(e) && e instanceof LivingEntity && !e.isSpectator(), 0.3f);
        return ent != null ? ent.getLocation() : limite;
    }

    /** O melhor alvo para o míssil: o mob hostil mais perto do centro da mira (cone de 18°, até 96 blocos). */
    public Entity procurarAlvo(ServerPlayer piloto) {
        Vec3 olhos = piloto.getEyePosition();
        Vec3 dir = piloto.getLookAngle();
        double cos = Math.cos(Math.toRadians(prm.d("lock_angle", 18)));
        double alcance = prm.d("lock_range", 96);
        Entity melhor = null;
        double melhorC = cos;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(alcance),
            e -> e != piloto && !hasPassenger(e) && Alvos.podeAtingir(piloto, e) && (e instanceof net.minecraft.world.entity.monster.Enemy || e instanceof Player))) {
            Vec3 para = e.getBoundingBox().getCenter().subtract(olhos);
            double d = para.length();
            if (d > alcance || d < 2) continue;
            double c = para.dot(dir) / d;
            if (c > melhorC && piloto.hasLineOfSight(e)) {
                melhorC = c;
                melhor = e;
            }
        }
        return melhor;
    }

    private void missil(ServerPlayer piloto) {
        if (misseis() <= 0) return;
        revelar();
        int n = misseis();
        entityData.set(MISSEIS, n - 1);
        int lado = n % 2 == 0 ? 1 : -1;
        Vec3 de = position().add(new Vec3(lado * (n > 2 ? 2.3 : 1.85), 0.05, 0.4).scale(ESCALA).yRot(-getYRot() * Mth.DEG_TO_RAD));
        Entity alvo = procurarAlvo(piloto);
        Vec3 dir = mira(piloto, prm.d("lock_range", 96)).subtract(de).normalize();
        MissilEntidade m = new MissilEntidade(level(), piloto, de, dir, position().subtract(xo, yo, zo).length(), alvo,
            (float) prm.d("missile_damage", 7) * 2, prm.f("missile_radius", 3.5f));
        level().addFreshEntity(m);
        level().playSound(null, de.x, de.y, de.z, Sons.BATWING_MISSIL.get(), SoundSource.PLAYERS, 1.5f, 1f);
    }

    @Override
    public void remove(RemovalReason motivo) {
        for (Entity p : getPassengers()) tirarCamera(p);
        super.remove(motivo);
    }
}
