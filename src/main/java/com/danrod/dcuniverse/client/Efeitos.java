package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.core.EstadoCliente;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import com.danrod.dcuniverse.core.Rede;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLivingEvent;
import org.joml.Matrix4fc;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/**
 * Efeitos visuais desenhados no próprio cliente (sem pacotes de partícula): raios e sopros canalizados,
 * a espiral de transformação e o rastro de ar da queda amortecida. O servidor só diz quem está canalizando
 * o quê (Rede.Aparencia); o dano continua no servidor.
 */
public final class Efeitos {
    private Efeitos() {}

    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) return;
        for (Player pl : level.players()) {
            Rede.Aparencia a = EstadoCliente.APARENCIAS.get(pl.getId());
            if (a == null || a.efeito().isEmpty() || pl.isInvisible()) continue;
            boolean primeiraPessoa = pl == mc.player && mc.options.getCameraType().isFirstPerson();
            switch (a.efeito()) {
                case "projectile_beam" -> raio(level, pl, a, primeiraPessoa);
                case "area_cone" -> sopro(level, pl, a, primeiraPessoa);
                case "bracelet_blast" -> carga(level, pl, a, primeiraPessoa);
                default -> {}
            }
        }
        EstadoCliente.TRANSFORMOU.forEach((id, a) -> {
            if (level.getEntity(id) instanceof Player pl) transformacao(level, pl, a.cor());
        });
        EstadoCliente.TRANSFORMOU.clear();
        if (mc.player != null && Planador.amortecendo(mc.player)) rastro(level, mc.player);
    }

    /**
     * Carga do golpe de braceletes: faíscas douradas e pequenos raios saltando entre os braceletes cruzados na
     * frente do peito, cada vez mais (o servidor não diz a carga; o cliente conta desde que a pose subiu).
     */
    private static void carga(ClientLevel level, Player pl, Rede.Aparencia a, boolean primeiraPessoa) {
        RandomSource rnd = level.getRandom();
        float g = Bloqueador.erguido(pl.getId(), 1);
        Vec3 olhar = Vec3.directionFromRotation(0, pl.getYRot());
        Vec3 c = pl.position().add(0, pl.getBbHeight() * (primeiraPessoa ? 0.78 : 0.72), 0).add(olhar.scale(primeiraPessoa ? 0.75 : 0.45));
        int n = 1 + Math.round(3 * g);
        for (int i = 0; i < n; i++) {
            level.addParticle(new DustParticleOptions(rnd.nextInt(3) == 0 ? 0xFFFFFF : a.efeitoCor(), 0.55f),
                c.x + (rnd.nextDouble() - 0.5) * 0.35, c.y + (rnd.nextDouble() - 0.5) * 0.3, c.z + (rnd.nextDouble() - 0.5) * 0.35,
                (rnd.nextDouble() - 0.5) * 0.04, 0.02, (rnd.nextDouble() - 0.5) * 0.04);
        }
        if (rnd.nextInt(3) == 0) level.addParticle(ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z, rnd.nextGaussian() * 0.12, rnd.nextGaussian() * 0.12, rnd.nextGaussian() * 0.12);
    }

    /** Dois feixes saindo dos olhos que se encontram no alvo, com núcleo claro, olhos acesos e faíscas no impacto. */
    private static void raio(ClientLevel level, Player pl, Rede.Aparencia a, boolean primeiraPessoa) {
        RandomSource rnd = level.getRandom();
        Vec3 olhos = pl.getEyePosition();
        Vec3 dir = pl.getViewVector(1f);
        Vec3 fim = alvo(level, pl, olhos, dir, a.alcance());
        Vec3 lado = dir.cross(new Vec3(0, 1, 0));
        lado = lado.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : lado.normalize();
        DustParticleOptions fora = new DustParticleOptions(a.efeitoCor(), 1.1f);
        DustParticleOptions nucleo = new DustParticleOptions(misturar(a.efeitoCor(), 0xFFF2C0, 0.6f), 0.55f);
        // Em primeira pessoa o feixe é desenhado no mundo (feixePrimeiraPessoa); aqui só um calor tremulando em volta.
        if (primeiraPessoa && rnd.nextFloat() < 0.5f) {
            double t = 1.5 + rnd.nextDouble() * Math.max(0, fim.distanceTo(olhos) - 1.5);
            Vec3 p = olhos.add(dir.scale(t)).add(lado.scale((rnd.nextDouble() - 0.5) * 0.3));
            level.addParticle(fora, p.x, p.y - 0.1, p.z, 0, 0.01, 0);
        }
        if (rnd.nextFloat() < 0.5f) level.addParticle(nucleo, fim.x, fim.y, fim.z, 0, 0, 0);
        for (int i = 0; i < 3; i++) {
            level.addParticle(ParticleTypes.SMALL_FLAME, fim.x, fim.y, fim.z,
                (rnd.nextDouble() - 0.5) * 0.12, rnd.nextDouble() * 0.1, (rnd.nextDouble() - 0.5) * 0.12);
        }
        if (rnd.nextFloat() < 0.35f) level.addParticle(ParticleTypes.SMOKE, fim.x, fim.y, fim.z, 0, 0.04, 0);
        if (rnd.nextFloat() < 0.08f) level.addParticle(ParticleTypes.LAVA, fim.x, fim.y, fim.z, 0, 0, 0);
    }

    /** Jato de neve e névoa saindo da boca, abrindo no ângulo do cone. */
    private static void sopro(ClientLevel level, Player pl, Rede.Aparencia a, boolean primeiraPessoa) {
        RandomSource rnd = level.getRandom();
        Vec3 dir = pl.getViewVector(1f);
        Vec3 boca = pl.getEyePosition().add(0, -0.22, 0).add(dir.scale(primeiraPessoa ? 0.9 : 0.35));
        double abre = Math.tan(Math.toRadians(a.angulo())) * 0.5;
        double vel = 0.35 + a.alcance() * 0.05;
        for (int i = 0; i < 7; i++) {
            Vec3 v = dir.add((rnd.nextDouble() - 0.5) * abre * 2, (rnd.nextDouble() - 0.5) * abre * 2, (rnd.nextDouble() - 0.5) * abre * 2)
                .normalize().scale(vel * (0.7 + rnd.nextDouble() * 0.5));
            level.addParticle(ParticleTypes.SNOWFLAKE, boca.x, boca.y, boca.z, v.x, v.y, v.z);
        }
        for (int i = 0; i < 2; i++) {
            Vec3 v = dir.scale(vel * 0.6).add((rnd.nextDouble() - 0.5) * 0.08, (rnd.nextDouble() - 0.5) * 0.08, (rnd.nextDouble() - 0.5) * 0.08);
            level.addParticle(ParticleTypes.CLOUD, boca.x, boca.y, boca.z, v.x, v.y, v.z);
        }
        if (rnd.nextFloat() < 0.4f) {
            Vec3 p = boca.add(dir.scale(rnd.nextDouble() * a.alcance()));
            level.addParticle(new DustParticleOptions(0xBFEFFF, 0.8f), p.x, p.y, p.z, 0, -0.01, 0);
        }
    }

    /** Espiral dupla de luz na cor do personagem subindo pelo corpo e um clarão em cima. */
    private static void transformacao(ClientLevel level, Player pl, int cor) {
        RandomSource rnd = level.getRandom();
        DustParticleOptions dust = new DustParticleOptions(cor, 1.3f);
        DustParticleOptions claro = new DustParticleOptions(misturar(cor, 0xFFFFFF, 0.6f), 0.8f);
        for (int i = 0; i < 48; i++) {
            double t = i / 48.0;
            for (int fio = 0; fio < 2; fio++) {
                double ang = t * Math.PI * 4 + fio * Math.PI;
                double r = 0.9 - t * 0.3;
                double x = pl.getX() + Math.cos(ang) * r, y = pl.getY() + t * 2.2, z = pl.getZ() + Math.sin(ang) * r;
                level.addParticle(fio == 0 ? dust : claro, x, y, z, 0, 0.02, 0);
            }
        }
        for (int i = 0; i < 24; i++) {
            level.addParticle(ParticleTypes.END_ROD, pl.getX(), pl.getY() + 1.1, pl.getZ(),
                (rnd.nextDouble() - 0.5) * 0.4, (rnd.nextDouble() - 0.5) * 0.4, (rnd.nextDouble() - 0.5) * 0.4);
        }
    }

    /** Fiapos de ar passando pelo corpo enquanto plana (só a cada poucos ticks, para não poluir a tela). */
    private static void rastro(ClientLevel level, Player pl) {
        if (pl.tickCount % 3 != 0) return;
        RandomSource rnd = level.getRandom();
        double x = pl.getX() + (rnd.nextDouble() - 0.5) * 0.8, z = pl.getZ() + (rnd.nextDouble() - 0.5) * 0.8;
        level.addParticle(ParticleTypes.CLOUD, x, pl.getY() + rnd.nextDouble() * 0.4, z, 0, 0.08, 0);
    }

    /** Feixe de luz (mistura aditiva, brilha no escuro) saindo dos olhos de quem é visto em terceira pessoa. */
    public static void registrarFeixe() {
        RenderLivingEvent.Post.BUS.addListener(e -> {
            if (!(e.getState() instanceof AvatarRenderState st)) return;
            Rede.Aparencia a = EstadoCliente.APARENCIAS.get(st.id);
            ClientLevel level = Minecraft.getInstance().level;
            if (a == null || !a.efeito().equals("projectile_beam") || level == null
                || !(level.getEntity(st.id) instanceof Player pl)) return;
            Vec3 olhos = new Vec3(st.x, st.y + st.eyeHeight, st.z);
            Vec3 dir = pl.getViewVector(1f);
            Vec3 fim = alvo(level, pl, olhos, dir, a.alcance()).subtract(st.x, st.y, st.z);
            Vec3 lado = dir.cross(new Vec3(0, 1, 0));
            lado = lado.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : lado.normalize();
            Vec3 base = new Vec3(0, st.eyeHeight, 0).add(dir.scale(0.27));
            int cor = a.efeitoCor();
            int claro = misturar(cor, 0xFFF4D0, 0.75f);
            for (int s = -1; s <= 1; s += 2) {
                Vec3 de = base.add(lado.scale(0.125 * s));
                e.getNodeCollector().submitCustomGeometry(e.getPoseStack(), Luz.tipo(), (pose, buf) -> {
                    feixe(pose.pose(), buf, de, fim, 0.16f, cor, 0.18f);
                    feixe(pose.pose(), buf, de, fim, 0.07f, cor, 0.55f);
                    feixe(pose.pose(), buf, de, fim, 0.03f, claro, 0.95f);
                });
            }
        });
    }

    /**
     * O próprio feixe visto em primeira pessoa: sai logo abaixo e ao lado dos olhos e converge na mira,
     * mais fino que o de terceira pessoa para não tapar o alvo. Chamado pelo Mundo.desenhar.
     */
    static void feixePrimeiraPessoa(PoseStack ps, SubmitNodeCollector col, Vec3 cam, float pt) {
        Minecraft mc = Minecraft.getInstance();
        Player pl = mc.player;
        if (pl == null || mc.level == null || !mc.options.getCameraType().isFirstPerson()) return;
        Rede.Aparencia a = EstadoCliente.APARENCIAS.get(pl.getId());
        if (a == null || !a.efeito().equals("projectile_beam")) return;
        Vec3 dir = pl.getViewVector(pt);
        Vec3 olhos = pl.getEyePosition(pt);
        Vec3 fim = alvo(mc.level, pl, olhos, dir, a.alcance()).subtract(cam);
        Vec3 lado = dir.cross(new Vec3(0, 1, 0));
        lado = lado.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : lado.normalize();
        Vec3 baixo = dir.cross(lado).normalize();
        int cor = a.efeitoCor();
        int claro = misturar(cor, 0xFFF4D0, 0.75f);
        float tremor = 0.9f + 0.1f * Mth.sin((mc.level.getGameTime() + pt) * 1.7f);
        for (int s = -1; s <= 1; s += 2) {
            Vec3 de = olhos.subtract(cam).add(dir.scale(0.4)).add(lado.scale(0.09 * s)).add(baixo.scale(0.13));
            col.submitCustomGeometry(ps, Luz.tipo(), (pose, buf) -> {
                feixe(pose.pose(), buf, de, fim, 0.09f, cor, 0.22f * tremor);
                feixe(pose.pose(), buf, de, fim, 0.04f, cor, 0.6f * tremor);
                feixe(pose.pose(), buf, de, fim, 0.016f, claro, 0.95f);
            });
        }
    }

    /** Dois planos cruzados ao longo do segmento, visíveis dos dois lados. */
    private static void feixe(Matrix4fc m, VertexConsumer buf, Vec3 de, Vec3 ate, float largura, int cor, float alfa) {
        Vec3 d = ate.subtract(de).normalize();
        Vec3 n1 = d.cross(new Vec3(0, 1, 0));
        n1 = (n1.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : n1.normalize()).scale(largura / 2);
        Vec3 n2 = d.cross(n1).normalize().scale(largura / 2);
        float r = ((cor >> 16) & 255) / 255f, g = ((cor >> 8) & 255) / 255f, b = (cor & 255) / 255f;
        for (Vec3 n : new Vec3[] {n1, n2}) {
            Vec3[] v = {de.add(n), ate.add(n), ate.subtract(n), de.subtract(n)};
            for (int i = 0; i < 4; i++) buf.addVertex(m, (float) v[i].x, (float) v[i].y, (float) v[i].z).setColor(r, g, b, alfa);
            for (int i = 3; i >= 0; i--) buf.addVertex(m, (float) v[i].x, (float) v[i].y, (float) v[i].z).setColor(r, g, b, alfa);
        }
    }

    private static Vec3 alvo(ClientLevel level, Player pl, Vec3 olhos, Vec3 dir, double alcance) {
        Vec3 fim = olhos.add(dir.scale(alcance));
        HitResult bloco = level.clip(new ClipContext(olhos, fim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, pl));
        if (bloco.getType() != HitResult.Type.MISS) fim = bloco.getLocation();
        EntityHitResult ent = ProjectileUtil.getEntityHitResult(pl, olhos, fim,
            pl.getBoundingBox().expandTowards(fim.subtract(olhos)).inflate(1), e -> !e.isSpectator() && e.isPickable(),
            fim.distanceToSqr(olhos));
        return ent != null ? ent.getLocation() : fim;
    }

    static int misturar(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
