package com.example.carteirinha;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

public class AlunoActivity extends AppCompatActivity {

    MaterialButton btnSair, btnAtualizarQr;
    TextView tvNomeAluno, tvCursoAluno, tvMatricula, tvTempoRestante;
    ImageView ivQrCode, ivFotoPerfil;

    FirebaseAuth auth;
    FirebaseFirestore db;
    FirebaseStorage storage;

    Handler handler = new Handler();
    long tempoRestante = 120;

    // Seleção de Foto
    ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    ActivityResultLauncher<Uri> takePhoto;
    Uri tempImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aluno);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        try {
            storage = FirebaseStorage.getInstance("gs://carteirinhauni.firebasestorage.app");
        } catch (Exception e) {
            storage = FirebaseStorage.getInstance();
        }

        ivQrCode = findViewById(R.id.ivQrCode);
        ivFotoPerfil = findViewById(R.id.ivFotoPerfil);
        tvTempoRestante = findViewById(R.id.tvTempoRestante);
        btnSair = findViewById(R.id.btnSair);
        btnAtualizarQr = findViewById(R.id.btnAtualizarQr);
        tvNomeAluno = findViewById(R.id.tvNomeAluno);
        tvCursoAluno = findViewById(R.id.tvCursoAluno);
        tvMatricula = findViewById(R.id.tvMatricula);

        // Configurar launchers para trocar foto
        configurarSeletoresDeFoto();

        carregarDadosAluno();
        gerarNovoQR();
        iniciarContador();

        // Clique na foto para trocar
        ivFotoPerfil.setOnClickListener(v -> mostrarDialogoTrocarFoto());

        btnSair.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(AlunoActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        btnAtualizarQr.setOnClickListener(v -> {
            gerarNovoQR();
            tempoRestante = 120;
        });
    }

    private void configurarSeletoresDeFoto() {
        pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                uploadNovaFoto(uri);
            }
        });

        takePhoto = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && tempImageUri != null) {
                uploadNovaFoto(tempImageUri);
            }
        });
    }

    private void mostrarDialogoTrocarFoto() {
        String[] opcoes = {"Tirar Nova Foto", "Escolher da Galeria"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Trocar Foto de Perfil")
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
    }

    private void abrirCamera() {
        File imageFile = new File(getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "temp_perfil.jpg");
        try {
            if (imageFile.exists()) imageFile.delete();
            imageFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        tempImageUri = FileProvider.getUriForFile(this, "com.example.carteirinha.fileprovider", imageFile);
        takePhoto.launch(tempImageUri);
    }

    private void uploadNovaFoto(Uri uri) {
        String uid = auth.getUid();
        if (uid == null) return;

        Toast.makeText(this, "Atualizando foto...", Toast.LENGTH_SHORT).show();

        StorageReference ref = storage.getReference().child("fotos_perfil/" + uid + ".jpg");

        ref.putFile(uri).continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            return ref.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String novaUrl = task.getResult().toString();
                db.collection("usuarios").document(uid)
                        .update("fotoUrl", novaUrl)
                        .addOnSuccessListener(aVoid -> {
                            Glide.with(this).load(novaUrl).placeholder(R.drawable.ic_user_placeholder).into(ivFotoPerfil);
                            Toast.makeText(this, "Foto atualizada com sucesso!", Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(this, "Erro ao enviar foto", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void carregarDadosAluno() {
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(AlunoActivity.this, MainActivity.class));
            finish();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("usuarios").document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String nome = document.getString("nome");
                        String curso = document.getString("curso");
                        String matricula = document.getString("matricula");
                        String fotoUrl = document.getString("fotoUrl");

                        tvNomeAluno.setText(nome);
                        tvCursoAluno.setText(curso);
                        tvMatricula.setText("Matrícula: " + matricula);

                        if (fotoUrl != null && !fotoUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(fotoUrl)
                                    .placeholder(R.drawable.ic_user_placeholder)
                                    .into(ivFotoPerfil);
                        }
                    }
                });
    }

    private void gerarNovoQR() {
        try {
            if (auth.getCurrentUser() == null) return;

            String uid = auth.getCurrentUser().getUid();
            long timestamp = System.currentTimeMillis();

            JSONObject json = new JSONObject();
            json.put("uid", uid);
            json.put("timestamp", timestamp);
            String conteudo = json.toString();

            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bitmap = encoder.encodeBitmap(conteudo, BarcodeFormat.QR_CODE, 400, 400);
            ivQrCode.setImageBitmap(bitmap);

            tempoRestante = 120;

        } catch (JSONException | WriterException e) {
            e.printStackTrace();
        }
    }

    private void iniciarContador() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                tempoRestante--;

                int minutos = (int) (tempoRestante / 60);
                int segundos = (int) (tempoRestante % 60);
                tvTempoRestante.setText("Atualiza em " + String.format("%d:%02d", minutos, segundos));

                if (tempoRestante <= 0) {
                    gerarNovoQR();
                }

                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}