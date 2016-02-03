package org.opennumismat.uscommemoratives;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    public static final String PREF_LAST_PATH = "last_path";
    private static final String AD_UNIT_ID = "ca-app-pub-2145913411061844/2399725611";

    public static final String UPDATE_URL = "https://raw.githubusercontent.com/OpenNumismat/catalogues-pro/master/update_dummy.json";

    public final static String EXTRA_COIN_ID = "org.janis.opennumismat.COIN_ID";
    public final static String EXTRA_COIN_IMAGE = "org.janis.opennumismat.COIN_IMAGE";

    private DrawerLayout drawerLayout;
    private ListView listView;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private Toolbar toolbar;
    private CharSequence title;
    private InterstitialAd mInterstitialAd;

    private String[] navigationDrawerItems;

    private SharedPreferences pref;
    SharedPreferences.OnSharedPreferenceChangeListener prefListener;
    private SqlAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        navigationDrawerItems = getResources().getStringArray(R.array.navigation_drawer_items);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        listView = (ListView) findViewById(R.id.left_drawer);

        // set a custom shadow that overlays the main content when the drawer opens
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener
        listView.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_list_item, navigationDrawerItems));
        listView.setOnItemClickListener(new DrawerItemClickListener());

        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.app_name, R.string.app_name) {
            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                setTitle(title);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                setTitle(R.string.app_name);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        drawerLayout.setDrawerListener(actionBarDrawerToggle);

        // enable ActionBar app icon to behave as action to toggle nav drawer
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        if (savedInstanceState == null) {
            selectItem(0);
        }

        // Set default density
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (pref.getBoolean("auto_update", false))
            startService(new Intent(this, UpdateService.class));

        // Load latest collection
        String path = pref.getString(PREF_LAST_PATH, "");
        if (!path.isEmpty()) {
            openFile(path, true);
        }
        else {
            startDownloadDialog();
        }

        prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs,
                                                  String key) {
                if (adapter != null) {
                    if (key.equals("sort_order") || key.equals("use_mint")) {
                        adapter.refresh();
                    } else if (key.equals("use_grading")) {
                        invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                    } else if (key.equals("filter_field")) {
                        adapter.setFilterField(prefs.getString(key, SqlAdapter.DEFAULT_FILTER));
                        adapter.refresh();

                        if (listView.isItemChecked(0)) {
                            title = adapter.getFilter() + " ▼";
                            setTitle(title);
                        }
                        else {
                            FragmentManager fragmentManager = getFragmentManager();
                            Fragment fragment = fragmentManager.findFragmentById(R.id.content_frame);
                            ((StatisticsFragment)fragment).refreshAdapter();
                        }
                    }
                }
            }
        };
        pref.registerOnSharedPreferenceChangeListener(prefListener);

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(AD_UNIT_ID);
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                requestNewInterstitial();
                actionAfterAd();
            }
        });
        requestNewInterstitial();
    }

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("7159446E541B66B1921DEA6BFE17219E")
                .build();

        mInterstitialAd.loadAd(adRequest);
    }

    private void actionAfterAd() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!drawerLayout.isDrawerOpen(findViewById(R.id.left_drawer))) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.main, menu);
            return true;
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        MenuItem item = menu.findItem(R.id.action_update);
        if (item != null)
            item.setVisible(adapter != null);

        item = menu.findItem(R.id.action_filter);
        if (item != null) {
            item.setVisible(adapter != null);
            if (adapter != null) {
                item.setVisible(listView.getCheckedItemPositions().get(0, false));
            }
        }

        item = menu.findItem(R.id.filter_not_unc);
        if (item != null)
            item.setVisible(pref.getBoolean("use_grading", false));

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_update:
                if (checkConnection())
                    new DownloadListTask().execute(UPDATE_URL);
                return true;

            case R.id.action_settings:
                if (mInterstitialAd.isLoaded())
                    mInterstitialAd.show();
                else
                    actionAfterAd();
                return true;

            case R.id.action_about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;

            case R.id.filter_all:
            case R.id.filter_present:
            case R.id.filter_need:
            case R.id.filter_for_change:
            case R.id.filter_not_unc:
                adapter.setMainFilter(id);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void downloadUpdate(DownloadEntry entry) {
        if (pref.getBoolean("use_external", false)) {
            entry.setFile(new File(getExternalCacheDir(), entry.file_name()));
        }
        else {
            entry.setFile(new File(getCacheDir(), entry.file_name()));
        }

        new DownloadUpdateTask(this, entry).execute();
    }

    private JSONObject loadJSONFromAsset() {
        JSONObject json;

        try {
            InputStream is = getAssets().open("list.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String str = new String(buffer, "UTF-8");
            json = new JSONObject(str);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }

        return json;
    }

    private void startDownloadDialog() {
        checkConnection();

        int selection;
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        if (metrics.densityDpi <= DisplayMetrics.DENSITY_MEDIUM) {
            selection = 0;
        }
        else if (metrics.densityDpi <= DisplayMetrics.DENSITY_HIGH) {
            selection = 1;
        }
        else if (metrics.densityDpi <= DisplayMetrics.DENSITY_XHIGH) {
            selection = 2;
        }
        else {
            selection = 3;
        }

        JSONObject json = loadJSONFromAsset();
        List<DownloadEntry> entries_arr = new ArrayList<>();
        try {
            JSONArray cats = json.getJSONArray("catalogues");
            JSONObject cat = cats.getJSONObject(0);
            String[] densities = {"MDPI", "HDPI", "XHDPI", "XXHDPI"};
            for (String density: densities) {
                entries_arr.add(
                        new DownloadEntry(cat.getString("title"),
                            cat.getString("date"), cat.getJSONObject("size").getString(density),
                            cat.getString("file"), cat.getJSONObject("url").getString(density))
                );
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final DownloadEntry[] entries = {entries_arr.get(0), entries_arr.get(1), entries_arr.get(2),
                entries_arr.get(3)};
        DownloadEntry entry = entries[selection];

        View view = View.inflate(this, R.layout.download_dialog, null);
        final CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkbox);
        final Spinner spinner = (Spinner) view.findViewById(R.id.spinner);
        spinner.setSelection(selection);

        AlertDialog.Builder ad = new AlertDialog.Builder(this);
        ad.setTitle(R.string.download);
        ad.setView(view);
        ad.setMessage(getString(R.string.download_ext, entry.title(), entry.size()));
        ad.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                int pos = spinner.getSelectedItemPosition();

                SharedPreferences.Editor ed = pref.edit();
                ed.putBoolean("use_external", checkBox.isChecked());
                String[] densities = getResources().getStringArray(R.array.densities);
                ed.putString("density", densities[pos]);
                ed.apply();

                downloadCatalog(entries[pos]);
            }
        });
        ad.setCancelable(false);
        final AlertDialog dialog = ad.show();

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                DownloadEntry entry = entries[position];
                dialog.setMessage(getString(R.string.download_ext, entry.title(), entry.size()));
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }

    private void downloadCatalog(DownloadEntry entry) {
        File targetDirectory = getFilesDir();

        if (pref.getBoolean("use_external", false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                File[] externalFilesDirs = getExternalFilesDirs(null);
                for (File f : externalFilesDirs) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        targetDirectory = f;
                        if (!Environment.isExternalStorageEmulated(f)) {
                            break;
                        }
                    } else
                        targetDirectory = f;
                }
            } else {
                targetDirectory = getExternalFilesDir(null);
            }
        }

        entry.setFile(new File(targetDirectory, entry.file_name()));

        new DownloadCatalogTask(this, entry).execute();
    }

    /* The click listener for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void selectItem(int position) {
        Fragment fragment;
        Bundle args = new Bundle();
        TextView text = (TextView) findViewById(R.id.toolbar_title);

        switch (position) {
            case 0:
                fragment = new MainFragment();
                ((MainFragment)fragment).setAdapter(adapter);
                if (adapter != null) {
                    text.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            openContextMenu(v);
                        }
                    });
                    title = adapter.getFilter() + " ▼";
                    setTitle(title);
                }
                else {
                    text.setOnClickListener(null);
                    setTitle(R.string.app_name);
                }
                break;

            case 1:
                fragment = new StatisticsFragment();
                ((StatisticsFragment)fragment).setAdapter(adapter);
                if (adapter != null) {
                    title = navigationDrawerItems[position];
                    setTitle(title);
                }
                else {
                    setTitle(R.string.app_name);
                }
                text.setOnClickListener(null);
                break;

            default:
                return;
        }

        // update the main content by replacing fragments
        fragment.setArguments(args);
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

        // update selected item and title, then close the drawer
        listView.setItemChecked(position, true);
        drawerLayout.closeDrawer(listView);
    }

    @Override
    public void setTitle(CharSequence title) {
//        getSupportActionBar().setTitle(title);
        TextView text = (TextView) findViewById(R.id.toolbar_title);
        text.setText(title);
        registerForContextMenu(text);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        switch (v.getId()) {
            case R.id.toolbar_title:
                List<String> list = adapter.getFilters();
                for (int i = 0; i < list.size(); i++) {
                    menu.add(0, i, 0, list.get(i));
                }
                break;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        String filter = adapter.getFilters().get(item.getItemId());
        adapter.setFilter(filter);
        title = adapter.getFilter() + " ▼";
        setTitle(title);
        return super.onContextItemSelected(item);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        actionBarDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        actionBarDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onDestroy() {
        if (adapter != null) {
            adapter.close();
            adapter = null;
        }

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    private void openFile(String path, boolean first) {
        try {
            if (adapter != null) {
                adapter.close();
                adapter = null;
            }

            adapter = new SqlAdapter(this, path);
        } catch (SQLiteException e) {
            e.printStackTrace();
            Toast toast = Toast.makeText(
                    getApplicationContext(), getString(R.string.could_not_open_database) + '\n' + path, Toast.LENGTH_LONG
            );
            toast.show();
        }

        selectItem(0);

        if (adapter != null) {
            if (!first) {
                SharedPreferences.Editor ed = pref.edit();
                ed.putString(PREF_LAST_PATH, path);
                ed.commit();
            }
        }
        else {
            if (first) {
                AlertDialog.Builder ad = new AlertDialog.Builder(MainActivity.this);
                ad.setMessage(R.string.cant_open);
                ad.setPositiveButton(R.string.try_again, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int arg1) {
                        String path = pref.getString(PREF_LAST_PATH, "");
                        openFile(path, true);
                    }
                });
                ad.setNegativeButton(R.string.download_new, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int arg1) {
                        startDownloadDialog();
                    }
                });
                ad.setCancelable(false);
                ad.show();
            }
        }
    }

    private boolean checkConnection() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected())
            return true;
        else {
            Toast toast = Toast.makeText(
                    getApplicationContext(), getString(R.string.not_connected), Toast.LENGTH_LONG
            );
            toast.show();
        }

        return false;
    }

    public static class MainFragment extends Fragment {
        private SqlAdapter adapter;

        public MainFragment() {
            // Empty constructor required for fragment subclasses
        }

        public void setAdapter(SqlAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.list_fragment, container, false);

            if (adapter != null) {
                ListView lView = (ListView) rootView.findViewById(R.id.lview);
                lView.setAdapter(adapter);
                lView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long id) {
                        Coin coin = adapter.getFullItem(pos);

                        Intent intent = new Intent(getActivity().getApplicationContext(), CoinActivity.class);
                        intent.putExtra(EXTRA_COIN_ID, coin);
                        startActivity(intent);
                    }
                });
            }

            return rootView;
        }
    }

    public static class StatisticsFragment extends Fragment {
        private SqlAdapter adapter;
        private ListView saveListView;

        public StatisticsFragment() {
            // Empty constructor required for fragment subclasses
        }

        public void setAdapter(SqlAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.statistics_fragment, container, false);
            saveListView = (ListView) rootView.findViewById(R.id.lview);

            refreshAdapter();

            return rootView;
        }

        public void refreshAdapter() {
            if (adapter != null) {
                saveListView.setAdapter(adapter.getStatisticsAdapter(getActivity()));
            }
        }
    }

    private class DownloadCatalogTask extends DownloadFileTask {
        public DownloadCatalogTask(Context context, DownloadEntry entry) {
            super(context, entry);
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);

            if (result == 0) {
                openFile(entry.file().getPath(), false);
            }
            else {
                startDownloadDialog();
            }
        }
    }

    private class DownloadUpdateTask extends DownloadFileTask {
        public DownloadUpdateTask(Context context, DownloadEntry entry) {
            super(context, entry);
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);

            if (result == 0) {
                adapter.update(entry.file().getPath(), entry.title());
                adapter.refresh();

                entry.file().delete();
            }
        }
    }

    private class DownloadListTask extends DownloadJsonTask {
        private final Integer LIST_VERSION = 1;

        @Override
        protected void onPostExecute(JSONObject json) {
            if (json == null) {
                Toast toast = Toast.makeText(
                        MainActivity.this, getString(R.string.could_not_download_list), Toast.LENGTH_LONG
                );
                toast.show();

                return;
            }

            try {
                if (json.getInt("version") != LIST_VERSION)
                    return;

                String density = pref.getString("density", "XHDPI");
                String catalog = adapter.getCatalogTitle();

                JSONArray cats = json.getJSONArray("catalogues");
                for (int i = 0; i < cats.length(); i++) {
                    JSONObject cat = cats.getJSONObject(i);
                    String file = cat.getString("file");
                    if (file.equals(catalog)) {
                        JSONArray upds = cat.getJSONArray("updates");
                        for (int j = 0; j < upds.length(); j++) {
                            JSONObject upd = upds.getJSONObject(j);

                            if (!adapter.checkUpdate(upd.getString("title"))) {
                                final DownloadEntry entry = new DownloadEntry(upd.getString("title"),
                                        upd.getString("date"), upd.getJSONObject("size").getString(density),
                                        upd.getString("file"), upd.getJSONObject("url").getString(density));

                                AlertDialog.Builder ad = new AlertDialog.Builder(MainActivity.this);
                                ad.setMessage(R.string.apply_update);
                                ad.setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int arg1) {
                                        downloadUpdate(entry);
                                    }
                                });
                                ad.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int arg1) {
                                        dialog.dismiss();
                                    }
                                });
                                ad.setCancelable(true);
                                ad.show();

                                return;
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Toast toast = Toast.makeText(
                    MainActivity.this, getString(R.string.no_updates_available), Toast.LENGTH_LONG
            );
            toast.show();
        }
    }
}
