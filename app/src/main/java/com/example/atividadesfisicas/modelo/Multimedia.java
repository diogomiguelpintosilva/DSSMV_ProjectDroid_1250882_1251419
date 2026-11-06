package com.example.atividadesfisicas.modelo;


//captura multimédia feita durante a atividade (foto ou vídeo).
public abstract class Multimedia {
    //ATRIBUTOS
    private final String caminho;
    private final long instante;

    //CONSTRUTOR
    //protegido para que apenas as subclasses podem chamar
    protected Multimedia(String caminho, long instante) {
        this.caminho = caminho;//caminho do ficheiro
        this.instante = instante;
    }

    //GETTERS(herdados pelas subclasses)
    public String getCaminho() {
        return caminho;
    }

    public long getInstante() {
        return instante;
    }
}

/*
não se pode criar um objeto Multimédia(CLASSE MÃE), já que ele é "abstract",
apenas se pode criar FOTO/VÍDEO
 */