package janis.opennumismat;

import android.database.Cursor;

/**
 * Created by v.ignatov on 20.10.2014.
 */
public class Coin {
    private static final int ID_COLUMN = 0;
    private static final int TITLE_COLUMN = 1;
    private static final int VALUE_COLUMN = 2;
    private static final int UNIT_COLUMN = 3;
    private static final int YEAR_COLUMN = 4;
    private static final int COUNTRY_COLUMN = 5;
    private static final int MINTMARK_COLUMN = 6;
    private static final int MINTAGE_COLUMN = 7;
    private static final int SERIES_COLUMN = 8;

    private long id;
    private String title;
    private long value;
    private String unit;
    private String country;
    private long year;
    private String mintmark;
    private long mintage;
    private String series;

    public Coin(String title) {
        this.title = title;
    }

    public Coin(long id, String title) {
        this.id = id;
        this.title = title;
    }

    public Coin(Cursor cursor) {
        id = cursor.getLong(ID_COLUMN);
        title = cursor.getString(TITLE_COLUMN);
        value = cursor.getLong(VALUE_COLUMN);
        unit = cursor.getString(UNIT_COLUMN);
        country = cursor.getString(COUNTRY_COLUMN);
        year = cursor.getLong(YEAR_COLUMN);
        mintmark = cursor.getString(MINTMARK_COLUMN);
        mintage = cursor.getLong(MINTAGE_COLUMN);
        series = cursor.getString(SERIES_COLUMN);
    }

    public long getId() {
        return id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        String desc = value + " " + unit;
        if (year > 0)
            desc += ", " + year;
        if (!mintmark.isEmpty())
            desc += ", " + mintmark;
        if (mintage > 0)
            desc += ", " + "Mintage: " + mintage;
        if (!series.isEmpty())
            desc += ", " + series;
        return desc;
    }

    @Override
    public String toString() {
        return title;
    }
}
