package com.example.atividadesfisicas.model;

/**
 * Modelo de domínio: vídeo gravado durante a atividade.
 */
public class Video extends Multimedia {

    private final long duracaoMs;

    public Video(String caminho, long instante, long duracaoMs) {
        super(caminho, instante);
        this.duracaoMs = duracaoMs;
    }

    public long getDuracaoMs() {
        return duracaoMs;
    }
}