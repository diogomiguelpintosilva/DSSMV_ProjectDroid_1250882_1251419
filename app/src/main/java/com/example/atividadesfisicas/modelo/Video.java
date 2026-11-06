package com.example.atividadesfisicas.modelo;


//vídeo gravado durante a atividade.

public class Video extends Multimedia { //herdada de Multimédia
    //ATRIBUTO
    private final long duracaoMs;

    //CONSTRUTOR
    public Video(String caminho, long instante, long duracaoMs) {
        super(caminho, instante);
        this.duracaoMs = duracaoMs;
    }

    //GETTER
    public long getDuracaoMs() {
        return duracaoMs;
    }
}