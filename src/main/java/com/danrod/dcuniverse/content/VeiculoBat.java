package com.danrod.dcuniverse.content;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.ability.active.Fumaca;
import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.guia.Guia;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Os veículos de chão e de água da suprema do Batman, escolhidos no Batcomputador (o Batwing é content/
 * BatwingEntidade): Batmóvel, Batmoto e Batbarco. Uma classe só, com o modelo vindo do tipo de entidade. Como o
 * Batwing, o movimento de quem pilota é do cliente (client/Motorista, como o barco do jogo) e o servidor cuida da
 * chegada, das armas, do atropelamento e da hora de ir embora.
 * <ul>
 * <li>Batmóvel: chega dirigindo; metralhadoras no capô, mísseis teleguiados no teto e casco elétrico (V) que
 * dá choque em quem está em volta. Freio de mão (pulo) faz derrapar. Atropela forte.</li>
 * <li>Batmoto: a mais rápida e ágil; metralhadoras leves, lançador de batarangues e cortina de fumaça (V) atrás
 * da moto. O pulo faz a moto saltar obstáculos.</li>
 * <li>Batbarco: lancha que mergulha (V) e vira submarino; metralhadoras na superfície e torpedos teleguiados.
 * Só aparece se houver água perto; quem está a bordo não se afoga.</li>
 * </ul>
 */
public class VeiculoBat extends Entity implements VeiculoDono {
    /** Dados de cada modelo: lugares (x, altura do assento, z; o piloto primeiro), distância extra da câmera e degrau. */
    public record Modelo(String id, Vec3[] assentos, float camera, float degrau, double ladoSaida) {}

    public static final Modelo BATMOVEL = new Modelo("batmovel",
        new Vec3[] {new Vec3(0.42, 0.18, -0.05), new Vec3(-0.42, 0.18, -0.05)}, 3.5f, 1.1f, 1.8);
    public static final Modelo BATMOTO = new Modelo("batmoto",
        new Vec3[] {new Vec3(0, 0.74, -0.22), new Vec3(0, 0.8, -0.68)}, 1.5f, 1.1f, 1.1);
    public static final Modelo BATBARCO = new Modelo("batbarco",
        new Vec3[] {new Vec3(0.4, 0.2, 0.15), new Vec3(-0.4, 0.2, 0.15)}, 3f, 0.6f, 1.8);
    private static final Map<String, Modelo> MODELOS = Map.of("batmovel", BATMOVEL, "batmoto", BATMOTO, "batbarco", BATBARCO);

