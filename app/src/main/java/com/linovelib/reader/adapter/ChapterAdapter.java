package com.linovelib.reader.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.linovelib.reader.R;
import com.linovelib.reader.model.ChapterItem;

import java.util.ArrayList;
import java.util.List;

public class ChapterAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ChapterItem> items = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<ChapterItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ChapterItem.TYPE_IMAGE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chapter_image, parent, false);
            return new ImageViewHolder(view);
        } else if (viewType == ChapterItem.TYPE_TITLE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chapter_title, parent, false);
            return new TitleViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chapter_text, parent, false);
            return new TextViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChapterItem item = items.get(position);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick();
            }
        });

        if (holder instanceof TextViewHolder) {
            ((TextViewHolder) holder).tvContent.setText(item.getContent());
        } else if (holder instanceof ImageViewHolder) {
            GlideUrl glideUrl = new GlideUrl(item.getContent(), new LazyHeaders.Builder()
                    .addHeader("Referer", "https://tw.linovelib.com/")
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    .build());

            Glide.with(holder.itemView.getContext())
                    .load(glideUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .fitCenter()
                    .into(((ImageViewHolder) holder).ivContent);
        } else if (holder instanceof TitleViewHolder) {
            ((TitleViewHolder) holder).tvTitle.setText(item.getContent());
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class TextViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent;

        TextViewHolder(View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tvContent);
        }
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivContent;

        ImageViewHolder(View itemView) {
            super(itemView);
            ivContent = itemView.findViewById(R.id.ivContent);
        }
    }
    
    static class TitleViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;

        TitleViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
        }
    }
}
