package com.example.atividadesfisicas.dados;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.atividadesfisicas.autenticacao.Sessao;
import com.example.atividadesfisicas.modelo.Rota;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 as rotas criadas pelo utilizador. Ficam no telemóvel
 separadas por conta, e são sincronizadas com o Firestore para estarem disponíveis
 noutro dispositivo com a mesma conta.
 */
public final class RotasGuardadas {

    private static final String PREFS = "rotas"; //nome do ficheiro no dispositivo
    private static final String CHAVE_CRIADAS = "criadas"; //etiqueta com q as rotas ficam guardadas dentro do ficheiro
    private static final String COLECAO = "rotas"; //nome da colecao no Firebase

    private RotasGuardadas() {
    }

    // devolve as rotas predefinidas
    public static List<Rota> listarPredefinidas() {
        return CatalogoRotas.obter();
    }

    // O ficheiro de preferências desta conta
    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS + "_" + Sessao.getInstance().getUid(), Context.MODE_PRIVATE); //junta rotas com Uid e abre o ficheiro de prefs
    } //é preciso o uid para cada utilizador ter a sua pasta, e não todos os utilizadores a mesma pasta

    // As rotas criadas pelo utilizador, guardadas no telemóvel.
    public static synchronized List<Rota> listarCriadas(Context ctx) {
        List<Rota> lista = new ArrayList<>();
        String json = prefs(ctx).getString(CHAVE_CRIADAS, "[]");// abre ficheiro com rotas guardadas e pede rotas que lá estão par a variavel json
        try {                                                              // "[]" é ovalor de reserva se não houver nada com a etiqueta
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                lista.add(Rota.fromJson(arr.getJSONObject(i))); //transforma cada rota em json, que fica num array, que vai conter todas as rotas.
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return lista;
    }

    //Pega na lista de rotas e guarda no telemóvel
    private static synchronized void gravarLocal(Context ctx, List<Rota> lista) {
        JSONArray arr = new JSONArray();
        try {
            for (Rota r : lista) {
                arr.put(r.toJson()); //pega em cada rota na lista, converte para json e junta aoarray
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        prefs(ctx).edit().putString(CHAVE_CRIADAS, arr.toString()).apply(); //converte array em texto e guarda no ficheiro, reescrevendo oq lá está
    }

    // Guarda a rota no telemóvel e envia-a para a cloud em segundo plano.
    public static synchronized void guardarCriada(Context ctx, final Rota rota) {
        List<Rota> lista = listarCriadas(ctx);
        lista.add(rota);
        gravarLocal(ctx, lista);
        new Thread(() -> {
            try {
                BaseDadosCloud.guardar(COLECAO, rota.getId(), rota.toJson().toString(), System.currentTimeMillis());
            } catch (IOException | JSONException e) {
                // sem rede ou sem sessão: fica só local e é reenviada na próxima sincronização
            }
        }).start();
    }

    // Apaga uma rota criada pelo utilizador, no telemóvel e (em segundo plano) na cloud.
    public static synchronized void apagarCriada(Context ctx, final String id) {
        List<Rota> lista = listarCriadas(ctx);
        lista.removeIf(rota -> rota.getId().equals(id));
        gravarLocal(ctx, lista);
        new Thread(() -> {
            try {
                BaseDadosCloud.apagar(COLECAO, id);
            } catch (IOException e) {
                // sem rede: a rota continua na cloud e volta a aparecer na próxima sincronização
            }
        }).start();
    }

    // Procura uma rota pelo id dentro do modo indicado.
    public static Rota procurar(Context ctx, Rota.Modo modo, String id) {
        List<Rota> lista = (modo == Rota.Modo.PREDEFINIDA) ? listarPredefinidas() : listarCriadas(ctx); //se for predefinida, usar lista de predef, senão usar as criadas
        for (Rota r : lista) {
            if (r.getId().equals(id)) {
                return r;
            }
        }
        return null;
    }

    /*
     Junta as rotas da cloud com as do telemóvel (nos dois sentidos).
     Chamar a partir de uma thread de fundo.
     */
    public static void sincronizar(Context ctx) throws IOException {
        List<String> remotos = BaseDadosCloud.listar(COLECAO); //json de todas as rotas na cloud
        Set<String> idsRemotos = new HashSet<>(); //onde guardamos os ids q já estão na cloud (Set pq lá não se repetem ids)
        List<Rota> paraEnviar = new ArrayList<>(); //lista de rotas q s´estão no dispositivo e têm q ir para a cloud

        synchronized (RotasGuardadas.class) {
            List<Rota> locais = listarCriadas(ctx); //rotas do dispositivo
            Set<String> idsLocais = new HashSet<>(); //ids das rotas
            for (Rota r : locais) {
                idsLocais.add(r.getId());
            }
            boolean alterou = false;
            for (String json : remotos) { //vai ver json de todas as rotas
                try {
                    Rota r = Rota.fromJson(new JSONObject(json)); //converte de json para rota
                    idsRemotos.add(r.getId()); //vê id da rota e adiciona ao Set
                    if (!idsLocais.contains(r.getId())) { //se n estiver no Set já
                        locais.add(r);
                        idsLocais.add(r.getId());
                        alterou = true;
                    }
                } catch (JSONException e) {
                    // documento inválido: ignora
                }
            }
            //se mudou alguma coisa
            if (alterou) {
                gravarLocal(ctx, locais);
            }
            for (Rota r : locais) { //vai ver a lista de rotas que estaavam no dispositivo, e se não estão no Set, vão para o array a mandar do dispositivo para a cloud.
                if (!idsRemotos.contains(r.getId())) {
                    paraEnviar.add(r);
                }
            }
        }

        for (Rota r : paraEnviar) { //adicionar à cloud no array das rotas a mandar do dispoitivo para cloud
            try {
                BaseDadosCloud.guardar(COLECAO, r.getId(), r.toJson().toString(), System.currentTimeMillis());
            } catch (JSONException e) {
                // ignora esta rota
            }
        }
    }
}
