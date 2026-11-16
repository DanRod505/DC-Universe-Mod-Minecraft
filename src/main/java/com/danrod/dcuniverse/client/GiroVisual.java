package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.ability.active.GiroCalor;
import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4fc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Giro de Calor no cliente (o servidor é ability/active/GiroCalor; a coreografia é a animação superman.giro).
 * <ul>
 * <li>Movimento do próprio jogador: agacha, sobe {@code altura} blocos, fica parado no ar girando e cai batendo no chão.</li>
 * <li>Corpo: gira com o raio e a cabeça acompanha a inclinação dele.</li>
 * <li>Raio: os dois feixes saindo dos olhos até onde batem, mais grossos com as melhorias de dano.</li>
 * <li>Anéis em espiral: o rastro do raio girando em alta velocidade, desenhado em faixas ao longo dele que somem em
 * poucos décimos de segundo; como a inclinação sobe e desce, os anéis ficam em alturas diferentes.</li>
 * <li>Chão: o risco em brasa onde o raio passou e a cratera incandescente que esquenta durante o giro e esfria depois.</li>
 * <li>Detritos: pedaços do chão (cubos com a textura do bloco) arremessados onde o raio bate e na batida final, com
 * poeira e fumaça.</li>
 * <li>Quem foi atingido (brilho do servidor) ganha contorno vermelho enquanto está perto do giro.</li>
 * </ul>
 */
public final class GiroVisual {
    private GiroVisual() {}

    /** Quanto tempo (ticks) o rastro de cada anel dura. */
    private static final float RASTRO = 8f;
    /** Quanto tempo (ticks) o risco no chão dura. */
    private static final float RISCO = 30f;
    /** Quanto tempo (ticks) a cratera continua acesa depois da batida. */
    private static final float ESFRIA = 70f;
    private static final int COR_CONTORNO = 0xFF3A1A;

    private static final List<Pedaco> PEDACOS = new ArrayList<>();
    private static final Map<Identifier, Boolean> TEXTURA_EXISTE = new HashMap<>();
    private static final Identifier TERRA = Identifier.withDefaultNamespace("textures/block/dirt.png");

    /** Pedaço do chão voando: posição e velocidade (blocos/tick), giro, tamanho, textura e luz. */
    private static final class Pedaco {
        Vec3 pos, antes, vel;
        final Vec3 eixo;
        float ang, angAntes;
        final float giro, tam;
        final Identifier textura;
        int idade, quiques;
        final int vida;

        Pedaco(Vec3 pos, Vec3 vel, Vec3 eixo, float giro, float tam, Identifier textura, int vida) {
            this.pos = pos;
            this.antes = pos;
            this.vel = vel;
            this.eixo = eixo;
            this.giro = giro;
            this.tam = tam;
            this.textura = textura;
            this.vida = vida;
        }
    }

    public static void registrar() {
        NeoForge.EVENT_BUS.addListener(RenderLivingEvent.Pre.class, e -> {
            long agora = System.currentTimeMillis();
            LivingEntityRenderState s = e.getRenderState();
            if (s instanceof AvatarRenderState st) {
                EstadoCliente.GiroAtivo g = EstadoCliente.GIROS.get(st.id);
                if (g != null) {
                    float t = g.tempo(agora);
                    if (t < g.giro().ticks()) {
                        float u = Mth.clamp(t / g.giro().ticks(), 0, 1);
                        st.bodyRot = t < 0 ? g.giro().yaw0() : GiroCalor.yaw(g.giro().yaw0(), g.giro().voltas(), u);
                        st.yRot = 0;
                        // A cabeça olha para onde o raio bate (no preparo, olha para o chão à frente).
                        st.xRot = t < 0 ? 20 : GiroCalor.pitch(g.giro().pitchMin(), g.giro().pitchMax(), u);
                    }
                }
            }
            // Contorno vermelho em quem foi atingido (o servidor acende o brilho) perto de um giro aceso.
            if (s.outlineColor != 0 && perto(s.x, s.z, agora)) s.outlineColor = ARGB.opaque(COR_CONTORNO);
        });
    }

