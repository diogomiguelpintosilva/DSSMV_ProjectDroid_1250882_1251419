package com.example.atividadesfisicas.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.atividadesfisicas.R;
import com.example.atividadesfisicas.data.AtividadeManager;
import com.example.atividadesfisicas.data.RotaRepository;
import com.example.atividadesfisicas.location.LocationService;
import com.example.atividadesfisicas.map.MapaHelper;
import com.example.atividadesfisicas.media.FotoOverlay;
import com.example.atividadesfisicas.media.VideoOverlayProcessor;
import com.example.atividadesfisicas.model.AvisoDesvio;
import com.example.atividadesfisicas.model.Foto;
import com.example.atividadesfisicas.model.LembreteMultimedia;
import com.example.atividadesfisicas.model.PontoGPS;
import com.example.atividadesfisicas.model.Rota;
import com.example.atividadesfisicas.model.Telemetria;
import com.example.atividadesfisicas.model.Video;
import com.example.atividadesfisicas.model.VideoComTelemetria;
import com.example.atividadesfisicas.util.Config;
import com.example.atividadesfisicas.util.FormatUtil;
import com.example.atividadesfisicas.util.PermissoesHelper;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ecrã principal durante a corrida/caminhada.
 * UC2 Iniciar atividade, UC3/UC4/UC5 (modos de rota), UC6 Consultar telemetria, UC7 Tirar foto,
 * UC8 Gravar vídeo com telemetria, UC9 Terminar atividade, UC10 Alerta de desvio, UC11 Lembrete multimédia.
 *
 * O ecrã atualiza-se a cada segundo com um Handler (aula T4) a partir do AtividadeManager.
 */
public class AtividadeActivity extends AppCompatActivity {

    public static final String EXTRA_MODO = "modo";
    public static final String EXTRA_ROTA_ID = "rota_id";

    private static final long PERIODO_ATUALIZACAO_MS = 1000L;
    private static final int COR_PLANEADA = 0xFF1565C0;
    private static final int COR_PERCURSO = 0xFFD32F2F;

    private final AtividadeManager manager = AtividadeManager.getInstance();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private MapView map;
    private TextView tvTempo;
    private TextView tvVelocidade;
    private TextView tvAltitude;
    private TextView tvBanner;

    private Rota rota;
    private Polyline linhaPercurso;
    private Marker marcadorAtual;
    private boolean marcadorNoMapa = false;

    private long ultimoLembreteMs = 0;
    private long ultimoAvisoMostrado = 0;

    // capturas em processamento (fotos/vídeos): não deixa terminar até acabarem
    private int processamentosPendentes = 0;
    private File ficheiroPendente;
    private Telemetria telemetriaPendente;

