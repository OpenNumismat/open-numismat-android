package janis.opennumismat;

import android.app.ActionBar;
//import android.support.v7.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

/**
 * Created by v.ignatov on 17.10.2014.
 */
public class AboutActivity extends Activity {
    private static final String HOME_URL = "http://opennumismat.github.io";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ActionBar actionBar = getActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
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

    public void sendMessage(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(HOME_URL));
        startActivity(browserIntent);
    }
}
