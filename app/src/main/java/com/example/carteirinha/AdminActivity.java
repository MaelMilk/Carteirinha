package com.example.carteirinha;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends AppCompatActivity {

    MaterialButton btnSairAdmin;
    TabLayout tabLayout;

    FirebaseFirestore db;
    RecyclerView rvAlunos;

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

                switch (tab.getPosition()) {

                    case 0:
                        carregarPendentes();
                        break;

                    case 1:
                        carregarAtivos();
                        break;

                    case 2:
                        carregarEmbarques();
                        break;
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // 🔥 PRIMEIRA CARGA AO ABRIR A TELA
        carregarPendentes();
    }

    // =========================
    // PENDENTES
    // =========================
    private void carregarPendentes() {

        db.collection("usuarios")
                .whereEqualTo("status", "pendente")
                .get()
                .addOnSuccessListener(query -> {

                    List<User> lista = new ArrayList<>();

                    for (DocumentSnapshot doc : query) {
                        User u = doc.toObject(User.class);
                        lista.add(u);
                    }

                    rvAlunos.setAdapter(new UserAdapter(lista, this::aprovarUsuario));
                });
    }

    // =========================
    // APROVAR USUÁRIO
    // =========================
    private void aprovarUsuario(User user) {

        db.collection("usuarios")
                .whereEqualTo("email", user.email)
                .get()
                .addOnSuccessListener(query -> {

                    for (DocumentSnapshot doc : query) {
                        doc.getReference().update("status", "ativo");
                    }

                    carregarPendentes(); // atualiza lista
                });
    }

    // =========================
    // ATIVOS
    // =========================
    private void carregarAtivos() {

        db.collection("usuarios")
                .whereEqualTo("status", "ativo")
                .get()
                .addOnSuccessListener(query -> {

                    List<User> lista = new ArrayList<>();

                    for (DocumentSnapshot doc : query) {
                        lista.add(doc.toObject(User.class));
                    }

                    rvAlunos.setAdapter(new UserAdapter(lista, null));
                });
    }

    // =========================
    // EMBARQUES
    // =========================
    private void carregarEmbarques() {

        db.collection("checkins")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(query -> {

                    List<Checkin> lista = new ArrayList<>();

                    for (DocumentSnapshot doc : query) {
                        lista.add(doc.toObject(Checkin.class));
                    }

                    rvAlunos.setAdapter(new CheckinAdapter(lista));
                });
    }
}