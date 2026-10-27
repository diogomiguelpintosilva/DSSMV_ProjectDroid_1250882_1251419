package com.example.atividadesfisicas.ecras;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.atividadesfisicas.R;
import com.example.atividadesfisicas.dados.GestorExercicio;
import com.example.atividadesfisicas.dados.RotasGuardadas;
import com.example.atividadesfisicas.mapa.Mapa;
import com.example.atividadesfisicas.modelo.Atividade;
import com.example.atividadesfisicas.modelo.Rota;
import com.example.atividadesfisicas.modelo.VideoResumo;
import com.example.atividadesfisicas.multimedia.Galeria;
import com.example.atividadesfisicas.multimedia.GeradorVideoResumo;
import com.example.atividadesfisicas.utilitarios.Formatacao;
import com.example.atividadesfisicas.utilitarios.SimplificarPercurso;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Ecrã final (UC9): rota efetuada, velocidade média, tempo total, distância total e vídeo resumo (máx. 2 min).
 * No modo livre permite guardar o percurso como rota; o vídeo resumo pode ser guardado na galeria.
 */
public class ResumoActivity extends AppCompatActivity {

    /** Para guardar uma rota do modo livre: um ponto a cada 10 m, no máximo 500 pontos. */
    private static final double DISTANCIA_MINIMA_PONTOS_M = 10;
    private static final int MAXIMO_PONTOS_ROTA = 500;
    /** Abaixo disto não há percurso que valha a pena guardar. */
    private static final double DISTANCIA_MINIMA_ROTA_M = 20;

    private MapView mapa;
    private TextView tvEstadoVideo;
    private VideoView videoView;
    private Button btnGuardarVideo;
    private String caminhoVideo;

