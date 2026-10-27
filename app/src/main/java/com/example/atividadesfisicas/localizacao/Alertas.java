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

import com.example.atividadesfisicas.R;
import com.example.atividadesfisicas.ecras.ExercicioActivity;
import com.example.atividadesfisicas.utilitarios.Permissoes;

/**
 * Vibração e notificações: notificação fixa do serviço de GPS, aviso de desvio da rota (UC10)
 * e lembretes multimédia (UC11).
 */
public final class Alertas {

    public static final String CANAL_ATIVIDADE = "canal_atividade";
    public static final String CANAL_ALERTAS = "canal_alertas";
    public static final String CANAL_LEMBRETES = "canal_lembretes";
    public static final int ID_NOTIFICACAO_SERVICO = 1001;
    private static final int ID_NOTIFICACAO_DESVIO = 1002;
    private static final int ID_NOTIFICACAO_LEMBRETE = 1003;

    /** Vibração do desvio: três pulsos longos (pares "espera, vibra", em milissegundos). */
    private static final long[] VIBRACAO_DESVIO = {0, 400, 200, 400, 200, 400};
    /** Vibração do lembrete: dois pulsos curtos. */
    private static final long[] VIBRACAO_LEMBRETE = {0, 250, 150, 250};

    private Alertas() {
    }

    /** Cria os canais de notificação (obrigatórios a partir do Android 8). */
    public static void criarCanais(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager gestorNotificacoes = ctx.getSystemService(NotificationManager.class);
            if (gestorNotificacoes == null) {
                return;
            }
            gestorNotificacoes.createNotificationChannel(new NotificationChannel(CANAL_ATIVIDADE,
                    ctx.getString(R.string.alertas_canal_atividade), NotificationManager.IMPORTANCE_LOW));
            gestorNotificacoes.createNotificationChannel(new NotificationChannel(CANAL_ALERTAS,
                    ctx.getString(R.string.alertas_canal_desvio), NotificationManager.IMPORTANCE_HIGH));
            gestorNotificacoes.createNotificationChannel(new NotificationChannel(CANAL_LEMBRETES,
                    ctx.getString(R.string.alertas_canal_lembretes), NotificationManager.IMPORTANCE_HIGH));
        }
    }

    /** Ao tocar numa notificação, abre o ecrã do exercício em curso. */
    private static PendingIntent abrirExercicio(Context ctx) {
        Intent intent = new Intent(ctx, ExercicioActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(ctx, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Notificação obrigatória do serviço em primeiro plano (GPS). */
    public static Notification criarNotificacaoServico(Context ctx) {
        return new NotificationCompat.Builder(ctx, CANAL_ATIVIDADE)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle(ctx.getString(R.string.alertas_servico_titulo))
                .setContentText(ctx.getString(R.string.alertas_servico_texto))
                .setContentIntent(abrirExercicio(ctx))
                .setOngoing(true)
                .build();
    }

    /** UC10: notificação de desvio da rota (só aparece se a permissão de notificações foi concedida). */
    @SuppressLint("MissingPermission")
    public static void notificarDesvio(Context ctx) {
        if (!Permissoes.temNotificacoes(ctx)) {
            return;
        }
        Notification notificacao = new NotificationCompat.Builder(ctx, CANAL_ALERTAS)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(ctx.getString(R.string.alertas_desvio_titulo))
                .setContentText(ctx.getString(R.string.alertas_desvio_texto))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(abrirExercicio(ctx))
                .setAutoCancel(true)
                .build();
        NotificationManagerCompat.from(ctx).notify(ID_NOTIFICACAO_DESVIO, notificacao);
    }

    /** UC11: notificação periódica para o utilizador não se esquecer de tirar foto ou gravar vídeo. */
    @SuppressLint("MissingPermission")
    public static void notificarLembrete(Context ctx) {
        if (!Permissoes.temNotificacoes(ctx)) {
            return;
        }
        String texto = ctx.getString(R.string.alertas_lembrete_texto);
        Notification notificacao = new NotificationCompat.Builder(ctx, CANAL_LEMBRETES)
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentTitle(ctx.getString(R.string.alertas_lembrete_titulo))
                .setContentText(texto)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(abrirExercicio(ctx))
                .setAutoCancel(true)
                .build();
        NotificationManagerCompat.from(ctx).notify(ID_NOTIFICACAO_LEMBRETE, notificacao);
    }

    /** Vibração do desvio da rota. */
    public static void vibrar(Context ctx) {
        vibrarPadrao(ctx, VIBRACAO_DESVIO);
    }

    /** Vibração que acompanha os lembretes multimédia. */
    public static void vibrarCurto(Context ctx) {
        vibrarPadrao(ctx, VIBRACAO_LEMBRETE);
    }

    /**
     * Vibra com o padrão pedido. A vibração é marcada como "alarme" para não ser silenciada
     * pelo modo silencioso.
     */
    @SuppressWarnings("deprecation")
    private static void vibrarPadrao(Context ctx, long[] padrao) {
        Vibrator vibrador;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager gestorVibracao = (VibratorManager) ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrador = (gestorVibracao != null) ? gestorVibracao.getDefaultVibrator() : null;
        } else {
            vibrador = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        }
        if (vibrador == null || !vibrador.hasVibrator()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes atributos = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            vibrador.vibrate(VibrationEffect.createWaveform(padrao, -1), atributos);
        } else {
            vibrador.vibrate(padrao, -1);
        }
    }
}
