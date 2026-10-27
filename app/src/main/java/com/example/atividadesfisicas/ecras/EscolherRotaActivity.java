package com.example.atividadesfisicas.ecras;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.atividadesfisicas.R;
import com.example.atividadesfisicas.dados.RotasGuardadas;
import com.example.atividadesfisicas.dados.SincronizacaoCloud;
import com.example.atividadesfisicas.modelo.Rota;

import java.util.List;

/**
 * UC3 (rota predefinida) e UC4 (rota criada previamente): lista de rotas numa ListView com ArrayAdapter.
 * Ao escolher uma rota, abre o ecrã do exercício. As rotas criadas podem ser apagadas (toque longo).
 */
public class EscolherRotaActivity extends AppCompatActivity {

    public static final String EXTRA_MODO = "modo";

    private ArrayAdapter<Rota> adaptador;
    private TextView tvVazio;
    private TextView tvDica;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_escolher_rota);

        TextView tvTitulo = findViewById(R.id.tvTitulo);
        tvVazio = findViewById(R.id.tvVazio);
        tvDica = findViewById(R.id.tvDica);
        ListView lvRotas = findViewById(R.id.lvRotas);

        String modoTexto = getIntent().getStringExtra(EXTRA_MODO);
        Rota.Modo modo = (modoTexto == null) ? Rota.Modo.PREDEFINIDA : Rota.Modo.valueOf(modoTexto);
        boolean criadas = (modo == Rota.Modo.CRIADA);

        List<Rota> rotas;
        if (criadas) {
            tvTitulo.setText(R.string.escolher_titulo_criada);
            rotas = RotasGuardadas.listarCriadas(this);
        } else {
            tvTitulo.setText(R.string.escolher_titulo_predefinida);
            rotas = RotasGuardadas.listarPredefinidas();
        }

        adaptador = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rotas);
        lvRotas.setAdapter(adaptador);
        atualizarTextos(criadas);

        if (criadas) {
            // rotas guardadas noutro dispositivo com a mesma conta
            SincronizacaoCloud.sincronizar(this, () -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                adaptador.clear();
                adaptador.addAll(RotasGuardadas.listarCriadas(this));
                adaptador.notifyDataSetChanged();
                atualizarTextos(true);
            });

            // manter premida uma rota criada permite apagá-la (as predefinidas não se apagam)
            lvRotas.setOnItemLongClickListener((lista, linha, posicao, id) -> {
                Rota alvo = adaptador.getItem(posicao);
                if (alvo != null) {
                    confirmarApagar(alvo);
                }
                return true; // o toque longo foi tratado (não abre a rota)
            });
        }

        lvRotas.setOnItemClickListener((lista, linha, posicao, id) -> {
            Rota escolhida = adaptador.getItem(posicao);
            if (escolhida == null) {
                return;
            }
            Intent intent = new Intent(this, ExercicioActivity.class);
            intent.putExtra(ExercicioActivity.EXTRA_MODO, escolhida.getModo().name());
            intent.putExtra(ExercicioActivity.EXTRA_ROTA_ID, escolhida.getId());
            startActivity(intent);
            finish();
        });
    }

    /** Pede confirmação e apaga a rota (no telemóvel e na cloud). */
    private void confirmarApagar(Rota alvo) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.escolher_apagar_titulo)
                .setMessage(getString(R.string.escolher_apagar_mensagem, alvo.getNome()))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.escolher_apagar, (dialogo, botao) -> {
                    RotasGuardadas.apagarCriada(this, alvo.getId());
                    adaptador.remove(alvo);
                    adaptador.notifyDataSetChanged();
                    atualizarTextos(true);
                    Toast.makeText(this, R.string.escolher_apagada, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    /** Texto de lista vazia e, nas rotas criadas, a dica para apagar. */
    private void atualizarTextos(boolean criadas) {
        boolean vazia = adaptador.isEmpty();
        tvVazio.setVisibility(vazia ? View.VISIBLE : View.GONE);
        tvDica.setVisibility(criadas && !vazia ? View.VISIBLE : View.GONE);
    }
}
