package janis.opennumismat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by v.ignatov on 20.10.2014.
 */
public class SqlAdapter extends BaseAdapter {
    private static final int DB_VERSION = 2;
    private static final int DB_NATIVE_VERSION = 3;

    private static final String TABLE_NAME = "coins";
    // Для удобства выполнения sql-запросов
    // создадим константы с именами полей таблицы
    // и номерами соответсвующих столбцов
    private static final String KEY_ID = "id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_VALUE = "value";
    private static final String KEY_UNIT = "unit";
    private static final String KEY_YEAR = "year";
    private static final String KEY_COUNTRY = "country";
    private static final String KEY_MINTMARK = "mintmark";
    private static final String KEY_MINTAGE = "mintage";
    private static final String KEY_SERIES = "series";
    private static final String KEY_SUBJECT_SHORT = "subjectshort";
    private static final String KEY_QUALITY = "quality";
    private static final String KEY_QUANTITY = "quantity";
    private static final String KEY_IMAGE = "image";

    private int version;
    private boolean isMobile;
    private Cursor cursor;
    private SQLiteDatabase database;
    private Context context;

    public SqlAdapter(Context context, String path) {
        super();
        this.context = context;
        init(path);
    }

    @Override
    public long getItemId(int position) {
        Coin coin = getItem(position);
        return coin.getId();
    }

    static class ViewHolder {
        public ImageView image;
        public TextView title;
        public TextView count;
        public TextView description;
        public LinearLayout count_layout;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        View rowView = convertView;
        if (null == convertView) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(R.layout.list_item, null);

            holder = new ViewHolder();
            holder.title = (TextView) rowView.findViewById(R.id.title);
            holder.description = (TextView) rowView.findViewById(R.id.description);
            holder.count = (TextView) rowView.findViewById(R.id.count);
            holder.image = (ImageView) rowView.findViewById(R.id.coin_image);
            if (!isMobile) {
                holder.image.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            holder.count_layout = (LinearLayout) rowView.findViewById(R.id.CountLayout);

            rowView.setTag(holder);
        }
        else {
            holder = (ViewHolder) rowView.getTag();
        }

        Coin coin = getItem(position);

        holder.title.setText(coin.getTitle());
        holder.description.setText(coin.getDescription(context));

        holder.image.setImageBitmap(coin.getImageBitmap());

        if (coin.count > 0) {
            holder.count.setText(coin.getCount());
            holder.count.setVisibility(View.VISIBLE);
        }
        else {
            holder.count.setVisibility(View.GONE);
        }

        holder.count_layout.setOnClickListener(new OnClickListener(coin));

        return rowView;
    }

