package com.example.atividadesfisicas.ecras;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;
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

import com.example.atividadesfisicas.R;
import com.example.atividadesfisicas.dados.GestorExercicio;
import com.example.atividadesfisicas.dados.HistoricoGuardado;
import com.example.atividadesfisicas.dados.RotasGuardadas;
import com.example.atividadesfisicas.localizacao.ServicoGps;
import com.example.atividadesfisicas.mapa.Mapa;
import com.example.atividadesfisicas.modelo.AvisoDesvio;
import com.example.atividadesfisicas.modelo.LembreteMultimedia;
import com.example.atividadesfisicas.modelo.PontoGPS;
import com.example.atividadesfisicas.modelo.Rota;
import com.example.atividadesfisicas.modelo.Telemetria;
import com.example.atividadesfisicas.utilitarios.Formatacao;
import com.example.atividadesfisicas.utilitarios.Permissoes;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ecrã da corrida/caminhada.
 * UC2 Iniciar atividade, UC3/UC4/UC5 (modos de rota), UC6 Consultar telemetria, UC9 Terminar atividade,
 * UC10 Alerta de desvio e UC11 Lembrete multimédia. As fotos e vídeos (UC7, UC8) estão na CapturaMultimedia.
 *
 * Tem dois estados: preparação (mapa, rota escolhida e posição do utilizador, com o botão
 * "Iniciar atividade") e exercício em curso (telemetria, fotos, vídeos e "Terminar").
 * O ecrã atualiza-se a cada segundo com um Handler, a partir do GestorExercicio.
 */
public class ExercicioActivity extends AppCompatActivity {

    public static final String EXTRA_MODO = "modo";
    public static final String EXTRA_ROTA_ID = "rota_id";

    private static final long PERIODO_ATUALIZACAO_MS = 1000L;
    private static final int COR_BANNER_LEMBRETE = 0xFF1565C0;
    private static final int COR_BANNER_DESVIO = 0xFFD32F2F;
    private static final long DURACAO_BANNER_LEMBRETE_MS = 6000;
    private static final long DURACAO_BANNER_DESVIO_MS = 5000;

    private final GestorExercicio gestor = GestorExercicio.getInstance();
    private final Handler agendador = new Handler(Looper.getMainLooper());
    private final CapturaMultimedia captura = new CapturaMultimedia(this);

    private MapView mapa;
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

    // instantes do último lembrete e do último aviso já mostrados (para mostrar cada banner uma só vez)
    private long ultimoLembreteMostrado = 0;
    private long ultimoAvisoMostrado = 0;

