package janis.opennumismat;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.widget.ImageView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by v.ignatov on 28.10.2014.
 */
public class ImageActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int maxSize = Math.min(metrics.widthPixels, metrics.heightPixels);

        Intent intent = getIntent();
        Boolean obverse = intent.getExtras().getBoolean(MainActivity.EXTRA_COIN_IMAGE);
        Coin coin = intent.getParcelableExtra(MainActivity.EXTRA_COIN_ID);

        ImageView coin_image = (ImageView) findViewById(R.id.coin_image);
        if (obverse) {
            coin_image.setImageBitmap(coin.getObverseImageBitmap(maxSize));
        }
        else {
            coin_image.setImageBitmap(coin.getReverseImageBitmap(maxSize));
        }

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(coin.getTitle());
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
}
