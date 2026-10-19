package com.example.atividadesfisicas.media;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import com.example.atividadesfisicas.model.Telemetria;
import com.example.atividadesfisicas.util.FormatUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
/**
 * UC7: desenha na foto as métricas do momento (velocidade e
 altitude) e volta a guardá-la.
 * Também corrige a rotação (EXIF), para a foto ficar sempre "em pé"
 (importante para o vídeo resumo).
 * Chamar numa worker thread.
 */
public final class FotoOverlay {
    private static final int LADO_MAXIMO = 2048;
    private FotoOverlay() {
    }
    public static void aplicar(File ficheiro, Telemetria telemetria)
            throws IOException {
        String caminho = ficheiro.getAbsolutePath();
        BitmapFactory.Options limites = new BitmapFactory.Options();
        limites.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(caminho, limites);
        int amostragem = 1;
        while (Math.max(limites.outWidth, limites.outHeight) / amostragem > LADO_MAXIMO) {
            amostragem *= 2;
        }
        BitmapFactory.Options opcoes = new BitmapFactory.Options();
        opcoes.inSampleSize = amostragem;
        Bitmap original = BitmapFactory.decodeFile(caminho, opcoes);
        if (original == null) {
            throw new IOException("Não foi possível ler a foto.");
        }
        int rotacao = lerRotacao(caminho);
        Bitmap rodada = original;
        if (rotacao != 0) {
            Matrix matriz = new Matrix();
            matriz.postRotate(rotacao);
            rodada = Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matriz, true);
        }
        Bitmap resultado = rodada.copy(Bitmap.Config.ARGB_8888, true);
        desenharTexto(new Canvas(resultado), resultado.getWidth(), resultado.getHeight(), telemetria);
        FileOutputStream out = new FileOutputStream(ficheiro);
        try {
            resultado.compress(Bitmap.CompressFormat.JPEG, 90, out);out.flush();
        } finally {
            out.close();
        }
    }
    private static void desenharTexto(Canvas canvas, int largura, int altura, Telemetria t) {
        String texto;
        if (t == null) {
            texto = "Vel: -- | Alt: -- ";
        } else {
            texto = "Vel: " + FormatUtil.formatarVelocidade(t.getVelocidadeKmh()) + " | Alt: " + FormatUtil.formatarAltitude(t.getAltitude());
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
            int orientacao =
                    exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL);
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