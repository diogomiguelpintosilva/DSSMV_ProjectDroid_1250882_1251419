package com.example.atividadesfisicas.ecras;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.atividadesfisicas.R;
import com.example.atividadesfisicas.autenticacao.Sessao;
import com.example.atividadesfisicas.dados.GestorExercicio;
import com.example.atividadesfisicas.dados.SincronizacaoCloud;
import com.example.atividadesfisicas.modelo.Rota;
import com.example.atividadesfisicas.utilitarios.Permissoes;

import java.util.Map;

/**
 * Menu principal: Planear rota (UC1), Escolher rota predefinida (UC3), Escolher rota criada (UC4),
 * Modo livre (UC5) e Histórico de Atividade.
 */
public class MenuPrincipalActivity extends AppCompatActivity {

    private final ActivityResultLauncher<String[]> pedidoPermissoes = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            (Map<String, Boolean> resultado) -> {
                if (!Permissoes.temLocalizacao(this)) {
                    Toast.makeText(this, R.string.permissao_localizacao_necessaria, Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        TextView tvBemVindo = findViewById(R.id.tvBemVindo);
        if (Sessao.getInstance().getUtilizador() != null) {
            tvBemVindo.setText(getString(R.string.menu_bemvindo,
                    Sessao.getInstance().getUtilizador().getEmail()));
        }

        Button btnPlanear = findViewById(R.id.btnPlanearRota);
        Button btnPredefinida = findViewById(R.id.btnRotaPredefinida);
        Button btnCriada = findViewById(R.id.btnRotaCriada);
        Button btnLivre = findViewById(R.id.btnModoLivre);
        Button btnHistorico = findViewById(R.id.btnHistorico);

        btnPlanear.setOnClickListener(v ->
                startActivity(new Intent(this, PlanearRotaActivity.class)));

        btnPredefinida.setOnClickListener(v -> abrirEscolhaDeRota(Rota.Modo.PREDEFINIDA));
        btnCriada.setOnClickListener(v -> abrirEscolhaDeRota(Rota.Modo.CRIADA));

        btnLivre.setOnClickListener(v -> {
            Intent intent = new Intent(this, ExercicioActivity.class);
            intent.putExtra(ExercicioActivity.EXTRA_MODO, Rota.Modo.LIVRE.name());
            startActivity(intent);
        });
        btnHistorico.setOnClickListener(v -> startActivity(new Intent(this, HistoricoActivity.class)));

        // traz da cloud as rotas e o histórico da conta (e envia o que ficou só no telemóvel)
        SincronizacaoCloud.sincronizar(this, null);

        if (!Permissoes.temLocalizacao(this)) {
            pedidoPermissoes.launch(Permissoes.permissoesPedidas());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // se há um exercício a decorrer (ex.: voltou à app pelo ícone), regressa ao ecrã do exercício
        if (GestorExercicio.getInstance().isAtiva()) {
            startActivity(new Intent(this, ExercicioActivity.class));
        }
    }

    private void abrirEscolhaDeRota(Rota.Modo modo) {
        Intent intent = new Intent(this, EscolherRotaActivity.class);
        intent.putExtra(EscolherRotaActivity.EXTRA_MODO, modo.name());
        startActivity(intent);
    }
}