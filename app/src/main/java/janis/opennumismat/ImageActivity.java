package janis.opennumismat;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

/**
 * Created by v.ignatov on 28.10.2014.
 */
public class ImageActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        Intent intent = getIntent();
        Coin coin = intent.getParcelableExtra(MainActivity.EXTRA_COIN_ID);

        ImageView coin_obverse = (ImageView) findViewById(R.id.coin_obverse);
        coin_obverse.setImageBitmap(coin.getObverseImageBitmap());
        ImageView coin_reverse = (ImageView) findViewById(R.id.coin_reverse);
        coin_reverse.setImageBitmap(coin.getReverseImageBitmap());

        ActionBar actionBar = getActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        actionBar.setTitle(coin.getTitle());
    }
}
