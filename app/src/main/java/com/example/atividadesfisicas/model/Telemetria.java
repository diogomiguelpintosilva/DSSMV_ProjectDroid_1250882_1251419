package com.example.atividadesfisicas.model;

/**
 * Modelo de domínio: métricas em tempo real (tempo decorrido, velocidade atual e altitude).
 */
public class Telemetria {

    private final long tempoDecorridoMs;
    private final double velocidadeKmh;
    private final double altitude;
    private final long instante;

    public Telemetria(long tempoDecorridoMs, double velocidadeKmh, double altitude, long instante) {
        this.tempoDecorridoMs = tempoDecorridoMs;
        this.velocidadeKmh = velocidadeKmh;
        this.altitude = altitude;
        this.instante = instante;
    }

    public long getTempoDecorridoMs() {
        return tempoDecorridoMs;
    }

    public double getVelocidadeKmh() {
        return velocidadeKmh;
    }

    public double getAltitude() {
        return altitude;
    }

    public long getInstante() {
        return instante;
    }
}