    private final ActivityResultLauncher<String[]> pedidoPermissoes = registerForActivityResult(
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

    private final Runnable esconderBanner = () -> tvBanner.setVisibility(View.GONE);

    /** Atualização a cada segundo. Classe anónima (e não lambda) porque se volta a agendar com "this". */
    private final Runnable atualizacao = new Runnable() {
        @Override
        public void run() {
            atualizarEcra();
            agendador.postDelayed(this, PERIODO_ATUALIZACAO_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapa.inicializar(getApplicationContext());
        setContentView(R.layout.activity_exercicio);

        mapa = findViewById(R.id.map);
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

        Mapa.configurar(mapa, 17.0);

        // durante o exercício o botão "Voltar" não sai: é preciso usar "Terminar"
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (gestor.isAtiva()) {
                    Toast.makeText(ExercicioActivity.this, R.string.atividade_usar_terminar,
                            Toast.LENGTH_SHORT).show();
                } else {
                    finish();
                }
            }
        });

        btnIniciar.setOnClickListener(v -> iniciarExercicio());
        btnFoto.setOnClickListener(v -> captura.tirarFoto());
        btnVideo.setOnClickListener(v -> captura.gravarVideo());
        btnTerminar.setOnClickListener(v -> terminarExercicio());

        LembreteMultimedia ultimoLembrete = gestor.getUltimoLembrete();
        if (ultimoLembrete != null) {
            ultimoLembreteMostrado = ultimoLembrete.getInstante();
        }

        if (gestor.isAtiva()) {
            // voltou ao ecrã de um exercício já em curso (por exemplo, pela notificação)
            rota = gestor.getAtual().getRota();
            mostrarEstadoEmCurso();
            prepararMapa();
        } else {
            rota = obterRotaEscolhida();
            if (rota == null) {
                Toast.makeText(this, R.string.atividade_rota_nao_encontrada, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            mostrarEstadoPreparacao();
            if (Permissoes.temLocalizacao(this) && Permissoes.temNotificacoes(this)) {
                prepararMapa();
            } else {
                pedidoPermissoes.launch(Permissoes.permissoesPedidas());
            }
        }
    }

    /** Descobre que rota seguir a partir do Intent (o modo livre cria uma rota vazia). */
    private Rota obterRotaEscolhida() {
        String modoTexto = getIntent().getStringExtra(EXTRA_MODO);
        if (modoTexto == null) {
            return null;
        }
        Rota.Modo modo = Rota.Modo.valueOf(modoTexto);
        if (modo == Rota.Modo.LIVRE) {
            return new Rota("livre_" + System.currentTimeMillis(), getString(R.string.rota_livre_nome), Rota.Modo.LIVRE);
        }
        return RotasGuardadas.procurar(this, modo, getIntent().getStringExtra(EXTRA_ROTA_ID));
    }

    /** Estado antes de começar: botão "Iniciar atividade" e mensagem sobre o modo escolhido. */
    private void mostrarEstadoPreparacao() {
        btnIniciar.setVisibility(View.VISIBLE);
        linhaAcoes.setVisibility(View.GONE);
        tvInfo.setVisibility(View.VISIBLE);
        if (rota.getModo() == Rota.Modo.LIVRE) {
            tvInfo.setText(R.string.atividade_preparar_livre);
        } else {
            tvInfo.setText(getString(R.string.atividade_preparar_rota, rota.getNome()));
        }
    }

    /** Estado com o exercício a decorrer: fotos, vídeos e "Terminar". */
    private void mostrarEstadoEmCurso() {
        btnIniciar.setVisibility(View.GONE);
        linhaAcoes.setVisibility(View.VISIBLE);
        tvInfo.setVisibility(View.GONE);
    }

    /** UC2: arranca o registo do exercício e o serviço de GPS (ao carregar em "Iniciar atividade"). */
    private void iniciarExercicio() {
        if (gestor.isAtiva()) {
            return;
        }
        if (!Permissoes.temLocalizacao(this)) {
            pedidoPermissoes.launch(Permissoes.permissoesPedidas());
            return;
        }
        if (!mapaPreparado) {
            prepararMapa();
        }
        LocationManager gestorLocalizacao = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (gestorLocalizacao != null && !gestorLocalizacao.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, R.string.atividade_ativar_gps, Toast.LENGTH_LONG).show();
        }
        gestor.iniciar(rota);
        ServicoGps.iniciar(this);
        mostrarEstadoEmCurso();
        if (minhaLocalizacao != null) {
            minhaLocalizacao.enableFollowLocation();
        }
        agendador.removeCallbacks(atualizacao);
        agendador.post(atualizacao);
    }

    /** Desenha a rota planeada (se houver), a linha do percurso e o ícone do utilizador. */
    private void prepararMapa() {
        mapa.getOverlays().clear();

        if (rota.getModo() != Rota.Modo.LIVRE && !rota.getPontos().isEmpty()) {
            List<GeoPoint> planeada = Mapa.paraGeoPoints(rota.getPontos());
            mapa.getOverlays().add(Mapa.criarLinha(planeada, Mapa.COR_ROTA_PLANEADA, 12f));
            Mapa.ajustarAosPontos(mapa, planeada);
        } else {
            GeoPoint posicao = Mapa.ultimaPosicaoConhecida(this);
            if (posicao != null) {
                Mapa.centrar(mapa, posicao);
            }
        }

        linhaPercurso = Mapa.criarLinha(new ArrayList<GeoPoint>(), Mapa.COR_PERCURSO, 10f);
        mapa.getOverlays().add(linhaPercurso);

        // nas rotas, antes de começar, o mapa mostra a rota inteira em vez de seguir o utilizador
        boolean seguir = gestor.isAtiva() || rota.getModo() == Rota.Modo.LIVRE;
        minhaLocalizacao = Mapa.adicionarMinhaLocalizacao(this, mapa, seguir);
        mapaPreparado = true;
        mapa.invalidate();
    }

    /** Corre a cada segundo: telemetria (UC6), percurso, lembretes (UC11) e avisos de desvio (UC10). */
    private void atualizarEcra() {
        if (!gestor.isAtiva() || linhaPercurso == null) {
            return;
        }
        tvTempo.setText(Formatacao.formatarTempo(gestor.getTempoDecorridoMs()));

        Telemetria t = gestor.getTelemetriaAtual();
        if (t != null) {
            tvVelocidade.setText(Formatacao.formatarVelocidade(t.getVelocidadeKmh()));
            tvAltitude.setText(Formatacao.formatarAltitude(t.getAltitude()));
        }

        List<PontoGPS> percurso = gestor.copiaPercurso();
        if (!percurso.isEmpty()) {
            linhaPercurso.setPoints(Mapa.paraGeoPoints(percurso));
            mapa.invalidate();
        }

        // UC11: a notificação e a vibração já foram feitas pelo ServicoGps; aqui só se mostra o banner
        LembreteMultimedia lembrete = gestor.getUltimoLembrete();
        if (lembrete != null && lembrete.getInstante() > ultimoLembreteMostrado) {
            ultimoLembreteMostrado = lembrete.getInstante();
            mostrarBanner(getString(R.string.atividade_lembrete), DURACAO_BANNER_LEMBRETE_MS, COR_BANNER_LEMBRETE);
        }

        // UC10: o mesmo para o aviso de desvio
        AvisoDesvio aviso = gestor.getUltimoAviso();
        if (aviso != null && aviso.getInstante() > ultimoAvisoMostrado) {
            ultimoAvisoMostrado = aviso.getInstante();
            mostrarBanner(getString(R.string.atividade_desvio), DURACAO_BANNER_DESVIO_MS, COR_BANNER_DESVIO);
        }
    }

    /** Mostra o banner no topo durante duracaoMs (um banner novo reinicia o tempo). */
    private void mostrarBanner(String texto, long duracaoMs, int cor) {
        tvBanner.setText(texto);
        tvBanner.setBackgroundColor(cor);
        tvBanner.setTextColor(Color.WHITE);
        tvBanner.setVisibility(View.VISIBLE);
        agendador.removeCallbacks(esconderBanner);
        agendador.postDelayed(esconderBanner, duracaoMs);
    }

    /** UC9: termina o exercício, guarda-o no histórico e abre o resumo. */
    private void terminarExercicio() {
        if (captura.temProcessamentosPendentes()) {
            Toast.makeText(this, R.string.atividade_a_processar, Toast.LENGTH_SHORT).show();
            return;
        }
        agendador.removeCallbacks(atualizacao);
        ServicoGps.parar(this);
        gestor.terminar();
        HistoricoGuardado.guardar(getApplicationContext(), gestor.getAtual());
        startActivity(new Intent(this, ResumoActivity.class));
        finish();
    }

    // ------------------------------------------------------------------ ciclo de vida

    @Override
    protected void onResume() {
        super.onResume();
        mapa.onResume();
        if (minhaLocalizacao != null) {
            minhaLocalizacao.enableMyLocation();
        }
        if (gestor.isAtiva() && linhaPercurso != null) {
            agendador.removeCallbacks(atualizacao);
            agendador.post(atualizacao);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapa.onPause();
        if (minhaLocalizacao != null) {
            minhaLocalizacao.disableMyLocation();
        }
        // o ServicoGps continua a registar o percurso; só o ecrã deixa de se atualizar
        agendador.removeCallbacks(atualizacao);
    }

    @Override
    protected void onDestroy() {
        agendador.removeCallbacksAndMessages(null);
        mapa.onDetach();
        super.onDestroy();
    }
}
