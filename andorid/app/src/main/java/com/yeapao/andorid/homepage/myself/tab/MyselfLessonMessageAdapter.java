package com.yeapao.andorid.homepage.myself.tab;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.scottfu.sflibrary.recyclerview.OnRecyclerViewClickListener;
import com.yeapao.andorid.R;
import com.yeapao.andorid.model.MyselfLessonModel;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * Created by fujindong on 2017/7/23.
 */

public class MyselfLessonMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context mContext;
    private LayoutInflater inflater;
    private OnRecyclerViewClickListener mListener;

    private MyselfLessonModel lessonModel;

    public MyselfLessonMessageAdapter(Context context,MyselfLessonModel lessonModel) {
        mContext = context;
        inflater = LayoutInflater.from(context);
        this.lessonModel = lessonModel;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        return new ViewHolder(inflater.inflate(R.layout.item_my_lesson_card, parent, false),mListener);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((ViewHolder)holder).tvMyLessonTitle.setText(lessonModel.getData().get(position).getCurriculumType());
        ((ViewHolder) holder).tvMyLessonRemainingDay.setText(lessonModel.getData().get(position).getTime());
        ((ViewHolder) holder).tvMyLessonRemainingClub.setText(lessonModel.getData().get(position).getSurplusNum());
        ((ViewHolder) holder).tvMyLessonPrice.setText(lessonModel.getData().get(position).getTotalPrice());
    }


    public void setItemOnClickListener(OnRecyclerViewClickListener listener) {
        mListener = listener;
    }

    @Override
    public int getItemCount() {
        return lessonModel.getData().size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        private OnRecyclerViewClickListener listener;


        @BindView(R.id.tv_my_lesson_title)
        TextView tvMyLessonTitle;
        @BindView(R.id.tv_my_lesson_price)
        TextView tvMyLessonPrice;
        @BindView(R.id.tv_my_lesson_remaining_day)
        TextView tvMyLessonRemainingDay;
        @BindView(R.id.tv_my_lesson_remaining_club)
        TextView tvMyLessonRemainingClub;

        ViewHolder(View view, OnRecyclerViewClickListener listener) {
            super(view);
            ButterKnife.bind(this, view);
            this.listener = listener;
            view.setOnClickListener(this);

        }

        @Override
        public void onClick(View v) {
            if (listener != null) {
                listener.OnItemClick(v, getLayoutPosition());
            }
        }
    }
}
