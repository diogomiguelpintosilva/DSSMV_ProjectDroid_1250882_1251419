package com.example.atividadesfisicas.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Pedidos HTTP simples
 * Tem de ser chamado a partir de uma worker thread, nunca da UI thread.*/
public final class HttpUtil {

    private HttpUtil() {
    }

    public static class Resposta {
        public final int codigo;
        public final String corpo;

        public Resposta(int codigo, String corpo) {
            this.codigo = codigo;
            this.corpo = corpo;
        }
    }

    /** POST com corpo JSON. Devolve o código e o corpo (também em caso de erro HTTP, ex.: 400). */
    public static Resposta postJson(String urlStr, String json) throws IOException {
        HttpURLConnection httpConn = null;
        try {
            URL url = new URL(urlStr);
            URLConnection urlConn = url.openConnection();
            if (!(urlConn instanceof HttpURLConnection)) {
                throw new IOException("URL is not an Http URL");
            }
            httpConn = (HttpURLConnection) urlConn;
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            httpConn.setConnectTimeout(10000);
            httpConn.setReadTimeout(10000);
            httpConn.setDoOutput(true);

            OutputStream out = httpConn.getOutputStream();
            writeBody(out, json);

            int resCode = httpConn.getResponseCode();
            InputStream in = (resCode >= 200 && resCode < 300)
                    ? httpConn.getInputStream()
                    : httpConn.getErrorStream();
            String body = (in != null) ? readBody(in) : "";
            return new Resposta(resCode, body);
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
    }

    private static String readBody(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        String read = br.readLine();
        while (read != null) {
            sb.append(read);
            read = br.readLine();
        }
        br.close();
        return sb.toString();
    }

    private static void writeBody(OutputStream writer, String body) throws IOException {
        byte[] dataBytes = body.getBytes("UTF-8");
        writer.write(dataBytes);
        writer.flush();
        writer.close();
    }
}
