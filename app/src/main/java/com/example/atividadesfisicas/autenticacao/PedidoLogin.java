package com.example.atividadesfisicas.autenticacao;

import org.json.JSONException;
import org.json.JSONObject;

/** DTO enviado à API de autenticação. */
public class PedidoLogin {

    private final String email;
    private final String password;

    public PedidoLogin(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("email", email);
        obj.put("password", password);
        obj.put("returnSecureToken", true);
        return obj.toString();
    }
}
