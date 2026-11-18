package com.example.brady_jack_s2327712.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.brady_jack_s2327712.data.TrafficRepository;
import com.example.brady_jack_s2327712.model.Situation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TrafficViewModel extends AndroidViewModel {

    private static final String TAG = "TrafficViewModel";
    private static final long REFRESH_INTERVAL_MINUTES = 60;

    public enum TypeFilter { ALL, INCIDENTS, ROADWORKS }

    private final TrafficRepository repository;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private final MutableLiveData<List<Situation>> situations = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> status = new MutableLiveData<>("Loading...");
    private final MutableLiveData<TypeFilter> typeFilter = new MutableLiveData<>(TypeFilter.ALL);
    private final MutableLiveData<Situation> selected = new MutableLiveData<>();

    private final MediatorLiveData<List<Situation>> filteredSituations = new MediatorLiveData<>();

    public TrafficViewModel(@NonNull Application application) {
        super(application);
        repository = new TrafficRepository(application);

        filteredSituations.addSource(situations, list -> applyFilters());
        filteredSituations.addSource(typeFilter, filter -> applyFilters());

        // Auto-update: first load now (delay 0), then repeat every REFRESH_INTERVAL_MINUTES
        executor.scheduleWithFixedDelay(this::loadData, 0, REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }


    public LiveData<List<Situation>> getSituations() { return situations; }
    public LiveData<List<Situation>> getFilteredSituations() { return filteredSituations; }
    public LiveData<Boolean> isLoading() { return loading; }
    public LiveData<String> getStatus() { return status; }
    public LiveData<TypeFilter> getTypeFilter() { return typeFilter; }
    public LiveData<Situation> getSelected() { return selected; }


    public void setTypeFilter(TypeFilter filter) { typeFilter.setValue(filter); }
    public void select(Situation situation) { selected.setValue(situation); }


    public void refresh() {
        executor.execute(this::loadData);
    }

    public void loadExampleFile(int resId, boolean roadworksFeed, String fileName) {
        executor.execute(() -> runLoad(() -> repository.loadExampleFile(resId, roadworksFeed, fileName)));
    }


    private void applyFilters() {
        List<Situation> all = situations.getValue();
        TypeFilter filter = typeFilter.getValue();
        List<Situation> result = new ArrayList<>();
        if (all != null) {
            for (Situation s : all) {
                boolean roadworks = s.getCategory().isRoadworks();
                if (filter == TypeFilter.ALL
                        || (filter == TypeFilter.ROADWORKS && roadworks)
                        || (filter == TypeFilter.INCIDENTS && !roadworks)) {
                    result.add(s);
                }
            }
        }
        filteredSituations.setValue(result);
    }

    private void loadData() {
        runLoad(repository::load);
    }


    private void runLoad(Callable<TrafficRepository.LoadResult> loader) {
        Log.d(TAG, "Loading on thread: " + Thread.currentThread().getName());
        loading.postValue(true);
        try {
            TrafficRepository.LoadResult result = loader.call();
            situations.postValue(result.getSituations());
            status.postValue(result.getSituations().size() + " situations - "
                    + result.getSourceDescription());
        } catch (Exception e) {
            Log.e(TAG, "Load failed", e);
            status.postValue("Could not load data: " + e.getMessage());
        } finally {
            loading.postValue(false);
        }
    }

    @Override
    protected void onCleared() {
        executor.shutdownNow();
        super.onCleared();
    }
}
