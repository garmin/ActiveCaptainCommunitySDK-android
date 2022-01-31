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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.SearchView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.garmin.marine.activecaptaincommunitysdk.DTO.SearchMarker;

public class SearchActivity extends AppCompatActivity implements ItemClickListener<SearchMarker> {
    public static final String MARKER_ID = "com.garmin.marine.activecaptainsample.MARKER_ID";

    private SearchView searchView;
    private MarkerRecyclerViewAdapter markerRecyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        searchView = findViewById(R.id.search_marker_name);
        searchView.setIconified(false);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                if (query.length() >= ActiveCaptainConfiguration.MARKER_MIN_SEARCH_LENGTH)
                {
                    SearchMarker[] searchMarkers = ActiveCaptainManager.getInstance().getDatabase().getSearchMarkers(query, -90, -180, 90, 180, ActiveCaptainConfiguration.MARKER_MAX_SEARCH_RESULTS, false);
                    markerRecyclerViewAdapter.updateSearchMarkers(searchMarkers);
                }

                return true;
            }
        });

        RecyclerView recyclerView = findViewById(R.id.rv_markers);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        markerRecyclerViewAdapter = new MarkerRecyclerViewAdapter(this);
        recyclerView.setAdapter(markerRecyclerViewAdapter);
    }

    @Override
    public void onItemClicked(SearchMarker searchMarker) {
        Intent intent = getIntent();
        intent.putExtra(MARKER_ID, searchMarker.getId());
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}
