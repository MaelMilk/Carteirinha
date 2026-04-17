package com.example.carteirinha;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class MotoristaActivity extends AppCompatActivity {

    MaterialButton btnSairMotorista, btnEscanear;
    MaterialButton btnConfirmarEmbarque, btnNegarEmbarque;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_motorista);

        db = FirebaseFirestore.getInstance();

        btnSairMotorista = findViewById(R.id.btnSairMotorista);
        btnEscanear = findViewById(R.id.btnEscanear);
        btnConfirmarEmbarque = findViewById(R.id.btnConfirmarEmbarque);
        btnNegarEmbarque = findViewById(R.id.btnNegarEmbarque);

        // Botão sair
        btnSairMotorista.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(MotoristaActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Botão escanear QR
        btnEscanear.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(MotoristaActivity.this);
            integrator.setPrompt("Aponte para o QR Code");
            integrator.setBeepEnabled(true);
            integrator.setOrientationLocked(false);
            integrator.initiateScan();
        });

        // Confirmar embarque (placeholder)
        btnConfirmarEmbarque.setOnClickListener(v -> {
            Toast.makeText(this, "Embarque confirmado", Toast.LENGTH_SHORT).show();
        });

        // Negar embarque (placeholder)
        btnNegarEmbarque.setOnClickListener(v -> {
            Toast.makeText(this, "Embarque negado", Toast.LENGTH_SHORT).show();
        });
    }

    // Resultado do QR
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null) {
            if (result.getContents() != null) {
                processarLeituraQR(result.getContents());
            } else {
                Toast.makeText(this, "Leitura cancelada", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void processarLeituraQR(String conteudo) {
        try {
            // O conteúdo é um JSON vindo da AlunoActivity
            // Ex: { "uid": "abc...", "timestamp": 123... }

            String uid = "";
            long timestampCode = 0;

            // Extrair UID
            if (conteudo.contains("\"uid\": \"")) {
                int start = conteudo.indexOf("\"uid\": \"") + 8;
                int end = conteudo.indexOf("\"", start);
                uid = conteudo.substring(start, end);
            }

            // Extrair Timestamp
            if (conteudo.contains("\"timestamp\": ")) {
                int start = conteudo.indexOf("\"timestamp\": ") + 13;
                int end = conteudo.indexOf(" ", start);
                if (end == -1) end = conteudo.indexOf("}", start);
                String tsStr = conteudo.substring(start, end).trim();
                timestampCode = Long.parseLong(tsStr);
            }

            if (uid.isEmpty()) {
                Toast.makeText(this, "QR Code inválido", Toast.LENGTH_SHORT).show();
                return;
            }

            // --- VALIDAÇÃO DE TEMPO (5 MINUTOS) ---
            long agora = System.currentTimeMillis();
            long diferenca = agora - timestampCode;
            long cincoMinutos = 5 * 60 * 1000;

            if (diferenca > cincoMinutos) {
                Toast.makeText(this, "ERRO: QR Code expirado!", Toast.LENGTH_LONG).show();
                Log.w("QR_VALIDACAO", "Código antigo detectado. Diferença: " + (diferenca / 1000) + " segundos");
                return; // Para o processo aqui
            }
            // --------------------------------------

            String finalUid = uid;
            // 1. Verificar se o QR já foi lido
            long finalTimestampCode = timestampCode;
            db.collection("checkins")
                    .whereEqualTo("qrTimestamp", timestampCode)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            Toast.makeText(this, "ERRO: Este QR Code já foi utilizado!", Toast.LENGTH_LONG).show();
                        } else {
                            // 2. Buscar aluno e verificar status
                            db.collection("usuarios").document(finalUid).get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            String status = documentSnapshot.getString("status");
                                            String nomeAluno = documentSnapshot.getString("nome");

                                            if ("ativo".equals(status)) {
                                                registrarCheckin(finalUid, nomeAluno, finalTimestampCode);
                                            } else {
                                                Toast.makeText(this, "ACESSO NEGADO: Aluno inativo ou pendente", Toast.LENGTH_LONG).show();
                                            }
                                        } else {
                                            Toast.makeText(this, "ERRO: Aluno não cadastrado!", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    });

        } catch (Exception e) {
            Log.e("QR_ERROR", "Erro ao processar QR", e);
            Toast.makeText(this, "Erro ao ler QR Code", Toast.LENGTH_SHORT).show();
        }
    }

    private void registrarCheckin(String uid, String nome, long qrTimestamp) {
        long agora = System.currentTimeMillis();
        Checkin novoCheckin = new Checkin(uid, nome, "Check-in", agora, qrTimestamp);

        db.collection("checkins")
                .add(novoCheckin)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "BEM-VINDO: " + nome, Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao registrar check-in", Toast.LENGTH_SHORT).show();
                });
    }
}