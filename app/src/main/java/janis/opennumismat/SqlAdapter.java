package janis.opennumismat;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by v.ignatov on 20.10.2014.
 */
public class SqlAdapter extends BaseAdapter {
    private static final int DB_VERSION = 6;
    private static final int DB_NATIVE_VERSION = 3;
    public static final String DEFAULT_GRADE = "XF";
    public static final String DEFAULT_FILTER = "series";

    private int version;
    private boolean isMobile;
    private Cursor cursor;
    private SQLiteDatabase database;
    private Context context;
    private SharedPreferences pref;
    private List<String> filters;
    private String filter;
    private String filter_field;

    static class Group {
        public Integer count;
        public String title;
        public Integer position;
    }

    private List<Group> groups;

    public SqlAdapter(Context context, String path) {
        super();
        this.context = context;
        pref = PreferenceManager.getDefaultSharedPreferences(context);
        init(path);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        for (Group group : groups) {
            if (group.position == position) {
                rowView = inflater.inflate(R.layout.group_header, null);
                TextView title = (TextView) rowView.findViewById(R.id.title);
                title.setText(group.title);
                return rowView;
            }
        }

        if (convertView != null && convertView.findViewById(R.id.coin_image) != null)
            rowView = convertView;
        else
            rowView = inflater.inflate(R.layout.list_item, null);

        Coin coin = getItem(position);

        TextView title = (TextView) rowView.findViewById(R.id.title);
        title.setText(coin.getTitle());
        TextView description = (TextView) rowView.findViewById(R.id.description);
        description.setText(coin.getDescription(context));

        TextView count = (TextView) rowView.findViewById(R.id.count);
        if (coin.count > 0) {
            count.setText(coin.getCount());
            count.setVisibility(View.VISIBLE);

            GradientDrawable back = (GradientDrawable) count.getBackground();
            if (coin.grade.equals("Unc"))
                back.setColor(context.getResources().getColor(R.color.unc));
            else if (coin.grade.equals("AU"))
                back.setColor(context.getResources().getColor(R.color.au));
            else if (coin.grade.equals("VF"))
                back.setColor(context.getResources().getColor(R.color.vf));
            else if (coin.grade.equals("F"))
                back.setColor(context.getResources().getColor(R.color.f));
            else
                back.setColor(context.getResources().getColor(R.color.xf));
        } else {
            if (!pref.getBoolean("show_zero", true))
                count.setVisibility(View.GONE);
            else {
                count.setText("+");
                count.setVisibility(View.VISIBLE);

                GradientDrawable back = (GradientDrawable) count.getBackground();
                back.setColor(context.getResources().getColor(R.color.not_present));
            }
        }

        ImageView imageView = (ImageView) rowView.findViewById(R.id.coin_image);
        if (!isMobile)
            imageView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setImageBitmap(coin.getImageBitmap());

        LinearLayout count_layout = (LinearLayout) rowView.findViewById(R.id.CountLayout);
        count_layout.setOnClickListener(new OnClickListener(coin));

        return rowView;
    }

    private class OnClickListener implements
            View.OnClickListener {

        private Coin coin;
        private String selected_grade;
        private int old_count;
        private Grading grading;
        private GradingAdapter grading_adapter;

        public OnClickListener(Coin coin) {
            this.coin = coin;
        }

        public void onClick(View v) {
            // TODO: Use count_dialog.xml
            if (!pref.getBoolean("use_grading", false)) {
                countDialog(null);
            }
            else {
                ArrayList<Grading> items = new ArrayList<>();

                RelativeLayout linearLayout = new RelativeLayout(context);
                final ListView lView= new ListView(context);

                grading_adapter = new GradingAdapter(context, items);
                lView.setAdapter(grading_adapter);

                lView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> arg0, View arg1,
                                            int pos, long id
                    ) {
                        Grading grade = grading_adapter.getItem(pos);
                        countDialog(grade);
                    }
                });

                Grading grade;
                Resources res = context.getResources();
                grade = new Grading("Unc", res.getString(R.string.Unc), res.getString(R.string.uncirculated));
                grade.count = coin.count_unc;
                grading_adapter.add(grade);
                grade = new Grading("AU", res.getString(R.string.AU), res.getString(R.string.about_uncirculated));
                grade.count = coin.count_au;
                grading_adapter.add(grade);
                grade = new Grading("XF", res.getString(R.string.XF), res.getString(R.string.extremely_fine));
                grade.count = coin.count_xf;
                grading_adapter.add(grade);
                grade = new Grading("VF", res.getString(R.string.VF), res.getString(R.string.very_fine));
                grade.count = coin.count_vf;
                grading_adapter.add(grade);
                grade = new Grading("F", res.getString(R.string.F), res.getString(R.string.fine));
                grade.count = coin.count_f;
                grading_adapter.add(grade);

                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(50, 50);
                RelativeLayout.LayoutParams numPicerParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                numPicerParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

