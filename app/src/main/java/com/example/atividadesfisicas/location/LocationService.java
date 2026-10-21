package com.example.atividadesfisicas.location;

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
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.example.atividadesfisicas.data.AtividadeManager;
import com.example.atividadesfisicas.model.Atividade;
import com.example.atividadesfisicas.model.AvisoDesvio;
import com.example.atividadesfisicas.model.PontoGPS;
import com.example.atividadesfisicas.model.Rota;
import com.example.atividadesfisicas.util.Config;
import com.example.atividadesfisicas.util.PermissoesHelper;

/**
 * Serviço em primeiro plano que recolhe o GPS enquanto a atividade decorre (Android Service, aula T3/T4).
 * Escreve no AtividadeManager e deteta desvios à rota planeada (vibração + notificação).
 */
public class LocationService extends Service {

    private LocationManager locationManager;
    private long ultimoAvisoMs = 0;

    private final LocationListener listener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            AtividadeManager manager = AtividadeManager.getInstance();
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
        ContextCompat.startForegroundService(ctx, new Intent(ctx, LocationService.class));
    }

    public static void parar(Context ctx) {
        ctx.stopService(new Intent(ctx, LocationService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AlertaHelper.criarCanais(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notificacao = AlertaHelper.criarNotificacaoServico(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(AlertaHelper.ID_NOTIFICACAO_SERVICO, notificacao, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(AlertaHelper.ID_NOTIFICACAO_SERVICO, notificacao);
        }
        if (!PermissoesHelper.temLocalizacao(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        locationManager.removeUpdates(listener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, Config.GPS_INTERVALO_MS, 0f, listener, Looper.getMainLooper());
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
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
    private void verificarDesvio(AtividadeManager manager) {
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
        double distancia = DesvioRota.distanciaARota(posicao, rota.getPontos());
        long agora = System.currentTimeMillis();
        if (distancia > Config.TOLERANCIA_DESVIO_METROS && agora - ultimoAvisoMs >= Config.COOLDOWN_AVISO_DESVIO_MS) {
            ultimoAvisoMs = agora;
            manager.registarAviso(new AvisoDesvio(agora, distancia));
            AlertaHelper.vibrar(this);
            AlertaHelper.notificarDesvio(this);
        }
    }
}