package com.example.atividadesfisicas.modelo;

/**
 * Modelo de domínio: ocorrência de desvio da rota planeada.
 */
public class AvisoDesvio {

    private final long instante;
    private final double distanciaMetros;

    public AvisoDesvio(long instante, double distanciaMetros) {
        this.instante = instante;
        this.distanciaMetros = distanciaMetros;
    }

    public long getInstante() {
        return instante;
    }

    public double getDistanciaMetros() {
        return distanciaMetros;
    }
}