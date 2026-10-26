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

/**
 * UC1 e UC4: as rotas criadas pelo utilizador. Ficam no telemóvel (SharedPreferences, em JSON),
 * separadas por conta, e são sincronizadas com o Firestore para estarem disponíveis
 * noutro dispositivo com a mesma conta.
 */
public final class RotasGuardadas {

    private static final String PREFS = "rotas";
    private static final String CHAVE_CRIADAS = "criadas";
    private static final String COLECAO = "rotas";

    private RotasGuardadas() {
    }

    /** As rotas predefinidas (vêm do CatalogoRotas). */
    public static List<Rota> listarPredefinidas() {
        return CatalogoRotas.obter();
    }

    /** O ficheiro de preferências desta conta (rotas_<uid>). */
    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS + "_" + Sessao.getInstance().getUid(), Context.MODE_PRIVATE);
    }

    /** As rotas criadas pelo utilizador, guardadas no telemóvel. */
    public static synchronized List<Rota> listarCriadas(Context ctx) {
        List<Rota> lista = new ArrayList<>();
        String json = prefs(ctx).getString(CHAVE_CRIADAS, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                lista.add(Rota.fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return lista;
    }

    private static synchronized void gravarLocal(Context ctx, List<Rota> lista) {
        JSONArray arr = new JSONArray();
        try {
            for (Rota r : lista) {
                arr.put(r.toJson());
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        prefs(ctx).edit().putString(CHAVE_CRIADAS, arr.toString()).apply();
    }

    /** Guarda a rota no telemóvel e envia-a para a cloud em segundo plano. */
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

    /** Apaga uma rota criada pelo utilizador, no telemóvel e (em segundo plano) na cloud. */
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

    /** Procura uma rota pelo id dentro do modo indicado. */
    public static Rota procurar(Context ctx, Rota.Modo modo, String id) {
        List<Rota> lista = (modo == Rota.Modo.PREDEFINIDA) ? listarPredefinidas() : listarCriadas(ctx);
        for (Rota r : lista) {
            if (r.getId().equals(id)) {
                return r;
            }
        }
        return null;
    }

    /**
     * Junta as rotas da cloud com as do telemóvel (nos dois sentidos).
     * Chamar a partir de uma thread de fundo.
     */
    public static void sincronizar(Context ctx) throws IOException {
        List<String> remotos = BaseDadosCloud.listar(COLECAO);
        Set<String> idsRemotos = new HashSet<>();
        List<Rota> paraEnviar = new ArrayList<>();

        synchronized (RotasGuardadas.class) {
            List<Rota> locais = listarCriadas(ctx);
            Set<String> idsLocais = new HashSet<>();
            for (Rota r : locais) {
                idsLocais.add(r.getId());
            }
            boolean alterou = false;
            for (String json : remotos) {
                try {
                    Rota r = Rota.fromJson(new JSONObject(json));
                    idsRemotos.add(r.getId());
                    if (!idsLocais.contains(r.getId())) {
                        locais.add(r);
                        idsLocais.add(r.getId());
                        alterou = true;
                    }
                } catch (JSONException e) {
                    // documento inválido: ignora
                }
            }
            if (alterou) {
                gravarLocal(ctx, locais);
            }
            for (Rota r : locais) {
                if (!idsRemotos.contains(r.getId())) {
                    paraEnviar.add(r);
                }
            }
        }

        for (Rota r : paraEnviar) {
            try {
                BaseDadosCloud.guardar(COLECAO, r.getId(), r.toJson().toString(), System.currentTimeMillis());
            } catch (JSONException e) {
                // ignora esta rota
            }
        }
    }
}
