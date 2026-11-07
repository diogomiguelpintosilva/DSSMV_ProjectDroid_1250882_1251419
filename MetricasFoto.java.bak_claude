package com.example.atividadesfisicas.multimedia;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;

import com.example.atividadesfisicas.modelo.Telemetria;
import com.example.atividadesfisicas.utilitarios.Config;
import com.example.atividadesfisicas.utilitarios.Formatacao;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * UC7: desenha na foto as métricas do momento (velocidade e altitude) e volta a guardá-la.
 * Também corrige a rotação (EXIF), para a foto ficar sempre "em pé" (importante para o vídeo resumo).
 * Mantém a resolução da câmara (até Config.FOTO_LADO_MAXIMO) e grava em JPEG de alta qualidade.
 * Chamar numa thread de fundo.
 */
public final class MetricasFoto {

    private static final int QUALIDADE_JPEG = 95;

    private MetricasFoto() {
    }

    /** Desenha as métricas na foto. Se faltar memória, lança IOException (a foto fica sem métricas). */
    public static void aplicar(File ficheiro, Telemetria telemetria) throws IOException {
        try {
            aplicarInterno(ficheiro, telemetria);
        } catch (OutOfMemoryError e) {
            throw new IOException("Sem memória para processar a foto.");
        }
    }

    private static void aplicarInterno(File ficheiro, Telemetria telemetria) throws IOException {
        String caminho = ficheiro.getAbsolutePath();

        // lê só as dimensões, para saber quanto reduzir antes de carregar a imagem
        BitmapFactory.Options limites = new BitmapFactory.Options();
        limites.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(caminho, limites);
        int amostragem = 1;
        while (Math.max(limites.outWidth, limites.outHeight) / amostragem > Config.FOTO_LADO_MAXIMO) {
            amostragem *= 2;
        }

        BitmapFactory.Options opcoes = new BitmapFactory.Options();
        opcoes.inSampleSize = amostragem;
        opcoes.inMutable = true;
        opcoes.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap imagem = BitmapFactory.decodeFile(caminho, opcoes);
        if (imagem == null) {
            throw new IOException("Não foi possível ler a foto.");
        }

        int rotacao = lerRotacao(caminho);
        if (rotacao != 0) {
            Matrix matriz = new Matrix();
            matriz.postRotate(rotacao);
            Bitmap rodada = Bitmap.createBitmap(imagem, 0, 0, imagem.getWidth(), imagem.getHeight(), matriz, true);
            if (rodada != imagem) {
                imagem.recycle();
            }
            imagem = rodada;
        }
        if (!imagem.isMutable()) {
            Bitmap copia = imagem.copy(Bitmap.Config.ARGB_8888, true);
            imagem.recycle();
            imagem = copia;
        }

        desenharTexto(new Canvas(imagem), imagem.getWidth(), imagem.getHeight(), telemetria);

        try (FileOutputStream saida = new FileOutputStream(ficheiro)) {
            imagem.compress(Bitmap.CompressFormat.JPEG, QUALIDADE_JPEG, saida);
            saida.flush();
        } finally {
            imagem.recycle();
        }
    }

    /** Texto branco sobre um retângulo semitransparente, no canto inferior esquerdo. */
    private static void desenharTexto(Canvas tela, int largura, int altura, Telemetria telemetria) {
        String texto = Formatacao.textoMetricas(telemetria);
        Paint tintaTexto = new Paint(Paint.ANTI_ALIAS_FLAG);
        tintaTexto.setColor(Color.WHITE);
        tintaTexto.setTextSize(largura * 0.045f);
        Paint tintaFundo = new Paint();
        tintaFundo.setColor(0x99000000);
        float margem = largura * 0.025f;
        float larguraTexto = tintaTexto.measureText(texto);
        float alturaTexto = tintaTexto.getTextSize();
        float esquerda = margem;
        float fundoBaixo = altura - margem;
        float fundoTopo = fundoBaixo - alturaTexto - margem;
        tela.drawRect(esquerda, fundoTopo, esquerda + larguraTexto + 2 * margem, fundoBaixo, tintaFundo);
        tela.drawText(texto, esquerda + margem, fundoBaixo - margem * 0.8f, tintaTexto);
    }

    /** Rotação indicada nos metadados EXIF da foto, em graus. */
    private static int lerRotacao(String caminho) {
        try {
            ExifInterface exif = new ExifInterface(caminho);
            int orientacao = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientacao) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (IOException e) {
            return 0;
        }
    }
}
