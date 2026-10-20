package com.example.atividadesfisicas.auth;

import org.json.JSONException;
import org.json.JSONObject;

/** DTO recebido da API de autenticação. */
public class LoginResponseDTO {

    private final String uid;
    private final String email;
    private final String idToken;

    public LoginResponseDTO(String uid, String email, String idToken) {
        this.uid = uid;
        this.email = email;
        this.idToken = idToken;
    }

    public String getUid() {
        return uid;
    }

    public String getEmail() {
        return email;
    }

    public String getIdToken() {
        return idToken;
    }

    public static LoginResponseDTO fromJson(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        return new LoginResponseDTO(obj.getString("localId"), obj.getString("email"),
                obj.getString("idToken"));
    }
}
