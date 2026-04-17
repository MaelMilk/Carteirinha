package com.example.carteirinha;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.VH> {

    List<User> list;
    OnClickListener listener;

    public interface OnClickListener {
        void onClick(User user);
    }

    public UserAdapter(List<User> list, OnClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    public class VH extends RecyclerView.ViewHolder {
        TextView nome;

        public VH(View v) {
            super(v);
            nome = v.findViewById(android.R.id.text1);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {

        User u = list.get(position);
        holder.nome.setText(u.nome);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(u);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}