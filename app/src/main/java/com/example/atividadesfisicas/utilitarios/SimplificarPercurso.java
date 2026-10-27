package com.example.atividadesfisicas.utilitarios;

import android.location.Location;

import com.example.atividadesfisicas.modelo.PontoGPS;

import java.util.ArrayList;
import java.util.List;

/**
 * Reduz o número de pontos de um percurso antes de o guardar (histórico e rotas guardadas).
 */
public final class SimplificarPercurso {

    private SimplificarPercurso() {
    }

    /**
     * Mantém só pontos afastados pelo menos distanciaMinimaMetros do último ponto mantido
     * (mais o último ponto do percurso) e limita o total a maximoPontos.
     */
    public static List<PontoGPS> simplificar(List<PontoGPS> pontos, double distanciaMinimaMetros,
                                             int maximoPontos) {
        List<PontoGPS> resultado = new ArrayList<>();
        if (pontos == null || pontos.isEmpty()) {
            return resultado;
        }

        // 1.º filtro: distância mínima entre pontos guardados
        float[] distancia = new float[1];
        PontoGPS ultimo = pontos.get(0);
        resultado.add(ultimo);
        for (PontoGPS p : pontos) {
            Location.distanceBetween(ultimo.getLatitude(), ultimo.getLongitude(),
                    p.getLatitude(), p.getLongitude(), distancia);
            if (distancia[0] >= distanciaMinimaMetros) {
                resultado.add(p);
                ultimo = p;
            }
        }
        PontoGPS fim = pontos.get(pontos.size() - 1);
        if (resultado.get(resultado.size() - 1) != fim) {
            resultado.add(fim);
        }

        // 2.º filtro: número máximo de pontos, escolhidos igualmente espaçados
        if (maximoPontos >= 2 && resultado.size() > maximoPontos) {
            List<PontoGPS> reduzido = new ArrayList<>();
            double passo = (resultado.size() - 1) / (double) (maximoPontos - 1);
            for (int i = 0; i < maximoPontos; i++) {
                reduzido.add(resultado.get((int) Math.round(i * passo)));
            }
            resultado = reduzido;
        }
        return resultado;
    }
}
