package janis.opennumismat;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ipaulpro.afilechooser.utils.FileUtils;

import java.util.List;


public class MainActivity extends ActionBarActivity {
    private static final String PREF_LAST_PATH = "last_path";
    private static final int REQUEST_CHOOSER = 1;
    private static final int REQUEST_DOWNLOADER = 2;

    public final static String EXTRA_COIN_ID = "org.janis.opennumismat.COIN_ID";
    public final static String EXTRA_COIN_IMAGE = "org.janis.opennumismat.COIN_IMAGE";

    private DrawerLayout drawerLayout;
    private ListView listView;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private Toolbar toolbar;
    private CharSequence title;

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

        if (savedInstanceState == null) {
            selectItem(0);
        }

        // Set default density
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (!pref.contains("density")) {
            String density;
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            if (metrics.densityDpi <= metrics.DENSITY_MEDIUM)
                density = "MDPI";
            else if (metrics.densityDpi <= metrics.DENSITY_HIGH)
                density = "HDPI";
            else if (metrics.densityDpi <= metrics.DENSITY_XHIGH)
                density = "XHDPI";
            else if (metrics.densityDpi <= metrics.DENSITY_XXHIGH)
                density = "XXHDPI";
            else
                density = "XXXHDPI";

            SharedPreferences.Editor ed = pref.edit();
            ed.putString("density", density);
            ed.apply();
        }

        // Load latest collection
        String path = pref.getString(PREF_LAST_PATH, "");
        if (!path.isEmpty()) {
            openFile(path, true);
        }
        else {
            AlertDialog.Builder ad = new AlertDialog.Builder(this);
            ad.setMessage(R.string.where_first);
            ad.setPositiveButton(R.string.download, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int arg1) {
                    openDownloadDialog();
                }
            });
            ad.setNeutralButton(R.string.open, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int arg1) {
                    openFileDialog();
                }
            });
            ad.setCancelable(true);
            ad.show();
        }

        prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs,
                                                  String key) {
                if (adapter != null) {
                    if (key.equals("sort_order")) {
                        adapter.refresh();
                    } else if (key.equals("filter_field")) {
                        adapter.setFilterField(prefs.getString(key, adapter.DEFAULT_FILTER));
                        adapter.refresh();

                        title = adapter.getFilter() + " ▼";
                        setTitle(title);
                    }
                }
            }
        };
        pref.registerOnSharedPreferenceChangeListener(prefListener);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_open) {
            openFileDialog();
            return true;
        }
        else if (id == R.id.action_download) {
            openDownloadDialog();
            return true;
        }
        else if (id == R.id.action_preferences) {
            startActivity(new Intent(this, PreferencesActivity.class));
            return true;
        }
        else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        else if (id == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openFileDialog() {
        Intent getContentIntent = FileUtils.createGetContentIntent();

        Intent intent = Intent.createChooser(getContentIntent, getString(R.string.file_chooser));
        startActivityForResult(intent, REQUEST_CHOOSER);
    }

    private void openDownloadDialog() {
        Intent intent = new Intent(this, DownloadActivity.class);
        startActivityForResult(intent, REQUEST_DOWNLOADER);
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
                args.putInt(DummyFragment.ARG_MENU_INDEX, position);

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
                }
                break;

            case 1:
                fragment = new DummyFragment();
                args.putInt(DummyFragment.ARG_MENU_INDEX, position);
                title = navigationDrawerItems[position];
                setTitle(title);
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
        getSupportActionBar().setTitle("");
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
        switch (requestCode) {
            case REQUEST_CHOOSER:
            case REQUEST_DOWNLOADER:
                if (resultCode == RESULT_OK) {

                    final Uri uri = data.getData();

                    // Get the File path from the Uri
                    String path = FileUtils.getPath(this, uri);
                    if (path != null) {
                        // TODO handle non-primary volumes
                        if (path.equals("TODO")) {
                            Toast toast = Toast.makeText(
                                    getApplicationContext(), getString(R.string.could_not_open_sd), Toast.LENGTH_LONG
                            );
                            toast.show();
                        }
                        else if (FileUtils.isLocal(path)) {
                            openFile(path, false);
                        }
                    }
                }
                break;
        }
    }

    private void openFile(String path, boolean first) {
        try {
            if (adapter != null) {
                adapter.close();
                adapter = null;
            }

            adapter = new SqlAdapter(this, path);
        } catch (SQLiteException e) {
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
                ed.apply();
            }
        }
        else {
            if (first) {
                SharedPreferences.Editor ed = pref.edit();
                ed.remove(PREF_LAST_PATH);
                ed.apply();
            }
        }
    }

    public class MainFragment extends Fragment {
        public MainFragment() {
            // Empty constructor required for fragment subclasses
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

                        Intent intent = new Intent(getApplicationContext(), CoinActivity.class);
                        intent.putExtra(EXTRA_COIN_ID, coin);
                        startActivity(intent);
                    }
                });
            }

            return rootView;
        }
    }

    public static class DummyFragment extends Fragment {
        public static final String ARG_MENU_INDEX = "index";

        public DummyFragment() {
            // Empty constructor required for fragment subclasses
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.statistics_fragment, container, false);
            int index = getArguments().getInt(ARG_MENU_INDEX);
            String text = String.format("Menu at index %s", index);
            ((TextView) rootView.findViewById(R.id.textView)).setText(text);
            return rootView;
        }
    }
}