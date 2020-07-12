package org.exthmui.updater.misc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.PowerManager;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;

import org.exthmui.updater.BaseActivity;
import org.exthmui.updater.ExportUpdateService;
import org.exthmui.updater.R;
import org.exthmui.updater.controller.UpdaterController;
import org.exthmui.updater.controller.UpdaterService;
import org.exthmui.updater.model.UpdateInfo;
import org.exthmui.updater.model.UpdateStatus;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Objects;

public class UpdateActionsUtils {

    private final static String TAG = "UpdateActionsUtils";
    private static String mSelectedDownload;

    public static void setButtonAction(BaseActivity activity, UpdaterController controller, ImageButton button, Action action, final String downloadId,
                                       boolean enabled) {
        final View.OnClickListener clickListener;

        TypedValue tv = new TypedValue();
        activity.getTheme().resolveAttribute(android.R.attr.disabledAlpha, tv, true);
        final float alphaDisabledValue = tv.getFloat();
        switch (action) {
            case DOWNLOAD:
                button.setImageResource(R.drawable.ic_file_download);
                button.setEnabled(enabled);
                clickListener = enabled ? view -> startDownloadWithWarning(activity, controller, downloadId) : null;
                break;
            case PAUSE:
                button.setImageResource(R.drawable.ic_pause);
                button.setEnabled(enabled);
                clickListener = enabled ? view -> controller.pauseDownload(downloadId)
                        : null;
                break;
            case RESUME: {
                button.setImageResource(R.drawable.ic_play_arrow);
                button.setEnabled(enabled);
                UpdateInfo update = controller.getUpdate(downloadId);
                final boolean canInstall = Utils.canInstall(update) ||
                        update.getFile().length() == update.getFileSize();
                clickListener = enabled ? view -> {
                    if (canInstall) {
                        controller.resumeDownload(downloadId);
                    } else {
                        activity.showSnackbar(R.string.snack_update_not_installable,
                                Snackbar.LENGTH_LONG);
                    }
                } : null;
            }
            break;
            case INSTALL: {
                button.setImageResource(R.drawable.ic_system_update);
                button.setEnabled(enabled);
                UpdateInfo update = controller.getUpdate(downloadId);
                final boolean canInstall = Utils.canInstall(update);
                clickListener = enabled ? view -> {
                    if (canInstall) {
                        Objects.requireNonNull(getInstallDialog(activity, controller, downloadId)).show();
                    } else {
                        activity.showSnackbar(R.string.snack_update_not_installable,
                                Snackbar.LENGTH_LONG);
                    }
                } : null;
            }
            break;
            case INFO: {
                button.setImageResource(R.drawable.ic_info_white);
                button.setEnabled(enabled);
                clickListener = enabled ? view -> showInfoDialog(activity) : null;
            }
            break;
            case INFO_LIST: {
                button.setImageResource(R.drawable.ic_info);
                button.setEnabled(enabled);
                clickListener = enabled ? view -> showInfoDialog(activity) : null;
            }
            break;
            case DELETE: {
                button.setImageResource(R.drawable.ic_delete_forever);
                button.setEnabled(enabled);
                clickListener = enabled ? view -> getDeleteDialog(activity, controller, downloadId).show() : null;
            }
            break;
            case CANCEL_INSTALLATION: {
                button.setImageResource(R.drawable.ic_cancel);
                button.setEnabled(enabled);
                clickListener = enabled ? view -> getCancelInstallationDialog(activity, controller).show() : null;
            }
            break;
            case REBOOT: {
                button.setImageResource(R.drawable.ic_reboot);
                button.setEnabled(enabled);
                clickListener = enabled ? view -> {
                    PowerManager pm =
                            (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
                    assert pm != null;
                    pm.reboot(null);
                } : null;
            }
            break;
            default:
                clickListener = null;
        }
        button.setAlpha(enabled ? 1.f : alphaDisabledValue);

        // Disable action mode when a button is clicked
        button.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onClick(v);
            }
        });
    }

    public static void startActionMode(Activity activity, UpdaterController controller, final UpdateInfo update, final boolean canDelete, View anchor) {
        mSelectedDownload = update.getDownloadId();

        ContextThemeWrapper wrapper = new ContextThemeWrapper(activity,
                R.style.AppTheme_PopupMenuOverlapAnchor);
        PopupMenu popupMenu = new PopupMenu(wrapper, anchor, Gravity.NO_GRAVITY,
                R.attr.actionOverflowMenuStyle, 0);
        popupMenu.inflate(R.menu.menu_action_mode);

        MenuBuilder menu = (MenuBuilder) popupMenu.getMenu();
        menu.findItem(R.id.menu_delete_action).setVisible(canDelete);
        menu.findItem(R.id.menu_copy_url).setVisible(update.getAvailableOnline());
        menu.findItem(R.id.menu_export_update).setVisible(
                update.getPersistentStatus() == UpdateStatus.Persistent.VERIFIED);

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_delete_action:
                    UpdateActionsUtils.getDeleteDialog(activity, controller, update.getDownloadId()).show();
                    return true;
                case R.id.menu_copy_url:
                    Utils.addToClipboard(activity,
                            activity.getString(R.string.label_download_url),
                            update.getDownloadUrl(),
                            activity.getString(R.string.toast_download_url_copied));
                    return true;
                case R.id.menu_export_update:
                    // TODO: start exporting once the permission has been granted
                    boolean hasPermission = PermissionsUtils.checkAndRequestStoragePermission(
                            activity, 0);
                    if (hasPermission) {
                        exportUpdate(activity, update);
                    }
                    return true;
            }
            return false;
        });

        MenuPopupHelper helper = new MenuPopupHelper(wrapper, menu, anchor);
        helper.show();
    }

    public static void exportUpdate(Context context, UpdateInfo update) {
        File dest = new File(Utils.getExportPath(context), update.getName());
        if (dest.exists()) {
            dest = Utils.appendSequentialNumber(dest);
        }
        Intent intent = new Intent(context, ExportUpdateService.class);
        intent.setAction(ExportUpdateService.ACTION_START_EXPORTING);
        intent.putExtra(ExportUpdateService.EXTRA_SOURCE_FILE, update.getFile());
        intent.putExtra(ExportUpdateService.EXTRA_DEST_FILE, dest);
        context.startService(intent);
    }

    public static void startDownloadWithWarning(BaseActivity activity, UpdaterController controller, final String downloadId) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean warn = preferences.getBoolean(Constants.PREF_MOBILE_DATA_WARNING, true);
        if (!Utils.isOnWifiOrEthernet(activity) || !warn) {
            controller.startDownload(downloadId);
            return;
        }

        View checkboxView = LayoutInflater.from(activity).inflate(R.layout.checkbox_view, null);
        CheckBox checkbox = checkboxView.findViewById(R.id.checkbox);
        checkbox.setText(R.string.checkbox_mobile_data_warning);

        new AlertDialog.Builder(activity)
                .setTitle(R.string.update_on_mobile_data_title)
                .setMessage(R.string.update_on_mobile_data_message)
                .setView(checkboxView)
                .setPositiveButton(R.string.action_download,
                        (dialog, which) -> {
                            if (checkbox.isChecked()) {
                                preferences.edit()
                                        .putBoolean(Constants.PREF_MOBILE_DATA_WARNING, false)
                                        .apply();
                                activity.supportInvalidateOptionsMenu();
                            }
                            controller.startDownload(downloadId);
                        })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public static AlertDialog.Builder getDeleteDialog(Context context, UpdaterController controller, final String downloadId) {
        return new AlertDialog.Builder(context)
                .setTitle(R.string.confirm_delete_dialog_title)
                .setMessage(R.string.confirm_delete_dialog_message)
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> {
                            controller.pauseDownload(downloadId);
                            controller.deleteUpdate(downloadId);
                        })
                .setNegativeButton(android.R.string.cancel, null);
    }

    public static AlertDialog.Builder getInstallDialog(Context context, UpdaterController controller, final String downloadId) {
        if (!Utils.isBatteryLevelOk(context)) {
            Resources resources = context.getResources();
            String message = resources.getString(R.string.dialog_battery_low_message_pct,
                    resources.getInteger(R.integer.battery_ok_percentage_discharging),
                    resources.getInteger(R.integer.battery_ok_percentage_charging));
            return new AlertDialog.Builder(context)
                    .setTitle(R.string.dialog_battery_low_title)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, null);
        }
        UpdateInfo update = controller.getUpdate(downloadId);
        int resId;
        try {
            if (Utils.isABUpdate(update.getFile())) {
                resId = R.string.apply_update_dialog_message_ab;
            } else {
                resId = R.string.apply_update_dialog_message;
            }
        } catch (IOException e) {
            Log.e(TAG, "Could not determine the type of the update");
            return null;
        }

        String buildDate = StringGenerator.getDateLocalizedUTC(context,
                DateFormat.MEDIUM, update.getTimestamp());
        String buildInfoText = context.getString(R.string.list_build_version_date,
                BuildInfoUtils.getBuildVersion(), buildDate).replace("{os_name}", context.getResources().getString(R.string.os_name));
        return new AlertDialog.Builder(context)
                .setTitle(R.string.apply_update_dialog_title)
                .setMessage(context.getString(resId, buildInfoText,
                        context.getString(android.R.string.ok)))
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> Utils.triggerUpdate(context, downloadId))
                .setNegativeButton(android.R.string.cancel, null);
    }

    public static void showInfoDialog(Context context) {
        String messageString = String.format(StringGenerator.getCurrentLocale(context),
                context.getString(R.string.blocked_update_dialog_message),
                Utils.getUpgradeBlockedURL(context));
        SpannableString message = new SpannableString(messageString);
        Linkify.addLinks(message, Linkify.WEB_URLS);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.blocked_update_dialog_title)
                .setPositiveButton(android.R.string.ok, null)
                .setMessage(message)
                .show();
        TextView textView = dialog.findViewById(android.R.id.message);
        assert textView != null;
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public static AlertDialog.Builder getCancelInstallationDialog(Context context, UpdaterController controller) {
        return new AlertDialog.Builder(context)
                .setMessage(R.string.cancel_installation_dialog_message)
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> {
                            Intent intent = new Intent(context, UpdaterService.class);
                            intent.setAction(UpdaterService.ACTION_INSTALL_STOP);
                            context.startService(intent);
                        })
                .setNegativeButton(android.R.string.cancel, null);
    }

    public static String getmSelectedDownload() {
        return mSelectedDownload;
    }

    public enum Action {
        DOWNLOAD,
        PAUSE,
        RESUME,
        INSTALL,
        INFO,
        INFO_LIST,
        DELETE,
        CANCEL_INSTALLATION,
        REBOOT,
    }
}
