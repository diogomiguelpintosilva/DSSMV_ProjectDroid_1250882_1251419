package com.example.atividadesfisicas.auth;

import com.example.atividadesfisicas.model.Utilizador;

/** Guarda o utilizador autenticado enquanto a aplicação está aberta. */

public final class Sessao {

    private static final Sessao INSTANCE = new Sessao();

    private Utilizador utilizador;

    private Sessao() {
    }

    public static Sessao getInstance() {
        return INSTANCE;
    }

    public Utilizador getUtilizador() {
        return utilizador;
    }

    public void setUtilizador(Utilizador utilizador) {
        this.utilizador = utilizador;
    }
}
