package org.opennumismat.uscommemoratives;

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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by v.ignatov on 20.10.2014.
 */
public class SqlAdapter extends BaseAdapter {
    private static final int DB_VERSION = 1;
    public static final int GRADE_UNC = 60;
    public static final int GRADE_AU = 55;
    public static final int GRADE_XF = 45;
    public static final int GRADE_VF = 35;
    public static final int GRADE_F = 15;
    public static final int GRADE_DEFAULT = GRADE_XF;
    public static final String DEFAULT_FILTER = "series";

    private int version;
    private Cursor cursor;
    private SQLiteDatabase database;
    private Context context;
    private SharedPreferences pref;
    private List<String> filters;
    private String filter;
    private String filter_field;
    private int main_filter_id = R.id.filter_all;

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
            if (coin.grade >= 60)       // Unc
                back.setColor(context.getResources().getColor(R.color.unc));
            else if (coin.grade >= 50)  // AU
                back.setColor(context.getResources().getColor(R.color.au));
            else if (coin.grade >= 40)  // XF
                back.setColor(context.getResources().getColor(R.color.xf));
            else if (coin.grade >= 25)  // VF
                back.setColor(context.getResources().getColor(R.color.vf));
            else
                back.setColor(context.getResources().getColor(R.color.f));
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
        imageView.setImageBitmap(coin.getImageBitmap());

        LinearLayout count_layout = (LinearLayout) rowView.findViewById(R.id.CountLayout);
        count_layout.setOnClickListener(new OnClickListener(coin));

