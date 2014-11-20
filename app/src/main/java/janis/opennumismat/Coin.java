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
    public static final int IMAGE_COLUMN = 11;

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
    public byte[] image;
    private String subject;
    public String subject_short;
    public String material;
    public String date;
    public byte[] obverse_image;
    public byte[] reverse_image;
    public String quality;
    public String grade;
    public long count;
    public int count_unc;
    public int count_au;
    public int count_xf;
    public int count_vf;
    public int count_f;

    public Coin(Cursor cursor) {
        id = cursor.getLong(ID_COLUMN);
        if (cursor.isNull(TITLE_COLUMN))
            title = "";
        else
            title = cursor.getString(TITLE_COLUMN);
        if (cursor.isNull(VALUE_COLUMN))
            value = 0;
        else
            value = cursor.getLong(VALUE_COLUMN);
        if (cursor.isNull(UNIT_COLUMN))
            unit = "";
        else
            unit = cursor.getString(UNIT_COLUMN);
        if (cursor.isNull(COUNTRY_COLUMN))
            country = "";
        else
            country = cursor.getString(COUNTRY_COLUMN);
        if (cursor.isNull(YEAR_COLUMN))
            year = 0;
        else
            year = cursor.getLong(YEAR_COLUMN);
        if (cursor.isNull(MINTMARK_COLUMN))
            mintmark = "";
        else
            mintmark = cursor.getString(MINTMARK_COLUMN);
        if (cursor.isNull(MINTAGE_COLUMN))
            mintage = 0;
        else
            mintage = cursor.getLong(MINTAGE_COLUMN);
        if (cursor.isNull(SERIES_COLUMN))
            series = "";
        else
            series = cursor.getString(SERIES_COLUMN);
        if (cursor.isNull(SUBJECT_SHORT_COLUMN))
            subject_short = "";
        else
            subject_short = cursor.getString(SUBJECT_SHORT_COLUMN);
        if (cursor.isNull(QUALITY_COLUMN))
            quality = "";
        else
            quality = cursor.getString(QUALITY_COLUMN);
    }

    private Coin(Parcel in) {
        int length;

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

        length = in.readInt();
        if (length > 0) {
            obverse_image = new byte[length];
            in.readByteArray(obverse_image);
        }
        length = in.readInt();
        if (length > 0) {
            reverse_image = new byte[length];
            in.readByteArray(reverse_image);
        }
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
        if (obverse_image != null) {
            out.writeInt(obverse_image.length);
            out.writeByteArray(obverse_image);
        }
        else {
            out.writeInt(0);
        }
        if (reverse_image != null) {
            out.writeInt(reverse_image.length);
            out.writeByteArray(reverse_image);
        }
        else {
            out.writeInt(0);
        }
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
        if (mintage > 0)
            return String.format(Locale.getDefault(), "%,d", mintage);
        else
            return "";
    }
    public String getDenomination() {
        if (value > 0 && !unit.isEmpty())
            return Long.toString(value) + ' ' + unit.toLowerCase();
        else if (value > 0)
            return Long.toString(value);
        else
            return unit.toLowerCase();
    }
    public String getYear() {
        if (year > 0)
            return Long.toString(year);
        else
            return "";
    }
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
        if (image != null)
            return BitmapFactory.decodeByteArray(image, 0, image.length);
        else
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    }
    public Bitmap getObverseImageBitmap() {
        if (obverse_image != null)
            return BitmapFactory.decodeByteArray(obverse_image, 0, obverse_image.length);
        else
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    }
    public Bitmap getReverseImageBitmap() {
        if (reverse_image != null)
            return BitmapFactory.decodeByteArray(reverse_image, 0, reverse_image.length);
        else
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    }

    public void addExtra(Cursor cursor) {
        if (cursor.isNull(SUBJECT_EX_COLUMN))
            subject = "";
        else
            subject = cursor.getString(SUBJECT_EX_COLUMN);
        if (cursor.isNull(MATERIAL_EX_COLUMN))
            material = "";
        else
            material = cursor.getString(MATERIAL_EX_COLUMN);
        if (cursor.isNull(DATE_EX_COLUMN))
            date = "";
        else
            date = cursor.getString(DATE_EX_COLUMN);
        obverse_image = cursor.getBlob(OBVERSE_IMAGE_EX_COLUMN);
        reverse_image = cursor.getBlob(REVERSE_IMAGE_EX_COLUMN);
    }

    @Override
    public String toString() {
        return title;
    }
}
