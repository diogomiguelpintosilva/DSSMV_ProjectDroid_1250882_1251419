package com.example.atividadesfisicas.data;

import android.location.Location;
import android.os.SystemClock;

import com.example.atividadesfisicas.model.Atividade;
import com.example.atividadesfisicas.model.AvisoDesvio;
import com.example.atividadesfisicas.model.LembreteMultimedia;
import com.example.atividadesfisicas.model.Multimedia;
import com.example.atividadesfisicas.model.PontoGPS;
import com.example.atividadesfisicas.model.Rota;
import com.example.atividadesfisicas.model.Telemetria;
import com.example.atividadesfisicas.model.VideoResumo;
import com.example.atividadesfisicas.util.Config;

import java.util.ArrayList;
import java.util.List;

/**
 * Guarda a atividade em curso. É partilhado entre o serviço de GPS (que escreve) e os ecrãs (que lêem).
 * Todos os métodos são synchronized porque há várias threads a usá-lo.
 */
public final class AtividadeManager {

    private static final AtividadeManager INSTANCE = new AtividadeManager();

    private Atividade atual;
    private boolean ativa = false;
    private long inicioElapsed;
    private PontoGPS ultimoPonto;
    private Telemetria ultimaTelemetria;
    private AvisoDesvio ultimoAviso;
    private LembreteMultimedia ultimoLembrete;

    private AtividadeManager() {
    }

    public static AtividadeManager getInstance() {
        return INSTANCE;
    }

    /** Iniciar atividade. */
    public synchronized void iniciar(Rota rota) {
        atual = new Atividade(Config.TIPO_ATIVIDADE, System.currentTimeMillis(), rota);
        inicioElapsed = SystemClock.elapsedRealtime();
        ultimoPonto = null;
        ultimaTelemetria = null;
        ultimoAviso = null;
        ultimoLembrete = null;
        ativa = true;
    }

    public synchronized boolean isAtiva() {
        return ativa;
    }

    public synchronized Atividade getAtual() {
        return atual;
    }

    public synchronized long getTempoDecorridoMs() {
        if (atual == null) {
            return 0;
        }
        return ativa ? SystemClock.elapsedRealtime() - inicioElapsed : atual.getTempoDecorrido();
    }

    /** Chamado pelo serviço de GPS a cada nova localização. Calcula velocidade, distância e altitude. */
    public synchronized void registarLocalizacao(Location loc) {
        if (!ativa || loc == null) {
            return;
        }
        if (loc.hasAccuracy() && loc.getAccuracy() > Config.PRECISAO_MAXIMA_METROS) {
            return;
        }

        long agora = System.currentTimeMillis();
        double altitude = loc.hasAltitude() ? loc.getAltitude() : 0;
        PontoGPS ponto = new PontoGPS(loc.getLatitude(), loc.getLongitude(), altitude, agora);

        double velocidadeKmh = loc.hasSpeed() ? loc.getSpeed() * 3.6 : 0;

        if (ultimoPonto != null) {
            float[] resultado = new float[1];
            Location.distanceBetween(ultimoPonto.getLatitude(), ultimoPonto.getLongitude(),
                    ponto.getLatitude(), ponto.getLongitude(), resultado);
            float distancia = resultado[0];

            // ignora pequenas oscilações do GPS quando o utilizador está parado
            if (distancia >= 1.5f) {
                atual.setDistanciaTotal(atual.getDistanciaTotal() + distancia);
            }
            if (!loc.hasSpeed()) {
                double dt = (agora - ultimoPonto.getInstante()) / 1000.0;
                velocidadeKmh = dt > 0 ? (distancia / dt) * 3.6 : 0;
            }
        }

        ultimoPonto = ponto;
        atual.getPercurso().add(ponto);

        Telemetria t = new Telemetria(getTempoDecorridoMs(), velocidadeKmh, altitude, agora);
        ultimaTelemetria = t;
        atual.getTelemetria().add(t);
    }

    public synchronized PontoGPS getUltimoPonto() {
        return ultimoPonto;
    }

    public synchronized Telemetria getTelemetriaAtual() {
        return ultimaTelemetria;
    }

    public synchronized List<PontoGPS> copiaPercurso() {
        if (atual == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(atual.getPercurso());
    }

    public synchronized List<Telemetria> copiaTelemetria() {
        if (atual == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(atual.getTelemetria());
    }

    public synchronized void registarAviso(AvisoDesvio aviso) {
        if (atual == null) {
            return;
        }
        atual.getAvisos().add(aviso);
        ultimoAviso = aviso;
    }

    public synchronized AvisoDesvio getUltimoAviso() {
        return ultimoAviso;
    }

    public synchronized void registarLembrete(LembreteMultimedia lembrete) {
        if (atual != null) {
            atual.getLembretes().add(lembrete);
            ultimoLembrete = lembrete;
        }
    }

    public synchronized LembreteMultimedia getUltimoLembrete() {
        return ultimoLembrete;
    }

    public synchronized void adicionarMultimedia(Multimedia multimedia) {
        if (atual != null) {
            atual.getMultimedia().add(multimedia);
        }
    }

    public synchronized void setVideoResumo(VideoResumo videoResumo) {
        if (atual != null) {
            atual.setVideoResumo(videoResumo);
        }
    }

    /** Fixa o fim e calcula tempo total e velocidade média. */
    public synchronized void terminar() {
        if (!ativa || atual == null) {
            return;
        }
        long tempoTotal = SystemClock.elapsedRealtime() - inicioElapsed;
        ativa = false;
        atual.setDataHoraFim(System.currentTimeMillis());
        atual.setTempoDecorrido(tempoTotal);
        if (tempoTotal > 0) {
            double horas = tempoTotal / 3600000.0;
            atual.setVelocidadeMedia((atual.getDistanciaTotal() / 1000.0) / horas);
        } else {
            atual.setVelocidadeMedia(0);
        }
    }
}