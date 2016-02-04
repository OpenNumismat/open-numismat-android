package org.opennumismat.uscommemoratives;

import android.support.v7.app.ActionBar;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

/**
 * Created by v.ignatov on 17.10.2014.
 */
public class AboutActivity extends AppCompatActivity {
    private static final String HOME_URL = "https://www.facebook.com/uscoincollection/";
    public static final String DB_VERSION_ID = "DB_VERSION_ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        TextView text = (TextView) findViewById(R.id.text_version);
        text.setText(getString(R.string.version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String version = extras.getString(DB_VERSION_ID);
            text = (TextView) findViewById(R.id.text_version_data);
            text.setText(getString(R.string.version_data, version));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void visitHome(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(HOME_URL));
        startActivity(browserIntent);
    }

    public void rateApp(View view) {
        final String appPackageName = getPackageName();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }
}