    private static boolean perto(double x, double z, long agora) {
        for (EstadoCliente.GiroAtivo g : EstadoCliente.GIROS.values()) {
            if (g.tempo(agora) > g.giro().ticks() + 60) continue;
            Entity dono = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getEntity(g.giro().entidade());
            if (dono == null) continue;
            double r = g.giro().alcance() * 1.6 + 6;
            double dx = x - dono.getX(), dz = z - dono.getZ();
            if (dx * dx + dz * dz < r * r) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ movimento do próprio jogador

    /** Tick do jogador local (client/Voador): true enquanto o giro manda no movimento dele. */
    static boolean mover(LocalPlayer p) {
        EstadoCliente.GiroAtivo g = EstadoCliente.GIROS.get(p.getId());
        if (g == null || Double.isNaN(g.baseY())) return false;
        Rede.Giro m = g.giro();
        float t = g.tempo(System.currentTimeMillis());
        int fim = m.ticks() + GiroCalor.QUEDA;
        if (t > fim + 14) return false;
        double alvo = g.baseY() + m.altura();
        Vec3 v;
        if (t < -m.preparo() + 4) {
            v = new Vec3(0, Math.min(0, p.getDeltaMovement().y), 0);           // agacha
        } else if (t < m.ticks()) {
            double falta = alvo - p.getY();
            v = new Vec3(0, Mth.clamp(falta * (t < 0 ? 0.35 : 0.3), -0.4, 0.7), 0); // sobe e fica parado no alto
        } else if (t < fim + 2 && !p.onGround()) {
            v = new Vec3(0, -1.8, 0);                                           // cai batendo
        } else {
            v = new Vec3(0, Math.min(0, p.getDeltaMovement().y), 0);           // ajoelhado no chão
        }
        p.setDeltaMovement(v);
        p.resetFallDistance();
        return true;
    }

    // ------------------------------------------------------------------ tick: partículas, detritos e limpeza

    static void tick(ClientLevel level) {
        long agora = System.currentTimeMillis();
        EstadoCliente.GIROS.entrySet().removeIf(en -> en.getValue().tempo(agora) > en.getValue().giro().ticks() + ESFRIA + 10);
        RandomSource rnd = level.getRandom();
        for (Map.Entry<Integer, EstadoCliente.GiroAtivo> en : EstadoCliente.GIROS.entrySet()) {
            EstadoCliente.GiroAtivo g = en.getValue();
            Entity dono = level.getEntity(en.getKey());
            if (dono == null) continue;
            if (Double.isNaN(g.chaoY())) {
                g = new EstadoCliente.GiroAtivo(g.giro(), g.inicio(), chao(level, dono.position()), dono.getY());
                en.setValue(g);
            }
            float t = g.tempo(agora);
            Rede.Giro m = g.giro();
            Vec3 centro = new Vec3(dono.getX(), g.chaoY(), dono.getZ());
            if (t < 0) {
                preparo(level, dono, g, t, rnd);
            } else if (t < m.ticks()) {
                float u = t / m.ticks();
                Vec3 olhos = dono.getEyePosition();
                Vec3 dir = Vec3.directionFromRotation(GiroCalor.pitch(m.pitchMin(), m.pitchMax(), u), GiroCalor.yaw(m.yaw0(), m.voltas(), u));
                Vec3 fim = alvo(level, dono, olhos, dir, m.alcance());
                boolean noChao = fim.distanceTo(olhos) < m.alcance() - 0.2;
                if (noChao) impactoRaio(level, fim, m, rnd);
                // Calor tremulando ao longo do feixe.
                for (int i = 0; i < 2; i++) {
                    Vec3 q = olhos.add(dir.scale(1 + rnd.nextDouble() * Math.max(0, fim.distanceTo(olhos) - 1)));
                    level.addParticle(new DustParticleOptions(m.cor(), 0.9f), q.x, q.y, q.z, 0, 0.01, 0);
                }
                // Fumaça e poeira subindo em volta da cratera (as nuvens do conceito).
                if (rnd.nextFloat() < 0.6f) {
                    double a = rnd.nextDouble() * Math.PI * 2, r = raioFora(g, dono) * (0.75 + rnd.nextDouble() * 0.35);
                    level.addParticle(rnd.nextBoolean() ? ParticleTypes.CAMPFIRE_COSY_SMOKE : ParticleTypes.POOF,
                        centro.x + Math.cos(a) * r, centro.y + 0.3, centro.z + Math.sin(a) * r,
                        Math.cos(a) * 0.04, 0.03 + rnd.nextDouble() * 0.04, Math.sin(a) * 0.04);
                }
            } else if (t < m.ticks() + ESFRIA) {
                // A cratera esfriando: brasas, faíscas e fumaça.
                float calor = 1 - (t - m.ticks()) / ESFRIA;
                for (int i = 0; i < 3; i++) {
                    if (rnd.nextFloat() > calor) continue;
                    double a = rnd.nextDouble() * Math.PI * 2, r = raioDentro(g, dono) + rnd.nextDouble() * (raioFora(g, dono) - raioDentro(g, dono));
                    double x = centro.x + Math.cos(a) * r, z = centro.z + Math.sin(a) * r;
                    level.addParticle(i == 0 ? ParticleTypes.LARGE_SMOKE : rnd.nextFloat() < 0.3f ? ParticleTypes.LAVA : ParticleTypes.SMALL_FLAME,
                        x, centro.y + 0.1, z, 0, 0.03, 0);
                }
            }
        }
        // Detritos: gravidade, quique no chão e fim.
        for (Iterator<Pedaco> it = PEDACOS.iterator(); it.hasNext(); ) {
            Pedaco p = it.next();
            p.antes = p.pos;
            p.angAntes = p.ang;
            p.vel = new Vec3(p.vel.x * 0.98, p.vel.y - 0.045, p.vel.z * 0.98);
            Vec3 novo = p.pos.add(p.vel);
            BlockPos bp = BlockPos.containing(novo.x, novo.y - p.tam / 2, novo.z);
            if (p.vel.y < 0 && !level.getBlockState(bp).getCollisionShape(level, bp).isEmpty()) {
                if (p.quiques++ >= 1 || p.vel.y > -0.2) {
                    if (rnd.nextFloat() < 0.5f) level.addParticle(ParticleTypes.POOF, p.pos.x, p.pos.y, p.pos.z, 0, 0.02, 0);
                    it.remove();
                    continue;
                }
                p.vel = new Vec3(p.vel.x * 0.5, -p.vel.y * 0.3, p.vel.z * 0.5);
                novo = p.pos;
            }
            p.pos = novo;
            p.ang += p.giro;
            if (rnd.nextFloat() < 0.25f) level.addParticle(ParticleTypes.SMOKE, p.pos.x, p.pos.y, p.pos.z, 0, 0, 0);
            if (++p.idade > p.vida) it.remove();
        }
    }

    /** Preparo: energia vermelha correndo em espiral para os olhos e o ar esquentando embaixo dele. */
    private static void preparo(ClientLevel level, Entity dono, EstadoCliente.GiroAtivo g, float t, RandomSource rnd) {
        float f = 1 + t / g.giro().preparo();                        // 0 no começo do preparo, 1 no fim
        Vec3 olhos = dono.getEyePosition();
        for (int i = 0; i < 2 + Math.round(3 * f); i++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = 2.5 + rnd.nextDouble() * 2;
            Vec3 de = olhos.add(Math.cos(a) * r, (rnd.nextDouble() - 0.6) * 2, Math.sin(a) * r);
            // Velocidade com um giro de lado: entra em espiral.
            Vec3 para = olhos.subtract(de).scale(0.11);
            Vec3 lado = new Vec3(-Math.sin(a), 0, Math.cos(a)).scale(0.12);
            level.addParticle(new DustParticleOptions(rnd.nextFloat() < 0.3f ? 0xFFE0A0 : g.giro().cor(), 0.5f + 0.5f * f),
                de.x, de.y, de.z, para.x + lado.x, para.y, para.z + lado.z);
        }
        if (rnd.nextFloat() < 0.5f) level.addParticle(ParticleTypes.WHITE_SMOKE, dono.getX(), g.chaoY() + 0.1, dono.getZ(),
            rnd.nextGaussian() * 0.06, 0.04, rnd.nextGaussian() * 0.06);
    }

    /** Onde o raio bate no chão: fogo, faíscas, fumaça, poeira e pedaços do chão voando. */
    private static void impactoRaio(ClientLevel level, Vec3 fim, Rede.Giro m, RandomSource rnd) {
        BlockState chao = bloco(level, fim);
        for (int i = 0; i < 4; i++) {
            level.addParticle(ParticleTypes.FLAME, fim.x, fim.y + 0.05, fim.z,
                rnd.nextGaussian() * 0.08, 0.05 + rnd.nextDouble() * 0.12, rnd.nextGaussian() * 0.08);
        }
        level.addParticle(ColorParticleOption.create(ParticleTypes.FLASH, 0xFF000000 | Efeitos.misturar(m.cor(), 0xFFFFFF, 0.5f)),
            fim.x, fim.y + 0.1, fim.z, 0, 0, 0);
        if (rnd.nextFloat() < 0.7f) level.addParticle(ParticleTypes.LARGE_SMOKE, fim.x, fim.y + 0.2, fim.z, 0, 0.06, 0);
        if (rnd.nextFloat() < 0.35f) level.addParticle(ParticleTypes.LAVA, fim.x, fim.y, fim.z, 0, 0, 0);
        if (!chao.isAir()) {
            BlockParticleOption pedras = new BlockParticleOption(ParticleTypes.BLOCK, chao);
            for (int i = 0; i < 6; i++) level.addParticle(pedras, fim.x, fim.y + 0.1, fim.z,
                rnd.nextGaussian() * 0.2, 0.3 + rnd.nextDouble() * 0.4, rnd.nextGaussian() * 0.2);
            if (rnd.nextFloat() < 0.55f) {
                Vec3 v = new Vec3(rnd.nextGaussian() * 0.16, 0.38 + rnd.nextDouble() * 0.32, rnd.nextGaussian() * 0.16);
                pedaco(fim.add(0, 0.3, 0), v, chao, 0.22f + rnd.nextFloat() * 0.3f, rnd);
            }
        }
        level.addParticle(ParticleTypes.POOF, fim.x, fim.y + 0.1, fim.z, 0, 0.05, 0);
    }

    // ------------------------------------------------------------------ eventos da coreografia (client/Cena)

    /** Ele sai do chão: poeira em anel e um bafo de ar quente. */
    static void sobe(ClientLevel level, Entity e) {
        RandomSource rnd = level.getRandom();
        BlockState chao = bloco(level, e.position().add(0, -0.2, 0));
        for (int i = 0; i < 24; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, v = 0.25 + rnd.nextDouble() * 0.25;
            level.addParticle(ParticleTypes.POOF, e.getX(), e.getY() + 0.1, e.getZ(), Math.cos(a) * v, 0.02, Math.sin(a) * v);
            if (!chao.isAir() && i % 2 == 0) level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, chao),
                e.getX(), e.getY() + 0.1, e.getZ(), Math.cos(a) * v, 0.2 + rnd.nextDouble() * 0.2, Math.sin(a) * v);
        }
    }

