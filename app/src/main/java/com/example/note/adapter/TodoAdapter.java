package com.example.note.adapter;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StrikethroughSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.note.R;
import com.example.note.db.Todo;
import com.example.note.util.TodoDbManager;

import java.util.Calendar;
import java.util.List;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.ViewHolder> implements View.OnClickListener, View.OnLongClickListener{

    private Context mContext;
    private List<Todo> todoList;
    private OnRecyclerViewItemClickListener mOnItemClickListener;
    private OnRecyclerViewItemLongClickListener mOnItemLongClickListener;

    static class ViewHolder extends RecyclerView.ViewHolder{
        public CardView cardView;
        public CheckBox checkBox;
        public TextView todoContent;
        public TextView updateTime;

        public ViewHolder(View view) {
            super(view);
            this.cardView = view.findViewById(R.id.todo_card_view);
            this.checkBox = view.findViewById(R.id.todo_check_box);
            this.todoContent = view.findViewById(R.id.todo_content);
            this.updateTime = view.findViewById(R.id.todo_create_time);
        }
    }

    public TodoAdapter(List<Todo> todoList, Context context) {

        this.todoList = todoList;
        this.mContext = context;
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
            mOnItemClickListener.onItemClick(v, (Todo)v.getTag());
        }

    }

    @Override
    public boolean onLongClick(View v) {
        if (mOnItemLongClickListener != null) {
            //注册点击事件
            //注意这里使用getTag方法获取数据
            mOnItemLongClickListener.onItemLongClick(v, (Todo)v.getTag());
        }
        return true;
    }

    //定义接口供外部设置点击事件
    public interface OnRecyclerViewItemClickListener{
        void onItemClick(View view, Todo note);
    }

    public interface OnRecyclerViewItemLongClickListener{
        void onItemLongClick(View view, Todo note);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.todo_item, parent, false);
        //注册点击事件
        view.setOnClickListener(this);
        view.setOnLongClickListener(this);

        ViewHolder viewHolder = new ViewHolder(view);

        CheckBox checkBox = view.findViewById(R.id.todo_check_box);

        //textView用view.findViewById(R.id.todo_edit_text)无效。
        //应该是要操控对应数据
        TextView textView = viewHolder.todoContent;
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Todo todo = (Todo) buttonView.getTag();
                Log.d("checkbox", "onCheckedChanged: " + isChecked + todo.getContent());
                if (!buttonView.isPressed()) {
                    return;
                }
                if (isChecked) {
                    SpannableString spanString = new SpannableString(todo.getContent());
                    StrikethroughSpan span = new StrikethroughSpan();
                    spanString.setSpan(span, 0, todo.getContent().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    textView.setText(spanString);
                    todo.setCheckBoxFlag(true);
                    //Log.d("checkbox", "onCheckedChanged: " +  todo.getContent() + todoList.get(position).isCheckBoxFlag());
                    TodoDbManager todoDbManager = TodoDbManager.getInstance();
                    Todo temp = new Todo();
                    temp.setCheckBoxFlag(true);
                    temp.setStatus(1);
                    todoDbManager.updateTodo(todo.getId(), temp);
                } else {
                    textView.setText(todo.getContent());
                    TodoDbManager todoDbManager = TodoDbManager.getInstance();
                    Todo temp = new Todo();
                    temp.setToDefault("checkBoxFlag");
                    temp.setStatus(1);
                    todoDbManager.updateTodo(todo.getId(), temp);
                    todo.setCheckBoxFlag(false);
                    //Log.d("checkbox", "onCheckedChanged: " +  todo.getContent() + todoList.get(position).isCheckBoxFlag());
                }
            }
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Todo todo = todoList.get(position);
        //颜色分为提醒的，过期的，正常的
        if (todo.getTimeRemind() > 0 && todo.getTimeRemind() >= System.currentTimeMillis()) {
            holder.cardView.setCardBackgroundColor(mContext.getResources().getColor(R.color.colorAccent));
        } else if (todo.getTimeRemind() > 0 && todo.getTimeRemind() < System.currentTimeMillis()) {
            holder.cardView.setCardBackgroundColor(mContext.getResources().getColor(R.color.prue));
        } else {
            holder.cardView.setCardBackgroundColor(mContext.getResources().getColor(R.color.white));//getColor(colors[id%11]));
        }
        holder.checkBox.setTag(todo);
        holder.itemView.setTag(todo);
        holder.checkBox.setChecked(todo.isCheckBoxFlag());
        //获取当前年份
        String currentYear = Calendar.getInstance().get(Calendar.YEAR) + "年";
        if(todo.getUpdateTime().contains(currentYear)){
            holder.updateTime.setText(todo.getUpdateTime().replace(currentYear,""));
        } else {
            holder.updateTime.setText(todo.getUpdateTime());
        }
        holder.todoContent.setTextColor(Color.rgb(0,0,0));
        if (todo.isCheckBoxFlag()) {
            SpannableString spanString = new SpannableString(todo.getContent());
            StrikethroughSpan span = new StrikethroughSpan();
            spanString.setSpan(span, 0, todo.getContent().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            holder.todoContent.setText(spanString);
        } else {
            holder.todoContent.setText(todo.getContent());
        }
        Log.d("checkbox", "onBindViewHolder: " + todo.getContent());
    }

    @Override
    public int getItemCount() {
        return todoList.size();
    }

    //简单的说就是notifyDataSetChanged()会记住你划到的位置,重新加载数据的时候不会改变位置,只是改变了数据;
    //而用notifyDataSetInvalidated()时,数据改变的同时,自动滑到顶部第0条的位置.
    public void setTodo(List<Todo> todos) {
        this.todoList = todos;
        notifyDataSetChanged();
    }
}

