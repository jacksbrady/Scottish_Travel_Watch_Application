package com.example.brady_jack_s2327712.ui;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.brady_jack_s2327712.R;
import com.example.brady_jack_s2327712.model.GeoPoint;
import com.example.brady_jack_s2327712.model.Situation;
import com.example.brady_jack_s2327712.model.SituationCategory;
import com.example.brady_jack_s2327712.viewmodel.TrafficViewModel;

import java.util.Locale;

/**
 * Screen 2 - Situation Detail
 *
 * Shows all known details of situations put into groups which updates
 * every time a new situation is selected as it reads from the ViewModel
 */
public class SituationDetailFragment extends Fragment {

    // Callback to the Activity for the "Show on map" button
    public interface OnShowOnMapListener {
        void onShowOnMap();
    }

    private OnShowOnMapListener mapListener;

    private View emptyView;
    private View contentView;
    private LinearLayout header;
    private ImageView icon;
    private TextView title;
    private TextView subtitle;
    private TextView comment;
    private LinearLayout impactRows;
    private LinearLayout whenRows;
    private LinearLayout locationRows;
    private LinearLayout recordRows;

    public SituationDetailFragment() {
        super(R.layout.fragment_situation_detail);
    }

    // Keep a reference to the Activity which implements the interface
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnShowOnMapListener) {
            mapListener = (OnShowOnMapListener) context;
        } else {
            throw new RuntimeException(context + " must implement OnShowOnMapListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mapListener = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emptyView = view.findViewById(R.id.detailEmpty);
        contentView = view.findViewById(R.id.detailContent);
        header = view.findViewById(R.id.detailHeader);
        icon = view.findViewById(R.id.detailIcon);
        title = view.findViewById(R.id.detailTitle);
        subtitle = view.findViewById(R.id.detailSubtitle);
        comment = view.findViewById(R.id.detailComment);
        impactRows = view.findViewById(R.id.impactRows);
        whenRows = view.findViewById(R.id.whenRows);
        locationRows = view.findViewById(R.id.locationRows);
        recordRows = view.findViewById(R.id.recordRows);

        // Opens the map zoomed to chosen situation
        view.findViewById(R.id.showOnMapButton).setOnClickListener(v -> {
            if (mapListener != null) mapListener.onShowOnMap();
        });

        // Same shared ViewModel as the list, the list stores the selection, this screen shows it
        TrafficViewModel viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);
        show(viewModel.getSelected().getValue());   // show the current state straight away (hint if nothing selected)
        viewModel.getSelected().observe(getViewLifecycleOwner(), this::show);
    }

    // Fills the screen from one situation or shows a hint if nothing is selected yet
    private void show(Situation situation) {
        if (situation == null) {
            emptyView.setVisibility(View.VISIBLE);
            contentView.setVisibility(View.GONE);
            return;
        }
        emptyView.setVisibility(View.GONE);
        contentView.setVisibility(View.VISIBLE);

        SituationCategory category = situation.getCategory();
        header.setBackgroundColor(ContextCompat.getColor(requireContext(), category.getColourRes()));
        icon.setImageResource(category.getIconRes());
        icon.setColorFilter(Color.WHITE);
        icon.setContentDescription(category.getLabel());
        title.setText(category.getLabel());
        String road = situation.getRoadName() != null ? situation.getRoadName() : "Unknown road";
        String direction = situation.getReadableDirection();
        subtitle.setText(direction.isEmpty() ? road : road + " - " + direction);
        comment.setText(situation.getComment());

        // Clear old rows first (this method runs again whenever the selection changes)
        impactRows.removeAllViews();
        whenRows.removeAllViews();
        locationRows.removeAllViews();
        recordRows.removeAllViews();

        // Impact
        addRow(impactRows, "Impact", situation.getImpactSummary());
        addRow(impactRows, "Type", situation.getReadableSubType());
        if (category.isRoadworks()) {
            String delays = situation.getDelaysType() == null
                    ? "None reported" : Situation.camelCaseToWords(situation.getDelaysType());
            addRow(impactRows, "Delays", delays);
            addRow(impactRows, "Urgent", situation.isUrgent() ? "Yes" : "No");
        }

        // When (validity)
        addRow(whenRows, "Starts", situation.getStartText());
        addRow(whenRows, "Ends", situation.getEndText());
        addRow(whenRows, "Duration", situation.getDurationText());

        // Location(s)
        addRow(locationRows, "Road", road);
        addRow(locationRows, "Direction", direction);
        addRow(locationRows, "From", pointText(situation.getFrom()));
        addRow(locationRows, "To", pointText(situation.getTo()));

        // Record details
        addRow(recordRows, "Feed", situation.isFromRoadworksFeed() ? "Current roadworks" : "Unplanned events");
        addRow(recordRows, "Status", Situation.camelCaseToWords(situation.getProbability()));
        addRow(recordRows, "Reference", situation.getId());
        addRow(recordRows, "Created", situation.getCreationText());
        addRow(recordRows, "Last updated", situation.getLastUpdatedText());
    }

    // Adds one "label: value" row to a section. Rows with no value are skipped
    private void addRow(LinearLayout section, String label, String value) {
        if (value == null || value.isEmpty()) return;
        View row = getLayoutInflater().inflate(R.layout.item_detail_row, section, false);
        ((TextView) row.findViewById(R.id.rowLabel)).setText(label);
        ((TextView) row.findViewById(R.id.rowValue)).setText(value);
        section.addView(row);
    }

    private String pointText(GeoPoint point) {
        if (point == null) return null;
        if (!point.hasCoordinates()) return point.getDisplayName();
        return point.getDisplayName() + "\n"
                + String.format(Locale.UK, "%.5f, %.5f", point.getLatitude(), point.getLongitude());
    }
}