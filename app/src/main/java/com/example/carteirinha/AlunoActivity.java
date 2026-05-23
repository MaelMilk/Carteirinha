package com.example.carteirinha;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONException;
import org.json.JSONObject;

public class AlunoActivity extends AppCompatActivity {

    MaterialButton btnSair, btnAtualizarQr;
    TextView tvNomeAluno, tvCursoAluno, tvMatricula, tvTempoRestante;
    ImageView ivQrCode, ivFotoPerfil;

    FirebaseAuth auth;
    FirebaseFirestore db;

    Handler handler = new Handler();
    long tempoRestante = 120;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aluno);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ivQrCode = findViewById(R.id.ivQrCode);
        ivFotoPerfil = findViewById(R.id.ivFotoPerfil);
        tvTempoRestante = findViewById(R.id.tvTempoRestante);
        btnSair = findViewById(R.id.btnSair);
        btnAtualizarQr = findViewById(R.id.btnAtualizarQr);
        tvNomeAluno = findViewById(R.id.tvNomeAluno);
        tvCursoAluno = findViewById(R.id.tvCursoAluno);
        tvMatricula = findViewById(R.id.tvMatricula);

        carregarDadosAluno();
        gerarNovoQR();
        iniciarContador();

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

            // ✅ JSON seguro via JSONObject
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