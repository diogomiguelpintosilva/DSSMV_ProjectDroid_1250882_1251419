package com.example.atividadesfisicas.localizacao;

import com.example.atividadesfisicas.modelo.PontoGPS;
import java.util.List;

/**
 * Lógica de desvio de rota: distância (em metros) de uma posição à linha da rota planeada.
 * Usa uma projeção plana local, suficiente para distâncias curtas.
 */
public final class CalculoDesvio {

    private static final double RAIO_TERRA = 6371000.0;

    private CalculoDesvio() {
    }

    public static double distanciaARota(PontoGPS posicao, List<PontoGPS> rota) {
        if (rota == null || rota.isEmpty()) {
            return 0;
        }
        double lat0 = Math.toRadians(posicao.getLatitude());
        double melhor = Double.MAX_VALUE;
        if (rota.size() == 1) {
            return Math.hypot(x(rota.get(0), posicao, lat0), y(rota.get(0), posicao));
        }
        for (int i = 0; i < rota.size() - 1; i++) {
            double ax = x(rota.get(i), posicao, lat0);
            double ay = y(rota.get(i), posicao);
            double bx = x(rota.get(i + 1), posicao, lat0);
            double by = y(rota.get(i + 1), posicao);
            double dx = bx - ax;
            double dy = by - ay;
            double comprimento2 = dx * dx + dy * dy;
            double t = 0;
            if (comprimento2 > 0) {
                t = ((-ax) * dx + (-ay) * dy) / comprimento2;
                t = Math.max(0, Math.min(1, t));
            }
            double cx = ax + t * dx;
            double cy = ay + t * dy;
            double distancia = Math.hypot(cx, cy);
            if (distancia < melhor) {
                melhor = distancia;
            }
        }
        return melhor;
    }

    private static double x(PontoGPS p, PontoGPS origem, double lat0) {
        return Math.toRadians(p.getLongitude() - origem.getLongitude()) * Math.cos(lat0) * RAIO_TERRA;
    }

    private static double y(PontoGPS p, PontoGPS origem) {
        return Math.toRadians(p.getLatitude() - origem.getLatitude()) * RAIO_TERRA;
    }
}