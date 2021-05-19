/*------------------------------------------------------------------------------
Copyright 2021 Garmin Ltd. or its subsidiaries.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
------------------------------------------------------------------------------*/

package com.garmin.marine.activecaptainsample;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.garmin.marine.activecaptaincommunitysdk.DTO.SearchMarker;

public class SearchResultsActivity extends AppCompatActivity implements ItemClickListener<SearchMarker> {
    public static final String MARKER_ID = "com.garmin.marine.activecaptainsample.MARKER_ID";

    private MarkerRecyclerViewAdapter markerRecyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);
        RecyclerView recyclerView = findViewById(R.id.rv_markers);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        markerRecyclerViewAdapter = new MarkerRecyclerViewAdapter(this);
        recyclerView.setAdapter(markerRecyclerViewAdapter);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);

            SearchMarker[] searchMarkers = ActiveCaptainManager.getInstance().getDatabase().getSearchMarkers(query, -90, -180, 90, 180, ActiveCaptainConfiguration.MARKER_MAX_SEARCH_RESULTS);
            markerRecyclerViewAdapter.updateSearchMarkers(searchMarkers);
        }
    }

    @Override
    public void onItemClicked(SearchMarker searchMarker) {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra(MARKER_ID, searchMarker.getId());
        startActivity(intent);
    }
}