package com.example.atividadesfisicas.auth;

import org.json.JSONException;
import org.json.JSONObject;

/** DTO enviado à API de autenticação. */
public class LoginRequestDTO {

    private final String email;
    private final String password;

    public LoginRequestDTO(String email, String password) {
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
