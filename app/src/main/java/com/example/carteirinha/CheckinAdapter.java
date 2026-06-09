package com.example.carteirinha;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CheckinAdapter extends RecyclerView.Adapter<CheckinAdapter.VH> {

    private List<Checkin> list;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());

    public CheckinAdapter(List<Checkin> list) {
        this.list = list;
    }

    public class VH extends RecyclerView.ViewHolder {
        TextView tvNome, tvMatricula, tvTipo, tvHora;
        com.google.android.material.imageview.ShapeableImageView ivFoto;
        View indicator;

        public VH(View v) {
            super(v);
            tvNome = v.findViewById(R.id.tvItemNome);
            tvMatricula = v.findViewById(R.id.tvItemMatricula);
            tvTipo = v.findViewById(R.id.tvItemTipo);
            tvHora = v.findViewById(R.id.tvItemHora);
            ivFoto = v.findViewById(R.id.ivItemFoto);
            indicator = v.findViewById(R.id.viewStatusIndicator);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_checkin, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Checkin c = list.get(position);

        holder.tvNome.setText(c.getNomeAluno());
        holder.tvMatricula.setText("Matrícula: " + (c.getMatricula() != null ? c.getMatricula() : "---"));
        holder.tvTipo.setText(c.getTipo());
        holder.tvHora.setText(sdf.format(new Date(c.getTimestamp())));

        // Carregar foto na lista
        if (c.getFotoUrl() != null && !c.getFotoUrl().isEmpty()) {
            com.bumptech.glide.Glide.with(holder.itemView.getContext())
                    .load(c.getFotoUrl())
                    .placeholder(R.drawable.ic_user_placeholder)
                    .circleCrop()
                    .into(holder.ivFoto);
        } else {
            holder.ivFoto.setImageResource(R.drawable.ic_user_placeholder);
        }

        if ("Check-in".equals(c.getTipo())) {
            holder.indicator.getBackground().setTint(Color.parseColor("#4CAF50")); // Verde
            holder.tvTipo.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            holder.indicator.getBackground().setTint(Color.parseColor("#F44336")); // Vermelho
            holder.tvTipo.setTextColor(Color.parseColor("#F44336"));
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}