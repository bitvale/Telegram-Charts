package com.bitvale.chartview;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 16-Mar-19
 */
public class Chart implements Parcelable {

    ArrayList<Column> columns;

    public Chart(ArrayList<Column> columns) {
        this.columns = columns;
    }

    public Chart(Parcel in) {
        in.readList(columns, Column.class.getClassLoader());
    }

    public static final Creator<Chart> CREATOR = new Creator<Chart>() {
        @Override
        public Chart createFromParcel(Parcel in) {
            return new Chart(in);
        }

        @Override
        public Chart[] newArray(int size) {
            return new Chart[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(columns);
    }

    public static class Column implements Parcelable {
        String name;
        Type type;
        String color;
        ArrayList<Long> values;
        boolean enabled;
        ChartAnimation animation;

        public Column(String name, Type type, String color, ArrayList<Long> values) {
            this.name = name;
            this.type = type;
            this.color = color;
            this.values = values;
            enabled = true;
            animation = ChartAnimation.NONE;
        }

        public Column(Parcel in) {
            name = in.readString();
            type = Type.valueOf(in.readString());
            color = in.readString();
            in.readList(values, Column.class.getClassLoader());
            enabled = in.readInt() != 0;
            animation = ChartAnimation.values()[in.readInt()];
        }

        public static final Creator<Column> CREATOR = new Creator<Column>() {
            @Override
            public Column createFromParcel(Parcel in) {
                return new Column(in);
            }

            @Override
            public Column[] newArray(int size) {
                return new Column[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(name);
            dest.writeString(type.getValue());
            dest.writeString(color);
            dest.writeList(values);
            dest.writeInt(enabled ? 1 : 0);
            dest.writeInt(animation.ordinal());
        }
    }

    public static class ChartSelectedData {
        public long date;
        public ArrayList<Data> values;

        public ChartSelectedData(long date, ArrayList<Data> values) {
            this.date = date;
            this.values = values;
        }
    }

    public static class Data {
        public long value;
        public String color;

        public Data(long value, String color) {
            this.value = value;
            this.color = color;
        }
    }


    public enum Type {
        LINE("line"),
        X("x");

        private String val;

        Type(String v) {
            val = v;
        }

        public String getValue() {
            return val;
        }
    }

    public enum ChartAnimation {
        UP,
        DOWN,
        NONE
    }
}