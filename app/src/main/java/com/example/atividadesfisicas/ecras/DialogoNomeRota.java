package com.example.atividadesfisicas.ecras;

import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.atividadesfisicas.R;
import com.example.atividadesfisicas.dados.RotasGuardadas;

/**
 * Caixa de diálogo que pede o nome de uma rota antes de a guardar.
 * Usada no Planear Rota (UC1) e no Guardar Rota do resumo do modo livre.
 */
final class DialogoNomeRota {

    /** Recebe o nome escolhido pelo utilizador (nunca vazio). */
    interface AoEscolherNome {
        void nomeEscolhido(String nome);
    }

    private DialogoNomeRota() {
    }

    /** Mostra o diálogo; se o nome ficar vazio, usa "Rota N". */
    static void mostrar(AppCompatActivity ecra, AoEscolherNome aoEscolher) {
        final EditText campoNome = new EditText(ecra);
        campoNome.setHint(R.string.planear_nome_hint);
        new AlertDialog.Builder(ecra)
                .setTitle(R.string.planear_guardar_titulo)
                .setView(campoNome)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.guardar, (dialogo, botao) -> {
                    String nome = campoNome.getText().toString().trim();
                    if (nome.isEmpty()) {
                        nome = ecra.getString(R.string.rota_nome_por_omissao,
                                RotasGuardadas.listarCriadas(ecra).size() + 1);
                    }
                    aoEscolher.nomeEscolhido(nome);
                })
                .show();
    }
}
