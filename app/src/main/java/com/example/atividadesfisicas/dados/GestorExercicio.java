package com.example.atividadesfisicas.dados;

import android.location.Location;
import android.os.SystemClock;

import com.example.atividadesfisicas.modelo.Atividade;
import com.example.atividadesfisicas.modelo.AvisoDesvio;
import com.example.atividadesfisicas.modelo.LembreteMultimedia;
import com.example.atividadesfisicas.modelo.Multimedia;
import com.example.atividadesfisicas.modelo.PontoGPS;
import com.example.atividadesfisicas.modelo.Rota;
import com.example.atividadesfisicas.modelo.Telemetria;
import com.example.atividadesfisicas.modelo.VideoResumo;
import com.example.atividadesfisicas.utilitarios.Config;

import java.util.ArrayList;
import java.util.List;

/**
 * Guarda o exercício (atividade) em curso. É partilhado entre o serviço de GPS, que escreve,
 * e os ecrãs, que leem. Calcula a distância, a velocidade e a velocidade média.
 * Todos os métodos são synchronized porque há várias threads a usá-lo.
 */
public final class GestorExercicio {

    private static final GestorExercicio INSTANCIA = new GestorExercicio();

    private Atividade atual;
    private boolean ativa = false;
    private long inicioElapsed;
    private PontoGPS ultimoPonto;
    private Telemetria ultimaTelemetria;
    private AvisoDesvio ultimoAviso;
    private LembreteMultimedia ultimoLembrete;

    private GestorExercicio() {
    }

    public static GestorExercicio getInstance() {
        return INSTANCIA;
    }

    /** UC2: começa um exercício novo na rota indicada. */
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

    /** Tempo decorrido: calculado agora se o exercício está ativo, ou o valor final se já terminou. */
    public synchronized long getTempoDecorridoMs() {
        if (atual == null) {
            return 0;
        }
        return ativa ? SystemClock.elapsedRealtime() - inicioElapsed : atual.getTempoDecorrido();
    }

    /** Chamado pelo serviço de GPS a cada nova posição. Calcula a velocidade, a distância e a telemetria. */
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

        // o GPS dá a velocidade em m/s; × 3,6 converte para km/h
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

    /** Cópia do percurso (para quem lê não ser afetado pelos pontos que o serviço vai acrescentando). */
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

    /** UC9: fixa o fim e calcula o tempo total e a velocidade média (km ÷ horas). */
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