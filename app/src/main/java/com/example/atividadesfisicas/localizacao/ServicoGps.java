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
 * Serviço em primeiro plano que recolhe o GPS enquanto o exercício decorre.
 * Escreve cada posição no GestorExercicio, deteta desvios à rota planeada (UC10)
 * e emite os lembretes multimédia (UC11).
 */
public class ServicoGps extends Service {

    private LocationManager gestorLocalizacao;
    private long ultimoAvisoMs = 0;

    private final Handler agendador = new Handler(Looper.getMainLooper());

    /**
     * UC11: de X em X tempo lembra o utilizador (notificação + vibração) de tirar foto ou vídeo.
     * É uma classe anónima (e não uma lambda) porque se volta a agendar a si própria com "this".
     */
    private final Runnable lembrete = new Runnable() {
        @Override
        public void run() {
            GestorExercicio gestor = GestorExercicio.getInstance();
            if (!gestor.isAtiva()) {
                return;
            }
            gestor.registarLembrete(new LembreteMultimedia(System.currentTimeMillis(),
                    Config.INTERVALO_LEMBRETE_MS));
            Alertas.notificarLembrete(ServicoGps.this);
            Alertas.vibrarCurto(ServicoGps.this);
            agendador.postDelayed(this, Config.INTERVALO_LEMBRETE_MS);
        }
    };

    /** Recebe cada nova posição do GPS. */
    private final LocationListener ouvinteGps = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            GestorExercicio gestor = GestorExercicio.getInstance();
            gestor.registarLocalizacao(location);
            verificarDesvio(gestor);
        }

        // os métodos seguintes não são usados, mas têm de existir nas versões antigas do Android

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

    /** Arranca o serviço (chamar com a permissão de localização já concedida). */
    public static void iniciar(Context ctx) {
        ContextCompat.startForegroundService(ctx, new Intent(ctx, ServicoGps.class));
    }

    /** Para o serviço (o Android chama então o onDestroy). */
    public static void parar(Context ctx) {
        ctx.stopService(new Intent(ctx, ServicoGps.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Alertas.criarCanais(this);
        gestorLocalizacao = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
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
        gestorLocalizacao.removeUpdates(ouvinteGps);
        gestorLocalizacao.requestLocationUpdates(LocationManager.GPS_PROVIDER, Config.GPS_INTERVALO_MS, 0f,
                ouvinteGps, Looper.getMainLooper());
        agendador.removeCallbacks(lembrete);
        agendador.postDelayed(lembrete, Config.INTERVALO_LEMBRETE_MS);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        agendador.removeCallbacks(lembrete);
        if (gestorLocalizacao != null) {
            gestorLocalizacao.removeUpdates(ouvinteGps);
        }
        stopForeground(true);
        super.onDestroy();
    }

    /** Ninguém se liga ao serviço: a comunicação com o ecrã faz-se pelo GestorExercicio. */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * UC10: compara a posição atual com a rota planeada; se passar a tolerância, vibra e notifica.
     */
    private void verificarDesvio(GestorExercicio gestor) {
        Atividade atividade = gestor.getAtual();
        if (atividade == null) {
            return;
        }
        Rota rota = atividade.getRota();
        if (rota.getModo() == Rota.Modo.LIVRE || rota.getPontos().size() < 2) {
            return;
        }
        PontoGPS posicao = gestor.getUltimoPonto();
        if (posicao == null) {
            return;
        }
        double distancia = CalculoDesvio.distanciaARota(posicao, rota.getPontos());
        long agora = System.currentTimeMillis();
        if (distancia > Config.TOLERANCIA_DESVIO_METROS && agora - ultimoAvisoMs >= Config.COOLDOWN_AVISO_DESVIO_MS) {
            ultimoAvisoMs = agora;
            gestor.registarAviso(new AvisoDesvio(agora, distancia));
            Alertas.vibrar(this);
            Alertas.notificarDesvio(this);
        }
    }
}
