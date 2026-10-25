package com.example.atividadesfisicas.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.atividadesfisicas.auth.Sessao;
import com.example.atividadesfisicas.model.Rota;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Guarda e lê as rotas criadas previamente. Ficam em SharedPreferences (em JSON), separadas por conta,
 * e são sincronizadas com o Firestore para estarem disponíveis noutro dispositivo com a mesma conta.
 */
public final class RotaRepository {

    private static final String PREFS = "rotas";
    private static final String CHAVE_CRIADAS = "criadas";
    private static final String CHAVE_APAGADAS = "apagadas";
    private static final String COLECAO = "rotas";

    private RotaRepository() {
    }

    public static List<Rota> listarPredefinidas() {
        return RotasPredefinidas.obter();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS + "_" + Sessao.getInstance().getUid(), Context.MODE_PRIVATE);
    }

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
                FirestoreApi.guardar(COLECAO, rota.getId(), rota.toJson().toString(), System.currentTimeMillis());
            } catch (IOException | JSONException e) {
                // sem rede ou sem sessão: fica só local e é reenviada na próxima sincronização
            }
        }).start();
    }

    // ids das rotas apagadas no telemóvel cujo apagamento na cloud ainda não foi confirmado
    private static synchronized Set<String> pendentesApagar(Context ctx) {
        return new HashSet<>(prefs(ctx).getStringSet(CHAVE_APAGADAS, new HashSet<String>()));
    }

    private static synchronized void removerPendente(Context ctx, String id) {
        Set<String> pendentes = pendentesApagar(ctx);
        if (pendentes.remove(id)) {
            prefs(ctx).edit().putStringSet(CHAVE_APAGADAS, pendentes).apply();
        }
    }

    /** Apaga uma rota criada pelo utilizador (no telemóvel e, em segundo plano, na cloud). */
    public static synchronized void apagarCriada(Context ctx, final String id) {
        List<Rota> lista = listarCriadas(ctx);
        for (Iterator<Rota> it = lista.iterator(); it.hasNext(); ) {
            if (it.next().getId().equals(id)) {
                it.remove();
            }
        }
        gravarLocal(ctx, lista);

        Set<String> pendentes = pendentesApagar(ctx);
        pendentes.add(id);
        prefs(ctx).edit().putStringSet(CHAVE_APAGADAS, pendentes).apply();

        final Context app = ctx.getApplicationContext();
        new Thread(() -> {
            try {
                FirestoreApi.apagar(COLECAO, id);
                removerPendente(app, id);
            } catch (IOException e) {
                // sem rede: fica pendente e é apagada da cloud na próxima sincronização
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
     * Junta as rotas da cloud com as locais (nos dois sentidos).
     * Chamar a partir de uma worker thread.
     */
    public static void sincronizar(Context ctx) throws IOException {
        // primeiro conclui os apagamentos que ficaram por fazer na cloud
        for (String id : pendentesApagar(ctx)) {
            FirestoreApi.apagar(COLECAO, id);
            removerPendente(ctx, id);
        }
        List<String> remotos = FirestoreApi.listar(COLECAO);
        Set<String> idsRemotos = new HashSet<>();
        List<Rota> paraEnviar = new ArrayList<>();

        synchronized (RotaRepository.class) {
            List<Rota> locais = listarCriadas(ctx);
            Set<String> apagadas = pendentesApagar(ctx);
            Set<String> idsLocais = new HashSet<>();
            for (Rota r : locais) {
                idsLocais.add(r.getId());
            }
            boolean alterou = false;
            for (String json : remotos) {
                try {
                    Rota r = Rota.fromJson(new JSONObject(json));
                    idsRemotos.add(r.getId());
                    if (!idsLocais.contains(r.getId()) && !apagadas.contains(r.getId())) {
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
                FirestoreApi.guardar(COLECAO, r.getId(), r.toJson().toString(), System.currentTimeMillis());
            } catch (JSONException e) {
                // ignora esta rota
            }
        }
    }
}
