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
import android.icu.text.DateFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemProperties;
import android.util.Log;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.snackbar.Snackbar;
import org.exthmui.updater.controller.UpdaterController;
import org.exthmui.updater.controller.UpdaterService;
import org.exthmui.updater.download.DownloadClient;
import org.exthmui.updater.misc.BuildInfoUtils;
import org.exthmui.updater.misc.Constants;
import org.exthmui.updater.misc.StringGenerator;
import org.exthmui.updater.misc.Utils;
import org.exthmui.updater.model.NoticeInfo;
import org.exthmui.updater.model.Update;
import org.exthmui.updater.model.UpdateBaseInfo;
import org.exthmui.updater.model.UpdateInfo;
import org.exthmui.updater.ui.OnlineImageView;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class UpdatesActivity extends UpdatesListActivity {

    private static final String TAG = "UpdatesActivity";
    private UpdaterService mUpdaterService;
    private BroadcastReceiver mBroadcastReceiver;

    private UpdatesListAdapter mUpdatesListAdapter;
    private NoticesListAdapter mNoticesListAdapter;

    private View mRefreshIconView;
    private RotateAnimation mRefreshAnimation;

    private static Application instance;

    private final ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            UpdaterService.LocalBinder binder = (UpdaterService.LocalBinder) service;
            mUpdaterService = binder.getService();
            mUpdatesListAdapter.setUpdaterController(mUpdaterService.getUpdaterController());
            mNoticesListAdapter.setUpdaterController(mUpdaterService.getUpdaterController());
            getLists();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mUpdatesListAdapter.setUpdaterController(null);
            mUpdaterService = null;
            mUpdatesListAdapter.notifyDataSetChanged();
        }
    };

    @Override
    public void onWindowFocusChanged(boolean hasFocused) {
        if (!(findViewById(R.id.menu_preferences) == null)) {
            findViewById(R.id.menu_preferences).setOnLongClickListener(v -> {
                Toast toast = Toast.makeText(getBaseContext(), getString(R.string.setting_advanced_warning), Toast.LENGTH_LONG);
                toast.show();
                Intent intent = new Intent(UpdatesActivity.this, AdvancedSettings.class);
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
            case R.id.menu_refresh: {
                downloadLists(true);
                return true;
            }
            case R.id.menu_preferences: {
                showPreferencesDialog();
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
        setContentView(R.layout.activity_updates);

        instance = getApplication();

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        RecyclerView noticeView = findViewById(R.id.notice_view);
        mUpdatesListAdapter = new UpdatesListAdapter(this);
        mNoticesListAdapter = new NoticesListAdapter(this);
        recyclerView.setAdapter(mUpdatesListAdapter);
        noticeView.setAdapter(mNoticesListAdapter);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        RecyclerView.LayoutManager layoutManagerN = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        noticeView.setLayoutManager(layoutManagerN);
        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        RecyclerView.ItemAnimator animatorN = noticeView.getItemAnimator();
        //修复 ScrollView 嵌套 RecyclerView 造成的滑动卡顿
        recyclerView.setHasFixedSize(true);
        recyclerView.setNestedScrollingEnabled(false);
        noticeView.setHasFixedSize(true);
        noticeView.setNestedScrollingEnabled(false);
        recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                UpdaterController controller = mUpdaterService == null ? null : mUpdaterService.getUpdaterController();
                if (controller != null && mUpdatesListAdapter != null) {
                    List<UpdateInfo> updates = controller.getUpdates();
                    boolean a = true;
                    for (int i = 0; i < recyclerView.getChildCount(); i++) {
                        UpdateInfo update = updates.get(i);
                        View view = recyclerView.getLayoutManager().findViewByPosition(i);
                        LinearLayout mBtnsLayout = view.findViewById(R.id.update_btns);
                        CardView mCard = view.findViewById(R.id.update_card);
                        ImageButton mAction = view.findViewById(R.id.update_action);
                        ImageButton mShowChangelog = view.findViewById(R.id.show_changelog);
                        OnlineImageView mImageView = view.findViewById(R.id.update_imageView);

                        if (!(mBtnsLayout == null || mCard == null || mAction == null || mShowChangelog == null || mImageView == null)) {
                            int btnsHeight = mBtnsLayout.getMeasuredHeight();
                            int cardHeight = mCard.getMeasuredHeight();
                            mBtnsLayout.setLayoutParams(Utils.getLayoutParams((int) (btnsHeight * 0.6), btnsHeight, mBtnsLayout));
                            mImageView.setImageURL(update.getImageUrl());
                            int w = (mImageView.getImageHeight() == 0 ? 0 : (cardHeight * mImageView.getImageWidth() / mImageView.getImageHeight()));
                            mImageView.setLayoutParams(Utils.getLayoutParams(w, cardHeight, mImageView));
                            mAction.setLayoutParams(Utils.getLayoutParams((int) (btnsHeight * 0.6), (int) (btnsHeight * 0.6), mAction));
                            mShowChangelog.setLayoutParams(Utils.getLayoutParams(btnsHeight - (int) (btnsHeight * 0.6), btnsHeight - (int) (btnsHeight * 0.6), mShowChangelog));
                            mUpdatesListAdapter.notifyItemChanged(i);

                            /* a = !a ? false : mBtnsLayout.getMeasuredWidth() == btnsHeight && mBtnsLayout.getMeasuredHeight() == btnsHeight &&
                                mImageView.getMeasuredWidth() == w && mImageView.getMeasuredHeight() == cardHeight &&
                                mAction.getMeasuredWidth() == (int) (btnsHeight * 0.6) && mAction.getMeasuredHeight() == (int) (btnsHeight * 0.6) &&
                                mShowChangelog.getMeasuredWidth() == (int) (btnsHeight * 0.3) && mShowChangelog.getMeasuredWidth() == (int) (btnsHeight * 0.3) &&
                                mBtnsSpace.getMeasuredWidth() == btnsHeight && mBtnsSpace.getMeasuredHeight() == (btnsHeight - mAction.getMeasuredHeight() - mShowChangelog.getMeasuredHeight());*/
                        } else a = false;
                    }
                    //在使用结束后不要忘记移除掉监听。
                    if(a) recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            }
        });
        noticeView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                UpdaterController controller = mUpdaterService == null ? null : mUpdaterService.getUpdaterController();
                if (controller != null && mNoticesListAdapter != null) {
                    List<NoticeInfo> notices = controller.getNotices();
                    for (int i = 0; i < noticeView.getChildCount(); i++) {
                        NoticeInfo notice = notices.get(i);
                        View view = noticeView.getLayoutManager().findViewByPosition(i);
                        CardView mCard = view.findViewById(R.id.notice_card);
                        OnlineImageView mImageView = view.findViewById(R.id.notice_imageView);

                        mImageView.setImageURL(notice.getImageUrl());
                        int cardHeight = mCard.getMeasuredHeight();
                        int w = (mImageView.getImageHeight() == 0 ? 0 : (cardHeight * mImageView.getImageWidth() / mImageView.getImageHeight()));
                        mImageView.setLayoutParams(Utils.getLayoutParams(w, cardHeight, mImageView));
                        mNoticesListAdapter.notifyItemChanged(i);
                    }
                    noticeView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            }
        });
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
        if (animatorN instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animatorN).setSupportsChangeAnimations(false);
        }

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (UpdaterController.ACTION_UPDATE_STATUS.equals(intent.getAction())) {
                    String downloadId = intent.getStringExtra(UpdaterController.EXTRA_DOWNLOAD_ID);
                    handleDownloadStatusChange(downloadId);
                    mUpdatesListAdapter.notifyDataSetChanged();
                } else if (UpdaterController.ACTION_DOWNLOAD_PROGRESS.equals(intent.getAction()) ||
                        UpdaterController.ACTION_INSTALL_PROGRESS.equals(intent.getAction())) {
                    String downloadId = intent.getStringExtra(UpdaterController.EXTRA_DOWNLOAD_ID);
                    mUpdatesListAdapter.notifyItemChanged(downloadId);
                } else if (UpdaterController.ACTION_UPDATE_REMOVED.equals(intent.getAction())) {
                    String downloadId = intent.getStringExtra(UpdaterController.EXTRA_DOWNLOAD_ID);
                    mUpdatesListAdapter.removeItem(downloadId);
                }
            }
        };

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView headerTitle = findViewById(R.id.header_title);
        headerTitle.setText(getString(R.string.header_title_text,
                BuildInfoUtils.getBuildVersion()).replace("{os_name}",getString(R.string.os_name)));

        updateLastCheckedString();

        TextView headerBuildVersion = findViewById(R.id.header_build_version);
        headerBuildVersion.setText(
                getString(R.string.header_android_version, Build.VERSION.RELEASE));

        TextView headerBuildDate = findViewById(R.id.header_build_date);
        headerBuildDate.setText(StringGenerator.getDateLocalizedUTC(this,
                DateFormat.LONG, BuildInfoUtils.getBuildDateTimestamp()));

        // Switch between header title and appbar title minimizing overlaps
        final CollapsingToolbarLayout collapsingToolbar =
                findViewById(R.id.collapsing_toolbar);
        final AppBarLayout appBar = findViewById(R.id.app_bar);
        appBar.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean mIsShown = false;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                int scrollRange = appBarLayout.getTotalScrollRange();
                if (!mIsShown && scrollRange + verticalOffset < 10) {
                    collapsingToolbar.setTitle(getString(R.string.display_name));
                    mIsShown = true;
                } else if (mIsShown && scrollRange + verticalOffset > 100) {
                    collapsingToolbar.setTitle(null);
                    mIsShown = false;
                }
            }
        });

        appBar.setOnFocusChangeListener((v, hasFocus) -> {

        });

        if (!Utils.hasTouchscreen(this)) {
            // This can't be collapsed without a touchscreen
            appBar.setExpanded(false);
        }

        mRefreshAnimation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        mRefreshAnimation.setInterpolator(new LinearInterpolator());
        mRefreshAnimation.setDuration(1000);
    }

    private void loadUpdatesList(File jsonFile, boolean manualRefresh)
            throws IOException, JSONException {
        Log.d(TAG, "Adding remote updates");
        UpdaterController controller = mUpdaterService.getUpdaterController();
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
        if (sortedUpdates.isEmpty()) {
            findViewById(R.id.no_new_updates_view).setVisibility(View.VISIBLE);
            findViewById(R.id.update_container).setVisibility(View.GONE);
        } else {
            findViewById(R.id.no_new_updates_view).setVisibility(View.GONE);
            findViewById(R.id.update_container).setVisibility(View.VISIBLE);
            // Sort:from small to big
            Log.d(TAG, "Sorting updates(list)(small > big)." );
            sortedUpdates.sort(Comparator.comparingLong(UpdateBaseInfo::getTimestamp));
            //遍历判断Target与Current版本Timestamp，并循环setChangeLog();合并log.
            long timestamp = SystemProperties.getLong(Constants.PROP_BUILD_DATE,0);
            Log.d(TAG, "Merging changelog for updates." );
            String changelog="";
            ListIterator<UpdateInfo> listIterator = sortedUpdates.listIterator();
            while (listIterator.hasNext()){
                Update u=(Update)listIterator.next();
                if (u.getTimestamp() > timestamp){
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
            mUpdatesListAdapter.setData(updateIds);
            mUpdatesListAdapter.notifyDataSetChanged();
        }
    }

    private void loadNoticesList(File jsonFile)
            throws IOException, JSONException {
        Log.d(TAG, "Adding remote notices");
        UpdaterController controller = mUpdaterService.getUpdaterController();
        boolean newNotices = false;

        List<NoticeInfo> notices = Utils.parseJsonNotice(jsonFile);
        for (NoticeInfo notice : notices) {
            newNotices |= controller.addNotice(notice);
        }

        List<String> noticeIds = new ArrayList<>();
        List<NoticeInfo> sortedNotices = controller.getNotices();
        if (sortedNotices.isEmpty()) {
            findViewById(R.id.notice_container).setVisibility(View.GONE);
        } else {
            findViewById(R.id.notice_container).setVisibility(View.VISIBLE);
            //sortedNotices.sort((u1, u2) -> String.compare(u2.getId(), u1.getId()));//It's already sorted in Utils.parseJsonUpdate(...)
            for (NoticeInfo notice : sortedNotices) {
                noticeIds.add(notice.getId());
            }
            mNoticesListAdapter.setData(noticeIds);
            mNoticesListAdapter.notifyDataSetChanged();
        }
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
            downloadNoticesList(false);
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
            if(isUpdate) {
                loadUpdatesList(jsonNew, manualRefresh);
            }else{
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
        String lastCheckString = getString(R.string.header_last_updates_check,
                StringGenerator.getDateLocalized(this, DateFormat.LONG, lastCheck),
                StringGenerator.getTimeLocalized(this, lastCheck));
        TextView headerLastCheck = findViewById(R.id.header_last_check);
        headerLastCheck.setText(lastCheckString);
    }

    private void handleDownloadStatusChange(String downloadId) {
        UpdateInfo update = mUpdaterService.getUpdaterController().getUpdate(downloadId);
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
        if (mRefreshIconView == null) {
            mRefreshIconView = findViewById(R.id.menu_refresh);
        }
        if (mRefreshIconView != null) {
            mRefreshAnimation.setRepeatCount(Animation.INFINITE);
            mRefreshIconView.startAnimation(mRefreshAnimation);
            mRefreshIconView.setEnabled(false);
        }
    }

    private void refreshAnimationStop() {
        if (mRefreshIconView != null) {
            mRefreshAnimation.setRepeatCount(0);
            mRefreshIconView.setEnabled(true);
        }
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
                    mUpdaterService.getUpdaterController().setPerformanceMode(enableABPerfMode);
                })
                .show();
    }
    public static Context getContextFromUA(){
        return instance;
    }
}
