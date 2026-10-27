package com.example.atividadesfisicas.localizacao;

import com.example.atividadesfisicas.modelo.PontoGPS;

import java.util.List;

/**
 * UC10: distância (em metros) de uma posição à linha da rota planeada.
 * Usa uma projeção plana local (a Terra tratada como plana à volta do utilizador),
 * suficiente para distâncias curtas.
 */
public final class CalculoDesvio {

    private static final double RAIO_TERRA_METROS = 6371000.0;

    private CalculoDesvio() {
    }

    /** Menor distância da posição a qualquer segmento da rota. */
    public static double distanciaARota(PontoGPS posicao, List<PontoGPS> rota) {
        if (rota == null || rota.isEmpty()) {
            return 0;
        }
        double latitudeOrigem = Math.toRadians(posicao.getLatitude());
        if (rota.size() == 1) {
            return Math.hypot(x(rota.get(0), posicao, latitudeOrigem), y(rota.get(0), posicao));
        }
        double menor = Double.MAX_VALUE;
        for (int i = 0; i < rota.size() - 1; i++) {
            // extremos A e B do segmento, em metros, com o utilizador na origem (0, 0)
            double ax = x(rota.get(i), posicao, latitudeOrigem);
            double ay = y(rota.get(i), posicao);
            double bx = x(rota.get(i + 1), posicao, latitudeOrigem);
            double by = y(rota.get(i + 1), posicao);
            double dx = bx - ax;
            double dy = by - ay;
            double comprimento2 = dx * dx + dy * dy;
            // t = posição, ao longo do segmento, do ponto mais próximo do utilizador (0 = A, 1 = B)
            double t = 0;
            if (comprimento2 > 0) {
                t = ((-ax) * dx + (-ay) * dy) / comprimento2;
                t = Math.max(0, Math.min(1, t));
            }
            double cx = ax + t * dx;
            double cy = ay + t * dy;
            menor = Math.min(menor, Math.hypot(cx, cy));
        }
        return menor;
    }

    /** Diferença de longitude em metros (eixo este–oeste). */
    private static double x(PontoGPS p, PontoGPS origem, double latitudeOrigem) {
        return Math.toRadians(p.getLongitude() - origem.getLongitude()) * Math.cos(latitudeOrigem) * RAIO_TERRA_METROS;
    }

    /** Diferença de latitude em metros (eixo norte–sul). */
    private static double y(PontoGPS p, PontoGPS origem) {
        return Math.toRadians(p.getLatitude() - origem.getLatitude()) * RAIO_TERRA_METROS;
    }
}
