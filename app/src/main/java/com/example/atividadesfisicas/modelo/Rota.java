package com.example.atividadesfisicas.modelo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de domínio: rota (predefinida, criada previamente ou livre) composta por pontos GPS.
 */
public class Rota {

    public enum Modo { PREDEFINIDA, CRIADA, LIVRE }

    private final String id;
    private final String nome;
    private final Modo modo;
    private final List<PontoGPS> pontos = new ArrayList<>();

    public Rota(String id, String nome, Modo modo) {
        this.id = id;
        this.nome = nome;
        this.modo = modo;
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Modo getModo() {
        return modo;
    }

    public List<PontoGPS> getPontos() {
        return pontos;
    }

    public void adicionarPonto(PontoGPS ponto) {
        pontos.add(ponto);
    }

    /**
     * Cria uma rota do modo CRIADA (planeada pelo utilizador ou guardada a partir do modo livre)
     * com um id único. Os pontos ficam sem instante, porque uma rota não tem horas.
     */
    public static Rota criada(String nome, List<PontoGPS> pontos) {
        Rota rota = new Rota("criada_" + System.currentTimeMillis(), nome, Modo.CRIADA);
        for (PontoGPS p : pontos) {
            rota.adicionarPonto(new PontoGPS(p.getLatitude(), p.getLongitude(), p.getAltitude(), 0));
        }
        return rota;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("nome", nome);
        obj.put("modo", modo.name());
        JSONArray arr = new JSONArray();
        for (PontoGPS p : pontos) {
            arr.put(p.toJson());
        }
        obj.put("pontos", arr);
        return obj;
    }

    public static Rota fromJson(JSONObject obj) throws JSONException {
        Rota rota = new Rota(
                obj.getString("id"),
                obj.getString("nome"),
                Modo.valueOf(obj.getString("modo"))
        );
        JSONArray arr = obj.getJSONArray("pontos");
        for (int i = 0; i < arr.length(); i++) {
            rota.adicionarPonto(PontoGPS.fromJson(arr.getJSONObject(i)));
        }
        return rota;
    }

    /** Texto de cada linha da lista de rotas (o ArrayAdapter usa o toString). */
    @Override
    public String toString() {
        return nome;
    }
}