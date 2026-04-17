package com.example.carteirinha;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class AguardandoActivity extends AppCompatActivity {

    MaterialButton btnSair;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aguardando);

        btnSair = findViewById(R.id.btnSair);

        // Botão Sair → volta para o Login e limpa a pilha de telas
        btnSair.setOnClickListener(v -> {
            Intent intent = new Intent(AguardandoActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}