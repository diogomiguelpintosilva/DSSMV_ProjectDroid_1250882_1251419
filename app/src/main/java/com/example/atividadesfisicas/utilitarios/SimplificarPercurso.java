package com.example.atividadesfisicas.utilitarios;

import android.location.Location;

import com.example.atividadesfisicas.modelo.PontoGPS;

import java.util.ArrayList;
import java.util.List;

/** Funções auxiliares sobre listas de pontos GPS. */
public final class SimplificarPercurso {

    private SimplificarPercurso() {
    }

    /**
     * Reduz o número de pontos de um percurso: mantém só pontos afastados pelo menos
     * distanciaMinimaMetros do anterior (mais o último) e limita o total a maximoPontos.
     * Serve para guardar rotas e histórico sem ocupar demasiado espaço.
     */
    public static List<PontoGPS> simplificar(List<PontoGPS> pontos, double distanciaMinimaMetros,
                                             int maximoPontos) {
        List<PontoGPS> resultado = new ArrayList<>();
        if (pontos == null || pontos.isEmpty()) {
            return resultado;
        }
        float[] d = new float[1];
        PontoGPS ultimo = pontos.get(0);
        resultado.add(ultimo);
        for (PontoGPS p : pontos) {
            Location.distanceBetween(ultimo.getLatitude(), ultimo.getLongitude(),
                    p.getLatitude(), p.getLongitude(), d);
            if (d[0] >= distanciaMinimaMetros) {
                resultado.add(p);
                ultimo = p;
            }
        }
        PontoGPS fim = pontos.get(pontos.size() - 1);
        if (resultado.get(resultado.size() - 1) != fim) {
            resultado.add(fim);
        }
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
