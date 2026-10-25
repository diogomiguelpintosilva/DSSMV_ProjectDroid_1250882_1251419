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
 * para o utilizador os ver na app Fotos/Galeria e os poder partilhar. Chamar numa worker thread.
 */
public final class Galeria {

    private static final String PASTA = "WayLine";

    private Galeria() {
    }

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
            try (InputStream in = new FileInputStream(origem);
                 OutputStream out = resolver.openOutputStream(destino)) {
                if (out == null) {
                    throw new IOException("Não foi possível abrir o destino.");
                }
                copiar(in, out);
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
            try (InputStream in = new FileInputStream(origem);
                 OutputStream out = new FileOutputStream(destino)) {
                copiar(in, out);
            }
            MediaScannerConnection.scanFile(ctx, new String[]{destino.getAbsolutePath()},
                    new String[]{"video/mp4"}, null);
        }
    }

    private static void copiar(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        int lidos;
        while ((lidos = in.read(buffer)) != -1) {
            out.write(buffer, 0, lidos);
        }
        out.flush();
    }
}
