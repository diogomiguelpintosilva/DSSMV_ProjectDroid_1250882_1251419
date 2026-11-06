package com.example.atividadesfisicas.modelo;

//Apenas para utilizadores já autenticados
public class Utilizador {
    //Atributos privados da classe
    private final String email; //final diz que após atribuído um valor, não pode ser mudado.
    private final String uid;

    //CONSTRUTOR
    public Utilizador(String email, String uid) {
        this.email = email;
        this.uid = uid;
    }

    //GETTERS
    public String getEmail() {
        return email;
    }

    public String getUid() {
        return uid;
    }
}