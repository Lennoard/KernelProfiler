package com.smartpack.kernelprofiler.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.app.ActivityCompat;

import com.smartpack.kernelprofiler.R;
import com.smartpack.kernelprofiler.utils.root.RootUtils;

import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * Created by sunilpaulmathew <sunil.kde@gmail.com> on May 22, 2020
 */

public class CreateProfileActivity extends AppCompatActivity {

    public static final String TITLE = "title";

    AppCompatEditText mProfileDescriptionHint;
    AppCompatEditText mProfileDetailsHint;
    AppCompatTextView mTitle;
    AppCompatTextView mTestButton;
    AppCompatTextView mTestOutput;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_createprofile);

        AppCompatImageButton mBack = findViewById(R.id.back_button);
        mBack.setOnClickListener(v -> onBackPressed());
        AppCompatImageButton mSave = findViewById(R.id.save_button);
        mProfileDescriptionHint = findViewById(R.id.profile_description_hint);
        mProfileDetailsHint = findViewById(R.id.profile_details_hint);
        mTitle = findViewById(R.id.title);
        String title = getIntent().getStringExtra(TITLE);
        if (title != null) {
            mTitle.setText(title);
        }
        mTestButton = findViewById(R.id.test_button);
        mTestOutput = findViewById(R.id.test_output);
        mBack.setOnClickListener(v -> onBackPressed());
        mSave.setOnClickListener(v -> {
            if (Utils.checkWriteStoragePermission(this)) {
                if (mProfileDetailsHint.getText() != null && !mProfileDetailsHint.getText().toString().equals("")) {
                    Utils.create("#!/system/bin/sh\n\n# Description=" + mProfileDescriptionHint.getText() + "\n\n" + mProfileDetailsHint.getText(),
                            Environment.getExternalStorageDirectory().toString() + "/" + title);
                    Utils.snackbarIndenite(mTitle, getString(R.string.create_profile_message, title) + " " +
                            Environment.getExternalStorageDirectory().toString());
                } else {
                    Utils.snackbar(mTitle, getString(R.string.profile_details_empty));
                }
            } else {
                ActivityCompat.requestPermissions(this, new String[] {
                        Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                Utils.snackbar(mTitle, getString(R.string.storage_access_denied) + " " +
                        Environment.getExternalStorageDirectory().toString());
            }
        });
        mTestButton.setOnClickListener(v -> {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            testCommands(new WeakReference<>(this));
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
        });
        mTestOutput = findViewById(R.id.test_output);
        refreshStatus();
    }

    private void createTestScript() {
        Utils.create("#!/system/bin/sh\n\n" + Objects.requireNonNull(mProfileDetailsHint.getText()).toString(),"/data/local/tmp/sm");
    }

    @SuppressLint("StaticFieldLeak")
    private void testCommands(WeakReference<Activity> activityRef) {
        new AsyncTask<Void, Void, Void>() {
            private ProgressDialog mProgressDialog;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mProgressDialog = new ProgressDialog(activityRef.get());
                mProgressDialog.setMessage(activityRef.get().getString(R.string.testing));
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();
                KP.mTestingProfile = true;
            }
            @SuppressLint("WrongThread")
            @Override
            protected Void doInBackground(Void... voids) {
                if (KP.mOutput == null) {
                    KP.mOutput = new StringBuilder();
                } else {
                    KP.mOutput.setLength(0);
                }
                Utils.delete("/data/local/tmp/sm");
                createTestScript();
                String output = RootUtils.runAndGetError("sh  /data/local/tmp/sm");
                if (output.isEmpty()) {
                    output = activityRef.get().getString(R.string.testing_success);
                }
                KP.mOutput.append(output);
                Utils.delete("/data/local/tmp/sm");
                return null;
            }
            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                try {
                    mProgressDialog.dismiss();
                } catch (IllegalArgumentException ignored) {
                }
                KP.mTestingProfile = false;
            }
        }.execute();
    }

    private void refreshStatus() {
        new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(100);
                        runOnUiThread(() -> {
                            if (mTestOutput != null && KP.mOutput != null) {
                                mTestOutput.setVisibility(View.VISIBLE);
                                mTestOutput.setText(KP.mOutput.toString());
                            }
                        });
                    }
                } catch (InterruptedException ignored) {}
            }
        }.start();
    }

    @Override
    public void onBackPressed() {
        if (KP.mTestingProfile) return;
        super.onBackPressed();
    }

}