package com.example.atividadesfisicas.utilitarios;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Pedidos HTTP simples (login e Firestore), com HttpURLConnection.
 * Tem de ser chamado a partir de uma thread de fundo, nunca da thread principal.
 */
public final class PedidosHttp {

    private static final int TEMPO_LIGACAO_MS = 10000;
    private static final int TEMPO_LEITURA_MS = 15000;

    private PedidosHttp() {
    }

    /** Resultado de um pedido: o código HTTP e o texto devolvido. */
    public static class Resposta {
        public final int codigo;
        public final String corpo;

        public Resposta(int codigo, String corpo) {
            this.codigo = codigo;
            this.corpo = corpo;
        }
    }

    /** POST com corpo JSON. Devolve o código e o corpo (também em caso de erro HTTP, ex.: 400). */
    public static Resposta postJson(String endereco, String json) throws IOException {
        return pedido("POST", endereco, json, "application/json; charset=UTF-8", null);
    }

    /**
     * Pedido HTTP genérico (GET, POST, DELETE...).
     *
     * @param corpo     corpo do pedido (null se não houver)
     * @param tipoCorpo tipo do corpo, cabeçalho Content-Type (null se não houver corpo)
     * @param token     token enviado em "Authorization: Bearer ..." (null se não for preciso)
     */
    public static Resposta pedido(String metodo, String endereco, String corpo, String tipoCorpo,
                                  String token) throws IOException {
        HttpURLConnection ligacao = null;
        try {
            URLConnection ligacaoUrl = new URL(endereco).openConnection();
            if (!(ligacaoUrl instanceof HttpURLConnection)) {
                throw new IOException("O endereço não é HTTP: " + endereco);
            }
            ligacao = (HttpURLConnection) ligacaoUrl;
            ligacao.setRequestMethod(metodo);
            if (tipoCorpo != null) {
                ligacao.setRequestProperty("Content-Type", tipoCorpo);
            }
            if (token != null) {
                ligacao.setRequestProperty("Authorization", "Bearer " + token);
            }
            ligacao.setConnectTimeout(TEMPO_LIGACAO_MS);
            ligacao.setReadTimeout(TEMPO_LEITURA_MS);
            if (corpo != null) {
                ligacao.setDoOutput(true);
                escreverCorpo(ligacao.getOutputStream(), corpo);
            }

            int codigo = ligacao.getResponseCode();
            // em caso de erro (fora de 200-299) a resposta vem no errorStream
            InputStream entrada = (codigo >= 200 && codigo < 300)
                    ? ligacao.getInputStream()
                    : ligacao.getErrorStream();
            String texto = (entrada != null) ? lerCorpo(entrada) : "";
            return new Resposta(codigo, texto);
        } finally {
            if (ligacao != null) {
                ligacao.disconnect();
            }
        }
    }

    private static String lerCorpo(InputStream entrada) throws IOException {
        StringBuilder texto = new StringBuilder();
        try (BufferedReader leitor = new BufferedReader(new InputStreamReader(entrada, "UTF-8"))) {
            String linha = leitor.readLine();
            while (linha != null) {
                texto.append(linha);
                linha = leitor.readLine();
            }
        }
        return texto.toString();
    }

    private static void escreverCorpo(OutputStream saida, String corpo) throws IOException {
        try (OutputStream s = saida) {
            s.write(corpo.getBytes("UTF-8"));
            s.flush();
        }
    }
}
