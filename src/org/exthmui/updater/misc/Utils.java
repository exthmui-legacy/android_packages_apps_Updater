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
package org.exthmui.updater.misc;

import android.app.AlarmManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.preference.PreferenceManager;
import org.exthmui.updater.R;
import org.exthmui.updater.UpdatesDbHelper;
import org.exthmui.updater.controller.UpdaterController;
import org.exthmui.updater.controller.UpdaterService;
import org.exthmui.updater.model.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Utils {

    private static final String TAG = "Utils";
    private List<UpdateInfo> updates;

    private Utils() {
    }

    public static File getDownloadPath(Context context) {
        String path = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context).getString("download_path", context.getString(R.string.download_path));
        return new File(path.equals("") ? path : context.getString(R.string.download_path));
    }

    public static File getExportPath(Context context) {
        File dir = new File(Environment.getExternalStorageDirectory(),
                context.getString(R.string.export_path));
        if (!dir.isDirectory()) {
            if (dir.exists() || !dir.mkdirs()) {
                throw new RuntimeException("Could not create directory");
            }
        }
        return dir;
    }

    public static File getCachedUpdateList(Context context) {
        return new File(context.getCacheDir(), "updates.json");
    }

    public static File getCachedNoticeList(Context context) {
        return new File(context.getCacheDir(), "notices.json");
    }

    // This should really return an UpdateBaseInfo object, but currently this only
    // used to initialize UpdateInfo objects
    private static UpdateInfo parseJsonUpdate(JSONObject object) throws JSONException {
        Update update = new Update();
        update.setVersionName(object.getString("name"));
        update.setDevice(object.getString("device"));
        update.setPType(object.getString("packagetype"));
        update.setRequirement(object.getLong("requirement"));
        update.setTimestamp(object.getLong("timestamp"));
        update.setChangeLog(new String(android.util.Base64.decode(object.getString("changelog").getBytes(), Base64.DEFAULT)));
        update.setName(object.getString("filename"));
        update.setDownloadId(object.getString("sha1"));
        update.setType(object.getString("romtype"));
        update.setFileSize(object.getLong("size"));
        update.setDownloadUrl(object.getString("url"));
        update.setImageUrl(object.getString("imageurl"));
        update.setVersion(object.getString("version"));
        return update;
    }

    // This should really return an NoticeInfo object, but currently this only
    // used to initialize NoticeInfo objects
    private static NoticeInfo parseJsonNotice(JSONObject object) throws JSONException {
        return new Notice(object.getString("title"), object.getString("text"), object.getString("id"), object.getString("imageurl"));
    }

    public static boolean isCompatible(UpdateBaseInfo update) {
        // TODO: Remove this before commit
//        if (update.getVersion().compareTo(SystemProperties.get(Constants.PROP_BUILD_VERSION)) < 0) {
//            Log.d(TAG, update.getName() + " is older than current Android version");
//            return false;
//        }
//        if (!SystemProperties.getBoolean(Constants.PROP_UPDATER_ALLOW_DOWNGRADING, false) &&
//                update.getTimestamp() <= SystemProperties.getLong(Constants.PROP_BUILD_DATE, 0)) {
//            Log.d(TAG, update.getName() + " is older than/equal to the current build");
//            return false;
//        }
//        if (!update.getType().equalsIgnoreCase(SystemProperties.get(Constants.PROP_RELEASE_TYPE))) {
//            Log.d(TAG, update.getName() + " has type " + update.getType());
//            return false;
//        }
//        if (!update.getDevice().equals(SystemProperties.get(Constants.PROP_DEVICE))) {
//            Log.d(TAG, update.getName() + " is made for " + update.getDevice() + " but this is a " + SystemProperties.get(Constants.PROP_DEVICE));
//            return false;
//        }
        return true;
    }

    public static boolean canInstall(UpdateBaseInfo update) {
        return !update.getDownloadUrl().equals("") && update.getDownloadUrl() != null &&
                !((update.getRequirement() >= SystemProperties.getInt(Constants.PROP_BUILD_DATE, 0) &&
                        update.getPType().equals("incremental"))) &&
                update.getVersion().equalsIgnoreCase(
                        SystemProperties.get(Constants.PROP_BUILD_VERSION));
    }

    public static List<UpdateInfo> parseJsonUpdate(File file, boolean compatibleOnly)
            throws IOException, JSONException {
        List<UpdateInfo> updates = new ArrayList<>();

        StringBuilder json = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            for (String line; (line = br.readLine()) != null; ) {
                json.append(line);
            }
        }

        JSONObject obj = new JSONObject(json.toString());
        JSONArray updatesList = obj.getJSONArray("response");
        for (int i = 0; i < updatesList.length(); i++) {
            if (updatesList.isNull(i)) {
                continue;
            }
            try {
                UpdateInfo update = parseJsonUpdate(updatesList.getJSONObject(i));
                if (!compatibleOnly || isCompatible(update)) {
                    updates.add(update);
                } else {
                    Log.d(TAG, "Ignoring incompatible update " + update.getName());
                    updates.remove(update);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Could not parse update object, index=" + i, e);
            }
        }
        return updates;
    }

    public static List<NoticeInfo> parseJsonNotice(File file)
            throws IOException, JSONException {
        List<NoticeInfo> notices = new ArrayList<>();

        StringBuilder json = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            for (String line; (line = br.readLine()) != null; ) {
                json.append(line);
            }
        }

        JSONObject obj = new JSONObject(json.toString());
        JSONArray noticesList = obj.getJSONArray("response");
        for (int i = 0; i < noticesList.length(); i++) {
            if (noticesList.isNull(i)) {
                continue;
            }
            try {
                NoticeInfo notice = parseJsonNotice(noticesList.getJSONObject(i));
                notices.add(notice);
            } catch (JSONException e) {
                Log.e(TAG, "Could not parse update object, index=" + i, e);
            }
        }
        /*
         * Do not put any same id notices in the json file!!!!!!!!
         */
        //对id进行排序 大>小
        Log.d(TAG, "Sorting updates(list).");
        notices.sort(Comparator.comparing(NoticeInfo::getId));
        return notices;
    }

    public static String getServerURL(Context context, boolean isUpdate) {
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String device = SystemProperties.get(Constants.PROP_NEXT_DEVICE,
                SystemProperties.get(Constants.PROP_DEVICE));
        String incrementalVersion = SystemProperties.get(Constants.PROP_BUILD_VERSION_INCREMENTAL);
        String type = SystemProperties.get(Constants.PROP_RELEASE_TYPE).toLowerCase(Locale.ROOT);

        String serverUrl;
        if (isUpdate) {
            serverUrl = mSharedPreferences.getString("updates_server_url", "");
            if (serverUrl.trim().isEmpty()) {
                serverUrl = SystemProperties.get(Constants.PROP_UPDATER_UPDATES_URI);
                if (serverUrl.trim().isEmpty()) {
                    serverUrl = context.getString(R.string.updates_server_url);
                }
            }
        } else {
            serverUrl = mSharedPreferences.getString("notices_server_url", "");
            if (serverUrl.trim().isEmpty()) {
                serverUrl = SystemProperties.get(Constants.PROP_UPDATER_NOTICES_URI);
                if (serverUrl.trim().isEmpty()) {
                    serverUrl = context.getString(R.string.notices_server_url);
                }
            }
        }

        //noinspection ResultOfMethodCallIgnored
        serverUrl.replace("{device}", device)
                .replace("{incr}", incrementalVersion)
                .replace("{type}", type);
        return serverUrl;
    }

    public static String getUpgradeBlockedURL(Context context) {
        String device = SystemProperties.get(Constants.PROP_NEXT_DEVICE,
                SystemProperties.get(Constants.PROP_DEVICE));
        return context.getString(R.string.blocked_update_info_url);
    }

    public static void triggerUpdate(Context context, String downloadId) {
        final Intent intent = new Intent(context, UpdaterService.class);
        intent.setAction(UpdaterService.ACTION_INSTALL_UPDATE);
        intent.putExtra(UpdaterService.EXTRA_DOWNLOAD_ID, downloadId);
        context.startService(intent);
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        NetworkCapabilities nc = cm.getNetworkCapabilities(network);
        if (nc == null) return false;
        return (nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) |
                nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) |
                nc.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) |
                nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    public static boolean isOnWifiOrEthernet(Context context) {
        if (isNetworkAvailable(context)) return false;
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        NetworkCapabilities nc = cm.getNetworkCapabilities(network);
        if (nc == null) return false;
        return nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) |
                nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
    }

    public static String getNetworkType(Context context) {
        if (context != null) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(
                    Context.CONNECTIVITY_SERVICE);
            if (cm == null) return "null";
            Network network = cm.getActiveNetwork();
            NetworkCapabilities nc = cm.getNetworkCapabilities(network);
            if (nc == null) return "null";
            if (nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return "cellular";
            } else if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return "wifi";
            } else if (nc.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) {
                return "bluetooth";
            } else if (nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return "ethernet";
            } else if (nc.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                return "vpn";
            } else if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE)) {
                return "wifi_aware";
            } else if (nc.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN)) {
                return "6lowpan";
            }
        }
        return "null";
    }

    /**
     * Compares two json formatted updates and notice list files
     *
     * @param oldJson old update list
     * @param newJson new update list
     * @return true if newJson has at least a compatible update not available in oldJson
     * @throws IOException   Throws IOException
     * @throws JSONException Throws JSONException
     */
    public static boolean checkForNewUpdates(File oldJson, File newJson, boolean isUpdate)
            throws IOException, JSONException {
        if (isUpdate) {
            List<UpdateInfo> oldList = parseJsonUpdate(oldJson, true);
            List<UpdateInfo> newList = parseJsonUpdate(newJson, true);
            Set<String> oldIds = new HashSet<>();
            for (UpdateInfo update : oldList) {
                oldIds.add(update.getDownloadId());
            }
            // In case of no new updates, the old list should
            // have all (if not more) the updates
            for (UpdateInfo update : newList) {
                if (!oldIds.contains(update.getDownloadId())) {
                    return true;
                }
            }
        } else {
            List<NoticeInfo> oldList = parseJsonNotice(oldJson);
            List<NoticeInfo> newList = parseJsonNotice(newJson);
            Set<String> oldIds = new HashSet<>();
            for (NoticeInfo notice : oldList) {
                oldIds.add(notice.getId());
            }
            // In case of no new updates, the old list should
            // have all (if not more) the updates
            for (NoticeInfo notice : newList) {
                if (!oldIds.contains(notice.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get the offset to the compressed data of a file inside the given zip
     *
     * @param zipFile   input zip file
     * @param entryPath full path of the entry
     * @return the offset of the compressed, or -1 if not found
     * @throws IllegalArgumentException if the given entry is not found
     */
    public static long getZipEntryOffset(ZipFile zipFile, String entryPath) {
        // Each entry has an header of (30 + n + m) bytes
        // 'n' is the length of the file name
        // 'm' is the length of the extra field
        final int FIXED_HEADER_SIZE = 30;
        Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
        long offset = 0;
        while (zipEntries.hasMoreElements()) {
            ZipEntry entry = zipEntries.nextElement();
            int n = entry.getName().length();
            int m = entry.getExtra() == null ? 0 : entry.getExtra().length;
            int headerSize = FIXED_HEADER_SIZE + n + m;
            offset += headerSize;
            if (entry.getName().equals(entryPath)) {
                return offset;
            }
            offset += entry.getCompressedSize();
        }
        Log.e(TAG, "Entry " + entryPath + " not found");
        throw new IllegalArgumentException("The given entry was not found");
    }

    public static void removeUncryptFiles(File downloadPath) {
        File[] uncryptFiles = downloadPath.listFiles(
                (dir, name) -> name.endsWith(Constants.UNCRYPT_FILE_EXT));
        if (uncryptFiles == null) {
            return;
        }
        for (File file : uncryptFiles) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    /**
     * Cleanup the download directory, which is assumed to be a privileged location
     * the user can't access and that might have stale files. This can happen if
     * the data of the application are wiped.
     *
     * @param context Context
     */
    public static void cleanupDownloadsDir(Context context) {
        File downloadPath = getDownloadPath(context);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        removeUncryptFiles(downloadPath);

        long buildTimestamp = SystemProperties.getLong(Constants.PROP_BUILD_DATE, 0);
        long prevTimestamp = preferences.getLong(Constants.PREF_INSTALL_OLD_TIMESTAMP, 0);
        String lastUpdatePath = preferences.getString(Constants.PREF_INSTALL_PACKAGE_PATH, null);
        boolean reinstalling = preferences.getBoolean(Constants.PREF_INSTALL_AGAIN, false);
        boolean deleteUpdates = preferences.getBoolean(Constants.PREF_AUTO_DELETE_UPDATES, false);
        if ((buildTimestamp != prevTimestamp || reinstalling) && deleteUpdates &&
                lastUpdatePath != null) {
            File lastUpdate = new File(lastUpdatePath);
            if (lastUpdate.exists()) {
                //noinspection ResultOfMethodCallIgnored
                lastUpdate.delete();
                // Remove the pref not to delete the file if re-downloaded
                preferences.edit().remove(Constants.PREF_INSTALL_PACKAGE_PATH).apply();
            }
        }

        final String DOWNLOADS_CLEANUP_DONE = "cleanup_done";
        if (preferences.getBoolean(DOWNLOADS_CLEANUP_DONE, false)) {
            return;
        }

        Log.d(TAG, "Cleaning " + downloadPath);
        if (!downloadPath.isDirectory()) {
            return;
        }
        File[] files = downloadPath.listFiles();
        if (files == null) {
            return;
        }

        // Ideally the database is empty when we get here
        UpdatesDbHelper dbHelper = new UpdatesDbHelper(context);
        List<String> knownPaths = new ArrayList<>();
        for (UpdateInfo update : dbHelper.getUpdates()) {
            knownPaths.add(update.getFile().getAbsolutePath());
        }
        for (File file : files) {
            if (!knownPaths.contains(file.getAbsolutePath())) {
                Log.d(TAG, "Deleting " + file.getAbsolutePath());
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }

        preferences.edit().putBoolean(DOWNLOADS_CLEANUP_DONE, true).apply();
    }

    public static File appendSequentialNumber(final File file) {
        String name;
        String extension;
        int extensionPosition = file.getName().lastIndexOf(".");
        if (extensionPosition > 0) {
            name = file.getName().substring(0, extensionPosition);
            extension = file.getName().substring(extensionPosition);
        } else {
            name = file.getName();
            extension = "";
        }
        final File parent = file.getParentFile();
        for (int i = 1; i < Integer.MAX_VALUE; i++) {
            File newFile = new File(parent, name + "-" + i + extension);
            if (!newFile.exists()) {
                return newFile;
            }
        }
        throw new IllegalStateException();
    }

    public static boolean isABDevice() {
        return !SystemProperties.getBoolean(Constants.PROP_AB_DEVICE, false);
    }

    public static boolean isABUpdate(ZipFile zipFile) {
        return zipFile.getEntry(Constants.AB_PAYLOAD_BIN_PATH) != null &&
                zipFile.getEntry(Constants.AB_PAYLOAD_PROPERTIES_PATH) != null;
    }

    public static boolean isABUpdate(File file) throws IOException {
        ZipFile zipFile = new ZipFile(file);
        boolean isAB = isABUpdate(zipFile);
        zipFile.close();
        return isAB;
    }

    public static boolean hasTouchscreen(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN);
    }

    public static void addToClipboard(Context context, String label, String text, String toastMessage) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(
                Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        if (clipboard == null) {
            Log.e(TAG, "Clipboard add failed, because clipboard == null");
            return;
        }
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show();
    }

    public static boolean isEncrypted(Context context, File file) {
        StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        assert sm != null;
        return sm.isEncrypted(file);
    }

    public static int getUpdateCheckSetting(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getInt(Constants.PREF_AUTO_UPDATES_CHECK_INTERVAL,
                Constants.AUTO_UPDATES_CHECK_INTERVAL_WEEKLY);
    }

    public static boolean isUpdateCheckEnabled(Context context) {
        return getUpdateCheckSetting(context) != Constants.AUTO_UPDATES_CHECK_INTERVAL_NEVER;
    }

    public static boolean isAutoDownloadEnabled(Context context) {
        return getUpdateCheckSetting(context) != Constants.AUTO_UPDATES_CHECK_INTERVAL_NEVER;
    }

    public static long getUpdateCheckInterval(Context context) {
        switch (Utils.getUpdateCheckSetting(context)) {
            case Constants.AUTO_UPDATES_CHECK_INTERVAL_DAILY:
                return AlarmManager.INTERVAL_DAY;
            case Constants.AUTO_UPDATES_CHECK_INTERVAL_WEEKLY:
            default:
                return AlarmManager.INTERVAL_DAY * 7;
            case Constants.AUTO_UPDATES_CHECK_INTERVAL_MONTHLY:
                return AlarmManager.INTERVAL_DAY * 30;
        }
    }

    public static ViewGroup.LayoutParams getLayoutParams(int tw, int th, View view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = tw;
        params.height = th;
        return params;
    }


    public static boolean isBatteryLevelOk(Context context) {
        Intent intent = context.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        assert intent != null;
        if (!intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false)) {
            return true;
        }
        int percent = Math.round(100.f * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100) /
                intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        int BATTERY_PLUGGED_ANY = BatteryManager.BATTERY_PLUGGED_AC | BatteryManager.BATTERY_PLUGGED_USB | BatteryManager.BATTERY_PLUGGED_WIRELESS;
        int required = (plugged & BATTERY_PLUGGED_ANY) != 0 ?
                context.getResources().getInteger(R.integer.battery_ok_percentage_charging) :
                context.getResources().getInteger(R.integer.battery_ok_percentage_discharging);
        return percent >= required;
    }


    public static boolean isBusy(UpdaterController controller) {
        return controller.hasNoActiveDownloads() && !controller.isVerifyingUpdate()
                && controller.isNotInstallingUpdate();
    }

}
