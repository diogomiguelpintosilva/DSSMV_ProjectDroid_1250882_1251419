package com.example.atividadesfisicas.autenticacao;

/** Erro de autenticação com mensagem pronta a mostrar ao utilizador. */
public class ErroAutenticacao extends Exception {

    public ErroAutenticacao(String mensagem) {
        super(mensagem);
    }
}
