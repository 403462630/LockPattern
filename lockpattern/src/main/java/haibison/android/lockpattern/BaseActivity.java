package haibison.android.lockpattern;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import haibison.android.lockpattern.util.AlpSettings;
import haibison.android.lockpattern.util.IEncrypter;
import haibison.android.lockpattern.util.InvalidEncrypterException;
import haibison.android.lockpattern.util.LoadingView;
import haibison.android.lockpattern.widget.LockPatternUtils;
import haibison.android.lockpattern.widget.LockPatternView;
import haibison.android.lockpattern.widget.LockPatternView.OnPatternListener;

import static android.text.format.DateUtils.SECOND_IN_MILLIS;
import static haibison.android.lockpattern.util.AlpSettings.Display.METADATA_CAPTCHA_WIRED_DOTS;
import static haibison.android.lockpattern.util.AlpSettings.Display.METADATA_MAX_RETRIES;
import static haibison.android.lockpattern.util.AlpSettings.Display.METADATA_MIN_WIRED_DOTS;
import static haibison.android.lockpattern.util.AlpSettings.Display.METADATA_STEALTH_MODE;
import static haibison.android.lockpattern.util.AlpSettings.Security.METADATA_AUTO_SAVE_PATTERN;
import static haibison.android.lockpattern.util.AlpSettings.Security.METADATA_ENCRYPTER_CLASS;


public class BaseActivity extends ActionBarActivity implements OnPatternListener {

    private static final String CLASSNAME = BaseActivity.class.getName();

    public static final int RESULT_FAILED = Activity.RESULT_FIRST_USER + 1;
    public static final int RESULT_FORGOT_PATTERN = RESULT_FIRST_USER + 2;

    public static final String EXTRA_LOCK_PATTERN_TYPE = CLASSNAME + ".lock_pattern_type";
    public static final String EXTRA_RETRY_COUNT = CLASSNAME + ".retry_count";
    public static final String EXTRA_PENDING_INTENT_OK = CLASSNAME + ".pending_intent_ok";
    public static final String EXTRA_RESULT_RECEIVER = CLASSNAME + ".result_receiver";
    public static final String EXTRA_PENDING_INTENT_CANCELLED = CLASSNAME + ".pending_intent_cancelled";
    public static final String EXTRA_PENDING_INTENT_FORGOT_PATTERN = CLASSNAME + ".pending_intent_forgot_pattern";
    public static final String EXTRA_PATTERN = CLASSNAME + ".pattern";

    private static final long DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW = SECOND_IN_MILLIS;

    private boolean autoSave, stealthMode;
    private IEncrypter encrypter;
    protected int maxRetries, minWiredDots, retryCount = 0, captchaWiredDots;

    private LoadingView<Void, Void, Object> loadingView;
    private LockPatternView lockPatternView;
    private View progressView;
    private Intent intentResult;

    private final Runnable mLockPatternViewReloader = new Runnable() {

        @Override
        public void run() {
            lockPatternView.clearPattern();
            onPatternCleared();
        }

    };

    public LockPatternType getLockPatternType() {
        return lockPatternType;
    }

    private LockPatternType lockPatternType;

    public static enum LockPatternType {
        CREATE_PATTERN, COMPARE_PATTERN, VERIFY_CAPTCHA
    }

    public static enum ResuleType {
        COMPARE_OK, COMPARE_FAIL, PATTERN_CREATE, MIN_DOTS_FAIL
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        intentResult = new Intent();
        setResult(Activity.RESULT_CANCELED, intentResult);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        /**
         * Use this hook instead of onBackPressed(), because onBackPressed() is not available in API 4.
         */
        if (keyCode == KeyEvent.KEYCODE_BACK && LockPatternType.COMPARE_PATTERN == lockPatternType) {
            if (loadingView != null) {
                loadingView.cancel(true);
            }

            finishWithNegativeResult(RESULT_CANCELED);

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        /**
         * Support canceling dialog on touching outside in APIs < 11.
         *
         * This piece of code is copied from android.view.Window. You can find it by searching for methods
         * shouldCloseOnTouch() and isOutOfBounds().
         */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB && event.getAction() == MotionEvent.ACTION_DOWN
                && getWindow().peekDecorView() != null) {
            final int x = (int) event.getX();
            final int y = (int) event.getY();
            final int slop = ViewConfiguration.get(this).getScaledWindowTouchSlop();
            final View decorView = getWindow().getDecorView();
            boolean isOutOfBounds = (x < -slop) || (y < -slop) || (x > (decorView.getWidth() + slop))
                    || (y > (decorView.getHeight() + slop));
            if (isOutOfBounds) {
                finishWithNegativeResult(RESULT_CANCELED);
                return true;
            }
        }

