package zx.com.zxvboxtvlive.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.AnimationDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import zx.com.zxvboxtvlive.Adapter.ChannelItemAdapter;
import zx.com.zxvboxtvlive.R;
import zx.com.zxvboxtvlive.Setting;
import zx.com.zxvboxtvlive.ijkplayer.media.IjkVideoView;
import zx.com.zxvboxtvlive.mode.ShowPlayTimes;
import zx.com.zxvboxtvlive.mode.TvSource;
import zx.com.zxvboxtvlive.presenter.AllChannelPresenter;
import zx.com.zxvboxtvlive.presenter.EDGPresenter;
import zx.com.zxvboxtvlive.presenter.VideoViewPresenter;
import zx.com.zxvboxtvlive.utils.Logger;
import zx.com.zxvboxtvlive.view.IMainView;
import zx.com.zxvboxtvlive.view.widget.MetroViewBorderImpl;


public class MainActivity extends FullActivity implements IMainView, View.OnTouchListener {

    private MyConnectionChanngeReceiver myReceiver;
    private boolean isNetWork = true;

    private View mChannelView;
    private View mShowPlayInfoView;
    private View mErrorView;

    private IjkVideoView mVideoView;

    private TextView mChannelName;
    private TextView mChannelId;
    private TextView mShowPlayingName;
    private TextView mShowWillPlayName;

    private RecyclerView mChannelList;

    private ProgressBar mShowProgress;
    private MetroViewBorderImpl mMetroViewBorder;

    private AllChannelPresenter mAllChannelPresenter;
    private VideoViewPresenter mVideoViewPresenter;
    private EDGPresenter mEDGPresenter;

    private ChannelItemAdapter mChannelItemAdapter;
    private List<TvSource> mTvSources = new ArrayList<>();
    private TvSource mCurrentPlaySource = null;

    private boolean showChannelView = true;
    private boolean showProgrameInfoView = true;

    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    private Handler mHideHandler = new Handler();

    private Runnable mHideChannelViewRunnable = new Runnable() {
        @Override
        public void run() {
            if (showChannelView) {

            } else {

            }
        }
    };

