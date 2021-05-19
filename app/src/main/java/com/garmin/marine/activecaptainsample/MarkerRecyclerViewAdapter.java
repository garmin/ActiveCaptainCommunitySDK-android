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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.garmin.marine.activecaptaincommunitysdk.DTO.MapIconType;
import com.garmin.marine.activecaptaincommunitysdk.DTO.SearchMarker;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarkerRecyclerViewAdapter extends RecyclerView.Adapter<MarkerRecyclerViewAdapter.ViewHolder> {
    private static final String ICON_BASE_PATH = "acdb/img/map/";

    private static final Map<MapIconType, String> ICON_FILENAMES = new HashMap<MapIconType, String>() {{
        put(MapIconType.UNKNOWN, "H_UX_Icon_Acdb_stacked_points.bmp");
        put(MapIconType.ANCHORAGE, "H_UX_Icon_Acdb_anchorage.bmp");
        put(MapIconType.ANCHORAGE_SPONSOR, "H_UX_Icon_Acdb_anchorage_sponsor.bmp");
        put(MapIconType.BOAT_RAMP, "H_UX_Icon_Acdb_lk_boatramp.bmp");
        put(MapIconType.BRIDGE, "H_UX_Icon_Acdb_lk_bridge.bmp");
        put(MapIconType.BUSINESS, "H_UX_Icon_Acdb_lk_shop.bmp");
        put(MapIconType.BUSINESS_SPONSOR, "H_UX_Icon_Acdb_lk_shop_sponsor.bmp");
        put(MapIconType.DAM, "H_UX_Icon_Acdb_lk_dam.bmp");
        put(MapIconType.FERRY, "H_UX_Icon_Acdb_lk_ferry.bmp");
        put(MapIconType.HAZARD, "H_UX_Icon_Acdb_hazard.bmp");
        put(MapIconType.INLET, "H_UX_Icon_Acdb_lk_inlet.bmp");
        put(MapIconType.LOCK, "H_UX_Icon_Acdb_lk_lock.bmp");
        put(MapIconType.MARINA, "H_UX_Icon_Acdb_marina.bmp");
        put(MapIconType.MARINA_SPONSOR, "H_UX_Icon_Acdb_marina_sponsor.bmp");
    }};

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final TextView textView;

        public ViewHolder(View view) {
            super(view);

            imageView = (ImageView) view.findViewById(R.id.markerIcon);
            textView = (TextView) view.findViewById(R.id.markerName);
        }

        public void bind(final SearchMarker searchMarker, final ItemClickListener<SearchMarker> listener) {
            try {
                InputStream inputStream = imageView.getContext().getAssets().open(ICON_BASE_PATH + ICON_FILENAMES.get(searchMarker.getMapIcon()));
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                imageView.setImageBitmap(bitmap);
            } catch (IOException ignored) {
            }

            textView.setText(searchMarker.getName());

            itemView.setOnClickListener(v -> listener.onItemClicked(searchMarker));
        }
    }

    private final List<SearchMarker> mSearchMarkers = new ArrayList<>();
    private final ItemClickListener<SearchMarker> mClickListener;

    public MarkerRecyclerViewAdapter(ItemClickListener<SearchMarker> clickListener) {
        this.mClickListener = clickListener;
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.markerrecyclerview_row, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        viewHolder.bind(getItem(position), mClickListener);
    }

    public SearchMarker getItem(int position) {
        return mSearchMarkers.get(position);
    }

    @Override
    public int getItemCount() {
        return mSearchMarkers.size();
    }

    public void updateSearchMarkers(SearchMarker[] markers) {
        mSearchMarkers.clear();
        if (markers != null && markers.length > 0) {
            mSearchMarkers.addAll(Arrays.asList(markers));
        }
        notifyDataSetChanged();
    }
}
