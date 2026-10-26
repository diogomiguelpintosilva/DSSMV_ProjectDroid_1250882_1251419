package com.example.atividadesfisicas.ecras;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.atividadesfisicas.R;
import com.example.atividadesfisicas.autenticacao.Autenticacao;
import com.example.atividadesfisicas.autenticacao.ErroAutenticacao;
import com.example.atividadesfisicas.autenticacao.RespostaLogin;
import com.example.atividadesfisicas.autenticacao.Sessao;
import com.example.atividadesfisicas.dados.SincronizacaoCloud;
import com.example.atividadesfisicas.modelo.Utilizador;

/**
 * Ecrã inicial (UC0): fazer login ou criar uma conta nova, ambos no Firebase por HTTP.
 * Os pedidos fazem-se numa thread de fundo (a rede não pode ser usada na thread principal)
 * e o resultado volta à thread principal com runOnUiThread, porque só ela pode mexer no ecrã.
 */
public class LoginActivity extends AppCompatActivity {

    /** O Firebase exige palavras-passe com pelo menos 6 caracteres. */
    private static final int TAMANHO_MINIMO_PASSWORD = 6;

    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;
    private Button btnCriarConta;
    private TextView tvErro;
    private TextView tvSucesso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnCriarConta = findViewById(R.id.btnCriarConta);
        tvErro = findViewById(R.id.tvErro);
        tvSucesso = findViewById(R.id.tvSucesso);

        btnLogin.setOnClickListener(v -> autenticar(false));
        btnCriarConta.setOnClickListener(v -> autenticar(true));
    }

    /**
     * Faz login ou cria uma conta nova, conforme o botão carregado.
     *
     * @param novaConta true para criar uma conta, false para fazer login
     */
    private void autenticar(final boolean novaConta) {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            mostrarErro(getString(R.string.login_campos_vazios));
            return;
        }
        if (novaConta && password.length() < TAMANHO_MINIMO_PASSWORD) {
            mostrarErro(getString(R.string.login_password_curta));
            return;
        }

        tvErro.setText("");
        tvSucesso.setText("");
        ativarBotoes(false); // evita vários pedidos ao mesmo tempo

        new Thread(() -> {
            try {
                // a chamada à rede demora, por isso corre aqui, fora da thread principal
                final RespostaLogin resposta = novaConta
                        ? Autenticacao.criarConta(email, password)
                        : Autenticacao.login(email, password);
                runOnUiThread(() -> {
                    if (novaConta) {
                        aoContaCriada();
                    } else {
                        aoLoginComSucesso(resposta);
                    }
                });
            } catch (ErroAutenticacao e) {
                runOnUiThread(() -> {
                    mostrarErro(e.getMessage());
                    ativarBotoes(true);
                });
            }
        }).start();
    }

    /** A conta foi criada: o utilizador já pode fazer login com o email e a palavra-passe escritos. */
    private void aoContaCriada() {
        tvSucesso.setText(R.string.login_conta_criada);
        ativarBotoes(true);
    }

    /** Guarda a sessão, traz os dados da cloud e abre o menu principal. */
    private void aoLoginComSucesso(RespostaLogin resposta) {
        Sessao.getInstance().setUtilizador(new Utilizador(resposta.getEmail(), resposta.getUid()));
        Sessao.getInstance().setTokens(resposta.getIdToken(), resposta.getRefreshToken(),
                resposta.getExpiresInSegundos());
        // traz as rotas e o histórico da conta (e envia o que ficou só no telemóvel)
        SincronizacaoCloud.sincronizar(this, null);
        startActivity(new Intent(this, MenuPrincipalActivity.class));
        finish(); // para o botão Voltar não regressar ao login
    }

    private void mostrarErro(String mensagem) {
        tvSucesso.setText("");
        tvErro.setText(mensagem);
    }

    private void ativarBotoes(boolean ativos) {
        btnLogin.setEnabled(ativos);
        btnCriarConta.setEnabled(ativos);
    }
}
