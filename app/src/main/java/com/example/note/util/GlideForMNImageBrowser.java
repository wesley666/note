package com.example.note.util;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.note.R;
import com.maning.imagebrowserlibrary.ImageEngine;

public class GlideForMNImageBrowser implements ImageEngine {

    @Override
    public void loadImage(Context context, String url, ImageView imageView, final View progressView) {
        Glide.with(context)
                .asBitmap()
                .load(url)
                .fitCenter()
               // .placeholder(R.drawable.default_placeholder)
                .error(R.mipmap.ic_launcher)

                .into(imageView);
    }
}
