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
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public CheckinAdapter(List<Checkin> list) {
        this.list = list;
    }

    public class VH extends RecyclerView.ViewHolder {
        TextView tvNome, tvTipo, tvHora;
        View indicator;

        public VH(View v) {
            super(v);
            tvNome = v.findViewById(R.id.tvItemNome);
            tvTipo = v.findViewById(R.id.tvItemTipo);
            tvHora = v.findViewById(R.id.tvItemHora);
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
        holder.tvTipo.setText(c.getTipo());
        holder.tvHora.setText(sdf.format(new Date(c.getTimestamp())));

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