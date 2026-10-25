package com.example.atividadesfisicas.localizacao;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.example.atividadesfisicas.dados.GestorExercicio;
import com.example.atividadesfisicas.modelo.Atividade;
import com.example.atividadesfisicas.modelo.AvisoDesvio;
import com.example.atividadesfisicas.modelo.LembreteMultimedia;
import com.example.atividadesfisicas.modelo.PontoGPS;
import com.example.atividadesfisicas.modelo.Rota;
import com.example.atividadesfisicas.utilitarios.Config;
import com.example.atividadesfisicas.utilitarios.Permissoes;

/**
 * Serviço em primeiro plano que recolhe o GPS enquanto a atividade decorre (Android Service, aula T3/T4).
 * Escreve no GestorExercicio e deteta desvios à rota planeada (vibração + notificação).
 */
public class ServicoGps extends Service {

    private LocationManager locationManager;
    private long ultimoAvisoMs = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());

    /** UC11: de X em X tempo lembra o utilizador (notificação + vibração) de tirar foto ou vídeo. */
    private final Runnable lembrete = new Runnable() {
        @Override
        public void run() {
            GestorExercicio manager = GestorExercicio.getInstance();
            if (!manager.isAtiva()) {
                return;
            }
            manager.registarLembrete(new LembreteMultimedia(System.currentTimeMillis(),
                    Config.INTERVALO_LEMBRETE_MS));
            Alertas.notificarLembrete(ServicoGps.this);
            Alertas.vibrarCurto(ServicoGps.this);
            handler.postDelayed(this, Config.INTERVALO_LEMBRETE_MS);
        }
    };

    private final LocationListener listener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            GestorExercicio manager = GestorExercicio.getInstance();
            manager.registarLocalizacao(location);
            verificarDesvio(manager);
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
        }

        @Override
        @SuppressWarnings("deprecation")
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    /**
     * Arranca o serviço (chamar com a permissão de localização já concedida).
     */
    public static void iniciar(Context ctx) {
        ContextCompat.startForegroundService(ctx, new Intent(ctx, ServicoGps.class));
    }

    public static void parar(Context ctx) {
        ctx.stopService(new Intent(ctx, ServicoGps.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Alertas.criarCanais(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notificacao = Alertas.criarNotificacaoServico(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(Alertas.ID_NOTIFICACAO_SERVICO, notificacao, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(Alertas.ID_NOTIFICACAO_SERVICO, notificacao);
        }
        if (!Permissoes.temLocalizacao(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        locationManager.removeUpdates(listener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, Config.GPS_INTERVALO_MS, 0f, listener, Looper.getMainLooper());
        handler.removeCallbacks(lembrete);
        handler.postDelayed(lembrete, Config.INTERVALO_LEMBRETE_MS);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(lembrete);
        if (locationManager != null) {
            locationManager.removeUpdates(listener);
        }
        stopForeground(true);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Compara a posição atual com a rota planeada; se passar a tolerância, vibra e notifica.
     */
    private void verificarDesvio(GestorExercicio manager) {
        Atividade atividade = manager.getAtual();
        if (atividade == null) {
            return;
        }
        Rota rota = atividade.getRota();
        if (rota.getModo() == Rota.Modo.LIVRE || rota.getPontos().size() < 2) {
            return;
        }
        PontoGPS posicao = manager.getUltimoPonto();
        if (posicao == null) {
            return;
        }
        double distancia = CalculoDesvio.distanciaARota(posicao, rota.getPontos());
        long agora = System.currentTimeMillis();
        if (distancia > Config.TOLERANCIA_DESVIO_METROS && agora - ultimoAvisoMs >= Config.COOLDOWN_AVISO_DESVIO_MS) {
            ultimoAvisoMs = agora;
            manager.registarAviso(new AvisoDesvio(agora, distancia));
            Alertas.vibrar(this);
            Alertas.notificarDesvio(this);
        }
    }
}