package com.example.atividadesfisicas.modelo;

import com.example.atividadesfisicas.utilitarios.Config;

//vídeo final (máx de 2 minutos) com todas as capturas da atividade.
public class VideoResumo {
    //ATRIBUTOS
    private final String caminho;
    private final long duracaoMs;

    //CONSTRUTOR
    public VideoResumo(String caminho, long duracaoMs) {
        this.caminho = caminho;
        this.duracaoMs = duracaoMs;
    }

    //GETTERS
    public String getCaminho() {
        return caminho;
    }
    public long getDuracaoMs() {return duracaoMs;}
    public static long getDuracaoMaxMs() {return Config.VIDEO_RESUMO_MAX_MS;} //devolve limite de tempo de vídeo(2min).
}   //static pq o tempo é constante para todos os vídeos