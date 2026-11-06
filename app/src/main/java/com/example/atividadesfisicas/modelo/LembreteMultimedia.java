package com.example.atividadesfisicas.modelo;

// lembrete periódico para tirar fotos ou gravar vídeos.
public class LembreteMultimedia {
    //ATRIBUTOS
    private final long instante;
    private final long intervalo;

    //CONSTRUTOR
    public LembreteMultimedia(long instante, long intervalo) {
        this.instante = instante;
        this.intervalo = intervalo;
    }

    //GETTERS
    public long getInstante() {
        return instante;
    }
    public long getIntervalo() {
        return intervalo;
    }
}

//Guarda quando foi o último lembrete e o intervalo entre cada um. criado pelo ServicoGps