package com.mylockpatternexample;

import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.ViewGroup;

import java.util.List;

import haibison.android.lockpattern.util.AlpSettings;
import haibison.android.lockpattern.util.IEncrypter;
import haibison.android.lockpattern.util.InvalidEncrypterException;
import haibison.android.lockpattern.util.UI;
import haibison.android.lockpattern.widget.LockPatternView;
import haibison.android.lockpattern.widget.LockPatternView.OnPatternListener;

import static haibison.android.lockpattern.util.AlpSettings.Display.METADATA_CAPTCHA_WIRED_DOTS;
import static haibison.android.lockpattern.util.AlpSettings.Display.METADATA_MAX_RETRIES;
import static haibison.android.lockpattern.util.AlpSettings.Display.METADATA_MIN_WIRED_DOTS;
import static haibison.android.lockpattern.util.AlpSettings.Display.METADATA_STEALTH_MODE;
import static haibison.android.lockpattern.util.AlpSettings.Security.METADATA_AUTO_SAVE_PATTERN;
import static haibison.android.lockpattern.util.AlpSettings.Security.METADATA_ENCRYPTER_CLASS;


public class ForgotPatternActivity extends ActionBarActivity implements OnPatternListener {

    private static final String CLASSNAME = ForgotPatternActivity.class.getName();
    public static final String ACTION_CREATE_PATTERN = CLASSNAME + ".CREATE_PATTERN";
    public static final String ACTION_COMPARE_PATTERN = CLASSNAME + ".COMPARE_PATTERN";
    public static final String ACTION_VERIFY_CAPTCHA = CLASSNAME + ".VERIFY_CAPTCHA";

    public static final String EXTRA_PATTERN = CLASSNAME + ".PATTERN";

    private boolean autoSave, stealthMode;
    private IEncrypter encrypter;
    private int maxRetries, minWiredDots, retryCount = 0, captchaWiredDots;

    private LockPatternView lockPatternView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_pattern);
        lockPatternView = (LockPatternView) findViewById(R.id.lock_pattern_view);
    }

    private void initContentView() {
        LockPatternView.DisplayMode lastDisplayMode = lockPatternView != null ? lockPatternView.getDisplayMode() : null;
        List<LockPatternView.Cell> lastPattern = lockPatternView != null ? lockPatternView.getPattern() : null;

        UI.adjustDialogSizeForLargeScreens(getWindow());

        /**
         * LOCK PATTERN VIEW
         */
        switch (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) {
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
            case Configuration.SCREENLAYOUT_SIZE_XLARGE: {
                final int size = getResources().getDimensionPixelSize(
                        haibison.android.lockpattern.R.dimen.lock_pattern_view_size);
                ViewGroup.LayoutParams lp = lockPatternView.getLayoutParams();
                lp.width = size;
                lp.height = size;
                lockPatternView.setLayoutParams(lp);

                break;
            }// LARGE / XLARGE
        }

        /**
         * Haptic feedback.
         */
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

        lockPatternView.setInStealthMode(stealthMode && !ACTION_VERIFY_CAPTCHA.equals(getIntent().getAction()));
        lockPatternView.setOnPatternListener(this);
        if (lastPattern != null && lastDisplayMode != null && !ACTION_VERIFY_CAPTCHA.equals(getIntent().getAction())) {
            lockPatternView.setPattern(lastDisplayMode, lastPattern);
        }
    }

    private void loadSettings() {
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


    @Override
    public void onPatternStart() {

    }

    @Override
    public void onPatternCleared() {

    }

    @Override
    public void onPatternCellAdded(List<LockPatternView.Cell> pattern) {

    }

    @Override
    public void onPatternDetected(List<LockPatternView.Cell> pattern) {

    }
}
