package com.example.carteirinha;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends AppCompatActivity implements UserAdapter.OnUserActionListener {

    MaterialButton btnSairAdmin;
    TabLayout tabLayout;

    FirebaseFirestore db;
    RecyclerView rvAlunos;

    // Contadores do topo
    TextView tvTotalAtivos, tvTotalPendentes, tvTotalEmbarques;

    // Listener para limpar ao sair
    ListenerRegistration listenerAtual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Firebase
        db = FirebaseFirestore.getInstance();

        // Views
        btnSairAdmin = findViewById(R.id.btnSairAdmin);
        tabLayout = findViewById(R.id.tabLayout);
        rvAlunos = findViewById(R.id.rvAlunos);
        rvAlunos.setLayoutManager(new LinearLayoutManager(this));

        // Contadores
        tvTotalAtivos = findViewById(R.id.tvTotalAtivos);
        tvTotalPendentes = findViewById(R.id.tvTotalPendentes);
        tvTotalEmbarques = findViewById(R.id.tvTotalEmbarques);

        // Botão sair
        btnSairAdmin.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(AdminActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Abas
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (listenerAtual != null) listenerAtual.remove();

                switch (tab.getPosition()) {
                    case 0: carregarPendentes(); break;
                    case 1: carregarAtivos(); break;
                    case 2: carregarEmbarques(); break;
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Monitorar contadores em tempo real (sempre ativo)
        iniciarMonitoramentoContadores();

        // 🔥 PRIMEIRA CARGA
        carregarPendentes();
    }

    private void iniciarMonitoramentoContadores() {
        // Ativos
        db.collection("usuarios").whereEqualTo("status", "ativo")
                .addSnapshotListener((value, error) -> {
                    if (value != null) tvTotalAtivos.setText(String.valueOf(value.size()));
                });

        // Pendentes
        db.collection("usuarios").whereEqualTo("status", "pendente")
                .addSnapshotListener((value, error) -> {
                    if (value != null) tvTotalPendentes.setText(String.valueOf(value.size()));
                });

        // Embarques (Geral)
        db.collection("checkins")
                .addSnapshotListener((value, error) -> {
                    if (value != null) tvTotalEmbarques.setText(String.valueOf(value.size()));
                });
    }

    private void carregarPendentes() {
        if (listenerAtual != null) listenerAtual.remove();

        listenerAtual = db.collection("usuarios")
                .whereEqualTo("status", "pendente")
                .addSnapshotListener((value, error) -> {
                    if (value == null) return;

                    List<User> lista = new ArrayList<>();
                    for (DocumentSnapshot doc : value) {
                        User u = doc.toObject(User.class);
                        if (u != null) {
                            u.uid = doc.getId(); // Vincula o ID do documento
                            lista.add(u);
                        }
                    }
                    rvAlunos.setAdapter(new UserAdapter(lista, true, this));
                });
    }

    @Override
    public void onAceitar(User user) {
        if (user.uid == null) return;

        db.collection("usuarios").document(user.uid)
                .update("status", "ativo")
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Usuário " + user.nome + " aprovado!", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erro ao aprovar: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    public void onNegar(User user) {
        if (user.uid == null) return;

        // Opção: Deletar o usuário ou mudar status para "negado"
        // Vamos deletar para limpar o banco, ou você pode mudar para "negado" se preferir manter histórico
        db.collection("usuarios").document(user.uid)
                .delete()
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Cadastro de " + user.nome + " recusado.", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erro ao recusar: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void carregarAtivos() {
        if (listenerAtual != null) listenerAtual.remove();

        listenerAtual = db.collection("usuarios")
                .whereEqualTo("status", "ativo")
                .addSnapshotListener((value, error) -> {
                    if (value == null) return;

                    List<User> lista = new ArrayList<>();
                    for (DocumentSnapshot doc : value) {
                        User u = doc.toObject(User.class);
                        if (u != null) {
                            u.uid = doc.getId();
                            lista.add(u);
                        }
                    }
                    rvAlunos.setAdapter(new UserAdapter(lista, false, null));
                });
    }

    private void carregarEmbarques() {
        if (listenerAtual != null) listenerAtual.remove();

        listenerAtual = db.collection("checkins")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value == null) return;

                    List<Checkin> lista = new ArrayList<>();
                    for (DocumentSnapshot doc : value) {
                        lista.add(doc.toObject(Checkin.class));
                    }
                    rvAlunos.setAdapter(new CheckinAdapter(lista));
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerAtual != null) listenerAtual.remove();
    }
}
