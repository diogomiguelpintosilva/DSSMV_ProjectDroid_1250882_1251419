package com.example.atividadesfisicas.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.atividadesfisicas.R;
import com.example.atividadesfisicas.data.RotaRepository;
import com.example.atividadesfisicas.data.SincronizacaoCloud;
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

        final TextView tvDica = findViewById(R.id.tvDica);
        if (modo == Rota.Modo.CRIADA && !rotas.isEmpty()) {
            tvDica.setVisibility(android.view.View.VISIBLE);
        }

        if (rotas.isEmpty()) {
            tvVazio.setVisibility(android.view.View.VISIBLE);
        }

        final ArrayAdapter<Rota> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, rotas);
        lvRotas.setAdapter(adapter);

        if (modo == Rota.Modo.CRIADA) {
            // rotas guardadas noutro dispositivo com a mesma conta
            SincronizacaoCloud.sincronizar(this, () -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                adapter.clear();
                adapter.addAll(RotaRepository.listarCriadas(this));
                adapter.notifyDataSetChanged();
                tvVazio.setVisibility(adapter.isEmpty()
                        ? android.view.View.VISIBLE : android.view.View.GONE);
                tvDica.setVisibility(adapter.isEmpty()
                        ? android.view.View.GONE : android.view.View.VISIBLE);
            });

            // manter premido numa rota criada permite apagá-la (as predefinidas não se apagam)
            lvRotas.setOnItemLongClickListener((parent, view, position, id) -> {
                final Rota alvo = adapter.getItem(position);
                if (alvo == null) {
                    return true;
                }
                new AlertDialog.Builder(this)
                        .setTitle(R.string.escolher_apagar_titulo)
                        .setMessage(getString(R.string.escolher_apagar_mensagem, alvo.getNome()))
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.escolher_apagar, (dialog, which) -> {
                            RotaRepository.apagarCriada(this, alvo.getId());
                            adapter.remove(alvo);
                            adapter.notifyDataSetChanged();
                            if (adapter.isEmpty()) {
                                tvVazio.setVisibility(android.view.View.VISIBLE);
                                tvDica.setVisibility(android.view.View.GONE);
                            }
                            android.widget.Toast.makeText(this, R.string.escolher_apagada,
                                    android.widget.Toast.LENGTH_SHORT).show();
                        })
                        .show();
                return true;
            });
        }

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
