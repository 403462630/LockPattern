package com.mylockpatternexample;

import android.app.PendingIntent;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import haibison.android.lockpattern.BaseActivity;
import haibison.android.lockpattern.LockPatternActivity;
import haibison.android.lockpattern.util.AlpSettings;


public class MainActivity extends ActionBarActivity {

    private static final String TAG = "MainActivity";
    private static final int REQ_CREATE_PATTERN = 1;
    private static final int REQ_ENTER_PATTERN = 2;
    private static final int REQ_VERIFY_CAPTCHA = 3;
    private static final int REQ_FORGOT_PATTERN = 4;
    private static final int REQ_FORGOT_PATTERN2 = 5;
    private char[] savedPattern;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AlpSettings.Display.setStealthMode(this, false);
        AlpSettings.Display.setMinWiredDots(this, 3);
        AlpSettings.Display.setCaptchaWiredDots(this, 6);
        findViewById(R.id.bt_new_pattern).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(LockPatternActivity.ACTION_CREATE_PATTERN, null,
//                        MainActivity.this, LockPatternActivity.class);
                Intent intent = new Intent(MainActivity.this, TestLockPatternActivity.class);
                intent.putExtra(TestLockPatternActivity.EXTRA_LOCK_PATTERN_TYPE, BaseActivity.LockPatternType.CREATE_PATTERN);
                startActivityForResult(intent, REQ_CREATE_PATTERN);
            }
        });
        findViewById(R.id.bt_enter_pattern).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(LockPatternActivity.ACTION_COMPARE_PATTERN, null,
//                        MainActivity.this, LockPatternActivity.class);
//                intent.putExtra(LockPatternActivity.EXTRA_PATTERN, savedPattern);
                Intent intent = new Intent(MainActivity.this, TestLockPatternActivity.class);
                intent.putExtra(TestLockPatternActivity.EXTRA_LOCK_PATTERN_TYPE, BaseActivity.LockPatternType.COMPARE_PATTERN);
                intent.putExtra(TestLockPatternActivity.EXTRA_PATTERN, savedPattern);
                startActivityForResult(intent, REQ_ENTER_PATTERN);
            }
        });
        findViewById(R.id.bt_verify_captcha).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(LockPatternActivity.ACTION_VERIFY_CAPTCHA, null,
//                        MainActivity.this, LockPatternActivity.class);
                Intent intent = new Intent(MainActivity.this, TestLockPatternActivity.class);
                intent.putExtra(TestLockPatternActivity.EXTRA_LOCK_PATTERN_TYPE, BaseActivity.LockPatternType.VERIFY_CAPTCHA);
                startActivityForResult(intent, REQ_VERIFY_CAPTCHA);
            }
        });
        findViewById(R.id.bt_forgot_pattern).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ForgotPatternActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, REQ_FORGOT_PATTERN2, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                Intent intentActivity = new Intent(LockPatternActivity.ACTION_COMPARE_PATTERN, null,
                        MainActivity.this, LockPatternActivity.class);
                intentActivity.putExtra(LockPatternActivity.EXTRA_PENDING_INTENT_FORGOT_PATTERN,
                        pendingIntent);
                startActivityForResult(intentActivity, REQ_FORGOT_PATTERN);
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_CREATE_PATTERN: {
                if (resultCode == RESULT_OK) {
                    char[] pattern = data.getCharArrayExtra(TestLockPatternActivity.EXTRA_PATTERN);
                    savedPattern = pattern;
                    Toast.makeText(MainActivity.this, pattern.toString(), Toast.LENGTH_SHORT).show();
                }
                int retryCount = data.getIntExtra(TestLockPatternActivity.EXTRA_RETRY_COUNT, 0);
                Toast.makeText(MainActivity.this, "retryCount: " + retryCount, Toast.LENGTH_SHORT).show();
                break;
            }// REQ_CREATE_PATTERN
            case REQ_ENTER_PATTERN:{
                switch (resultCode) {
                    case RESULT_OK:
                        // The user passed
                        Toast.makeText(MainActivity.this, "The user passed", Toast.LENGTH_SHORT).show();
                        break;
                    case RESULT_CANCELED:
                        // The user cancelled the task
                        Toast.makeText(MainActivity.this, "The user cancelled the task", Toast.LENGTH_SHORT).show();
                        break;
                    case TestLockPatternActivity.RESULT_FAILED:
                        // The user failed to enter the pattern
                        Toast.makeText(MainActivity.this, "The user failed to enter the pattern", Toast.LENGTH_SHORT).show();
                        break;
                    case LockPatternActivity.RESULT_FORGOT_PATTERN:
                        // The user forgot the pattern and invoked your recovery Activity.
                        Toast.makeText(MainActivity.this, "The user forgot the pattern and invoked your recovery Activity", Toast.LENGTH_SHORT).show();
                        break;
                }
                int retryCount = data.getIntExtra(TestLockPatternActivity.EXTRA_RETRY_COUNT, 0);
                Toast.makeText(MainActivity.this, "retryCount: " + retryCount, Toast.LENGTH_SHORT).show();
                break;
            }
            case REQ_VERIFY_CAPTCHA: {
                switch (resultCode) {
                    case RESULT_OK:
                        // The user passed
                        Toast.makeText(MainActivity.this, "The user passed", Toast.LENGTH_SHORT).show();
                        break;
                    case RESULT_CANCELED:
                        // The user cancelled the task
                        Toast.makeText(MainActivity.this, "The user cancelled the task", Toast.LENGTH_SHORT).show();
                        break;
                    case TestLockPatternActivity.RESULT_FAILED:
                        // The user failed to enter the pattern
                        Toast.makeText(MainActivity.this, "The user failed to enter the pattern", Toast.LENGTH_SHORT).show();
                        break;
                }
                int retryCount = data.getIntExtra(TestLockPatternActivity.EXTRA_RETRY_COUNT, 0);
                Toast.makeText(MainActivity.this, "retryCount: " + retryCount, Toast.LENGTH_SHORT).show();
                break;
            }
            case REQ_FORGOT_PATTERN:
                Toast.makeText(MainActivity.this, "REQ_FORGOT_PATTERN", Toast.LENGTH_SHORT).show();
                break;
            case REQ_FORGOT_PATTERN2:
                Toast.makeText(MainActivity.this, "REQ_FORGOT_PATTERN2", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
