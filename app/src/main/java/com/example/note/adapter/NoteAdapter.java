package com.example.note.adapter;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.note.R;
import com.example.note.db.Note;
import com.example.note.util.ContentToSpannableString;

import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NoteAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener, View.OnLongClickListener{

    //private static int color;
    private List<Note> notes;
    private Context mContext;
    private int itemLayout;
    private OnRecyclerViewItemClickListener mOnItemClickListener;
    private OnRecyclerViewItemLongClickListener mOnItemLongClickListener;

    private static final int[] colors = new int[]{R.color.color_0,R.color.color_1,
            R.color.color_2,R.color.color_3,R.color.color_4,
            R.color.color_5,R.color.color_6,R.color.color_7,
            R.color.color_8,R.color.color_9,R.color.color_10};

    public NoteAdapter(List<Note> notes, Context context, int itemLayout) {
        this.notes = notes;
        this.mContext = context;
        this.itemLayout = itemLayout;
    }

    //定义接口供外部设置点击事件
    public interface OnRecyclerViewItemClickListener{
        void onItemClick(View view, Note note);
    }

    public interface OnRecyclerViewItemLongClickListener{
        void onItemLongClick(View view, Note note);
    }

    static class ViewHolder extends RecyclerView.ViewHolder{
        public CardView cardView;
        public ImageView imageView;
        public TextView title;
        public TextView subContent;
        public TextView createTime;

        public ViewHolder(View view) {
            super(view);
            this.cardView = view.findViewById(R.id.card_view);
            this.imageView = view.findViewById(R.id.note_img);
            this.title = view.findViewById(R.id.note_title);
            this.subContent = view.findViewById(R.id.note_subContent);
            this.createTime = view.findViewById(R.id.note_createTime);
        }
    }

    static class ViewHolderForListMode extends RecyclerView.ViewHolder{
        public CardView cardView;
        public ImageView imageView;
        public TextView title;
        public TextView createTime;
        public LinearLayout linearLayout;

        public ViewHolderForListMode(View view) {
            super(view);
            this.cardView = view.findViewById(R.id.card_view2);
            this.imageView = view.findViewById(R.id.note_img2);
            this.linearLayout = view.findViewById(R.id.note_img2_layout);
            this.title = view.findViewById(R.id.note_title2);
            this.createTime = view.findViewById(R.id.note_createTime2);
        }
    }

    public void setOnItemClickListener(OnRecyclerViewItemClickListener listener) {
        this.mOnItemClickListener = listener;
    }

    public void setOnItemLongClickListener(OnRecyclerViewItemLongClickListener listener) {
        this.mOnItemLongClickListener = listener;
    }

    @Override
    public void onClick(View v) {
        if (mOnItemClickListener != null) {
            //注意这里使用getTag方法获取数据
            mOnItemClickListener.onItemClick(v, (Note)v.getTag());
        }

    }

    @Override
    public boolean onLongClick(View v) {
        if (mOnItemLongClickListener != null) {
            //注册点击事件
            //注意这里使用getTag方法获取数据
            mOnItemLongClickListener.onItemLongClick(v, (Note)v.getTag());
        }
        return true;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (itemLayout == R.layout.note_item) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_item, parent, false);
            //注册点击事件
            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
            return new ViewHolder(view);
        }

        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_item_list_mode, parent, false);
        //注册点击事件
        view.setOnClickListener(this);
        view.setOnLongClickListener(this);
        return new ViewHolderForListMode(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Note note = notes.get(position);
        String content = note.getContent();
        String subContent = note.getSubContent();
        String pattern = "<img src='(.*?)'/>";
        Pattern r = Pattern.compile(pattern);

        Matcher matcher = r.matcher(content);
        Uri uri = null;
        //只找一张图片
        if (matcher.find()) {
            File file = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES), matcher.group(1));
            if (Build.VERSION.SDK_INT < 24) {
                uri = Uri.fromFile(file);
            } else {
                uri = FileProvider.getUriForFile(mContext, "com.example.note.fileprovider", file);
            }
                //Drawable drawable = Drawable.createFromStream(mContext.getContentResolver().openInputStream(uri),null);
                //drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                //holder.imageView.setImageDrawable(drawable);
        } else {
            //holder.imageView.setImageDrawable(null);
            Log.d("匹配","没有完成匹配");
        }

        String updateTime = note.getUpdateTime();

        //如果是今年新建的便签的话，没必要显示年份
        String currentYear = Calendar.getInstance().get(Calendar.YEAR) + "年";
        if (updateTime.contains(currentYear)){
            updateTime = updateTime.replace(currentYear,"");
        }

        if (holder instanceof ViewHolder) {
            ViewHolder viewHolder = (ViewHolder)holder;

            //颜色分为提醒的，过期的，正常的
            if (note.getTimeRemind() > 0 && note.getTimeRemind() >= System.currentTimeMillis()) {
                viewHolder.cardView.setCardBackgroundColor(mContext.getResources().getColor(R.color.colorAccent));
            } else if (note.getTimeRemind() > 0 && note.getTimeRemind() < System.currentTimeMillis()) {
                viewHolder.cardView.setCardBackgroundColor(mContext.getResources().getColor(R.color.prue));
            } else {
                viewHolder.cardView.setCardBackgroundColor(mContext.getResources().getColor(R.color.white));//getColor(colors[id%11]));
            }
            viewHolder.itemView.setTag(note);

            if (uri != null) {
//                BitmapFactory.Options op = new BitmapFactory.Options();
//                op.inJustDecodeBounds = true;
//                try {
//                    InputStream inputStream = mContext.getContentResolver().openInputStream(uri);
//                    BitmapFactory.decodeStream(inputStream,null, op);
//                    inputStream.close();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
                ViewGroup.LayoutParams imageLayoutParams = viewHolder.imageView.getLayoutParams();
                imageLayoutParams.width = (ContentToSpannableString.getScreenRealWidth(mContext) -15) / 2;//获取实际展示的图片宽度
                //imageLayoutParams.height = (int) (1.0*imageLayoutParams.width * op.outHeight / op.outWidth);//获取最终图片高度
                imageLayoutParams.height = (int) (1.0*imageLayoutParams.width * 4 / 3);
                viewHolder.imageView.setLayoutParams(imageLayoutParams);//应用高度到布局中
                viewHolder.imageView.setVisibility(View.VISIBLE);
                Glide.with(mContext).load(uri).override(imageLayoutParams.width,imageLayoutParams.height)
                        .error(ContextCompat.getDrawable(mContext, R.drawable.ic_img_fail))
                        .into(viewHolder.imageView);
            } else {
                //ViewGroup.LayoutParams imageLayoutParams = viewHolder.imageView.getLayoutParams();
               // imageLayoutParams.width = RecyclerView.LayoutParams.MATCH_PARENT;
                //imageLayoutParams.height = RecyclerView.LayoutParams.WRAP_CONTENT;
                //viewHolder.imageView.setLayoutParams(imageLayoutParams);//应用高度到布局中
                viewHolder.imageView.setVisibility(View.GONE);
                viewHolder.imageView.setImageDrawable(null);
            }

            viewHolder.title.setTextColor(Color.rgb(0,0,0));
            viewHolder.title.setText(note.getTitle());
            viewHolder.subContent.setText(subContent.trim());
            viewHolder.createTime.setText(updateTime);
            return;
        }

        ViewHolderForListMode viewHolderForListMode = (ViewHolderForListMode)holder;

        //颜色分为提醒的，过期的，正常的
        if (note.getTimeRemind() > 0 && note.getTimeRemind() >= System.currentTimeMillis()) {
            viewHolderForListMode.cardView.setCardBackgroundColor(mContext.getResources().getColor(R.color.colorAccent));
        } else if (note.getTimeRemind() > 0 && note.getTimeRemind() < System.currentTimeMillis()) {
            viewHolderForListMode.cardView.setCardBackgroundColor(mContext.getResources().getColor(R.color.prue));
        } else {
            viewHolderForListMode.cardView.setCardBackgroundColor(mContext.getResources().getColor(R.color.white));
        }

        viewHolderForListMode.itemView.setTag(note);

        if (uri != null) {
            viewHolderForListMode.linearLayout.setVisibility(View.VISIBLE);
            Glide.with(mContext).load(uri)//.override(100,100)
                    .error(ContextCompat.getDrawable(mContext, R.drawable.ic_img_fail))
                    .into(viewHolderForListMode.imageView);
        } else {
            viewHolderForListMode.linearLayout.setVisibility(View.GONE);
        }

        viewHolderForListMode.title.setTextColor(Color.rgb(0,0,0));
        viewHolderForListMode.title.setText(note.getTitle());
        //viewHolderForListMode.subContent.setText(subContent);
        viewHolderForListMode.createTime.setText(updateTime);
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }


    //简单的说就是notifyDataSetChanged()会记住你划到的位置,重新加载数据的时候不会改变位置,只是改变了数据;
    //而用notifyDataSetInvalidated()时,数据改变的同时,自动滑到顶部第0条的位置.
    public void setNotes(List<Note> notes) {
        this.notes = notes;
        notifyDataSetChanged();
    }
}
