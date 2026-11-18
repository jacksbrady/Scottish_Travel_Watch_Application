package com.example.brady_jack_s2327712.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.example.brady_jack_s2327712.R;
import com.example.brady_jack_s2327712.model.Situation;
import com.example.brady_jack_s2327712.viewmodel.TrafficViewModel;
import com.example.brady_jack_s2327712.viewmodel.TrafficViewModel.TypeFilter;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;


public class SituationListFragment extends Fragment {

    private TrafficViewModel viewModel;
    private SituationAdapter adapter;

    public SituationListFragment() { }

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

        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);


        adapter = new SituationAdapter(requireContext());
        listView.setAdapter(adapter);
        listView.setEmptyView(emptyText);
        listView.setOnItemClickListener((parent, itemView, position, id) -> {
            Situation situation = (Situation) parent.getItemAtPosition(position);
            viewModel.select(situation);

            Toast.makeText(requireContext(), "Selected: " + situation.getCategory().getLabel()
                    + " on " + situation.getRoadName(), Toast.LENGTH_SHORT).show();
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

        LifecycleOwner owner = getViewLifecycleOwner();
        viewModel.getFilteredSituations().observe(owner, adapter::setSituations);
        viewModel.getStatus().observe(owner, statusText::setText);
        viewModel.isLoading().observe(owner, loading ->
                progress.setVisibility(loading ? View.VISIBLE : View.GONE));
    }
}