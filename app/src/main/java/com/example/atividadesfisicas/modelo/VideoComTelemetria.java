package com.example.atividadesfisicas.modelo;

//vídeo com velocidade e altitude sobrepostas (overlay).

//CONSTRUTOR(vai passando por classes até a Classe Mãe)
public class VideoComTelemetria extends Video { //herdada de video
    public VideoComTelemetria(String caminho, long instante, long duracaoMs) {
        super(caminho, instante, duracaoMs);
    }
}