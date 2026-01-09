package com.gotak.address.search.nearby;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gotak.address.plugin.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RecyclerView adapter for displaying Nearby POI search results.
 */
public class NearbyResultsAdapter extends RecyclerView.Adapter<NearbyResultsAdapter.ViewHolder> {

    private final Context context;
    private final List<OverpassSearchResult> results;
    private final Set<String> selectedIds;
    private final OnResultClickListener listener;
    private final IconsetHelper iconsetHelper;
    private OnSelectionChangedListener selectionListener;

    /**
     * Listener interface for result item interactions.
     */
    public interface OnResultClickListener {
        void onResultClick(OverpassSearchResult result);
        void onNavigateClick(OverpassSearchResult result);
        void onDropMarkerClick(OverpassSearchResult result);
    }

    /**
     * Listener interface for selection changes.
     */
    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount, int totalCount);
    }

    public NearbyResultsAdapter(Context context, OnResultClickListener listener) {
        this.context = context;
        this.results = new ArrayList<>();
        this.selectedIds = new HashSet<>();
        this.listener = listener;
        this.iconsetHelper = new IconsetHelper(context);
    }

    public void setSelectionListener(OnSelectionChangedListener listener) {
        this.selectionListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.nearby_result_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OverpassSearchResult result = results.get(position);
        holder.bind(result);
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    /**
     * Set the list of results to display.
     */
    public void setResults(List<OverpassSearchResult> newResults) {
        results.clear();
        selectedIds.clear();
        if (newResults != null) {
            results.addAll(newResults);
        }
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    /**
     * Clear all results.
     */
    public void clear() {
        results.clear();
        selectedIds.clear();
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    /**
     * Toggle selection state for a result.
     */
    public void toggleSelection(OverpassSearchResult result) {
        String id = result.getUniqueId();
        if (selectedIds.contains(id)) {
            selectedIds.remove(id);
        } else {
            selectedIds.add(id);
        }
        notifySelectionChanged();
    }

    /**
     * Check if a result is selected.
     */
    public boolean isSelected(OverpassSearchResult result) {
        return selectedIds.contains(result.getUniqueId());
    }

    /**
     * Select all results.
     */
    public void selectAll() {
        for (OverpassSearchResult result : results) {
            selectedIds.add(result.getUniqueId());
        }
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    /**
     * Deselect all results.
     */
    public void deselectAll() {
        selectedIds.clear();
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    /**
     * Check if all results are selected.
     */
    public boolean isAllSelected() {
        return !results.isEmpty() && selectedIds.size() == results.size();
    }

    /**
     * Get the count of selected results.
     */
    public int getSelectedCount() {
        return selectedIds.size();
    }

    /**
     * Get all selected results.
     */
    public List<OverpassSearchResult> getSelectedResults() {
        List<OverpassSearchResult> selected = new ArrayList<>();
        for (OverpassSearchResult result : results) {
            if (selectedIds.contains(result.getUniqueId())) {
                selected.add(result);
            }
        }
        return selected;
    }

    /**
     * Get all results.
     */
    public List<OverpassSearchResult> getAllResults() {
        return new ArrayList<>(results);
    }

    private void notifySelectionChanged() {
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(selectedIds.size(), results.size());
        }
    }

    /**
     * ViewHolder for a single POI result item.
     */
    class ViewHolder extends RecyclerView.ViewHolder {
        private final CheckBox checkbox;
        private final ImageView iconView;
        private final TextView nameText;
        private final TextView typeText;
        private final TextView distanceText;
        private final TextView addressText;
        private final ImageButton navigateButton;
        private final ImageButton markerButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            checkbox = itemView.findViewById(R.id.nearby_result_checkbox);
            iconView = itemView.findViewById(R.id.nearby_result_icon);
            nameText = itemView.findViewById(R.id.nearby_result_name);
            typeText = itemView.findViewById(R.id.nearby_result_type);
            distanceText = itemView.findViewById(R.id.nearby_result_distance);
            addressText = itemView.findViewById(R.id.nearby_result_address);
            navigateButton = itemView.findViewById(R.id.nearby_navigate_button);
            markerButton = itemView.findViewById(R.id.nearby_marker_button);
        }

        void bind(OverpassSearchResult result) {
            // Set checkbox state
            checkbox.setChecked(isSelected(result));

            // Set POI icon
            if (result.getPoiType() != null) {
                Drawable icon = iconsetHelper.getIconDrawable(result.getPoiType());
                if (icon != null) {
                    iconView.setImageDrawable(icon);
                    iconView.setVisibility(View.VISIBLE);
                } else {
                    iconView.setVisibility(View.GONE);
                }
            } else {
                iconView.setVisibility(View.GONE);
            }

            // Set name
            nameText.setText(result.getDisplayName());

            // Set POI type
            if (result.getPoiType() != null) {
                typeText.setText(result.getPoiType().getStringResId());
                typeText.setVisibility(View.VISIBLE);
            } else {
                typeText.setVisibility(View.GONE);
            }

            // Set distance
            distanceText.setText(result.getFormattedDistance());

            // Set address (hide if empty)
            String address = result.getAddress();
            if (address != null && !address.isEmpty()) {
                addressText.setText(address);
                addressText.setVisibility(View.VISIBLE);
            } else {
                addressText.setVisibility(View.GONE);
            }

            // Checkbox click listener - toggle selection only
            checkbox.setOnClickListener(v -> {
                toggleSelection(result);
                // Notify item changed to update checkbox state
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    notifyItemChanged(pos);
                }
            });

            // Row click navigates to the POI
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onResultClick(result);
                }
            });

            navigateButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNavigateClick(result);
                }
            });

            markerButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDropMarkerClick(result);
                }
            });
        }
    }
}