    // Android 9 ou anterior: guardar na galeria exige permissão de escrita
    private final ActivityResultLauncher<String> pedidoPermissaoArmazenamento = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            concedida -> {
                if (concedida) {
                    guardarVideoNaGaleria();
                } else {
                    Toast.makeText(this, R.string.resumo_permissao_armazenamento, Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapa.inicializar(getApplicationContext());
        setContentView(R.layout.activity_resumo);

        mapa = findViewById(R.id.map);
        tvEstadoVideo = findViewById(R.id.tvEstadoVideo);
        videoView = findViewById(R.id.videoView);
        btnGuardarVideo = findViewById(R.id.btnGuardarVideo);
        btnGuardarVideo.setOnClickListener(v -> aoGuardarVideo());
        Button btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        final Atividade atividade = GestorExercicio.getInstance().getAtual();
        if (atividade == null) {
            finish();
            return;
        }

        ((TextView) findViewById(R.id.tvTempoTotal)).setText(getString(R.string.resumo_tempo_total,
                Formatacao.formatarTempo(atividade.getTempoDecorrido())));
        ((TextView) findViewById(R.id.tvVelocidadeMedia)).setText(getString(R.string.resumo_velocidade_media,
                Formatacao.formatarVelocidade(atividade.getVelocidadeMedia())));
        ((TextView) findViewById(R.id.tvDistanciaTotal)).setText(getString(R.string.resumo_distancia_total,
                Formatacao.formatarDistancia(atividade.getDistanciaTotal())));

        // rota efetuada (o mapa aqui só serve para ver, sem zoom com os dedos)
        Mapa.configurar(mapa, 16.0);
        mapa.setMultiTouchControls(false);
        List<GeoPoint> pontos = Mapa.paraGeoPoints(atividade.getPercurso());
        mapa.getOverlays().add(Mapa.criarLinha(pontos, Mapa.COR_PERCURSO, 10f));
        Mapa.ajustarAosPontos(mapa, pontos);

        configurarGuardarRota(atividade);
        prepararVideoResumo(atividade);
    }

    /** Modo livre: oferece guardar o percurso feito como rota, para o utilizador o poder repetir. */
    private void configurarGuardarRota(final Atividade atividade) {
        final Button btnGuardarRota = findViewById(R.id.btnGuardarRota);
        if (atividade.getRota().getModo() != Rota.Modo.LIVRE) {
            return;
        }
        btnGuardarRota.setVisibility(View.VISIBLE);
        btnGuardarRota.setOnClickListener(v -> {
            // sem deslocação (ex.: emulador com localização fixa) não há percurso para guardar
            if (atividade.getPercurso().size() < 2 || atividade.getDistanciaTotal() < DISTANCIA_MINIMA_ROTA_M) {
                Toast.makeText(this, R.string.resumo_percurso_curto, Toast.LENGTH_LONG).show();
                return;
            }
            DialogoNomeRota.mostrar(this, nome -> {
                guardarRotaLivre(atividade, nome);
                btnGuardarRota.setEnabled(false);
                btnGuardarRota.setText(R.string.resumo_rota_guardada);
            });
        });
    }

    /** Guarda o percurso (simplificado, para a rota ficar leve) como rota criada. */
    private void guardarRotaLivre(Atividade atividade, String nome) {
        Rota rota = Rota.criada(nome, SimplificarPercurso.simplificar(atividade.getPercurso(),
                DISTANCIA_MINIMA_PONTOS_M, MAXIMO_PONTOS_ROTA));
        RotasGuardadas.guardarCriada(this, rota);
        Toast.makeText(this, R.string.planear_guardada, Toast.LENGTH_SHORT).show();
    }

    /** Mostra o vídeo resumo, ou gera-o a partir das fotos e vídeos do exercício. */
    private void prepararVideoResumo(final Atividade atividade) {
        VideoResumo existente = atividade.getVideoResumo();
        if (existente != null) {
            mostrarVideo(existente.getCaminho());
            return;
        }
        if (atividade.getMultimedia().isEmpty()) {
            tvEstadoVideo.setText(R.string.resumo_sem_capturas);
            return;
        }

        tvEstadoVideo.setText(R.string.resumo_a_gerar);
        File base = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (base == null) {
            base = getFilesDir();
        }
        final File saida = new File(base, "resumo_" + atividade.getDataHoraInicio() + ".mp4");
        if (saida.exists()) {
            saida.delete();
        }

        try {
            GeradorVideoResumo.gerar(this, atividade.getMultimedia(), saida, new GeradorVideoResumo.AoTerminar() {
                @Override
                public void onConcluido(long duracaoMs) {
                    GestorExercicio.getInstance().setVideoResumo(new VideoResumo(saida.getAbsolutePath(), duracaoMs));
                    mostrarVideo(saida.getAbsolutePath());
                }

                @Override
                public void onErro(Exception e) {
                    tvEstadoVideo.setText(R.string.resumo_erro_video);
                }
            });
        } catch (RuntimeException e) {
            tvEstadoVideo.setText(R.string.resumo_erro_video);
        }
    }

    /** Botão "Guardar vídeo na galeria": antes do Android 10 é preciso a permissão de escrita. */
    private void aoGuardarVideo() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            pedidoPermissaoArmazenamento.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return;
        }
        guardarVideoNaGaleria();
    }

    /** Copia o vídeo resumo para a galeria (Filmes/WayLine), numa thread de fundo. */
    private void guardarVideoNaGaleria() {
        if (caminhoVideo == null) {
            return;
        }
        btnGuardarVideo.setEnabled(false);
        final File origem = new File(caminhoVideo);
        final String nome = "WayLine_" + origem.getName();
        new Thread(() -> {
            boolean ok = true;
            try {
                Galeria.guardarVideo(getApplicationContext(), origem, nome);
            } catch (IOException e) {
                ok = false;
            }
            final boolean sucesso = ok;
            runOnUiThread(() -> {
                if (sucesso) {
                    btnGuardarVideo.setText(R.string.resumo_video_guardado_btn);
                    Toast.makeText(this, R.string.resumo_video_guardado, Toast.LENGTH_LONG).show();
                } else {
                    btnGuardarVideo.setEnabled(true);
                    Toast.makeText(this, R.string.resumo_video_erro_guardar, Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    /** Mostra o vídeo resumo com os controlos de reprodução e o botão de guardar na galeria. */
    private void mostrarVideo(String caminho) {
        caminhoVideo = caminho;
        btnGuardarVideo.setVisibility(View.VISIBLE);
        tvEstadoVideo.setText(R.string.resumo_video_pronto);
        videoView.setVisibility(View.VISIBLE);
        videoView.setMediaController(new MediaController(this));
        videoView.setVideoPath(caminho);
        videoView.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapa.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapa.onPause();
    }

    @Override
    protected void onDestroy() {
        mapa.onDetach();
        super.onDestroy();
    }
}
