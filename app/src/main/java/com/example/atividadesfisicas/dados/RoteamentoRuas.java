package com.example.atividadesfisicas.dados;

import com.example.atividadesfisicas.modelo.PontoGPS;
import com.example.atividadesfisicas.utilitarios.PedidosHttp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * Transforma uma rota feita de pontos soltos (que, ligados em linha reta, atravessavam casas e prédios)
 * num percurso que segue as ruas. Pede-o por HTTP ao serviço de rotas a pé do OpenStreetMap (OSRM).
 * Tem de ser chamado a partir de uma thread de fundo (faz rede).
 */


public final class RoteamentoRuas{

    /**
     * Servidor público de rotas a pé do OpenStreetMap. O nome do perfil no endereço ("driving") é ignorado:
     * este servidor ("routed-foot") calcula sempre percursos a pé.
     */

    private static final String URL_BASE = "https://routing.openstreetmap.de/routed-foot/route/v1/driving/";


    /** O serviço aceita poucos pontos por pedido; acima disto escolhem-se pontos igualmente espaçados. */
    private static final int MAXIMO_PONTOS_PEDIDO = 25;

    private RoteamentoRuas() {
    }

    /**
     * Devolve o percurso pelas ruas que passa pelos pontos dados, pela ordem em que estão.
     *
     * @throws IOException sem rede, serviço indisponível ou sem caminho possível
     */
    public static List<PontoGPS> porRuas(List<PontoGPS> pontos) throws IOException {
        if (pontos == null) {
            return new ArrayList<>();
        }
        if (pontos.size() < 2) {
            return new ArrayList<>(pontos);
        }

        // o serviço quer "longitude,latitude" (por esta ordem), com ponto decimal, separados por ";"

        List<PontoGPS> chave = escolherPontosChave(pontos);
        StringBuilder coordenadas = new StringBuilder();
        for (int i = 0; i < chave.size(); i++) {
            if (i > 0) {
                coordenadas.append(';');
            }
            coordenadas.append(String.format(Locale.US, "%.6f,%.6f",
                    chave.get(i).getLongitude(), chave.get(i).getLatitude()));
        }

        String endereco = URL_BASE + coordenadas + "?overview=full&geometries=geojson";

        PedidosHttp.Resposta resposta = PedidosHttp.pedido("GET", endereco, null, null, null);
        if (resposta.codigo != 200) {
            throw new IOException("O serviço de rotas devolveu " + resposta.codigo);
        }

        try {
            JSONObject json = new JSONObject(resposta.corpo);
            if (!"Ok".equals(json.optString("code"))) {
                throw new IOException("Sem caminho possível: " + json.optString("code"));
            }
            // a geometria é uma lista de pares [longitude, latitude]
            JSONArray pares = json.getJSONArray("routes").getJSONObject(0)
                    .getJSONObject("geometry").getJSONArray("coordinates");
            List<PontoGPS> resultado = new ArrayList<>();
            for (int i = 0; i < pares.length(); i++) {
                JSONArray par = pares.getJSONArray(i);
                resultado.add(new PontoGPS(par.getDouble(1), par.getDouble(0), 0, 0));
            }
            if (resultado.size() < 2) {
                throw new IOException("O serviço de rotas devolveu um percurso vazio");
            }
            return resultado;
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }


    /** Se houver mais pontos do que o serviço aceita, escolhe pontos igualmente espaçados (início e fim incluídos). */
    private static List<PontoGPS> escolherPontosChave(List<PontoGPS> pontos) {
        if (pontos.size() <= MAXIMO_PONTOS_PEDIDO) {
            return pontos;
        }
        List<PontoGPS> escolhidos = new ArrayList<>();
        double passo = (pontos.size() - 1) / (double) (MAXIMO_PONTOS_PEDIDO - 1);
        for (int i = 0; i < MAXIMO_PONTOS_PEDIDO; i++) {
            escolhidos.add(pontos.get((int) Math.round(i * passo)));
        }
        return escolhidos;
    }
}