package com.example.brady_jack_s2327712.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.brady_jack_s2327712.data.TrafficRepository;
import com.example.brady_jack_s2327712.model.Situation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ViewModel (MVVM): holds the data the screens show, and survives screen rotation.
 *  - Loads the data on a worker thread (ScheduledExecutorService), never on the UI thread.
 *  - Loads once at start-up, then automatically every REFRESH_INTERVAL_MINUTES.
 *  - Results reach the UI through LiveData.postValue(), which queues them on the
 *    main thread's message queue - replacing the starter code's runOnUiThread().
 */
public class TrafficViewModel extends AndroidViewModel {

    private static final String TAG = "TrafficViewModel";

    // Every hour, as the brief asks. Set to 1 for the video demo, then back to 60.
    private static final long REFRESH_INTERVAL_MINUTES = 60;

    private final TrafficRepository repository;

    // One worker thread: loads queue up one after another, so two can never overlap
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    // Data the View observes. Mutable inside the ViewModel, read-only (LiveData) outside.
    private final MutableLiveData<List<Situation>> situations = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> status = new MutableLiveData<>("Loading...");

    public TrafficViewModel(@NonNull Application application) {
        super(application);
        repository = new TrafficRepository(application);

        // Auto-update: first load now (delay 0), then repeat every REFRESH_INTERVAL_MINUTES
        executor.scheduleWithFixedDelay(this::loadData, 0, REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    public LiveData<List<Situation>> getSituations() { return situations; }
    public LiveData<Boolean> isLoading() { return loading; }
    public LiveData<String> getStatus() { return status; }

    // Manual refresh
    public void refresh() {
        executor.execute(this::loadData);
    }

    // loading example files for teesting
    public void loadExampleFile(int resId, boolean roadworksFeed, String fileName) {
        executor.execute(() -> runLoad(() -> repository.loadExampleFile(resId, roadworksFeed, fileName)));
    }

    // In use load
    private void loadData() {
        runLoad(repository::load);
    }

    // Shared load function either by refresh button, four test buttons or live data load
    private void runLoad(Callable<TrafficRepository.LoadResult> loader) {
        Log.d(TAG, "Loading on thread: " + Thread.currentThread().getName());
        loading.postValue(true);
        try {
            TrafficRepository.LoadResult result = loader.call();
            situations.postValue(result.getSituations());   // handed over to the UI thread
            status.postValue(result.getSituations().size() + " situations - "
                    + result.getSourceDescription());
        } catch (Exception e) {
            Log.e(TAG, "Load failed", e);
            // Keep showing whatever was already displayed; just report the problem
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