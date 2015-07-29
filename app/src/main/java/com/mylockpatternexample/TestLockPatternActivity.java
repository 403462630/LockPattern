package com.mylockpatternexample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import haibison.android.lockpattern.BaseActivity;
import haibison.android.lockpattern.widget.LockPatternView;


public class TestLockPatternActivity extends BaseActivity implements OnClickListener {

    private LockPatternView lockPatternView;
    private TextView headerView;
    private LinearLayout buttonContainerView;
    private Button cancelView;
    private Button confirmView;
    private Button nextView;
    private Button forgetView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_lock_pattern);

        lockPatternView = (LockPatternView) findViewById(R.id.lock_pattern_view);
        headerView = (TextView) findViewById(R.id.tv_header);
        buttonContainerView = (LinearLayout) findViewById(R.id.ll_button_container);
        cancelView = (Button) findViewById(R.id.bt_cancel);
        confirmView = (Button) findViewById(R.id.bt_bt_confirm);
        nextView = (Button) findViewById(R.id.bt_next);
        forgetView = (Button) findViewById(R.id.bt_forget);

        cancelView.setOnClickListener(this);
        nextView.setOnClickListener(this);
        confirmView.setOnClickListener(this);
        forgetView.setOnClickListener(this);

        init(lockPatternView);
    }

    @Override
    protected void onInit(LockPatternType lockPatternType) {
        super.onInit(lockPatternType);
        switch (lockPatternType) {
            case CREATE_PATTERN:
                buttonContainerView.setVisibility(View.VISIBLE);
                nextView.setVisibility(View.VISIBLE);
                cancelView.setVisibility(View.VISIBLE);
                confirmView.setVisibility(View.GONE);
                forgetView.setVisibility(View.GONE);
                break;
            case COMPARE_PATTERN:
                buttonContainerView.setVisibility(View.VISIBLE);
                cancelView.setVisibility(View.GONE);
                nextView.setVisibility(View.GONE);
                confirmView.setVisibility(View.GONE);
                forgetView.setVisibility(View.VISIBLE);
                break;
            case VERIFY_CAPTCHA:
                buttonContainerView.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    protected void onPatternStart(LockPatternType lockPatternType) {
        super.onPatternStart(lockPatternType);
        if (lockPatternType == LockPatternType.CREATE_PATTERN && nextView.getVisibility() == View.VISIBLE) {
            getIntent().removeExtra(EXTRA_PATTERN);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_cancel:
                finishWithNegativeResult(RESULT_CANCELED);
                break;
            case R.id.bt_next:
                lockPatternView.clearPattern();
                v.setVisibility(View.GONE);
                confirmView.setVisibility(View.VISIBLE);
                break;
            case R.id.bt_bt_confirm:
                final char[] pattern = getIntent().getCharArrayExtra(EXTRA_PATTERN);
//                if (mAutoSave) AlpSettings.Security.setPattern(this, pattern);
                finishWithResultOk(pattern);
                break;
            case R.id.bt_forget:
                break;
        }
    }

    public static Intent buildIntent(Context context, LockPatternType lockPatternType) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_LOCK_PATTERN_TYPE, lockPatternType);
        intent.setClass(context, TestLockPatternActivity.class);
//        intent.putExtra()
        return intent;
    }
}