                linearLayout.setLayoutParams(params);
                linearLayout.addView(lView, numPicerParams);

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                Drawable drawable = new BitmapDrawable(context.getResources(), coin.getImageBitmap());
                alertDialogBuilder.setIcon(drawable);
                alertDialogBuilder.setTitle(coin.getTitle());
                alertDialogBuilder.setView(linearLayout);
                alertDialogBuilder
                        .setCancelable(true)
                        .setNeutralButton(R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        dialog.cancel();
                                    }
                                });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        }

        private void countDialog(Grading grade) {
            if (grade != null) {
                selected_grade = grade.grade;
                old_count = grade.count;
            }
            else {
                selected_grade = DEFAULT_GRADE;
                old_count = (int) coin.count;
            }
            grading = grade;

            RelativeLayout linearLayout = new RelativeLayout(context);
            final NumberPicker aNumberPicker = new NumberPicker(context);
            aNumberPicker.setMaxValue(1000);
            aNumberPicker.setMinValue(0);
            aNumberPicker.setValue(old_count);
            aNumberPicker.setWrapSelectorWheel(false);

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(50, 50);
            RelativeLayout.LayoutParams numPicerParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            numPicerParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

            linearLayout.setLayoutParams(params);
            linearLayout.addView(aNumberPicker, numPicerParams);

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
            Drawable drawable = new BitmapDrawable(context.getResources(), coin.getImageBitmap());
            alertDialogBuilder.setIcon(drawable);
            alertDialogBuilder.setTitle(coin.getTitle());
            alertDialogBuilder.setView(linearLayout);
            alertDialogBuilder
                    .setCancelable(true)
                    .setPositiveButton(R.string.save,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    int new_count = aNumberPicker.getValue();

                                    if (new_count > old_count) {
                                        addCoin(coin, new_count - old_count, selected_grade);
                                    } else if (new_count < old_count) {
                                        removeCoin(coin, old_count - new_count, selected_grade);
                                    }

                                    if (grading != null)
                                        grading.count = new_count;
                                    if (grading_adapter != null) {
                                        grading_adapter.notifyDataSetChanged();
                                    }

                                    refresh();
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
    }

    @Override
    public int getCount() {
        return cursor.getCount() + groups.size();
    }

    public int getTotalCount(String filter) {
        String sql = "SELECT id FROM coins";
        if (filter != null) {
            sql += " WHERE " + makeFilter(filter.isEmpty(), filter_field);
        }
        sql += " GROUP BY value, unit, year, country, mintmark, series, subjectshort," +
                " quality, material, variety, obversevar, reversevar, edgevar";

        ArrayList<String> params = new ArrayList<>();
        if (filter != null) {
            if (filter_field.contains(",")) {
                String[] parts = filter.split(" ");
                params.add(parts.length > 1 ? parts[1] : "");
                params.add(parts[0]);
            } else {
                if (!filter.isEmpty())
                    params.add(filter);
            }
        }
        String[] params_arr = new String[params.size()];
        params_arr = params.toArray(params_arr);

        Cursor cursor = database.rawQuery(sql, params_arr);
        int count = 0;
        if(cursor.moveToFirst()) {
            count = cursor.getCount();
        }
        cursor.close();

        return count;
    }

    public int getTotalCount() {
        return getTotalCount(null);
    }

    public int getCollectedCount(String filter) {
        String sql = "SELECT id FROM coins" +
                " WHERE status='owned'";
        if (filter != null) {
            sql += " AND " + makeFilter(filter.isEmpty(), filter_field);
        }
        sql += " GROUP BY value, unit, year, country, mintmark, series, subjectshort," +
                " quality, material, variety, obversevar, reversevar, edgevar";

        ArrayList<String> params = new ArrayList<>();
        if (filter != null) {
            if (filter_field.contains(",")) {
                String[] parts = filter.split(" ");
                params.add(parts.length > 1 ? parts[1] : "");
                params.add(parts[0]);
            } else {
                if (!filter.isEmpty())
                    params.add(filter);
            }
        }
        String[] params_arr = new String[params.size()];
        params_arr = params.toArray(params_arr);

        Cursor cursor = database.rawQuery(sql, params_arr);
        int count = 0;
        if(cursor.moveToFirst()) {
            count = cursor.getCount();
        }
        cursor.close();

        return count;
    }

    public int getCollectedCount() {
        return getCollectedCount(null);
    }

    public int getCoinsCount() {
        String sql = "SELECT COUNT(id) FROM coins" +
                " WHERE status='owned'";
        Cursor cursor = database.rawQuery(sql, new String[]{});
        int count = 0;
        if(cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();

        return count;
    }

    public String getCatalogTitle() {
        String sql = "SELECT value FROM settings" +
                " WHERE title='File'";
        Cursor cursor = database.rawQuery(sql, new String[]{});
        if(cursor.moveToFirst()) {
            return cursor.getString(0);
        }

        return "";
    }

    public boolean isEnabled(int position) {
        for (Group group : groups) {
            if (group.position == position) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Coin getItem(int position) {
        if (cursor.moveToPosition(positionToCursor(position))) {
            Coin coin = new Coin(cursor);
            coin.count = getCoinsCount(coin);
            if (coin.count > 0) {
                coin = getCoinsGrade(coin);
            }
            if (isMobile) {
                coin.image = cursor.getBlob(Coin.IMAGE_COLUMN);
            } else {
                Cursor extra_cursor = database.rawQuery("SELECT image FROM images WHERE id = ?",
                        new String[]{Long.toString(cursor.getLong(Coin.IMAGE_COLUMN))});
                if (extra_cursor.moveToFirst())
                    coin.image = extra_cursor.getBlob(0);
                extra_cursor.close();
            }
            return coin;
        } else {
            throw new CursorIndexOutOfBoundsException(
                    "Can't move cursor to position");
        }
    }

    private int positionToCursor(int position) {
        int group_count = 0;
        for (Group group : groups) {
            if (group.position == position)
                Log.e("WRONG POSITION", Integer.toString(position));
            if (group.position > position)
                break;
            group_count++;
        }
        return position - group_count;
    }

    public Coin getFullItem(int position) {
        if (cursor.moveToPosition(positionToCursor(position))) {
            Coin coin = new Coin(cursor);

            return fillExtra(coin);
        } else {
            throw new CursorIndexOutOfBoundsException(
                    "Can't move cursor to position");
        }
    }

    private Coin fillExtra(Coin coin) {
        Cursor extra_cursor = database.rawQuery("SELECT subject, issuedate, mint," +
                " obverseimg.image AS obverseimg, reverseimg.image AS reverseimg," +
                " price1, price2, price3, price4 FROM coins" +
                " LEFT JOIN photos AS obverseimg ON coins.obverseimg = obverseimg.id" +
                " LEFT JOIN photos AS reverseimg ON coins.reverseimg = reverseimg.id" +
                " WHERE coins.id = ?", new String[]{Long.toString(coin.getId())});
        if (extra_cursor.moveToFirst())
            coin.addExtra(extra_cursor);

        return coin;
    }

    private void addCoin(Coin coin, int count, String grade) {
        Time now = new Time();
        now.setToNow();
        String timestamp = now.format("%Y-%m-%dT%H:%M:%SZ");

        int i;
        for (i = 0; i < count; i++) {
            ContentValues values = new ContentValues();
            values.put("status", "owned");
            values.put("updatedat", timestamp);
            values.put("createdat", timestamp);

            if (!coin.title.isEmpty())
                values.put("title", coin.title);
            if (!coin.subject_short.isEmpty())
                values.put("subjectshort", coin.subject_short);
            if (!coin.series.isEmpty())
                values.put("series", coin.series);
            if (coin.value != 0)
                values.put("value", coin.value);
            if (!coin.country.isEmpty())
                values.put("country", coin.country);
            if (!coin.unit.isEmpty())
                values.put("unit", coin.unit);
            if (coin.year != 0)
                values.put("year", coin.year);
            if (!coin.mintmark.isEmpty())
                values.put("mintmark", coin.mintmark);
            if (coin.mintage != 0)
                values.put("mintage", coin.mintage);
            if (!coin.quality.isEmpty())
                values.put("quality", coin.quality);
            if (!coin.material.isEmpty())
                values.put("material", coin.material);
            if (!coin.variety.isEmpty())
                values.put("variety", coin.variety);
            if (!coin.obversevar.isEmpty())
                values.put("obversevar", coin.obversevar);
            if (!coin.reversevar.isEmpty())
                values.put("reversevar", coin.reversevar);
            if (!coin.edgevar.isEmpty())
                values.put("edgevar", coin.edgevar);
            values.put("grade", grade);

            database.insert("coins", null, values);
        }
    }

    private void removeCoin(Coin coin, int count, String grade) {
        String sql_grade = "";
        if (pref.getBoolean("use_grading", false)) {
            sql_grade = " AND grade = '" + grade + "'";
        }
        String sql = "SELECT id, image, obverseimg, reverseimg FROM coins WHERE status='owned'" +
                " AND " + makeFilter(coin.title.isEmpty(), "title") +
                " AND " + makeFilter(coin.subject_short.isEmpty(), "subjectshort") +
                " AND " + makeFilter(coin.series.isEmpty(), "series") +
                " AND " + makeFilter(coin.value == 0, "value") +
                " AND " + makeFilter(coin.country.isEmpty(), "country") +
                " AND " + makeFilter(coin.unit.isEmpty(), "unit") +
                " AND " + makeFilter(coin.year == 0, "year") +
                " AND " + makeFilter(coin.mintmark.isEmpty(), "mintmark") +
                " AND " + makeFilter(coin.quality.isEmpty(), "quality") +
                " AND " + makeFilter(coin.material.isEmpty(), "material") +
                " AND " + makeFilter(coin.variety.isEmpty(), "variety") +
                " AND " + makeFilter(coin.obversevar.isEmpty(), "obversevar") +
                " AND " + makeFilter(coin.reversevar.isEmpty(), "reversevar") +
                " AND " + makeFilter(coin.edgevar.isEmpty(), "edgevar") +
                sql_grade +
                " ORDER BY id DESC" + " LIMIT " + Integer.toString(count);
        ArrayList<String> params = new ArrayList<>();

        if (!coin.title.isEmpty()) {
            params.add(coin.title);
        }
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
        if (!coin.material.isEmpty()) {
            params.add(coin.material);
        }
        if (!coin.variety.isEmpty()) {
            params.add(coin.variety);
        }
        if (!coin.obversevar.isEmpty()) {
            params.add(coin.obversevar);
        }
        if (!coin.reversevar.isEmpty()) {
            params.add(coin.reversevar);
        }
        if (!coin.edgevar.isEmpty()) {
            params.add(coin.edgevar);
        }

        String[] params_arr = new String[params.size()];
        params_arr = params.toArray(params_arr);
        Cursor cursor = database.rawQuery(sql, params_arr);

        while (cursor.moveToNext()) {
            if (!isMobile) {
                long image_id = cursor.getLong(1);
                if (image_id > 0)
                    database.delete("images", "id = ?", new String[] {Long.toString(image_id)});

                long photo_id;
                photo_id = cursor.getLong(2);
                if (photo_id > 0)
                    database.delete("photos", "id = ?", new String[] {Long.toString(photo_id)});
                photo_id = cursor.getLong(3);
                if (photo_id > 0)
                    database.delete("photos", "id = ?", new String[] {Long.toString(photo_id)});
            }

            long id = cursor.getLong(0);
            database.delete("coins", "id = ?", new String[] {Long.toString(id)});
        }
        cursor.close();
    }

    //Методы для работы с базой данных

    public Cursor getAllEntries() {
        String sql;
        String order;

        if (pref.getString("sort_order", "0").equals("0"))
            order = "ASC";
        else
            order = "DESC";

        ArrayList<String> params = new ArrayList<>();
        if (filter != null && !filter.isEmpty()) {
            if (filter_field.contains(",")) {
                String[] parts = filter.split(" ");
                params.add(parts[1]);
                params.add(parts[0]);
            }
            else
                params.add(filter);
        }
        String[] params_arr = new String[params.size()];
        params_arr = params.toArray(params_arr);

        sql = "SELECT year, COUNT(id) FROM coins" +
                " WHERE status='demo'" +
                (filter != null ? (" AND " + makeFilter(filter.isEmpty(), filter_field)) : "") +
                " GROUP BY year" +
                " ORDER BY year " + order;
        Cursor group_cursor = database.rawQuery(sql, params_arr);
        int position = 0;
        groups = new ArrayList<>();
        while(group_cursor.moveToNext()) {
            Group group = new Group();
            group.count = group_cursor.getInt(1);
            group.title = group_cursor.getString(0);
            if (group.title == null)
                group.title = "";
            group.position = position;
            position += group.count+1;
            if (!group.title.isEmpty())
                groups.add(group);
        }
        group_cursor.close();

        //Список колонок базы, которые следует включить в результат
        sql = "SELECT * FROM (SELECT id, title, value, unit, year, country, mintmark, mintage, series, subjectshort," +
                "quality, material, variety, obversevar, reversevar, edgevar, image, issuedate FROM coins" +
                (filter != null ? (" WHERE " + makeFilter(filter.isEmpty(), filter_field)) : "") +
                " ORDER BY image ASC" +
                ") GROUP BY title, value, unit, year, country, mintmark, series, subjectshort," +
                " quality, material, variety, obversevar, reversevar, edgevar" +
                " ORDER BY year " + order + ", issuedate " + order + ", id ASC";
        return database.rawQuery(sql, params_arr);
    }

    public List<String> getFilters() {
        if (filters == null) {
            Cursor group_cursor = database.rawQuery("SELECT " + filter_field + " FROM coins" +
                    " GROUP BY " + filter_field +
                    " ORDER BY " + filter_field + " ASC", new String[]{});

            filters = new ArrayList<>();
            boolean empty_present = false;
            Resources res = context.getResources();
            switch (filter_field) {
                case "series":
                    filters.add(res.getString(R.string.filter_all_series));
                    break;
                case "country":
                    filters.add(res.getString(R.string.filter_all_countries));
                    break;
                default:
                    filters.add(res.getString(R.string.filter_all));
                    break;
            }
            while (group_cursor.moveToNext()) {
                String val = group_cursor.getString(0);
                if (val == null || val.isEmpty()) {
                    empty_present = true;
                }
                else {
                    if (filter_field.equals("unit,value"))
                        val = group_cursor.getString(1) + " " + val;

                    filters.add(val);
                }
            }
            group_cursor.close();
            if (empty_present)
                if (filter_field.equals("series"))
                    filters.add(res.getString(R.string.filter_empty_series));
                else
                    filters.add(res.getString(R.string.filter_empty));
        }

        return filters;
    }

    public void setFilterField(String field) {
        Cursor filter_field_cursor = database.rawQuery("SELECT value FROM settings" +
                " WHERE title = 'Filter'", new String[] {});
        if (filter_field_cursor.moveToFirst()) {
            ContentValues values = new ContentValues();
            values.put("value", field);
            database.update("settings", values, "title='Filter'", new String[] {});
        }
        else {
            ContentValues values = new ContentValues();
            values.put("value", field);
            database.insert("settings", null, values);
        }
        filter_field_cursor.close();

        filter_field = field;
        filters = null;
        filter = null;
    }

    public void setFilter(String filter) {
        if (this.filter == null && filter == null)
            return;
        if (this.filter != null && this.filter.equals(filter))
            return;

        Resources res = context.getResources();
        if (filter.equals(res.getString(R.string.filter_all)) ||
                filter.equals(res.getString(R.string.filter_all_series)) ||
                filter.equals(res.getString(R.string.filter_all_countries))) {
            this.filter = null;
        } else if (filter.equals(res.getString(R.string.filter_empty)) ||
                filter.equals(res.getString(R.string.filter_empty_series))) {
            this.filter = "";
        } else {
            this.filter = filter;
        }

        refresh();
    }

    public String getFilter() {
        Resources res = context.getResources();
        if (filter == null) {
            switch (filter_field) {
                case "series":
                    return res.getString(R.string.filter_all_series);
                case "country":
                    return res.getString(R.string.filter_all_countries);
                default:
                    return res.getString(R.string.filter_all);
            }
        }
        else if (filter.equals("")) {
            if (filter_field.equals("series"))
                return res.getString(R.string.filter_empty_series);
            else
                return res.getString(R.string.filter_empty);
        }

        return filter;
    }

    public void close() {
        database.close();
    }

    //Вызывает обновление вида
    public void refresh() {
        cursor = getAllEntries();
        notifyDataSetChanged();
    }

    private class MyDbErrorHandler implements DatabaseErrorHandler {
        @Override
        public void onCorruption(SQLiteDatabase dbObj) {
            // Back up the db or do some other stuff
        }
    }

    // Инициализация адаптера: открываем базу и создаем курсор
    private void init(String path) {
        MyDbErrorHandler databaseErrorHandler = new MyDbErrorHandler();
        database = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READWRITE, databaseErrorHandler);

        isMobile = false;
        String type = "unknown";
        Cursor type_cursor = database.rawQuery("SELECT value FROM settings" +
                " WHERE title = 'Type'", new String[] {});
        if (type_cursor.moveToFirst()) {
            type = type_cursor.getString(0);
            if (type.equals("Mobile"))
                isMobile = true;
        }
        type_cursor.close();

        Cursor version_cursor = database.rawQuery("SELECT value FROM settings" +
                " WHERE title = 'Version'", new String[] {});
        if (version_cursor.moveToFirst()) {
            String version_str = version_cursor.getString(0);
            if (version_str.equals("M2")) {
                Toast toast = Toast.makeText(
                        context, R.string.old_db_version, Toast.LENGTH_LONG
                );
                toast.show();
                throw new SQLiteException("Wrong DB format");
            }
            else {
                version = Integer.parseInt(version_cursor.getString(0));
                if (isMobile) {
                    if (version == 4) {
                        database.execSQL("ALTER TABLE coins ADD COLUMN obversevar STRING");
                        database.execSQL("ALTER TABLE coins ADD COLUMN reversevar STRING");
                        database.execSQL("ALTER TABLE coins ADD COLUMN edgevar STRING");
                        database.execSQL("UPDATE settings SET value='5' WHERE title='Version'");
                        database.execSQL("UPDATE settings SET value='country' WHERE title='Filter' AND value='countries'");

                        version = 5;

                        Toast toast = Toast.makeText(
                                context, R.string.upgraded_db_version, Toast.LENGTH_LONG
                        );
                        toast.show();
                    }
                    if (version == 5) {
                        database.execSQL("UPDATE coins SET title=NULL WHERE title=''");
                        database.execSQL("UPDATE coins SET subjectshort=NULL WHERE subjectshort=''");
                        database.execSQL("UPDATE coins SET series=NULL WHERE series=''");
                        database.execSQL("UPDATE coins SET country=NULL WHERE country=''");
                        database.execSQL("UPDATE coins SET unit=NULL WHERE unit=''");
                        database.execSQL("UPDATE coins SET mintmark=NULL WHERE mintmark=''");
                        database.execSQL("UPDATE coins SET material=NULL WHERE material=''");
                        database.execSQL("UPDATE coins SET quality=NULL WHERE quality=''");
                        database.execSQL("UPDATE coins SET variety=NULL WHERE variety=''");
                        database.execSQL("UPDATE coins SET obversevar=NULL WHERE obversevar=''");
                        database.execSQL("UPDATE coins SET reversevar=NULL WHERE reversevar=''");
                        database.execSQL("UPDATE coins SET edgevar=NULL WHERE edgevar=''");
                        database.execSQL("UPDATE settings SET value='6' WHERE title='Version'");

                        File f = new File(path);
                        database.execSQL("INSERT INTO settings VALUES ('File', '" + f.getName() + "')");

                        Cursor cursor = database.rawQuery("SELECT a.id, b.material FROM coins AS a" +
                                " INNER JOIN coins b ON a.title = b.title WHERE a.status='owned'" +
                                " AND b.status='demo' AND a.material IS NULL AND b.material NOT NULL",
                                new String[]{});
                        while (cursor.moveToNext()) {
                            Long id = cursor.getLong(0);
                            String material = cursor.getString(1);

                            ContentValues values = new ContentValues();
                            values.put("material", material);
                            database.update("coins", values, "id=?", new String[] {Long.toString(id)});
                        }
                        cursor.close();

                        version = 6;

                        Toast toast = Toast.makeText(
                                context, R.string.upgraded_db_version, Toast.LENGTH_LONG
                        );
                        toast.show();
                    }

                    if (version > DB_VERSION) {
                        Toast toast = Toast.makeText(
                                context, R.string.new_db_version, Toast.LENGTH_LONG
                        );
                        toast.show();
                    }
                }
                else if (type.equals("OpenNumismat")) {
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
                else {
                    throw new SQLiteException("Wrong DB format");
                }
            }
        }
        else {
            throw new SQLiteException("Wrong DB format");
        }

        Cursor filter_field_cursor = database.rawQuery("SELECT value FROM settings" +
                " WHERE title='Filter'", new String[] {});
        if (filter_field_cursor.moveToFirst()) {
            filter_field = filter_field_cursor.getString(0);
        }
        else {
            filter_field = DEFAULT_FILTER;
        }
        filter_field_cursor.close();
        SharedPreferences.Editor ed = pref.edit();
        ed.putString("filter_field", filter_field);
        ed.apply();

        cursor = getAllEntries();
    }

    private long getCoinsCount(Coin coin) {
        String sql = "SELECT COUNT(*) FROM coins WHERE status='owned'" +
                " AND " + makeFilter(coin.title.isEmpty(), "title") +
                " AND " + makeFilter(coin.subject_short.isEmpty(), "subjectshort") +
                " AND " + makeFilter(coin.series.isEmpty(), "series") +
                " AND " + makeFilter(coin.value == 0, "value") +
                " AND " + makeFilter(coin.country.isEmpty(), "country") +
                " AND " + makeFilter(coin.unit.isEmpty(), "unit") +
                " AND " + makeFilter(coin.year == 0, "year") +
                " AND " + makeFilter(coin.mintmark.isEmpty(), "mintmark") +
                " AND " + makeFilter(coin.quality.isEmpty(), "quality") +
                " AND " + makeFilter(coin.material.isEmpty(), "material") +
                " AND " + makeFilter(coin.variety.isEmpty(), "variety") +
                " AND " + makeFilter(coin.obversevar.isEmpty(), "obversevar") +
                " AND " + makeFilter(coin.reversevar.isEmpty(), "reversevar") +
                " AND " + makeFilter(coin.edgevar.isEmpty(), "edgevar");
        ArrayList<String> params = new ArrayList<>();

        if (!coin.title.isEmpty()) {
            params.add(coin.title);
        }
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
        if (!coin.material.isEmpty()) {
            params.add(coin.material);
        }
        if (!coin.variety.isEmpty()) {
            params.add(coin.variety);
        }
        if (!coin.obversevar.isEmpty()) {
            params.add(coin.obversevar);
        }
        if (!coin.reversevar.isEmpty()) {
            params.add(coin.reversevar);
        }
        if (!coin.edgevar.isEmpty()) {
            params.add(coin.edgevar);
        }

        String[] params_arr = new String[params.size()];
        params_arr = params.toArray(params_arr);
        Cursor extra_cursor = database.rawQuery(sql, params_arr);

        if (extra_cursor.moveToFirst())
            return extra_cursor.getLong(0);
        else
            return 0;
    }

    private Coin getCoinsGrade(Coin coin) {
        if (pref.getBoolean("use_grading", false)) {
            String sql = "SELECT grade, COUNT(grade) FROM coins WHERE status='owned'" +
                    " AND " + makeFilter(coin.title.isEmpty(), "title") +
                    " AND " + makeFilter(coin.subject_short.isEmpty(), "subjectshort") +
                    " AND " + makeFilter(coin.series.isEmpty(), "series") +
                    " AND " + makeFilter(coin.value == 0, "value") +
                    " AND " + makeFilter(coin.country.isEmpty(), "country") +
                    " AND " + makeFilter(coin.unit.isEmpty(), "unit") +
                    " AND " + makeFilter(coin.year == 0, "year") +
                    " AND " + makeFilter(coin.mintmark.isEmpty(), "mintmark") +
                    " AND " + makeFilter(coin.quality.isEmpty(), "quality") +
                    " AND " + makeFilter(coin.material.isEmpty(), "material") +
                    " AND " + makeFilter(coin.variety.isEmpty(), "variety") +
                    " AND " + makeFilter(coin.obversevar.isEmpty(), "obversevar") +
                    " AND " + makeFilter(coin.reversevar.isEmpty(), "reversevar") +
                    " AND " + makeFilter(coin.edgevar.isEmpty(), "edgevar") +
                    " GROUP BY grade";
            ArrayList<String> params = new ArrayList<>();

            if (!coin.title.isEmpty()) {
                params.add(coin.title);
            }
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
            if (!coin.material.isEmpty()) {
                params.add(coin.material);
            }
            if (!coin.variety.isEmpty()) {
                params.add(coin.variety);
            }
            if (!coin.obversevar.isEmpty()) {
                params.add(coin.obversevar);
            }
            if (!coin.reversevar.isEmpty()) {
                params.add(coin.reversevar);
            }
            if (!coin.edgevar.isEmpty()) {
                params.add(coin.edgevar);
            }

            String[] params_arr = new String[params.size()];
            params_arr = params.toArray(params_arr);
            Cursor grading_cursor = database.rawQuery(sql, params_arr);

            String grade;
            while(grading_cursor.moveToNext()) {
                if (grading_cursor.isNull(0) || grading_cursor.getString(0).isEmpty()) {
                    coin.count_xf += grading_cursor.getInt(1);
                    continue;
                }

                grade = grading_cursor.getString(0);
                switch (grade) {
                    case "Unc":
                        coin.count_unc = grading_cursor.getInt(1);
                        break;
                    case "AU":
                        coin.count_au = grading_cursor.getInt(1);
                        break;
                    case "XF":
                        coin.count_xf += grading_cursor.getInt(1);
                        break;
                    case "VF":
                        coin.count_vf = grading_cursor.getInt(1);
                        break;
                    case "F":
                        coin.count_f = grading_cursor.getInt(1);
                        break;
                }
            }
            grading_cursor.close();

            if (coin.count_unc > 0)
                coin.grade = "Unc";
            else if (coin.count_au > 0)
                coin.grade = "AU";
            else if (coin.count_xf > 0)
                coin.grade = "XF";
            else if (coin.count_vf > 0)
                coin.grade = "VF";
            else if (coin.count_f > 0)
                coin.grade = "F";
        }
        else {
            coin.grade = DEFAULT_GRADE;
        }

        return coin;
    }

    private String makeFilter(boolean empty, String field) {
        int i;
        String result;
        String[] parts = field.split(",");
        if (empty) {
            result = "IFNULL(" + parts[0] + ",'')=''";
            for (i = 1; i < parts.length; i++)
                result += " AND " + "IFNULL(" + parts[i] + ",'')=''";
        }
        else {
            result = parts[0] + "=?";
            for (i = 1; i < parts.length; i++)
                result += " AND " + parts[i] + "=?";
        }

        return result;
    }

    public boolean checkUpdate(String title) {
        String sql = "SELECT value FROM updates" +
                " WHERE title=?";
        Cursor cursor = database.rawQuery(sql, new String[]{title});
        return cursor.moveToFirst();
    }

    public boolean update(String patch, String title) {
        SQLiteDatabase patch_db = SQLiteDatabase.openDatabase(patch, null,
                SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);

        String type;
        Cursor type_cursor = patch_db.rawQuery("SELECT value FROM settings" +
                " WHERE title = 'Type'", new String[] {});
        if (type_cursor.moveToFirst()) {
            type = type_cursor.getString(0);
            if (!type.equals("Patch"))
                return false;
        }

        Time now = new Time();
        now.setToNow();
        String timestamp = now.format("%Y-%m-%dT%H:%M:%SZ");

        String action, sql;
        Coin coin;
        Cursor patch_cursor = patch_db.rawQuery("SELECT action, src_id, dst_id FROM patches",
                new String[] {});
        while (patch_cursor.moveToNext()) {
            action = patch_cursor.getString(0);
            if (action.equals("add")) {
                Long id = patch_cursor.getLong(1);

                sql = "SELECT id, title, value, unit, year, country, mintmark, mintage, series, subjectshort," +
                        " quality, material, variety, obversevar, reversevar, edgevar, image, issuedate FROM coins" +
                        " WHERE id=?";
                Cursor cursor = patch_db.rawQuery(sql, new String[] {id.toString()});
                if (!cursor.moveToFirst())
                    return false;
                coin = new Coin(cursor);
                coin.image = cursor.getBlob(Coin.IMAGE_COLUMN);

                Cursor extra_cursor = patch_db.rawQuery("SELECT subject, issuedate, mint," +
                        " obverseimg.image AS obverseimg, reverseimg.image AS reverseimg FROM coins" +
                        " LEFT JOIN photos AS obverseimg ON coins.obverseimg = obverseimg.id" +
                        " LEFT JOIN photos AS reverseimg ON coins.reverseimg = reverseimg.id" +
                        " WHERE coins.id = ?", new String[]{Long.toString(coin.getId())});
                if (extra_cursor.moveToFirst())
                    coin.addExtra(extra_cursor);

                ContentValues values = new ContentValues();
                values.put("image", coin.obverse_image);
                long obvere_id = database.insert("photos", null, values);
                if (obvere_id < 0)
                    return false;

                values = new ContentValues();
                values.put("image", coin.reverse_image);
                long revere_id = database.insert("photos", null, values);
                if (revere_id < 0)
                    return false;

                values = new ContentValues();
                values.put("status", "demo");
                values.put("updatedat", timestamp);
                values.put("createdat", timestamp);
                values.put("image", coin.image);
                values.put("obverseimg", obvere_id);
                values.put("reverseimg", revere_id);

                if (!coin.title.isEmpty())
                    values.put("title", coin.title);
                if (!coin.subject_short.isEmpty())
                    values.put("subjectshort", coin.subject_short);
                if (!coin.series.isEmpty())
                    values.put("series", coin.series);
                if (coin.value != 0)
                    values.put("value", coin.value);
                if (!coin.country.isEmpty())
                    values.put("country", coin.country);
                if (!coin.unit.isEmpty())
                    values.put("unit", coin.unit);
                if (coin.year != 0)
                    values.put("year", coin.year);
                if (!coin.mintmark.isEmpty())
                    values.put("mintmark", coin.mintmark);
                if (!coin.mint.isEmpty())
                    values.put("mint", coin.mint);
                if (coin.mintage != 0)
                    values.put("mintage", coin.mintage);
                if (!coin.quality.isEmpty())
                    values.put("quality", coin.quality);
                if (!coin.material.isEmpty())
                    values.put("material", coin.material);
                if (!coin.variety.isEmpty())
                    values.put("variety", coin.variety);
                if (!coin.obversevar.isEmpty())
                    values.put("obversevar", coin.obversevar);
                if (!coin.reversevar.isEmpty())
                    values.put("reversevar", coin.reversevar);
                if (!coin.edgevar.isEmpty())
                    values.put("edgevar", coin.edgevar);
                if (!coin.subject.isEmpty())
                    values.put("subject", coin.subject);
                if (!coin.date.isEmpty())
                    values.put("issuedate", coin.date);

                database.insert("coins", null, values);
            }
            else if (action.equals("update_img")) {
                Long id = patch_cursor.getLong(1);

                sql = "SELECT title, series, subjectshort, coins.image AS image," +
                        " obverseimg.image AS obverseimg, reverseimg.image AS reverseimg FROM coins" +
                        " LEFT JOIN photos AS obverseimg ON coins.obverseimg = obverseimg.id" +
                        " LEFT JOIN photos AS reverseimg ON coins.reverseimg = reverseimg.id" +
                        " WHERE coins.id=?";

                Cursor cursor = patch_db.rawQuery(sql, new String[] {id.toString()});
                if (!cursor.moveToFirst())
                    return false;

                ContentValues values = new ContentValues();
                values.put("image", cursor.getBlob(4));
                long obvere_id = database.insert("photos", null, values);
                if (obvere_id < 0)
                    return false;

                values = new ContentValues();
                values.put("image", cursor.getBlob(5));
                long revere_id = database.insert("photos", null, values);
                if (revere_id < 0)
                    return false;

                values = new ContentValues();
                values.put("updatedat", timestamp);
                values.put("image", cursor.getBlob(3));
                values.put("obverseimg", obvere_id);
                values.put("reverseimg", revere_id);

                // TODO: Remove old images

                database.update("coins", values, "title=? AND series=? AND subjectshort=?",
                        new String[] {cursor.getString(0), cursor.getString(1), cursor.getString(2)});
            }
            else if (action.equals("update_desc")) {
                Long src_id = patch_cursor.getLong(1);
                Long dst_id = patch_cursor.getLong(2);

                sql = "SELECT title, series, subjectshort FROM coins" +
                        " WHERE coins.id=?";
                Cursor src_cursor = patch_db.rawQuery(sql, new String[] {src_id.toString()});
                if (!src_cursor.moveToFirst())
                    return false;

                sql = "SELECT title, series, subjectshort, obversedesign, reversedesign, subject" +
                        " FROM coins" +
                        " WHERE coins.id=?";

                Cursor cursor = patch_db.rawQuery(sql, new String[] {dst_id.toString()});
                if (!cursor.moveToFirst())
                    return false;

                ContentValues values = new ContentValues();
                values.put("updatedat", timestamp);
                values.put("title", cursor.getString(0));
                values.put("series", cursor.getString(1));
                values.put("subjectshort", cursor.getString(2));
                values.put("obversedesign", cursor.getString(3));
                values.put("reversedesign", cursor.getString(4));
                values.put("subject", cursor.getString(5));

                database.update("coins", values, "title=? AND series=? AND subjectshort=?",
                        new String[] {src_cursor.getString(0), src_cursor.getString(1), src_cursor.getString(2)});
            }
        }

        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("value", timestamp);
        database.insert("updates", null, values);

        patch_db.close();

        return true;
    }

    private static class StatisticsEntry {
        private final String title;
        private final Integer total;
        private final Integer collected;

        private StatisticsEntry(String title, int collected, int total) {
            this.title = title;
            this.total = total;
            this.collected = collected;
        }

        private StatisticsEntry(String title, int collected) {
            this.title = title;
            this.total = null;
            this.collected = collected;
        }

        public String getTitle() {
            return title;
        }

        public String getCount() {
            if (total == null)
                return collected.toString();
            else
                return collected.toString() + " / " + total.toString();
        }
    }

    public class StatisticsListAdapter extends ArrayAdapter<StatisticsEntry> {
        private final Context context;
        private final List values;

        public StatisticsListAdapter(Context context, List values) {
            super(context, R.layout.statistics_item, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (null == convertView) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.statistics_item, null);
            }

            StatisticsEntry entry = (StatisticsEntry)values.get(position);

            TextView title = (TextView) convertView.findViewById(R.id.title);
            title.setText(entry.getTitle());
            TextView description = (TextView) convertView.findViewById(R.id.count);
            description.setText(entry.getCount());

            return convertView;
        }
    }

    public StatisticsListAdapter getStatisticsAdapter(Context context) {
        List<StatisticsEntry> list = new ArrayList<>();
        StatisticsEntry empty_entry = null;
        int total, collected;

        Resources res = context.getResources();

        String sql = "SELECT " + filter_field + " FROM coins" +
                " GROUP BY " + filter_field +
                " ORDER BY " + filter_field + " ASC";
        Cursor group_cursor = database.rawQuery(sql, new String[]{});
        while(group_cursor.moveToNext()) {
            if (group_cursor.isNull(0))
                filter = "";
            else {
                if (filter_field.contains(","))
                    filter = group_cursor.getString(1) + ' ' + group_cursor.getString(0);
                else
                    filter = group_cursor.getString(0);
            }

            collected = getCollectedCount(filter);
            total = getTotalCount(filter);

            if (filter.isEmpty()) {
                if (filter_field.equals("series"))
                    filter = res.getString(R.string.filter_empty_series);
                else
                    filter = res.getString(R.string.filter_empty);

                empty_entry = new StatisticsEntry(filter, collected, total);
            }
            else
                list.add(new StatisticsEntry(filter, collected, total));
        }
        group_cursor.close();

        if (empty_entry != null)
            list.add(empty_entry);

        String title;
        switch (filter_field) {
            case "series":
                title = res.getString(R.string.filter_all_series);
                break;
            case "country":
                title = res.getString(R.string.filter_all_countries);
                break;
            default:
                title = res.getString(R.string.filter_all);
                break;
        }
        list.add(0, new StatisticsEntry(title, getCollectedCount(), getTotalCount()));

        list.add(new StatisticsEntry(res.getString(R.string. coins_count), getCoinsCount()));

        return new StatisticsListAdapter(context, list);
    }
}