        return rowView;
    }

    private class OnClickListener implements
            View.OnClickListener {

        private Coin coin;
        private int selected_grade;
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
                grade = new Grading(GRADE_UNC, res.getString(R.string.Unc), res.getString(R.string.uncirculated));
                grade.count = coin.count_unc;
                grading_adapter.add(grade);
                grade = new Grading(GRADE_AU, res.getString(R.string.AU), res.getString(R.string.about_uncirculated));
                grade.count = coin.count_au;
                grading_adapter.add(grade);
                grade = new Grading(GRADE_XF, res.getString(R.string.XF), res.getString(R.string.extremely_fine));
                grade.count = coin.count_xf;
                grading_adapter.add(grade);
                grade = new Grading(GRADE_VF, res.getString(R.string.VF), res.getString(R.string.very_fine));
                grade.count = coin.count_vf;
                grading_adapter.add(grade);
                grade = new Grading(GRADE_F, res.getString(R.string.F), res.getString(R.string.fine));
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
                selected_grade = GRADE_DEFAULT;
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
        String sql_filter = "";
        String sql_mint = "";
        String sql_part = "";

        if (filter != null)
            sql_filter = makeFilter(filter.isEmpty(), filter_field);
        if (!pref.getBoolean("use_mint", false))
            sql_mint = "mintmark != 'P'";
        if (!sql_filter.isEmpty() || !sql_mint.isEmpty())
            sql_part += " WHERE ";
        sql_part += sql_filter;
        if (!sql_filter.isEmpty() && !sql_mint.isEmpty())
            sql_part += " AND ";
        sql_part += sql_mint;

        ArrayList<String> params = new ArrayList<>();
        String sql = "SELECT COUNT(id) FROM descriptions";
        sql += sql_part;
        if (filter != null && !filter.isEmpty()) {
            params.add(filter);
        }
        String[] params_arr = new String[params.size()];
        params_arr = params.toArray(params_arr);

        Cursor cursor = database.rawQuery(sql, params_arr);
        int count = 0;
        if(cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();

        return count;
    }

    public int getTotalCount() {
        return getTotalCount(null);
    }

    public int getCollectedCount(String filter) {
        String sql_filter = "";
        String sql_mint = "";
        String sql_part = "";

        if (filter != null)
            sql_filter = makeFilter(filter.isEmpty(), filter_field);
        if (!pref.getBoolean("use_mint", false))
            sql_mint = "mintmark != 'P'";
        if (!sql_filter.isEmpty() || !sql_mint.isEmpty())
            sql_part += " WHERE ";
        sql_part += sql_filter;
        if (!sql_filter.isEmpty() && !sql_mint.isEmpty())
            sql_part += " AND ";
        sql_part += sql_mint;

        ArrayList<String> params = new ArrayList<>();
        String sql = "SELECT coins.id FROM coins" +
                " LEFT JOIN descriptions ON descriptions.id=coins.description_id";
        sql += sql_part;
        if (filter != null && !filter.isEmpty()) {
            params.add(filter);
        }
        sql += " GROUP BY descriptions.id";
        String[] params_arr = new String[params.size()];
        params_arr = params.toArray(params_arr);

        Cursor cursor = database.rawQuery(sql, params_arr);
        int count = cursor.getCount();
        cursor.close();

        return count;
    }

    public int getCollectedCount() {
        return getCollectedCount(null);
    }

    public int getCoinsCount() {
        String sql = "SELECT COUNT(id) FROM coins";
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
            Coin coin = new Coin(cursor, pref.getBoolean("use_mint", false));
            coin.count = getCoinsCount(coin);
            if (coin.count > 0) {
                coin = getCoinsGrade(coin);
            }
            coin.image = cursor.getBlob(Coin.IMAGE_COLUMN);
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
            Coin coin = new Coin(cursor, pref.getBoolean("use_mint", false));

            return fillExtra(coin);
        } else {
            throw new CursorIndexOutOfBoundsException(
                    "Can't move cursor to position");
        }
    }

    private Coin fillExtra(Coin coin) {
        Cursor extra_cursor = database.rawQuery("SELECT subject, issuedate, mint, material," +
                " obverseimg.image AS obverseimg, reverseimg.image AS reverseimg" +
                " FROM descriptions" +
                " LEFT JOIN photos AS obverseimg ON descriptions.obverseimg = obverseimg.id" +
                " LEFT JOIN photos AS reverseimg ON descriptions.reverseimg = reverseimg.id" +
                " WHERE descriptions.id = ?", new String[]{Long.toString(coin.getId())});
        if (extra_cursor.moveToFirst())
            coin.addExtra(extra_cursor);

        return coin;
    }

    private void addCoin(Coin coin, int count, int grade) {
        Time now = new Time();
        now.setToNow();
        String timestamp = now.format("%Y-%m-%dT%H:%M:%SZ");

        int i;
        for (i = 0; i < count; i++) {
            ContentValues values = new ContentValues();
            values.put("description_id", coin.getId());
            values.put("grade", grade);
            values.put("createdat", timestamp);

            database.insert("coins", null, values);
        }
    }

    private void removeCoin(Coin coin, int count, int grade) {
        ArrayList<String> params = new ArrayList<>();
        params.add(Long.toString(coin.getId()));
        String sql_grade = "";
        if (pref.getBoolean("use_grading", false)) {
            sql_grade = " AND grade=?";
            params.add(Integer.toString(grade));
        }
        params.add(Integer.toString(count));

        String[] params_arr = new String[params.size()];
        params_arr = params.toArray(params_arr);
        database.delete("coins",
                "id IN (SELECT id FROM coins WHERE description_id=? " + sql_grade + " ORDER BY id DESC LIMIT ?)",
                params_arr);
    }

    //Методы для работы с базой данных

    public Cursor getAllEntries() {
        String sql;
        String sql_main_filter = "";
        String sql_filter = "";
        String sql_mint = "";
        String sql_part = "";
        String order;

        switch (main_filter_id) {
            case R.id.filter_present:
            case R.id.filter_for_change:
            case R.id.filter_not_unc:
                sql_main_filter = "coins.id NOT NULL";
                break;
            case R.id.filter_need:
                sql_main_filter = "coins.id IS NULL";
                break;
        }

        if (filter != null)
            sql_filter = makeFilter(filter.isEmpty(), filter_field);

        if (!pref.getBoolean("use_mint", false))
            sql_mint = "mintmark != 'P'";

        if (!sql_main_filter.isEmpty()) {
            sql_part += sql_main_filter;
        }
        if (!sql_filter.isEmpty()) {
            if (!sql_part.isEmpty())
                sql_part += " AND ";
            sql_part += sql_filter;
        }
        if (!sql_mint.isEmpty()) {
            if (!sql_part.isEmpty())
                sql_part += " AND ";
            sql_part += sql_mint;
        }
        if (!sql_part.isEmpty()) {
            sql_part = " WHERE " + sql_part;
        }


        if (pref.getString("sort_order", "0").equals("0"))
            order = "ASC";
        else
            order = "DESC";

        ArrayList<String> params = new ArrayList<>();
        if (filter != null && !filter.isEmpty()) {
            params.add(filter);
        }
        String[] params_arr = new String[params.size()];
        params_arr = params.toArray(params_arr);

        if (main_filter_id == R.id.filter_all) {
            sql = "SELECT year, COUNT(id) FROM descriptions" +
                    sql_part +
                    " GROUP BY year" +
                    " ORDER BY year " + order;
        }
        else if (main_filter_id == R.id.filter_not_unc) {
            sql = "SELECT year, COUNT(id) FROM (SELECT year, grade, descriptions.id FROM descriptions" +
                    " LEFT OUTER JOIN coins ON descriptions.id = coins.description_id" +
                    sql_part +
                    " GROUP BY descriptions.id ORDER BY grade ASC)" +
                    " WHERE grade < 60" +
                    " GROUP BY year" +
                    " ORDER BY year " + order;
        }
        else if (main_filter_id == R.id.filter_for_change) {
            sql = "SELECT year, COUNT(id) FROM (SELECT year, grade, descriptions.id, COUNT(coins.id) AS coins_count FROM descriptions" +
                    " LEFT OUTER JOIN coins ON descriptions.id = coins.description_id" +
                    sql_part +
                    " GROUP BY descriptions.id ORDER BY grade ASC)" +
                    " WHERE coins_count > 1" +
                    " GROUP BY year" +
                    " ORDER BY year " + order;
        }
        else {
            sql = "SELECT year, COUNT(id) FROM (SELECT year, descriptions.id FROM descriptions" +
                    " LEFT OUTER JOIN coins ON descriptions.id = coins.description_id" +
                    sql_part +
                    " GROUP BY descriptions.id)" +
                    " GROUP BY year" +
                    " ORDER BY year " + order;
        }
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
        if (main_filter_id == R.id.filter_all) {
            sql = "SELECT descriptions.id, title, unit, year, country, mintmark, mintage, series, subjectshort," +
                    "photos.image FROM descriptions INNER JOIN photos ON descriptions.image=photos.id" +
                    sql_part +
                    " ORDER BY year " + order + ", issuedate " + order + ", descriptions.id ASC";
        }
        else if (main_filter_id == R.id.filter_not_unc) {
            sql = "SELECT descriptions_id, title, unit, year, country, mintmark, mintage, series, subjectshort," +
                    "photos.image FROM (" +
                    "SELECT descriptions.id AS descriptions_id, title, unit, year, country, mintmark, mintage, series, subjectshort," +
                    " image AS image_id, grade, issuedate FROM descriptions" +
                    " LEFT OUTER JOIN coins ON descriptions.id = coins.description_id" +
                    sql_part +
                    " GROUP BY descriptions.id" +
                    " ORDER BY grade ASC)" +
                    " INNER JOIN photos ON image_id=photos.id" +
                    " WHERE grade < 60" +
                    " ORDER BY year " + order + ", issuedate " + order + ", descriptions_id ASC";
        }
        else if (main_filter_id == R.id.filter_for_change) {
            sql = "SELECT descriptions_id, title, unit, year, country, mintmark, mintage, series, subjectshort," +
                    "photos.image FROM (" +
                    "SELECT descriptions.id AS descriptions_id, title, unit, year, country, mintmark, mintage, series, subjectshort," +
                    " image AS image_id, issuedate, COUNT(coins.id) AS coins_count FROM descriptions" +
                    " LEFT OUTER JOIN coins ON descriptions.id = coins.description_id" +
                    sql_part +
                    " GROUP BY descriptions.id)" +
                    " INNER JOIN photos ON image_id=photos.id" +
                    " WHERE coins_count > 1" +
                    " ORDER BY year " + order + ", issuedate " + order + ", descriptions_id ASC";
        }
        else {
            sql = "SELECT descriptions.id, title, unit, year, country, mintmark, mintage, series, subjectshort," +
                    " photos.image FROM descriptions INNER JOIN photos ON descriptions.image=photos.id" +
                    " LEFT OUTER JOIN coins ON descriptions.id = coins.description_id" +
                    sql_part +
                    " GROUP BY descriptions.id" +
                    " ORDER BY year " + order + ", issuedate " + order + ", descriptions.id ASC";
        }
        return database.rawQuery(sql, params_arr);
    }

    public List<String> getFilters() {
        if (filters == null) {
            Cursor group_cursor = database.rawQuery("SELECT " + filter_field + " FROM descriptions" +
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

    public void setMainFilter(int id) {
        if (main_filter_id == id)
            return;

        main_filter_id = id;

        refresh();
    }

    public int getMainFilter() {
        return main_filter_id;
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

        String type = "unknown";
        Cursor type_cursor = database.rawQuery("SELECT value FROM settings" +
                " WHERE title = 'Type'", new String[] {});
        if (type_cursor.moveToFirst()) {
            type = type_cursor.getString(0);
        }
        type_cursor.close();
        if (!type.equals("MobilePro")) {
            throw new SQLiteException("Wrong DB format");
        }

        Cursor version_cursor = database.rawQuery("SELECT value FROM settings" +
                " WHERE title = 'Version'", new String[] {});
        if (version_cursor.moveToFirst()) {
            version = Integer.parseInt(version_cursor.getString(0));
            if (version > DB_VERSION) {
                Toast toast = Toast.makeText(
                        context, R.string.new_db_version, Toast.LENGTH_LONG
                );
                toast.show();
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
        String sql = "SELECT COUNT(*) FROM coins WHERE description_id=?";
        Cursor extra_cursor = database.rawQuery(sql, new String[]{Long.toString(coin.getId())});

        if (extra_cursor.moveToFirst())
            return extra_cursor.getLong(0);
        else
            return 0;
    }

    private Coin getCoinsGrade(Coin coin) {
        int grade = GRADE_DEFAULT;

        if (pref.getBoolean("use_grading", false)) {
            coin.count_unc = coin.count_au = coin.count_xf = coin.count_vf = coin.count_f = 0;

            String sql = "SELECT grade, COUNT(grade) FROM coins WHERE description_id=? GROUP BY grade ORDER BY grade ASC";
            Cursor grading_cursor = database.rawQuery(sql, new String[]{Long.toString(coin.getId())});
            while(grading_cursor.moveToNext()) {
                if (grading_cursor.isNull(0) || grading_cursor.getInt(0) == 0) {
                    coin.count_xf += grading_cursor.getInt(1);
                    continue;
                }

                grade = grading_cursor.getInt(0);
                if (grade >= 60)       // Unc
                    coin.count_unc += grading_cursor.getInt(1);
                else if (grade >= 50)  // AU
                    coin.count_au += grading_cursor.getInt(1);
                else if (grade >= 40)  // XF
                    coin.count_xf += grading_cursor.getInt(1);
                else if (grade >= 25)  // VF
                    coin.count_vf += grading_cursor.getInt(1);
                else
                    coin.count_f += grading_cursor.getInt(1);
            }
            grading_cursor.close();
        }

        coin.grade = grade;

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

                sql = "SELECT id, title, unit, year, country, mintmark, mintage, series, subjectshort," +
                        " material, image, issuedate FROM descriptions" +
                        " WHERE id=?";
                Cursor cursor = patch_db.rawQuery(sql, new String[] {id.toString()});
                if (!cursor.moveToFirst())
                    return false;
                coin = new Coin(cursor, true);
                coin.image = cursor.getBlob(Coin.IMAGE_COLUMN);

                Cursor extra_cursor = patch_db.rawQuery("SELECT subject, issuedate, mint," +
                        " obverseimg.image AS obverseimg, reverseimg.image AS reverseimg FROM descriptions" +
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
                values.put("image", coin.image);
                values.put("obverseimg", obvere_id);
                values.put("reverseimg", revere_id);

                if (!coin.title.isEmpty())
                    values.put("title", coin.title);
                if (!coin.subject_short.isEmpty())
                    values.put("subjectshort", coin.subject_short);
                if (!coin.series.isEmpty())
                    values.put("series", coin.series);
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
                if (!coin.material.isEmpty())
                    values.put("material", coin.material);
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
                long obverse_id = database.insert("photos", null, values);
                if (obverse_id < 0)
                    return false;

                values = new ContentValues();
                values.put("image", cursor.getBlob(5));
                long reverse_id = database.insert("photos", null, values);
                if (reverse_id < 0)
                    return false;

                values = new ContentValues();
                values.put("image", cursor.getBlob(3));
                values.put("obverseimg", obverse_id);
                values.put("reverseimg", reverse_id);

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

                sql = "SELECT title, series, subjectshort, subject" +
                        " FROM coins" +
                        " WHERE coins.id=?";

                Cursor cursor = patch_db.rawQuery(sql, new String[] {dst_id.toString()});
                if (!cursor.moveToFirst())
                    return false;

                ContentValues values = new ContentValues();
                values.put("title", cursor.getString(0));
                values.put("series", cursor.getString(1));
                values.put("subjectshort", cursor.getString(2));
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

        String sql = "SELECT " + filter_field + " FROM descriptions" +
                " GROUP BY " + filter_field +
                " ORDER BY " + filter_field + " ASC";
        Cursor group_cursor = database.rawQuery(sql, new String[]{});
        String st_filter;
        while(group_cursor.moveToNext()) {
            if (group_cursor.isNull(0))
                st_filter = "";
            else {
                st_filter = group_cursor.getString(0);
            }

            collected = getCollectedCount(st_filter);
            total = getTotalCount(st_filter);

            if (st_filter.isEmpty()) {
                if (filter_field.equals("series"))
                    st_filter = res.getString(R.string.filter_empty_series);
                else
                    st_filter = res.getString(R.string.filter_empty);

                empty_entry = new StatisticsEntry(st_filter, collected, total);
            }
            else
                list.add(new StatisticsEntry(st_filter, collected, total));
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
