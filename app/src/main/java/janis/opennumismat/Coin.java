package janis.opennumismat;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;

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
    private static final int SUBJECT_SHORT_COLUMN = 9;
    private static final int QUALITY_COLUMN = 10;
    private static final int QUANTITY_COLUMN = 11;
    private static final int IMAGE_COLUMN = 12;

    private static final int SUBJECT_EX_COLUMN = 0;
    private static final int MATERIAL_EX_COLUMN = 1;
    private static final int DATE_EX_COLUMN = 2;
    private static final int OBVERSE_IMAGE_EX_COLUMN = 3;
    private static final int REVERSE_IMAGE_EX_COLUMN = 4;

    private long id;
    public String title;
    public long value;
    public String unit;
    public String country;
    public long year;
    public String mintmark;
    public long mintage;
    public String series;
    private byte[] image;
    private String subject;
    public String subject_short;
    public String material;
    public String date;
    private byte[] obverse_image;
    private byte[] reverse_image;
    public String quality;
    public long count;

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
        subject_short = cursor.getString(SUBJECT_SHORT_COLUMN);
        quality = cursor.getString(QUALITY_COLUMN);
        count = cursor.getLong(QUANTITY_COLUMN);
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
        subject_short = in.readString();
        material = in.readString();
        date = in.readString();
        quality = in.readString();
        count = in.readLong();
        obverse_image = new byte[in.readInt()];
        in.readByteArray(obverse_image);
        reverse_image = new byte[in.readInt()];
        in.readByteArray(reverse_image);
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
        out.writeString(subject_short);
        out.writeString(material);
        out.writeString(date);
        out.writeString(quality);
        out.writeLong(count);
        out.writeInt(obverse_image.length);
        out.writeByteArray(obverse_image);
        out.writeInt(reverse_image.length);
        out.writeByteArray(reverse_image);
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
    public String getSubjectShort() {
        return subject_short;
    }
    public String getSeries() {
        return series;
    }

    public String getDescription(Context context) {
        String desc = value + " " + unit;
        if (year > 0)
            desc += ", " + year;
        if (!mintmark.isEmpty())
            desc += ", " + mintmark;
        if (mintage > 0)
            desc += ", " + context.getString(R.string.mintage) + ": " + String.format(Locale.getDefault(), "%,d", mintage);
        if (!series.isEmpty())
            desc += ", " + series;
        return desc;
    }

    public String getMintage() {
        return String.format(Locale.getDefault(), "%,d", mintage);
    }
    public String getDenomination() { return Long.toString(value) + ' ' + unit.toLowerCase(); }
    public String getYear() { return Long.toString(year); }
    public String getMaterial() { return material; }
    public String getCount() {
        return String.format(Locale.getDefault(), "%d", count);
    }
    public String getDate(Context context) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date date_obj;
        try {
            date_obj = sdf.parse(date);
        } catch (ParseException e) {
            return date;
        }
        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
        return dateFormat.format(date_obj);
    }

    public Bitmap getImageBitmap() {
        return BitmapFactory.decodeByteArray(image, 0, image.length);
    }
    public Bitmap getObverseImageBitmap() {
        return BitmapFactory.decodeByteArray(obverse_image, 0, obverse_image.length);
    }
    public Bitmap getReverseImageBitmap() {
        return BitmapFactory.decodeByteArray(reverse_image, 0, reverse_image.length);
    }

    public void addExtra(Cursor cursor) {
        subject = cursor.getString(SUBJECT_EX_COLUMN);
        material = cursor.getString(MATERIAL_EX_COLUMN);
        date = cursor.getString(DATE_EX_COLUMN);
        obverse_image = cursor.getBlob(OBVERSE_IMAGE_EX_COLUMN);
        reverse_image = cursor.getBlob(REVERSE_IMAGE_EX_COLUMN);
    }

    @Override
    public String toString() {
        return title;
    }
}
