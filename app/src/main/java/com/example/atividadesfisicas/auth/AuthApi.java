package com.example.atividadesfisicas.auth;

import com.example.atividadesfisicas.util.Config;
import com.example.atividadesfisicas.util.HttpUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;


public final class AuthApi {

    private static final String URL_LOGIN =
            "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=";

    private AuthApi() {
    }

    public static LoginResponseDTO login(String email, String password) throws AuthException {
        try {
            LoginRequestDTO pedido = new LoginRequestDTO(email, password);
            HttpUtil.Resposta resposta = HttpUtil.postJson(URL_LOGIN + Config.FIREBASE_API_KEY, pedido.toJson());

            if (resposta.codigo == 200) {
                return LoginResponseDTO.fromJson(resposta.corpo);
            }
            throw new AuthException(traduzirErro(resposta.corpo));
        } catch (IOException e) {
            throw new AuthException("Sem ligação à internet ou servidor indisponível.");
        } catch (JSONException e) {
            throw new AuthException("Resposta inválida do servidor.");
        }
    }

    private static String traduzirErro(String corpo) {
        String codigo = "";
        try {
            codigo = new JSONObject(corpo).getJSONObject("error").getString("message");
        } catch (JSONException e) {
            // ignora, usa a mensagem genérica
        }
        if (codigo.contains("INVALID_LOGIN_CREDENTIALS") || codigo.contains("INVALID_PASSWORD")
                || codigo.contains("EMAIL_NOT_FOUND")) {
            return "Email ou palavra-passe incorretos.";
        }
        if (codigo.contains("INVALID_EMAIL")) {
            return "Email inválido.";
        }
        if (codigo.contains("USER_DISABLED")) {
            return "Conta desativada.";
        }
        if (codigo.contains("API key not valid") || codigo.contains("API_KEY")) {
            return "Web API Key do Firebase inválida (ver Config.java).";
        }
        return "Não foi possível iniciar sessão.";
    }
}
