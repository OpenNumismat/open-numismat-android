package janis.opennumismat;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by v.ignatov on 20.10.2014.
 */
public class SqlAdapter extends BaseAdapter {
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
    private static final String KEY_IMAGE = "image";

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
        Coin nameOnPosition = getItem(position);
        return nameOnPosition.getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView;
        if (null == convertView) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_item, null);
        }

        Coin coin = getItem(position);
        TextView title = (TextView) convertView.findViewById(R.id.title);
        title.setText(coin.getTitle());
        TextView description = (TextView) convertView.findViewById(R.id.description);
        description.setText(coin.getDescription());
        ImageView imageView = (ImageView) convertView.findViewById(R.id.coin_image);
        imageView.setImageBitmap(coin.getImageBitmap());

        return convertView;
    }

    @Override
    public int getCount() {
        return cursor.getCount();
    }

    @Override
    public Coin getItem(int position) {
        if (cursor.moveToPosition(position)) {
            Coin coin = new Coin(cursor);
            return coin;
        } else {
            throw new CursorIndexOutOfBoundsException(
                    "Cant move cursor to postion");
        }
    }

    //Методы для работы с базой данных

    public Cursor getAllEntries() {
        //Список колонок базы, которые следует включить в результат
        String[] columnsToTake = { KEY_ID, KEY_TITLE, KEY_VALUE, KEY_UNIT, KEY_YEAR, KEY_COUNTRY, KEY_MINTMARK, KEY_MINTAGE, KEY_SERIES, KEY_IMAGE };
        // составляем запрос к базе
        return database.query(TABLE_NAME, columnsToTake,
                null, null, null, null, KEY_ID);
    }

    //Прочие служебные методы

    public void onDestroy() {
//        dbOpenHelper.close();
    }

    //Вызывает обновление вида
    private void refresh() {
        cursor = getAllEntries();
        notifyDataSetChanged();
    }

    // Инициализация адаптера: открываем базу и создаем курсор
    private void init(String path) {
        database = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READWRITE);
        cursor = getAllEntries();
    }
}
