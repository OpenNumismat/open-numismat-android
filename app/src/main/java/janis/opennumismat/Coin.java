package janis.opennumismat;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by v.ignatov on 20.10.2014.
 */
public class Coin implements Parcelable {
    private static final int ID_COLUMN = 0;
    private static final int TITLE_COLUMN = 1;
    private static final int VALUE_COLUMN = 2;
    private static final int UNIT_COLUMN = 3;
    private static final int YEAR_COLUMN = 4;
    private static final int COUNTRY_COLUMN = 5;
    private static final int MINTMARK_COLUMN = 6;
    private static final int MINTAGE_COLUMN = 7;
    private static final int SERIES_COLUMN = 8;
    private static final int IMAGE_COLUMN = 9;

    private static final int SUBJECT_EX_COLUMN = 0;

    private long id;
    private String title;
    private long value;
    private String unit;
    private String country;
    private long year;
    private String mintmark;
    private long mintage;
    private String series;
    private byte[] image;
    private String subject;

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
        image = cursor.getBlob(IMAGE_COLUMN);
    }

    private Coin(Parcel in) {
        id = in.readLong();
        title = in.readString();
        value = in.readLong();
        unit = in.readString();
        country = in.readString();
        year = in.readLong();
        mintmark = in.readString();
        mintage = in.readLong();
        series = in.readString();
        subject = in.readString();
    }

    public static final Parcelable.Creator<Coin> CREATOR
            = new Parcelable.Creator<Coin>() {
        public Coin createFromParcel(Parcel in) {
            return new Coin(in);
        }

        public Coin[] newArray(int size) {
            return new Coin[size];
        }
    };

    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(id);
        out.writeString(title);
        out.writeLong(value);
        out.writeString(unit);
        out.writeString(country);
        out.writeLong(year);
        out.writeString(mintmark);
        out.writeLong(mintage);
        out.writeString(series);
        out.writeString(subject);
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
    public String getCountry() {
        return country;
    }
    public String getSubject() {
        return subject;
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

    public Bitmap getImageBitmap() {
        return BitmapFactory.decodeByteArray(image, 0, image.length);
    }

    public void addExtra(Cursor cursor) {
        subject = cursor.getString(SUBJECT_EX_COLUMN);
    }

    @Override
    public String toString() {
        return title;
    }
}
