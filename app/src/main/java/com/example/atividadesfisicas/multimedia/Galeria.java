package com.example.atividadesfisicas.multimedia;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Copia ficheiros da pasta privada da app para a galeria do telemóvel (pasta Filmes/WayLine),
 * para o utilizador os ver na app Fotos/Galeria e os poder partilhar. Chamar numa thread de fundo.
 */
public final class Galeria {

    private static final String PASTA = "WayLine";

    private Galeria() {
    }

    /** Copia o vídeo para a galeria com o nome indicado. */
    public static void guardarVideo(Context ctx, File origem, String nome) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 ou superior: MediaStore, sem pedir permissão
            ContentResolver resolver = ctx.getContentResolver();
            ContentValues valores = new ContentValues();
            valores.put(MediaStore.MediaColumns.DISPLAY_NAME, nome);
            valores.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
            valores.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/" + PASTA);
            valores.put(MediaStore.MediaColumns.IS_PENDING, 1);
            Uri destino = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, valores);
            if (destino == null) {
                throw new IOException("Não foi possível criar o ficheiro na galeria.");
            }
            try (InputStream entrada = new FileInputStream(origem);
                 OutputStream saida = resolver.openOutputStream(destino)) {
                if (saida == null) {
                    throw new IOException("Não foi possível abrir o destino.");
                }
                copiar(entrada, saida);
            } catch (IOException e) {
                resolver.delete(destino, null, null);
                throw e;
            }
            valores.clear();
            valores.put(MediaStore.MediaColumns.IS_PENDING, 0);
            resolver.update(destino, valores, null, null);
        } else {
            // Android 7 a 9: copia para a pasta pública Filmes (precisa de WRITE_EXTERNAL_STORAGE)
            File pasta = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), PASTA);
            if (!pasta.exists() && !pasta.mkdirs()) {
                throw new IOException("Não foi possível criar a pasta Filmes/" + PASTA + ".");
            }
            File destino = new File(pasta, nome);
            try (InputStream entrada = new FileInputStream(origem);
                 OutputStream saida = new FileOutputStream(destino)) {
                copiar(entrada, saida);
            }
            MediaScannerConnection.scanFile(ctx, new String[]{destino.getAbsolutePath()},
                    new String[]{"video/mp4"}, null);
        }
    }

    /** Copia em blocos de 64 KB, para não carregar o vídeo inteiro para a memória. */
    private static void copiar(InputStream entrada, OutputStream saida) throws IOException {
        byte[] bloco = new byte[64 * 1024];
        int lidos;
        while ((lidos = entrada.read(bloco)) != -1) {
            saida.write(bloco, 0, lidos);
        }
        saida.flush();
    }
}
