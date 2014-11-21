package janis.opennumismat;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
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
        if (grading.count > 0) {
            count.setText(grading.getCount());
            count.setVisibility(View.VISIBLE);

            GradientDrawable back = (GradientDrawable) count.getBackground();
            if (grading.grade.equals("Unc"))
                back.setColor(getContext().getResources().getColor(R.color.unc));
            else if (grading.grade.equals("AU"))
                back.setColor(getContext().getResources().getColor(R.color.au));
            else if (grading.grade.equals("VF"))
                back.setColor(getContext().getResources().getColor(R.color.vf));
            else if (grading.grade.equals("F"))
                back.setColor(getContext().getResources().getColor(R.color.f));
            else
                back.setColor(getContext().getResources().getColor(R.color.xf));
        } else {
            if (!pref.getBoolean("show_zero", true))
                count.setVisibility(View.GONE);
            else {
                count.setText("+");
                count.setVisibility(View.VISIBLE);

                GradientDrawable back = (GradientDrawable) count.getBackground();
                back.setColor(getContext().getResources().getColor(R.color.not_present));
            }
        }

        // Return the completed view to render on screen
        return convertView;
    }
}
