package com.example.atividadesfisicas.dados;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.atividadesfisicas.autenticacao.Sessao;
import com.example.atividadesfisicas.modelo.Atividade;
import com.example.atividadesfisicas.modelo.RegistoAtividade;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Histórico de atividades do utilizador. Fica em SharedPreferences (separado por conta) e é sincronizado
 * com o Firestore, tal como as rotas criadas.
 */
public final class HistoricoGuardado {

    private static final String PREFS = "historico";
    private static final String CHAVE = "atividades";
    private static final String COLECAO = "atividades";

    private HistoricoGuardado() {
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS + "_" + Sessao.getInstance().getUid(), Context.MODE_PRIVATE);
    }

    /** Lista as atividades, da mais recente para a mais antiga. */
    public static synchronized List<RegistoAtividade> listar(Context ctx) {
        List<RegistoAtividade> lista = new ArrayList<>();
        String json = prefs(ctx).getString(CHAVE, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                lista.add(RegistoAtividade.fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Collections.sort(lista, new Comparator<RegistoAtividade>() {
            @Override
            public int compare(RegistoAtividade a, RegistoAtividade b) {
                return Long.compare(b.getInicio(), a.getInicio());
            }
        });
        return lista;
    }

    private static synchronized void gravarLocal(Context ctx, List<RegistoAtividade> lista) {
        JSONArray arr = new JSONArray();
        try {
            for (RegistoAtividade r : lista) {
                arr.put(r.toJson());
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        prefs(ctx).edit().putString(CHAVE, arr.toString()).apply();
    }

    public static synchronized RegistoAtividade procurar(Context ctx, String id) {
        for (RegistoAtividade r : listar(ctx)) {
            if (r.getId().equals(id)) {
                return r;
            }
        }
        return null;
    }

    /** Guarda a atividade terminada no histórico e envia-a para a cloud em segundo plano. */
    public static synchronized void guardar(Context ctx, Atividade atividade) {
        if (atividade == null) {
            return;
        }
        final RegistoAtividade registo = RegistoAtividade.de(atividade);
        List<RegistoAtividade> lista = listar(ctx);
        boolean existe = false;
        for (RegistoAtividade r : lista) {
            if (r.getId().equals(registo.getId())) {
                existe = true;
                break;
            }
        }
        if (existe) {
            return;
        }
        lista.add(registo);
        gravarLocal(ctx, lista);
        new Thread(() -> {
            try {
                BaseDadosCloud.guardar(COLECAO, registo.getId(), registo.toJson().toString(), registo.getInicio());
            } catch (IOException | JSONException e) {
                // sem rede ou sem sessão: fica só local e é reenviado na próxima sincronização
            }
        }).start();
    }

    /**
     * Junta o histórico da cloud com o local (nos dois sentidos).
     * Chamar a partir de uma worker thread.
     */
    public static void sincronizar(Context ctx) throws IOException {
        List<String> remotos = BaseDadosCloud.listar(COLECAO);
        Set<String> idsRemotos = new HashSet<>();
        List<RegistoAtividade> paraEnviar = new ArrayList<>();

        synchronized (HistoricoGuardado.class) {
            List<RegistoAtividade> locais = listar(ctx);
            Set<String> idsLocais = new HashSet<>();
            for (RegistoAtividade r : locais) {
                idsLocais.add(r.getId());
            }
            boolean alterou = false;
            for (String json : remotos) {
                try {
                    RegistoAtividade r = RegistoAtividade.fromJson(new JSONObject(json));
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
            for (RegistoAtividade r : locais) {
                if (!idsRemotos.contains(r.getId())) {
                    paraEnviar.add(r);
                }
            }
        }

        for (RegistoAtividade r : paraEnviar) {
            try {
                BaseDadosCloud.guardar(COLECAO, r.getId(), r.toJson().toString(), r.getInicio());
            } catch (JSONException e) {
                // ignora este registo
            }
        }
    }
}
