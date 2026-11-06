package com.example.atividadesfisicas.modelo;

//CLASSE que trata a ocorrência de desvio da rota planeada.

public class AvisoDesvio {
    //ATRIBUTOS
    private final long instante;
    private final double distanciaMetros;

    //CONSTRUTOR
    public AvisoDesvio(long instante, double distanciaMetros) {
        this.instante = instante;
        this.distanciaMetros = distanciaMetros;
    }

    //GETTERS
    public long getInstante() {
        return instante;
    }
    public double getDistanciaMetros() {
        return distanciaMetros;
    }
}

/*
    basicamente regista-se o momento em que o utilizador estavaa fora de rota(quando e a que distância);
    cirado pelo ServicoGps.verificarDesvio() e guarda-se na lista de avisos da atividade
 */