package com.example.atividadesfisicas.auth;

import com.example.atividadesfisicas.model.Utilizador;

/** Guarda o utilizador autenticado (e os tokens do Firebase) enquanto a aplicação está aberta. */
public final class Sessao {

    private static final Sessao INSTANCE = new Sessao();

    private Utilizador utilizador;
    private String idToken;
    private String refreshToken;
    private long expiraEmMs;

    private Sessao() {
    }

    public static Sessao getInstance() {
        return INSTANCE;
    }

    public synchronized Utilizador getUtilizador() {
        return utilizador;
    }

    public synchronized void setUtilizador(Utilizador utilizador) {
        this.utilizador = utilizador;
    }

    /** Uid do utilizador autenticado (usado para separar os dados de cada conta). */
    public synchronized String getUid() {
        return (utilizador != null) ? utilizador.getUid() : "anonimo";
    }

    public synchronized void setTokens(String idToken, String refreshToken, long expiraEmSegundos) {
        this.idToken = idToken;
        this.refreshToken = refreshToken;
        this.expiraEmMs = System.currentTimeMillis() + expiraEmSegundos * 1000L;
    }

    public synchronized String getIdToken() {
        return idToken;
    }

    public synchronized String getRefreshToken() {
        return refreshToken;
    }

    public synchronized long getExpiraEmMs() {
        return expiraEmMs;
    }
}
