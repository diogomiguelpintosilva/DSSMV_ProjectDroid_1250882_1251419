package com.example.atividadesfisicas.data;

import com.example.atividadesfisicas.auth.Sessao;
import com.example.atividadesfisicas.util.Config;
import com.example.atividadesfisicas.util.HttpUtil;
import com.example.atividadesfisicas.util.HttpUtil.Resposta;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Acesso ao Firebase Firestore pela API REST, usando o token do utilizador autenticado
 * (mesma abordagem do AuthApi). Cada utilizador tem as suas coleções em
 * utilizadores/{uid}/rotas e utilizadores/{uid}/atividades.
 * Cada documento guarda o JSON do objeto num único campo de texto ("json").
 * Todos os métodos têm de ser chamados a partir de uma worker thread.
 */
public final class FirestoreApi {

    private static final String BASE = "https://firestore.googleapis.com/v1/projects/"
            + Config.FIREBASE_PROJECT_ID + "/databases/(default)/documents/";
    private static final String URL_REFRESH = "https://securetoken.googleapis.com/v1/token?key=";

    private FirestoreApi() {
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

    /** Devolve o JSON de todos os documentos da coleção do utilizador. */
    public static List<String> listar(String colecao) throws IOException {
        List<String> resultado = new ArrayList<>();
        String pagina = null;
        try {
            do {
                String url = BASE + caminhoColecao(colecao) + "?pageSize=300";
                if (pagina != null) {
                    url += "&pageToken=" + URLEncoder.encode(pagina, "UTF-8");
                }
                Resposta r = enviar("GET", url, null);
                if (r.codigo != 200) {
                    throw new IOException("Firestore devolveu " + r.codigo);
                }
                JSONObject obj = new JSONObject(r.corpo);
                JSONArray docs = obj.optJSONArray("documents");
                if (docs != null) {
                    for (int i = 0; i < docs.length(); i++) {
                        JSONObject campos = docs.getJSONObject(i).optJSONObject("fields");
                        if (campos != null && campos.has("json")) {
                            resultado.add(campos.getJSONObject("json").getString("stringValue"));
                        }
                    }
                }
                pagina = obj.optString("nextPageToken", "");
                if (pagina.isEmpty()) {
                    pagina = null;
                }
            } while (pagina != null);
        } catch (JSONException e) {
            throw new IOException(e);
        }
        return resultado;
    }

    private static String caminhoColecao(String colecao) {
        return "utilizadores/" + Sessao.getInstance().getUid() + "/" + colecao;
    }

    private static Resposta enviar(String metodo, String url, String corpo) throws IOException {
        String tipo = (corpo != null) ? "application/json; charset=UTF-8" : null;
        Resposta r = HttpUtil.pedido(metodo, url, corpo, tipo, obterToken(false));
        if (r.codigo == 401) {
            // token expirado: renova e tenta outra vez
            r = HttpUtil.pedido(metodo, url, corpo, tipo, obterToken(true));
        }
        return r;
    }

    /** Devolve um token válido, renovando-o com o refresh token quando está a expirar. */
    private static String obterToken(boolean forcar) throws IOException {
        Sessao sessao = Sessao.getInstance();
        synchronized (FirestoreApi.class) {
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
            Resposta r = HttpUtil.pedido("POST", URL_REFRESH + Config.FIREBASE_API_KEY, corpo,
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
