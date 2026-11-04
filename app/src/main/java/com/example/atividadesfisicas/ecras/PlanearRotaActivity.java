package com.example.atividadesfisicas.ecras;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.atividadesfisicas.R;
import com.example.atividadesfisicas.dados.RotasGuardadas;
import com.example.atividadesfisicas.mapa.Mapa;
import com.example.atividadesfisicas.modelo.PontoGPS;
import com.example.atividadesfisicas.modelo.Rota;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

import com.example.atividadesfisicas.dados.RoteamentoRuas;
import com.example.atividadesfisicas.utilitarios.SimplificarPercurso;
import java.io.IOException;

/**
 * UC1 Planear rota: o utilizador toca no mapa para desenhar a rota e depois guarda-a com um nome.
 */
public class PlanearRotaActivity extends AppCompatActivity {

    /** Centro do Porto, se ainda não houver posição do utilizador. */
    private static final GeoPoint CENTRO_POR_OMISSAO = new GeoPoint(41.1458, -8.6140);

    private MapView mapa;
    private TextView tvInfo;
    private Polyline linha;
    private MyLocationNewOverlay minhaLocalizacao;
    // listas paralelas: o ponto i corresponde ao marcador i
    private final List<GeoPoint> pontos = new ArrayList<>();
    private final List<Marker> marcadores = new ArrayList<>();

    /** Máximo de pontos por rota (o serviço de rotas aceita poucos pontos por pedido). */
    private static final int MAXIMO_PONTOS = 25;
    // percurso pelas ruas já calculado para os pontos atuais (null enquanto não chega a resposta)
    private List<PontoGPS> percursoPorRuas;
    // aumenta a cada alteração aos pontos, para ignorar respostas antigas
    private int versaoPedido = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapa.inicializar(getApplicationContext());
        setContentView(R.layout.activity_planear_rota);

        mapa = findViewById(R.id.map);
        tvInfo = findViewById(R.id.tvInfo);
        Button btnDesfazer = findViewById(R.id.btnDesfazer);
        Button btnGuardar = findViewById(R.id.btnGuardarRota);

        Mapa.configurar(mapa, 16.0);
        GeoPoint inicio = Mapa.ultimaPosicaoConhecida(this);
        Mapa.centrar(mapa, (inicio != null) ? inicio : CENTRO_POR_OMISSAO);

        // toque no mapa = novo ponto da rota (camada na posição 0, por baixo dos marcadores)
        MapEventsReceiver recetorToques = new MapEventsReceiver() {
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
        mapa.getOverlays().add(0, new MapEventsOverlay(recetorToques));

        linha = Mapa.criarLinha(new ArrayList<GeoPoint>(), Mapa.COR_ROTA_PLANEADA, 10f);
        mapa.getOverlays().add(linha);

        // ícone da posição do utilizador (o mapa centra-se nele quando há posição)
        minhaLocalizacao = Mapa.adicionarMinhaLocalizacao(this, mapa, true);
        findViewById(R.id.btnCentrar).setOnClickListener(v -> Mapa.centrarNoUtilizador(this, mapa, minhaLocalizacao));

        btnDesfazer.setOnClickListener(v -> desfazerUltimoPonto());
        btnGuardar.setOnClickListener(v -> pedirNomeEGuardar());
        atualizarInfo();
    }

    private void adicionarPonto(GeoPoint p) {
        if (pontos.size() >= MAXIMO_PONTOS) {
            Toast.makeText(this, getString(R.string.planear_maximo_pontos, MAXIMO_PONTOS),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        pontos.add(p);
        Marker marcador = new Marker(mapa);
        marcador.setPosition(p);
        marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marcador.setTitle("Ponto " + pontos.size());
        marcadores.add(marcador);
        mapa.getOverlays().add(marcador);
        atualizarLinha();
    }

    private void desfazerUltimoPonto() {
        if (pontos.isEmpty()) {
            return;
        }
        pontos.remove(pontos.size() - 1);
        Marker ultimo = marcadores.remove(marcadores.size() - 1);
        mapa.getOverlays().remove(ultimo);
        atualizarLinha();
    }

    private void atualizarLinha() {
        // mostra já uma linha reta, enquanto o percurso pelas ruas não chega
        linha.setPoints(new ArrayList<>(pontos));
        mapa.invalidate();
        atualizarInfo();

        percursoPorRuas = null;
        versaoPedido++;
        if (pontos.size() < 2) {
            return;
        }

        final int versao = versaoPedido;
        final List<PontoGPS> pontosChave = new ArrayList<>();
        for (GeoPoint p : pontos) {
            pontosChave.add(new PontoGPS(p.getLatitude(), p.getLongitude(), 0, 0));
        }
        new Thread(() -> {
            try {
                final List<PontoGPS> ruas = RoteamentoRuas.porRuas(pontosChave);
                runOnUiThread(() -> {
                    // ignora respostas antigas: o utilizador já mexeu nos pontos entretanto
                    if (versao != versaoPedido || isFinishing() || isDestroyed()) {
                        return;
                    }
                    percursoPorRuas = ruas;
                    linha.setPoints(Mapa.paraGeoPoints(ruas));
                    mapa.invalidate();
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    if (versao == versaoPedido && !isFinishing() && !isDestroyed()) {
                        Toast.makeText(this, R.string.rota_sem_ruas, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    private void atualizarInfo() {
        tvInfo.setText(getString(R.string.planear_info, pontos.size()));
    }

    private void pedirNomeEGuardar() {
        if (pontos.size() < 2) {
            Toast.makeText(this, R.string.planear_minimo_pontos, Toast.LENGTH_SHORT).show();
            return;
        }
        DialogoNomeRota.mostrar(this, this::guardar);
    }

    private void guardar(String nome) {
        List<PontoGPS> pontosRota = new ArrayList<>();
        if (percursoPorRuas != null) {
            // o percurso pelas ruas, reduzido para a rota ficar leve
            pontosRota = SimplificarPercurso.simplificar(percursoPorRuas, 5, 800);
        } else {
            // o percurso ainda não chegou: guarda os pontos marcados (serão ligados pelas ruas ao iniciar)
            for (GeoPoint p : pontos) {
                pontosRota.add(new PontoGPS(p.getLatitude(), p.getLongitude(), 0, 0));
            }
        }
        RotasGuardadas.guardarCriada(this, Rota.criada(nome, pontosRota));
        Toast.makeText(this, R.string.planear_guardada, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapa.onResume();
        minhaLocalizacao.enableMyLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapa.onPause();
        minhaLocalizacao.disableMyLocation();
    }

    @Override
    protected void onDestroy() {
        mapa.onDetach();
        super.onDestroy();
    }
}
