package com.gotak.address.search.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gotak.address.plugin.R;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying saved views.
 * Supports drag-and-drop reordering.
 */
public class ViewsAdapter extends RecyclerView.Adapter<ViewsAdapter.ViewHolder> {
    
    private final Context context;
    private final List<SavedView> views;
    private final ViewActionListener listener;
    
    /**
     * Listener for view card actions.
     */
    public interface ViewActionListener {
        void onViewClick(SavedView view);
        void onRenameClick(SavedView view);
        void onDeleteClick(SavedView view);
        void onViewsMoved(int fromPosition, int toPosition);
    }
    
    public ViewsAdapter(Context context, ViewActionListener listener) {
        this.context = context;
        this.listener = listener;
        this.views = new ArrayList<>();
    }
    
    /**
     * Move an item from one position to another (called during drag-drop).
     */
    public void moveItem(int fromPosition, int toPosition) {
        if (fromPosition < 0 || fromPosition >= views.size() ||
            toPosition < 0 || toPosition >= views.size()) {
            return;
        }
        
        SavedView view = views.remove(fromPosition);
        views.add(toPosition, view);
        notifyItemMoved(fromPosition, toPosition);
        
        // Notify listener to persist the change
        if (listener != null) {
            listener.onViewsMoved(fromPosition, toPosition);
        }
    }
    
    /**
     * Get the current list of views (for persisting order).
     */
    public List<SavedView> getViews() {
        return new ArrayList<>(views);
    }
    
    /**
     * Update the list of views.
     */
    public void setViews(List<SavedView> newViews) {
        views.clear();
        if (newViews != null) {
            views.addAll(newViews);
        }
        notifyDataSetChanged();
    }
    
    /**
     * Clear all views.
     */
    public void clear() {
        views.clear();
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.view_card_item, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SavedView view = views.get(position);
        holder.bind(view);
    }
    
    @Override
    public int getItemCount() {
        return views.size();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        
        private final FrameLayout thumbnailContainer;
        private final ImageView thumbnail;
        private final TextView modeBadge;
        private final TextView name;
        private final TextView subtitle;
        private final ImageButton renameButton;
        private final ImageButton deleteButton;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            
            // The thumbnail container is the FrameLayout parent of the ImageView
            thumbnailContainer = (FrameLayout) itemView.findViewById(R.id.view_thumbnail).getParent();
            thumbnail = itemView.findViewById(R.id.view_thumbnail);
            modeBadge = itemView.findViewById(R.id.view_mode_badge);
            name = itemView.findViewById(R.id.view_name);
            subtitle = itemView.findViewById(R.id.view_subtitle);
            renameButton = itemView.findViewById(R.id.view_rename_button);
            deleteButton = itemView.findViewById(R.id.view_delete_button);
        }
        
        void bind(SavedView view) {
            // Make thumbnail square by setting height equal to width
            thumbnailContainer.post(() -> {
                int width = thumbnailContainer.getWidth();
                if (width > 0) {
                    ViewGroup.LayoutParams params = thumbnailContainer.getLayoutParams();
                    if (params.height != width) {
                        params.height = width;
                        thumbnailContainer.setLayoutParams(params);
                    }
                }
            });
            // Name
            name.setText(view.getName());
            
            // Subtitle (address or coordinates)
            String sub = view.getGeocodedAddress();
            if (sub == null || sub.isEmpty()) {
                // Show coordinates if no address
                sub = String.format("%.4f, %.4f", view.getLatitude(), view.getLongitude());
            }
            subtitle.setText(sub);
            
            // Thumbnail
            Bitmap thumb = view.getThumbnail();
            if (thumb != null) {
                thumbnail.setImageBitmap(thumb);
                thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                thumbnail.setImageResource(R.drawable.ic_views_empty);
                thumbnail.setScaleType(ImageView.ScaleType.CENTER);
            }
            
            // Mode badge (2D/3D)
            modeBadge.setText(view.is3DMode() ? "3D" : "2D");
            
            // Click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewClick(view);
                }
            });
            
            renameButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRenameClick(view);
                }
            });
            
            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(view);
                }
            });
        }
    }
}

