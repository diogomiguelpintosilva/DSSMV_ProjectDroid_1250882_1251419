package com.example.atividadesfisicas.localizacao;

import static org.junit.Assert.assertEquals;

import com.example.atividadesfisicas.modelo.PontoGPS;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Testa o cálculo da distância (em metros) entre o utilizador e a rota. */
public class CalculoDesvioTest {

    // 1 grau de latitude = ~111 195 m
    private static final double METROS_POR_GRAU = Math.toRadians(1) * 6371000.0;

    private static PontoGPS p(double lat, double lon) {
        return new PontoGPS(lat, lon, 0, 0);
    }

    @Test
    public void rotaVazia_devolveZero() {
        assertEquals(0, CalculoDesvio.distanciaARota(p(41, -8.6), new ArrayList<PontoGPS>()), 0.0);
    }

    @Test
    public void posicaoSobreARota_distanciaZero() {
        List<PontoGPS> rota = Arrays.asList(p(41, -8.61), p(41, -8.60));
        assertEquals(0, CalculoDesvio.distanciaARota(p(41, -8.605), rota), 0.01);
    }

    @Test
    public void posicaoAoLadoDaRota_distanciaPerpendicular() {
        // 0,0003 graus a norte do segmento = ~33,4 m
        List<PontoGPS> rota = Arrays.asList(p(41, -8.61), p(41, -8.60));
        assertEquals(METROS_POR_GRAU * 0.0003, CalculoDesvio.distanciaARota(p(41.0003, -8.605), rota), 0.1);
    }
}