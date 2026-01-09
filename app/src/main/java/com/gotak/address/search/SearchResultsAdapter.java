package com.gotak.address.search;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gotak.address.plugin.R;
import com.gotak.address.search.nearby.IconsetHelper;
import com.gotak.address.search.nearby.OverpassSearchResult;
import com.gotak.address.search.nearby.PointOfInterestType;

import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying search results.
 * Supports both address results (NominatimSearchResult) and POI results (OverpassSearchResult).
 */
public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ViewHolder> {

    private static final int VIEW_TYPE_ADDRESS = 0;
    private static final int VIEW_TYPE_POI = 1;

    private final List<Object> results = new ArrayList<>();
    private final OnResultClickListener listener;
    private final Context context;
    private int currentViewType = VIEW_TYPE_ADDRESS;
    private Context pluginContext; // For POI string resources
    private IconsetHelper iconsetHelper; // For consistent POI icons

    /**
     * Interface for handling result item clicks.
     */
    public interface OnResultClickListener {
        void onResultClick(NominatimSearchResult result);
        void onResultDropMarker(NominatimSearchResult result);
        void onResultNavigate(NominatimSearchResult result);
    }

    public SearchResultsAdapter(Context context, OnResultClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    /**
     * Update the list of address results displayed.
     */
    public void setResults(List<NominatimSearchResult> newResults) {
        results.clear();
        currentViewType = VIEW_TYPE_ADDRESS;
        if (newResults != null) {
            results.addAll(newResults);
        }
        notifyDataSetChanged();
    }

    /**
     * Update the list of POI results displayed.
     */
    public void setPoiResults(List<OverpassSearchResult> poiResults, Context pluginCtx) {
        results.clear();
        currentViewType = VIEW_TYPE_POI;
        this.pluginContext = pluginCtx;
        // Initialize IconsetHelper for consistent POI icons (same as Nearby tab)
        if (this.iconsetHelper == null) {
            this.iconsetHelper = new IconsetHelper(pluginCtx);
        }
        if (poiResults != null) {
            results.addAll(poiResults);
        }
        notifyDataSetChanged();
    }

    /**
     * Clear all results.
     */
    public void clear() {
        results.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return currentViewType;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.address_search_result_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Object item = results.get(position);
        if (item instanceof NominatimSearchResult) {
            holder.bindAddress((NominatimSearchResult) item, listener);
        } else if (item instanceof OverpassSearchResult) {
            holder.bindPoi((OverpassSearchResult) item, listener, pluginContext, iconsetHelper);
        }
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    /**
     * ViewHolder for search result items.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        private final View clickableArea;
        private final ImageView iconView;
        private final TextView nameText;
        private final TextView addressText;
        private final ImageButton dropMarkerButton;
        private final ImageButton navigateButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            clickableArea = itemView.findViewById(R.id.result_item_clickable);
            iconView = itemView.findViewById(R.id.result_icon);
            nameText = itemView.findViewById(R.id.result_name);
            addressText = itemView.findViewById(R.id.result_address);
            dropMarkerButton = itemView.findViewById(R.id.result_drop_marker);
            navigateButton = itemView.findViewById(R.id.result_navigate);
        }

        void bindAddress(NominatimSearchResult result, OnResultClickListener listener) {
            nameText.setText(result.getName());
            addressText.setText(result.getDisplayName());

            // Set icon based on location type
            LocationType locationType = LocationType.fromResult(result);
            iconView.setImageResource(locationType.getIconRes());

            clickableArea.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onResultClick(result);
                }
            });

            dropMarkerButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onResultDropMarker(result);
                }
            });

            navigateButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onResultNavigate(result);
                }
            });
        }

        void bindPoi(OverpassSearchResult result, OnResultClickListener listener, Context pluginContext, IconsetHelper iconHelper) {
            // Display name with distance
            String displayName = result.getDisplayName();
            String distance = result.getFormattedDistance();
            nameText.setText(displayName + " (" + distance + ")");

            // Build subtitle with category and address
            PointOfInterestType poiType = result.getPoiType();
            StringBuilder subtitle = new StringBuilder();
            if (poiType != null && pluginContext != null) {
                subtitle.append(pluginContext.getString(poiType.getStringResId()));
            }
            String address = result.getAddress();
            if (address != null && !address.isEmpty()) {
                if (subtitle.length() > 0) subtitle.append(" • ");
                subtitle.append(address);
            }
            addressText.setText(subtitle.toString());

            // Set icon based on POI type - use IconsetHelper for consistency with Nearby tab
            if (poiType != null && iconHelper != null) {
                Drawable icon = iconHelper.getIconDrawable(poiType);
                if (icon != null) {
                    iconView.setImageDrawable(icon);
                } else {
                    iconView.setImageResource(R.drawable.ic_location_pin);
                }
            } else {
                iconView.setImageResource(R.drawable.ic_location_pin);
            }

            // Create a temporary NominatimSearchResult for click handlers
            // This allows reusing the existing navigation/marker logic
            NominatimSearchResult tempResult = new NominatimSearchResult(
                result.getOsmId(),
                result.getLatitude(),
                result.getLongitude(),
                result.getAddress() != null ? result.getAddress() : "",
                result.getDisplayName(),
                poiType != null ? poiType.getOsmValue() : "poi",
                result.getOsmType(),
                result.getOsmId()
            );

            clickableArea.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onResultClick(tempResult);
                }
            });

            dropMarkerButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onResultDropMarker(tempResult);
                }
            });

            navigateButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onResultNavigate(tempResult);
                }
            });
        }

    }
}

