package com.danrod.dcuniverse.world.smallville;

import com.danrod.dcuniverse.world.estrutura.Estruturas;
import com.danrod.dcuniverse.world.estrutura.Local;
import com.danrod.dcuniverse.world.estrutura.PecaObra;
import com.danrod.dcuniverse.world.estrutura.Planta;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

/**
 * Smallville: a vila rural do Kansas. Uma por mundo, perto do ponto de nascimento (structure_set
 * smallville). A Rua Principal corre de oeste para leste com as casas, a igreja, o Talon e o armazém;
 * a leste a estrada de terra leva à Fazenda Kent, e no pasto ao sul fica a Cratera de Meteoro da chuva
 * de 1989.
 */
public final class Smallville implements Local {
    private Smallville() {}

    public static void registrar() {
        Estruturas.LOCAIS.put("smallville", new Smallville());
        Estruturas.PLANTAS.put("smallville/casa", new Casas.CasaVila());
        Estruturas.PLANTAS.put("smallville/rua_principal", new Ruas.Estrada(130, 7, Ruas.Piso.RUA, 14));
        Estruturas.PLANTAS.put("smallville/estrada_fazenda", new Ruas.Estrada(90, 5, Ruas.Piso.TERRA, 0));
        Estruturas.PLANTAS.put("smallville/trilha_kent", new Ruas.Estrada(16, 3, Ruas.Piso.TERRA, 0));
        Estruturas.PLANTAS.put("smallville/trilha_celeiro", new Ruas.Estrada(22, 3, Ruas.Piso.TERRA, 0));
        Estruturas.PLANTAS.put("smallville/casa_kent", new Casas.CasaKent());
        Estruturas.PLANTAS.put("smallville/celeiro", new Fazenda.Celeiro());
        Estruturas.PLANTAS.put("smallville/silo", new Fazenda.Silo());
        Estruturas.PLANTAS.put("smallville/cata_vento", new Fazenda.CataVento());
        Estruturas.PLANTAS.put("smallville/curral", new Fazenda.Curral());
        Estruturas.PLANTAS.put("smallville/estabulo", new Fazenda.Estabulo());
        Estruturas.PLANTAS.put("smallville/correio_kent", new Fazenda.Correio());
        Estruturas.PLANTAS.put("smallville/espantalho", new Fazenda.Espantalho());
        Estruturas.PLANTAS.put("smallville/campo_trigo", new Fazenda.Campo(30, 19, "trigo"));
        Estruturas.PLANTAS.put("smallville/campo_trigo2", new Fazenda.Campo(24, 19, "trigo"));
        Estruturas.PLANTAS.put("smallville/campo_milho", new Fazenda.Campo(22, 19, "batata"));
        Estruturas.PLANTAS.put("smallville/campo_cenoura", new Fazenda.Campo(20, 14, "cenoura"));
        Estruturas.PLANTAS.put("smallville/campo_beterraba", new Fazenda.Campo(14, 14, "beterraba"));
        Estruturas.PLANTAS.put("smallville/campo_abobora", new Fazenda.Campo(28, 16, "abobora"));
        Estruturas.PLANTAS.put("smallville/cratera", new Fazenda.CrateraPasto());
        Estruturas.PLANTAS.put("smallville/talon", new Vila.Talon());
        Estruturas.PLANTAS.put("smallville/armazem", new Vila.Armazem());
        Estruturas.PLANTAS.put("smallville/igreja", new Vila.Igreja());
        Estruturas.PLANTAS.put("smallville/caixa_dagua", new Vila.CaixaDagua());
        Estruturas.PLANTAS.put("smallville/boas_vindas", new Vila.BoasVindas());
        Estruturas.PLANTAS.put("smallville/placa_estrada", new Vila.PlacaEstrada());
        Estruturas.PLANTAS.put("smallville/praca", new Vila.Praca());
    }

    @Override
    public int[] area() {
        return new int[]{-126, -73, 115, 45};
    }

    @Override
    public java.util.Map<String, Ponto> pontos() {
        return java.util.Map.ofEntries(
            java.util.Map.entry("centro", new Ponto("smallville/rua_principal", 65, 1, 4, true)),
            java.util.Map.entry("kent", new Ponto("smallville/casa_kent", 8, 1, -2, true)),
            java.util.Map.entry("quarto_clark", new Ponto("smallville/casa_kent", 4, 7, 7, false)),
            java.util.Map.entry("celeiro", new Ponto("smallville/celeiro", 7, 1, -2, true)),
            java.util.Map.entry("sotao", new Ponto("smallville/celeiro", 8, 8, 16, false)),
            java.util.Map.entry("nave", new Ponto("smallville/celeiro", 10, -4, 16, false)),
            java.util.Map.entry("talon", new Ponto("smallville/talon", 9, 1, -3, true)),
            java.util.Map.entry("igreja", new Ponto("smallville/igreja", 6, 1, -3, true)),
            java.util.Map.entry("praca", new Ponto("smallville/praca", 13, 2, 11, false)),
            java.util.Map.entry("cratera", new Ponto("smallville/cratera", Fazenda.CrateraPasto.C, 1, -2, true)),
            java.util.Map.entry("martha", new Ponto("smallville/casa_kent", 12, 2, 7, false)),
            java.util.Map.entry("lana", new Ponto("smallville/talon", 7, 1, 13, false)),
            java.util.Map.entry("armazem", new Ponto("smallville/armazem", 6, 1, -2, true)));
    }

