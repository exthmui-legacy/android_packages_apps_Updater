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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.*;
import android.icu.text.DateFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemProperties;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.exthmui.updater.controller.UpdaterController;
import org.exthmui.updater.controller.UpdaterService;
import org.exthmui.updater.download.DownloadClient;
import org.exthmui.updater.misc.*;
import org.exthmui.updater.model.*;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;

import static org.exthmui.updater.misc.Utils.isNetworkAvailable;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";
    private UpdaterService mUpdaterService;
    private BroadcastReceiver mBroadcastReceiver;

    private NoticesListAdapter mNoticesListAdapter;

    private FloatingActionButton mAction;
    private Button mShowChangelog;
    private Button mRefreshButton;
    private TextView mBuildDate;
    private TextView mBuildSize;
    private TextView textLastCheck;
    private TextView headerTitle;
    private ProgressBar mProgressBar;
    private TextView mProgressText;
    private View mRefreshView;
    private TextView mNoNewUpdatesView;

    private UpdaterController mUpdaterController;


    private final ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            UpdaterService.LocalBinder binder = (UpdaterService.LocalBinder) service;
            mUpdaterService = binder.getService();
            mNoticesListAdapter.setUpdaterController(getUpdaterController());
            getLists();
            refreshUpdate(getUpdaterController().getLatestUpdate());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mNoticesListAdapter.setUpdaterController(null);
            mUpdaterService = null;
            mNoticesListAdapter.notifyDataSetChanged();
        }
    };

    private UpdaterController getUpdaterController() {
        if (mUpdaterController == null) {
            if (mUpdaterService != null)
                mUpdaterController = mUpdaterService.getUpdaterController();
            if (mUpdaterController == null)
                mUpdaterController = UpdaterController.getInstanceReceiver(this);
        }
        return mUpdaterController;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocused) {
        if (!(findViewById(R.id.menu_preferences) == null)) {
            findViewById(R.id.menu_preferences).setOnLongClickListener(v -> {
                Toast toast = Toast.makeText(getBaseContext(), getString(R.string.setting_advanced_warning), Toast.LENGTH_LONG);
                toast.show();
                Intent intent = new Intent(MainActivity.this, AdvancedSettings.class);
                startActivity(intent);
                return true;
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(this, UpdaterService.class);

        startService(intent);

        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UpdaterController.ACTION_UPDATE_STATUS);
        intentFilter.addAction(UpdaterController.ACTION_DOWNLOAD_PROGRESS);
        intentFilter.addAction(UpdaterController.ACTION_INSTALL_PROGRESS);
        intentFilter.addAction(UpdaterController.ACTION_UPDATE_REMOVED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        if (mUpdaterService != null) {
            unbindService(mConnection);
        }
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_preferences: {
                showPreferencesDialog();
                return true;
            }
            case R.id.menu_all_updates: {
                Intent intent = new Intent(this, UpdatesActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.menu_notices: {
                Intent intent = new Intent(this, NoticesActivity.class);
                startActivity(intent);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAction = findViewById(R.id.main_action);
        mShowChangelog = findViewById(R.id.main_show_changelog);
        mRefreshButton = findViewById(R.id.main_check_for_updates);

        mBuildDate = findViewById(R.id.main_build_date);
        mBuildSize = findViewById(R.id.main_build_size);

        mProgressBar = findViewById(R.id.main_progress_bar);
        mProgressText = findViewById(R.id.main_progress_text);

        mNoNewUpdatesView = findViewById(R.id.main_no_new_updates_view);

        RecyclerView noticeView = findViewById(R.id.main_notice_view);
        mNoticesListAdapter = new NoticesListAdapter(this, true);
        noticeView.setAdapter(mNoticesListAdapter);
        RecyclerView.LayoutManager noticeLayoutManager = new LinearLayoutManager(this);
        noticeView.setLayoutManager(noticeLayoutManager);
        RecyclerView.ItemAnimator noticeViewItemAnimator = noticeView.getItemAnimator();
        if (noticeViewItemAnimator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) noticeViewItemAnimator).setSupportsChangeAnimations(false);
        }

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (UpdaterController.ACTION_UPDATE_STATUS.equals(intent.getAction())) {
                    String downloadId = intent.getStringExtra(UpdaterController.EXTRA_DOWNLOAD_ID);
                    handleDownloadStatusChange(downloadId);
                }
            }
        };

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        headerTitle = findViewById(R.id.main_title);
        headerTitle.setText(getString(R.string.main_title_text,
                BuildInfoUtils.getBuildVersion()).replace("{os_name}", getString(R.string.os_name)));

        updateLastCheckedString();

        TextView androidVersion = findViewById(R.id.main_android_version);
        androidVersion.setText(
                getString(R.string.main_android_version, Build.VERSION.RELEASE));

        TextView headerBuildDate = findViewById(R.id.main_build_date);
        headerBuildDate.setText(StringGenerator.getDateLocalizedUTC(this,
                DateFormat.LONG, BuildInfoUtils.getBuildDateTimestamp()));

        mRefreshView = findViewById(R.id.main_refreshing);
        mRefreshButton.setOnClickListener(v -> downloadLists(true));
    }

    private void setViewAnim(View resId, float alpha, int duration) {
        resId.animate()
                .alpha(alpha)
                .setDuration(duration)
                .setListener(null)
                .start();
    }

    private void setGoneAnim(View resId) {
        setViewAnim(resId, 0.0f, 500);
    }

    private void setVisibleAnim(View resId) {
        setViewAnim(resId, 1.0f, 500);
    }

    private void refreshView(boolean foundNewUpdate, boolean network) {
        if (network) {
            if (foundNewUpdate) {
                setGoneAnim(textLastCheck);
                setGoneAnim(mRefreshButton);
                mNoNewUpdatesView.setText(R.string.new_updates_found_title);
                mNoNewUpdatesView.setTextColor(getColor(R.color.theme_accent));
                setVisibleAnim(mAction);
                setVisibleAnim(mBuildDate);
                setVisibleAnim(mShowChangelog);
                setGoneAnim(mRefreshView);
            } else {
                mNoNewUpdatesView.setText(R.string.main_no_updates);
                mAction.setVisibility(View.GONE);
                mBuildDate.setVisibility(View.GONE);
                mShowChangelog.setVisibility(View.GONE);
                setGoneAnim(mBuildSize);
                setGoneAnim(mProgressText);
                setGoneAnim(mProgressBar);
                setGoneAnim(mRefreshView);
            }
        } else {
            mNoNewUpdatesView.setText(R.string.network_not_available);
            setGoneAnim(mAction);
            setGoneAnim(mBuildDate);
            setGoneAnim(mShowChangelog);
            setGoneAnim(mBuildSize);
            setGoneAnim(mProgressText);
            setGoneAnim(mProgressBar);
            setGoneAnim(mRefreshButton);
            setGoneAnim(textLastCheck);
            setGoneAnim(mRefreshView);
        }
    }

    private void refreshUpdate(UpdateInfo update) {
        // TODO: use selected update & No update detected
        if (update == null) {
            // No updates detected
            refreshView(false, isNetworkAvailable(this));
            return;
        }

        refreshView(true, isNetworkAvailable(this));
        boolean activeLayout;
        switch (update.getPersistentStatus()) {
            case UpdateStatus.Persistent.UNKNOWN:
                activeLayout = update.getStatus() == UpdateStatus.STARTING;
                break;
            case UpdateStatus.Persistent.VERIFIED:
                activeLayout = update.getStatus() == UpdateStatus.INSTALLING;
                break;
            case UpdateStatus.Persistent.INCOMPLETE:
                activeLayout = true;
                break;
            default:
                throw new RuntimeException("Unknown update status");
        }

        String buildDate = getString(R.string.build_version_date) + StringGenerator.getDateLocalizedUTC(this,
                java.text.DateFormat.LONG, update.getTimestamp());
        String buildVersion = update.getVersionName().replace("{os_name}", getResources().getString(R.string.os_name));
        mBuildDate.setText(buildDate);
        headerTitle.setText(buildVersion);
        headerTitle.setCompoundDrawables(null, null, null, null);

        mShowChangelog.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.changelogs)
                    .setMessage(update.getChangeLog())
                    .show();
        });
        if (activeLayout) {
            handleActiveStatus(update);
        } else {
            handleNotActiveStatus(update);
        }
    }

    private void loadUpdatesList(File jsonFile, boolean manualRefresh)
            throws IOException, JSONException {
        Log.d(TAG, "Adding remote updates");
        UpdaterController controller = getUpdaterController();
        boolean newUpdates = false;

        List<UpdateInfo> updates = Utils.parseJsonUpdate(jsonFile, true);
        List<String> updatesOnline = new ArrayList<>();
        for (UpdateInfo update : updates) {
            newUpdates |= controller.addUpdate(update);
            updatesOnline.add(update.getDownloadId());
        }
        controller.setUpdatesAvailableOnline(updatesOnline, true);

        if (manualRefresh) {
            showSnackbar(
                    newUpdates ? R.string.snack_updates_found : R.string.snack_no_updates_found,
                    Snackbar.LENGTH_SHORT);
        }

        List<String> updateIds = new ArrayList<>();
        List<UpdateInfo> sortedUpdates = controller.getUpdates();

        if (!sortedUpdates.isEmpty()) {
            // Sort:from small to big
            Log.d(TAG, "Sorting updates(list)(small > big).");
            sortedUpdates.sort(Comparator.comparingLong(UpdateBaseInfo::getTimestamp));
            //遍历判断Target与Current版本Timestamp，并循环setChangeLog();合并log.
            long timestamp = SystemProperties.getLong(Constants.PROP_BUILD_DATE, 0);
            Log.d(TAG, "Merging changelog for updates.");
            String changelog = "";
            ListIterator<UpdateInfo> listIterator = sortedUpdates.listIterator();
            while (listIterator.hasNext()) {
                Update u = (Update) listIterator.next();
                if (u.getTimestamp() > timestamp) {
                    changelog = u.getChangeLog() + (changelog.equals("") ? "" : "\n") + changelog;
                    u.setChangeLog(changelog);
                    listIterator.remove();
                    listIterator.add(u);
                }
            }
            // Sort:from big to small
            Log.d(TAG, "Sorting updates(list)(big > small).");
            sortedUpdates.sort((u1, u2) -> Long.compare(u2.getTimestamp(), u1.getTimestamp()));
            for (UpdateInfo update : sortedUpdates) {
                updateIds.add(update.getDownloadId());
            }
        }
    }

    private void loadNoticesList(File jsonFile)
            throws IOException, JSONException {
        Log.d(TAG, "Adding remote notices");
        UpdaterController controller = getUpdaterController();
        boolean newNotices = false;

        List<NoticeInfo> notices = Utils.parseJsonNotice(jsonFile);
        for (NoticeInfo notice : notices) {
            newNotices |= controller.addNotice(notice);
        }

        List<String> noticeIds = new ArrayList<>();
        List<NoticeInfo> sortedNotices = controller.getNotices();
        if (sortedNotices.isEmpty()) {
            findViewById(R.id.main_notice_card).setVisibility(View.GONE);
        } else {
            findViewById(R.id.main_notice_card).setVisibility(View.VISIBLE);
            //sortedNotices.sort((u1, u2) -> String.compare(u2.getId(), u1.getId()));//It's already sorted in Utils.parseJsonUpdate(...)
            for (NoticeInfo notice : sortedNotices) {
                noticeIds.add(notice.getId());
            }
            mNoticesListAdapter.setData(noticeIds);
            mNoticesListAdapter.notifyDataSetChanged();
        }
        refreshUpdate(getUpdaterController().getLatestUpdate());
    }

    private void getLists() {
        getNoticesList();
        getUpdatesList();
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
            downloadNoticesList(true);
        }
    }

    private void getUpdatesList() {
        File cachedUpdateList = Utils.getCachedUpdateList(this);
        if (cachedUpdateList.exists()) {
            try {
                loadUpdatesList(cachedUpdateList, false);
                Log.d(TAG, "Cached update list parsed");
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error while parsing json list", e);
            }
        } else {
            downloadUpdatesList(false);
        }
    }

    private void processNewJson(File json, File jsonNew, boolean manualRefresh, boolean isUpdate) {
        try {
            if (isUpdate) {
                loadUpdatesList(jsonNew, manualRefresh);
            } else {
                loadNoticesList(jsonNew);
            }
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            long millis = System.currentTimeMillis();
            preferences.edit().putLong(Constants.PREF_LAST_UPDATE_CHECK, millis).apply();
            updateLastCheckedString();
            if (json.exists() && Utils.isUpdateCheckEnabled(this) &&
                    Utils.checkForNewUpdates(json, jsonNew, isUpdate)) {
                UpdatesCheckReceiver.updateRepeatingUpdatesCheck(this);
            }
            // In case we set a one-shot check because of a previous failure
            UpdatesCheckReceiver.cancelUpdatesCheck(this);
            //noinspection ResultOfMethodCallIgnored
            jsonNew.renameTo(json);
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Could not read json", e);
            showSnackbar(R.string.snack_updates_check_failed, Snackbar.LENGTH_LONG);
        }
    }

    private void downloadLists(final boolean manualRefresh) {
        downloadNoticesList(manualRefresh);
        downloadUpdatesList(manualRefresh);
    }

    private void downloadNoticesList(final boolean manualRefresh) {
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
                    refreshAnimationStop();
                });
            }

            @Override
            public void onResponse(int statusCode, String url, DownloadClient.Headers headers) {
            }

            @Override
            public void onSuccess(File destination) {
                runOnUiThread(() -> {
                    Log.d(TAG, "Notice list downloaded");
                    processNewJson(jsonFile, jsonFileTmp, manualRefresh, false);
                    refreshAnimationStop();
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

        refreshAnimationStart();
        downloadClient.start();
    }

    private void downloadUpdatesList(final boolean manualRefresh) {
        final File jsonFile = Utils.getCachedUpdateList(this);
        final File jsonFileTmp = new File(jsonFile.getAbsolutePath() + UUID.randomUUID());
        String url = Utils.getServerURL(this, true);
        Log.d(TAG, "Checking " + url);

        DownloadClient.DownloadCallback callback = new DownloadClient.DownloadCallback() {
            @Override
            public void onFailure(final boolean cancelled) {
                Log.e(TAG, "Could not download updates list");
                runOnUiThread(() -> {
                    if (!cancelled) {
                        showSnackbar(R.string.snack_updates_check_failed, Snackbar.LENGTH_LONG);
                    }
                    refreshUpdate(getUpdaterController().getLatestUpdate());
                    refreshAnimationStop();
                });
            }

            @Override
            public void onResponse(int statusCode, String url,
                                   DownloadClient.Headers headers) {
            }

            @Override
            public void onSuccess(File destination) {
                runOnUiThread(() -> {
                    Log.d(TAG, "Update list downloaded");
                    processNewJson(jsonFile, jsonFileTmp, manualRefresh, true);
                    refreshAnimationStop();
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
            showSnackbar(R.string.snack_updates_check_failed, Snackbar.LENGTH_LONG);
            return;
        }

        refreshAnimationStart();
        downloadClient.start();
    }

    private void updateLastCheckedString() {
        final SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        long lastCheck = preferences.getLong(Constants.PREF_LAST_UPDATE_CHECK, -1) / 1000;
        String lastCheckString = getString(R.string.main_last_updates_check,
                StringGenerator.getDateLocalized(this, DateFormat.LONG, lastCheck),
                StringGenerator.getTimeLocalized(this, lastCheck));
        textLastCheck = findViewById(R.id.main_last_check);
        textLastCheck.setText(lastCheckString);
    }

    private void handleDownloadStatusChange(String downloadId) {
        UpdateInfo update = getUpdaterController().getUpdate(downloadId);
        switch (update.getStatus()) {
            case PAUSED_ERROR:
                showSnackbar(R.string.snack_download_failed, Snackbar.LENGTH_LONG);
                break;
            case VERIFICATION_FAILED:
                showSnackbar(R.string.snack_download_verification_failed, Snackbar.LENGTH_LONG);
                break;
            case VERIFIED:
                showSnackbar(R.string.snack_download_verified, Snackbar.LENGTH_LONG);
                break;
        }
    }

    @Override
    public void showSnackbar(int stringId, int duration) {
        Snackbar.make(findViewById(R.id.main_container), stringId, duration).show();
    }

    private void refreshAnimationStart() {
        setGoneAnim(mAction);
        setGoneAnim(mNoNewUpdatesView);
        setGoneAnim(mBuildDate);
        setGoneAnim(mShowChangelog);
        setGoneAnim(mBuildSize);
        setGoneAnim(mProgressText);
        setGoneAnim(mProgressBar);
        setGoneAnim(mRefreshButton);
        setVisibleAnim(mRefreshView);
        textLastCheck.setText(R.string.checking_updates);
    }

    private void refreshAnimationStop() {
        setVisibleAnim(mNoNewUpdatesView);
        setVisibleAnim(headerTitle);
        setGoneAnim(mRefreshView);
        setVisibleAnim(mRefreshButton);
        updateLastCheckedString();
    }

    private void showPreferencesDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.preferences_dialog, null);
        Spinner autoCheckInterval =
                view.findViewById(R.id.preferences_auto_updates_check_interval);
        Switch autoDelete = view.findViewById(R.id.preferences_auto_delete_updates);
        Switch dataWarning = view.findViewById(R.id.preferences_mobile_data_warning);
        Switch abPerfMode = view.findViewById(R.id.preferences_ab_perf_mode);

        if (Utils.isABDevice()) {
            abPerfMode.setVisibility(View.GONE);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        autoCheckInterval.setSelection(Utils.getUpdateCheckSetting(this));
        autoDelete.setChecked(prefs.getBoolean(Constants.PREF_AUTO_DELETE_UPDATES, false));
        dataWarning.setChecked(prefs.getBoolean(Constants.PREF_MOBILE_DATA_WARNING, true));
        abPerfMode.setChecked(prefs.getBoolean(Constants.PREF_AB_PERF_MODE, false));

        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_preferences)
                .setView(view)
                .setOnDismissListener(dialogInterface -> {
                    prefs.edit()
                            .putInt(Constants.PREF_AUTO_UPDATES_CHECK_INTERVAL,
                                    autoCheckInterval.getSelectedItemPosition())
                            .putBoolean(Constants.PREF_AUTO_DELETE_UPDATES,
                                    autoDelete.isChecked())
                            .putBoolean(Constants.PREF_MOBILE_DATA_WARNING,
                                    dataWarning.isChecked())
                            .putBoolean(Constants.PREF_AB_PERF_MODE,
                                    abPerfMode.isChecked())
                            .apply();

                    if (Utils.isUpdateCheckEnabled(this)) {
                        UpdatesCheckReceiver.scheduleRepeatingUpdatesCheck(this);
                    } else {
                        UpdatesCheckReceiver.cancelRepeatingUpdatesCheck(this);
                        UpdatesCheckReceiver.cancelUpdatesCheck(this);
                    }

                    boolean enableABPerfMode = abPerfMode.isChecked();
                    getUpdaterController().setPerformanceMode(enableABPerfMode);
                })
                .show();
    }


    private void handleActiveStatus(UpdateInfo update) {
        UpdaterController controller = getUpdaterController();

        final String downloadId = update.getDownloadId();
        if (controller.isDownloading(downloadId)) {
            String downloaded = StringGenerator.bytesToMegabytes(this,
                    update.getFile().length());
            String total = Formatter.formatFileSize(this, update.getFileSize());
            String percentage = NumberFormat.getPercentInstance().format(
                    update.getProgress() / 100.f);
            long eta = update.getEta();
            if (eta > 0) {
                CharSequence etaString = StringGenerator.formatETA(this, eta * 1000);
                mProgressText.setText(this.getString(
                        R.string.list_download_progress_eta_new, downloaded, total, etaString,
                        percentage));
            } else {
                mProgressText.setText(this.getString(
                        R.string.list_download_progress_new, downloaded, total, percentage));
            }
            UpdateActionsUtils.setButtonAction(this, getUpdaterController(), mAction, UpdateActionsUtils.Action.PAUSE, downloadId, true);
            mProgressBar.setIndeterminate(update.getStatus() == UpdateStatus.STARTING);
            mProgressBar.setProgress(update.getProgress());
        } else if (getUpdaterController().isInstallingUpdate(downloadId)) {
            UpdateActionsUtils.setButtonAction(this, getUpdaterController(), mAction, UpdateActionsUtils.Action.CANCEL_INSTALLATION, downloadId, true);
            boolean notAB = getUpdaterController().isNotInstallingABUpdate();
            mProgressText.setText(notAB ? R.string.dialog_prepare_zip_message :
                    update.getFinalizing() ?
                            R.string.finalizing_package :
                            R.string.preparing_ota_first_boot);
            mProgressBar.setIndeterminate(false);
            mProgressBar.setProgress(update.getInstallProgress());
        } else if (getUpdaterController().isVerifyingUpdate(downloadId)) {
            UpdateActionsUtils.setButtonAction(this, getUpdaterController(), mAction, UpdateActionsUtils.Action.INSTALL, downloadId, false);
            mProgressText.setText(R.string.list_verifying_update);
            mProgressBar.setIndeterminate(true);
        } else {
            UpdateActionsUtils.setButtonAction(this, getUpdaterController(), mAction, UpdateActionsUtils.Action.RESUME, downloadId, Utils.isBusy(controller));
            String downloaded = StringGenerator.bytesToMegabytes(this,
                    update.getFile().length());
            String total = Formatter.formatFileSize(this, update.getFileSize());
            String percentage = NumberFormat.getPercentInstance().format(
                    update.getProgress() / 100.f);
            mProgressText.setText(this.getString(R.string.list_download_progress_new,
                    downloaded, total, percentage));
            mProgressBar.setIndeterminate(false);
            mProgressBar.setProgress(update.getProgress());
        }

        mProgressBar.setVisibility(View.VISIBLE);
        mProgressText.setVisibility(View.VISIBLE);
        mBuildSize.setVisibility(View.INVISIBLE);
    }

    private void handleNotActiveStatus(UpdateInfo update) {
        final String downloadId = update.getDownloadId();
        if (getUpdaterController().isWaitingForReboot(downloadId)) {
            UpdateActionsUtils.setButtonAction(this, getUpdaterController(), mAction, UpdateActionsUtils.Action.REBOOT, downloadId, true);
        } else if (update.getPersistentStatus() == UpdateStatus.Persistent.VERIFIED) {
            UpdateActionsUtils.setButtonAction(this, getUpdaterController(), mAction,
                    Utils.canInstall(update) ? UpdateActionsUtils.Action.INSTALL : UpdateActionsUtils.Action.DELETE,
                    downloadId, Utils.isBusy(getUpdaterController()));
        } else if (!Utils.canInstall(update)) {
            UpdateActionsUtils.setButtonAction(this, getUpdaterController(), mAction, UpdateActionsUtils.Action.INFO, downloadId, Utils.isBusy(getUpdaterController()));
        } else {
            UpdateActionsUtils.setButtonAction(this, getUpdaterController(), mAction, UpdateActionsUtils.Action.DOWNLOAD, downloadId, Utils.isBusy(getUpdaterController()));
        }
        String fileSize = getString(R.string.build_size_text) + Formatter.formatFileSize(this, update.getFileSize());
        mBuildSize.setText(fileSize);

        mProgressBar.setVisibility(View.INVISIBLE);
        mProgressText.setVisibility(View.INVISIBLE);
        mBuildSize.setVisibility(View.VISIBLE);
    }
}
