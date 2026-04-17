package com.example.carteirinha;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class CadastroActivity extends AppCompatActivity {

    MaterialButton btnCadastrar, btnSelecionarFoto;
    android.widget.TextView tvVoltarLogin;
    TextInputEditText etNome, etMatricula, etCurso, etEmail, etTelefone, etSenha, etConfirmarSenha;

    FirebaseAuth auth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        // Inicializa Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Conecta os campos do XML
        etNome = findViewById(R.id.etNome);
        etMatricula = findViewById(R.id.etMatricula);
        etCurso = findViewById(R.id.etCurso);
        etEmail = findViewById(R.id.etEmail);
        etTelefone = findViewById(R.id.etTelefone);
        etSenha = findViewById(R.id.etSenha);
        etConfirmarSenha = findViewById(R.id.etConfirmarSenha);
        btnCadastrar = findViewById(R.id.btnCadastrar);
        btnSelecionarFoto = findViewById(R.id.btnSelecionarFoto);
        tvVoltarLogin = findViewById(R.id.tvVoltarLogin);

        // Botão Criar Conta
        btnCadastrar.setOnClickListener(v -> cadastrarAluno());

        // Voltar para login
        tvVoltarLogin.setOnClickListener(v -> finish());

        // Foto - por enquanto placeholder
        btnSelecionarFoto.setOnClickListener(v -> {
            // TODO: abrir galeria
        });
    }

    private void cadastrarAluno() {
        // Pega os valores dos campos
        String nome = etNome.getText().toString().trim();
        String matricula = etMatricula.getText().toString().trim();
        String curso = etCurso.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String telefone = etTelefone.getText().toString().trim();
        String senha = etSenha.getText().toString().trim();
        String confirmarSenha = etConfirmarSenha.getText().toString().trim();

        // Validações básicas
        if (nome.isEmpty() || matricula.isEmpty() || curso.isEmpty() ||
                email.isEmpty() || telefone.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!senha.equals(confirmarSenha)) {
            Toast.makeText(this, "As senhas não coincidem!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (senha.length() < 6) {
            Toast.makeText(this, "A senha deve ter pelo menos 6 caracteres!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Cria o usuário no Firebase Auth
        auth.createUserWithEmailAndPassword(email, senha)
                .addOnSuccessListener(authResult -> {
                    // Usuário criado! Agora salva os dados no Firestore
                    String uid = authResult.getUser().getUid();

                    Map<String, Object> usuario = new HashMap<>();
                    usuario.put("nome", nome);
                    usuario.put("matricula", matricula);
                    usuario.put("curso", curso);
                    usuario.put("email", email);
                    usuario.put("telefone", telefone);
                    usuario.put("tipo", "aluno");
                    usuario.put("status", "pendente");
                    usuario.put("fotoBase64", "");

                    db.collection("usuarios").document(uid)
                            .set(usuario)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Cadastro realizado!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(CadastroActivity.this, AguardandoActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Erro ao salvar dados: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao cadastrar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}