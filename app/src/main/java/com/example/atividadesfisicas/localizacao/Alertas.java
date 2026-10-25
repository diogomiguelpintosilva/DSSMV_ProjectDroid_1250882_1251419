package com.example.atividadesfisicas.localizacao;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.example.atividadesfisicas.ecras.ExercicioActivity;
import com.example.atividadesfisicas.utilitarios.Permissoes;

/**
 * Vibração e notificações (aviso de desvio da rota, lembretes multimédia e notificação do serviço
 * em primeiro plano).
 */
public final class Alertas {

    public static final String CANAL_ATIVIDADE = "canal_atividade";
    public static final String CANAL_ALERTAS = "canal_alertas";
    public static final String CANAL_LEMBRETES = "canal_lembretes";
    public static final int ID_NOTIFICACAO_SERVICO = 1001;
    private static final int ID_NOTIFICACAO_DESVIO = 1002;
    private static final int ID_NOTIFICACAO_LEMBRETE = 1003;

    private Alertas() {
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
            nm.createNotificationChannel(new NotificationChannel(
                    CANAL_LEMBRETES,
                    "Lembretes de foto e vídeo",
                    NotificationManager.IMPORTANCE_HIGH
            ));
        }
    }

    /** Abre o ecrã da atividade em curso ao tocar numa notificação. */
    private static PendingIntent abrirAtividade(Context ctx) {
        Intent intent = new Intent(ctx, ExercicioActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(ctx, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * Notificação obrigatória do serviço em primeiro plano (GPS).
     */
    public static Notification criarNotificacaoServico(Context ctx) {
        return new NotificationCompat.Builder(ctx, CANAL_ATIVIDADE)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("Atividade em curso")
                .setContentText("A registar o percurso com GPS")
                .setContentIntent(abrirAtividade(ctx))
                .setOngoing(true)
                .build();
    }

    /**
     * Notificação de desvio da rota (só aparece se a permissão de notificações foi concedida).
     */
    @SuppressLint("MissingPermission")
    public static void notificarDesvio(Context ctx) {
        if (!Permissoes.temNotificacoes(ctx)) {
            return;
        }
        Notification n = new NotificationCompat.Builder(ctx, CANAL_ALERTAS)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Desvio da rota")
                .setContentText("Está a desviar-se da rota pretendida.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(abrirAtividade(ctx))
                .setAutoCancel(true)
                .build();
        NotificationManagerCompat.from(ctx).notify(ID_NOTIFICACAO_DESVIO, n);
    }

    /**
     * Notificação periódica para o utilizador não se esquecer de tirar foto ou gravar vídeo (UC11).
     */
    @SuppressLint("MissingPermission")
    public static void notificarLembrete(Context ctx) {
        if (!Permissoes.temNotificacoes(ctx)) {
            return;
        }
        Notification n = new NotificationCompat.Builder(ctx, CANAL_LEMBRETES)
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentTitle("Não se esqueça de registar o momento!")
                .setContentText("Tire uma foto ou grave um vídeo para o vídeo resumo da atividade.")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Tire uma foto ou grave um vídeo para o vídeo resumo da atividade."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(abrirAtividade(ctx))
                .setAutoCancel(true)
                .build();
        NotificationManagerCompat.from(ctx).notify(ID_NOTIFICACAO_LEMBRETE, n);
    }

    /**
     * Faz o telemóvel vibrar (três pulsos) quando o utilizador se desvia da rota.
     */
    public static void vibrar(Context ctx) {
        vibrarPadrao(ctx, new long[]{0, 400, 200, 400, 200, 400});
    }

    /**
     * Vibração curta (dois pulsos) que acompanha os lembretes multimédia.
     */
    public static void vibrarCurto(Context ctx) {
        vibrarPadrao(ctx, new long[]{0, 250, 150, 250});
    }

    /**
     * A vibração é marcada como "alarme" para não ser silenciada pelos modos silencioso/não incomodar.
     */
    @SuppressWarnings("deprecation")
    private static void vibrarPadrao(Context ctx, long[] padrao) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes atributos = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            vibrator.vibrate(VibrationEffect.createWaveform(padrao, -1), atributos);
        } else {
            vibrator.vibrate(padrao, -1);
        }
    }
}
