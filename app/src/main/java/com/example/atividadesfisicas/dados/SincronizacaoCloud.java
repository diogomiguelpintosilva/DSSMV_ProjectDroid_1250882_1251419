package com.example.atividadesfisicas.dados;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;

/**
 * Sincroniza as rotas criadas e o histórico com o Firestore, numa thread de fundo.
 * Se não houver rede (ou o Firestore não estiver configurado), os dados locais continuam a funcionar.
 */
public final class SincronizacaoCloud {

    private SincronizacaoCloud() {
    }

    /** @param aoTerminar corre na thread principal quando acaba (pode ser null). */
    public static void sincronizar(Context ctx, final Runnable aoTerminar) {
        final Context app = ctx.getApplicationContext();
        final Handler principal = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                RotasGuardadas.sincronizar(app);
            } catch (IOException e) {
                // continua com os dados locais
            }
            try {
                HistoricoGuardado.sincronizar(app);
            } catch (IOException e) {
                // continua com os dados locais
            }
            if (aoTerminar != null) {
                principal.post(aoTerminar);
            }
        }).start();
    }
}
