package com.example.atividadesfisicas.modelo;

//CLASSE que trata dos dados recolhidos durante a atividade(tempo,velociade,altitude)
public class Telemetria {
    //ATRIBUTOS
    private final long tempoDecorridoMs;
    private final double velocidadeKmh;
    private final double altitude;
    private final long instante;

    //CONSTRUTOR
    public Telemetria(long tempoDecorridoMs, double velocidadeKmh, double altitude, long instante) {
        this.tempoDecorridoMs = tempoDecorridoMs;
        this.velocidadeKmh = velocidadeKmh;
        this.altitude = altitude;
        this.instante = instante;
    }

    //GETTERS
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