package com.example.atividadesfisicas.location;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.example.atividadesfisicas.util.PermissoesHelper;

/**
 * Vibração e notificações (aviso de desvio da rota e notificação do serviço em primeiro plano).
 */
public final class AlertaHelper {

    public static final String CANAL_ATIVIDADE = "canal_atividade";
    public static final String CANAL_ALERTAS = "canal_alertas";
    public static final int ID_NOTIFICACAO_SERVICO = 1001;
    private static final int ID_NOTIFICACAO_DESVIO = 1002;

    private AlertaHelper() {
    }

    public static void criarCanais(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm == null) {
                return;
            }
            nm.createNotificationChannel(new NotificationChannel(
                    CANAL_ATIVIDADE,
                    "Atividade em curso",
                    NotificationManager.IMPORTANCE_LOW
            ));
            nm.createNotificationChannel(new NotificationChannel(
                    CANAL_ALERTAS,
                    "Alertas de desvio",
                    NotificationManager.IMPORTANCE_HIGH
            ));
        }
    }

    /**
     * Notificação obrigatória do serviço em primeiro plano (GPS).
     */
    public static Notification criarNotificacaoServico(Context ctx) {
        return new NotificationCompat.Builder(ctx, CANAL_ATIVIDADE)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("Atividade em curso")
                .setContentText("A registar o percurso com GPS")
                .setOngoing(true)
                .build();
    }

    /**
     * Notificação de desvio da rota (só aparece se a permissão de notificações foi concedida).
     */
    @SuppressLint("MissingPermission")
    public static void notificarDesvio(Context ctx) {
        if (!PermissoesHelper.temNotificacoes(ctx)) {
            return;
        }
        Notification n = new NotificationCompat.Builder(ctx, CANAL_ALERTAS)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Desvio da rota")
                .setContentText("Está a desviar-se da rota pretendida.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build();
        NotificationManagerCompat.from(ctx).notify(ID_NOTIFICACAO_DESVIO, n);
    }

    /**
     * Faz o telemóvel vibrar (três pulsos).
     */
    @SuppressWarnings("deprecation")
    public static void vibrar(Context ctx) {
        Vibrator vibrator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = (vm != null) ? vm.getDefaultVibrator() : null;
        } else {
            vibrator = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        }
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }
        long[] padrao = {0, 400, 200, 400, 200, 400};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(padrao, -1));
        } else {
            vibrator.vibrate(padrao, -1);
        }
    }
}