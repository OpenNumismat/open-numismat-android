package janis.opennumismat;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by v.ignatov on 21.10.2014.
 */
public class CoinActivity extends Activity {
    private static final String COIN_DETAILS_DELIMITER = ": ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coin);

        Intent intent = getIntent();
        Coin coin = intent.getParcelableExtra(MainActivity.EXTRA_COIN_ID);

        ActionBar actionBar = getActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        actionBar.setTitle(coin.getTitle());

        ImageView coin_obverse = (ImageView) findViewById(R.id.coin_obverse);
        coin_obverse.setImageBitmap(coin.getObverseImageBitmap());
        ImageView coin_reverse = (ImageView) findViewById(R.id.coin_reverse);
        coin_reverse.setImageBitmap(coin.getReverseImageBitmap());

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
        if (!coin.getDate().isEmpty()) {
            text = new TextView(this);
            text.setText(getString(R.string.date_of_issue) + COIN_DETAILS_DELIMITER);
            coin_details.addView(text);

            text = new TextView(this);
            text.setTypeface(null, Typeface.BOLD);
            text.setText(coin.getDate());
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
        if (!coin.getMintage().isEmpty()) {
            text = new TextView(this);
            text.setText(getString(R.string.mintage) + COIN_DETAILS_DELIMITER);
            coin_details.addView(text);

            text = new TextView(this);
            text.setTypeface(null, Typeface.BOLD);
            text.setText(coin.getMintage());
            coin_details.addView(text);
        }
/*
        TODO: Enable mintmark and price fields
        if (!coin.getMintmark().isEmpty()) {
            text = new TextView(this);
            text.setText(getString(R.string.mintmark) + COIN_DETAILS_DELIMITER);
            coin_details.addView(text);

            text = new TextView(this);
            text.setTypeface(null, Typeface.BOLD);
            text.setText(coin.getMintmark());
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
*/
        if (!coin.getMaterial().isEmpty()) {
            text = new TextView(this);
            text.setText(getString(R.string.material) + COIN_DETAILS_DELIMITER);
            coin_details.addView(text);

            text = new TextView(this);
            text.setTypeface(null, Typeface.BOLD);
            text.setText(coin.getMaterial());
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
}
