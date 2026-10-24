package com.example.atividadesfisicas.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.atividadesfisicas.R;
import com.example.atividadesfisicas.data.RotaRepository;
import com.example.atividadesfisicas.map.MapaHelper;
import com.example.atividadesfisicas.model.PontoGPS;
import com.example.atividadesfisicas.model.Rota;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

/**
 * UC1 Planear rota: o utilizador toca no mapa para desenhar a rota e depois guarda-a.
 */
public class PlanearRotaActivity extends AppCompatActivity {

    private MapView map;
    private TextView tvInfo;
    private Polyline linha;
    private MyLocationNewOverlay minhaLocalizacao;
    private final List<GeoPoint> pontos = new ArrayList<>();
    private final List<Marker> marcadores = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapaHelper.inicializar(getApplicationContext());
        setContentView(R.layout.activity_planear_rota);

        map = findViewById(R.id.map);
        tvInfo = findViewById(R.id.tvInfo);
        Button btnDesfazer = findViewById(R.id.btnDesfazer);
        Button btnGuardar = findViewById(R.id.btnGuardarRota);

        MapaHelper.configurar(map, 16.0);
        GeoPoint inicio = MapaHelper.ultimaPosicaoConhecida(this);
        if (inicio == null) {
            inicio = new GeoPoint(41.1458, -8.6140); // centro do Porto, se não houver GPS
        }
        MapaHelper.centrar(map, inicio);

        // toque no mapa = novo ponto da rota
        MapEventsReceiver receiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                adicionarPonto(p);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };
        map.getOverlays().add(0, new MapEventsOverlay(receiver));

        linha = MapaHelper.criarLinha(new ArrayList<GeoPoint>(), 0xFF1565C0, 10f);
        map.getOverlays().add(linha);

        // ícone da posição do utilizador (o mapa centra-se nele quando há posição)
        minhaLocalizacao = MapaHelper.adicionarMinhaLocalizacao(this, map, true);

        btnDesfazer.setOnClickListener(v -> desfazerUltimoPonto());
        btnGuardar.setOnClickListener(v -> pedirNomeEGuardar());
        atualizarInfo();
    }

    private void adicionarPonto(GeoPoint p) {
        pontos.add(p);
        Marker marcador = new Marker(map);
        marcador.setPosition(p);
        marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marcador.setTitle("Ponto " + pontos.size());
        marcadores.add(marcador);
        map.getOverlays().add(marcador);
        atualizarLinha();
    }

    private void desfazerUltimoPonto() {
        if (pontos.isEmpty()) {
            return;
        }
        pontos.remove(pontos.size() - 1);
        Marker ultimo = marcadores.remove(marcadores.size() - 1);
        map.getOverlays().remove(ultimo);
        atualizarLinha();
    }

    private void atualizarLinha() {
        linha.setPoints(new ArrayList<>(pontos));
        map.invalidate();
        atualizarInfo();
    }

    private void atualizarInfo() {
        tvInfo.setText(getString(R.string.planear_info, pontos.size()));
    }

    private void pedirNomeEGuardar() {
        if (pontos.size() < 2) {
            Toast.makeText(this, R.string.planear_minimo_pontos, Toast.LENGTH_SHORT).show();
            return;
        }
        final EditText etNome = new EditText(this);
        etNome.setHint(R.string.planear_nome_hint);

        new AlertDialog.Builder(this)
                .setTitle(R.string.planear_guardar_titulo)
                .setView(etNome)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.guardar, (dialog, which) -> {
                    String nome = etNome.getText().toString().trim();
                    if (nome.isEmpty()) {
                        nome = "Rota " + (RotaRepository.listarCriadas(this).size() + 1);
                    }
                    guardar(nome);
                })
                .show();
    }

    private void guardar(String nome) {
        Rota rota = new Rota("criada_" + System.currentTimeMillis(), nome, Rota.Modo.CRIADA);
        for (GeoPoint p : pontos) {
            rota.adicionarPonto(new PontoGPS(p.getLatitude(), p.getLongitude(), 0, 0));
        }
        RotaRepository.guardarCriada(this, rota);
        Toast.makeText(this, R.string.planear_guardada, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
        minhaLocalizacao.enableMyLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
        minhaLocalizacao.disableMyLocation();
    }
}