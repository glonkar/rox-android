package com.grayfox.android.fragment;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import com.grayfox.android.R;
import com.grayfox.android.client.RecommenderApi;
import com.grayfox.android.client.model.Location;
import com.grayfox.android.client.model.Poi;
import com.grayfox.android.client.model.Recommendation;
import com.grayfox.android.client.task.RecommendedSearchAsyncTask;

import com.shamanland.fab.FloatingActionButton;

import java.lang.ref.WeakReference;

import roboguice.fragment.RoboFragment;
import roboguice.inject.InjectView;

public class ExploreFragment extends RoboFragment {

    @InjectView(R.id.search_button) private FloatingActionButton searchButton;

    private GoogleMap map;
    private SupportMapFragment mapFragment;
    private ProgressDialog searchProgressDialog;
    private SearchTask searchTask;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_explore, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mapFragment = SupportMapFragment.newInstance();
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.map_content, mapFragment)
                .commit();
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSearch();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        setupMapIfNeeded();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (searchProgressDialog != null && searchProgressDialog.isShowing()) {
            searchTask.cancel(true);
            searchProgressDialog.dismiss();
        }
    }

    private void setupMapIfNeeded() {
        if (map == null) {
            map = mapFragment.getMap();
            if (map != null) {
                map.setMyLocationEnabled(true);
            }
        }
    }

    private void onSearch() {
        Location location = new Location();
        location.setLatitude(map.getCameraPosition().target.latitude);
        location.setLongitude(map.getCameraPosition().target.longitude);
        searchTask = new SearchTask(this);
        searchTask.radius(3000).category("shops")
                .transportation(RecommenderApi.Transportation.DRIVING)
                .location(location)
                .request();
    }

    private void onPreSearch() {
        searchProgressDialog = ProgressDialog.show(getActivity(), null, getString(R.string.search_in_progress), true, false);
    }

    private void onRecommendationAcquired(Recommendation recommendation) {
        map.clear();
        Log.d("TAG", recommendation.toString());
        if (!recommendation.getPois().isEmpty()) {
            for (Poi poi : recommendation.getPois()) {
                map.addMarker(new MarkerOptions()
                        .position(new LatLng(poi.getLocation().getLatitude(), poi.getLocation().getLongitude()))
                        .title(poi.getName()));
            }
            map.animateCamera(CameraUpdateFactory.newLatLng(
                    new LatLng(recommendation.getPois().get(0).getLocation().getLatitude(), recommendation.getPois().get(0).getLocation().getLongitude())));
        }

        if (!recommendation.getRoutePoints().isEmpty()) {
            PolylineOptions pathOptions = new PolylineOptions().color(Color.RED);
            for (Location point : recommendation.getRoutePoints()) {
                pathOptions.add(new LatLng(point.getLatitude(), point.getLongitude()));
            }
            map.addPolyline(pathOptions);
        }
    }

    private void onSearchFinally() {
        searchProgressDialog.dismiss();
    }

    private static class SearchTask extends RecommendedSearchAsyncTask {

        private WeakReference<ExploreFragment> reference;

        private SearchTask(ExploreFragment fragment) {
            super(fragment.getActivity().getApplicationContext());
            reference = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() throws Exception {
            ExploreFragment fragment = reference.get();
            if (fragment != null) fragment.onPreSearch();
        }

        @Override
        protected void onSuccess(Recommendation recommendation) throws Exception {
            ExploreFragment fragment = reference.get();
            if (fragment != null) fragment.onRecommendationAcquired(recommendation);
        }

        @Override
        protected void onFinally() throws RuntimeException {
            ExploreFragment fragment = reference.get();
            if (fragment != null) fragment.onSearchFinally();
        }
    }
}