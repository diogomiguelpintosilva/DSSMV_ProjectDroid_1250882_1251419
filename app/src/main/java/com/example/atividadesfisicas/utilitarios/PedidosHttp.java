package com.example.atividadesfisicas.utilitarios;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/*
 Pedidos HTTP simples (login e Firestore), com HttpURLConnection.
 Tem de ser chamado a partir de uma thread de fundo, nunca da thread principal.
 */
public final class PedidosHttp {

    //ATRIBUTOS
    private static final int TEMPO_LIGACAO_MS = 10000;
    private static final int TEMPO_LEITURA_MS = 15000;

    private PedidosHttp() {
    }

    //Resultado de um pedido: o código HTTP e o texto devolvido.
    public static class Resposta {
        public final int codigo;
        public final String corpo;

        public Resposta(int codigo, String corpo) {
            this.codigo = codigo;
            this.corpo = corpo;
        }
    }

    //POST com corpo JSON. Devolve o código e o corpo (também em caso de erro HTTP, ex.: 400).
    //atalho para pedidos POST, que tem sempre os campos "metodo" e tipoCorpo" iguais
    public static Resposta postJson(String endereco, String json) throws IOException {
        return pedido("POST", endereco, json, "application/json; charset=UTF-8", null);
    }
/*
 METODO    -> a ação que se pede ao servidor: POST (enviar dados), GET (ler dados) ou DELETE (apagar)
 ENDERECO  -> o endereço (URL) a que se faz o pedido, por exemplo o do Firebase
 CORPO     -> os dados enviados ao servidor, em texto (por exemplo, JSON); null se não houver (GET e DELETE)
 TIPOCORPO -> o formato do corpo, para o servidor o saber ler (por exemplo, application/json); null se não houver corpo
 TOKEN     -> o "passe" do utilizador, enviado no cabeçalho Authorization para o servidor saber quem pede; null se não for preciso (login)
 */

    public static Resposta pedido(String metodo, String endereco, String corpo, String tipoCorpo,
                                  String token) throws IOException {
        HttpURLConnection ligacao = null; //variavel q vai guardar a ligação, variável q vem com o java. Representa uma ligação a um endereço HTTP
        try {
            URLConnection ligacaoUrl = new URL(endereco).openConnection();//converte texto em URL e prepara aligação a esse endereço
            if (!(ligacaoUrl instanceof HttpURLConnection)) { //instanceof pergunta o tipo do obj (http,file,ftp) e verifica se é mesmo HTTP
                throw new IOException("O endereço não é HTTP: " + endereco); //lança erro se n for http
            }
            ligacao = (HttpURLConnection) ligacaoUrl; //já garantindo q é HTTP
            ligacao.setRequestMethod(metodo); //define o metodo
            ligacao.setRequestProperty("User-Agent", Config.USER_AGENT_MAPA);
            if (tipoCorpo != null) {
                ligacao.setRequestProperty("Content-Type", tipoCorpo); //verifica tipoCorpo e token e junta ambos ao pedido
            }
            if (token != null) {
                ligacao.setRequestProperty("Authorization", "Bearer " + token);
            }
            ligacao.setConnectTimeout(TEMPO_LIGACAO_MS);
            ligacao.setReadTimeout(TEMPO_LEITURA_MS); //definir limites de tempo de espera
            if (corpo != null) {
                ligacao.setDoOutput(true);
                escreverCorpo(ligacao.getOutputStream(), corpo);
            }

            int codigo = ligacao.getResponseCode(); //pedido enviado e resposta é recebida, espera-se pelo servidor(corre noutra thread)
            // em caso de erro (fora de 200-299) a resposta vem no errorStream
            InputStream entrada = (codigo >= 200 && codigo < 300)
                    ? ligacao.getInputStream() //Se (condição), então esta linha. PEdido correu bem
                    : ligacao.getErrorStream(); //Senão isto. Pedido correu mal.
            String texto = (entrada != null) ? lerCorpo(entrada) : ""; //se houver entrada lerCorpo, senão null
            return new Resposta(codigo, texto);
        } finally { //corre sempre,quer tenha dado erro o try ou não.
            if (ligacao != null) {
                ligacao.disconnect(); //desligar a conexão
            }
        }
    }

    //lê a resposta q o servidor devolve
    private static String lerCorpo(InputStream entrada) throws IOException {
        StringBuilder texto = new StringBuilder(); //pode ir se acrescentando pedaçoes de texto
        try (BufferedReader leitor = new BufferedReader(new InputStreamReader(entrada, "UTF-8"))) { //lê bytes e converte em texto, guardando num buffer
            String linha = leitor.readLine(); //lê linha do buffer sem o /n
            while (linha != null) { //enquanto houver linha
                texto.append(linha); //juntar ao texto
                linha = leitor.readLine(); //ler a seguinte
            }
        }
        return texto.toString(); //converte o StringBuilder numa string normal e devolve
    }

    //enviar dados para o servidor
    private static void escreverCorpo(OutputStream saida, String corpo) throws IOException {
        try (OutputStream s = saida) {
            s.write(corpo.getBytes("UTF-8"));//converte texto em bytes. UTF-8 é usado para codificação de acentos/caracteres especiais
            s.flush(); //envio direto de todos os dados, evitando que fiquem presos em algum buffer
        }
    }
}
