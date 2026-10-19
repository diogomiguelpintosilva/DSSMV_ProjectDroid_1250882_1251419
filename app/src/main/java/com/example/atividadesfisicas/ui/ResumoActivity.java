package com.example.atividadesfisicas.ui;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.atividadesfisicas.R;
import com.example.atividadesfisicas.data.AtividadeManager;
import com.example.atividadesfisicas.map.MapaHelper;
import com.example.atividadesfisicas.media.VideoResumoBuilder;
import com.example.atividadesfisicas.model.Atividade;
import com.example.atividadesfisicas.model.VideoResumo;
import com.example.atividadesfisicas.util.FormatUtil;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.File;
import java.util.List;

/**
 * Ecrã final (UC9): rota efetuada, velocidade média, tempo total, distância total e vídeo resumo (máx. 2 min).
 */
public class ResumoActivity extends AppCompatActivity {

    private MapView map;
    private TextView tvEstadoVideo;
    private VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapaHelper.inicializar(getApplicationContext());
        setContentView(R.layout.activity_resumo);

        map = findViewById(R.id.map);
        tvEstadoVideo = findViewById(R.id.tvEstadoVideo);
        videoView = findViewById(R.id.videoView);
        Button btnVoltar = findViewById(R.id.btnVoltar);

        final Atividade atividade = AtividadeManager.getInstance().getAtual();
        if (atividade == null) {
            finish();
            return;
        }

        ((TextView) findViewById(R.id.tvTempoTotal)).setText(getString(R.string.resumo_tempo_total,
                FormatUtil.formatarTempo(atividade.getTempoDecorrido())));
        ((TextView) findViewById(R.id.tvVelocidadeMedia)).setText(getString(R.string.resumo_velocidade_media,
                FormatUtil.formatarVelocidade(atividade.getVelocidadeMedia())));
        ((TextView) findViewById(R.id.tvDistanciaTotal)).setText(getString(R.string.resumo_distancia_total,
                FormatUtil.formatarDistancia(atividade.getDistanciaTotal())));

        // rota efetuada
        MapaHelper.configurar(map, 16.0);
        map.setMultiTouchControls(false);
        List<GeoPoint> pontos = MapaHelper.paraGeoPoints(atividade.getPercurso());
        map.getOverlays().add(MapaHelper.criarLinha(pontos, 0xFFD32F2F, 10f));
        MapaHelper.ajustarAosPontos(map, pontos);

        btnVoltar.setOnClickListener(v -> finish());

        prepararVideoResumo(atividade);
    }

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
            VideoResumoBuilder.gerar(this, atividade.getMultimedia(), saida, new VideoResumoBuilder.Listener() {
                @Override
                public void onConcluido(long duracaoMs) {
                    AtividadeManager.getInstance().setVideoResumo(new VideoResumo(saida.getAbsolutePath(), duracaoMs));
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

    private void mostrarVideo(String caminho) {
        tvEstadoVideo.setText(R.string.resumo_video_pronto);
        videoView.setVisibility(View.VISIBLE);
        videoView.setMediaController(new MediaController(this));
        videoView.setVideoPath(caminho);
        videoView.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }

    @Override
    protected void onDestroy() {
        map.onDetach();
        super.onDestroy();
    }
}
