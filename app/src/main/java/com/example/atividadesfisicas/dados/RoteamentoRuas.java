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


/*
 Transforma uma rota feita de pontos soltos (que, ligados em linha reta, atravessavam casas e prédios)
 num percurso que segue as ruas. Pede-o por HTTP ao serviço de rotas a pé do OpenStreetMap (OSRM).
 Tem de ser chamado a partir de uma thread de fundo (faz rede).
 */


public final class RoteamentoRuas{

    /*
     Servidor público de rotas a pé do OpenStreetMap. O nome do perfil no endereço ("driving") é ignorado:
     este servidor ("routed-foot") calcula sempre percursos a pé.
     */
    //endereço do serviço de rotas a pé
    private static final String URL_BASE = "https://routing.openstreetmap.de/routed-foot/route/v1/driving/";


    // O serviço aceita poucos pontos por pedido; acima disto escolhem-se pontos igualmente espaçados.
    private static final int MAXIMO_PONTOS_PEDIDO = 25;

    private RoteamentoRuas() {
    }

    /*
     Devolve o percurso pelas ruas que passa pelos pontos dados, pela ordem em que estão.
     IOException se utilizador está sem rede, serviço indisponível ou sem caminho possível
     */
    public static List<PontoGPS> porRuas(List<PontoGPS> pontos) throws IOException {
        if (pontos == null) { //não há pontos
            return new ArrayList<>();
        }
        if (pontos.size() < 2) { //não há camiho(origem e destino)
            return new ArrayList<>(pontos);
        }

        // o serviço quer "longitude,latitude" (por esta ordem), com ponto decimal, separados por ";"
        List<PontoGPS> chave = escolherPontosChave(pontos); //escolher pontos essenciais, se houver menos de 25, então usa todos os pontos disponíveis

        //este bloco cria um único texto com todas as coordenadas, no formato pedido pelo serviço de rotas
        StringBuilder coordenadas = new StringBuilder(); //texto onde se vai acrescentando partes com append
        for (int i = 0; i < chave.size(); i++) {
            if (i > 0) {
                coordenadas.append(';');
            }
            coordenadas.append(String.format(Locale.US, "%.6f,%.6f", //usamos Locale.US pois o serviço de rotas só aceta assim. É a regra de escrita de números a usar
                    chave.get(i).getLongitude(), chave.get(i).getLatitude()));
        }

        //montar endereço de pedido de caminho que temos de ir, pelas ruas.
        String endereco = URL_BASE + coordenadas + "?overview=full&geometries=geojson";
        //url + coordenadas + "?overview=full" dá caminho completo, com todos os pontos + devolve num formato fácil de ler
        PedidosHttp.Resposta resposta = PedidosHttp.pedido("GET", endereco, null, null, null);
        if (resposta.codigo != 200) {
            throw new IOException("O serviço de rotas devolveu " + resposta.codigo);
        }

        try {
            JSONObject json = new JSONObject(resposta.corpo); //transformar resposta(corpo= do servidor em json
            if (!"Ok".equals(json.optString("code"))) { //vai ver se de facto há rota "ok" = deu, "NoRoute" = não há caminho. "code" é oq tem o estado do cálculo.
                throw new IOException("Sem caminho possível: " + json.optString("code"));
            }
            // a geometria é uma lista de pares [longitude, latitude]
            JSONArray pares = json.getJSONArray("routes").getJSONObject(0)
                    .getJSONObject("geometry").getJSONArray("coordinates"); //desce pelos arr até chegar ao mais básico, as coordenadas(forma longitude,latitude)
            List<PontoGPS> resultado = new ArrayList<>(); //arr que vai ter pontos gps, com as coordenadas.
            for (int i = 0; i < pares.length(); i++) {
                JSONArray par = pares.getJSONArray(i); //transformar em json
                resultado.add(new PontoGPS(par.getDouble(1), par.getDouble(0), 0, 0));// criar ponto gps com latitude,longitude,... (resto a 0)
            }
            if (resultado.size() < 2) {
                throw new IOException("O serviço de rotas devolveu um percurso vazio");
            }
            return resultado; //devolve percurso
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }


    //Se houver mais pontos do que o serviço aceita, escolhe pontos igualmente espaçados (início e fim incluídos).
    private static List<PontoGPS> escolherPontosChave(List<PontoGPS> pontos) {
        if (pontos.size() <= MAXIMO_PONTOS_PEDIDO) {
            return pontos;
        }
        List<PontoGPS> escolhidos = new ArrayList<>();
        //usa-se 24 e -1 para o último ponto ser sempre escolhido
        double passo = (pontos.size() - 1) / (double) (MAXIMO_PONTOS_PEDIDO - 1); //double converte para decimal, divisão não arredondada
        for (int i = 0; i < MAXIMO_PONTOS_PEDIDO; i++) {
            escolhidos.add(pontos.get((int) Math.round(i * passo)));
        }
        return escolhidos;
    }
}