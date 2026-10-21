package com.example.atividadesfisicas.model;

/**
 * Modelo de domínio: lembrete periódico para tirar fotos ou gravar vídeos.
 */
public class LembreteMultimedia {

    private final long instante;
    private final long intervalo;

    public LembreteMultimedia(long instante, long intervalo) {
        this.instante = instante;
        this.intervalo = intervalo;
    }

    public long getInstante() {
        return instante;
    }

    public long getIntervalo() {
        return intervalo;
    }
}