package com.example.atividadesfisicas.auth;

/** Erro de autenticação com mensagem pronta a mostrar ao utilizador. */
public class AuthException extends Exception {

    public AuthException(String mensagem) {
        super(mensagem);
    }
}
