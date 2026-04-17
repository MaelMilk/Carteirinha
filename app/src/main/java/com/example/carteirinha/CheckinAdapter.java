package com.example.carteirinha;

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
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    public CheckinAdapter(List<Checkin> list) {
        this.list = list;
    }

    public class VH extends RecyclerView.ViewHolder {
        TextView text;

        public VH(View v) {
            super(v);
            text = v.findViewById(android.R.id.text1);
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
        Checkin c = list.get(position);

        String horaFormatada = sdf.format(new Date(c.getTimestamp()));

        holder.text.setText(
                "Aluno: " + c.getNomeAluno() +
                "\nAção: " + c.getTipo() +
                " às " + horaFormatada
        );
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}