package haibison.android.lockpattern;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import haibison.android.lockpattern.widget.LockPatternView;


public class LockPatternActivity extends BaseActivity implements OnClickListener {

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
        setContentView(R.layout.activity_lock_pattern);

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
                headerView.setText("请输入新手势密码");
                buttonContainerView.setVisibility(View.VISIBLE);
                nextView.setVisibility(View.VISIBLE);
                cancelView.setVisibility(View.VISIBLE);
                confirmView.setVisibility(View.GONE);
                forgetView.setVisibility(View.GONE);
                break;
            case COMPARE_PATTERN:
                headerView.setText("请输入手势密码");
                buttonContainerView.setVisibility(View.VISIBLE);
                cancelView.setVisibility(View.GONE);
                nextView.setVisibility(View.GONE);
                confirmView.setVisibility(View.GONE);
                forgetView.setVisibility(View.VISIBLE);
                break;
            case VERIFY_CAPTCHA:
                headerView.setText("请输入手势密码");
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
    protected void onPatternCleared(LockPatternType lockPatternType) {
        super.onPatternCleared(lockPatternType);
        if (lockPatternType == LockPatternType.CREATE_PATTERN) {
            nextView.setEnabled(false);
            confirmView.setEnabled(false);
        }
    }

    @Override
    protected void doLockPatternResult(ResuleType resuleType) {
        switch (resuleType) {
            case MIN_DOTS_FAIL:
                Toast.makeText(this, "至少连接" + minWiredDots + "个点，请重试", Toast.LENGTH_SHORT).show();
                nextView.setEnabled(false);
                confirmView.setEnabled(false);
                break;
            case PATTERN_CREATE:
                nextView.setEnabled(true);
                confirmView.setEnabled(false);
                break;
            case COMPARE_OK:
                if (getLockPatternType() == LockPatternType.CREATE_PATTERN) {
//                    finishWithResultOk(getIntent().getCharArrayExtra(EXTRA_PATTERN));
                    confirmView.setEnabled(true);
                    headerView.setText("");
                } else {
                    finishWithResultOk(null);
                }
                break;
            case COMPARE_FAIL:
                if (getLockPatternType() == LockPatternType.CREATE_PATTERN) {
                    confirmView.setEnabled(false);
                    headerView.setText("手势密码不一致，请重新输入");
                } else {
                    if (retryCount >= maxRetries) {
                        finishWithNegativeResult(RESULT_FAILED);
                    } else {
                        headerView.setText("还有" + (maxRetries - retryCount) + "次尝试机会");
                    }
                }
                break;
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.bt_cancel) {
            finishWithNegativeResult(RESULT_CANCELED);
        } else if (i == R.id.bt_next) {
            lockPatternView.clearPattern();
            headerView.setText("确认手势密码");
            v.setVisibility(View.GONE);
            confirmView.setVisibility(View.VISIBLE);
        } else if (i == R.id.bt_bt_confirm) {
            finishWithResultOk(getIntent().getCharArrayExtra(EXTRA_PATTERN));
        } else if (i == R.id.bt_forget) {
            finishWithForgotPatternResult(RESULT_FORGOT_PATTERN);
        }
    }

}
