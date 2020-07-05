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

import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemProperties;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import androidx.cardview.widget.CardView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import com.google.android.material.snackbar.Snackbar;
import org.exthmui.updater.controller.UpdaterController;
import org.exthmui.updater.controller.UpdaterService;
import org.exthmui.updater.download.DownloadClient;
import org.exthmui.updater.misc.Constants;
import org.exthmui.updater.misc.Utils;
import org.exthmui.updater.model.Update;
import org.exthmui.updater.model.UpdateBaseInfo;
import org.exthmui.updater.model.UpdateInfo;
import org.exthmui.updater.ui.OnlineImageView;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class UpdatesActivity extends BaseActivity {

    private static final String TAG = "UpdatesActivity";
    private UpdaterService mUpdaterService;
    private BroadcastReceiver mBroadcastReceiver;

    private UpdatesListAdapter mUpdatesListAdapter;

    private final ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            UpdaterService.LocalBinder binder = (UpdaterService.LocalBinder) service;
            mUpdaterService = binder.getService();
            mUpdatesListAdapter.setUpdaterController(mUpdaterService.getUpdaterController());
            getUpdatesList();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mUpdatesListAdapter.setUpdaterController(null);
            mUpdaterService = null;
            mUpdatesListAdapter.notifyDataSetChanged();
        }
    };

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
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_updates);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        mUpdatesListAdapter = new UpdatesListAdapter(this);
        recyclerView.setAdapter(mUpdatesListAdapter);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        mUpdatesListAdapter.setUpdaterController(UpdaterController.getInstanceReceiver(this));
        recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                UpdaterController controller = mUpdaterService == null ? null : mUpdaterService.getUpdaterController();
                if (controller != null && mUpdatesListAdapter != null) {
                    List<UpdateInfo> updates = controller.getUpdates();
                    boolean a = true;
                    for (int i = 0; i < recyclerView.getChildCount(); i++) {
                        UpdateInfo update = updates.get(i);
                        View view = Objects.requireNonNull(recyclerView.getLayoutManager()).findViewByPosition(i);
                        assert view != null;
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
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
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
    }

    private void loadUpdatesList(File jsonFile)
            throws IOException, JSONException {
        Log.d(TAG, "Adding remote updates");
        UpdaterController controller = mUpdaterService.getUpdaterController();

        List<UpdateInfo> updates = Utils.parseJsonUpdate(jsonFile, true);
        List<String> updatesOnline = new ArrayList<>();
        for (UpdateInfo update : updates) {
            controller.addUpdate(update);
            updatesOnline.add(update.getDownloadId());
        }
        controller.setUpdatesAvailableOnline(updatesOnline, true);

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

    private void getUpdatesList() {
        File cachedUpdateList = Utils.getCachedUpdateList(this);
        if (cachedUpdateList.exists()) {
            try {
                loadUpdatesList(cachedUpdateList);
                Log.d(TAG, "Cached update list parsed");
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error while parsing json list", e);
            }
        } else {
            downloadUpdatesList();
        }
    }

    private void processNewJson(File json, File jsonNew) {
        try {
            loadUpdatesList(jsonNew);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            long millis = System.currentTimeMillis();
            preferences.edit().putLong(Constants.PREF_LAST_UPDATE_CHECK, millis).apply();
            if (json.exists() && Utils.isUpdateCheckEnabled(this) &&
                    Utils.checkForNewUpdates(json, jsonNew, true)) {
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

    private void downloadUpdatesList() {
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
            showSnackbar(R.string.snack_updates_check_failed, Snackbar.LENGTH_LONG);
            return;
        }

        downloadClient.start();
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
}
