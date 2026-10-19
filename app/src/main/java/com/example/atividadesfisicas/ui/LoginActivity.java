package com.example.atividadesfisicas.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.atividadesfisicas.R;
import com.example.atividadesfisicas.auth.AuthApi;
import com.example.atividadesfisicas.auth.AuthException;
import com.example.atividadesfisicas.auth.LoginResponseDTO;
import com.example.atividadesfisicas.auth.Sessao;
import com.example.atividadesfisicas.model.Utilizador;

/**
 * Ecrã inicial: UC0 Fazer Login.
 * A chamada à API faz-se numa worker thread e o resultado volta à UI thread com runOnUiThread (aula T4).
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;
    private TextView tvErro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvErro = findViewById(R.id.tvErro);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fazerLogin();
            }
        });
    }

    private void fazerLogin() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            tvErro.setText(R.string.login_campos_vazios);
            return;
        }

        tvErro.setText("");
        btnLogin.setEnabled(false);

        new Thread() {
            @Override
            public void run() {
                try {
                    final LoginResponseDTO resposta = AuthApi.login(email, password);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Sessao.getInstance().setUtilizador(
                                    new Utilizador(resposta.getEmail(), resposta.getUid()));
                            startActivity(new Intent(LoginActivity.this, MenuPrincipalActivity.class));
                            finish();
                        }
                    });
                } catch (final AuthException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvErro.setText(e.getMessage());
                            btnLogin.setEnabled(true);
                        }
                    });
                }
            }
        }.start();
    }
}