    private class OnClickListener implements
            View.OnClickListener {

        private Coin coin;

        public OnClickListener(Coin coin) {
            this.coin = coin;
        }

        public void onClick(View v) {
            // TODO: Use count_dialog.xml
            RelativeLayout linearLayout = new RelativeLayout(context);
            final NumberPicker aNumberPicker = new NumberPicker(context);
            aNumberPicker.setMaxValue(1000);
            aNumberPicker.setMinValue(0);
            aNumberPicker.setValue((int)coin.count);
            aNumberPicker.setWrapSelectorWheel(false);
            final TextView aTextView = new TextView(context);
            aTextView.setText(coin.getTitle());
            aTextView.setTextSize(18.f);

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(50, 50);
            RelativeLayout.LayoutParams numPicerParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            numPicerParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
            RelativeLayout.LayoutParams textPicerParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            textPicerParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
            textPicerParams.setMargins(10, 10, 10, 10);

            linearLayout.setLayoutParams(params);
            linearLayout.addView(aTextView,textPicerParams);
            linearLayout.addView(aNumberPicker,numPicerParams);

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
            alertDialogBuilder.setTitle(R.string.change_count);
            alertDialogBuilder.setView(linearLayout);
            alertDialogBuilder
                    .setCancelable(true)
                    .setPositiveButton(R.string.save,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    int new_count = aNumberPicker.getValue();

                                    if (!isMobile) {
                                        Toast toast = Toast.makeText(
                                                context, R.string.not_implemented_for_desktop, Toast.LENGTH_LONG
                                        );
                                        toast.show();
/*
                                        int old_count = (int) coin.count;
                                        if (new_count > old_count) {
                                            for (int i = old_count; i < new_count; i++) {
                                                addCoin();
                                            }
                                        } else if (new_count < old_count) {
                                            for (int i = old_count; i < new_count; i--) {
                                                removeCoin();
                                            }
                                        }
*/
                                    }
                                    else {
                                        setCoinCount(coin, new_count);
                                    }
                                }
                            })
                    .setNegativeButton(R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    dialog.cancel();
                                }
                            });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
    };

    @Override
    public int getCount() {
        return cursor.getCount();
    }

    @Override
    public Coin getItem(int position) {
        if (cursor.moveToPosition(position)) {
            Coin coin = new Coin(cursor);
            if (isMobile) {
                coin.image = cursor.getBlob(Coin.IMAGE_COLUMN);
            }
            else {
                coin.count = getCoinsCount(coin);
                Cursor extra_cursor = database.rawQuery("SELECT image FROM images WHERE id = ?",
                        new String[] { Long.toString(cursor.getLong(Coin.IMAGE_COLUMN)) });
                if (extra_cursor.moveToFirst())
                    coin.image = extra_cursor.getBlob(0);
            }
            return coin;
        } else {
            throw new CursorIndexOutOfBoundsException(
                    "Cant move cursor to postion");
        }
    }

    public Coin getFullItem(int position) {
        if (cursor.moveToPosition(position)) {
            Coin coin = new Coin(cursor);
            Cursor extra_cursor;

            if (isMobile) {
                extra_cursor = database.rawQuery("SELECT subject, material, issuedate," +
                        " obverseimg.image AS obverseimg, reverseimg.image AS reverseimg FROM coins" +
                        " LEFT JOIN images AS obverseimg ON coins.obverseimg = obverseimg.id" +
                        " LEFT JOIN images AS reverseimg ON coins.reverseimg = reverseimg.id" +
                        " WHERE coins.id = ?", new String[]{Long.toString(coin.getId())});
            }
            else {
                extra_cursor = database.rawQuery("SELECT subject, material, issuedate," +
                        " obverseimg.image AS obverseimg, reverseimg.image AS reverseimg FROM coins" +
                        " LEFT JOIN photos AS obverseimg ON coins.obverseimg = obverseimg.id" +
                        " LEFT JOIN photos AS reverseimg ON coins.reverseimg = reverseimg.id" +
                        " WHERE coins.id = ?", new String[]{Long.toString(coin.getId())});
            }
            if (extra_cursor.moveToFirst())
                coin.addExtra(extra_cursor);

            return coin;
        } else {
            throw new CursorIndexOutOfBoundsException(
                    "Cant move cursor to postion");
        }
    }

    private void setCoinCount(Coin coin, int new_count) {
        ContentValues cv = new ContentValues();
        cv.put("quantity", Integer.toString(new_count));
        database.update("coins", cv, "id = ?", new String[] { Long.toString(coin.getId()) });

        coin.count = new_count;

        refresh();
    }

    //Методы для работы с базой данных

    public Cursor getAllEntries() {
        //Список колонок базы, которые следует включить в результат
        String[] columnsToTake = { KEY_ID, KEY_TITLE, KEY_VALUE, KEY_UNIT, KEY_YEAR, KEY_COUNTRY, KEY_MINTMARK, KEY_MINTAGE, KEY_SERIES, KEY_SUBJECT_SHORT, KEY_QUALITY, KEY_QUANTITY, KEY_IMAGE };
        // составляем запрос к базе
        return database.query(TABLE_NAME, columnsToTake,
                null, null, null, null, KEY_ID);
    }

    public void onDestroy() {
        database.close();
    }

    //Вызывает обновление вида
    private void refresh() {
        cursor = getAllEntries();
        notifyDataSetChanged();
    }

    // Инициализация адаптера: открываем базу и создаем курсор
    private void init(String path) {
        database = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READWRITE);

        Cursor version_cursor = database.rawQuery("SELECT value FROM settings" +
                " WHERE title = 'Version'", new String[] {});
        if (version_cursor.moveToFirst()) {
            String version_str = version_cursor.getString(0);
            isMobile = version_str.startsWith("M");
            if (isMobile) {
                version = Integer.parseInt(version_str.substring(1));
                if (version > DB_VERSION) {
                    Toast toast = Toast.makeText(
                            context, R.string.new_db_version, Toast.LENGTH_LONG
                    );
                    toast.show();
                }
            }
            else {
                version = Integer.parseInt(version_str);
                if (version > DB_NATIVE_VERSION) {
                    Toast toast = Toast.makeText(
                            context, R.string.new_db_version, Toast.LENGTH_LONG
                    );
                    toast.show();
                }
                else if (version < DB_NATIVE_VERSION) {
                    Toast toast = Toast.makeText(
                            context, R.string.old_db_version, Toast.LENGTH_LONG
                    );
                    toast.show();
                }
            }
        }
        else {
            throw new SQLiteException("Wrong DB format");
        }

        cursor = getAllEntries();
    }

    // Only for desktop version
    private long getCoinsCount(Coin coin) {
        String sql = "SELECT COUNT(*) FROM coins WHERE status='owned'" +
                    " AND " + makeFilter(coin.subject_short.isEmpty(), "subjectshort") +
                    " AND " + makeFilter(coin.series.isEmpty(), "series") +
                    " AND " + makeFilter(coin.value == 0, "value") +
                    " AND " + makeFilter(coin.country.isEmpty(), "country") +
                    " AND " + makeFilter(coin.unit.isEmpty(), "unit") +
                    " AND " + makeFilter(coin.year == 0, "year") +
                    " AND " + makeFilter(coin.mintmark.isEmpty(), "mintmark") +
                    " AND " + makeFilter(coin.quality.isEmpty(), "quality");
        ArrayList<String> params = new ArrayList<String>();

        if (!coin.subject_short.isEmpty()) {
            params.add(coin.subject_short);
        }
        if (!coin.series.isEmpty()) {
            params.add(coin.series);
        }
        if (coin.value > 0) {
            params.add(Long.toString(coin.value));
        }
        if (!coin.country.isEmpty()) {
            params.add(coin.country);
        }
        if (!coin.unit.isEmpty()) {
            params.add(coin.unit);
        }
        if (coin.year > 0) {
            params.add(Long.toString(coin.year));
        }
        if (!coin.mintmark.isEmpty()) {
            params.add(coin.mintmark);
        }
        if (!coin.quality.isEmpty()) {
            params.add(coin.quality);
        }

        String[] params_arr = new String[params.size()];
        params_arr = params.toArray(params_arr);
        Cursor extra_cursor = database.rawQuery(sql, params_arr);

        if (extra_cursor.moveToFirst())
            return extra_cursor.getLong(0);
        else
            return 0;
    }

    private String makeFilter(boolean empty, String field) {
        if (empty)
            return "IFNULL(" + field + ",'')=''";
        else
            return field + "=?";
    }
}
