package janis.opennumismat;

import android.app.Activity;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

import com.ipaulpro.afilechooser.utils.FileUtils;

import java.io.File;

public class MainActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    private static final String PREF_LAST_PATH = "last_path";
    public final static String EXTRA_COIN_ID = "org.janis.opennumismat.COIN_ID";

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    private String[] animals;
    private SharedPreferences pref;
    private static final int REQUEST_CHOOSER = 1;
    private static final int REQUEST_DOWNLOADER = 2;
    private SqlAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        adapter = null;

        pref = PreferenceManager.getDefaultSharedPreferences(this);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        ListView lView = (ListView) findViewById(R.id.lview);
        lView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                                    int pos, long id
            ) {
                if (adapter != null) {
                    Coin coin = adapter.getFullItem(pos);

                    Intent intent = new Intent(getApplicationContext(), CoinActivity.class);
                    intent.putExtra(EXTRA_COIN_ID, coin);
                    startActivity(intent);
                }
            }
        });

        String path = pref.getString(PREF_LAST_PATH, "");
        if (!path.isEmpty()) {
            try {
                adapter = new SqlAdapter(this, path);
            } catch (SQLiteException e) {
                Toast toast = Toast.makeText(
                        getApplicationContext(), getString(R.string.could_not_open_database) + '\n' + path, Toast.LENGTH_LONG
                );
                toast.show();

                adapter = null;
            }

            lView.setAdapter(adapter);
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
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                .commit();
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

                    // Alternatively, use FileUtils.getFile(Context, Uri)
                    if (path != null && FileUtils.isLocal(path)) {
                        try {
                            adapter = new SqlAdapter(this, path);
                        } catch (SQLiteException e) {
                            Toast toast = Toast.makeText(
                                    getApplicationContext(), getString(R.string.could_not_open_database) + '\n' + path, Toast.LENGTH_LONG
                            );
                            toast.show();

                            adapter = null;
                        }

                        ListView lView = (ListView) findViewById(R.id.lview);
                        lView.setAdapter(adapter);

                        if (adapter != null) {
                            SharedPreferences.Editor ed = pref.edit();
                            ed.putString(PREF_LAST_PATH, path);
                            ed.commit();
                        }
                    }
                }
                break;
        }
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.app_name);
                break;
            case 2:
                mTitle = getString(R.string.title_section_about);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
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
        else if (id == R.id.action_settings) {
            startActivity(new Intent(this, PrefActivity.class));
            return true;
        }
        else if (id == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

}
