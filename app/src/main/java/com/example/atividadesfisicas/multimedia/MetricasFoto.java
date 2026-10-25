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
 * Chamar numa worker thread.
 */
public final class MetricasFoto {

    private static final int QUALIDADE_JPEG = 95;

    private MetricasFoto() {
    }

    public static void aplicar(File ficheiro, Telemetria telemetria) throws IOException {
        try {
            aplicarInterno(ficheiro, telemetria);
        } catch (OutOfMemoryError e) {
            throw new IOException("Sem memória para processar a foto.");
        }
    }

    private static void aplicarInterno(File ficheiro, Telemetria telemetria) throws IOException {
        String caminho = ficheiro.getAbsolutePath();
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
        Bitmap bitmap = BitmapFactory.decodeFile(caminho, opcoes);
        if (bitmap == null) {
            throw new IOException("Não foi possível ler a foto.");
        }

        int rotacao = lerRotacao(caminho);
        if (rotacao != 0) {
            Matrix matriz = new Matrix();
            matriz.postRotate(rotacao);
            Bitmap rodada = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matriz, true);
            if (rodada != bitmap) {
                bitmap.recycle();
            }
            bitmap = rodada;
        }
        if (!bitmap.isMutable()) {
            Bitmap copia = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            bitmap.recycle();
            bitmap = copia;
        }

        desenharTexto(new Canvas(bitmap), bitmap.getWidth(), bitmap.getHeight(), telemetria);

        FileOutputStream out = new FileOutputStream(ficheiro);
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, QUALIDADE_JPEG, out);
            out.flush();
        } finally {
            out.close();
            bitmap.recycle();
        }
    }

    private static void desenharTexto(Canvas canvas, int largura, int altura, Telemetria t) {
        String texto;
        if (t == null) {
            texto = "Vel: -- | Alt: -- ";
        } else {
            texto = "Vel: " + Formatacao.formatarVelocidade(t.getVelocidadeKmh()) + " | Alt: " + Formatacao.formatarAltitude(t.getAltitude());
        }
        Paint tinta = new Paint(Paint.ANTI_ALIAS_FLAG);
        tinta.setColor(Color.WHITE);
        tinta.setTextSize(largura * 0.045f);
        Paint fundo = new Paint();
        fundo.setColor(0x99000000);
        float margem = largura * 0.025f;
        float larguraTexto = tinta.measureText(texto);
        float alturaTexto = tinta.getTextSize();
        float esquerda = margem;
        float fundoBaixo = altura - margem;
        float fundoTopo = fundoBaixo - alturaTexto - margem;
        canvas.drawRect(esquerda, fundoTopo, esquerda + larguraTexto + 2 * margem, fundoBaixo, fundo);
        canvas.drawText(texto, esquerda + margem, fundoBaixo - margem * 0.8f, tinta);
    }

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
