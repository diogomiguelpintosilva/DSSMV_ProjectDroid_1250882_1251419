package com.example.atividadesfisicas.ecras;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.atividadesfisicas.R;
import com.example.atividadesfisicas.dados.HistoricoGuardado;
import com.example.atividadesfisicas.dados.SincronizacaoCloud;
import com.example.atividadesfisicas.modelo.RegistoAtividade;

import java.util.ArrayList;

/**
 * Histórico de Atividade: lista de todas as atividades da conta (da mais recente para a mais antiga).
 * Mostra primeiro o que está no telemóvel e atualiza com o que vem da cloud (outros dispositivos).
 */
public class HistoricoActivity extends AppCompatActivity {

    private ArrayAdapter<RegistoAtividade> adaptador;
    private TextView tvVazio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // reutiliza o layout da lista de rotas (título, texto de lista vazia e lista)
        setContentView(R.layout.activity_escolher_rota);

        TextView tvTitulo = findViewById(R.id.tvTitulo);
        tvVazio = findViewById(R.id.tvVazio);
        ListView lvRotas = findViewById(R.id.lvRotas);

        tvTitulo.setText(R.string.historico_titulo);
        tvVazio.setText(R.string.historico_vazio);

        adaptador = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                new ArrayList<RegistoAtividade>(HistoricoGuardado.listar(this)));
        lvRotas.setAdapter(adaptador);
        atualizarVazio();

        lvRotas.setOnItemClickListener((lista, linha, posicao, id) -> {
            RegistoAtividade escolhida = adaptador.getItem(posicao);
            if (escolhida == null) {
                return;
            }
            Intent intent = new Intent(this, HistoricoDetalheActivity.class);
            intent.putExtra(HistoricoDetalheActivity.EXTRA_ID, escolhida.getId());
            startActivity(intent);
        });

        SincronizacaoCloud.sincronizar(this, this::recarregar);
    }

    /** Atualiza a lista com o histórico depois da sincronização. */
    private void recarregar() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        adaptador.clear();
        adaptador.addAll(HistoricoGuardado.listar(this));
        adaptador.notifyDataSetChanged();
        atualizarVazio();
    }

    private void atualizarVazio() {
        tvVazio.setVisibility(adaptador.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
