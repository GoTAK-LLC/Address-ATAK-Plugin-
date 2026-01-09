package com.gotak.address.search;

import android.content.Context;
import android.graphics.drawable.Drawable;
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
import com.gotak.address.search.nearby.PointOfInterestType;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying search history with remove buttons.
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final List<NominatimSearchResult> items = new ArrayList<>();
    private final HistoryItemListener listener;
    private final Context context;
    private final IconsetHelper iconsetHelper;

    /**
     * Interface for handling history item interactions.
     */
    public interface HistoryItemListener {
        void onHistoryItemClick(NominatimSearchResult result);
        void onHistoryItemRemove(NominatimSearchResult result);
        void onHistoryItemNavigate(NominatimSearchResult result);
        void onHistoryItemDropMarker(NominatimSearchResult result);
    }

    public HistoryAdapter(Context context, HistoryItemListener listener) {
        this.context = context;
        this.listener = listener;
        this.iconsetHelper = new IconsetHelper(context);
    }

    /**
     * Update the list of history items.
     */
    public void setItems(List<NominatimSearchResult> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    /**
     * Clear all items.
     */
    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.address_history_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NominatimSearchResult item = items.get(position);
        holder.bind(item, listener, iconsetHelper);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * ViewHolder for history items.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        private final View clickableArea;
        private final ImageView iconView;
        private final TextView nameText;
        private final TextView addressText;
        private final ImageButton dropMarkerButton;
        private final ImageButton navigateButton;
        private final ImageButton removeButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            clickableArea = itemView.findViewById(R.id.history_item_clickable);
            iconView = itemView.findViewById(R.id.history_icon);
            nameText = itemView.findViewById(R.id.history_name);
            addressText = itemView.findViewById(R.id.history_address);
            dropMarkerButton = itemView.findViewById(R.id.history_drop_marker);
            navigateButton = itemView.findViewById(R.id.history_navigate);
            removeButton = itemView.findViewById(R.id.history_remove);
        }

        void bind(NominatimSearchResult item, HistoryItemListener listener, IconsetHelper iconHelper) {
            nameText.setText(item.getName());
            addressText.setText(item.getDisplayName());

            // Try to get POI icon first (for POI search results that were saved to history)
            Drawable poiIcon = null;
            String type = item.getType();
            if (type != null && iconHelper != null) {
                PointOfInterestType poiType = mapTypeToPoiType(type);
                if (poiType != null) {
                    poiIcon = iconHelper.getIconDrawable(poiType);
                }
            }
            
            if (poiIcon != null) {
                // Use POI-specific icon (same as Nearby tab)
                iconView.setImageDrawable(poiIcon);
            } else {
                // Fall back to location type icon (city, country, etc.)
                LocationType locationType = LocationType.fromResult(item);
                iconView.setImageResource(locationType.getIconRes());
            }

            clickableArea.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onHistoryItemClick(item);
                }
            });

            dropMarkerButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onHistoryItemDropMarker(item);
                }
            });

            navigateButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onHistoryItemNavigate(item);
                }
            });

            removeButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onHistoryItemRemove(item);
                }
            });
        }
        
        /**
         * Try to map a type string to a PointOfInterestType enum.
         */
        private PointOfInterestType mapTypeToPoiType(String type) {
            if (type == null) return null;
            
            String typeLower = type.toLowerCase().replace(" ", "_");
            
            // Try direct enum match first
            try {
                return PointOfInterestType.valueOf(typeLower.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
            
            // Map common type strings to POI types
            switch (typeLower) {
                case "fuel":
                    return PointOfInterestType.GAS_STATION;
                case "hospital":
                    return PointOfInterestType.HOSPITAL;
                case "pharmacy":
                    return PointOfInterestType.PHARMACY;
                case "police":
                    return PointOfInterestType.POLICE_STATION;
                case "fire_station":
                    return PointOfInterestType.FIRE_STATION;
                case "bank":
                    return PointOfInterestType.BANK;
                case "atm":
                    return PointOfInterestType.ATM;
                case "restaurant":
                    return PointOfInterestType.RESTAURANT;
                case "cafe":
                    return PointOfInterestType.CAFE;
                case "fast_food":
                    return PointOfInterestType.FAST_FOOD;
                case "bar":
                    return PointOfInterestType.BAR;
                case "pub":
                    return PointOfInterestType.PUB;
                case "hotel":
                    return PointOfInterestType.HOTEL;
                case "supermarket":
                    return PointOfInterestType.SUPERMARKET;
                case "convenience":
                    return PointOfInterestType.CONVENIENCE_STORE;
                case "parking":
                    return PointOfInterestType.PARKING;
                case "school":
                    return PointOfInterestType.SCHOOL;
                case "library":
                    return PointOfInterestType.LIBRARY;
                case "cinema":
                    return PointOfInterestType.CINEMA;
                case "place_of_worship":
                case "church":
                    return PointOfInterestType.PLACE_OF_WORSHIP;
                case "dentist":
                    return PointOfInterestType.DENTIST;
                case "doctors":
                case "doctor":
                    return PointOfInterestType.DOCTOR;
                case "clinic":
                    return PointOfInterestType.CLINIC;
                case "veterinary":
                case "vet":
                    return PointOfInterestType.VETERINARIAN;
                case "post_office":
                    return PointOfInterestType.POST_OFFICE;
                case "aerodrome":
                case "airport":
                    return PointOfInterestType.AIRPORT;
                case "helipad":
                case "heliport":
                    return PointOfInterestType.HELIPORT;
                default:
                    return null;
            }
        }
    }
}

