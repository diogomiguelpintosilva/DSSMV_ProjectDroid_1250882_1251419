package com.example.atividadesfisicas.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.atividadesfisicas.R;
import com.example.atividadesfisicas.data.HistoricoRepository;
import com.example.atividadesfisicas.map.MapaHelper;
import com.example.atividadesfisicas.model.RegistoAtividade;
import com.example.atividadesfisicas.util.FormatUtil;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.List;

/**
 * Detalhe de uma atividade do histórico: rota feita, tempo, velocidade média, distância e capturas.
 */
public class HistoricoDetalheActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "registo_id";

    private MapView map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapaHelper.inicializar(getApplicationContext());
        setContentView(R.layout.activity_historico_detalhe);

        RegistoAtividade registo = HistoricoRepository.procurar(this, getIntent().getStringExtra(EXTRA_ID));
        if (registo == null) {
            finish();
            return;
        }

        map = findViewById(R.id.map);
        ((TextView) findViewById(R.id.tvTitulo)).setText(getString(R.string.historico_rota, registo.getNomeRota()));
        ((TextView) findViewById(R.id.tvData)).setText(registo.getDataFormatada());
        ((TextView) findViewById(R.id.tvTempoTotal)).setText(getString(R.string.resumo_tempo_total,
                FormatUtil.formatarTempo(registo.getTempoMs())));
        ((TextView) findViewById(R.id.tvVelocidadeMedia)).setText(getString(R.string.resumo_velocidade_media,
                FormatUtil.formatarVelocidade(registo.getVelocidadeMediaKmh())));
        ((TextView) findViewById(R.id.tvDistanciaTotal)).setText(getString(R.string.resumo_distancia_total,
                FormatUtil.formatarDistancia(registo.getDistanciaMetros())));
        ((TextView) findViewById(R.id.tvAvisos)).setText(getString(R.string.historico_avisos, registo.getNumAvisos()));
        ((TextView) findViewById(R.id.tvCapturas)).setText(getString(R.string.historico_capturas,
                registo.getNumFotos(), registo.getNumVideos()));

        MapaHelper.configurar(map, 16.0);
        List<GeoPoint> pontos = MapaHelper.paraGeoPoints(registo.getPercurso());
        map.getOverlays().add(MapaHelper.criarLinha(pontos, 0xFFD32F2F, 10f));
        MapaHelper.ajustarAosPontos(map, pontos);

        Button btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) {
            map.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) {
            map.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        if (map != null) {
            map.onDetach();
        }
        super.onDestroy();
    }
}