    private Runnable mHideShowInfoViewRunnable = new Runnable() {
        @Override
        public void run() {
            if (showProgrameInfoView) {
                showProgrameInfoView = false;
                mShowPlayInfoView.setVisibility(View.GONE);
            } else {

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        initView();

        registerReceiver();

        mAllChannelPresenter.updateChannelData();
//        mAllChannelPresenter.updateChannelDataOther();


        new Thread(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    View rootview = MainActivity.this.getWindow().getDecorView();
                    View aaa = rootview.findFocus();
//                    if(aaa != null)
//                    Logger.getLogger().d("" + aaa.toString());
                }

            }
        }).start();

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            showLoadingView(R.string.load_progress_promate);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        showShowInfoView();
    }

    @Override
    protected void onStop() {
        super.onStop();

        Setting.lastPlayChannel(this, mCurrentPlaySource.getTvName());

        mVideoViewPresenter.stop();
    }

    private void initView() {

        mMetroViewBorder = new MetroViewBorderImpl(this);
        mMetroViewBorder.setBackgroundResource(R.drawable.border_color);

        mShowPlayInfoView = findViewById(R.id.show_playing_info_view);
        mVideoView = (IjkVideoView) findViewById(R.id.videoview);
        mChannelView = findViewById(R.id.channel_name_view);

        mChannelList = (RecyclerView) findViewById(R.id.channel_menu);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 1);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mChannelList.setLayoutManager(layoutManager);
        mChannelList.setFocusable(false);
        mMetroViewBorder.attachTo(mChannelList);

        createOptionItemData(mChannelList, R.layout.detail_menu_item);

        mAllChannelPresenter = new AllChannelPresenter(this, this);

        mVideoViewPresenter = new VideoViewPresenter(this, this, (IjkVideoView) findViewById(R.id.videoview));

        mEDGPresenter = new EDGPresenter(this, this);

        findViewById(R.id.show_playing_info_view).setOnTouchListener(this);
        findViewById(R.id.show_playing_info_view).setOnTouchListener(this);
        findViewById(R.id.videoview).setOnTouchListener(this);
        findViewById(R.id.videoview).setFocusable(false);
    }

    private void createOptionItemData(RecyclerView recyclerView, int id) {
        mChannelItemAdapter = new ChannelItemAdapter(this, id);
        recyclerView.setAdapter(mChannelItemAdapter);
        recyclerView.scrollToPosition(0);

        mChannelItemAdapter.setOnBindListener(new ChannelItemAdapter.OnBindListener() {
            @Override
            public void onBind(View view, int i) {
                mVideoViewPresenter.playVideo(mTvSources.get(i).getTvDataSource());
                mCurrentPlaySource = mTvSources.get(i);

                showShowInfoView();
            }
        });
    }

    @Override
    public void showError(boolean show) {
        mVideoView.setVisibility(View.GONE);
        View view = findViewById(R.id.load_error);
        if (show) {
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
        Logger.getLogger().i("show error");
    }

    @Override
    public void hideLoadingView() {
        findViewById(R.id.load_progress_container).setVisibility(View.GONE);
    }

    @Override
    public void showLoadingView(int id) {
        findViewById(R.id.load_progress_container).setVisibility(View.VISIBLE);
        ImageView loadImg = (ImageView) findViewById(R.id.load_progress);
        AnimationDrawable animationDrawable = (AnimationDrawable) loadImg.getBackground();
        animationDrawable.start();
        ((TextView) findViewById(R.id.load_progress_promate)).setText(id);
    }

    @Override
    public void updateChannleAdapter(List<TvSource> data) {
        mTvSources.addAll(data);
        mChannelItemAdapter.setData(data);
        mChannelItemAdapter.notifyDataSetChanged();


        mVideoView.setVisibility(View.VISIBLE);
        String channelName  = Setting.getLastPlayChannelName(this);
        for(TvSource source : data) {
            if(source.getTvName().equals(channelName)) {
                mCurrentPlaySource = source;
                break;
            }
        }
        if(mCurrentPlaySource == null) {
            mCurrentPlaySource = data.get(0);
        }

        mVideoViewPresenter.playVideo(mCurrentPlaySource.getTvDataSource());

        int index = data.indexOf(mCurrentPlaySource);
        mChannelList.scrollToPosition(index);
        mChannelList.requestFocus();
    }

    @Override
    public void checkNextSource() {
        showToastLong(getString(R.string.get_channel_source_other));
        showError(false);
        showLoadingView(R.string.load_progress_promate);
        mAllChannelPresenter.updateChannelDataOther();
    }

    @Override
    public void updateShowInfo() {
        mEDGPresenter.updateShowInfo(mCurrentPlaySource);
    }

    @Override
    public void updateShowInfoUI(List<ShowPlayTimes> timesList) {
        TextView curPlay = (TextView) findViewById(R.id.show_playing);
        TextView curPlayNext = (TextView) findViewById(R.id.show_will_playing);
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.show_progress);
        Logger.getLogger().i(" " + timesList.size());
        if (timesList != null && timesList.size() > 0) {
            ShowPlayTimes showPlaying = timesList.get(0);
            ShowPlayTimes showNext = timesList.get(1);

            String showPlaingStartTime = showPlaying.getShowStartTime();
            String showNextStartTime = showNext.getShowStartTime();

            if (showPlaingStartTime != null) {
                curPlay.setText(showPlaying.getShowStartTime() + "  " + getString(R.string.show_playing_tilte) + " "
                        + showPlaying.getShowName());
            } else {
                curPlay.setText(getString(R.string.hava_no_show_info));
            }

            if (showNextStartTime != null) {
                curPlayNext.setText(showNext.getShowStartTime() + "  " + getString(R.string.show_will_play_title) + " "
                        + showNext.getShowName());
            } else {
                curPlayNext.setText(getString(R.string.hava_no_show_info));
            }

            double totalTime = processShowTimeLong(showPlaying.getShowStartTime(), showNext.getShowStartTime());

            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            int minute = Calendar.getInstance().get(Calendar.MINUTE);

            double nowPlay = processShowTimeLong(showPlaying.getShowStartTime(), hour + ":" + minute);
            Logger.getLogger().e("totalTime  = " + totalTime + " nowPlay = " + nowPlay);
            if (nowPlay <= 0 || nowPlay > totalTime) {
                progressBar.setProgress(100);
            } else {
//                Logger.getLogger().e("value " + (int)(( (double) (nowPlay / totalTime) ) * 100));
                progressBar.setProgress((int) (((double) (nowPlay / totalTime)) * 100));
            }

        } else {

            curPlay.setText(getString(R.string.hava_no_show_info));
            curPlayNext.setText(getString(R.string.hava_no_show_info));
            progressBar.setProgress(100);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        mVideoView.setFocusable(false);

        if (v.getId() == R.id.videoview) {
            showShowInfoView();
            findViewById(R.id.channel_menu).requestFocus();
        }

        if (v.getId() == R.id.channel_name_view) {
            if (!showProgrameInfoView) {
                showProgrameInfoView = true;

                delayedHideShowInfoView(AUTO_HIDE_DELAY_MILLIS);
            }
        }

        if (v.getId() == R.id.show_playing_info_view) {

        }
        return false;
    }

    private void showShowInfoView() {
        showProgrameInfoView = true;
        mShowPlayInfoView.setVisibility(View.VISIBLE);
        delayedHideShowInfoView(AUTO_HIDE_DELAY_MILLIS);

        mEDGPresenter.updateShowInfo(mCurrentPlaySource);
    }

    private void delayedHideShowInfoView(int delayMillis) {
        mHideHandler.removeCallbacks(mHideShowInfoViewRunnable);
        mHideHandler.postDelayed(mHideShowInfoViewRunnable, delayMillis);
    }


    class MyConnectionChanngeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mobNetInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            NetworkInfo wifiNetInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

//            if (!mobNetInfo.isConnected() && !wifiNetInfo.isConnected()) {
//                // showToastShort("网络断开连接");
//                isNetWork = false;
//                netWorkNO();
//            } else {
//                netWorkYew();
//            }
        }
    }

    public void netWorkNO() {
        /*final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.lemon95_dialog_title);
        builder.setMessage(R.string.lemon95_dialog_net_msg);
        builder.setNegativeButton(R.string.lemon95_dialog_cancal, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.setPositiveButton(R.string.lemon95_dialog_net_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = null;
                //判断手机系统的版本  即API大于10 就是3.0或以上版本
                if (android.os.Build.VERSION.SDK_INT > 10) {
                    intent = new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
                } else {
                    intent = new Intent("android.settings.WIFI_SETTINGS");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                BaseActivity.this.startActivity(intent);
            }
        });
        builder.show();*/
    }

    public void netWorkYew() {
        if (!isNetWork) {
            showToastShort("网络连接成功");
            isNetWork = true;
        }
    }

    //注册广播
    private void registerReceiver() {
        if (myReceiver == null) {
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            myReceiver = new MyConnectionChanngeReceiver();
            this.registerReceiver(myReceiver, filter);
        }
    }

    private void unregisterReceiver() {
        if (myReceiver != null) {
            this.unregisterReceiver(myReceiver);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver();
    }

    public void showToastLong(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    public void showToastShort(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private int processShowTimeLong(String timeStart, String endTime) {
        if (TextUtils.isEmpty(timeStart) || TextUtils.isEmpty(endTime)) {
            return 0;
        }

        String[] startTimeArr = timeStart.split(":");
        String[] endTimeArr = endTime.split(":");

        if (startTimeArr[0].equals(endTimeArr[0])) {
            return Integer.valueOf(endTimeArr[1]) - Integer.valueOf(startTimeArr[1]);
        } else {
            int hour = Integer.valueOf(endTimeArr[0]) - Integer.valueOf(startTimeArr[0]);
            return (hour - 1) * 60 + (60 - Integer.valueOf(startTimeArr[1])) + Integer.valueOf(endTimeArr[1]);
        }

    }
}
