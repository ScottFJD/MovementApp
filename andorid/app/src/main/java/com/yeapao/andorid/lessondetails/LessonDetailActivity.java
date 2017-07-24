package com.yeapao.andorid.lessondetails;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.gson.Gson;
import com.scottfu.sflibrary.net.CloudClient;
import com.scottfu.sflibrary.net.JSONResultHandler;
import com.scottfu.sflibrary.util.GlideUtil;
import com.scottfu.sflibrary.util.LogUtil;
import com.yeapao.andorid.R;
import com.yeapao.andorid.api.ConstantYeaPao;
import com.yeapao.andorid.api.NetImpl;
import com.yeapao.andorid.base.BaseActivity;
import com.yeapao.andorid.dialog.DialogCallback;
import com.yeapao.andorid.dialog.DialogUtils;
import com.yeapao.andorid.model.LessonDetailData;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by fujindong on 2017/7/14.
 */

public class LessonDetailActivity extends BaseActivity {


    private static final String TAG = "LessonDetailActivity";
    @BindView(R.id.tv_order)
    TextView tvOrder;
    @BindView(R.id.tv_shop_name)
    TextView tvShopName;
    @BindView(R.id.tv_coach_name)
    TextView tvCoachName;
    @BindView(R.id.tv_good)
    TextView tvGood;
    @BindView(R.id.tv_lesson_content)
    TextView tvLessonContent;
    @BindView(R.id.tv_adress)
    TextView tvAdress;
    @BindView(R.id.tv_lesson_time)
    TextView tvLessonTime;
    @BindView(R.id.rv_lesson_content_list)
    RecyclerView rvLessonContentList;
    @BindView(R.id.tv_bespeak)
    TextView tvBespeak;
    @BindView(R.id.iv_head)
    ImageView ivHead;

    private LessonDetailData lessonDetailData;


    private Gson gson = new Gson();

    private LinearLayoutManager llm;

    private LessonDetailContentAdapter lessonDetailContentAdapter;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson_detail);
        ButterKnife.bind(this);
        initView();
        initTopBar();
        getData();

    }

    private void initView() {
        llm = new LinearLayoutManager(getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        rvLessonContentList.setLayoutManager(llm);
    }

    @OnClick(R.id.tv_order)
    void setTvOrder(View view) {
        DialogUtils.showDialog(getContext(), "消息提示", "您还没有购买课程，请先购买", "线下课程", "线上课程", new DialogCallback() {
            @Override
            public void onItemClick(int position) {
                LogUtil.e(TAG,"onitemclick");
            }

            @Override
            public void onLeftClick() {
                LogUtil.e(TAG,"leftClick");
            }

            @Override
            public void onRightClick() {
                LogUtil.e(TAG,"rightClick");
            }
        });
    }

    @Override
    protected void initTopBar() {

        initBack();
        initTitle("课程详情");
    }


    private void getData() {
        CloudClient.doHttpRequest(getContext(), ConstantYeaPao.GET_LESSON_DETAIL, NetImpl.getInstance().getLessonDetail("1"), null, new JSONResultHandler() {
            @Override
            public void onSuccess(String jsonString) {
                LogUtil.e(TAG, jsonString);
                lessonDetailData = gson.fromJson(jsonString, LessonDetailData.class);
                if (lessonDetailData.getErrmsg().equals("ok")) {
                    setData();
                } else {

                }
            }

            @Override
            public void onError(VolleyError errorMessage) {

            }
        });
    }

    private void setData() {
        tvShopName.setText(lessonDetailData.getData().getCurriculum().getShopName());
        tvCoachName.setText(lessonDetailData.getData().getCurriculum().getCoach());
        tvAdress.setText(lessonDetailData.getData().getCurriculum().getAddress());
        tvLessonTime.setText(lessonDetailData.getData().getCurriculum().getClassTimeStart()+":00"+
        "-"+lessonDetailData.getData().getCurriculum().getClassTimeEnd()+":00");
        tvGood.setText(lessonDetailData.getData().getCurriculum().getBeGoodAt());
        tvBespeak.setText("已预约  " + lessonDetailData.getData().getCurriculum().getBespeak() + "/" +
                lessonDetailData.getData().getCurriculum().getTotalNumber());

        GlideUtil glideUtil = new GlideUtil();
        glideUtil.glideLoadingImage(getContext(), ConstantYeaPao.HOST + lessonDetailData.getData().getCurriculum().getHead(),
                R.drawable.y_you, ivHead);

        if (lessonDetailContentAdapter == null) {
            lessonDetailContentAdapter = new LessonDetailContentAdapter(getContext(), lessonDetailData);
            rvLessonContentList.setAdapter(lessonDetailContentAdapter);
        }
    }

    @Override
    protected Context getContext() {
        return this;
    }
}
