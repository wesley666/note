package com.example.note.util;

/*
将传入的String类型的便签内容，转化为包含图片 + 可点击声音的 spannableString
这里传入 note content（string） 其中格式如下 你好，<img src=''>， <voice src=''> 经过处理后 得到一个spannableString ，将其中的img he
voice setSpan变为两个标志，之后textView .set 就会将其还原
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Parcel;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.TypefaceSpan;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;

import com.example.note.R;
import com.maning.imagebrowserlibrary.MNImageBrowser;
import com.maning.imagebrowserlibrary.listeners.OnClickListener;
import com.maning.imagebrowserlibrary.listeners.OnLongClickListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Context.MODE_PRIVATE;

public class ContentToSpannableString {
    private static final String TAG = "ContentToSpannableStr";
    private static int boldStyle;
    private static int highlightStyle;
    private static int titleStyle;
    public static class MyImageSpan extends ImageSpan {

        public MyImageSpan(Drawable drawable) {
            super(drawable);
        }

        @Override
        public void draw(Canvas canvas, CharSequence text, int start, int end,
                         float x, int top, int y, int bottom, Paint paint) {

            Paint.FontMetricsInt fm = paint.getFontMetricsInt();
            Drawable drawable = getDrawable();
            int transY = (y + fm.descent + y + fm.ascent) / 2
                    - drawable.getBounds().bottom / 2;
            canvas.save();
            canvas.translate(x, transY);
            drawable.draw(canvas);
            canvas.restore();
        }
    }

    public static SpannableStringBuilder contentToSpanStr(Context context, String noteContent, byte[] bytes) {

        String PatternNoteContent = noteContent;
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(PatternNoteContent);

        Pattern voice = Pattern.compile("<voice src='(.*?)'/>");
        Matcher mVoice = voice.matcher(noteContent);

        //find the voice
        while (mVoice.find()) {
            int start = mVoice.start();
            int end = mVoice.end();
            //String str1 = mVoice.group(0);
            Log.d(TAG, "ContentToSpanStr: " + mVoice.group(0));
            //PatternNoteContent = PatternNoteContent.replace(str1, "");
            String voiceName = mVoice.group(1);

            Uri voiceUri = fileToUri(voiceName, context, false);
            final MediaPlayer[] mediaPlayer = {new MediaPlayer()};
            try {
                mediaPlayer[0].setDataSource(context, voiceUri);
            } catch (IOException e) {
                e.printStackTrace();
                SharedPreferences cloudService = context.getSharedPreferences("CloudService", MODE_PRIVATE);
                String userName = cloudService.getString("UserName", "");
                String userPassword = cloudService.getString("UserPassword", "");
                SynWithWebDav synWithWebDav = new SynWithWebDav(userName, userPassword);
                InputStream inputStream = synWithWebDav.getFile(voiceName, "media");
                try {
                    byte[] fileBytes = new byte[1024];
                    //getActivity().openFileOutput("noteBackup.db", MODE_PRIVATE);
                    File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_RINGTONES) + "");
                    dir.mkdirs();
                    FileOutputStream outStream = new FileOutputStream(new File(dir, voiceName));
                    int index;
                    while ((index = inputStream.read(fileBytes)) != -1) {
                        Log.d(TAG, "run: " + index);
                        outStream.write(fileBytes, 0, index);
                        outStream.flush();
                    }
                    outStream.close();
                    inputStream.close();
                    mediaPlayer[0].setDataSource(context, voiceUri);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            //Log.d(TAG, "ContentToSpanStr: "+voiceFilePath);
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    //实现点击事件
                    Log.d("voice能否点击","能够点击");
                    if (mediaPlayer[0] != null && mediaPlayer[0].isPlaying()) {
                        mediaPlayer[0].stop();
                        mediaPlayer[0].release();
                        mediaPlayer[0] = null;
                        Log.d("voice能否点击","停止");
                        return;
                    }
                    mediaPlayer[0] = new MediaPlayer();
                    mediaPlayer[0].setOnCompletionListener((mp) -> {
                        if (mp != null) {
                            mp.stop();
                            mp.release();
                            mediaPlayer[0] = null;
                        }
                    });
                    try {
                        mediaPlayer[0].setDataSource(context, voiceUri);
                        mediaPlayer[0].prepare();
                        mediaPlayer[0].start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            Drawable drawable = ContextCompat.getDrawable(context, R.drawable.audio); //context.getResources().getDrawable(R.drawable.user_img, null);//
            //我们发现SpannableStringBuilder + ImageSpan可以实现将图片自动换行，并且如果剩余空间不足时图片会自动换行
            int width = getScreenRealWidth(context)*1/15;
            drawable.setBounds(0,0, width, drawable.getIntrinsicHeight() * width / drawable.getIntrinsicWidth());
            ImageSpan imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_CENTER);
            //限制点击事件范围，否则后面有空白地方会变成点击范围
            spannableStringBuilder.setSpan(clickableSpan, start, end - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableStringBuilder.setSpan(new MyImageSpan(drawable), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        Pattern img = Pattern.compile("<img src='(.*?)'/>");
        Matcher mImg = img.matcher(PatternNoteContent);

        //查找图片
        while (mImg.find()) {
            int start = mImg.start();
            int end = mImg.end();
            String imgName = mImg.group(1);
            Uri imgUri = fileToUri(imgName, context, true);
            //Uri imgUri = Uri.parse(mImg.group(1));
            Drawable drawable = null;
            boolean isPhotoExist = true;

            try {
                drawable = Drawable.createFromStream(context.getContentResolver().openInputStream(imgUri), null);
                //drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());

                //我们发现SpannableStringBuilder + ImageSpan可以实现将图片自动换行，并且如果剩余空间不足时图片会自动换行
                int width = getScreenRealWidth(context);
                drawable.setBounds(0,0, width, drawable.getIntrinsicHeight() * width / drawable.getIntrinsicWidth());
            } catch (Exception e) {
                SharedPreferences cloudService = context.getSharedPreferences("CloudService", MODE_PRIVATE);
                String userName = cloudService.getString("UserName", "");
                String userPassword = cloudService.getString("UserPassword", "");
                SynWithWebDav synWithWebDav = new SynWithWebDav(userName, userPassword);
                InputStream inputStream = synWithWebDav.getFile(imgName, "media");
                try {
                    byte[] fileBytes = new byte[1024];
                    //getActivity().openFileOutput("noteBackup.db", MODE_PRIVATE);
                    File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "");
                    dir.mkdirs();
                    FileOutputStream outStream = new FileOutputStream(new File(dir, imgName));
                    int index;
                    while ((index = inputStream.read(fileBytes)) != -1) {
                        Log.d(TAG, "run: " + index);
                        outStream.write(fileBytes, 0, index);
                        outStream.flush();
                    }
                    outStream.close();
                    inputStream.close();
                    drawable = Drawable.createFromStream(context.getContentResolver().openInputStream(imgUri), null);
                } catch (Exception ex) {
                    isPhotoExist = false;
                    drawable = ContextCompat.getDrawable(context, R.drawable.ic_img_fail);
                    ex.printStackTrace();
                }
                int width = getScreenRealWidth(context);
                //防止0kb文件
                if (drawable == null) {
                    isPhotoExist = false;
                    drawable = ContextCompat.getDrawable(context, R.drawable.ic_img_fail);
                }
                drawable.setBounds(0,0, width, drawable.getIntrinsicHeight() * width / drawable.getIntrinsicWidth());

                e.printStackTrace();
            }

            boolean finalIsPhotoExist = isPhotoExist;
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(View view) {
                    if (!finalIsPhotoExist) {
                        return;
                    }
                    Toast.makeText(context, "长按保存,单击分享", Toast.LENGTH_SHORT).show();
                    MNImageBrowser.with(context)
                            //设置隐藏指示器
                            .setIndicatorHide(false)
                            //必须-图片加载用户自己去选择
                            .setImageEngine(new GlideForMNImageBrowser())
                            //必须（setImageList和setImageUrl二选一，会覆盖）-图片集合
                            .setImageUrl(imgUri.toString())
                            .setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(FragmentActivity activity, ImageView view, int position, String url) {
                                    Intent shareIntent = new Intent();
                                    shareIntent.setAction(Intent.ACTION_SEND);
                                    shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(url));
                                    shareIntent.setType("image/jpeg");
                                    context.startActivity(Intent.createChooser(shareIntent, "分享图片"));

                                }
                            })
                            //非必须-图片长按监听
                            .setOnLongClickListener(new OnLongClickListener() {
                                @Override
                                public void onLongClick(FragmentActivity activity, ImageView imageView, int position, String url) {
                                    //长按监听
                                    Bitmap bitmap = null;
                                    Toast.makeText(context, "图片已保存", Toast.LENGTH_SHORT).show();
                                    try {
                                        bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(Uri.parse(url)));
                                    } catch (FileNotFoundException e) {
                                        e.printStackTrace();
                                    }
                                    saveImageToGallery(context, bitmap);
                                }
                            })
                            .show(view);
                }
            };

            Log.d(TAG, "ContentToSpanStr: "+ spannableStringBuilder + start + end);
            ImageSpan imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM);
            spannableStringBuilder.setSpan(imageSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableStringBuilder.setSpan(clickableSpan, start, end-1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if (bytes == null) {
            spannableStringBuilder.setSpan(new TypefaceSpan("serif"),0,spannableStringBuilder.length(),Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            //设置字体前景色
            spannableStringBuilder.setSpan(new ForegroundColorSpan(Color.BLACK), 0, spannableStringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            return spannableStringBuilder;//.append("\u200b");
        }
        getWordsSize(context);
        Parcel parcel1 = Parcel.obtain();
        parcel1.unmarshall(bytes, 0, bytes.length);
        parcel1.setDataPosition(0);
        SaveTextAppearanceSpan data = parcel1.readParcelable(SaveTextAppearanceSpan.class.getClassLoader());
        ArrayList<byte[]> temp = data.getSpansList();
        for (int i = 0; i < temp.size(); i++){
            Parcel parcel2 = Parcel.obtain();
            parcel2.unmarshall(temp.get(i),0,temp.get(i).length);
            parcel2.setDataPosition(0);
            //Parcel parcel3 = parcel2.readParcelable(Parcel.class.getClassLoader());

            Log.d("二进制", "AddNote: "+new String(Base64.encode(parcel2.marshall(),Base64.DEFAULT)));
            TextAppearanceSpan temp1 = new TextAppearanceSpan(parcel2);
            parcel2.recycle();
            int sp = pxToSp(context, temp1.getTextSize());
            Log.d("TextAppearanceSpan", "ContentToSpanStr: "+temp1.getTextSize()+temp1.getTextColor()+"sp" +sp);
            Log.d("二进制", "AddNote: "+data.getSpanStartList().get(i) +" "+ data.getSpanEndList().get(i));
            spannableStringBuilder.setSpan(setStyle(context,sp,temp1.getTextColor()),data.getSpanStartList().get(i),data.getSpanEndList().get(i),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannableStringBuilder;//.append("\u200b");
    }

    private static Display getDisplay(Context context) {
        WindowManager wm;
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            wm = activity.getWindowManager();
        } else {
            wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        }
        if (wm != null) {
            return wm.getDefaultDisplay();
        }
        return null;
    }

    public static int getScreenRealWidth(Context context) {
        Display display = getDisplay(context);
        if (display == null) {
            return 0;
        }
        Point outSize = new Point();
        display.getRealSize(outSize);
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);
        Log.d(TAG, "getScreenRealWidth: "+outSize.x +outSize.y+ " " +dm.heightPixels+ context.getResources().getDisplayMetrics()+"sp"+pxToSp(context,63));
        return outSize.x;
    }

    public static int pxToSp(Context context, float pxValue) {
        if (context == null) {
            return -1;
        }

        final float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        return (int)(pxValue / scaledDensity + 0.5f);
    }

    private static void saveImageToGallery(Context context, Bitmap bmp) {
        // 首先保存图片
//        File appDir = new File(Environment.getExternalStorageDirectory(), "Note");
//        if (!appDir.exists()) {
//            appDir.mkdir();
//        }
        String fileName = System.currentTimeMillis() + ".jpg";
//        File file = new File(appDir, fileName);
//        try {
//            FileOutputStream fos = new FileOutputStream(file);
//            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
//            fos.flush();
//            fos.close();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        // 其次把文件插入到系统图库
//        try {
//            MediaStore.Images.Media.insertImage(context.getContentResolver(),
//                    file.getAbsolutePath(), fileName, null);
//
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
        String url = MediaStore.Images.Media.insertImage(context.getContentResolver(),
                bmp, fileName, null);
        // 最后通知图库更新
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(url)));
        //context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(file.getAbsolutePath())));
    }

    private static Uri fileToUri(String fileName, Context context, boolean isPhoto) {
        File file = null;
        //File fileDirectory = C.FilePath.getPicturesDirectory();//即：/storage/emulated/0/Android/data/包名/files/Pictures/
        if (isPhoto) {
            file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName);
        } else {
            file = new File(context.getExternalFilesDir(Environment.DIRECTORY_RINGTONES), fileName);
        }
        Uri uri;
        if (Build.VERSION.SDK_INT < 24) {
            uri = Uri.fromFile(file);
        } else {
            uri = FileProvider.getUriForFile(context, "com.example.note.fileprovider", file);
        }
        return uri;
    }

    private static void getWordsSize(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("Setting", MODE_PRIVATE);
        String wordSizePrefs = sharedPreferences.getString("WordSize", "正常");
        int[] boldStyles = {R.style.BoldStyle, R.style.BoldStyle2, R.style.BoldStyle3};
        int[] highlightStyles = {R.style.HighlightStyle, R.style.HighlightStyle2, R.style.HighlightStyle3};
        int[] titleStyles = {R.style.TitleStyle, R.style.TitleStyle2, R.style.TitleStyle3};

        switch (wordSizePrefs) {
            case "正常":
                boldStyle = boldStyles[0];
                highlightStyle = highlightStyles[0];
                titleStyle = titleStyles[0];
                break;
            case "大":
                boldStyle = boldStyles[1];
                highlightStyle = highlightStyles[1];
                titleStyle = titleStyles[1];
                break;
            case "超大":
                boldStyle = boldStyles[2];
                highlightStyle = highlightStyles[2];
                titleStyle = titleStyles[2];
                break;
        }
    }

    private static TextAppearanceSpan setStyle(Context context, int sp, ColorStateList colorStateList) {
        if (colorStateList == null && (sp == 20 || sp == 25 || sp == 30)) {
            return new TextAppearanceSpan(context, boldStyle);
        } else if (sp == 22 || sp == 27 || sp == 32) {
            return new TextAppearanceSpan(context, titleStyle);
        } else {
            return new TextAppearanceSpan(context, highlightStyle);
        }
    }

}
