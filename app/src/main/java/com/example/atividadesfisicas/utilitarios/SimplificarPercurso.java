package com.example.atividadesfisicas.utilitarios;

import android.location.Location;

import com.example.atividadesfisicas.modelo.PontoGPS;

import java.util.ArrayList;
import java.util.List;

/*
 Reduz o número de pontos de um percurso antes de o guardar (histórico e rotas guardadas).
 */
public final class SimplificarPercurso {

    private SimplificarPercurso() {
    }

    /*
     Mantém só pontos afastados pelo menos distanciaMinimaMetros do último ponto mantido
     (mais o último ponto do percurso) e limita o total a maximoPontos.
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
        resultado.add(ultimo); //p é o ponto atual enquanto o último é o naterior, para poder comparar distancias
        for (PontoGPS p : pontos) {
            Location.distanceBetween(ultimo.getLatitude(), ultimo.getLongitude(),
                    p.getLatitude(), p.getLongitude(), distancia); //vai medir a distância entre ultimo e p, pondo o resultado no vetor distancia
            if (distancia[0] >= distanciaMinimaMetros) {
                resultado.add(p);
                ultimo = p;
            }
        }
        PontoGPS fim = pontos.get(pontos.size() - 1);//ponto gps final
        if (resultado.get(resultado.size() - 1) != fim) { //compara o ultimo ponto efetivo, com o registado pelo código
            resultado.add(fim); //garante q o ultimo ponto é o final
        }

        // 2.º filtro: número máximo de pontos, escolhidos igualmente espaçados
        if (maximoPontos >= 2 && resultado.size() > maximoPontos) { //ter a certeza que é um lista que precisa de ser simplificado
            List<PontoGPS> reduzido = new ArrayList<>();//lista com menos pontos, mais distribuídos
            double passo = (resultado.size() - 1) / (double) (maximoPontos - 1);// intervalos totais/intervalos entre os pontos
            for (int i = 0; i < maximoPontos; i++) {  //double obriga a divisão a ter casas decimais
                reduzido.add(resultado.get((int) Math.round(i * passo)));
            }
            resultado = reduzido;
        }
        return resultado;
        /*
        basicamente cria-se uma nova lista, que vai ser a rota com menos pontos.
        determina-se o passo a que se vai considerar os pontos, consoante a sua posição na lista de pontos.
        vai se adicionando pontos à lista, multiplicando o indice do ciclo for pelo passo(arredondando-se para valor inteiro com math.round)
         */
    }
}
