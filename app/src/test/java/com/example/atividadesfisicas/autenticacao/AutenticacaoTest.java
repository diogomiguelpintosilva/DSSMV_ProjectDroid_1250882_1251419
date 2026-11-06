package com.example.atividadesfisicas.autenticacao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.lang.reflect.Method;

/**
 * Testa o login: o pedido enviado ao Firebase, a resposta recebida
 * e a tradução das mensagens de erro para português.
 * (Precisa de testImplementation 'org.json:json:20240303' no build.gradle.)
 */
public class AutenticacaoTest {

    // ---------- PedidoLogin (o que enviamos) ----------

    @Test
    public void pedidoLogin_temEmailPasswordEReturnSecureToken() throws JSONException {
        JSONObject obj = new JSONObject(new PedidoLogin("ana@exemplo.pt", "segredo1").toJson());
        assertEquals("ana@exemplo.pt", obj.getString("email"));
        assertEquals("segredo1", obj.getString("password"));
        assertTrue(obj.getBoolean("returnSecureToken"));
    }

    // ---------- RespostaLogin (o que recebemos) ----------

    @Test
    public void respostaLogin_completa_preencheOsCampos() throws JSONException {
        RespostaLogin r = RespostaLogin.fromJson(
                "{\"localId\":\"uid123\",\"email\":\"ana@exemplo.pt\",\"idToken\":\"tok\","
                        + "\"refreshToken\":\"ref\",\"expiresIn\":\"7200\"}");
        assertEquals("uid123", r.getUid());
        assertEquals("tok", r.getIdToken());
        assertEquals("ref", r.getRefreshToken());
        assertEquals(7200L, r.getExpiresInSegundos());
    }

    @Test
    public void respostaLogin_semExpiresIn_usa3600() throws JSONException {
        RespostaLogin r = RespostaLogin.fromJson("{\"localId\":\"u\",\"email\":\"e\",\"idToken\":\"t\"}");
        assertEquals(3600L, r.getExpiresInSegundos());
    }

    @Test(expected = JSONException.class)
    public void respostaLogin_semIdToken_lancaExcecao() throws JSONException {
        RespostaLogin.fromJson("{\"localId\":\"u\",\"email\":\"e\"}");
    }

    // ---------- tradução de erros (método private, chamado por reflexão) ----------

    private static String traduzir(String codigoFirebase) throws Exception {
        Method m = Autenticacao.class.getDeclaredMethod("traduzirErro", String.class, String.class);
        m.setAccessible(true);
        String corpo = "{\"error\":{\"message\":\"" + codigoFirebase + "\"}}";
        return (String) m.invoke(null, corpo, "Falha genérica");
    }

    @Test
    public void erro_credenciaisInvalidas() throws Exception {
        assertEquals("Email ou palavra-passe incorretos.", traduzir("INVALID_LOGIN_CREDENTIALS"));
    }

    @Test
    public void erro_emailJaExiste() throws Exception {
        assertEquals("Já existe uma conta com este email.", traduzir("EMAIL_EXISTS"));
    }

    @Test
    public void erro_desconhecido_devolveMensagemGenerica() throws Exception {
        assertEquals("Falha genérica", traduzir("ALGO_NOVO"));
    }
}
