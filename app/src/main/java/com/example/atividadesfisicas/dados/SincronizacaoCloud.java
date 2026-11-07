package com.example.atividadesfisicas.dados;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;

/*
 Sincroniza as rotas criadas e o histórico com o Firestore, numa thread de fundo.
 Se não houver rede (ou o Firestore não estiver configurado), os dados locais continuam a funcionar.
 */
public final class SincronizacaoCloud {

    private SincronizacaoCloud() {
    }

    //pôr cloud e dispositivo com os mesmos dados
    /*
    ctx -> ecrâ que está a chamar a+o metodo
    aoTerminar -> chamar, ou não, outro metodo, quando acaba de sincronizar
    */
    public static void sincronizar(Context ctx, final Runnable aoTerminar) {
        final Context app = ctx.getApplicationContext();
        final Handler principal = new Handler(Looper.getMainLooper()); //cria handler, que vai poder comunicar com a fila da thread e acrescentar "tarefas"
        new Thread(() -> {                                  //getMainLooper devolve fila da thread principal
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
            if (aoTerminar != null) { //se houver algo a realizar no final, é entregue aqui
                principal.post(aoTerminar);
            }
        }).start(); //fim da thread, nao se guardou em variável
                    //resto da sincronização acontece noutra thread
    }
}
