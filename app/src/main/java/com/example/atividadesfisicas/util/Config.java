package com.example.atividadesfisicas.util;

/**
 * Constantes de configuração da aplicação (um só sítio para ajustar valores).
 */
public final class Config {

    private Config() {
    }

    /** Web API Key do projeto Firebase (Project settings > General). Autenticação Email/Password tem de estar ativa. */
    public static final String FIREBASE_API_KEY = "AIzaSyBtXZgijThuMFkOlywY2EXkGqfxfnNbxAk";

    /**Identificação da app nos pedidos de mapas.*/
    public static final String USER_AGENT_MAPA =
            "DSSMV_ProjectDroid/1.0 (projeto academico ISEP; +https://github.com/diogomiguelpintosilva/DSSMV_ProjectDroid_1250882_1251419)";

    /** Tipo de atividade (a aplicação não distingue corrida de caminhada). */
    public static final String TIPO_ATIVIDADE = "corrida/caminhada";

    /** Distância máxima à rota planeada antes de ser considerado desvio. */
    public static final double TOLERANCIA_DESVIO_METROS = 30.0;

    /** Tempo mínimo entre dois avisos de desvio consecutivos. */
    public static final long COOLDOWN_AVISO_DESVIO_MS = 15000L;

    /** Intervalo "X em X tempo" entre lembretes para tirar fotos/vídeos (para testar, pôr 30000L). */
    public static final long INTERVALO_LEMBRETE_MS = 5 * 60 * 1000L;

    /** Intervalo pedido ao GPS. */
    public static final long GPS_INTERVALO_MS = 1000L;

    /** Pontos GPS com precisão pior do que isto são ignorados. */
    public static final float PRECISAO_MAXIMA_METROS = 50f;

    /** Duração máxima do vídeo resumo (2 minutos). */
    public static final long VIDEO_RESUMO_MAX_MS = 120000L;

    /** Tempo que cada foto fica no vídeo resumo. */
    public static final long DURACAO_FOTO_NO_RESUMO_MS = 3000L;
}