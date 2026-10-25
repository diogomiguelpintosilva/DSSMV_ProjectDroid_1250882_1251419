package com.example.atividadesfisicas.ecras;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.atividadesfisicas.R;
import com.example.atividadesfisicas.dados.GestorExercicio;
import com.example.atividadesfisicas.dados.HistoricoGuardado;
import com.example.atividadesfisicas.dados.RotasGuardadas;
import com.example.atividadesfisicas.localizacao.ServicoGps;
import com.example.atividadesfisicas.mapa.Mapa;
import com.example.atividadesfisicas.multimedia.MetricasFoto;
import com.example.atividadesfisicas.multimedia.MetricasVideo;
import com.example.atividadesfisicas.modelo.AvisoDesvio;
import com.example.atividadesfisicas.modelo.Foto;
import com.example.atividadesfisicas.modelo.LembreteMultimedia;
import com.example.atividadesfisicas.modelo.PontoGPS;
import com.example.atividadesfisicas.modelo.Rota;
import com.example.atividadesfisicas.modelo.Telemetria;
import com.example.atividadesfisicas.modelo.Video;
import com.example.atividadesfisicas.modelo.VideoComTelemetria;
import com.example.atividadesfisicas.utilitarios.Formatacao;
import com.example.atividadesfisicas.utilitarios.Permissoes;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ecrã da corrida/caminhada.
 * UC2 Iniciar atividade, UC3/UC4/UC5 (modos de rota), UC6 Consultar telemetria, UC7 Tirar foto,
 * UC8 Gravar vídeo com telemetria, UC9 Terminar atividade, UC10 Alerta de desvio, UC11 Lembrete multimédia.
 *
 * Tem dois estados: preparação (mostra o mapa, a rota escolhida e a posição do utilizador, com o botão
 * "Iniciar atividade") e atividade em curso (telemetria, fotos, vídeos e "Terminar").
 * O ecrã atualiza-se a cada segundo com um Handler (aula T4) a partir do GestorExercicio.
 */
public class ExercicioActivity extends AppCompatActivity {

    public static final String EXTRA_MODO = "modo";
    public static final String EXTRA_ROTA_ID = "rota_id";

    private static final long PERIODO_ATUALIZACAO_MS = 1000L;
    private static final int COR_PLANEADA = 0xFF1565C0;
    private static final int COR_PERCURSO = 0xFFD32F2F;

    private final GestorExercicio manager = GestorExercicio.getInstance();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private MapView map;
    private TextView tvTempo;
    private TextView tvVelocidade;
    private TextView tvAltitude;
    private TextView tvBanner;
    private TextView tvInfo;
    private Button btnIniciar;
    private View linhaAcoes;

    private Rota rota;
    private Polyline linhaPercurso;
    private MyLocationNewOverlay minhaLocalizacao;
    private boolean mapaPreparado = false;

    private long ultimoLembreteMostrado = 0;
    private long ultimoAvisoMostrado = 0;

    // capturas em processamento (fotos/vídeos): não deixa terminar até acabarem
    private int processamentosPendentes = 0;
    private File ficheiroPendente;
    private Telemetria telemetriaPendente;

