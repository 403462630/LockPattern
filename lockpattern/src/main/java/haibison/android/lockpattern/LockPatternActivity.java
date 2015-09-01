package haibison.android.lockpattern;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import haibison.android.lockpattern.widget.LockPatternView;


public class LockPatternActivity extends BaseActivity implements OnClickListener {

    private static final String RETRY_COUNT = LockPatternActivity.class.getName() + ".retry_count";

    private LockPatternView lockPatternView;
    private TextView headerView;
    private LinearLayout buttonContainerView;
    private TextView redrawView;
    private TextView confirmView;
    private TextView exitView;
    private RelativeLayout titleContainerView;

    private boolean enableTouched = true;

    private final Runnable runnable = new Runnable() {

        @Override
        public void run() {
            lockPatternView.clearPattern();
            onPatternCleared();
        }

    };

    private void setRetryCount(int count) {
        SharedPreferences.Editor editor = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE).edit();
        editor.putInt(RETRY_COUNT, count);
        editor.commit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_pattern);

        lockPatternView = (LockPatternView) findViewById(R.id.lock_pattern_view);
        headerView = (TextView) findViewById(R.id.tv_header);
        buttonContainerView = (LinearLayout) findViewById(R.id.ll_button_container);
        redrawView = (TextView) findViewById(R.id.tv_redraw);
        confirmView = (TextView) findViewById(R.id.tv_confirm);
        exitView = (TextView) findViewById(R.id.tv_exit);
        titleContainerView = (RelativeLayout) findViewById(R.id.rl_title_container);

        redrawView.setOnClickListener(this);
        confirmView.setOnClickListener(this);
        exitView.setOnClickListener(this);

        init(lockPatternView);
        setUpProgressView(findViewById(R.id.rl_progress_bar_container));

        if (getLockPatternType() == LockPatternType.COMPARE_PATTERN) {
            retryCount = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE).getInt(RETRY_COUNT, 0);
            if (retryCount >= maxRetries) {
                lockPatternView.setEnabled(false);
                headerView.setText(retryCount + "次密码输入错误");
                headerView.setTextColor(getResources().getColor(R.color.lock_pattern_head_hint_color_error));
                exitView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onInit(LockPatternType lockPatternType) {
        super.onInit(lockPatternType);
        lockPatternView.setEnabled(true);
        redrawView.setVisibility(View.GONE);
        confirmView.setVisibility(View.GONE);
        exitView.setVisibility(View.GONE);
        headerView.setTextColor(getResources().getColor(R.color.lock_pattern_head_hint_color_regular));
        switch (lockPatternType) {
            case CREATE_PATTERN:
                headerView.setText("绘制解锁图案 最少连接4个点");
                break;
            case COMPARE_PATTERN:
                if (isModify()) {
                    headerView.setText("请输入旧的手势密码");
                } else {
                    headerView.setText("请绘制手势密码");
                }
                break;
            case VERIFY_CAPTCHA:
                headerView.setText("请输入手势");
                break;
        }
    }

    @Override
    protected void onPatternStart(LockPatternType lockPatternType) {
        super.onPatternStart(lockPatternType);
//        if (lockPatternType == LockPatternType.CREATE_PATTERN && nextView.getVisibility() == View.VISIBLE) {
//            getIntent().removeExtra(EXTRA_PATTERN);
//        }
    }

    @Override
    protected void onPatternCleared(LockPatternType lockPatternType) {
        super.onPatternCleared(lockPatternType);
        if (lockPatternType == LockPatternType.CREATE_PATTERN) {
            confirmView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void doLockPatternResult(ResuleType resuleType) {
        switch (resuleType) {
            case MIN_DOTS_FAIL:
                headerView.setText("至少连接" + minWiredDots + "个点,请重试");
                headerView.setTextColor(getResources().getColor(R.color.lock_pattern_head_hint_color_error));
                break;
            case PATTERN_CREATE:
                headerView.setTextColor(getResources().getColor(R.color.lock_pattern_head_hint_color_regular));
                headerView.setText("已记录图案");
                lockPatternView.setEnabled(false);
                lockPatternView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        lockPatternView.setEnabled(true);
                        headerView.setText("再次绘制图案");
                        headerView.setTextColor(getResources().getColor(R.color.lock_pattern_head_hint_color_regular));
                        lockPatternView.clearPattern();
                        redrawView.setVisibility(View.VISIBLE);
                    }
                }, 1000);
                break;
            case COMPARE_OK:
                setRetryCount(0);
                if (getLockPatternType() == LockPatternType.CREATE_PATTERN) {
                    confirmView.setVisibility(View.VISIBLE);
                    headerView.setText("请确认您的手势图案");
                    headerView.setTextColor(getResources().getColor(R.color.lock_pattern_head_hint_color_regular));
                } else {
                    if (isModify()) {
                        headerView.setTextColor(getResources().getColor(R.color.lock_pattern_head_hint_color_regular));
                        headerView.setText("手势密码一致");
                        exitView.setVisibility(View.GONE);
                        lockPatternView.setEnabled(false);
                        lockPatternView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                lockPatternView.setEnabled(true);
                                setLockPatternType(LockPatternType.CREATE_PATTERN);
                                lockPatternView.clearPattern();
                                getIntent().removeExtra(EXTRA_PATTERN);
                                headerView.setText("绘制解锁图案 最少连接4个点");
                                headerView.setTextColor(getResources().getColor(R.color.lock_pattern_head_hint_color_regular));
                                redrawView.setVisibility(View.GONE);
                                confirmView.setVisibility(View.GONE);
                                exitView.setVisibility(View.GONE);
                            }
                        }, 1000);
                    } else {
                        finishWithResultOk(null);
                    }
                }
                break;
            case COMPARE_FAIL:
                if (getLockPatternType() == LockPatternType.CREATE_PATTERN) {
                    confirmView.setVisibility(View.GONE);
                    headerView.setText("与上次绘制不一致,请重试");
                    headerView.setTextColor(getResources().getColor(R.color.lock_pattern_head_hint_color_error));
                } else {
                    setRetryCount(retryCount);
                    if (retryCount >= maxRetries) {
                        lockPatternView.setEnabled(false);
                        headerView.setText(retryCount + "次密码输入错误");
                        headerView.setTextColor(getResources().getColor(R.color.lock_pattern_head_hint_color_error));
                        exitView.setVisibility(View.VISIBLE);
//                        finishWithNegativeResult(RESULT_FAILED);
                    } else {
//                        headerView.setText("还有" + (maxRetries - retryCount) + "次尝试机会");
                        headerView.setText("输入错误,请重试");
                        headerView.setTextColor(getResources().getColor(R.color.lock_pattern_head_hint_color_error));
                    }
                }
                break;
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.tv_redraw) {

            getIntent().removeExtra(EXTRA_PATTERN);
            lockPatternView.clearPattern();
            headerView.setText("绘制解锁图案 最少连接4个点");
            headerView.setTextColor(getResources().getColor(R.color.lock_pattern_head_hint_color_regular));
            redrawView.setVisibility(View.GONE);
            confirmView.setVisibility(View.GONE);
            exitView.setVisibility(View.GONE);
        } else if (i == R.id.tv_confirm) {
            finishWithResultOk(getIntent().getCharArrayExtra(EXTRA_PATTERN));
        } else if (i == R.id.tv_exit) {
            finishWithNegativeResult(RESULT_FAILED);
        }
//        else if (i == R.id.bt_forget) {
//            finishWithForgotPatternResult(RESULT_FORGOT_PATTERN);
//        }
    }
}
