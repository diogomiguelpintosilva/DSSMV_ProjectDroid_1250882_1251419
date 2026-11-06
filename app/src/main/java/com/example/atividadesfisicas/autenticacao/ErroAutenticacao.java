package com.example.atividadesfisicas.autenticacao;

/* Erro do login ou de criar conta, com a mensagem em português pronta a mostrar no ecrã. */
public class ErroAutenticacao extends Exception {

    //CONSTRUTOR
    public ErroAutenticacao(String mensagem) {
        super(mensagem);
    }
}
