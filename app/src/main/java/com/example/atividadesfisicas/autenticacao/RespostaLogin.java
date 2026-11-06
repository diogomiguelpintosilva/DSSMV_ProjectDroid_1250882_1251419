package com.example.atividadesfisicas.autenticacao;

import org.json.JSONException;
import org.json.JSONObject;

// Resposta do Firebase ao login e à criação de conta: uid, email e tokens.
public class RespostaLogin {

    //ATRIBUTOS
    private final String uid;
    private final String email;
    private final String idToken;
    private final String refreshToken;
    private final long expiresInSegundos;

    //CONSTRUTOR
    public RespostaLogin(String uid, String email, String idToken, String refreshToken,
                            long expiresInSegundos) {
        this.uid = uid;
        this.email = email;
        this.idToken = idToken;
        this.refreshToken = refreshToken;
        this.expiresInSegundos = expiresInSegundos;
    }

    //GETTERS
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

    //Transforma o texto do Firebase num objeto RespostaLogin
    public static RespostaLogin fromJson(String json) throws JSONException {
        JSONObject obj = new JSONObject(json); //torna num objeto JSON
        long expira;
        try {
            expira = Long.parseLong(obj.optString("expiresIn", "3600")); //lê se o3600(texto) com optString. Long.parselong converte para num.
        } catch (NumberFormatException e) {
            expira = 3600;  //se campo não existir usa-se 3600
        }
        return new RespostaLogin(obj.getString("localId"), obj.getString("email"),
                obj.getString("idToken"), obj.optString("refreshToken", null), expira); //devolvle uma "ficha" com os campos já preenchidos
    }
}
