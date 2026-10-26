package com.example.atividadesfisicas.dados;

import com.example.atividadesfisicas.autenticacao.Sessao;
import com.example.atividadesfisicas.utilitarios.Config;
import com.example.atividadesfisicas.utilitarios.PedidosHttp;
import com.example.atividadesfisicas.utilitarios.PedidosHttp.Resposta;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Acesso ao Firebase Firestore (base de dados na cloud) pela API REST, com o token do utilizador
 * autenticado. Cada utilizador tem as suas coleções em utilizadores/{uid}/rotas e
 * utilizadores/{uid}/atividades. Cada documento guarda o objeto em JSON num campo de texto ("json").
 * Todos os métodos têm de ser chamados a partir de uma thread de fundo.
 */
public final class BaseDadosCloud {

    private static final String BASE = "https://firestore.googleapis.com/v1/projects/"
            + Config.FIREBASE_PROJECT_ID + "/databases/(default)/documents/";
    private static final String URL_RENOVAR_TOKEN = "https://securetoken.googleapis.com/v1/token?key=";

    private BaseDadosCloud() {
    }

    /** Cria um documento. Se já existir (409), não faz nada. */
    public static void guardar(String colecao, String docId, String json, long ordem) throws IOException {
        try {
            JSONObject campos = new JSONObject();
            campos.put("json", new JSONObject().put("stringValue", json));
            campos.put("ordem", new JSONObject().put("integerValue", String.valueOf(ordem)));
            String corpo = new JSONObject().put("fields", campos).toString();
            String url = BASE + caminhoColecao(colecao) + "?documentId=" + URLEncoder.encode(docId, "UTF-8");
            Resposta r = enviar("POST", url, corpo);
            if (r.codigo != 200 && r.codigo != 409) {
                throw new IOException("Firestore devolveu " + r.codigo);
            }
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    /** Apaga um documento da coleção do utilizador (se já não existir, não faz nada). */
    public static void apagar(String colecao, String docId) throws IOException {
        String url = BASE + caminhoColecao(colecao) + "/" + URLEncoder.encode(docId, "UTF-8");
        Resposta r = enviar("DELETE", url, null);
        if (r.codigo != 200 && r.codigo != 404) {
            throw new IOException("Firestore devolveu " + r.codigo);
        }
    }

    /**
     * Devolve o JSON dos documentos da coleção do utilizador (até 300; é mais do que suficiente
     * para as rotas e o histórico de uma conta).
     */
    public static List<String> listar(String colecao) throws IOException {
        List<String> resultado = new ArrayList<>();
        try {
            Resposta r = enviar("GET", BASE + caminhoColecao(colecao) + "?pageSize=300", null);
            if (r.codigo != 200) {
                throw new IOException("Firestore devolveu " + r.codigo);
            }
            // se a coleção estiver vazia, o Firestore nem envia a chave "documents"
            JSONArray documentos = new JSONObject(r.corpo).optJSONArray("documents");
            if (documentos != null) {
                for (int i = 0; i < documentos.length(); i++) {
                    JSONObject campos = documentos.getJSONObject(i).optJSONObject("fields");
                    if (campos != null && campos.has("json")) {
                        resultado.add(campos.getJSONObject("json").getString("stringValue"));
                    }
                }
            }
        } catch (JSONException e) {
            throw new IOException(e);
        }
        return resultado;
    }

    /** Caminho da coleção da conta atual: utilizadores/<uid>/<colecao>. */
    private static String caminhoColecao(String colecao) {
        return "utilizadores/" + Sessao.getInstance().getUid() + "/" + colecao;
    }

    /** Envia um pedido com o token; se o token tiver expirado (401), renova-o e tenta outra vez. */
    private static Resposta enviar(String metodo, String url, String corpo) throws IOException {
        String tipo = (corpo != null) ? "application/json; charset=UTF-8" : null;
        Resposta r = PedidosHttp.pedido(metodo, url, corpo, tipo, obterToken(false));
        if (r.codigo == 401) {
            // token expirado: renova e tenta outra vez
            r = PedidosHttp.pedido(metodo, url, corpo, tipo, obterToken(true));
        }
        return r;
    }

    /** Devolve um token válido, renovando-o com o refresh token quando está a expirar. */
    private static String obterToken(boolean forcar) throws IOException {
        Sessao sessao = Sessao.getInstance();
        synchronized (BaseDadosCloud.class) {
            if (sessao.getIdToken() == null) {
                throw new IOException("Sem sessão iniciada");
            }
            boolean expirado = System.currentTimeMillis() > sessao.getExpiraEmMs() - 60000L;
            if (!forcar && !expirado) {
                return sessao.getIdToken();
            }
            if (sessao.getRefreshToken() == null) {
                throw new IOException("Sem refresh token");
            }
            String corpo = "grant_type=refresh_token&refresh_token="
                    + URLEncoder.encode(sessao.getRefreshToken(), "UTF-8");
            Resposta r = PedidosHttp.pedido("POST", URL_RENOVAR_TOKEN + Config.FIREBASE_API_KEY, corpo,
                    "application/x-www-form-urlencoded", null);
            if (r.codigo != 200) {
                throw new IOException("Não foi possível renovar o token (" + r.codigo + ")");
            }
            try {
                JSONObject obj = new JSONObject(r.corpo);
                long expira = Long.parseLong(obj.optString("expires_in", "3600"));
                sessao.setTokens(obj.getString("id_token"),
                        obj.optString("refresh_token", sessao.getRefreshToken()), expira);
            } catch (JSONException | NumberFormatException e) {
                throw new IOException(e);
            }
            return sessao.getIdToken();
        }
    }
}
