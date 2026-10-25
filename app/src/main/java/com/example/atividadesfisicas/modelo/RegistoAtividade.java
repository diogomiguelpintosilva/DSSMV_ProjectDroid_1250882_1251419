package com.example.atividadesfisicas.modelo;

import com.example.atividadesfisicas.utilitarios.Formatacao;
import com.example.atividadesfisicas.utilitarios.SimplificarPercurso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Resumo de uma atividade já terminada, guardado no histórico (local e na cloud).
 * Não inclui as fotos/vídeos (ficam no telemóvel onde foram tiradas).
 */
public class RegistoAtividade {

    private final String id;
    private final String nomeRota;
    private final String modo;
    private final long inicio;
    private final long tempoMs;
    private final double distanciaMetros;
    private final double velocidadeMediaKmh;
    private final int numAvisos;
    private final int numFotos;
    private final int numVideos;
    private final List<PontoGPS> percurso;

    public RegistoAtividade(String id, String nomeRota, String modo, long inicio, long tempoMs,
                            double distanciaMetros, double velocidadeMediaKmh, int numAvisos,
                            int numFotos, int numVideos, List<PontoGPS> percurso) {
        this.id = id;
        this.nomeRota = nomeRota;
        this.modo = modo;
        this.inicio = inicio;
        this.tempoMs = tempoMs;
        this.distanciaMetros = distanciaMetros;
        this.velocidadeMediaKmh = velocidadeMediaKmh;
        this.numAvisos = numAvisos;
        this.numFotos = numFotos;
        this.numVideos = numVideos;
        this.percurso = percurso;
    }

    /** Cria o registo a partir de uma atividade terminada. */
    public static RegistoAtividade de(Atividade a) {
        int fotos = 0;
        int videos = 0;
        for (Multimedia m : a.getMultimedia()) {
            if (m instanceof Foto) {
                fotos++;
            } else if (m instanceof Video) {
                videos++;
            }
        }
        return new RegistoAtividade("atv_" + a.getDataHoraInicio(), a.getRota().getNome(),
                a.getRota().getModo().name(), a.getDataHoraInicio(), a.getTempoDecorrido(),
                a.getDistanciaTotal(), a.getVelocidadeMedia(), a.getAvisos().size(), fotos, videos,
                SimplificarPercurso.simplificar(a.getPercurso(), 5, 600));
    }

    public String getId() {
        return id;
    }

    public String getNomeRota() {
        return nomeRota;
    }

    public String getModo() {
        return modo;
    }

    public long getInicio() {
        return inicio;
    }

    public long getTempoMs() {
        return tempoMs;
    }

    public double getDistanciaMetros() {
        return distanciaMetros;
    }

    public double getVelocidadeMediaKmh() {
        return velocidadeMediaKmh;
    }

    public int getNumAvisos() {
        return numAvisos;
    }

    public int getNumFotos() {
        return numFotos;
    }

    public int getNumVideos() {
        return numVideos;
    }

    public List<PontoGPS> getPercurso() {
        return percurso;
    }

    public String getDataFormatada() {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(inicio));
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("nomeRota", nomeRota);
        obj.put("modo", modo);
        obj.put("inicio", inicio);
        obj.put("tempoMs", tempoMs);
        obj.put("distanciaM", distanciaMetros);
        obj.put("velMedia", velocidadeMediaKmh);
        obj.put("avisos", numAvisos);
        obj.put("fotos", numFotos);
        obj.put("videos", numVideos);
        JSONArray arr = new JSONArray();
        for (PontoGPS p : percurso) {
            arr.put(p.toJson());
        }
        obj.put("percurso", arr);
        return obj;
    }

    public static RegistoAtividade fromJson(JSONObject obj) throws JSONException {
        List<PontoGPS> pontos = new ArrayList<>();
        JSONArray arr = obj.optJSONArray("percurso");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                pontos.add(PontoGPS.fromJson(arr.getJSONObject(i)));
            }
        }
        return new RegistoAtividade(obj.getString("id"), obj.optString("nomeRota", ""),
                obj.optString("modo", ""), obj.optLong("inicio", 0), obj.optLong("tempoMs", 0),
                obj.optDouble("distanciaM", 0), obj.optDouble("velMedia", 0), obj.optInt("avisos", 0),
                obj.optInt("fotos", 0), obj.optInt("videos", 0), pontos);
    }

    /** Texto mostrado na lista do histórico. */
    @Override
    public String toString() {
        return getDataFormatada() + "\n" + nomeRota + " · " + Formatacao.formatarDistancia(distanciaMetros)
                + " · " + Formatacao.formatarTempo(tempoMs);
    }
}
