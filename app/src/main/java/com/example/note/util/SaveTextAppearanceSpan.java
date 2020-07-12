package com.example.note.util;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.style.TextAppearanceSpan;

import java.util.ArrayList;

public class SaveTextAppearanceSpan implements Parcelable {
    private ArrayList<byte[]> spansList;
    private ArrayList<Integer> spanStartList;
    private ArrayList<Integer> spanEndList;

    public SaveTextAppearanceSpan() {

    }
    public SaveTextAppearanceSpan(ArrayList<byte[]> arrayList, ArrayList<Integer> spanStartList, ArrayList<Integer> spanEndList) {
        this.spansList = arrayList;
        this.spanStartList = spanStartList;
        this.spanEndList = spanEndList;
    }

    public ArrayList<byte[]> getSpansList() {
        return spansList;
    }

    public void setSpansList(ArrayList<byte[]> spansList) {
        this.spansList = spansList;
    }

    public ArrayList<Integer> getSpanStartList() {
        return spanStartList;
    }

    public void setSpanStartList(ArrayList<Integer> spanStartList) {
        this.spanStartList = spanStartList;
    }

    public ArrayList<Integer> getSpanEndList() {
        return spanEndList;
    }

    public void setSpanEndList(ArrayList<Integer> spanEndList) {
        this.spanEndList = spanEndList;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(spansList);
        dest.writeList(spanStartList);
        dest.writeList(spanEndList);
    }

    public static final Parcelable.Creator<SaveTextAppearanceSpan> CREATOR = new Parcelable.Creator<SaveTextAppearanceSpan>() {
        @Override
        public SaveTextAppearanceSpan createFromParcel(Parcel source) {
            SaveTextAppearanceSpan saveTextAppearanceSpan = new SaveTextAppearanceSpan();
            saveTextAppearanceSpan.spansList = new ArrayList<>();
            saveTextAppearanceSpan.spanStartList = new ArrayList<>();
            saveTextAppearanceSpan.spanEndList = new ArrayList<>();
            source.readList(saveTextAppearanceSpan.spansList, getClass().getClassLoader());
            source.readList(saveTextAppearanceSpan.spanStartList, getClass().getClassLoader());
            source.readList(saveTextAppearanceSpan.spanEndList, getClass().getClassLoader());
            //source.readTypedList(saveTextAppearanceSpan.spansList,TextAppearanceSpan.Creator<TextAppearanceSpan>);
            return saveTextAppearanceSpan;
        }

        @Override
        public SaveTextAppearanceSpan[] newArray(int size) {
            return new SaveTextAppearanceSpan[size];
        }
    };
}
