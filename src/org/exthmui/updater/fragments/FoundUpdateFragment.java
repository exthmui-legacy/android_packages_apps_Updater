package org.exthmui.updater.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.icu.text.DateFormat;
import android.os.Build;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import org.exthmui.updater.MainActivity;
import org.exthmui.updater.R;
import org.exthmui.updater.controller.UpdaterController;
import org.exthmui.updater.misc.StringGenerator;
import org.exthmui.updater.misc.UpdateActionsUtils;
import org.exthmui.updater.misc.Utils;
import org.exthmui.updater.model.UpdateInfo;
import org.exthmui.updater.model.UpdateStatus;

import java.text.NumberFormat;

public class FoundUpdateFragment extends Fragment {

    private TextView mVersionView;              //显示exTHmUI 版本
    private TextView mLastCheckTextView;        //显示最后检查日期
    private TextView mAndroidVersionView;       //显示Android版本
    private TextView mHeaderBuildDateView;      //显示构建日期
    private TextView mBuildSizeView;            //显示ROM大小
    private TextView mMaintainerView;           //显示维护者

    private UpdateInfo mUpdateInfo;

    private Button mRefreshUpdateButton;
    private Button mChangeLogButton;
    private FloatingActionButton mAction;

    private ProgressBar mProgressBar;           //进度条
    private TextView mProgressText;             //进度条文本

