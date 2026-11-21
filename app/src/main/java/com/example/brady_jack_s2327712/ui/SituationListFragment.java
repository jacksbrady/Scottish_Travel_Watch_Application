package com.example.brady_jack_s2327712.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.example.brady_jack_s2327712.R;
import com.example.brady_jack_s2327712.model.Situation;
import com.example.brady_jack_s2327712.viewmodel.TrafficViewModel;
import com.example.brady_jack_s2327712.viewmodel.TrafficViewModel.DistanceFilter;
import com.example.brady_jack_s2327712.viewmodel.TrafficViewModel.TypeFilter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

/**
 * Screen 1 - The List Screen
 * <p>
 * Shows situations with type, icon, road  and description with the ability
 * to search reads with incidents/roadworks filter and distance search
 * <p>
 * The search passes the users selection to the View model and displays the filtered lists
 * it receives. Tapping a situation communicates to the activity which shows its details
 */

public class SituationListFragment extends Fragment {

    public interface OnSituationSelectedListener {
        void onSituationSelected();
    }

    // Reference places for the distance search, in the same order as R.array.reference_places
    // The last entry ("My location") uses the phone's coarse location.
    private static final double[] PLACE_LATITUDES = {55.8666, 55.9533, 56.4620, 57.1497, 57.4778, 56.1165};
    private static final double[] PLACE_LONGITUDES = {-4.2504, -3.1883, -2.9707, -2.0943, -4.2247, -3.9369};

    private TrafficViewModel viewModel;
    private SituationAdapter adapter;
    private OnSituationSelectedListener listener;
    private int pendingKm;   // distance chosen while waiting for the permission answer

