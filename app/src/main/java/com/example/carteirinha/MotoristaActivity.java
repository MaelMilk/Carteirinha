package com.example.carteirinha;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.bumptech.glide.Glide;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class MotoristaActivity extends AppCompatActivity {

    MaterialButton btnSairMotorista, btnEscanear;
    RecyclerView rvEmbarquesMotorista;
    TextView tvNomeMotorista, tvContadorAFecho, tvContadorHoje;
    TextView tvNomeAlunoScan, tvMatriculaScan, tvCursoScan, tvStatusScan;
    com.google.android.material.card.MaterialCardView cardResultado;
    com.google.android.material.imageview.ShapeableImageView ivFotoAlunoScan;
    MaterialButton btnFecharResultado;
    com.google.android.material.switchmaterial.SwitchMaterial switchModo;
    FirebaseFirestore db;

    // Listeners
    ListenerRegistration listenerContadores, listenerUltimosEmbarques;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_motorista);

        db = FirebaseFirestore.getInstance();

        btnSairMotorista = findViewById(R.id.btnSairMotorista);
        btnEscanear = findViewById(R.id.btnEscanear);
        rvEmbarquesMotorista = findViewById(R.id.rvEmbarquesMotorista);
        switchModo = findViewById(R.id.switchModo);
        tvNomeMotorista = findViewById(R.id.tvNomeMotorista);
        tvContadorAFecho = findViewById(R.id.tvContadorAFecho);
        tvContadorHoje = findViewById(R.id.tvContadorHoje);

        // Views do Resultado do Scan
        cardResultado = findViewById(R.id.cardResultado);
        tvNomeAlunoScan = findViewById(R.id.tvNomeAlunoScan);
        tvMatriculaScan = findViewById(R.id.tvMatriculaScan);
        tvCursoScan = findViewById(R.id.tvCursoScan);
        tvStatusScan = findViewById(R.id.tvStatusScan);
        ivFotoAlunoScan = findViewById(R.id.ivFotoAlunoScan);
        btnFecharResultado = findViewById(R.id.btnFecharResultado);

        rvEmbarquesMotorista.setLayoutManager(new LinearLayoutManager(this));

        carregarDadosMotorista();
        atualizarContadores();

        // Mudar texto do switch ao clicar
        switchModo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                switchModo.setText("Modo: Saída (Desembarque)");
            } else {
                switchModo.setText("Modo: Entrada (Embarque)");
            }
        });

        carregarUltimosEmbarques();

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

        // Fechar card de resultado
        btnFecharResultado.setOnClickListener(v -> {
            cardResultado.setVisibility(android.view.View.GONE);
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
            // ✅ Parse seguro com org.json
            org.json.JSONObject json = new org.json.JSONObject(conteudo);
            String uid = json.getString("uid");
            long timestampCode = json.getLong("timestamp");

            if (uid.isEmpty()) {
                Toast.makeText(this, "QR Code inválido", Toast.LENGTH_SHORT).show();
                return;
            }

            // --- VALIDAÇÃO DE TEMPO (3 MINUTOS) ---
            long agora = System.currentTimeMillis();
            long diferenca = agora - timestampCode;
            long tresMinutos = 3 * 60 * 1000L; 

            if (diferenca > tresMinutos) {
                Toast.makeText(this, "ERRO: QR Code expirado!", Toast.LENGTH_LONG).show();
                Log.w("QR_VALIDACAO", "Código antigo detectado. Diferença: " + (diferenca / 1000) + " segundos");
                return;
            }
            // --------------------------------------

            final String finalUid = uid;
            final long finalTimestampCode = timestampCode;

            // 1. Verificar se o QR já foi lido (Evita duplicidade por erro ou foto do QR)
            db.collection("checkins")
                    .whereEqualTo("qrTimestamp", finalTimestampCode)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            Toast.makeText(this, "ERRO: Este QR Code já foi utilizado!", Toast.LENGTH_LONG).show();
                        } else {
                            // 2. BUSCA SIMPLIFICADA: Busca os últimos registros do aluno e filtra no código Java
                            // Isso elimina a necessidade de índices compostos manuais que demoram a ativar
                            db.collection("checkins")
                                    .whereEqualTo("userId", finalUid)
                                    .get()
                                    .addOnSuccessListener(ultimosCheckins -> {
                                        String ultimoTipo = "";
                                        long ultimoTs = 0;
                                        long inicioTurno = getInicioDoTurno();

                                        // Filtra manualmente o registro mais recente DESTE turno (pós 17h ou hoje cedo)
                                        for (com.google.firebase.firestore.DocumentSnapshot doc : ultimosCheckins) {
                                            Long ts = doc.getLong("timestamp");
                                            String tipo = doc.getString("tipo");
                                            if (ts != null && ts >= inicioTurno) {
                                                if (ts > ultimoTs) {
                                                    ultimoTs = ts;
                                                    ultimoTipo = tipo;
                                                }
                                            }
                                        }

                                        final String statusUltimoAcesso = ultimoTipo;
                                        final long finalUltimoTs = ultimoTs;

                                        // 3. Buscar dados do aluno e validar status
                                        db.collection("usuarios").document(finalUid).get()
                                                .addOnSuccessListener(documentSnapshot -> {
                                                    if (documentSnapshot.exists()) {
                                                        String status = documentSnapshot.getString("status");
                                                        String nomeAluno = documentSnapshot.getString("nome");

                                                        if (!"ativo".equals(status)) {
                                                            exibirErroScan("ACESSO NEGADO", "Aluno inativo ou pendente");
                                                            return;
                                                        }

                                                        // --- LÓGICA AUTOMÁTICA DE CHECK-IN/OUT ---
                                                        String tipoAcao;
                                                        long janelaTurno = 6 * 60 * 60 * 1000; // 6 horas
                                                        long agoraLocal = System.currentTimeMillis();

                                                        // Se ele já fez Check-in recentemente (mesmo turno), agora é Check-out
                                                        if ("Check-in".equals(statusUltimoAcesso) && (agoraLocal - finalUltimoTs) < janelaTurno) {
                                                            tipoAcao = "Check-out";
                                                        } else {
                                                            tipoAcao = "Check-in";
                                                        }
                                                        
                                                        // Atualiza o switch visual
                                                        switchModo.setChecked("Check-out".equals(tipoAcao));
                                                        
                                                        String matricula = documentSnapshot.contains("matricula") ? documentSnapshot.getString("matricula") : "---";
                                                        String fotoUrl = documentSnapshot.contains("fotoUrl") ? documentSnapshot.getString("fotoUrl") : "";

                                                        // Preencher o card com dados do Firestore
                                                        tvNomeAlunoScan.setText(nomeAluno);
                                                        tvMatriculaScan.setText("Matrícula: " + matricula);
                                                        tvCursoScan.setText(documentSnapshot.getString("curso"));
                                                        tvStatusScan.setText(tipoAcao + " REGISTRADO");
                                                        tvStatusScan.setTextColor(android.graphics.Color.parseColor("#2E7D32"));
                                                        
                                                        if (!fotoUrl.isEmpty()) {
                                                            Glide.with(MotoristaActivity.this).load(fotoUrl).placeholder(R.drawable.ic_user_placeholder).into(ivFotoAlunoScan);
                                                        } else {
                                                            ivFotoAlunoScan.setImageResource(R.drawable.ic_user_placeholder);
                                                        }

                                                        cardResultado.setVisibility(android.view.View.VISIBLE);
                                                        registrarAcao(finalUid, nomeAluno, matricula, fotoUrl, finalTimestampCode, tipoAcao);
                                                    } else {
                                                        exibirErroScan("ERRO", "Aluno não cadastrado!");
                                                    }
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e("SCAN_ERROR", "Erro ao buscar aluno", e);
                                                    exibirErroScan("ERRO", "Falha ao buscar dados");
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("SCAN_ERROR", "Erro na consulta de histórico", e);
                                        Toast.makeText(this, "Erro de Banco: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                        }
                    });

        } catch (org.json.JSONException e) {
            Log.e("QR_ERROR", "JSON inválido: " + conteudo, e);
            exibirErroScan("ERRO", "QR Code inválido");
        } catch (Exception e) {
            Log.e("QR_ERROR", "Erro inesperado", e);
            exibirErroScan("ERRO", "Falha ao ler QR Code");
        }
    }

    private void exibirErroScan(String titulo, String mensagem) {
        tvNomeAlunoScan.setText(titulo);
        tvMatriculaScan.setText(mensagem);
        tvCursoScan.setText("");
        tvStatusScan.setText("BLOQUEADO");
        tvStatusScan.setTextColor(android.graphics.Color.RED);
        cardResultado.setVisibility(android.view.View.VISIBLE);
    }

    private long getInicioDoTurno() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int horaAtual = cal.get(java.util.Calendar.HOUR_OF_DAY);

        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);

        if (horaAtual >= 17) {
            cal.set(java.util.Calendar.HOUR_OF_DAY, 17);
        } else {
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        }
        return cal.getTimeInMillis();
    }

    private void registrarAcao(String uid, String nome, String matricula, String fotoUrl, long qrTimestamp, String tipo) {
        long agora = System.currentTimeMillis();
        Checkin novoCheckin = new Checkin(uid, nome, matricula, fotoUrl, tipo, agora, qrTimestamp);

        db.collection("checkins")
                .add(novoCheckin)
                .addOnSuccessListener(documentReference -> {
                    String msg = tipo.equals("Check-in") ? "BEM-VINDO: " : "TCHAU: ";
                    Toast.makeText(this, msg + nome, Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao registrar " + tipo.toLowerCase(), Toast.LENGTH_SHORT).show();
                });
    }

    private void carregarDadosMotorista() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            db.collection("usuarios").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String nome = documentSnapshot.getString("nome");
                            if (nome != null) {
                                int hora = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
                                String saudacao = (hora >= 5 && hora < 12) ? "Bom dia" : (hora >= 12 && hora < 18) ? "Boa tarde" : "Boa noite";
                                tvNomeMotorista.setText(saudacao + ", " + nome);
                            }
                        }
                    });
        }
    }

    private void atualizarContadores() {
        long inicioTurno = getInicioDoTurno();
        long agora = System.currentTimeMillis();
        long seisHorasEmMillis = 6 * 60 * 60 * 1000;

        listenerContadores = db.collection("checkins")
                .whereGreaterThanOrEqualTo("timestamp", inicioTurno)
                .addSnapshotListener((query, error) -> {
                    if (error != null) return;
                    if (query != null) {
                        int entradasAtivas = 0;
                        int totalTurno = 0;
                        int saidasTurno = 0;

                        for (com.google.firebase.firestore.DocumentSnapshot doc : query) {
                            String tipo = doc.getString("tipo");
                            Long ts = doc.getLong("timestamp");
                            if (ts == null) continue;

                            if ("Check-in".equals(tipo)) {
                                totalTurno++;
                                if ((agora - ts) < seisHorasEmMillis) entradasAtivas++;
                            } else if ("Check-out".equals(tipo)) {
                                saidasTurno++;
                            }
                        }
                        int lotacaoAtual = Math.max(0, entradasAtivas - saidasTurno);
                        tvContadorAFecho.setText(String.valueOf(lotacaoAtual));
                        tvContadorHoje.setText(String.valueOf(totalTurno));
                    }
                });
    }

    private void carregarUltimosEmbarques() {
        // Esta busca ordena por timestamp DESC, o que exige um índice simples automático.
        listenerUltimosEmbarques = db.collection("checkins")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener((query, error) -> {
                    if (error != null) {
                        Log.e("MotoristaActivity", "Erro ao carregar lista", error);
                        return;
                    }
                    if (query != null) {
                        java.util.List<Checkin> lista = new java.util.ArrayList<>();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : query) {
                            lista.add(doc.toObject(Checkin.class));
                        }
                        rvEmbarquesMotorista.setAdapter(new CheckinAdapter(lista));
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerContadores != null) listenerContadores.remove();
        if (listenerUltimosEmbarques != null) listenerUltimosEmbarques.remove();
    }
}