package com.example.atividadesfisicas.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de domínio: atividade (corrida/caminhada).
 * Atributos do modelo: tipo, dataHoraInicio, dataHoraFim, tempoDecorrido, velocidadeMedia, distanciaTotal.
 */
public class Atividade {

    private final String tipo;
    private final long dataHoraInicio;
    private long dataHoraFim;
    private long tempoDecorrido; // milissegundos
    private double velocidadeMedia; // km/h
    private double distanciaTotal; // metros
    private final Rota rota;
    private final List<PontoGPS> percurso = new ArrayList<>();
    private final List<Telemetria> telemetria = new ArrayList<>();
    private final List<AvisoDesvio> avisos = new ArrayList<>();
    private final List<LembreteMultimedia> lembretes = new ArrayList<>();
    private final List<Multimedia> multimedia = new ArrayList<>();
    private VideoResumo videoResumo;

    public Atividade(String tipo, long dataHoraInicio, Rota rota) {
        this.tipo = tipo;
        this.dataHoraInicio = dataHoraInicio;
        this.rota = rota;
    }

    public String getTipo() {
        return tipo;
    }

    public long getDataHoraInicio() {
        return dataHoraInicio;
    }

    public long getDataHoraFim() {
        return dataHoraFim;
    }

    public void setDataHoraFim(long dataHoraFim) {
        this.dataHoraFim = dataHoraFim;
    }

    public long getTempoDecorrido() {
        return tempoDecorrido;
    }

    public void setTempoDecorrido(long tempoDecorrido) {
        this.tempoDecorrido = tempoDecorrido;
    }

    public double getVelocidadeMedia() {
        return velocidadeMedia;
    }

    public void setVelocidadeMedia(double velocidadeMedia) {
        this.velocidadeMedia = velocidadeMedia;
    }

    public double getDistanciaTotal() {
        return distanciaTotal;
    }

    public void setDistanciaTotal(double distanciaTotal) {
        this.distanciaTotal = distanciaTotal;
    }

    public Rota getRota() {
        return rota;
    }

    public List<PontoGPS> getPercurso() {
        return percurso;
    }

    public List<Telemetria> getTelemetria() {
        return telemetria;
    }

    public List<AvisoDesvio> getAvisos() {
        return avisos;
    }

    public List<LembreteMultimedia> getLembretes() {
        return lembretes;
    }

    public List<Multimedia> getMultimedia() {
        return multimedia;
    }

    public VideoResumo getVideoResumo() {
        return videoResumo;
    }

    public void setVideoResumo(VideoResumo videoResumo) {
        this.videoResumo = videoResumo;
    }
}