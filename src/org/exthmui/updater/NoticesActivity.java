/*
 * Copyright (C) 2020 The exTHmUI Project, Copyright (C) 2017 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.exthmui.updater;

import android.app.Application;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import org.exthmui.updater.controller.UpdaterController;
import org.exthmui.updater.controller.UpdaterService;
import org.exthmui.updater.download.DownloadClient;
import org.exthmui.updater.misc.Constants;
import org.exthmui.updater.misc.Utils;
import org.exthmui.updater.model.NoticeInfo;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class NoticesActivity extends BaseActivity {

    private static final String TAG = "NoticesActivity";
    private static Application instance;
    private UpdaterService mUpdaterService;
    private NoticesListAdapter mNoticesListAdapter;
    private final ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            UpdaterService.LocalBinder binder = (UpdaterService.LocalBinder) service;
            mUpdaterService = binder.getService();
            mNoticesListAdapter.setUpdaterController(mUpdaterService.getUpdaterController());
            getNoticesList();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mNoticesListAdapter.setUpdaterController(null);
            mUpdaterService = null;
            mNoticesListAdapter.notifyDataSetChanged();
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(this, UpdaterService.class);

        startService(intent);

        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        if (mUpdaterService != null) {
            unbindService(mConnection);
        }
        super.onStop();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notices);

        instance = getApplication();

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        RecyclerView noticeView = findViewById(R.id.notice_view);
        mNoticesListAdapter = new NoticesListAdapter(this, false);
        noticeView.setAdapter(mNoticesListAdapter);
        RecyclerView.LayoutManager noticeLayoutManager = new LinearLayoutManager(this);
        noticeView.setLayoutManager(noticeLayoutManager);
    }

    private void loadNoticesList(File jsonFile)
            throws IOException, JSONException {
        Log.d(TAG, "Adding remote notices");
        UpdaterController controller = mUpdaterService.getUpdaterController();

        List<NoticeInfo> notices = Utils.parseJsonNotice(jsonFile);
        for (NoticeInfo notice : notices) {
            controller.addNotice(notice);
        }

        List<String> noticeIds = new ArrayList<>();
        List<NoticeInfo> sortedNotices = controller.getNotices();
        if (sortedNotices.isEmpty()) {
            findViewById(R.id.no_notices_view).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.no_notices_view).setVisibility(View.GONE);
            //sortedNotices.sort((u1, u2) -> String.compare(u2.getId(), u1.getId()));//It's already sorted in Utils.parseJsonUpdate(...)
            for (NoticeInfo notice : sortedNotices) {
                noticeIds.add(notice.getId());
            }
            mNoticesListAdapter.setData(noticeIds);
            mNoticesListAdapter.notifyDataSetChanged();
        }
    }

    private void getNoticesList() {
        File cachedNoticeList = Utils.getCachedNoticeList(this);
        if (cachedNoticeList.exists()) {
            try {
                loadNoticesList(cachedNoticeList);
                Log.d(TAG, "Cached notice list parsed");
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error while parsing json list", e);
            }
        } else {
            downloadNoticesList();
        }
    }

    private void processNewJson(File json, File jsonNew) {
        try {
            loadNoticesList(jsonNew);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            long millis = System.currentTimeMillis();
            preferences.edit().putLong(Constants.PREF_LAST_UPDATE_CHECK, millis).apply();
            if (json.exists() && Utils.isUpdateCheckEnabled(this) &&
                    Utils.checkForNewUpdates(json, jsonNew, false)) {
                UpdatesCheckReceiver.updateRepeatingUpdatesCheck(this);
            }
            // In case we set a one-shot check because of a previous failure
            UpdatesCheckReceiver.cancelUpdatesCheck(this);
            //noinspection ResultOfMethodCallIgnored
            jsonNew.renameTo(json);
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Could not read json", e);
            showSnackbar(R.string.snack_notices_check_failed, Snackbar.LENGTH_LONG);
        }
    }

    private void downloadNoticesList() {
        final File jsonFile = Utils.getCachedNoticeList(this);
        final File jsonFileTmp = new File(jsonFile.getAbsolutePath() + UUID.randomUUID());
        String url = Utils.getServerURL(this, false);
        Log.d(TAG, "Checking " + url);

        DownloadClient.DownloadCallback callback = new DownloadClient.DownloadCallback() {
            @Override
            public void onFailure(final boolean cancelled) {
                Log.e(TAG, "Could not download updates list");
                runOnUiThread(() -> {
                    if (!cancelled) {
                        showSnackbar(R.string.snack_notices_check_failed, Snackbar.LENGTH_LONG);
                    }
                });
            }

            @Override
            public void onResponse(int statusCode, String url, DownloadClient.Headers headers) {
            }

            @Override
            public void onSuccess(File destination) {
                runOnUiThread(() -> {
                    Log.d(TAG, "Notice list downloaded");
                    processNewJson(jsonFile, jsonFileTmp);
                });
            }
        };

        final DownloadClient downloadClient;
        try {
            downloadClient = new DownloadClient.Builder()
                    .setUrl(url)
                    .setDestination(jsonFileTmp)
                    .setDownloadCallback(callback)
                    .build();
        } catch (IOException exception) {
            Log.e(TAG, "Could not build download client");
            showSnackbar(R.string.snack_notices_check_failed, Snackbar.LENGTH_LONG);
            return;
        }

        downloadClient.start();
    }

    @Override
    public void showSnackbar(int stringId, int duration) {
        Snackbar.make(findViewById(R.id.notice_view), stringId, duration).show();
    }
}
