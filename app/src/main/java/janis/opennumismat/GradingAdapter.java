package janis.opennumismat;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by v.ignatov on 20.11.2014.
 */
public class GradingAdapter extends ArrayAdapter<Grading> {
    private SharedPreferences pref;

    public GradingAdapter(Context context, ArrayList<Grading> gradings) {
        super(context, 0, gradings);
        pref = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Grading grading = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.grading_item, parent, false);
        }

        // Lookup view for data population
        TextView title = (TextView) convertView.findViewById(R.id.title);
        TextView desc = (TextView) convertView.findViewById(R.id.description);
        // Populate the data into the template view using the data object
        title.setText(grading.title);
        desc.setText(grading.desc);

        TextView count = (TextView) convertView.findViewById(R.id.count);
        count.setText(grading.getCount());

        if (grading.count > 0) {
            count.setBackgroundResource(R.drawable.count_box);
            count.setVisibility(View.VISIBLE);
        } else {
            if (!pref.getBoolean("show_zero", true))
                count.setVisibility(View.GONE);
            else {
                count.setBackgroundResource(R.drawable.zero_count_box);
                count.setVisibility(View.VISIBLE);
            }
        }

        // Return the completed view to render on screen
        return convertView;
    }
}