    /** Cidadãos de cada casa da vila, na ordem das casas no traçado. */
    private static final String CIDADAOS = "dcuniverse:cidadao_fazendeiro,dcuniverse:cidadao_jornaleiro,dcuniverse:cidadao_fazendeiro,"
        + "dcuniverse:cidadao_xerife,dcuniverse:cidadao_fazendeira,dcuniverse:cidadao_jornaleiro,dcuniverse:cidadao_fazendeira";

    @Override
    public java.util.List<Morador> moradores() {
        return java.util.List.of(
            new Morador("martha", "dcuniverse:martha_kent", "smallville/casa_kent", 12, 2, 7, false),
            new Morador("lana", "dcuniverse:lana_lang", "smallville/talon", 7, 1, 13, false),
            new Morador("comerciante", "dcuniverse:cidadao_comerciante", "smallville/armazem", 11, 2, 8, false),
            new Morador("praca", "dcuniverse:cidadao_jornaleiro", "smallville/praca", 13, 2, 11, false),
            new Morador("casa", CIDADAOS, "smallville/casa", 6, 2, 2, true),
            new Morador("krypto", "entity:dcuniverse:krypto", "smallville/casa_kent", 8, 1, -3, false),
            new Morador("streaky", "entity:dcuniverse:streaky", "smallville/casa_kent", 13, 2, 2, false));
    }

    @Override
    public void tracar(Terreno t, BlockPos c, StructurePiecesBuilder pecas, long semente) {
        Traco tr = new Traco(t, c, pecas, semente);
        // Rua Principal: z de -3 a 3 (com as calçadas), x de -115 a 14.
        int rua = -4;
        // O perfil da Rua Principal sai primeiro: as construções dos dois lados ficam na altura dela.
        tr.prepararRua("smallville/rua_principal", -115, rua);
        // Lado norte da rua (fachada para o sul): igreja, casa, Talon, armazém e mais casas.
        tr.predio("smallville/igreja", -110, rua - 25, Direction.SOUTH);
        tr.predio("smallville/casa", -94, rua - 15, Direction.SOUTH);
        tr.predio("smallville/talon", -78, rua - 16, Direction.SOUTH);
        tr.predio("smallville/armazem", -56, rua - 15, Direction.SOUTH);
        tr.predio("smallville/casa", -40, rua - 15, Direction.SOUTH);
        tr.predio("smallville/casa", -22, rua - 15, Direction.SOUTH);
        // Lado sul (fachada para o norte): casa, praça com a caixa d'água atrás, casas.
        int sul = rua + 10;
        tr.predio("smallville/casa", -110, sul, Direction.NORTH);
        tr.predio("smallville/praca", -94, sul, Direction.NORTH);
        tr.predio("smallville/caixa_dagua", -64, sul + 8, Direction.NORTH);
        tr.predio("smallville/casa", -48, sul, Direction.NORTH);
        tr.predio("smallville/casa", -32, sul, Direction.NORTH);
        tr.predio("smallville/casa", -16, sul, Direction.NORTH);
        // Placa de boas-vindas na entrada oeste e a placa de direção no fim da rua.
        tr.predio("smallville/boas_vindas", -122, sul - 1, Direction.WEST);
        tr.predio("smallville/placa_estrada", 12, sul - 1, Direction.WEST);

        // Fazenda Kent, ao norte da estrada de terra.
        int fz = rua + 1;
        tr.predio("smallville/casa_kent", 20, fz - 33, Direction.SOUTH);
        tr.predio("smallville/celeiro", 48, fz - 43, Direction.SOUTH);
        tr.predio("smallville/silo", 64, fz - 37, Direction.SOUTH);
        tr.predio("smallville/cata_vento", 39, fz - 21, Direction.SOUTH);
        tr.predio("smallville/curral", 64, fz - 19, Direction.SOUTH);
        tr.predio("smallville/estabulo", 82, fz - 17, Direction.SOUTH);
        tr.predio("smallville/campo_trigo", 16, fz - 56, Direction.SOUTH);
        tr.predio("smallville/campo_trigo2", 48, fz - 66, Direction.SOUTH);
        tr.predio("smallville/campo_milho", 76, fz - 50, Direction.SOUTH);
        tr.predio("smallville/campo_cenoura", 92, fz - 32, Direction.SOUTH);
        tr.predio("smallville/campo_beterraba", 0, fz - 33, Direction.SOUTH);
        tr.predio("smallville/campo_abobora", 22, fz + 9, Direction.SOUTH);
        tr.predio("smallville/espantalho", 30, fz - 48, Direction.SOUTH);
        tr.predio("smallville/correio_kent", 31, fz - 1, Direction.NORTH);
        // A cratera do meteoro de 1989, no pasto ao sul da estrada.
        tr.predio("smallville/cratera", 60, fz + 12, Direction.NORTH);

        // Ruas por último: leem o terreno já arrumado pelas construções.
        tr.rua("smallville/rua_principal", -115, rua);
        tr.rua("smallville/estrada_fazenda", 15, fz);
        tr.trilha("smallville/trilha_kent", 26, fz - 15);
        tr.trilha("smallville/trilha_celeiro", 53, fz - 21);
    }

