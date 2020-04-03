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
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemProperties;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;
import com.google.android.material.snackbar.Snackbar;
import org.exthmui.updater.controller.UpdaterController;
import org.exthmui.updater.controller.UpdaterService;
import org.exthmui.updater.download.DownloadClient;
import org.exthmui.updater.fragments.FoundUpdateFragment;
import org.exthmui.updater.fragments.LoadingFragment;
import org.exthmui.updater.fragments.NoNewUpdatesFragment;
import org.exthmui.updater.misc.Constants;
import org.exthmui.updater.misc.Utils;
import org.exthmui.updater.model.Update;
import org.exthmui.updater.model.UpdateBaseInfo;
import org.exthmui.updater.model.UpdateInfo;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";
    private UpdaterService mUpdaterService;

    public UpdaterController mUpdaterController;
    private NoNewUpdatesFragment noNewUpdatesFragment;
    private LoadingFragment mLoadingFragment;
    private FoundUpdateFragment mFoundUpdateFragment;
    private final ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            UpdaterService.LocalBinder binder = (UpdaterService.LocalBinder) service;
            mUpdaterService = binder.getService();
            getUpdatesList();
            check_updates(false);
            new Handler().postDelayed(() -> reloadFragment(), 1000);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mUpdaterService = null;
        }
    };

    public UpdaterController getUpdaterController() {
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
    }

    @Override
    public void onStop() {
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.noNewUpdatesFragment = new NoNewUpdatesFragment();
        this.mLoadingFragment = new LoadingFragment();
        this.mFoundUpdateFragment = new FoundUpdateFragment();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    public void loadFragment(Fragment fragment) {
        try {
            if (!fragment.isAdded()) {
                FragmentTransaction beginTransaction = getSupportFragmentManager().beginTransaction();
                beginTransaction.replace(R.id.fragment_container, fragment);
                beginTransaction.show(fragment);
                beginTransaction.commit();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void reloadFragment() {
        reloadFragment(getUpdaterController().getLatestUpdate());
    }

    private void reloadFragment(UpdateInfo update) {
        if (update == null) {
            // No updates detected
            reloadFragment(false);
            return;
        }
        reloadFragment(true);
    }


    private void reloadFragment(boolean hasUpdate) {
        if (hasUpdate) loadFragment(this.mFoundUpdateFragment);
        else loadFragment(this.noNewUpdatesFragment);
    }

    public void check_updates(boolean manual_refresh) {
        loadFragment(mLoadingFragment);
        downloadLists(manual_refresh);
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
        }
        reloadFragment();
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
            }
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            long millis = System.currentTimeMillis();
            preferences.edit().putLong(Constants.PREF_LAST_UPDATE_CHECK, millis).apply();
//            updateLastCheckedString();
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

    public void downloadLists(final boolean manualRefresh) {
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
                Log.e(TAG, "Could not download notices list");
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
                    processNewJson(jsonFile, jsonFileTmp, manualRefresh, false);
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
                        reloadFragment();
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
                    processNewJson(jsonFile, jsonFileTmp, manualRefresh, true);
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

    @Override
    public void showSnackbar(int stringId, int duration) {
        Snackbar.make(findViewById(R.id.main_container), stringId, duration).show();
    }

    private void showPreferencesDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.preferences_dialog, null);
        Spinner autoCheckInterval =
                view.findViewById(R.id.preferences_auto_updates_check_interval);
        SwitchCompat autoDelete = view.findViewById(R.id.preferences_auto_delete_updates);
        SwitchCompat dataWarning = view.findViewById(R.id.preferences_mobile_data_warning);
        SwitchCompat abPerfMode = view.findViewById(R.id.preferences_ab_perf_mode);

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

                    if (Utils.isABDevice()) {
                        boolean enableABPerfMode = abPerfMode.isChecked();
                        mUpdaterService.getUpdaterController().setPerformanceMode(enableABPerfMode);
                    }
                })
                .show();
    }
}
