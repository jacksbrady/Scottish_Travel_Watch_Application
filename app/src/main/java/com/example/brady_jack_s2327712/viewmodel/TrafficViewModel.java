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
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ViewModel (MVVM): holds the data and the user's choices (filters, search, selection)
 * for all the screens. It survives screen rotation, so nothing is lost or reloaded,
 * and every fragment shares the same instance, so they always agree.
 * <p>
 * Loads data on a background thread at start-up and then every REFRESH_INTERVAL_MINUTES.
 * Results reach the screens through LiveData.postValue().
 */
public class TrafficViewModel extends AndroidViewModel {

    private static final String TAG = "TrafficViewModel";
    private static final long REFRESH_INTERVAL_MINUTES = 60; // auto update period

    public enum TypeFilter {ALL, INCIDENTS, ROADWORKS}

    // "within km of a place"
    // one object, so the place and distance always match
    public static class DistanceFilter {
        private final String placeName;
        private final double latitude;
        private final double longitude;
        private final int km;

        public DistanceFilter(String placeName, double latitude, double longitude, int km) {
            this.placeName = placeName;
            this.latitude = latitude;
            this.longitude = longitude;
            this.km = km;
        }

        public String getPlaceName() {
            return placeName;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public int getKm() {
            return km;
        }

        // Distance from this filter's place to the situation in km.
        public double distanceTo(Situation situation) {
            return situation.distanceKmFrom(latitude, longitude);
        }
    }

    private final TrafficRepository repository;

    // One background thread
    // Loads queue in order so two can't overlap
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    // LiveData notifies the screen when value changes
    // Return read-only LiveData so only this class can change it
    private final MutableLiveData<List<Situation>> situations = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> status = new MutableLiveData<>("Loading...");
    private final MutableLiveData<TypeFilter> typeFilter = new MutableLiveData<>(TypeFilter.ALL);
    private final MutableLiveData<Situation> selected = new MutableLiveData<>();
    private final MutableLiveData<String> roadQuery = new MutableLiveData<>("");
    private final MutableLiveData<DistanceFilter> distanceFilter = new MutableLiveData<>(null);

    private final MediatorLiveData<List<Situation>> filteredSituations = new MediatorLiveData<>();
    private final MediatorLiveData<List<String>> roadNames = new MediatorLiveData<>();

    // Set by "Show on map", read once by the map so it zooms to the selected situation
    private boolean mapFocusRequested = false;

    public TrafficViewModel(@NonNull Application application) {
        super(application);
        repository = new TrafficRepository(application);

        filteredSituations.addSource(situations, list -> applyFilters());
        filteredSituations.addSource(typeFilter, filter -> applyFilters());
        filteredSituations.addSource(roadQuery, query -> applyFilters());
        filteredSituations.addSource(distanceFilter, filter -> applyFilters());

        // List of road names for the search suggestions, rebuilt when the data changes
        roadNames.addSource(situations, this::buildRoadNames);

        // Auto updates, first immediately, then every REFRESH_INTERVAL_MINUTES (set to 60 mins)
        executor.scheduleWithFixedDelay(this::loadData, 0, REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }


    public LiveData<List<Situation>> getFilteredSituations() {
        return filteredSituations;
    }

    public LiveData<Boolean> isLoading() {
        return loading;
    }

    public LiveData<String> getStatus() {
        return status;
    }

    public LiveData<TypeFilter> getTypeFilter() {
        return typeFilter;
    }

    public LiveData<Situation> getSelected() {
        return selected;
    }

    public LiveData<String> getRoadQuery() {
        return roadQuery;
    }

    public LiveData<DistanceFilter> getDistanceFilter() {
        return distanceFilter;
    }

    public LiveData<List<String>> getRoadNames() {
        return roadNames;
    }


    public void setTypeFilter(TypeFilter filter) {
        typeFilter.setValue(filter);
    }

    public void select(Situation situation) {
        selected.setValue(situation);
    }

    public void setRoadQuery(String query) {
        roadQuery.setValue(query);
    }

    public void setDistanceFilter(DistanceFilter filter) {
        distanceFilter.setValue(filter);
    } // null = off

    public void requestMapFocus() {
        mapFocusRequested = true;
    }

    // true once after requestMapFocus(), then false again
    public boolean consumeMapFocus() {
        boolean requested = mapFocusRequested;
        mapFocusRequested = false;
        return requested;
    }

    // Queue a load on the background thread and return immediately
    public void refresh() {
        executor.execute(this::loadData);
    }

    public void loadExampleFile(int resId, boolean roadworksFeed, String fileName) {
        executor.execute(() -> runLoad(() -> repository.loadExampleFile(resId, roadworksFeed, fileName)));
    }


    // Keeps situations fitting all three filters, nearest first if distance is used
    private void applyFilters() {
        List<Situation> all = situations.getValue();
        TypeFilter filter = typeFilter.getValue();
        String query = roadQuery.getValue();
        DistanceFilter near = distanceFilter.getValue();
        List<Situation> result = new ArrayList<>();
        if (all != null) {
            for (Situation s : all) {
                boolean roadworks = s.getCategory().isRoadworks();
                boolean typeOk = filter == TypeFilter.ALL
                        || (filter == TypeFilter.ROADWORKS && roadworks)
                        || (filter == TypeFilter.INCIDENTS && !roadworks);
                boolean roadOk = s.isOnRoad(query);
                boolean distanceOk = near == null || near.distanceTo(s) <= near.getKm();
                if (typeOk && roadOk && distanceOk) {
                    result.add(s);
                }
            }
        }
        if (near != null) {
            result.sort(Comparator.comparingDouble(near::distanceTo));
        }
        filteredSituations.setValue(result);
    }

    // Every distinct road name, sorted (TreeSet removes duplicates and sorts)
    private void buildRoadNames(List<Situation> list) {
        TreeSet<String> names = new TreeSet<>();
        if (list != null) {
            for (Situation s : list) {
                if (s.getRoadName() != null) names.add(s.getRoadName());
            }
        }
        roadNames.setValue(new ArrayList<>(names));
    }

    private void loadData() {
        runLoad(repository::load);
    }


    // Shared by every load so error handling and threading is not repeated
    // The background thread's postValue() sends results to UI thread
    // In case of failure, old data remains but an error report is sent
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