    private static final Identifier CAMERA = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "veiculo_camera");

    private static final EntityDataAccessor<Integer> DONO = SynchedEntityData.defineId(VeiculoBat.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ESTADO = SynchedEntityData.defineId(VeiculoBat.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RESTANTE = SynchedEntityData.defineId(VeiculoBat.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> CALOR = SynchedEntityData.defineId(VeiculoBat.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> MUNICAO = SynchedEntityData.defineId(VeiculoBat.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RECARGA = SynchedEntityData.defineId(VeiculoBat.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ALVO = SynchedEntityData.defineId(VeiculoBat.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ESPECIAL_CD = SynchedEntityData.defineId(VeiculoBat.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SUBMERSO = SynchedEntityData.defineId(VeiculoBat.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FAROL = SynchedEntityData.defineId(VeiculoBat.class, EntityDataSerializers.BOOLEAN);

    /** Física de quem pilota, posta por client/Motorista (o código comum não conhece as classes de cliente). */
    public static Consumer<VeiculoBat> controleCliente = v -> {};

    private final InterpolationHandler interpolacao = new InterpolationHandler(this, 3);
    private final Modelo modelo;

    // Parâmetros (do JSON da habilidade) e estado do servidor.
    private Params prm = Params.EMPTY;
    private boolean atirando, superaquecido;
    private int tiros, tempoEstado, preso;
    private boolean donoABordo;
    private Vec3 subida = Vec3.ZERO;
    private final Map<Integer, Long> atropelados = new HashMap<>();

    /**
     * Estado do cliente: velocidade para a frente (blocos/tick, negativa de ré), rumo do movimento (difere do
     * nariz na derrapagem), esterço das rodas, inclinações visuais, turbo e o giro das rodas.
     */
    public float velocidade, rumo, esterco, estercoO, rolagem, rolagemO, arfagem, arfagemO, giro, giroO;
    public float turbo = 1;
    public boolean turboLigado, derrapando;
    public int puloEspera;

    public VeiculoBat(EntityType<? extends VeiculoBat> tipo, Level level) {
        super(tipo, level);
        String id = EntityType.getKey(tipo).getPath();
        modelo = MODELOS.getOrDefault(id, BATMOVEL);
    }

    // ------------------------------------------------------------------ invocar

    /**
     * Chama o veículo para o dono. Os de chão vêm dirigindo de trás dele quando há caminho (senão aparecem do lado
     * numa nuvem de fumaça); o Batbarco sobe do fundo da água mais perto. @return null se não deu (barco sem água)
     */
    public static VeiculoBat invocar(ServerPlayer dono, String modelo, Params prm) {
        ServerLevel level = dono.level();
        EntityType<VeiculoBat> tipo = switch (modelo) {
            case "batmoto" -> Entidades.BATMOTO.get();
            case "batbarco" -> Entidades.BATBARCO.get();
            default -> Entidades.BATMOVEL.get();
        };
        VeiculoBat v = new VeiculoBat(tipo, level);
        v.prm = prm;
        int dur = Math.round(prm.f("duration", 90) * 20);
        v.entityData.set(DONO, dono.getId());
        v.entityData.set(RESTANTE, dur);
        v.entityData.set(MUNICAO, prm.i("rounds", 4));
        v.entityData.set(FAROL, true);
        float yaw = dono.getYRot();
        if (v.modelo == BATBARCO) {
            Vec3 agua = acharAgua(level, dono.blockPosition(), 14);
            if (agua == null) return null;
            // Começa no fundo (até 4 blocos abaixo da superfície) e sobe.
            double fundo = agua.y - 1.5;
            for (int i = 1; i <= 4 && level.getFluidState(BlockPos.containing(agua.x, agua.y - i - 0.5, agua.z)).is(FluidTags.WATER); i++) fundo = agua.y - i - 0.8;
            // De frente para onde o dono olha (para a água), e não para a margem.
            v.subida = agua;
            v.snapTo(agua.x, fundo, agua.z, yaw, 0);
            v.noPhysics = true;
            level.addFreshEntity(v);
            level.playSound(null, agua.x, agua.y, agua.z, Sons.BATBARCO_EMERGE.get(), SoundSource.PLAYERS, 1.4f, 1f);
            return v;
        }
        Vec3 frente = Vec3.directionFromRotation(0, yaw);
        Vec3 inicio = chao(level, dono.position().subtract(frente.scale(24)), dono.getY());
        boolean caminho = inicio != null && livre(level, inicio.add(0, 0.6, 0), dono.position().add(0, 0.6, 0), v);
        if (caminho) {
            v.snapTo(inicio.x, inicio.y, inicio.z, yaw, 0);
            v.velocidade = 0.6f;
            level.addFreshEntity(v);
            level.playSound(null, dono.getX(), dono.getY(), dono.getZ(), v.modelo == BATMOTO ? Sons.BATMOTO_CHEGA.get() : Sons.BATMOVEL_CHEGA.get(),
                SoundSource.PLAYERS, 1.6f, 1f);
        } else {
            // Sem estrada: aparece ao lado do dono numa nuvem de fumaça, já pronto.
            Vec3 lado = dono.position().add(Vec3.directionFromRotation(0, yaw + 90).scale(2.5));
            Vec3 p = level.noCollision(v, v.getDimensions(v.getPose()).makeBoundingBox(lado)) ? lado : dono.position();
            v.snapTo(p.x, p.y, p.z, yaw, 0);
            level.addFreshEntity(v);
            v.fumacaDeChegada(level);
            v.entityData.set(ESTADO, PARADO);
            v.embarcar(dono);
        }
        return v;
    }

    /** Superfície mais perto em volta de onde (raio em blocos): o ponto no topo da água, ou null. */
    private static Vec3 acharAgua(ServerLevel level, BlockPos onde, int raio) {
        // Prefere um lugar com 5x5 de água em volta (o casco não encosta na margem); num rio estreito, 3x3 basta.
        Vec3 largo = acharAgua(level, onde, raio, 2);
        return largo != null ? largo : acharAgua(level, onde, raio, 1);
    }

    private static Vec3 acharAgua(ServerLevel level, BlockPos onde, int raio, int folga) {
        Vec3 melhor = null;
        double melhorD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(onde.offset(-raio, -4, -raio), onde.offset(raio, 2, raio))) {
            if (!level.getFluidState(p).is(FluidTags.WATER) || !level.getBlockState(p.above()).isAir()) continue;
            // Pelo menos 2 de fundo.
            if (!level.getFluidState(p.below()).is(FluidTags.WATER)) continue;
            double d = p.distSqr(onde);
            if (d >= melhorD) continue;
            boolean largo = true;
            for (int dx = -folga; dx <= folga && largo; dx++) {
                for (int dz = -folga; dz <= folga && largo; dz++) largo = level.getFluidState(p.offset(dx, 0, dz)).is(FluidTags.WATER);
            }
            if (!largo) continue;
            melhorD = d;
            melhor = new Vec3(p.getX() + 0.5, p.getY() + 0.9, p.getZ() + 0.5);
        }
        return melhor;
    }

    /** O chão em (x, z) perto da altura y (no máximo 4 blocos acima ou abaixo), ou null. */
    private static Vec3 chao(ServerLevel level, Vec3 p, double y) {
        int topo = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(p.x), Mth.floor(p.z));
        if (Math.abs(topo - y) > 4) return null;
        return new Vec3(p.x, topo, p.z);
    }

    private static boolean livre(ServerLevel level, Vec3 de, Vec3 ate, Entity e) {
        return level.clip(new ClipContext(de, ate, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, e)).getType() == HitResult.Type.MISS;
    }

    private void fumacaDeChegada(ServerLevel level) {
        Rede.efeito(level, getX(), getY() + 0.5, getZ(), "smoke", 2.5f, 3f, 0x6E737C);
        level.playSound(null, getX(), getY(), getZ(), Sons.FUMACA.get(), SoundSource.PLAYERS, 1f, 1.1f);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(DONO, -1);
        b.define(ESTADO, CHEGANDO);
        b.define(RESTANTE, 0);
        b.define(CALOR, 0f);
        b.define(MUNICAO, 0);
        b.define(RECARGA, 0);
        b.define(ALVO, -1);
        b.define(ESPECIAL_CD, 0);
        b.define(SUBMERSO, false);
        b.define(FAROL, false);
    }

    // ------------------------------------------------------------------ consultas

    public Modelo forma() {
        return modelo;
    }

    public String modelo() {
        return modelo.id();
    }

    @Override
    public int estado() {
        return entityData.get(ESTADO);
    }

    @Override
    public int donoId() {
        return entityData.get(DONO);
    }

    public Entity dono() {
        return level().getEntity(donoId());
    }

    public int restante() {
        return entityData.get(RESTANTE);
    }

    public float calor() {
        return entityData.get(CALOR);
    }

    /** Mísseis, batarangues ou torpedos prontos. */
    public int municao() {
        return entityData.get(MUNICAO);
    }

    /** Fração da recarga da próxima munição (1 = pronta). */
    public float recarga() {
        int total = Math.round(prm.f("rounds_reload", 4) * 20);
        return 1f - entityData.get(RECARGA) / (float) Math.max(1, total);
    }

    public int alvoTravado() {
        return entityData.get(ALVO);
    }

    /** Ticks até o especial (V) poder ser usado de novo. */
    public int especialEspera() {
        return entityData.get(ESPECIAL_CD);
    }

    public boolean submerso() {
        return entityData.get(SUBMERSO);
    }

    public boolean farol() {
        return entityData.get(FAROL);
    }

    public Params params() {
        return prm;
    }

    /** O veículo que o jogador pilota agora, ou null. */
    public static VeiculoBat pilotando(Player player) {
        return player.getVehicle() instanceof VeiculoBat v && v.donoId() == player.getId() ? v : null;
    }

    @Override
    public float maxUpStep() {
        return modelo.degrau();
    }

    @Override
    public boolean causeFallDamage(double distancia, float mult, DamageSource fonte) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    // ------------------------------------------------------------------ passageiros

    @Override
    public LivingEntity getControllingPassenger() {
        Entity d = dono();
        return d instanceof LivingEntity vivo && hasPassenger(d) && estado() == PILOTADO ? vivo : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passageiro) {
        return getPassengers().size() < modelo.assentos().length && estado() != INDO;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passageiro, EntityDimensions dim, float escala) {
        int i = passageiro.getId() == donoId() ? 0 : 1;
        Vec3 a = modelo.assentos()[Math.min(i, modelo.assentos().length - 1)];
        // O assento acompanha o veículo inteiro (rumo, nariz e inclinação), girando em volta do chão.
        Vector3f v = rotacao(1).transform(new Vector3f((float) a.x, (float) a.y, (float) a.z));
        return new Vec3(v.x, v.y, v.z);
    }

    /** Rotação com que o veículo é desenhado: rumo, depois nariz (subidas) e inclinação (curvas). */
    public Quaternionf rotacao(float pt) {
        float yaw = Mth.rotLerp(pt, yRotO, getYRot()), pitch = Mth.lerp(pt, arfagemO, arfagem), roll = Mth.lerp(pt, rolagemO, rolagem);
        return new Quaternionf().rotationY(-yaw * Mth.DEG_TO_RAD).rotateX(pitch * Mth.DEG_TO_RAD).rotateZ(roll * Mth.DEG_TO_RAD);
    }

    /** Só a inclinação vista de fora, para girar o corpo de quem está montado. */
    public Quaternionf inclinacao(float pt) {
        float yaw = Mth.rotLerp(pt, yRotO, getYRot());
        return rotacao(pt).rotateY(yaw * Mth.DEG_TO_RAD);
    }

    @Override
    protected void positionRider(Entity passageiro, MoveFunction mover) {
        super.positionRider(passageiro, mover);
        if (passageiro instanceof LivingEntity vivo) {
            vivo.setYBodyRot(getYRot());
            vivo.yBodyRotO = yRotO;
            // Na moto e no carro a cabeça olha livre, mas não para trás do corpo.
            float d = Mth.wrapDegrees(vivo.getYRot() - getYRot());
            if (Math.abs(d) > 120) vivo.setYRot(getYRot() + Math.signum(d) * 120);
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passageiro) {
        boolean piloto = passageiro.getId() == donoId();
        // A caixa de colisão é quadrada e não gira: de lado, na diagonal, é preciso sair mais longe para não cair nela.
        float a = getYRot() * Mth.DEG_TO_RAD;
        double eixo = Math.max(Math.abs(Mth.sin(a)), Math.abs(Mth.cos(a)));
        double dist = Math.max(modelo.ladoSaida(), (getBbWidth() / 2 + passageiro.getBbWidth() / 2 + 0.05) / eixo);
        for (int lado : new int[] {piloto ? 1 : -1, piloto ? -1 : 1}) {
            Vec3 p = position().add(new Vec3(lado * dist, 0.1, 0).yRot(-a));
            if (level().noCollision(passageiro, passageiro.getDimensions(passageiro.getPose()).makeBoundingBox(p))) return p;
        }
        Vec3 cima = position().add(0, getBbHeight() + 0.1, 0);
        if (level().noCollision(passageiro, passageiro.getDimensions(passageiro.getPose()).makeBoundingBox(cima))) return cima;
        return super.getDismountLocationForPassenger(passageiro);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 onde) {
        if (player.isSecondaryUseActive() || hasPassenger(player)) return InteractionResult.PASS;
        if (!level().isClientSide()) {
            if (player.getId() == donoId()) embarcar((ServerPlayer) player);
            else if (getPassengers().size() < (hasPassenger(dono()) ? modelo.assentos().length : modelo.assentos().length - 1)) player.startRiding(this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void embarcar(ServerPlayer dono) {
        if (dono.getId() != donoId() || estado() == INDO || hasPassenger(dono)) return;
        // O jogador falso dos GameTests não tem conexão e não consegue descer depois.
        if (dono.connection == null) return;
        dono.startRiding(this, true, true);
        entityData.set(ESTADO, PILOTADO);
        tempoEstado = 0;
        dono.resetFallDistance();
        var cam = dono.getAttribute(Attributes.CAMERA_DISTANCE);
        if (cam != null) cam.addOrUpdateTransientModifier(new AttributeModifier(CAMERA, modelo.camera(), AttributeModifier.Operation.ADD_VALUE));
        Guia.concluir(dono, "garagem");
    }

    @Override
    public void chamar(ServerPlayer dono) {
        if (dono.distanceTo(this) < 8 || modelo == BATBARCO) {
            if (dono.distanceTo(this) < 8) embarcar(dono);
            return;
        }
        // Longe: o veículo vai até o dono.
        if (estado() == PARADO && !hasPassenger(dono)) {
            entityData.set(ESTADO, CHEGANDO);
            tempoEstado = 0;
            preso = 0;
            level().playSound(null, getX(), getY(), getZ(), modelo == BATMOTO ? Sons.BATMOTO_CHEGA.get() : Sons.BATMOVEL_CHEGA.get(),
                SoundSource.PLAYERS, 1.4f, 1.05f);
        }
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
        return d < 192 * 192;
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
        arfagemO = arfagem;
        estercoO = esterco;
        giroO = giro;
        if (level().isClientSide()) {
            if (isLocalInstanceAuthoritative()) {
                interpolacao.cancel();
                controleCliente.accept(this);
            } else {
                interpolacao.interpolate();
                estimarPose();
            }
            Vec3 d = position().subtract(xo, yo, zo);
            double frente = d.dot(Vec3.directionFromRotation(0, getYRot()));
            // Rodas: giro pela distância andada (raio ~0,45 bloco).
            giro += (float) (frente / 0.45 * Mth.RAD_TO_DEG);
            return;
        }
        tickServidor((ServerLevel) level());
    }

    /** Quem vê de fora estima a inclinação e o esterço pela curva e pela subida. */
    private void estimarPose() {
        float girou = Mth.wrapDegrees(getYRot() - yRotO);
        Vec3 d = position().subtract(xo, yo, zo);
        double h = d.horizontalDistance();
        float alvoRol = modelo == BATMOTO ? Mth.clamp(-girou * (float) h * 14, -40, 40) : Mth.clamp(girou * 0.8f, -4, 4);
        rolagem += (alvoRol - rolagem) * 0.25f;
        esterco += (Mth.clamp(girou / 4f, -1, 1) - esterco) * 0.3f;
        float alvoArf = h > 0.05 ? Mth.clamp((float) (-Mth.atan2(d.y, h) * Mth.RAD_TO_DEG), -30, 30) : 0;
        if (modelo == BATBARCO && submerso()) alvoArf = getXRot();
        arfagem += (alvoArf - arfagem) * 0.25f;
    }

    private void tickServidor(ServerLevel level) {
        tempoEstado++;
        Entity dono = dono();
        int restante = Math.max(0, restante() - 1);
        entityData.set(RESTANTE, restante);
        boolean aBordo = dono != null && hasPassenger(dono);
        if (donoABordo && !aBordo && estado() == PILOTADO) {
            // O dono desceu: o veículo fica parado onde está, à espera.
            entityData.set(ESTADO, PARADO);
            tempoEstado = 0;
            atirando = false;
            if (dono instanceof ServerPlayer sp) {
                tirarCamera(sp);
                sp.resetFallDistance();
            }
            if (submerso()) entityData.set(SUBMERSO, false);
        }
        donoABordo = aBordo;
        if (estado() != INDO && (dono == null || !dono.isAlive() || restante <= 0
            || !(dono instanceof ServerPlayer sp && EstadoJogador.de(sp).transformado()))) {
            irEmbora();
        }
        for (Entity p : getPassengers()) {
            // Na cabine fechada (e no barco mergulhado) ninguém fica sem ar.
            if (p instanceof LivingEntity vivo) vivo.setAirSupply(vivo.getMaxAirSupply());
        }
        switch (estado()) {
            case CHEGANDO -> chegar(level, dono);
            case PARADO -> parado();
            case INDO -> indo(level);
            case PILOTADO -> atropelar(level, dono);
            default -> {}
        }
        int cd = especialEspera();
        if (cd > 0) entityData.set(ESPECIAL_CD, cd - 1);
        armas(level, dono);
    }

    private void chegar(ServerLevel level, Entity dono) {
        if (modelo == BATBARCO) {
            // Sobe do fundo até a superfície, soltando bolhas.
            float t = Math.min(1f, tempoEstado / 30f);
            double y = Mth.lerp(1 - (1 - t) * (1 - t), getY(), subida.y - 0.35);
            setPos(subida.x, y, subida.z);
            level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, getX(), getY() + 0.5, getZ(), 6, 1.2, 0.3, 1.2, 0.05);
            if (t >= 1) {
                noPhysics = false;
                level.sendParticles(ParticleTypes.SPLASH, getX(), subida.y, getZ(), 40, 1.5, 0.1, 1.5, 0.2);
                entityData.set(ESTADO, PARADO);
                tempoEstado = 0;
                if (dono instanceof ServerPlayer sp && sp.distanceTo(this) < 6) embarcar(sp);
            }
            return;
        }
        if (dono == null) return;
        // Dirige sozinho até o lado do dono, freando no fim.
        Vec3 alvo = dono.position().add(Vec3.directionFromRotation(0, dono.getYRot() + 90).scale(2.4));
        Vec3 para = alvo.subtract(position());
        double dist = para.horizontalDistance();
        float yawAlvo = (float) (Mth.atan2(para.z, para.x) * Mth.RAD_TO_DEG) - 90;
        float yaw = getYRot() + Mth.clamp(Mth.wrapDegrees(yawAlvo - getYRot()), -9, 9);
        setYRot(yaw);
        float vel = (float) Mth.clamp(dist * 0.12, 0.15, 0.95);
        if (dist < 1.2) vel = 0;
        Vec3 mov = Vec3.directionFromRotation(0, yaw).scale(vel);
        Vec3 antes = position();
        double vy = onGround() ? -0.08 : getDeltaMovement().y - 0.08;
        setDeltaMovement(mov.x, vy, mov.z);
        move(MoverType.SELF, getDeltaMovement());
        if (horizontalCollision) atropelar(level, dono);
        if (position().distanceToSqr(antes) < 0.002 && dist > 1.5) preso++;
        else preso = 0;
        if (preso > 15 || tempoEstado > 200) {
            // Travou no caminho: some na fumaça e reaparece ao lado.
            fumacaDeChegada(level);
            Vec3 p = level.noCollision(this, getDimensions(getPose()).makeBoundingBox(alvo)) ? alvo : dono.position();
            setPos(p);
            setYRot(dono.getYRot());
            fumacaDeChegada(level);
            dist = 0;
        }
        if (dist < 1.6 || vel == 0) {
            entityData.set(ESTADO, PARADO);
            tempoEstado = 0;
            setDeltaMovement(Vec3.ZERO);
            level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.1, getZ(), 12, 1.2, 0.1, 1.2, 0.02);
            if (dono instanceof ServerPlayer sp && sp.distanceTo(this) < 6) embarcar(sp);
        }
    }

    private void parado() {
        Vec3 v = getDeltaMovement();
        if (modelo == BATBARCO && isInWater()) {
            // Boia: sobe até a linha d'água e balança.
            double fh = getFluidHeight(FluidTags.WATER);
            double vy = (v.y + (fh - 0.45) * 0.08) * 0.85;
            setDeltaMovement(v.x * 0.9, vy + Math.sin(tickCount * 0.1) * 0.002, v.z * 0.9);
        } else {
            setDeltaMovement(v.x * 0.8, onGround() ? -0.08 : (v.y - 0.08) * 0.98, v.z * 0.8);
        }
        move(MoverType.SELF, getDeltaMovement());
    }

    private void indo(ServerLevel level) {
        if (modelo == BATBARCO) {
            noPhysics = true;
            setPos(position().add(0, -0.08, 0));
            if (tempoEstado % 3 == 0) level.sendParticles(ParticleTypes.BUBBLE, getX(), getY() + 0.5, getZ(), 8, 1, 0.3, 1, 0.05);
            if (tempoEstado > 40) discard();
            return;
        }
        // Acelera para a frente e some numa nuvem de fumaça.
        float vel = Math.min(1.2f, 0.2f + tempoEstado * 0.04f);
        Vec3 mov = Vec3.directionFromRotation(0, getYRot()).scale(vel);
        setDeltaMovement(mov.x, onGround() ? -0.08 : getDeltaMovement().y - 0.08, mov.z);
        move(MoverType.SELF, getDeltaMovement());
        if (tempoEstado > 30 || horizontalCollision && tempoEstado > 4) {
            fumacaDeChegada(level);
            discard();
        }
    }

    @Override
    public void irEmbora() {
        if (estado() == INDO) return;
        for (Entity p : getPassengers()) {
            p.stopRiding();
            tirarCamera(p);
            p.resetFallDistance();
        }
        Entity dono = dono();
        if (dono instanceof ServerPlayer sp) tirarCamera(sp);
        entityData.set(ESTADO, INDO);
        entityData.set(SUBMERSO, false);
        tempoEstado = 0;
        atirando = false;
        level().playSound(null, getX(), getY(), getZ(), modelo == BATBARCO ? Sons.BATBARCO_MERGULHA.get()
            : modelo == BATMOTO ? Sons.BATMOTO_CHEGA.get() : Sons.BATMOVEL_CHEGA.get(), SoundSource.PLAYERS, 1.2f, 1.15f);
    }

    /**
     * Atropelamento: rápido, quem está na frente leva dano pela velocidade e é jogado para o lado e para cima (cada
     * um no máximo uma vez a cada meio segundo). Parede não machuca ninguém.
     */
    private void atropelar(ServerLevel level, Entity dono) {
        if (!(dono instanceof ServerPlayer piloto)) return;
        Vec3 d = position().subtract(xo, yo, zo);
        double v = d.horizontalDistance();
        double minimo = prm.d("ram_min_speed", 8) / 20;
        if (v < minimo) return;
        Vec3 frente = Vec3.directionFromRotation(0, getYRot());
        AABB caixa = getBoundingBox().inflate(0.3, 0, 0.3).expandTowards(frente.scale(Math.min(1.5, v * 1.5)));
        long agora = level.getGameTime();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, caixa, e -> e != piloto && !hasPassenger(e) && e.isAlive())) {
            if (!Alvos.podeAtingir(piloto, e)) continue;
            Long ultimo = atropelados.get(e.getId());
            if (ultimo != null && agora - ultimo < 10) continue;
            atropelados.put(e.getId(), agora);
            float maxima = prm.f("boost_speed", 40) / 20f;
            float dano = (float) (prm.d("ram_damage", 6) * 2 * Mth.clamp(v / maxima, 0.3, 1.2));
            Alvos.ferir(piloto, e, damageSources().mobAttack(piloto), dano);
            Vec3 fora = e.position().subtract(position()).multiply(1, 0, 1);
            fora = fora.lengthSqr() < 1e-4 ? frente : fora.normalize();
            Vec3 empurra = frente.scale(v * 1.4).add(fora.scale(0.6)).add(0, 0.45 + v * 0.3, 0);
            e.push(empurra.x, empurra.y, empurra.z);
            e.hurtMarked = true;
            level.playSound(null, e.getX(), e.getY(), e.getZ(), Sons.VEICULO_ATROPELA.get(), SoundSource.PLAYERS, 1.1f, 0.9f + random.nextFloat() * 0.2f);
            level.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), 10, 0.3, 0.3, 0.3, 0.3);
        }
        if (atropelados.size() > 32) atropelados.values().removeIf(t -> agora - t > 40);
    }

    // ------------------------------------------------------------------ armas

    @Override
    public void comando(ServerPlayer piloto, int acao) {
        if (pilotando(piloto) != this || estado() != PILOTADO) return;
        switch (acao) {
            case TIRO_LIGA -> atirando = true;
            case TIRO_DESLIGA -> atirando = false;
            case SECUNDARIA -> secundaria(piloto);
            case ESPECIAL -> especial(piloto);
            default -> {}
        }
    }

    private void armas(ServerLevel level, Entity dono) {
        float calor = calor();
        boolean pode = estado() == PILOTADO && dono instanceof ServerPlayer;
        if (pode && atirando && !superaquecido) {
            if (tickCount % 2 == 0) tiro(level, (ServerPlayer) dono);
            calor += prm.f("gun_heat", 0.022f);
            if (calor >= 1) {
                superaquecido = true;
                level.playSound(null, getX(), getY(), getZ(), Sons.BATWING_SUPERAQUECE.get(), SoundSource.PLAYERS, 0.9f, 1f);
            }
        } else {
            calor -= superaquecido ? 0.012f : 0.02f;
            if (calor <= 0.25f) superaquecido = false;
        }
        entityData.set(CALOR, Mth.clamp(calor, 0, 1));
        int max = prm.i("rounds", 4);
        int rec = entityData.get(RECARGA);
        if (municao() < max) {
            if (rec <= 0) rec = Math.round(prm.f("rounds_reload", 4) * 20);
            else if (--rec <= 0) entityData.set(MUNICAO, municao() + 1);
            entityData.set(RECARGA, Math.max(0, rec));
        }
        if (modelo != BATMOTO && estado() == PILOTADO && dono instanceof ServerPlayer sp && tickCount % 4 == 0) {
            Entity alvo = procurarAlvo(sp);
            entityData.set(ALVO, alvo == null ? -1 : alvo.getId());
        }
    }

    /** Ponto de saída de uma arma (lado -1 direita, 1 esquerda), no mundo, já girado com o veículo. */
    public Vec3 bocal(int lado) {
        Vector3f p = switch (modelo.id()) {
            case "batmoto" -> new Vector3f(lado * 0.24f, 0.62f, 1.25f);
            case "batbarco" -> new Vector3f(lado * 0.55f, 0.95f, 2.1f);
            default -> new Vector3f(lado * 0.62f, 0.55f, 2.45f);
        };
        Vector3f v = rotacao(1).transform(p);
        return position().add(v.x, v.y, v.z);
    }

    private void tiro(ServerLevel level, ServerPlayer piloto) {
        tiros++;
        int lado = tiros % 2 == 0 ? 1 : -1;
        Vec3 de = bocal(lado);
        double alcance = prm.d("gun_range", 48);
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
        if (level.getFluidState(BlockPos.containing(de)).is(FluidTags.WATER)) {
            // Debaixo d'água o tiro deixa um rastro de bolhas.
            Vec3 passo = acerto.subtract(de);
            int n = Math.min(24, (int) (passo.length() / 1.2));
            for (int i = 1; i <= n; i++) {
                Vec3 q = de.add(passo.scale(i / (double) n));
                level.sendParticles(ParticleTypes.BUBBLE, q.x, q.y, q.z, 2, 0.05, 0.05, 0.05, 0.02);
            }
        }
        Rede.efeito(level, de.x, de.y, de.z, "tiro", (float) (acerto.x - de.x), (float) (acerto.z - de.z), Float.floatToIntBits((float) (acerto.y - de.y)));
        if (tiros % 2 == 0) {
            level.playSound(null, de.x, de.y, de.z, Sons.BATWING_TIRO.get(), SoundSource.PLAYERS, modelo == BATMOTO ? 0.9f : 1.2f,
                (modelo == BATMOTO ? 1.2f : 0.9f) + random.nextFloat() * 0.1f);
        }
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

    /** O mob hostil (ou jogador atingível) mais perto do centro da mira, para os mísseis e torpedos. */
    public Entity procurarAlvo(ServerPlayer piloto) {
        Vec3 olhos = piloto.getEyePosition();
        Vec3 dir = piloto.getLookAngle();
        double cos = Math.cos(Math.toRadians(prm.d("lock_angle", 18)));
        double alcance = prm.d("lock_range", 64);
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

    /** Botão de usar: mísseis do Batmóvel, batarangues da Batmoto, torpedos do Batbarco. */
    private void secundaria(ServerPlayer piloto) {
        int n = municao();
        if (n <= 0) return;
        entityData.set(MUNICAO, n - 1);
        ServerLevel level = (ServerLevel) level();
        Vec3 vel = position().subtract(xo, yo, zo);
        switch (modelo.id()) {
            case "batmoto" -> {
                BatarangueEntidade b = new BatarangueEntidade(piloto, BatarangueEntidade.NORMAL, (float) prm.d("rounds_damage", 4) * 2,
                    prm.f("rounds_stun", 1), 1, prm.f("rounds_range", 28), 1.6f + (float) vel.length(), 3, false);
                level.addFreshEntity(b);
                level.playSound(null, getX(), getY(), getZ(), Sons.BATARANGUE_ARREMESSO.get(), SoundSource.PLAYERS, 1f, 1.1f);
            }
            case "batbarco" -> {
                // Torpedo: sai por baixo da proa e persegue o alvo travado.
                Vector3f p = rotacao(1).transform(new Vector3f(n % 2 == 0 ? 0.5f : -0.5f, 0.1f, 1.6f));
                Vec3 de = position().add(p.x, p.y, p.z);
                Vec3 ponto = mira(piloto, prm.d("lock_range", 64));
                Vec3 dir = ponto.subtract(de).normalize();
                level.addFreshEntity(new MissilEntidade(level, piloto, de, dir, vel.length(), procurarAlvo(piloto),
                    (float) prm.d("rounds_damage", 7) * 2, prm.f("rounds_radius", 3)).mirando(ponto));
                level.playSound(null, de.x, de.y, de.z, Sons.BATBARCO_TORPEDO.get(), SoundSource.PLAYERS, 1.4f, 1f);
            }
            default -> {
                // Mísseis do teto, alternando os lados.
                Vector3f p = rotacao(1).transform(new Vector3f(n % 2 == 0 ? 0.55f : -0.55f, 1.25f, -0.9f));
                Vec3 de = position().add(p.x, p.y, p.z);
                // Sobe um pouco ao sair do lançador e depois vai até onde a mira aponta (ou atrás do alvo travado).
                Vec3 ponto = mira(piloto, prm.d("lock_range", 64));
                Vec3 dir = ponto.subtract(de).normalize().add(0, 0.25, 0).normalize();
                level.addFreshEntity(new MissilEntidade(level, piloto, de, dir, Math.max(0.6, vel.length()), procurarAlvo(piloto),
                    (float) prm.d("rounds_damage", 6) * 2, prm.f("rounds_radius", 3)).mirando(ponto));
                level.playSound(null, de.x, de.y, de.z, Sons.BATWING_MISSIL.get(), SoundSource.PLAYERS, 1.5f, 1.05f);
            }
        }
    }

    /** Tecla de habilidade: casco elétrico, cortina de fumaça ou mergulho. */
    private void especial(ServerPlayer piloto) {
        ServerLevel level = (ServerLevel) level();
        if (modelo == BATBARCO) {
            boolean novo = !submerso();
            if (novo && !isInWater()) return;
            entityData.set(SUBMERSO, novo);
            level.playSound(null, getX(), getY(), getZ(), novo ? Sons.BATBARCO_MERGULHA.get() : Sons.BATBARCO_EMERGE.get(), SoundSource.PLAYERS, 1.3f, 1f);
            level.sendParticles(novo ? ParticleTypes.BUBBLE : ParticleTypes.SPLASH, getX(), getY() + 0.6, getZ(), 40, 1.4, 0.3, 1.4, 0.15);
            return;
        }
        if (especialEspera() > 0) return;
        entityData.set(ESPECIAL_CD, Math.round(prm.f("special_cooldown", 12) * 20));
        if (modelo == BATMOTO) {
            Vec3 tras = position().add(Vec3.directionFromRotation(0, getYRot()).scale(-2.5));
            Fumaca.nuvem(piloto, tras, prm.f("special_radius", 5), prm.f("special_duration", 6), prm.f("blind", 4));
            return;
        }
        // Casco elétrico: descarga em todos em volta, com arcos saindo da lataria.
        double raio = prm.d("special_radius", 5);
        Vec3 c = position().add(0, 0.7, 0);
        int stun = Math.round(prm.f("stun", 3) * 20);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(raio),
            e -> e != piloto && !hasPassenger(e) && e.isAlive() && e.distanceToSqr(c) < raio * raio)) {
            if (!Alvos.podeAtingir(piloto, e)) continue;
            Alvos.ferir(piloto, e, damageSources().indirectMagic(this, piloto), (float) prm.d("special_damage", 4) * 2);
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, stun, 4), piloto);
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, stun, 1), piloto);
            if (e instanceof Mob m) m.getNavigation().stop();
            Vec3 fora = e.position().subtract(position()).multiply(1, 0, 1);
            fora = fora.lengthSqr() < 1e-4 ? Vec3.ZERO : fora.normalize();
            e.push(fora.x * 0.9, 0.35, fora.z * 0.9);
            e.hurtMarked = true;
            Vec3 alvo = e.getBoundingBox().getCenter();
            Vec3 de = c.add(fora.scale(1.1));
            Rede.efeito(level, de.x, de.y, de.z, "arco", (float) (alvo.x - de.x), (float) (alvo.z - de.z), Float.floatToIntBits((float) (alvo.y - de.y)));
        }
        for (int i = 0; i < 6; i++) {
            // Arcos curtos dançando em volta da lataria, mesmo sem ninguém por perto.
            double a = Math.PI * 2 * i / 6 + random.nextDouble() * 0.5;
            Vec3 de = c.add(Math.cos(a) * 1.2, random.nextDouble() * 0.4, Math.sin(a) * 1.2);
            Vec3 ate = c.add(Math.cos(a) * 3.2, -0.6 + random.nextDouble(), Math.sin(a) * 3.2);
            Rede.efeito(level, de.x, de.y, de.z, "arco", (float) (ate.x - de.x), (float) (ate.z - de.z), Float.floatToIntBits((float) (ate.y - de.y)));
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z, 60, 1.6, 0.5, 2.2, 0.3);
        level.playSound(null, c.x, c.y, c.z, Sons.CHOQUE.get(), SoundSource.PLAYERS, 1.6f, 0.8f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.7f, 1.6f);
    }

    @Override
    public void remove(RemovalReason motivo) {
        for (Entity p : getPassengers()) tirarCamera(p);
        super.remove(motivo);
    }
}
