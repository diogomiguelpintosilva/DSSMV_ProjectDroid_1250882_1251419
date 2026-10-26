package com.example.atividadesfisicas.autenticacao;

import org.json.JSONException;
import org.json.JSONObject;

/** Dados enviados ao Firebase no login e na criação de conta (DTO: só transporta dados). */
public class PedidoLogin {

    private final String email;
    private final String password;

    public PedidoLogin(String email, String password) {
        this.email = email;
        this.password = password;
    }

    /** O corpo do pedido em JSON. "returnSecureToken" pede ao Firebase que devolva os tokens. */
    public String toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("email", email);
        obj.put("password", password);
        obj.put("returnSecureToken", true);
        return obj.toString();
    }
}
