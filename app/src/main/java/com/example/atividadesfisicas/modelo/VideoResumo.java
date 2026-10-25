package com.example.atividadesfisicas.modelo;

import com.example.atividadesfisicas.utilitarios.Config;

/**
 * Modelo de domínio: vídeo final (máx. 2 minutos) com todas as capturas da atividade.
 */
public class VideoResumo {

    private final String caminho;
    private final long duracaoMs;

    public VideoResumo(String caminho, long duracaoMs) {
        this.caminho = caminho;
        this.duracaoMs = duracaoMs;
    }

    public String getCaminho() {
        return caminho;
    }

    public long getDuracaoMs() {
        return duracaoMs;
    }

    public static long getDuracaoMaxMs() {
        return Config.VIDEO_RESUMO_MAX_MS;
    }
}