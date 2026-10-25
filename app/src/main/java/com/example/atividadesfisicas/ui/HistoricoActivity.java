package com.example.atividadesfisicas.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.atividadesfisicas.R;
import com.example.atividadesfisicas.data.HistoricoRepository;
import com.example.atividadesfisicas.data.SincronizacaoCloud;
import com.example.atividadesfisicas.model.RegistoAtividade;

import java.util.ArrayList;

/**
 * Histórico de Atividade: lista de todas as atividades da conta (da mais recente para a mais antiga).
 * Mostra primeiro o que está no telemóvel e atualiza com o que vem da cloud (outros dispositivos).
 */
public class HistoricoActivity extends AppCompatActivity {

    private ArrayAdapter<RegistoAtividade> adapter;
    private TextView tvVazio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_escolher_rota);

        TextView tvTitulo = findViewById(R.id.tvTitulo);
        tvVazio = findViewById(R.id.tvVazio);
        ListView lvRotas = findViewById(R.id.lvRotas);

        tvTitulo.setText(R.string.historico_titulo);
        tvVazio.setText(R.string.historico_vazio);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                new ArrayList<RegistoAtividade>(HistoricoRepository.listar(this)));
        lvRotas.setAdapter(adapter);
        atualizarVazio();

        lvRotas.setOnItemClickListener((parent, view, position, id) -> {
            RegistoAtividade escolhida = adapter.getItem(position);
            if (escolhida == null) {
                return;
            }
            Intent intent = new Intent(this, HistoricoDetalheActivity.class);
            intent.putExtra(HistoricoDetalheActivity.EXTRA_ID, escolhida.getId());
            startActivity(intent);
        });

        SincronizacaoCloud.sincronizar(this, this::recarregar);
    }

    private void recarregar() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        adapter.clear();
        adapter.addAll(HistoricoRepository.listar(this));
        adapter.notifyDataSetChanged();
        atualizarVazio();
    }

    private void atualizarVazio() {
        tvVazio.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
