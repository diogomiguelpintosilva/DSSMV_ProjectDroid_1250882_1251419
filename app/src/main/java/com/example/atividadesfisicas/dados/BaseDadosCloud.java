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

/*
 * Acesso ao Firebase Firestore (base de dados na cloud) pela API REST, com o token do utilizador
 * autenticado. Cada utilizador tem as suas coleções. Cada documento guarda o objeto em JSON num campo de texto ("json").
 * Todos os métodos têm de ser chamados a partir de uma thread de fundo.
 */
public final class BaseDadosCloud {

    private static final String BASE = "https://firestore.googleapis.com/v1/projects/" //endereço do Firestore do nosso porjeto: Pedidos começam aqui.
            + Config.FIREBASE_PROJECT_ID + "/databases/(default)/documents/";
    private static final String URL_RENOVAR_TOKEN = "https://securetoken.googleapis.com/v1/token?key=";//endereço do Google para renovar token. Junta-se com a chave da API na altura do pedido.

    private BaseDadosCloud() {
    }

    /* Cria um documento. Se já existir (409), não faz nada. */
    public static void guardar(String colecao, String docId, String json, long ordem) throws IOException {
        try {
            JSONObject campos = new JSONObject(); //criar objeto tipo JSON
            campos.put("json", new JSONObject().put("stringValue", json)); //atribui tipo string ao json
            campos.put("ordem", new JSONObject().put("integerValue", String.valueOf(ordem))); //atribui tipo inteiro ao json
            String corpo = new JSONObject().put("fields", campos).toString(); //coloca campos dentro campo maior e transforma em string, para se poder enviar pela rede
            String url = BASE + caminhoColecao(colecao) + "?documentId=" + URLEncoder.encode(docId, "UTF-8"); //vai construir endereço para onde vai pedido
            Resposta r = enviar("POST", url, corpo); //entrega os dados ao metodo "enviar"          //caminhoColecao devolce pasta com dados do utilizador.
            if (r.codigo != 200 && r.codigo != 409) {                                                       //"?" dá nome ao documento, para não ser o Firebase a dar.
                throw new IOException("Firestore devolveu " + r.codigo); //outro código para além de 200(doc criado) e 409(já existe)
            }
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    // Apaga um documento da coleção do utilizador (se já não existir, não faz nada).
    public static void apagar(String colecao, String docId) throws IOException {
        String url = BASE + caminhoColecao(colecao) + "/" + URLEncoder.encode(docId, "UTF-8");
        Resposta r = enviar("DELETE", url, null);
        if (r.codigo != 200 && r.codigo != 404) {
            throw new IOException("Firestore devolveu " + r.codigo);
        }
    }

    /*
     Devolve o JSON dos documentos da coleção do utilizador (até 300; é mais do que suficiente
     para as rotas e o histórico de uma conta).
     */
    public static List<String> listar(String colecao) throws IOException {
        List<String> resultado = new ArrayList<>();
        try {
            Resposta r = enviar("GET", BASE + caminhoColecao(colecao) + "?pageSize=300", null); //ler documentos
            if (r.codigo != 200) {
                throw new IOException("Firestore devolveu " + r.codigo); //se der erro
            }
            // se a coleção estiver vazia, o Firestore nem envia a chave "documents"
            JSONArray documentos = new JSONObject(r.corpo).optJSONArray("documents"); //transformar documentos em json
            if (documentos != null) {
                for (int i = 0; i < documentos.length(); i++) {
                    JSONObject campos = documentos.getJSONObject(i).optJSONObject("fields");
                    if (campos != null && campos.has("json")) {
                        resultado.add(campos.getJSONObject("json").getString("stringValue"));
                    }
                }
                /*
                Vai percorrer cada documento, retira texto de atividade/rota organizadamente e adiciona à lista q vai ser devolvida no final.
                 */
            }
        } catch (JSONException e) {
            throw new IOException(e);
        }
        return resultado; //devolve lista de documentos
    }

    // Caminho da coleção da conta atual: utilizadores/<uid>/<colecao>
    private static String caminhoColecao(String colecao) {
        return "utilizadores/" + Sessao.getInstance().getUid() + "/" + colecao;
    }

    // Envia um pedido com o token; se o token tiver expirado (401), renova-o e tenta outra vez.
    private static Resposta enviar(String metodo, String url, String corpo) throws IOException {
        String tipo = (corpo != null) ? "application/json; charset=UTF-8" : null; //se há corpo, avisa q é json, senão não diz nada
        Resposta r = PedidosHttp.pedido(metodo, url, corpo, tipo, obterToken(false));//tipo -> json/null/pedido specífico de obter token
        if (r.codigo == 401) {
            // token expirado, renova e tenta outra vez
            r = PedidosHttp.pedido(metodo, url, corpo, tipo, obterToken(true));
        }
        return r;
    }

    // Devolve um token válido, renovando-o com o refresh token quando está a expirar.
    private static String obterToken(boolean forcar) throws IOException {
        Sessao sessao = Sessao.getInstance();
        synchronized (BaseDadosCloud.class) { //tudo no loope é executado por uma thread à vez.
            if (sessao.getIdToken() == null) { //sem token -> loop não acontece
                throw new IOException("Sem sessão iniciada");
            }
            boolean expirado = System.currentTimeMillis() > sessao.getExpiraEmMs() - 60000L; //vê se token está expirado
            if (!forcar && !expirado) { //forcar(True/False) diz se quer,ou não, renovar o token(mesmo que seja válido).
                return sessao.getIdToken(); //devolve token atual
            }
            if (sessao.getRefreshToken() == null) {
                throw new IOException("Sem refresh token");
            }
            String corpo = "grant_type=refresh_token&refresh_token=" //corpo a dar token para refresh
                    + URLEncoder.encode(sessao.getRefreshToken(), "UTF-8");
            Resposta r = PedidosHttp.pedido("POST", URL_RENOVAR_TOKEN + Config.FIREBASE_API_KEY, corpo,
                    "application/x-www-form-urlencoded", null); //pede refresh
            if (r.codigo != 200) {
                throw new IOException("Não foi possível renovar o token (" + r.codigo + ")");
            }
            try {
                JSONObject obj = new JSONObject(r.corpo); //transforma mensagem do google em json
                long expira = Long.parseLong(obj.optString("expires_in", "3600")); //lê duração, converte 3600 para int, se não existor, usa 1 hora como duração
                sessao.setTokens(obj.getString("id_token"),
                        obj.optString("refresh_token", sessao.getRefreshToken()), expira); //guarda novo token na sessão
            } catch (JSONException | NumberFormatException e) {
                throw new IOException(e);
            }
            return sessao.getIdToken(); //retorna token do user
        }
    }
}