        return super.onTouchEvent(event);
    }

    @Override
    protected void onDestroy() {
        if (loadingView != null) {
            loadingView.cancel(true);
        }

        super.onDestroy();
    }

    protected final void init(LockPatternView lockPatternView) {
        this.lockPatternView = lockPatternView;
        initLockPatternType();
        initSettings();
        initLockPatternView();
        if (lockPatternType == LockPatternType.VERIFY_CAPTCHA) {
            final ArrayList<LockPatternView.Cell> pattern;
            if (getIntent().hasExtra(EXTRA_PATTERN)) {
                pattern = getIntent().getParcelableArrayListExtra(EXTRA_PATTERN);
            } else {
                getIntent().putParcelableArrayListExtra(EXTRA_PATTERN, pattern = LockPatternUtils.genCaptchaPattern(captchaWiredDots));
            }

            lockPatternView.setPattern(LockPatternView.DisplayMode.Animate, pattern);
        }
        onInit(lockPatternType);
    }

    protected void onInit(LockPatternType lockPatternType) {}

    protected final void setUpProgressView(View view) {
        this.progressView = view;
    }

    private void initLockPatternType() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_LOCK_PATTERN_TYPE)) {
            lockPatternType = (LockPatternType) intent.getSerializableExtra(EXTRA_LOCK_PATTERN_TYPE);
        }
        if (lockPatternType == null) {
            throw new IllegalArgumentException("intent not putExtra(EXTRA_LOCK_PATTERN_TYPE, LockPatternType)");
        }
    }

    private final void initLockPatternView() {
        LockPatternView.DisplayMode lastDisplayMode = lockPatternView != null ? lockPatternView.getDisplayMode() : null;
        List<LockPatternView.Cell> lastPattern = lockPatternView != null ? lockPatternView.getPattern() : null;

        switch (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) {
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
            case Configuration.SCREENLAYOUT_SIZE_XLARGE: {
                final int size = getResources().getDimensionPixelSize(R.dimen.alp_42447968_lockpatternview_size);
                ViewGroup.LayoutParams lp = lockPatternView.getLayoutParams();
                lp.width = size;
                lp.height = size;
                lockPatternView.setLayoutParams(lp);

                break;
            }// LARGE / XLARGE
        }

        boolean hapticFeedbackEnabled = false;
        try {
            hapticFeedbackEnabled = Settings.System.getInt(getContentResolver(),
                    Settings.System.HAPTIC_FEEDBACK_ENABLED, 0) != 0;
        } catch (Throwable t) {
            /**
             * Ignore it.
             */
        }
        lockPatternView.setTactileFeedbackEnabled(hapticFeedbackEnabled);
        lockPatternView.setOnPatternListener(this);
        lockPatternView.setInStealthMode(stealthMode && lockPatternType != LockPatternType.VERIFY_CAPTCHA);

        if (lastPattern != null && lastDisplayMode != null && lockPatternType != LockPatternType.VERIFY_CAPTCHA) {
            lockPatternView.setPattern(lastDisplayMode, lastPattern);
        }
    }


    private void initSettings() {
        Bundle metaData = null;
        try {
            metaData = getPackageManager().getActivityInfo(getComponentName(), PackageManager.GET_META_DATA).metaData;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (metaData != null && metaData.containsKey(METADATA_MIN_WIRED_DOTS)) {
            minWiredDots = AlpSettings.Display.validateMinWiredDots(this, metaData.getInt(METADATA_MIN_WIRED_DOTS));
        } else {
            minWiredDots = AlpSettings.Display.getMinWiredDots(this);
        }

        if (metaData != null && metaData.containsKey(METADATA_MAX_RETRIES)) {
            maxRetries = AlpSettings.Display.validateMaxRetries(this, metaData.getInt(METADATA_MAX_RETRIES));
        } else {
            maxRetries = AlpSettings.Display.getMaxRetries(this);
        }

        if (metaData != null && metaData.containsKey(METADATA_AUTO_SAVE_PATTERN)) {
            autoSave = metaData.getBoolean(METADATA_AUTO_SAVE_PATTERN);
        } else {
            autoSave = AlpSettings.Security.isAutoSavePattern(this);
        }

        if (metaData != null && metaData.containsKey(METADATA_CAPTCHA_WIRED_DOTS)) {
            captchaWiredDots = AlpSettings.Display.validateCaptchaWiredDots(this, metaData.getInt(METADATA_CAPTCHA_WIRED_DOTS));
        } else {
            captchaWiredDots = AlpSettings.Display.getCaptchaWiredDots(this);
        }

        if (metaData != null && metaData.containsKey(METADATA_STEALTH_MODE)) {
            stealthMode = metaData.getBoolean(METADATA_STEALTH_MODE);
        } else {
            stealthMode = AlpSettings.Display.isStealthMode(this);
        }

        char[] encrypterClass;
        if (metaData != null && metaData.containsKey(METADATA_ENCRYPTER_CLASS)) {
            encrypterClass = metaData.getString(METADATA_ENCRYPTER_CLASS).toCharArray();
        } else {
            encrypterClass = AlpSettings.Security.getEncrypterClass(this);
        }

        if (encrypterClass != null) {
            try {
                encrypter = (IEncrypter) Class.forName(new String(encrypterClass), false, getClassLoader()).newInstance();
            } catch (Throwable t) {
                throw new InvalidEncrypterException();
            }
        }
    }

    protected void onPatternStart(LockPatternType lockPatternType) {}
    protected void onPatternDetected(LockPatternType lockPatternType) {}
    protected void onPatternCellAdded(LockPatternType lockPatternType) {}
    protected void onPatternCleared(LockPatternType lockPatternType) {}

    @Override
    public void onPatternStart() {
        lockPatternView.removeCallbacks(mLockPatternViewReloader);
        lockPatternView.setDisplayMode(LockPatternView.DisplayMode.Correct);
        onPatternStart(lockPatternType);
    }

    @Override
    public void onPatternCleared() {
        lockPatternView.removeCallbacks(mLockPatternViewReloader);
        switch (lockPatternType) {
            case CREATE_PATTERN:
            case COMPARE_PATTERN:
                lockPatternView.setDisplayMode(LockPatternView.DisplayMode.Correct);
                break;
            case VERIFY_CAPTCHA:
                List<LockPatternView.Cell> pattern = getIntent().getParcelableArrayListExtra(EXTRA_PATTERN);
                lockPatternView.setPattern(LockPatternView.DisplayMode.Animate, pattern);
                break;
        }
        onPatternCleared(lockPatternType);
    }

    @Override
    public void onPatternCellAdded(List<LockPatternView.Cell> pattern) {
        onPatternCellAdded(lockPatternType);
    }

    @Override
    public void onPatternDetected(List<LockPatternView.Cell> pattern) {
        switch (lockPatternType) {
            case CREATE_PATTERN:
                doCheckAndCreatePattern(pattern);
                break;
            case COMPARE_PATTERN:
                doComparePattern(pattern);
                break;
            case VERIFY_CAPTCHA:
                if (!LockPatternView.DisplayMode.Animate.equals(lockPatternView.getDisplayMode())) {
                    doComparePattern(pattern);
                }
                break;
        }
        onPatternDetected(lockPatternType);
    }

    private void executeLockPatternTask(final List<LockPatternView.Cell> pattern) {
        View view = progressView;
        if (view == null) {
            view = new android.support.v4.widget.Space(this);
        }

        final boolean hasExtra = getIntent().hasExtra(EXTRA_PATTERN);
        loadingView = new LoadingView<Void, Void, Object>(this, view) {
            @Override
            protected Object doInBackground(Void... params) {
                switch (lockPatternType) {
                    case CREATE_PATTERN:
                        if (!hasExtra) {
                            return encrypter != null ? encrypter.encrypt(BaseActivity.this, pattern) : LockPatternUtils.patternToSha1(pattern).toCharArray();
                        } else {
                            if (encrypter != null) {
                                return pattern.equals(encrypter.decrypt(BaseActivity.this, getIntent().getCharArrayExtra(EXTRA_PATTERN)));
                            } else {
                                return Arrays.equals(getIntent().getCharArrayExtra(EXTRA_PATTERN), LockPatternUtils.patternToSha1(pattern).toCharArray());
                            }
                        }
                    case COMPARE_PATTERN:
                        char[] currentPattern = getIntent().getCharArrayExtra(EXTRA_PATTERN);
                        if (currentPattern == null)
                            currentPattern = AlpSettings.Security.getPattern(BaseActivity.this);
                        if (currentPattern != null) {
                            if (encrypter != null) {
                                return pattern.equals(encrypter.decrypt(BaseActivity.this, currentPattern));
                            } else {
                                return Arrays.equals(currentPattern, LockPatternUtils.patternToSha1(pattern).toCharArray());
                            }
                        }
                        break;
                    case VERIFY_CAPTCHA:
                        return pattern.equals(getIntent().getParcelableArrayListExtra(EXTRA_PATTERN));
                }
                return false;

            }

            @Override
            protected void onPostExecute(Object result) {
                super.onPostExecute(result);
                switch (lockPatternType) {
                    case CREATE_PATTERN:
                        if (!hasExtra) {
                            getIntent().putExtra(EXTRA_PATTERN, (char[]) result);
                            doLockPatternResult(ResuleType.PATTERN_CREATE);
                        } else {
                            if (!(Boolean) result) {
                                lockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                                lockPatternView.postDelayed(mLockPatternViewReloader, DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW);
                                doLockPatternResult(ResuleType.COMPARE_FAIL);
                            } else {
                                doLockPatternResult(ResuleType.COMPARE_OK);
                            }
                        }
                        break;
                    case COMPARE_PATTERN:
                    case VERIFY_CAPTCHA:
                        retryCount++;
                        if (!(Boolean) result) {
                            if (retryCount >= maxRetries) {
//                                finishWithNegativeResult(RESULT_FAILED);
                            } else {
                                lockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                                lockPatternView.postDelayed(mLockPatternViewReloader, DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW);
                            }
                            doLockPatternResult(ResuleType.COMPARE_FAIL);
                        } else {
//                            finishWithResultOk(null);
                            doLockPatternResult(ResuleType.COMPARE_OK);
                        }
                        break;
                }
            }
        };
        loadingView.execute();
    }

    protected void doLockPatternResult(ResuleType resuleType) {
        switch (resuleType) {
            case MIN_DOTS_FAIL:
                Toast.makeText(this, "至少连接" + minWiredDots + "个点，请重试", Toast.LENGTH_SHORT).show();
                break;
            case PATTERN_CREATE:
                lockPatternView.clearPattern();
                break;
            case COMPARE_OK:
                if (lockPatternType == LockPatternType.CREATE_PATTERN) {
                    finishWithResultOk(getIntent().getCharArrayExtra(EXTRA_PATTERN));
                } else {
                    finishWithResultOk(null);
                }
                break;
            case COMPARE_FAIL:
                if (lockPatternType == LockPatternType.CREATE_PATTERN) {
                    // not do something
                } else {
                    if (retryCount >= maxRetries) {
                        finishWithNegativeResult(RESULT_FAILED);
                    }
                }
                break;
        }
    }

    private void doCheckAndCreatePattern(List<LockPatternView.Cell> pattern) {
        if (pattern.size() < minWiredDots) {
            lockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
            doLockPatternResult(ResuleType.MIN_DOTS_FAIL);
            lockPatternView.postDelayed(mLockPatternViewReloader, DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW);
            return;
        }
        executeLockPatternTask(pattern);
    }

    private void doComparePattern(final List<LockPatternView.Cell> pattern) {
        if (pattern == null) return;
        executeLockPatternTask(pattern);
    }

    protected final void finishWithResultOk(char[] pattern) {
        if (LockPatternType.CREATE_PATTERN == lockPatternType) {
            if (autoSave) {
                AlpSettings.Security.setPattern(this, pattern);
            }
            intentResult.putExtra(EXTRA_PATTERN, pattern);
        } else {
            intentResult.putExtra(EXTRA_RETRY_COUNT, retryCount);
        }

        setResult(Activity.RESULT_OK, intentResult);

        /**
         * ResultReceiver
         */
        ResultReceiver receiver = getIntent().getParcelableExtra(EXTRA_RESULT_RECEIVER);
        if (receiver != null) {
            Bundle bundle = new Bundle();
            if (LockPatternType.CREATE_PATTERN == lockPatternType)
                bundle.putCharArray(EXTRA_PATTERN, pattern);
            else {
                /**
                 * If the user was "logging in", minimum try count can not be zero.
                 */
                bundle.putInt(EXTRA_RETRY_COUNT, retryCount);
            }
            receiver.send(Activity.RESULT_OK, bundle);
        }

        /**
         * PendingIntent
         */
        PendingIntent pi = getIntent().getParcelableExtra(EXTRA_PENDING_INTENT_OK);
        if (pi != null) {
            try {
                pi.send(this, Activity.RESULT_OK, intentResult);
            } catch (Throwable t) {
                Log.e(CLASSNAME, "Error sending PendingIntent: " + pi, t);
            }
        }

        finish();
    }

    protected final void finishWithNegativeResult(int resultCode) {
        if (LockPatternType.CREATE_PATTERN != lockPatternType) {
            intentResult.putExtra(EXTRA_RETRY_COUNT, retryCount);
        }

        setResult(resultCode, intentResult);

        /**
         * ResultReceiver
         */
        ResultReceiver receiver = getIntent().getParcelableExtra(EXTRA_RESULT_RECEIVER);
        if (receiver != null) {
            Bundle resultBundle = null;
            if (LockPatternType.COMPARE_PATTERN == lockPatternType) {
                resultBundle = new Bundle();
                resultBundle.putInt(EXTRA_RETRY_COUNT, retryCount);
            }
            receiver.send(resultCode, resultBundle);
        }

        /**
         * PendingIntent
         */
        PendingIntent pi = getIntent().getParcelableExtra(EXTRA_PENDING_INTENT_CANCELLED);
        if (pi != null) {
            try {
                pi.send(this, resultCode, intentResult);
            } catch (Throwable t) {
                Log.e(CLASSNAME, "Error sending PendingIntent: " + pi, t);
            }
        }

        finish();
    }

    protected final void finishWithForgotPatternResult(int resultCode) {
        if (LockPatternType.CREATE_PATTERN != lockPatternType) {
            intentResult.putExtra(EXTRA_RETRY_COUNT, retryCount);
        }

        setResult(resultCode, intentResult);

        /**
         * ResultReceiver
         */
        ResultReceiver receiver = getIntent().getParcelableExtra(EXTRA_RESULT_RECEIVER);
        if (receiver != null) {
            Bundle resultBundle = null;
            if (LockPatternType.COMPARE_PATTERN == lockPatternType) {
                resultBundle = new Bundle();
                resultBundle.putInt(EXTRA_RETRY_COUNT, retryCount);
            }
            receiver.send(resultCode, resultBundle);
        }

        /**
         * PendingIntent
         */
        PendingIntent pi = getIntent().getParcelableExtra(EXTRA_PENDING_INTENT_FORGOT_PATTERN);
        if (pi != null) {
            try {
                pi.send();
            } catch (Throwable t) {
                Log.e(CLASSNAME, "Error sending PendingIntent: " + pi, t);
            }
        }

        finish();
    }
}