    /** Pede ao Android a gravação de vídeo na qualidade máxima (por omissão pode ser baixa). */
    private static class CapturarVideoQualidadeMaxima extends ActivityResultContracts.CaptureVideo {
        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @NonNull Uri input) {
            Intent intent = super.createIntent(context, input);
            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
            return intent;
        }
    }

    private final ActivityResultLauncher<String[]> permissoesLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            (Map<String, Boolean> resultado) -> {
                if (Permissoes.temLocalizacao(this)) {
                    if (!mapaPreparado) {
                        prepararMapa();
                    }
                } else {
                    Toast.makeText(this, R.string.permissao_localizacao_necessaria, Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    private final ActivityResultLauncher<Uri> fotoLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), sucesso -> aoFotoTirada(sucesso));

    private final ActivityResultLauncher<Uri> videoLauncher = registerForActivityResult(
            new CapturarVideoQualidadeMaxima(), sucesso -> aoVideoGravado(sucesso));

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
        Mapa.inicializar(getApplicationContext());
        setContentView(R.layout.activity_exercicio);

        map = findViewById(R.id.map);
        tvTempo = findViewById(R.id.tvTempo);
        tvVelocidade = findViewById(R.id.tvVelocidade);
        tvAltitude = findViewById(R.id.tvAltitude);
        tvBanner = findViewById(R.id.tvBanner);
        tvInfo = findViewById(R.id.tvInfo);
        btnIniciar = findViewById(R.id.btnIniciar);
        linhaAcoes = findViewById(R.id.linhaAcoes);
        Button btnFoto = findViewById(R.id.btnFoto);
        Button btnVideo = findViewById(R.id.btnVideo);
        Button btnTerminar = findViewById(R.id.btnTerminar);

        Mapa.configurar(map, 17.0);

        // durante a atividade o botão "Voltar" não sai: é preciso usar "Terminar atividade"
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (manager.isAtiva()) {
                    Toast.makeText(ExercicioActivity.this, R.string.atividade_usar_terminar,
                            Toast.LENGTH_SHORT).show();
                } else {
                    finish();
                }
            }
        });

        btnIniciar.setOnClickListener(v -> iniciarAtividade());
        btnFoto.setOnClickListener(v -> tirarFoto());
        btnVideo.setOnClickListener(v -> gravarVideo());
        btnTerminar.setOnClickListener(v -> terminarAtividade());

        LembreteMultimedia ultimoLembrete = manager.getUltimoLembrete();
        if (ultimoLembrete != null) {
            ultimoLembreteMostrado = ultimoLembrete.getInstante();
        }

        if (manager.isAtiva()) {
            // voltou a um ecrã de uma atividade já em curso
            rota = manager.getAtual().getRota();
            mostrarModoAtividade();
            prepararMapa();
        } else {
            rota = resolverRota();
            if (rota == null) {
                Toast.makeText(this, R.string.atividade_rota_nao_encontrada, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            mostrarModoPreparacao();
            if (Permissoes.temLocalizacao(this) && Permissoes.temNotificacoes(this)) {
                prepararMapa();
            } else {
                permissoesLauncher.launch(Permissoes.permissoesPedidas());
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
        return RotasGuardadas.procurar(this, modo, getIntent().getStringExtra(EXTRA_ROTA_ID));
    }

    /** Estado antes de começar: botão "Iniciar atividade" e mensagem sobre o modo escolhido. */
    private void mostrarModoPreparacao() {
        btnIniciar.setVisibility(View.VISIBLE);
        linhaAcoes.setVisibility(View.GONE);
        tvInfo.setVisibility(View.VISIBLE);
        if (rota.getModo() == Rota.Modo.LIVRE) {
            tvInfo.setText(R.string.atividade_preparar_livre);
        } else {
            tvInfo.setText(getString(R.string.atividade_preparar_rota, rota.getNome()));
        }
    }

    /** Estado com a atividade a decorrer: fotos, vídeos e "Terminar". */
    private void mostrarModoAtividade() {
        btnIniciar.setVisibility(View.GONE);
        linhaAcoes.setVisibility(View.VISIBLE);
        tvInfo.setVisibility(View.GONE);
    }

    /** UC2: arranca o registo da atividade e o serviço de GPS (ao carregar em "Iniciar atividade"). */
    private void iniciarAtividade() {
        if (manager.isAtiva()) {
            return;
        }
        if (!Permissoes.temLocalizacao(this)) {
            permissoesLauncher.launch(Permissoes.permissoesPedidas());
            return;
        }
        if (!mapaPreparado) {
            prepararMapa();
        }
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm != null && !lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, R.string.atividade_ativar_gps, Toast.LENGTH_LONG).show();
        }
        manager.iniciar(rota);
        ServicoGps.iniciar(this);
        mostrarModoAtividade();
        if (minhaLocalizacao != null) {
            minhaLocalizacao.enableFollowLocation();
        }
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    private void prepararMapa() {
        map.getOverlays().clear();

        if (rota.getModo() != Rota.Modo.LIVRE && !rota.getPontos().isEmpty()) {
            List<GeoPoint> planeada = Mapa.paraGeoPoints(rota.getPontos());
            map.getOverlays().add(Mapa.criarLinha(planeada, COR_PLANEADA, 12f));
            Mapa.ajustarAosPontos(map, planeada);
        } else {
            GeoPoint pos = Mapa.ultimaPosicaoConhecida(this);
            if (pos != null) {
                Mapa.centrar(map, pos);
            }
        }

        linhaPercurso = Mapa.criarLinha(new ArrayList<GeoPoint>(), COR_PERCURSO, 10f);
        map.getOverlays().add(linhaPercurso);

        // ícone da posição do utilizador: fica sempre visível e acompanha-o durante a atividade;
        // nas rotas, antes de começar, o mapa mostra a rota inteira em vez de seguir o utilizador
        boolean seguir = manager.isAtiva() || rota.getModo() == Rota.Modo.LIVRE;
        minhaLocalizacao = Mapa.adicionarMinhaLocalizacao(this, map, seguir);
        mapaPreparado = true;
        map.invalidate();
    }

    /** Corre a cada segundo: telemetria (UC6), percurso, lembretes (UC11) e avisos de desvio (UC10). */
    private void atualizarEcra() {
        if (!manager.isAtiva() || linhaPercurso == null) {
            return;
        }
        long tempoMs = manager.getTempoDecorridoMs();
        tvTempo.setText(Formatacao.formatarTempo(tempoMs));

        Telemetria t = manager.getTelemetriaAtual();
        if (t != null) {
            tvVelocidade.setText(Formatacao.formatarVelocidade(t.getVelocidadeKmh()));
            tvAltitude.setText(Formatacao.formatarAltitude(t.getAltitude()));
        }

        List<PontoGPS> percurso = manager.copiaPercurso();
        if (!percurso.isEmpty()) {
            linhaPercurso.setPoints(Mapa.paraGeoPoints(percurso));
            map.invalidate();
        }

        // UC11: o lembrete (notificação + vibração) é emitido pelo ServicoGps; aqui só se mostra o banner
        LembreteMultimedia lembrete = manager.getUltimoLembrete();
        if (lembrete != null && lembrete.getInstante() > ultimoLembreteMostrado) {
            ultimoLembreteMostrado = lembrete.getInstante();
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
                    MetricasFoto.aplicar(ficheiro, telemetria);
                } catch (IOException e) {
                    comMetricas = false;
                }
                final boolean ok = comMetricas;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        processamentosPendentes--;
                        manager.adicionarMultimedia(new Foto(ficheiro.getAbsolutePath(), System.currentTimeMillis()));
                        Toast.makeText(ExercicioActivity.this,
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
        final long duracao = MetricasVideo.duracaoMs(original);
        final long inicio = fim - duracao;
        final List<Telemetria> amostras = manager.copiaTelemetria();
        final File saida = novoFicheiro("video_tel_", ".mp4", Environment.DIRECTORY_MOVIES);

        processamentosPendentes++;
        Toast.makeText(this, R.string.atividade_video_a_processar, Toast.LENGTH_SHORT).show();

        try {
            MetricasVideo.processar(this, original, saida, amostras, inicio,
                    new MetricasVideo.Listener() {
                        @Override
                        public void onConcluido() {
                            processamentosPendentes--;
                            manager.adicionarMultimedia(
                                    new VideoComTelemetria(saida.getAbsolutePath(), fim, duracao));
                            original.delete();
                            Toast.makeText(ExercicioActivity.this, R.string.atividade_video_guardado,
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
        ServicoGps.parar(this);
        manager.terminar();
        // guarda a atividade no histórico (local e na cloud)
        HistoricoGuardado.guardar(getApplicationContext(), manager.getAtual());
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
        if (minhaLocalizacao != null) {
            minhaLocalizacao.enableMyLocation();
        }
        if (manager.isAtiva() && linhaPercurso != null) {
            handler.removeCallbacks(tick);
            handler.post(tick);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
        if (minhaLocalizacao != null) {
            minhaLocalizacao.disableMyLocation();
        }
        handler.removeCallbacks(tick);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        map.onDetach();
        super.onDestroy();
    }
}
