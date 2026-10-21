package com.example.atividadesfisicas.model;

/**
 * Modelo de domínio: utilizador autenticado.
 */
public class Utilizador {

    private final String email;
    private final String uid;

    public Utilizador(String email, String uid) {
        this.email = email;
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public String getUid() {
        return uid;
    }
}