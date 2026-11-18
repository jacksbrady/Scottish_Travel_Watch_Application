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

import java.util.ArrayList;
import java.util.List;

public class SituationAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private List<Situation> situations = new ArrayList<>();

    public SituationAdapter(Context context) {
        inflater = LayoutInflater.from(context);
    }

    public void setSituations(List<Situation> newSituations) {
        situations = newSituations;
        notifyDataSetChanged();
    }

    @Override public int getCount() { return situations.size(); }
    @Override public Object getItem(int position) { return situations.get(position); }
    @Override public long getItemId(int position) { return position; }

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
        holder.details.setText(situation.getImpactSummary() + "  |  from " + situation.getStartText());

        return convertView;
    }

    static class ViewHolder {
        View colourStrip;
        ImageView icon;
        TextView title;
        TextView comment;
        TextView details;
    }
}