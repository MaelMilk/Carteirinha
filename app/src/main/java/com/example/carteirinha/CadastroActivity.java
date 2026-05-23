package com.example.carteirinha;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.IOException;

import androidx.core.content.FileProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class CadastroActivity extends AppCompatActivity {

    MaterialButton btnCadastrar, btnSelecionarFoto;
    android.widget.TextView tvVoltarLogin;
    TextInputEditText etNome, etMatricula, etCurso, etEmail, etTelefone, etSenha, etConfirmarSenha;
    ImageView ivFotoAluno;

    FirebaseAuth auth;
    FirebaseFirestore db;
    FirebaseStorage storage;

    Uri imageUri;
    ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    ActivityResultLauncher<Uri> takePhoto;
    Uri tempImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        // Inicializa Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Configuração exata baseada no seu console
        try {
            // Usando o formato completo do link conforme sugerido
            storage = FirebaseStorage.getInstance("gs://carteirinhauni.firebasestorage.app");
        } catch (Exception e) {
            storage = FirebaseStorage.getInstance();
            Log.e("STORAGE_INIT", "Erro ao iniciar: " + e.getMessage());
        }

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
        ivFotoAluno = findViewById(R.id.ivFotoAluno);
        tvVoltarLogin = findViewById(R.id.tvVoltarLogin);

        // Configura o seletor de mídia (Galeria)
        pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                imageUri = uri;
                ivFotoAluno.setImageURI(uri);
            }
        });

        // Configura a captura de foto (Câmera)
        takePhoto = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && tempImageUri != null) {
                imageUri = tempImageUri;
                ivFotoAluno.setImageURI(imageUri);
            }
        });

        // Botão Criar Conta
        btnCadastrar.setOnClickListener(v -> cadastrarAluno());

        // Voltar para login
        tvVoltarLogin.setOnClickListener(v -> finish());

        // Selecionar foto (Menu Câmera ou Galeria)
        btnSelecionarFoto.setOnClickListener(v -> {
            String[] opcoes = {"Tirar Foto", "Escolher da Galeria"};
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Foto do Aluno")
                    .setItems(opcoes, (dialog, which) -> {
                        if (which == 0) {
                            abrirCamera();
                        } else {
                            pickMedia.launch(new PickVisualMediaRequest.Builder()
                                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                    .build());
                        }
                    })
                    .show();
        });
    }

    private void abrirCamera() {
        File imageFile = new File(getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "temp_image.jpg");
        try {
            if (imageFile.exists()) imageFile.delete();
            imageFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        tempImageUri = FileProvider.getUriForFile(this, "com.example.carteirinha.fileprovider", imageFile);
        takePhoto.launch(tempImageUri);
    }

    private void cadastrarAluno() {
        String nome = etNome.getText().toString().trim();
        String matricula = etMatricula.getText().toString().trim();
        String curso = etCurso.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String telefone = etTelefone.getText().toString().trim();
        String senha = etSenha.getText().toString().trim();
        String confirmarSenha = etConfirmarSenha.getText().toString().trim();

        if (nome.isEmpty() || matricula.isEmpty() || curso.isEmpty() ||
                email.isEmpty() || telefone.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri == null) {
            Toast.makeText(this, "Por favor, selecione uma foto!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!senha.equals(confirmarSenha)) {
            Toast.makeText(this, "As senhas não coincidem!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCadastrar.setEnabled(false);
        btnCadastrar.setText("Aguarde...");

        // 1. Criar usuário no Auth
        auth.createUserWithEmailAndPassword(email, senha)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    uploadFotoESalvarDados(uid, nome, matricula, curso, email, telefone);
                })
                .addOnFailureListener(e -> {
                    btnCadastrar.setEnabled(true);
                    btnCadastrar.setText("Criar conta");
                    Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void uploadFotoESalvarDados(String uid, String nome, String matricula, String curso, String email, String telefone) {
        StorageReference ref = storage.getReference().child("fotos_perfil/" + uid + ".jpg");

        // Novo fluxo de upload com continueWithTask para evitar erro 404 (Objeto não existe)
        ref.putFile(imageUri).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                if (task.getException() != null) throw task.getException();
            }
            // Só pede a URL se o upload foi concluído
            return ref.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String fotoUrl = task.getResult().toString();
                salvarNoFirestore(uid, nome, matricula, curso, email, telefone, fotoUrl);
            } else {
                btnCadastrar.setEnabled(true);
                btnCadastrar.setText("Criar conta");
                
                // Pega a mensagem real do erro para diagnóstico
                String erro = task.getException() != null ? task.getException().getMessage() : "Erro desconhecido";
                
                Log.e("UPLOAD_ERROR", "Falha: " + erro);
                if (task.getException() != null) {
                    task.getException().printStackTrace();
                }
                
                // Agora mostra o erro REAL no Toast
                Toast.makeText(this, "Erro no Firebase: " + erro, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void salvarNoFirestore(String uid, String nome, String matricula, String curso, String email, String telefone, String fotoUrl) {
        Map<String, Object> usuario = new HashMap<>();
        usuario.put("nome", nome);
        usuario.put("matricula", matricula);
        usuario.put("curso", curso);
        usuario.put("email", email);
        usuario.put("telefone", telefone);
        usuario.put("tipo", "aluno");
        usuario.put("status", "pendente");
        usuario.put("fotoUrl", fotoUrl);

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
                    btnCadastrar.setEnabled(true);
                    btnCadastrar.setText("Criar conta");
                    // Mostra o erro real do Firestore (ex: Permission Denied)
                    Log.e("FIRESTORE_ERROR", "Erro detalhado: " + e.getMessage());
                    Toast.makeText(this, "Erro no Banco: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
