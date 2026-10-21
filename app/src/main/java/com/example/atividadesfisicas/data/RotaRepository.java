package com.example.atividadesfisicas.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.atividadesfisicas.model.Rota;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Guarda e lê as rotas criadas previamente em SharedPreferences, em formato JSON.
 */
public final class RotaRepository {

    private static final String PREFS = "rotas";
    private static final String CHAVE_CRIADAS = "criadas";

    private RotaRepository() {
    }

    public static List<Rota> listarPredefinidas() {
        return RotasPredefinidas.obter();
    }

    public static List<Rota> listarCriadas(Context ctx) {
        List<Rota> lista = new ArrayList<>();
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = prefs.getString(CHAVE_CRIADAS, "[]");
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

    public static void guardarCriada(Context ctx, Rota rota) {
        List<Rota> lista = listarCriadas(ctx);
        lista.add(rota);
        JSONArray arr = new JSONArray();
        try {
            for (Rota r : lista) {
                arr.put(r.toJson());
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(CHAVE_CRIADAS, arr.toString()).apply();
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
}