    /** Pulso de carga: um anel de brasas fechando em volta dele. */
    static void carga(ClientLevel level, Entity e) {
        EstadoCliente.GiroAtivo g = EstadoCliente.GIROS.get(e.getId());
        int cor = g == null ? 0xFF2A1A : g.giro().cor();
        Vec3 c = e.position().add(0, e.getBbHeight() * 0.6, 0);
        for (int i = 0; i < 36; i++) {
            double a = Math.PI * 2 * i / 36, r = 3.2;
            level.addParticle(new DustParticleOptions(i % 3 == 0 ? 0xFFF0C0 : cor, 1.0f), c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r,
                -Math.cos(a) * 0.3 - Math.sin(a) * 0.12, 0, -Math.sin(a) * 0.3 + Math.cos(a) * 0.12);
        }
    }

    /** A batida no chão: clarão, onda de choque, detritos e poeira para todo lado. */
    static void impacto(ClientLevel level, Entity e) {
        RandomSource rnd = level.getRandom();
        EstadoCliente.GiroAtivo g = EstadoCliente.GIROS.get(e.getId());
        int cor = g == null ? 0xFF2A1A : g.giro().cor();
        float alcance = g == null ? 12 : g.giro().alcance();
        Vec3 c = e.position();
        BlockState chao = bloco(level, c.add(0, -0.2, 0));
        if (chao.isAir()) chao = Blocks.DIRT.defaultBlockState();
        level.addParticle(ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF), c.x, c.y + 0.5, c.z, 0, 0, 0);
        for (int i = 0; i < 3; i++) level.addParticle(ParticleTypes.EXPLOSION, c.x + rnd.nextGaussian(), c.y + 0.4, c.z + rnd.nextGaussian(), 0, 0, 0);
        Combate.onda(level, c, cor, 2.5f);
        for (int i = 0; i < 40; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, v = 0.35 + rnd.nextDouble() * 0.5;
            level.addParticle(i % 2 == 0 ? ParticleTypes.POOF : ParticleTypes.CAMPFIRE_COSY_SMOKE, c.x + Math.cos(a) * 1.5, c.y + 0.2, c.z + Math.sin(a) * 1.5,
                Math.cos(a) * v, 0.03 + rnd.nextDouble() * 0.05, Math.sin(a) * v);
            if (i % 3 == 0) level.addParticle(new DustParticleOptions(cor, 2f), c.x + Math.cos(a), c.y + 0.3, c.z + Math.sin(a),
                Math.cos(a) * v * 1.4, 0.05, Math.sin(a) * v * 1.4);
        }
        // Pedaços do chão arremessados em volta (os blocos voando do conceito).
        for (int i = 0; i < 26; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = 2.5 + rnd.nextDouble() * alcance * 0.45;
            Vec3 p = new Vec3(c.x + Math.cos(a) * r, c.y + 0.3, c.z + Math.sin(a) * r);
            BlockState b = bloco(level, p.add(0, -0.6, 0));
            Vec3 v = new Vec3(Math.cos(a) * (0.12 + rnd.nextDouble() * 0.2), 0.45 + rnd.nextDouble() * 0.45, Math.sin(a) * (0.12 + rnd.nextDouble() * 0.2));
            pedaco(p, v, b.isAir() ? chao : b, 0.25f + rnd.nextFloat() * 0.3f, rnd);
        }
    }

    private static void pedaco(Vec3 pos, Vec3 vel, BlockState b, float tam, RandomSource rnd) {
        if (PEDACOS.size() > 160) PEDACOS.remove(0);
        Vec3 eixo = new Vec3(rnd.nextGaussian(), rnd.nextGaussian(), rnd.nextGaussian());
        eixo = eixo.lengthSqr() < 1e-4 ? new Vec3(0, 1, 0) : eixo.normalize();
        PEDACOS.add(new Pedaco(pos, vel, eixo, (rnd.nextFloat() - 0.5f) * 40f, tam, textura(b), 40 + rnd.nextInt(30)));
    }

    // ------------------------------------------------------------------ desenho (uma vez por quadro, origem na câmera)

    static void desenhar(PoseStack ps, SubmitNodeCollector col, Vec3 cam, float pt) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        long agora = System.currentTimeMillis();
        for (EstadoCliente.GiroAtivo g : EstadoCliente.GIROS.values()) {
            Entity dono = level.getEntity(g.giro().entidade());
            if (dono == null || Double.isNaN(g.chaoY())) continue;
            float t = g.tempo(agora);
            Rede.Giro m = g.giro();
            Vec3 centro = new Vec3(Mth.lerp(pt, dono.xo, dono.getX()), g.chaoY(), Mth.lerp(pt, dono.zo, dono.getZ()));
            Vec3 olhos = dono.getEyePosition(pt);
            int cor = m.cor();
            float k = m.escala();
            // Cratera: acende durante o giro e esfria depois.
            float calor = t < 0 ? 0.25f * (1 + t / m.preparo()) : t < m.ticks() ? 0.25f + 0.75f * t / m.ticks()
                : 1 - (t - m.ticks()) / ESFRIA;
            if (calor > 0) {
                float rIn = raioDentro(g, dono), rOut = raioFora(g, dono);
                float pulso = 0.85f + 0.15f * Mth.sin(t * 0.7f);
                col.submitCustomGeometry(ps, Luz.tipo(), (pose, buf) -> cratera(pose.pose(), buf, centro.subtract(cam), rIn, rOut, cor, calor * pulso));
            }
            if (t >= 0 && t < m.ticks()) {
                boolean euPrimeira = dono == mc.player && mc.options.getCameraType().isFirstPerson();
                col.submitCustomGeometry(ps, Luz.tipo(), (pose, buf) -> {
                    aneis(pose.pose(), buf, g, olhos.subtract(cam), centro.y - cam.y, t, cor, k);
                    risco(pose.pose(), buf, g, olhos.subtract(cam), centro.y - cam.y + 0.04, t, cor);
                });
                // O feixe de agora, até onde ele bate de verdade.
                float u = t / m.ticks();
                Vec3 dir = Vec3.directionFromRotation(GiroCalor.pitch(m.pitchMin(), m.pitchMax(), u), GiroCalor.yaw(m.yaw0(), m.voltas(), u));
                Vec3 fim = alvo(level, dono, olhos, dir, m.alcance()).subtract(cam);
                Vec3 lado = dir.cross(new Vec3(0, 1, 0));
                lado = lado.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : lado.normalize();
                for (int s = -1; s <= 1; s += 2) {
                    Vec3 de = olhos.subtract(cam).add(dir.scale(euPrimeira ? 0.45 : 0.27)).add(lado.scale(0.12 * s))
                        .add(0, euPrimeira ? -0.14 : 0, 0);
                    col.submitCustomGeometry(ps, Luz.tipo(), (pose, buf) -> Efeitos.camadas(pose.pose(), buf, de, fim, 1.8f, cor, k));
                }
            }
            // Onda de choque da batida: um anel no chão abrindo rápido.
            float dt = t - m.ticks() - GiroCalor.QUEDA;
            if (dt >= 0 && dt < 14) {
                float r = m.alcance() * 0.9f * Mth.sqrt(dt / 14f), a = 1 - dt / 14f;
                col.submitCustomGeometry(ps, Luz.tipo(), (pose, buf) -> cratera(pose.pose(), buf, centro.subtract(cam), Math.max(0, r - 1.6f), r,
                    Efeitos.misturar(cor, 0xFFFFFF, 0.5f), a * 1.4f));
            }
        }
        // Detritos com a textura do bloco.
        for (Pedaco p : PEDACOS) {
            Vec3 pos = new Vec3(Mth.lerp(pt, p.antes.x, p.pos.x), Mth.lerp(pt, p.antes.y, p.pos.y), Mth.lerp(pt, p.antes.z, p.pos.z));
            BlockPos bp = BlockPos.containing(pos);
            int luz = LightCoordsUtil.pack(Math.max(10, level.getBrightness(LightLayer.BLOCK, bp)), level.getBrightness(LightLayer.SKY, bp));
            float ang = Mth.lerp(pt, p.angAntes, p.ang);
            Vec3 rel = pos.subtract(cam);
            col.submitCustomGeometry(ps, Luz.translucido(p.textura), (pose, buf) -> cubo(pose.pose(), buf, rel, p.tam, p.eixo, ang, luz));
        }
    }

    /**
     * Os anéis em espiral: para algumas distâncias ao longo do raio, a faixa que esse ponto do raio varreu nos últimos
     * RASTRO ticks. Duas camadas (halo largo e fio quente no meio), sumindo com a idade.
     */
    private static void aneis(Matrix4fc m4, VertexConsumer buf, EstadoCliente.GiroAtivo g, Vec3 olhos, double chaoY, float t, int cor, float k) {
        Rede.Giro m = g.giro();
        int passos = 32;
        float[] fracoes = {0.3f, 0.5f, 0.7f, 0.9f};
        for (float f : fracoes) {
            Vec3 ant = null, antEixo = null;
            float antIdade = 0;
            for (int i = 0; i <= passos; i++) {
                float idade = RASTRO * (1 - i / (float) passos);       // do mais velho para o de agora
                float tt = t - idade;
                if (tt < 0) continue;
                float u = tt / m.ticks();
                float pitch = GiroCalor.pitch(m.pitchMin(), m.pitchMax(), u);
                Vec3 dir = Vec3.directionFromRotation(pitch, GiroCalor.yaw(m.yaw0(), m.voltas(), u));
                double comp = comprimento(olhos.y - chaoY, pitch, m.alcance());
                Vec3 p = olhos.add(dir.scale(comp * f));
                if (ant != null) {
                    float a0 = alfaRastro(antIdade), a1 = alfaRastro(idade);
                    int c0 = corRastro(cor, antIdade), c1 = corRastro(cor, idade);
                    float largo = 1.1f * k * (0.5f + f), fino = 0.3f * k * (0.5f + f);
                    faixa(m4, buf, ant, p, antEixo, dir, largo, c0, c1, a0 * 0.55f, a1 * 0.55f);
                    faixa(m4, buf, ant, p, antEixo, dir, fino, Efeitos.misturar(c0, 0xFFFFFF, 0.6f), Efeitos.misturar(c1, 0xFFFFFF, 0.6f), a0, a1);
                }
                ant = p;
                antEixo = dir;
                antIdade = idade;
            }
        }
    }

    /** O risco em brasa no chão, onde a ponta do raio passou nos últimos RISCO ticks. */
    private static void risco(Matrix4fc m4, VertexConsumer buf, EstadoCliente.GiroAtivo g, Vec3 olhos, double chaoY, float t, int cor) {
        Rede.Giro m = g.giro();
        int passos = 60;
        Vec3 ant = null;
        float antIdade = 0;
        for (int i = 0; i <= passos; i++) {
            float idade = RISCO * (1 - i / (float) passos);
            float tt = t - idade;
            if (tt < 0) continue;
            float u = tt / m.ticks();
            float pitch = GiroCalor.pitch(m.pitchMin(), m.pitchMax(), u);
            Vec3 dir = Vec3.directionFromRotation(pitch, GiroCalor.yaw(m.yaw0(), m.voltas(), u));
            double comp = comprimento(olhos.y - chaoY, pitch, m.alcance());
            Vec3 p = olhos.add(dir.scale(comp));
            if (Math.abs(p.y - chaoY) > 0.3) {
                ant = null;
                continue;
            }
            p = new Vec3(p.x, chaoY, p.z);
            if (ant != null && p.distanceToSqr(ant) > 1e-4) {
                float a0 = 1 - antIdade / RISCO, a1 = 1 - idade / RISCO;
                int c0 = Efeitos.misturar(0xFFB040, cor, Mth.clamp(antIdade / 8f, 0, 1)), c1 = Efeitos.misturar(0xFFB040, cor, Mth.clamp(idade / 8f, 0, 1));
                faixa(m4, buf, ant, p, new Vec3(0, 1, 0).cross(p.subtract(ant)).normalize(), new Vec3(0, 1, 0).cross(p.subtract(ant)).normalize(),
                    0.9f, c0, c1, a0 * 0.8f, a1 * 0.8f);
            }
            ant = p;
            antIdade = idade;
        }
    }

    private static float alfaRastro(float idade) {
        float a = 1 - idade / RASTRO;
        return a * a * 0.95f;
    }

    /** Branco quente no raio de agora, vermelho no meio do rastro e vermelho escuro na ponta que some. */
    private static int corRastro(int cor, float idade) {
        float f = idade / RASTRO;
        return f < 0.25f ? Efeitos.misturar(0xFFF4D0, cor, f / 0.25f) : Efeitos.misturar(cor, 0x6A0806, (f - 0.25f) / 0.75f);
    }

    /** Comprimento do raio até o chão (plano em chaoY) ou o alcance. */
    private static double comprimento(double altura, float pitch, float alcance) {
        float s = Mth.sin(pitch * Mth.DEG_TO_RAD);
        return s <= 0.01f ? alcance : Math.min(alcance, Math.max(0.5, altura) / s);
    }

    /**
     * Uma faixa entre dois pontos do rastro. A largura vai ao longo do raio (eixo0 em a, eixo1 em b), e cada ponta tem
     * a própria cor e transparência; desenhada dos dois lados.
     */
    private static void faixa(Matrix4fc m, VertexConsumer buf, Vec3 a, Vec3 b, Vec3 eixo0, Vec3 eixo1, float largura,
                              int c0, int c1, float a0, float a1) {
        Vec3 da = eixo0.scale(largura / 2), db = eixo1.scale(largura / 2);
        Vec3[] v = {a.add(da), b.add(db), b.subtract(db), a.subtract(da)};
        int[] c = {c0, c1, c1, c0};
        float[] al = {a0, a1, a1, a0};
        for (int i = 0; i < 4; i++) vertice(m, buf, v[i], c[i], al[i]);
        for (int i = 3; i >= 0; i--) vertice(m, buf, v[i], c[i], al[i]);
    }

    /** Cratera incandescente: um anel no chão mais forte no meio e sumindo nas bordas, mais um disco fraco por dentro. */
    private static void cratera(Matrix4fc m, VertexConsumer buf, Vec3 c, float rIn, float rOut, int cor, float forca) {
        int lados = 64;
        float meio = (rIn + rOut) / 2;
        int quente = Efeitos.misturar(cor, 0xFFB040, 0.45f);
        float y = (float) c.y + 0.03f;
        for (int i = 0; i < lados; i++) {
            double a0 = Math.PI * 2 * i / lados, a1 = Math.PI * 2 * (i + 1) / lados;
            float[][] bandas = {{rIn * 0.4f, rIn, 0.1f, 0.35f}, {rIn, meio, 0.35f, 0.9f}, {meio, rOut, 0.9f, 0f}};
            for (float[] b : bandas) {
                Vec3 p00 = new Vec3(c.x + Math.cos(a0) * b[0], y, c.z + Math.sin(a0) * b[0]);
                Vec3 p01 = new Vec3(c.x + Math.cos(a1) * b[0], y, c.z + Math.sin(a1) * b[0]);
                Vec3 p11 = new Vec3(c.x + Math.cos(a1) * b[1], y, c.z + Math.sin(a1) * b[1]);
                Vec3 p10 = new Vec3(c.x + Math.cos(a0) * b[1], y, c.z + Math.sin(a0) * b[1]);
                float ai = Math.min(1, b[2] * forca) * 0.6f, ao = Math.min(1, b[3] * forca) * 0.6f;
                vertice(m, buf, p00, quente, ai);
                vertice(m, buf, p10, quente, ao);
                vertice(m, buf, p11, quente, ao);
                vertice(m, buf, p01, quente, ai);
                vertice(m, buf, p01, quente, ai);
                vertice(m, buf, p11, quente, ao);
                vertice(m, buf, p10, quente, ao);
                vertice(m, buf, p00, quente, ai);
            }
        }
    }

    private static void vertice(Matrix4fc m, VertexConsumer buf, Vec3 p, int cor, float alfa) {
        buf.addVertex(m, (float) p.x, (float) p.y, (float) p.z)
            .setColor(((cor >> 16) & 255) / 255f, ((cor >> 8) & 255) / 255f, (cor & 255) / 255f, Mth.clamp(alfa, 0, 1));
    }

    /** Cubo texturizado girando em volta de um eixo (os pedaços do chão). */
    private static void cubo(Matrix4fc m, VertexConsumer buf, Vec3 c, float tam, Vec3 eixo, float graus, int luz) {
        org.joml.Quaternionf q = new org.joml.Quaternionf().rotateAxis(graus * Mth.DEG_TO_RAD, (float) eixo.x, (float) eixo.y, (float) eixo.z);
        float h = tam / 2;
        float[][][] faces = {
            {{-h, -h, -h}, {-h, h, -h}, {h, h, -h}, {h, -h, -h}, {0, 0, -1}},
            {{h, -h, h}, {h, h, h}, {-h, h, h}, {-h, -h, h}, {0, 0, 1}},
            {{-h, -h, h}, {-h, h, h}, {-h, h, -h}, {-h, -h, -h}, {-1, 0, 0}},
            {{h, -h, -h}, {h, h, -h}, {h, h, h}, {h, -h, h}, {1, 0, 0}},
            {{-h, h, -h}, {-h, h, h}, {h, h, h}, {h, h, -h}, {0, 1, 0}},
            {{-h, -h, h}, {-h, -h, -h}, {h, -h, -h}, {h, -h, h}, {0, -1, 0}},
        };
        float[][] uv = {{0, 1}, {0, 0}, {1, 0}, {1, 1}};
        // A face de cima é mais clara e as de baixo mais escuras, como os blocos do jogo.
        int[] tom = {0xFFCCCCCC, 0xFFCCCCCC, 0xFF999999, 0xFF999999, 0xFFFFFFFF, 0xFF808080};
        for (int f = 0; f < 6; f++) {
            org.joml.Vector3f n = q.transform(new org.joml.Vector3f(faces[f][4][0], faces[f][4][1], faces[f][4][2]));
            for (int i = 0; i < 4; i++) {
                org.joml.Vector3f v = q.transform(new org.joml.Vector3f(faces[f][i][0], faces[f][i][1], faces[f][i][2]));
                buf.addVertex(m, (float) c.x + v.x, (float) c.y + v.y, (float) c.z + v.z).setColor(tom[f]).setUv(uv[i][0], uv[i][1])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(luz).setNormal(n.x, n.y, n.z);
            }
        }
    }

    // ------------------------------------------------------------------ ajudas

    /** Raios da cratera: de onde o raio mais inclinado bate até onde o menos inclinado bate (limitado pelo alcance). */
    private static float raioDentro(EstadoCliente.GiroAtivo g, Entity dono) {
        double h = g.giro().altura() + dono.getEyeHeight();
        return (float) Math.min(g.giro().alcance() * 0.6, h / Math.tan(g.giro().pitchMax() * Mth.DEG_TO_RAD));
    }

    private static float raioFora(EstadoCliente.GiroAtivo g, Entity dono) {
        double h = g.giro().altura() + dono.getEyeHeight();
        return (float) Math.min(g.giro().alcance(), h / Math.tan(g.giro().pitchMin() * Mth.DEG_TO_RAD));
    }

    private static Vec3 alvo(ClientLevel level, Entity dono, Vec3 olhos, Vec3 dir, double alcance) {
        var hit = level.clip(new ClipContext(olhos, olhos.add(dir.scale(alcance)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, dono));
        return hit.getLocation();
    }

    /** Altura do chão embaixo do ponto (até 30 blocos para baixo). */
    private static double chao(ClientLevel level, Vec3 p) {
        var hit = level.clip(new ClipContext(p.add(0, 0.5, 0), p.add(0, -30, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY,
            net.minecraft.world.phys.shapes.CollisionContext.empty()));
        return hit.getType() == HitResult.Type.MISS ? p.y : hit.getLocation().y;
    }

    private static BlockState bloco(ClientLevel level, Vec3 p) {
        BlockPos bp = BlockPos.containing(p.x, p.y - 0.05, p.z);
        BlockState b = level.getBlockState(bp);
        return b.isAir() ? level.getBlockState(bp.below()) : b;
    }

    /** Textura do bloco para o pedaço: o nome do bloco em textures/block (lado do bloco de grama), ou terra. */
    private static Identifier textura(BlockState b) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(b.getBlock());
        String nome = id.getPath();
        String[] tentativas = nome.equals("grass_block") ? new String[] {"grass_block_side"}
            : new String[] {nome, nome + "_side", nome + "_top", nome.replace("_slab", "").replace("_stairs", "")};
        for (String t : tentativas) {
            Identifier tx = Identifier.fromNamespaceAndPath(id.getNamespace(), "textures/block/" + t + ".png");
            boolean existe = TEXTURA_EXISTE.computeIfAbsent(tx, x -> Minecraft.getInstance().getResourceManager().getResource(x).isPresent());
            if (existe) return tx;
        }
        return TERRA;
    }

    public static void limpar() {
        PEDACOS.clear();
    }
}
