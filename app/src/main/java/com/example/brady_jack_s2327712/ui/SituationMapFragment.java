package com.example.brady_jack_s2327712.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
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
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * All situations are shown on the map with lines connecting their from and to points.
 * All situations are clickable to receive further details
 */
public class SituationMapFragment extends Fragment implements OnMapReadyCallback {

    private static final LatLng SCOTLAND = new LatLng(56.8, -4.2);
    private static final float MAX_ZOOM = 14f;

    private TrafficViewModel viewModel;
    private GoogleMap map;
    private TextView mapInfo;
    private List<Situation> shown = new ArrayList<>();
    private boolean focusSelected;   // true when opened from "Show on map"
    private final Map<SituationCategory, BitmapDescriptor> markerIcons = new EnumMap<>(SituationCategory.class);

    public SituationMapFragment() {
        super(R.layout.fragment_situation_map);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mapInfo = view.findViewById(R.id.mapInfo);

        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);
        focusSelected = viewModel.consumeMapFocus();

        // The map itself is a SupportMapFragment inside this fragment's layout
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapView);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);   // onMapReady() is called when the map is ready
        }

        // Redraw whenever the list's search/filters (or the data) change
        viewModel.getFilteredSituations().observe(getViewLifecycleOwner(), list -> {
            shown = list;
            drawSituations();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(SCOTLAND, 6));

        // Tap a marker's info window > select it and open the detail screen
        map.setOnInfoWindowClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof Situation) {
                viewModel.select((Situation) tag);
                if (requireActivity() instanceof SituationListFragment.OnSituationSelectedListener) {
                    ((SituationListFragment.OnSituationSelectedListener) requireActivity()).onSituationSelected();
                }
            }
        });

        drawSituations();
    }

    // Draws every shown situation with a line connecting the from and to points
    private void drawSituations() {
        if (map == null) return;   // map not ready yet, onMapReady() will call this again
        map.clear();

        Situation selected = viewModel.getSelected().getValue();
        List<Situation> toDraw = new ArrayList<>(shown);
        if (focusSelected && selected != null && !toDraw.contains(selected)) {
            toDraw.add(selected);   // show it even if the current filters hide it
        }

        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        int boundsPoints = 0;

        for (Situation situation : toDraw) {
            List<GeoPoint> points = situation.getPoints();   // only points with coordinates (0, 1 or 2)
            if (points.isEmpty()) continue;

            SituationCategory category = situation.getCategory();
            int colour = ContextCompat.getColor(requireContext(), category.getColourRes());
            boolean isSelected = situation == selected;

            // Line between the two points
            if (points.size() == 2) {
                map.addPolyline(new PolylineOptions()
                        .add(toLatLng(points.get(0)), toLatLng(points.get(1)))
                        .color(colour)
                        .width(isSelected && focusSelected ? 18f : 10f));
            }

            // Marker in the middle of the situation, with its category icon
            String road = situation.getRoadName() != null ? situation.getRoadName() : "Unknown road";
            Marker marker = map.addMarker(new MarkerOptions()
                    .position(middleOf(points))
                    .title(category.getLabel() + " - " + road)
                    .snippet(situation.getImpactSummary() + " (tap for details)")
                    .icon(markerIcon(category))
                    .anchor(0.5f, 0.5f));
            if (marker != null) {
                marker.setTag(situation);   // so a tap knows which situation it is
                if (isSelected && focusSelected) marker.showInfoWindow();
            }

            if (!focusSelected || isSelected) {
                for (GeoPoint p : points) {
                    bounds.include(toLatLng(p));
                    boundsPoints++;
                }
            }
        }

        mapInfo.setText(getString(R.string.map_info, shown.size()));
        moveCamera(bounds, boundsPoints);
        focusSelected = false;   // only zoom to the selected situation the first time
    }

    // Zooms the map so all the included points fit on screen
    private void moveCamera(LatLngBounds.Builder bounds, int pointCount) {
        if (pointCount == 0) return;
        View view = getView();
        int width = view != null ? view.getWidth() : 0;
        int height = view != null ? view.getHeight() : 0;
        LatLngBounds area = bounds.build();
        if (width == 0 || height == 0) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(area.getCenter(), 8));
            return;
        }
        int padding = (int) (Math.min(width, height) * 0.12);
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(area, width, height, padding));
        if (map.getCameraPosition().zoom > MAX_ZOOM) {
            map.moveCamera(CameraUpdateFactory.zoomTo(MAX_ZOOM));   // don't zoom in too far on one point
        }
    }

    private BitmapDescriptor markerIcon(SituationCategory category) {
        BitmapDescriptor cached = markerIcons.get(category);
        if (cached != null) return cached;

        int size = (int) (32 * getResources().getDisplayMetrics().density);   // 32dp in pixels
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint circle = new Paint(Paint.ANTI_ALIAS_FLAG);
        circle.setColor(Color.WHITE);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, circle);

        Drawable icon = ContextCompat.getDrawable(requireContext(), category.getIconRes());
        if (icon != null) {
            icon = icon.mutate();
            icon.setTint(ContextCompat.getColor(requireContext(), category.getColourRes()));
            int inset = size / 6;
            icon.setBounds(inset, inset, size - inset, size - inset);
            icon.draw(canvas);
        }

        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
        markerIcons.put(category, descriptor);
        return descriptor;
    }

    private static LatLng toLatLng(GeoPoint point) {
        return new LatLng(point.getLatitude(), point.getLongitude());
    }

    private static LatLng middleOf(List<GeoPoint> points) {
        double lat = 0, lon = 0;
        for (GeoPoint p : points) {
            lat += p.getLatitude();
            lon += p.getLongitude();
        }
        return new LatLng(lat / points.size(), lon / points.size());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        map = null;   // the map belongs to this view; drop it when the view goes
    }
}