    private BroadcastReceiver mBroadcastReceiver;
    private Context context;
    private MainActivity mActivity;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_busy, container, false);

        this.mUpdateInfo = this.mActivity.getUpdaterController().getLatestUpdate();
        this.mAction = view.findViewById(R.id.main_action);
        this.mProgressBar = view.findViewById(R.id.main_progress_bar);
        this.mProgressText = view.findViewById(R.id.main_progress_text);
        this.mVersionView = view.findViewById(R.id.main_title);
        this.mAndroidVersionView = view.findViewById(R.id.main_android_version);
        this.mAndroidVersionView.setText(getString(R.string.main_android_version, Build.VERSION.RELEASE));

        this.mHeaderBuildDateView = view.findViewById(R.id.main_build_date);
        this.mRefreshUpdateButton = view.findViewById(R.id.main_check_for_updates);
        this.mRefreshUpdateButton.setOnClickListener(v ->
                this.mActivity.check_updates(true));
        this.mBuildSizeView = view.findViewById(R.id.main_build_size);
        this.mMaintainerView = view.findViewById(R.id.main_maintainer);
        this.mChangeLogButton = view.findViewById(R.id.main_show_changelog);

        this.mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (UpdaterController.ACTION_UPDATE_STATUS.equals(intent.getAction())) {
                    String downloadId = intent.getStringExtra(UpdaterController.EXTRA_DOWNLOAD_ID);
                    handleDownloadStatusChange(downloadId);
                    refreshFragment(mActivity.getUpdaterController().getUpdate(downloadId));
                } else if (UpdaterController.ACTION_DOWNLOAD_PROGRESS.equals(intent.getAction()) ||
                        UpdaterController.ACTION_INSTALL_PROGRESS.equals(intent.getAction())) {
                    String downloadId = intent.getStringExtra(UpdaterController.EXTRA_DOWNLOAD_ID);
                    refreshFragment(mActivity.getUpdaterController().getUpdate(downloadId));
                } else if (UpdaterController.ACTION_UPDATE_REMOVED.equals(intent.getAction())) {
                    refreshFragment(mActivity.getUpdaterController().getLatestUpdate());
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UpdaterController.ACTION_UPDATE_STATUS);
        intentFilter.addAction(UpdaterController.ACTION_DOWNLOAD_PROGRESS);
        intentFilter.addAction(UpdaterController.ACTION_INSTALL_PROGRESS);
        intentFilter.addAction(UpdaterController.ACTION_UPDATE_REMOVED);
        LocalBroadcastManager.getInstance(mActivity).registerReceiver(mBroadcastReceiver, intentFilter);

        refreshFragment(mActivity.getUpdaterController().getLatestUpdate());

        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        this.mActivity = (MainActivity) context;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(mActivity).unregisterReceiver(mBroadcastReceiver);
    }

    private void refreshFragment(UpdateInfo update) {
        if (update == null) {
            // No updates detected
            mActivity.reloadFragment();
            return;
        }

        String buildDate = getString(R.string.build_version_date) + StringGenerator.getDateLocalizedUTC(this.context,
                DateFormat.LONG, update.getTimestamp());
        mHeaderBuildDateView.setText(buildDate);

        String buildVersion = update.getVersionName();
        mVersionView.setText(buildVersion);

        String fileSize = getString(R.string.build_size_text) + Formatter.formatFileSize(this.context, update.getFileSize());
        this.mBuildSizeView.setText(fileSize);

        this.mMaintainerView.setText(getString(R.string.build_maintainer_text) + update.getMaintainer());

        this.mChangeLogButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this.context)
                    .setTitle(R.string.changelogs)
                    .setMessage(update.getChangeLog())
                    .show();
        });

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

        if (activeLayout) {
            handleActiveStatus(mActivity.getUpdaterController(), update);
        } else {
            handleNotActiveStatus(mActivity.getUpdaterController(), update);
        }

    }


    public void handleActiveStatus(UpdaterController updaterController, UpdateInfo update) {

        final String downloadId = update.getDownloadId();
        if (updaterController.isDownloading(downloadId)) {
            String downloaded = StringGenerator.bytesToMegabytes(this.context,
                    update.getFile().length());
            String total = Formatter.formatFileSize(this.context, update.getFileSize());
            String percentage = NumberFormat.getPercentInstance().format(
                    update.getProgress() / 100.f);
            long eta = update.getEta();
            if (eta > 0) {
                CharSequence etaString = StringGenerator.formatETA(this.context, eta * 1000);
                mProgressText.setText(this.getString(
                        R.string.list_download_progress_eta_new, downloaded, total, etaString,
                        percentage));
            } else {
                mProgressText.setText(this.getString(
                        R.string.list_download_progress_new, downloaded, total, percentage));
            }
            UpdateActionsUtils.setButtonAction(this.mActivity, updaterController, this.mAction, UpdateActionsUtils.Action.PAUSE, downloadId, true);
            this.mProgressBar.setIndeterminate(update.getStatus() == UpdateStatus.STARTING);
            this.mProgressBar.setProgress(update.getProgress());
        } else if (updaterController.isInstallingUpdate(downloadId)) {
            UpdateActionsUtils.setButtonAction(this.mActivity, updaterController, this.mAction, UpdateActionsUtils.Action.CANCEL_INSTALLATION, downloadId, true);
            boolean notAB = updaterController.isNotInstallingABUpdate();
            this.mProgressText.setText(notAB ? R.string.dialog_prepare_zip_message :
                    update.getFinalizing() ?
                            R.string.finalizing_package :
                            R.string.preparing_ota_first_boot);
            this.mProgressBar.setIndeterminate(false);
            this.mProgressBar.setProgress(update.getInstallProgress());
        } else if (updaterController.isVerifyingUpdate(downloadId)) {
            UpdateActionsUtils.setButtonAction(this.mActivity, updaterController, this.mAction, UpdateActionsUtils.Action.INSTALL, downloadId, false);
            mProgressText.setText(R.string.list_verifying_update);
            mProgressBar.setIndeterminate(true);
        } else {
            UpdateActionsUtils.setButtonAction(this.mActivity, updaterController, this.mAction, UpdateActionsUtils.Action.RESUME, downloadId, Utils.isBusy(updaterController));
            String downloaded = StringGenerator.bytesToMegabytes(this.context,
                    update.getFile().length());
            String total = Formatter.formatFileSize(this.context, update.getFileSize());
            String percentage = NumberFormat.getPercentInstance().format(
                    update.getProgress() / 100.f);
            this.mProgressText.setText(this.getString(R.string.list_download_progress_new,
                    downloaded, total, percentage));
            this.mProgressBar.setIndeterminate(false);
            this.mProgressBar.setProgress(update.getProgress());
        }
    }

    public void handleNotActiveStatus(UpdaterController updaterController, UpdateInfo update) {
        final String downloadId = update.getDownloadId();
        if (updaterController.isWaitingForReboot(downloadId)) {
            UpdateActionsUtils.setButtonAction(this.mActivity, updaterController, mAction, UpdateActionsUtils.Action.REBOOT, downloadId, true);
        } else if (update.getPersistentStatus() == UpdateStatus.Persistent.VERIFIED) {
            UpdateActionsUtils.setButtonAction(this.mActivity, updaterController, mAction,
                    Utils.canInstall(update) ? UpdateActionsUtils.Action.INSTALL : UpdateActionsUtils.Action.DELETE,
                    downloadId, Utils.isBusy(updaterController));
        } else if (!Utils.canInstall(update)) {
            UpdateActionsUtils.setButtonAction(this.mActivity, updaterController, mAction, UpdateActionsUtils.Action.INFO, downloadId, Utils.isBusy(updaterController));
        } else {
            UpdateActionsUtils.setButtonAction(this.mActivity, updaterController, mAction, UpdateActionsUtils.Action.DOWNLOAD, downloadId, Utils.isBusy(updaterController));
        }
        String fileSize = getString(R.string.build_size_text) + Formatter.formatFileSize(this.context, update.getFileSize());
        mBuildSizeView.setText(fileSize);

        mProgressBar.setVisibility(View.INVISIBLE);
        mProgressText.setVisibility(View.INVISIBLE);
        mBuildSizeView.setVisibility(View.VISIBLE);
    }

    private void handleDownloadStatusChange(String downloadId) {
        UpdateInfo update = mActivity.getUpdaterController().getUpdate(downloadId);
        switch (update.getStatus()) {
            case PAUSED_ERROR:
                mActivity.showSnackbar(R.string.snack_download_failed, Snackbar.LENGTH_LONG);
                break;
            case VERIFICATION_FAILED:
                mActivity.showSnackbar(R.string.snack_download_verification_failed, Snackbar.LENGTH_LONG);
                break;
            case VERIFIED:
                mActivity.showSnackbar(R.string.snack_download_verified, Snackbar.LENGTH_LONG);
                break;
        }
    }

    public BroadcastReceiver getBroadcastReceiver() {
        return mBroadcastReceiver;
    }
}
