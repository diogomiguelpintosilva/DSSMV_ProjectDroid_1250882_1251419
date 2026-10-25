package com.example.atividadesfisicas.auth;

import org.json.JSONException;
import org.json.JSONObject;

/** DTO recebido da API de autenticação. */
public class LoginResponseDTO {

    private final String uid;
    private final String email;
    private final String idToken;
    private final String refreshToken;
    private final long expiresInSegundos;

    public LoginResponseDTO(String uid, String email, String idToken, String refreshToken,
                            long expiresInSegundos) {
        this.uid = uid;
        this.email = email;
        this.idToken = idToken;
        this.refreshToken = refreshToken;
        this.expiresInSegundos = expiresInSegundos;
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

    public String getRefreshToken() {
        return refreshToken;
    }

    public long getExpiresInSegundos() {
        return expiresInSegundos;
    }

    public static LoginResponseDTO fromJson(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        long expira;
        try {
            expira = Long.parseLong(obj.optString("expiresIn", "3600"));
        } catch (NumberFormatException e) {
            expira = 3600;
        }
        return new LoginResponseDTO(obj.getString("localId"), obj.getString("email"),
                obj.getString("idToken"), obj.optString("refreshToken", null), expira);
    }
}
