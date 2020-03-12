package janis.opennumismat;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;

import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by v.ignatov on 21.10.2014.
 */
public class CoinActivity extends AppCompatActivity {
    private static final String COIN_DETAILS_DELIMITER = ": ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coin);

        Intent intent = getIntent();
        Coin coin = intent.getParcelableExtra(MainActivity.EXTRA_COIN_ID);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(coin.getTitle());
        }
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int maxSize = metrics.widthPixels > metrics.heightPixels ? metrics.heightPixels : metrics.widthPixels;

        ImageView coin_obverse = (ImageView) findViewById(R.id.coin_obverse);
        coin_obverse.setImageBitmap(coin.getObverseImageBitmap(maxSize/2));
        coin_obverse.setOnClickListener(new OnClickListener(coin, true));
        ImageView coin_reverse = (ImageView) findViewById(R.id.coin_reverse);
        coin_reverse.setImageBitmap(coin.getReverseImageBitmap(maxSize/2));
        coin_reverse.setOnClickListener(new OnClickListener(coin, false));

        GridLayout coin_details = (GridLayout) findViewById(R.id.coin_details);
        TextView text;
        if (!coin.getCountry().isEmpty()) {
            text = new TextView(this);
            text.setText(getString(R.string.country) + COIN_DETAILS_DELIMITER);
            coin_details.addView(text);

            text = new TextView(this);
            text.setTypeface(null, Typeface.BOLD);
            text.setText(coin.getCountry());
            coin_details.addView(text);
        }
        if (!coin.getDenomination().isEmpty()) {
            text = new TextView(this);
            text.setText(getString(R.string.denomination) + COIN_DETAILS_DELIMITER);
            coin_details.addView(text);

            text = new TextView(this);
            text.setTypeface(null, Typeface.BOLD);
            text.setText(coin.getDenomination());
            coin_details.addView(text);
        }
        if (!coin.getSubjectShort().isEmpty()) {
            text = new TextView(this);
            text.setText(getString(R.string.subject_short) + COIN_DETAILS_DELIMITER);
            coin_details.addView(text);

            text = new TextView(this);
            text.setTypeface(null, Typeface.BOLD);
            text.setText(coin.getSubjectShort());
            coin_details.addView(text);
        }
        if (!coin.getSeries().isEmpty()) {
            text = new TextView(this);
            text.setText(getString(R.string.series) + COIN_DETAILS_DELIMITER);
            coin_details.addView(text);

            text = new TextView(this);
            text.setTypeface(null, Typeface.BOLD);
            text.setText(coin.getSeries());
            coin_details.addView(text);
        }
        if (!coin.getDate(this).isEmpty()) {
            text = new TextView(this);
            text.setText(getString(R.string.date_of_issue) + COIN_DETAILS_DELIMITER);
            coin_details.addView(text);

            text = new TextView(this);
            text.setTypeface(null, Typeface.BOLD);
            text.setText(coin.getDate(this));
            coin_details.addView(text);
        }
        else if (!coin.getYear().isEmpty()) {
            text = new TextView(this);
            text.setText(getString(R.string.year) + COIN_DETAILS_DELIMITER);
            coin_details.addView(text);

            text = new TextView(this);
            text.setTypeface(null, Typeface.BOLD);
            text.setText(coin.getYear());
            coin_details.addView(text);
        }
        if (!coin.getMint().isEmpty()) {
            text = new TextView(this);
            text.setText(getString(R.string.mint) + COIN_DETAILS_DELIMITER);
            coin_details.addView(text);

            text = new TextView(this);
            text.setTypeface(null, Typeface.BOLD);
            text.setText(coin.getMint());
            coin_details.addView(text);
        }
        if (!coin.getMintage().isEmpty()) {
            text = new TextView(this);
            text.setText(getString(R.string.mintage) + COIN_DETAILS_DELIMITER);
            coin_details.addView(text);

            text = new TextView(this);
            text.setTypeface(null, Typeface.BOLD);
            text.setText(coin.getMintage());
            coin_details.addView(text);
        }

        if (!coin.getMaterial().isEmpty()) {
            text = new TextView(this);
            text.setText(getString(R.string.material) + COIN_DETAILS_DELIMITER);
            coin_details.addView(text);

            text = new TextView(this);
            text.setTypeface(null, Typeface.BOLD);
            text.setText(coin.getMaterial());
            coin_details.addView(text);
        }

        if (!coin.getPrices().isEmpty()) {
            text = new TextView(this);
            text.setText(getString(R.string.price) + COIN_DETAILS_DELIMITER);
            coin_details.addView(text);

            text = new TextView(this);
            text.setTypeface(null, Typeface.BOLD);
            text.setText(coin.getPrices());
            coin_details.addView(text);
        }

        TextView subject = (TextView) findViewById(R.id.subject);
        subject.setText(coin.getSubject());
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

    private class OnClickListener implements
            View.OnClickListener {

        private Coin coin;
        private boolean obverse;

        public OnClickListener(Coin coin, boolean obverse) {
            this.coin = coin;
            this.obverse = obverse;
        }

        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), ImageActivity.class);
            intent.putExtra(MainActivity.EXTRA_COIN_IMAGE, obverse);
            intent.putExtra(MainActivity.EXTRA_COIN_ID, coin);
            startActivity(intent);
        }
    };
}