    // Asks the user for the location permission at runtime
    private final ActivityResultLauncher<String> locationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    useDeviceLocation(pendingKm);
                } else {
                    Toast.makeText(requireContext(),
                            "Location permission denied - choose a place from the list instead",
                            Toast.LENGTH_LONG).show();
                }
            });

    public SituationListFragment() {
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnSituationSelectedListener) {
            listener = (OnSituationSelectedListener) context;
        } else {
            throw new RuntimeException(context + " must implement OnSituationSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_situation_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ListView listView = view.findViewById(R.id.situationList);
        TextView statusText = view.findViewById(R.id.statusText);
        TextView emptyText = view.findViewById(R.id.emptyText);
        LinearProgressIndicator progress = view.findViewById(R.id.progress);
        ChipGroup filterGroup = view.findViewById(R.id.filterGroup);
        MaterialAutoCompleteTextView roadSearch = view.findViewById(R.id.roadSearch);
        Chip distanceChip = view.findViewById(R.id.distanceChip);
        TextView resultCount = view.findViewById(R.id.resultCount);

        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);


        adapter = new SituationAdapter(requireContext());
        listView.setAdapter(adapter);
        listView.setEmptyView(emptyText);
        listView.setOnItemClickListener((parent, itemView, position, id) -> {
            Situation situation = (Situation) parent.getItemAtPosition(position);
            viewModel.select(situation);
            if (listener != null) listener.onSituationSelected();
        });

        TypeFilter current = viewModel.getTypeFilter().getValue();
        if (current == TypeFilter.INCIDENTS) filterGroup.check(R.id.chipIncidents);
        else if (current == TypeFilter.ROADWORKS) filterGroup.check(R.id.chipRoadworks);
        else filterGroup.check(R.id.chipAll);

        filterGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checked = checkedIds.get(0);
            if (checked == R.id.chipIncidents) viewModel.setTypeFilter(TypeFilter.INCIDENTS);
            else if (checked == R.id.chipRoadworks) viewModel.setTypeFilter(TypeFilter.ROADWORKS);
            else viewModel.setTypeFilter(TypeFilter.ALL);
        });

        // Show the ViewModel's current search first (false = don't pop up the suggestions)
        roadSearch.setText(viewModel.getRoadQuery().getValue(), false);
        // Filter as the user types
        roadSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setRoadQuery(s.toString());
            }
        });
        // Picking a suggestion closes the keyboard so the results are clearly visible
        roadSearch.setOnItemClickListener((parent, itemView, position, id) -> hideKeyboard(roadSearch));

        // Search by distance
        distanceChip.setOnClickListener(v -> showDistanceDialog());
        distanceChip.setOnCloseIconClickListener(v -> viewModel.setDistanceFilter(null));

        LifecycleOwner owner = getViewLifecycleOwner();
        viewModel.getFilteredSituations().observe(owner, list -> {
            adapter.setSituations(list);
            resultCount.setText(getString(R.string.showing_count, list.size()));
        });
        viewModel.getStatus().observe(owner, statusText::setText);
        viewModel.isLoading().observe(owner, loading ->
                progress.setVisibility(loading ? View.VISIBLE : View.GONE));

        // Road names for the search suggestions
        viewModel.getRoadNames().observe(owner, names -> roadSearch.setAdapter(
                new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, names)));

        // The chip shows whether a distance search is on, and the adapter shows distances
        viewModel.getDistanceFilter().observe(owner, near -> {
            adapter.setDistanceFilter(near);
            if (near == null) {
                distanceChip.setText(R.string.distance_off);
                distanceChip.setCloseIconVisible(false);
            } else {
                distanceChip.setText(getString(R.string.distance_on, near.getKm(), near.getPlaceName()));
                distanceChip.setCloseIconVisible(true);   // the x clears the distance search
            }
        });
    }

    // AlertDialog with a custom layout - a Spinner of places and a distance Slider
    private void showDistanceDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_distance, null);
        Spinner placeSpinner = dialogView.findViewById(R.id.placeSpinner);
        Slider slider = dialogView.findViewById(R.id.distanceSlider);
        TextView distanceLabel = dialogView.findViewById(R.id.distanceLabel);

        // Spinner filled from a string-array resource
        ArrayAdapter<CharSequence> places = ArrayAdapter.createFromResource(requireContext(),
                R.array.reference_places, android.R.layout.simple_spinner_item);
        places.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        placeSpinner.setAdapter(places);

        // Start from the current search, if there is one
        DistanceFilter current = viewModel.getDistanceFilter().getValue();
        if (current != null) slider.setValue(current.getKm());
        distanceLabel.setText(getString(R.string.distance_label, (int) slider.getValue()));
        slider.addOnChangeListener((s, value, fromUser) ->
                distanceLabel.setText(getString(R.string.distance_label, (int) value)));

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.distance_title)
                .setView(dialogView)
                .setPositiveButton(R.string.apply, (dialog, which) -> {
                    int km = (int) slider.getValue();
                    int place = placeSpinner.getSelectedItemPosition();
                    if (place < PLACE_LATITUDES.length) {
                        // A fixed place
                        String name = placeSpinner.getSelectedItem().toString();
                        viewModel.setDistanceFilter(new DistanceFilter(
                                name, PLACE_LATITUDES[place], PLACE_LONGITUDES[place], km));
                    } else {
                        requestDeviceLocation(km);   // last entry: "My location"
                    }
                })
                .setNeutralButton(R.string.clear, (dialog, which) -> viewModel.setDistanceFilter(null))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // Uses the phone's coarse location, asking for permission first if needed
    private void requestDeviceLocation(int km) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            useDeviceLocation(km);
        } else {
            pendingKm = km;
            locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
    }

    @SuppressLint("MissingPermission")   // only called after the permission check above
    private void useDeviceLocation(int km) {
        LocationManager manager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        Location best = null;
        try {
            for (String provider : manager.getProviders(true)) {
                Location location = manager.getLastKnownLocation(provider);
                if (location != null && (best == null || location.getTime() > best.getTime())) {
                    best = location;
                }
            }
        } catch (SecurityException e) {
            best = null;
        }
        if (best == null) {
            Toast.makeText(requireContext(),
                    "Your location isn't available yet - choose a place from the list instead",
                    Toast.LENGTH_LONG).show();
            return;
        }
        viewModel.setDistanceFilter(new DistanceFilter(
                "your location", best.getLatitude(), best.getLongitude(), km));
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}