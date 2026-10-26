package com.example.atividadesfisicas.autenticacao;

import com.example.atividadesfisicas.utilitarios.Config;
import com.example.atividadesfisicas.utilitarios.PedidosHttp;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * UC0: login e criação de conta no Firebase Authentication, através de pedidos HTTP (API REST).
 * A conta fica guardada no Firebase. Os dois pedidos têm o mesmo formato e a mesma resposta.
 * Tem de ser chamado a partir de uma thread de fundo (faz rede).
 * As mensagens de erro estão aqui, em vez de no strings.xml, porque esta classe não tem acesso aos recursos do Android.
 */
public final class Autenticacao {

    private static final String URL_LOGIN =
            "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=";
    private static final String URL_CRIAR_CONTA =
            "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=";

    private Autenticacao() {
    }

    /** Faz login com uma conta que já existe. */
    public static RespostaLogin login(String email, String password) throws ErroAutenticacao {
        return enviarPedido(URL_LOGIN, email, password, "Não foi possível iniciar sessão.");
    }

    /**
     * Cria uma conta nova no Firebase. Se correr bem, o utilizador já pode fazer login com o email e a
     * palavra-passe escolhidos.
     */
    public static RespostaLogin criarConta(String email, String password) throws ErroAutenticacao {
        return enviarPedido(URL_CRIAR_CONTA, email, password, "Não foi possível criar a conta.");
    }

    /**
     * Envia o email e a palavra-passe para o endereço indicado (login ou criar conta).
     *
     * @param mensagemGenerica texto usado quando o Firebase devolve um erro que não conhecemos
     */
    private static RespostaLogin enviarPedido(String urlBase, String email, String password,
                                              String mensagemGenerica) throws ErroAutenticacao {
        try {
            PedidoLogin pedido = new PedidoLogin(email, password);
            PedidosHttp.Resposta resposta = PedidosHttp.postJson(urlBase + Config.FIREBASE_API_KEY, pedido.toJson());

            // código 200 = correu bem
            if (resposta.codigo == 200) {
                return RespostaLogin.fromJson(resposta.corpo);
            }
            throw new ErroAutenticacao(traduzirErro(resposta.corpo, mensagemGenerica));
        } catch (IOException e) {
            throw new ErroAutenticacao("Sem ligação à internet ou servidor indisponível.");
        } catch (JSONException e) {
            throw new ErroAutenticacao("Resposta inválida do servidor.");
        }
    }

    /**
     * O Firebase devolve os erros em inglês, por exemplo {"error":{"message":"EMAIL_EXISTS"}}.
     * Este método converte o código numa mensagem em português.
     */
    private static String traduzirErro(String corpo, String mensagemGenerica) {
        String codigo = "";
        try {
            codigo = new JSONObject(corpo).getJSONObject("error").getString("message");
        } catch (JSONException e) {
            // não veio no formato esperado: usa a mensagem genérica
        }
        if (codigo.contains("INVALID_LOGIN_CREDENTIALS") || codigo.contains("INVALID_PASSWORD")
                || codigo.contains("EMAIL_NOT_FOUND")) {
            return "Email ou palavra-passe incorretos.";
        }
        if (codigo.contains("EMAIL_EXISTS")) {
            return "Já existe uma conta com este email.";
        }
        if (codigo.contains("WEAK_PASSWORD")) {
            return "A palavra-passe tem de ter pelo menos 6 caracteres.";
        }
        if (codigo.contains("INVALID_EMAIL")) {
            return "Email inválido.";
        }
        if (codigo.contains("USER_DISABLED")) {
            return "Conta desativada.";
        }
        if (codigo.contains("OPERATION_NOT_ALLOWED")) {
            return "O registo por email e palavra-passe não está ativo no Firebase.";
        }
        if (codigo.contains("TOO_MANY_ATTEMPTS_TRY_LATER")) {
            return "Demasiadas tentativas. Tente outra vez daqui a alguns minutos.";
        }
        if (codigo.contains("API key not valid") || codigo.contains("API_KEY")) {
            return "Chave da API do Firebase inválida (ver Config.java).";
        }
        return mensagemGenerica;
    }
}
