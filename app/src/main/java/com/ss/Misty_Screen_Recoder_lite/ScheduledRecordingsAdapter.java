package com.ss.Misty_Screen_Recoder_lite;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ScheduledRecordingsAdapter extends RecyclerView.Adapter<ScheduledRecordingsAdapter.ViewHolder> {
    
    private final List<SmartRecordingFragment.ScheduledRecording> recordings;
    private final OnEditClickListener editListener;
    private final OnCancelClickListener cancelListener;
    
    public interface OnEditClickListener {
        void onEditClick(SmartRecordingFragment.ScheduledRecording recording);
    }
    
    public interface OnCancelClickListener {
        void onCancelClick(SmartRecordingFragment.ScheduledRecording recording);
    }
    
    public ScheduledRecordingsAdapter(List<SmartRecordingFragment.ScheduledRecording> recordings,
                                    OnEditClickListener editListener,
                                    OnCancelClickListener cancelListener) {
        this.recordings = recordings;
        this.editListener = editListener;
        this.cancelListener = cancelListener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scheduled_recording, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(recordings.get(position));
    }
    
    @Override
    public int getItemCount() {
        return recordings.size();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvRecordingName;
        private final TextView tvScheduledTime;
        private final TextView tvStatus;
        private final TextView tvDuration;
        private final TextView tvQuality;
        private final MaterialButton btnEdit;
        private final MaterialButton btnCancel;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvRecordingName = itemView.findViewById(R.id.tvRecordingName);
            tvScheduledTime = itemView.findViewById(R.id.tvScheduledTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvQuality = itemView.findViewById(R.id.tvQuality);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnCancel = itemView.findViewById(R.id.btnCancel);
        }
        
        void bind(SmartRecordingFragment.ScheduledRecording recording) {
            tvRecordingName.setText(recording.getName());
            
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.US);
            tvScheduledTime.setText(sdf.format(recording.getScheduledTime()));
            
            tvDuration.setText(recording.getDuration() + " minutes");
            tvQuality.setText(recording.getQuality());
            
            // Set status based on time
            long now = System.currentTimeMillis();
            if (recording.getScheduledTime() <= now) {
                tvStatus.setText("Overdue");
                tvStatus.setBackgroundResource(R.drawable.status_overdue_background);
            } else {
                tvStatus.setText("Scheduled");
                tvStatus.setBackgroundResource(R.drawable.status_scheduled_background);
            }
            
            btnEdit.setOnClickListener(v -> {
                if (editListener != null) {
                    editListener.onEditClick(recording);
                }
            });
            
            btnCancel.setOnClickListener(v -> {
                if (cancelListener != null) {
                    cancelListener.onCancelClick(recording);
                }
            });
        }
    }
}
