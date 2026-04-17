package com.example.carteirinha;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    MaterialButton btnEntrar, btnCriarConta;
    TextInputEditText etEmail, etSenha;
    FirebaseAuth auth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etSenha = findViewById(R.id.etSenha);
        btnEntrar = findViewById(R.id.btnEntrar);
        btnCriarConta = findViewById(R.id.btnCriarConta);

        // Se já está logado, redireciona direto
        if (auth.getCurrentUser() != null) {
            verificarUsuario(auth.getCurrentUser().getUid());
        }

        btnEntrar.setOnClickListener(v -> fazerLogin());

        btnCriarConta.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CadastroActivity.class));
        });
    }

    private void fazerLogin() {
        String email = etEmail.getText().toString().trim();
        String senha = etSenha.getText().toString().trim();

        if (email.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Preencha e-mail e senha!", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, senha)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    verificarUsuario(uid);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "E-mail ou senha incorretos!", Toast.LENGTH_SHORT).show();
                });
    }

    private void verificarUsuario(String uid) {
        db.collection("usuarios").document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String tipo = document.getString("tipo");
                        String status = document.getString("status");

                        if (tipo.equals("aluno")) {
                            if (status.equals("ativo")) {
                                startActivity(new Intent(MainActivity.this, AlunoActivity.class));
                            } else {
                                startActivity(new Intent(MainActivity.this, AguardandoActivity.class));
                            }
                        } else if (tipo.equals("motorista")) {
                            startActivity(new Intent(MainActivity.this, MotoristaActivity.class));
                        } else if (tipo.equals("admin")) {
                            startActivity(new Intent(MainActivity.this, AdminActivity.class));
                        }
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao buscar dados!", Toast.LENGTH_SHORT).show();
                });
    }
}