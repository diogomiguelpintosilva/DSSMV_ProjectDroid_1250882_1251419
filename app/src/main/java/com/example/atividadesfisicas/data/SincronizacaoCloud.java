package com.example.atividadesfisicas.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;

/**
 * Sincroniza rotas criadas e histórico com o Firestore, numa worker thread.
 * Se não houver rede (ou o Firestore não estiver configurado), os dados locais continuam a funcionar.
 */
public final class SincronizacaoCloud {

    private SincronizacaoCloud() {
    }

    /** @param aoTerminar corre na UI thread quando acaba (pode ser null). */
    public static void sincronizar(Context ctx, final Runnable aoTerminar) {
        final Context app = ctx.getApplicationContext();
        final Handler principal = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                RotaRepository.sincronizar(app);
            } catch (IOException e) {
                // continua com os dados locais
            }
            try {
                HistoricoRepository.sincronizar(app);
            } catch (IOException e) {
                // continua com os dados locais
            }
            if (aoTerminar != null) {
                principal.post(aoTerminar);
            }
        }).start();
    }
}
