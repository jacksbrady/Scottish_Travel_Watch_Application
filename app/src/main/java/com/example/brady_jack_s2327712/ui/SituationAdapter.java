package com.example.brady_jack_s2327712.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.brady_jack_s2327712.R;
import com.example.brady_jack_s2327712.model.Situation;
import com.example.brady_jack_s2327712.model.SituationCategory;
import com.example.brady_jack_s2327712.viewmodel.TrafficViewModel.DistanceFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds each row of the situation list from item_situation.xml: a colour strip and
 * icon for the category, the type and road, the description, and the impact and start date
 * plus the distance when a distance search is on
 * <p>
 * Rows are recycled as the list scrolls, with a ViewHolder so their views are only looked up once.
 */
public class SituationAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private List<Situation> situations = new ArrayList<>();
    private DistanceFilter distanceFilter;

    public SituationAdapter(Context context) {
        inflater = LayoutInflater.from(context);
    }

    // Stores new data from the ViewModel and tells the listView to redraw
    public void setSituations(List<Situation> newSituations) {
        situations = newSituations;
        notifyDataSetChanged();
    }

    // When a distance search is on, each row also shows how far away it si
    public void setDistanceFilter(DistanceFilter filter) {
        distanceFilter = filter;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return situations.size();
    }

    @Override
    public Object getItem(int position) {
        return situations.get(position);
    } // Used by the click listener (getItemAtPosition) to find the selected situation

    @Override
    public long getItemId(int position) {
        return position;
    }

    // Build a recyclable row so only about a screen-full of rows are created at one time
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {

            convertView = inflater.inflate(R.layout.item_situation, parent, false);
            holder = new ViewHolder();
            holder.colourStrip = convertView.findViewById(R.id.colourStrip);
            holder.icon = convertView.findViewById(R.id.icon);
            holder.title = convertView.findViewById(R.id.title);
            holder.comment = convertView.findViewById(R.id.comment);
            holder.details = convertView.findViewById(R.id.details);
            convertView.setTag(holder);
        } else {

            holder = (ViewHolder) convertView.getTag();
        }

        // Fill in every field including refilling the recycled rows so data is not from the previous row
        Situation situation = situations.get(position);
        SituationCategory category = situation.getCategory();
        int colour = ContextCompat.getColor(convertView.getContext(), category.getColourRes());

        holder.colourStrip.setBackgroundColor(colour);
        holder.icon.setImageResource(category.getIconRes());
        holder.icon.setColorFilter(colour);
        holder.icon.setContentDescription(category.getLabel());

        String road = situation.getRoadName() != null ? situation.getRoadName() : "Unknown road";
        holder.title.setText(category.getLabel() + " - " + road);
        holder.comment.setText(situation.getComment());

        // Add the distance when a distance search is active
        String details = situation.getImpactSummary() + "  |  from " + situation.getStartText();
        if (distanceFilter != null) {
            details = String.format(Locale.UK, "%.1f km away  |  ", distanceFilter.distanceTo(situation)) + details;
        }
        holder.details.setText(details);

        return convertView;
    }

    // Holds one row's views so findViewById does not need to be repeated
    static class ViewHolder {
        View colourStrip;
        ImageView icon;
        TextView title;
        TextView comment;
        TextView details;
    }
}