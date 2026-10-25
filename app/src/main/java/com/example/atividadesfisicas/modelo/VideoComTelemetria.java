package com.example.atividadesfisicas.modelo;

/**
 * Modelo de domínio: vídeo com velocidade e altitude sobrepostas (overlay).
 */
public class VideoComTelemetria extends Video {

    public VideoComTelemetria(String caminho, long instante, long duracaoMs) {
        super(caminho, instante, duracaoMs);
    }
}