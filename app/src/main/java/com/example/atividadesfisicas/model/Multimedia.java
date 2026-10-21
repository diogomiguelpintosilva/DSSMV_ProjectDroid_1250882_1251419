package com.example.atividadesfisicas.model;

/**
 * Modelo de domínio: captura multimédia feita durante a atividade (foto ou vídeo).
 */
public abstract class Multimedia {

    private final String caminho;
    private final long instante;

    protected Multimedia(String caminho, long instante) {
        this.caminho = caminho;
        this.instante = instante;
    }

    public String getCaminho() {
        return caminho;
    }

    public long getInstante() {
        return instante;
    }
}