    /** Ajudante do traçado: posição em relação ao centro, altura do chão e semente de cada peça. */
    private static final class Traco {
        final Terreno t;
        final BlockPos c;
        final StructurePiecesBuilder pecas;
        long semente;

        Traco(Terreno t, BlockPos c, StructurePiecesBuilder pecas, long semente) {
            this.t = t;
            this.c = c;
            this.pecas = pecas;
            this.semente = semente;
        }

        /** Perfil (altura absoluta de cada trecho) e começo da Rua Principal, depois de {@link #prepararRua}. */
        int[] ruaH;
        int ruaX0, ruaZ0;

        /** Construção com o canto noroeste em (dx, dz) a partir do centro e a fachada virada para {@code frente}. */
        void predio(String planta, int dx, int dz, Direction frente) {
            int[] f = PecaObra.pegada(planta, frente);
            int x = c.getX() + dx, z = c.getZ() + dz;
            int y = t.nivel(x, z, x + f[0] - 1, z + f[1] - 1) - 1;
            // De frente para a Rua Principal: o chão fica na altura da rua (até 3 blocos de diferença do terreno).
            boolean naRua = ruaH != null && (frente == Direction.NORTH || frente == Direction.SOUTH) && Math.abs(z - ruaZ0) < 40;
            int meio = x + f[0] / 2 - ruaX0;
            if (naRua && meio >= 0 && meio < ruaH.length) y = Math.max(y - 3, Math.min(y + 3, ruaH[meio]));
            pecas.addPiece(PecaObra.comFrente(planta, new BlockPos(x, y, z), frente, ++semente * 0x9E3779B97F4A7C15L));
        }

        void prepararRua(String planta, int dx, int dz) {
            ruaX0 = c.getX() + dx;
            ruaZ0 = c.getZ() + dz;
            ruaH = perfil(planta, ruaX0, ruaZ0, Direction.NORTH);
        }

        /** Rua ao longo do eixo x, começando em (dx, dz); acompanha o terreno suavizado. */
        void rua(String planta, int dx, int dz) {
            int x = c.getX() + dx, z = c.getZ() + dz;
            int[] h = ruaH != null && x == ruaX0 && z == ruaZ0 ? ruaH : perfil(planta, x, z, Direction.NORTH);
            estrada(planta, x, z, Direction.NORTH, h);
        }

        /** Trilha ao longo do eixo z (de norte para sul), começando em (dx, dz). */
        void trilha(String planta, int dx, int dz) {
            int x = c.getX() + dx, z = c.getZ() + dz;
            estrada(planta, x, z, Direction.EAST, perfil(planta, x, z, Direction.EAST));
        }

        /**
         * Altura do chão em cada trecho do eixo da estrada, com média móvel (sem degraus a cada bloco) e no
         * máximo um bloco de diferença entre trechos vizinhos. Sobre a água, a altura da água (vira ponte).
         */
        private int[] perfil(String planta, int x, int z, Direction frente) {
            Planta p = Estruturas.planta(planta);
            int n = p.largura(), meio = (p.fundo() - 1) / 2;
            boolean aoLongoDeX = frente == Direction.NORTH;
            int[] cru = new int[n];
            for (int i = 0; i < n; i++) {
                int wx = aoLongoDeX ? x + i : x + meio, wz = aoLongoDeX ? z + meio : z + i;
                cru[i] = t.agua(wx, wz) ? t.superficie(wx, wz) - 1 : t.fundo(wx, wz) - 1;
            }
            int[] h = new int[n];
            for (int i = 0; i < n; i++) {
                double soma = 0;
                int k = 0;
                for (int j = Math.max(0, i - 8); j <= Math.min(n - 1, i + 8); j++, k++) soma += cru[j];
                h[i] = (int) Math.round(soma / k);
            }
            for (int i = 1; i < n; i++) h[i] = Math.max(h[i - 1] - 1, Math.min(h[i - 1] + 1, h[i]));
            return h;
        }

        /** Põe a estrada com o perfil relativo à altura mediana (a origem da peça). */
        private void estrada(String planta, int x, int z, Direction frente, int[] h) {
            Planta p = Estruturas.planta(planta);
            int n = h.length;
            int[] ordem = h.clone();
            java.util.Arrays.sort(ordem);
            int y0 = ordem[n / 2];
            int lim = Math.min(p.altura() - 6, p.abaixo() - 2);
            int[] perfil = new int[n];
            for (int i = 0; i < n; i++) perfil[i] = Math.max(-lim, Math.min(lim, h[i] - y0));
            pecas.addPiece(PecaObra.comFrente(planta, new BlockPos(x, y0, z), frente, ++semente * 0x9E3779B97F4A7C15L, perfil));
        }
    }
}
