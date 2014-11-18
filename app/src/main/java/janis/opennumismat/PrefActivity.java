package janis.opennumismat;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TextView;

/**
 * Created by v.ignatov on 17.10.2014.
 */
public class PrefActivity extends PreferenceActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {
    SettingsFr fragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            onCreatePreferenceActivity();
        } else {
            onCreatePreferenceFragment();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        if (key.equals("sort_order") || key.equals("density")) {
            Preference pref = getPreference(key);
            ListPreference listPref = (ListPreference) pref;
            pref.setSummary(listPref.getEntry());
        }
    }

    private Preference getPreference(String key) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            return findPreference(key);
        } else {
            return fragment.findPreference(key);
        }
    }

    /**
     * code for Android < 3 (i.e. API lvl < 11)
     */
    @SuppressWarnings("deprecation")
    private void onCreatePreferenceActivity() {
        addPreferencesFromResource(R.layout.activity_pref);
    }

    /**
     * code for Android >= 3 (i.e. API lvl >= 11)
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void onCreatePreferenceFragment() {
        fragment = new SettingsFr();
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SettingsFr extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.layout.activity_pref);
        }
    }
}
