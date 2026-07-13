package com.ss.Misty_Screen_Recoder_lite;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.util.LruCache;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecordingsAdapter extends RecyclerView.Adapter<RecordingsAdapter.ViewHolder> {

    public interface Listener {
        void onItemClick(RecordingItem item);

        void onItemMenuClick(RecordingItem item, View anchor);
    }

    private final List<RecordingItem> items = new ArrayList<>();
    private final Listener listener;
    private final ExecutorService thumbnailExecutor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final LruCache<String, Bitmap> thumbnailCache = new LruCache<>(40);

    public RecordingsAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<RecordingItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void shutdown() {
        thumbnailExecutor.shutdownNow();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recording, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecordingItem item = items.get(position);
        Context context = holder.itemView.getContext();

        holder.title.setText(item.displayName);

        if (item.durationMs > 0) {
            holder.durationBadge.setText(DateUtils.formatElapsedTime(item.durationMs / 1000));
            holder.durationBadge.setVisibility(View.VISIBLE);
        } else {
            holder.durationBadge.setVisibility(View.GONE);
        }

        StringBuilder meta = new StringBuilder();
        if (item.sizeBytes > 0) {
            meta.append(Formatter.formatShortFileSize(context, item.sizeBytes));
        }
        if (item.dateAddedMs > 0) {
            if (meta.length() > 0) meta.append("  •  ");
            meta.append(DateUtils.getRelativeTimeSpanString(item.dateAddedMs));
        }
        if (item.encrypted) {
            if (meta.length() > 0) meta.append("  •  ");
            meta.append(context.getString(R.string.encrypted));
        }
        holder.subtitle.setText(meta.toString());

        holder.lockBadge.setVisibility(item.encrypted ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
        holder.menuButton.setOnClickListener(v -> listener.onItemMenuClick(item, v));

        bindThumbnail(holder, item);
    }

    private void bindThumbnail(ViewHolder holder, RecordingItem item) {
        String key = item.isVaultItem() ? item.file.getAbsolutePath() : String.valueOf(item.uri);
        holder.thumbnailKey = key;
        Bitmap cached = thumbnailCache.get(key);
        if (cached != null) {
            holder.thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            holder.thumbnail.setImageBitmap(cached);
            return;
        }
        holder.thumbnail.setScaleType(ImageView.ScaleType.CENTER);
        holder.thumbnail.setImageResource(R.drawable.ic_video_placeholder);
        if (item.encrypted) {
            // Encrypted files stay opaque — show the placeholder with the lock badge.
            return;
        }
        Context appContext = holder.itemView.getContext().getApplicationContext();
        thumbnailExecutor.execute(() -> {
            Bitmap bitmap = null;
            try {
                if (item.isVaultItem()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        bitmap = ThumbnailUtils.createVideoThumbnail(item.file, new Size(512, 384), null);
                    } else {
                        bitmap = ThumbnailUtils.createVideoThumbnail(item.file.getAbsolutePath(),
                                MediaStore.Images.Thumbnails.MINI_KIND);
                    }
                } else if (item.uri != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        bitmap = appContext.getContentResolver().loadThumbnail(item.uri, new Size(512, 384), null);
                    } else {
                        android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
                        try {
                            retriever.setDataSource(appContext, item.uri);
                            bitmap = retriever.getFrameAtTime();
                        } finally {
                            retriever.release();
                        }
                    }
                }
            } catch (Exception e) {
                LogUtils.e("RecordingsAdapter", "Thumbnail failed: " + e.getMessage());
            }
            if (bitmap != null) {
                thumbnailCache.put(key, bitmap);
                final Bitmap result = bitmap;
                mainHandler.post(() -> {
                    if (key.equals(holder.thumbnailKey)) {
                        holder.thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        holder.thumbnail.setImageBitmap(result);
                    }
                });
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView thumbnail;
        final ImageView lockBadge;
        final ImageView menuButton;
        final TextView title;
        final TextView subtitle;
        final TextView durationBadge;
        String thumbnailKey;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.recording_thumbnail);
            lockBadge = itemView.findViewById(R.id.recording_lock_badge);
            menuButton = itemView.findViewById(R.id.recording_menu);
            title = itemView.findViewById(R.id.recording_title);
            subtitle = itemView.findViewById(R.id.recording_subtitle);
            durationBadge = itemView.findViewById(R.id.recording_duration_badge);
        }
    }
}
