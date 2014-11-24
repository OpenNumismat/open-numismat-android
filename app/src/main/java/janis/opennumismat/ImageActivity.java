package janis.opennumismat;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.widget.ImageView;

/**
 * Created by v.ignatov on 28.10.2014.
 */
public class ImageActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int maxSize = metrics.widthPixels > metrics.heightPixels ? metrics.heightPixels : metrics.widthPixels;

        Intent intent = getIntent();
        Coin coin = intent.getParcelableExtra(MainActivity.EXTRA_COIN_ID);

        ImageView coin_obverse = (ImageView) findViewById(R.id.coin_obverse);
        coin_obverse.setImageBitmap(coin.getObverseImageBitmap(maxSize));
        ImageView coin_reverse = (ImageView) findViewById(R.id.coin_reverse);
        coin_reverse.setImageBitmap(coin.getReverseImageBitmap(maxSize));

        ActionBar actionBar = getActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        actionBar.setTitle(coin.getTitle());
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
}
