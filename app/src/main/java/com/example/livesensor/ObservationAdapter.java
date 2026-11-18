package com.example.livesensor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ObservationAdapter extends RecyclerView.Adapter<ObservationAdapter.ViewHolder> {

    private final List<Observation> observations = new ArrayList<>();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_observation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Observation obs = observations.get(position);
        holder.bind(obs);
    }

    @Override
    public int getItemCount() {
        return observations.size();
    }

    public void addObservation(Observation obs) {
        observations.add(0, obs); // Add to top (most recent first)
        notifyItemInserted(0);
    }

    public void setObservations(List<Observation> list) {
        observations.clear();
        observations.addAll(list);
        notifyDataSetChanged();
    }

    public List<Observation> getObservations() {
        return new ArrayList<>(observations);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTimestamp;
        private final TextView tvCondition;
        private final TextView tvRawValue;
        private final TextView tvSmoothedValue;
        private final TextView tvAccuracy;
        private final TextView tvSource;

        ViewHolder(View itemView) {
            super(itemView);
            tvTimestamp = itemView.findViewById(R.id.tvItemTimestamp);
            tvCondition = itemView.findViewById(R.id.tvItemCondition);
            tvRawValue = itemView.findViewById(R.id.tvItemRawValue);
            tvSmoothedValue = itemView.findViewById(R.id.tvItemSmoothedValue);
            tvAccuracy = itemView.findViewById(R.id.tvItemAccuracy);
            tvSource = itemView.findViewById(R.id.tvItemSource);
        }

        void bind(Observation obs) {
            tvTimestamp.setText(obs.timestamp);
            tvCondition.setText(obs.condition);
            tvRawValue.setText(String.format("%.2f lux", obs.rawValue));
            tvSmoothedValue.setText(String.format("%.2f lux", obs.smoothedValue));
            tvAccuracy.setText(obs.getAccuracyLabel());
            tvSource.setText(obs.source);

            // Highlight unreliable rows
            if (obs.unreliable) {
                itemView.setBackgroundColor(0xFFFFEBEE); // Light red background
                tvAccuracy.setTextColor(0xFFC62828); // Dark red text
            } else {
                itemView.setBackgroundColor(0x00000000); // Transparent
                tvAccuracy.setTextColor(0xFF000000); // Black text
            }

            // Highlight simulated rows
            if ("Simulated".equals(obs.source)) {
                tvSource.setTextColor(0xFFFF9800); // Orange
                tvSource.setText("SIMULATED");
            } else {
                tvSource.setTextColor(0xFF2E7D32); // Green
                tvSource.setText("MEASURED");
            }
        }
    }
}