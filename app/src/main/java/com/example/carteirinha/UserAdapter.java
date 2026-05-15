package com.example.carteirinha;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.VH> {

    private List<User> list;
    private OnUserActionListener listener;
    private boolean mostrarBotoes;

    public interface OnUserActionListener {
        void onAceitar(User user);
        void onNegar(User user);
    }

    public UserAdapter(List<User> list, boolean mostrarBotoes, OnUserActionListener listener) {
        this.list = list;
        this.mostrarBotoes = mostrarBotoes;
        this.listener = listener;
    }

    public class VH extends RecyclerView.ViewHolder {
        TextView nome, curso;
        View btnAceitar, btnNegar, layoutBotoes;

        public VH(View v) {
            super(v);
            nome = v.findViewById(R.id.tvNomeItem);
            curso = v.findViewById(R.id.tvCursoItem);
            btnAceitar = v.findViewById(R.id.btnAceitar);
            btnNegar = v.findViewById(R.id.btnNegar);
            layoutBotoes = v.findViewById(R.id.layoutBotoes);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        User u = list.get(position);
        holder.nome.setText(u.nome);
        holder.curso.setText(u.curso);

        if (mostrarBotoes) {
            holder.layoutBotoes.setVisibility(View.VISIBLE);
            holder.btnAceitar.setOnClickListener(v -> {
                if (listener != null) listener.onAceitar(u);
            });
            holder.btnNegar.setOnClickListener(v -> {
                if (listener != null) listener.onNegar(u);
            });
        } else {
            holder.layoutBotoes.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
