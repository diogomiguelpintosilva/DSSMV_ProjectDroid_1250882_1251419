package com.example.atividadesfisicas.utilitarios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Testa PedidosHttp contra um servidor HTTP falso, feito só com ServerSocket (java.net),
 * a correr numa thread da própria JVM em localhost. Não usa a internet nem o Firebase.
 */
public class PedidosHttpTest {

    private ServerSocket socketServidor;
    private String base;

    // o que o servidor deve responder (cada teste pode mudar antes de fazer o pedido)
    private volatile int codigoResposta = 200;
    private volatile String corpoResposta = "{\"ola\":\"mundo\"}";

    // o que o servidor recebeu no último pedido
    private volatile String corpoRecebido;
    private volatile String contentTypeRecebido;
    private volatile String authorizationRecebido;

    @Before
    public void arrancarServidor() throws IOException {
        socketServidor = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        base = "http://127.0.0.1:" + socketServidor.getLocalPort();
        Thread fio = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!socketServidor.isClosed()) {
                    try (Socket ligacao = socketServidor.accept()) {
                        atender(ligacao);
                    } catch (IOException e) {
                        // servidor fechado no fim do teste
                    }
                }
            }
        });
        fio.setDaemon(true);
        fio.start();
    }

    @After
    public void pararServidor() throws IOException {
        socketServidor.close();
    }

    /** Lê um pedido HTTP, guarda o que interessa e responde com codigoResposta/corpoResposta. */
    private void atender(Socket ligacao) throws IOException {
        InputStream entrada = ligacao.getInputStream();

        // 1) cabeçalhos: lê byte a byte até aparecer a linha vazia (\r\n\r\n)
        ByteArrayOutputStream bytesCabecalho = new ByteArrayOutputStream();
        String cabecalho = "";
        int b;
        while ((b = entrada.read()) != -1) {
            bytesCabecalho.write(b);
            cabecalho = new String(bytesCabecalho.toByteArray(), StandardCharsets.ISO_8859_1);
            if (cabecalho.endsWith("\r\n\r\n")) {
                break;
            }
        }
        int tamanho = 0;
        String contentType = null;
        String authorization = null;
        for (String linha : cabecalho.split("\r\n")) {
            String minuscula = linha.toLowerCase(Locale.ROOT);
            if (minuscula.startsWith("content-length:")) {
                tamanho = Integer.parseInt(linha.substring(linha.indexOf(':') + 1).trim());
            } else if (minuscula.startsWith("content-type:")) {
                contentType = linha.substring(linha.indexOf(':') + 1).trim();
            } else if (minuscula.startsWith("authorization:")) {
                authorization = linha.substring(linha.indexOf(':') + 1).trim();
            }
        }

        // 2) corpo: lê exatamente "tamanho" bytes
        byte[] corpo = new byte[tamanho];
        int lidos = 0;
        while (lidos < tamanho) {
            int n = entrada.read(corpo, lidos, tamanho - lidos);
            if (n == -1) {
                break;
            }
            lidos += n;
        }
        corpoRecebido = new String(corpo, 0, lidos, StandardCharsets.UTF_8);
        contentTypeRecebido = contentType;
        authorizationRecebido = authorization;

        // 3) resposta
        byte[] resposta = corpoResposta.getBytes(StandardCharsets.UTF_8);
        String cabecalhoResposta = "HTTP/1.1 " + codigoResposta + " Resposta\r\n"
                + "Content-Type: application/json; charset=UTF-8\r\n"
                + "Content-Length: " + resposta.length + "\r\n"
                + "Connection: close\r\n\r\n";
        OutputStream saida = ligacao.getOutputStream();
        saida.write(cabecalhoResposta.getBytes(StandardCharsets.ISO_8859_1));
        saida.write(resposta);
        saida.flush();
    }

    @Test
    public void get_devolveCodigoECorpo() throws IOException {
        PedidosHttp.Resposta r = PedidosHttp.pedido("GET", base + "/ok", null, null, null);
        assertEquals(200, r.codigo);
        assertEquals("{\"ola\":\"mundo\"}", r.corpo);
    }

    @Test
    public void postJson_enviaOCorpoEOContentType() throws IOException {
        PedidosHttp.postJson(base + "/ok", "{\"a\":1}");
        assertEquals("{\"a\":1}", corpoRecebido);
        assertTrue(contentTypeRecebido.startsWith("application/json"));
    }

    @Test
    public void comToken_enviaCabecalhoAuthorizationBearer() throws IOException {
        PedidosHttp.pedido("GET", base + "/ok", null, null, "abc123");
        assertEquals("Bearer abc123", authorizationRecebido);
    }

    @Test
    public void erroHttp_devolveOCodigoEOCorpoDoErro() throws IOException {
        codigoResposta = 400;
        corpoResposta = "{\"error\":{\"message\":\"EMAIL_EXISTS\"}}";
        PedidosHttp.Resposta r = PedidosHttp.postJson(base + "/erro", "{}");
        assertEquals(400, r.codigo);
        assertEquals("{\"error\":{\"message\":\"EMAIL_EXISTS\"}}", r.corpo);
    }

    @Test(expected = IOException.class)
    public void enderecoQueNaoEHttp_lancaIOException() throws IOException {
        PedidosHttp.pedido("GET", "file:///tmp/qualquer", null, null, null);
    }
}