package com.example.atividadesfisicas.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.atividadesfisicas.R;
import com.example.atividadesfisicas.data.RotaRepository;
import com.example.atividadesfisicas.model.Rota;

import java.util.List;

/**
 * UC3 (rota predefinida) e UC4 (rota criada previamente): lista de rotas numa ListView com ArrayAdapter
 * (aula TP3/TP4). Ao escolher uma rota, arranca a atividade.
 */
public class EscolherRotaActivity extends AppCompatActivity {

    public static final String EXTRA_MODO = "modo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_escolher_rota);

        TextView tvTitulo = findViewById(R.id.tvTitulo);
        TextView tvVazio = findViewById(R.id.tvVazio);
        ListView lvRotas = findViewById(R.id.lvRotas);

        String modoTexto = getIntent().getStringExtra(EXTRA_MODO);
        Rota.Modo modo = (modoTexto == null) ? Rota.Modo.PREDEFINIDA : Rota.Modo.valueOf(modoTexto);

        List<Rota> rotas;
        if (modo == Rota.Modo.PREDEFINIDA) {
            tvTitulo.setText(R.string.escolher_titulo_predefinida);
            rotas = RotaRepository.listarPredefinidas();
        } else {
            tvTitulo.setText(R.string.escolher_titulo_criada);
            rotas = RotaRepository.listarCriadas(this);
        }

        if (rotas.isEmpty()) {
            tvVazio.setVisibility(android.view.View.VISIBLE);
        }

        final ArrayAdapter<Rota> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, rotas);
        lvRotas.setAdapter(adapter);

        lvRotas.setOnItemClickListener((parent, view, position, id) -> {
            Rota escolhida = adapter.getItem(position);
            if (escolhida == null) {
                return;
            }
            Intent intent = new Intent(this, AtividadeActivity.class);
            intent.putExtra(AtividadeActivity.EXTRA_MODO, escolhida.getModo().name());
            intent.putExtra(AtividadeActivity.EXTRA_ROTA_ID, escolhida.getId());
            startActivity(intent);
            finish();
        });
    }
}