    private final ActivityResultLauncher<String[]> permissoesLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            (Map<String, Boolean> resultado) -> {
                if (PermissoesHelper.temLocalizacao(this)) {
                    iniciarAtividade();
                } else {
                    Toast.makeText(this, R.string.permissao_localizacao_necessaria, Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    private final ActivityResultLauncher<Uri> fotoLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), sucesso -> aoFotoTirada(sucesso));

    private final ActivityResultLauncher<Uri> videoLauncher = registerForActivityResult(
            new ActivityResultContracts.CaptureVideo(), sucesso -> aoVideoGravado(sucesso));

    private final Runnable esconderBanner = new Runnable() {
        @Override
        public void run() {
            tvBanner.setVisibility(View.GONE);
        }
    };

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            atualizarEcra();
            handler.postDelayed(this, PERIODO_ATUALIZACAO_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapaHelper.inicializar(getApplicationContext());
        setContentView(R.layout.activity_atividade);

        map = findViewById(R.id.map);
        tvTempo = findViewById(R.id.tvTempo);
        tvVelocidade = findViewById(R.id.tvVelocidade);
        tvAltitude = findViewById(R.id.tvAltitude);
        tvBanner = findViewById(R.id.tvBanner);
        Button btnFoto = findViewById(R.id.btnFoto);
        Button btnVideo = findViewById(R.id.btnVideo);
        Button btnTerminar = findViewById(R.id.btnTerminar);

        MapaHelper.configurar(map, 17.0);

        // o botão "Voltar" não sai da atividade: é preciso usar "Terminar atividade"
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Toast.makeText(AtividadeActivity.this, R.string.atividade_usar_terminar,
                        Toast.LENGTH_SHORT).show();
            }
        });

        btnFoto.setOnClickListener(v -> tirarFoto());
        btnVideo.setOnClickListener(v -> gravarVideo());
        btnTerminar.setOnClickListener(v -> terminarAtividade());

        if (manager.isAtiva()) {
            // voltou a um ecrã de uma atividade já em curso
            rota = manager.getAtual().getRota();
            prepararMapa();
        } else {
            rota = resolverRota();
            if (rota == null) {
                Toast.makeText(this, R.string.atividade_rota_nao_encontrada, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            if (PermissoesHelper.temLocalizacao(this)) {
                iniciarAtividade();
            } else {
                permissoesLauncher.launch(PermissoesHelper.permissoesPedidas());
            }
        }
    }

    /** Descobre que rota seguir a partir do Intent (modo livre cria uma rota vazia). */
    private Rota resolverRota() {
        String modoTexto = getIntent().getStringExtra(EXTRA_MODO);
        if (modoTexto == null) {
            return null;
        }
        Rota.Modo modo = Rota.Modo.valueOf(modoTexto);
        if (modo == Rota.Modo.LIVRE) {
            return new Rota("livre_" + System.currentTimeMillis(), "Percurso livre", Rota.Modo.LIVRE);
        }
        return RotaRepository.procurar(this, modo, getIntent().getStringExtra(EXTRA_ROTA_ID));
    }

    /** UC2: arranca o registo da atividade e o serviço de GPS. */
    private void iniciarAtividade() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm != null && !lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, R.string.atividade_ativar_gps, Toast.LENGTH_LONG).show();
        }
        manager.iniciar(rota);
        LocationService.iniciar(this);
        prepararMapa();
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    private void prepararMapa() {
        map.getOverlays().clear();
        marcadorNoMapa = false;

        if (rota.getModo() != Rota.Modo.LIVRE && !rota.getPontos().isEmpty()) {
            List<GeoPoint> planeada = MapaHelper.paraGeoPoints(rota.getPontos());
            map.getOverlays().add(MapaHelper.criarLinha(planeada, COR_PLANEADA, 12f));
            MapaHelper.ajustarAosPontos(map, planeada);
        } else {
            GeoPoint pos = MapaHelper.ultimaPosicaoConhecida(this);
            if (pos != null) {
                MapaHelper.centrar(map, pos);
            }
        }

        linhaPercurso = MapaHelper.criarLinha(new ArrayList<GeoPoint>(), COR_PERCURSO, 10f);
        map.getOverlays().add(linhaPercurso);

        marcadorAtual = new Marker(map);
        marcadorAtual.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        marcadorAtual.setTitle(getString(R.string.atividade_posicao_atual));
    }

    /** Corre a cada segundo: telemetria (UC6), mapa, lembretes (UC11) e avisos de desvio (UC10). */
    private void atualizarEcra() {
        long tempoMs = manager.getTempoDecorridoMs();
        tvTempo.setText(FormatUtil.formatarTempo(tempoMs));

        Telemetria t = manager.getTelemetriaAtual();
        if (t != null) {
            tvVelocidade.setText(FormatUtil.formatarVelocidade(t.getVelocidadeKmh()));
            tvAltitude.setText(FormatUtil.formatarAltitude(t.getAltitude()));
        }

        List<PontoGPS> percurso = manager.copiaPercurso();
        if (!percurso.isEmpty()) {
            List<GeoPoint> pontos = MapaHelper.paraGeoPoints(percurso);
            linhaPercurso.setPoints(pontos);
            GeoPoint ultimo = pontos.get(pontos.size() - 1);
            if (!marcadorNoMapa) {
                map.getOverlays().add(marcadorAtual);
                marcadorNoMapa = true;
            }
            marcadorAtual.setPosition(ultimo);
            MapaHelper.centrar(map, ultimo);
            map.invalidate();
        }

        // UC11: lembrete de X em X tempo
        if (tempoMs - ultimoLembreteMs >= Config.INTERVALO_LEMBRETE_MS) {
            ultimoLembreteMs = tempoMs;
            manager.registarLembrete(new LembreteMultimedia(System.currentTimeMillis(),
                    Config.INTERVALO_LEMBRETE_MS));
            mostrarBanner(getString(R.string.atividade_lembrete), 6000, 0xFF1565C0);
        }

        // UC10: aviso de desvio (a vibração e a notificação já foram feitas pelo serviço)
        AvisoDesvio aviso = manager.getUltimoAviso();
        if (aviso != null && aviso.getInstante() > ultimoAvisoMostrado) {
            ultimoAvisoMostrado = aviso.getInstante();
            mostrarBanner(getString(R.string.atividade_desvio), 5000, 0xFFD32F2F);
        }
    }

    private void mostrarBanner(String texto, long duracaoMs, int cor) {
        tvBanner.setText(texto);
        tvBanner.setBackgroundColor(cor);
        tvBanner.setTextColor(Color.WHITE);
        tvBanner.setVisibility(View.VISIBLE);
        handler.removeCallbacks(esconderBanner);
        handler.postDelayed(esconderBanner, duracaoMs);
    }

    // ------------------------------------------------------------------ UC7 Tirar foto

    private void tirarFoto() {
        if (!manager.isAtiva()) {
            return;
        }
        File ficheiro = novoFicheiro("foto_", ".jpg", Environment.DIRECTORY_PICTURES);
        ficheiroPendente = ficheiro;
        telemetriaPendente = manager.getTelemetriaAtual();
        fotoLauncher.launch(uriDe(ficheiro));
    }

    private void aoFotoTirada(boolean sucesso) {
        final File ficheiro = ficheiroPendente;
        final Telemetria telemetria = telemetriaPendente;
        ficheiroPendente = null;
        if (ficheiro == null) {
            return;
        }
        if (!sucesso) {
            ficheiro.delete();
            return;
        }

        processamentosPendentes++;
        new Thread() {
            @Override
            public void run() {
                boolean comMetricas = true;
                try {
                    FotoOverlay.aplicar(ficheiro, telemetria);
                } catch (IOException e) {
                    comMetricas = false;
                }
                final boolean ok = comMetricas;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        processamentosPendentes--;
                        manager.adicionarMultimedia(new Foto(ficheiro.getAbsolutePath(), System.currentTimeMillis()));
                        Toast.makeText(AtividadeActivity.this,
                                ok ? R.string.atividade_foto_guardada : R.string.atividade_foto_sem_metricas,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }.start();
    }

    // ------------------------------------------------------------------ UC8 Gravar vídeo com telemetria

    private void gravarVideo() {
        if (!manager.isAtiva()) {
            return;
        }
        File ficheiro = novoFicheiro("video_", ".mp4", Environment.DIRECTORY_MOVIES);
        ficheiroPendente = ficheiro;
        videoLauncher.launch(uriDe(ficheiro));
    }

    private void aoVideoGravado(boolean sucesso) {
        final File original = ficheiroPendente;
        ficheiroPendente = null;
        if (original == null) {
            return;
        }
        if (!sucesso || !original.exists() || original.length() == 0) {
            original.delete();
            return;
        }

        // o vídeo acabou agora; o início calcula-se a partir da duração
        final long fim = System.currentTimeMillis();
        final long duracao = VideoOverlayProcessor.duracaoMs(original);
        final long inicio = fim - duracao;
        final List<Telemetria> amostras = manager.copiaTelemetria();
        final File saida = novoFicheiro("video_tel_", ".mp4", Environment.DIRECTORY_MOVIES);

        processamentosPendentes++;
        Toast.makeText(this, R.string.atividade_video_a_processar, Toast.LENGTH_SHORT).show();

        try {
            VideoOverlayProcessor.processar(this, original, saida, amostras, inicio,
                    new VideoOverlayProcessor.Listener() {
                        @Override
                        public void onConcluido() {
                            processamentosPendentes--;
                            manager.adicionarMultimedia(
                                    new VideoComTelemetria(saida.getAbsolutePath(), fim, duracao));
                            original.delete();
                            Toast.makeText(AtividadeActivity.this, R.string.atividade_video_guardado,
                                    Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onErro(Exception e) {
                            guardarVideoSemMetricas(original, fim, duracao);
                        }
                    });
        } catch (RuntimeException e) {
            guardarVideoSemMetricas(original, fim, duracao);
        }
    }

    private void guardarVideoSemMetricas(File original, long fim, long duracao) {
        processamentosPendentes--;
        manager.adicionarMultimedia(new Video(original.getAbsolutePath(), fim, duracao));
        Toast.makeText(this, R.string.atividade_video_sem_metricas, Toast.LENGTH_LONG).show();
    }

    // ------------------------------------------------------------------ UC9 Terminar atividade

    private void terminarAtividade() {
        if (processamentosPendentes > 0) {
            Toast.makeText(this, R.string.atividade_a_processar, Toast.LENGTH_SHORT).show();
            return;
        }
        handler.removeCallbacks(tick);
        LocationService.parar(this);
        manager.terminar();
        startActivity(new Intent(this, ResumoActivity.class));
        finish();
    }

    // ------------------------------------------------------------------ ficheiros

    private File novoFicheiro(String prefixo, String sufixo, String tipoPasta) {
        File base = getExternalFilesDir(tipoPasta);
        if (base == null) {
            base = getFilesDir();
        }
        File pasta = new File(base, "atividade_" + manager.getAtual().getDataHoraInicio());
        pasta.mkdirs();
        File ficheiro = new File(pasta, prefixo + System.currentTimeMillis() + sufixo);
        try {
            ficheiro.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ficheiro;
    }

    private Uri uriDe(File ficheiro) {
        return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", ficheiro);
    }

    // ------------------------------------------------------------------ ciclo de vida

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
        if (manager.isAtiva() && linhaPercurso != null) {
            handler.removeCallbacks(tick);
            handler.post(tick);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
        handler.removeCallbacks(tick);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        map.onDetach();
        super.onDestroy();
    }
}