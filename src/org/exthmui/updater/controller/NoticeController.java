package org.exthmui.updater.controller;

import android.content.Context;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import org.exthmui.updater.model.Notice;
import org.exthmui.updater.model.NoticeInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NoticeController {

    private final String TAG="NoticeController";

    private static NoticeController sNoticeController;
    private final LocalBroadcastManager mBroadcastManager;
    private final Context mContext;
    private String ACTION_NOTICE_REMOVED = "action_update_removed";
    private String EXTRA_NOTICE_ID = "extra_notice_id";
    private Map<String, Notice> mNotices = new HashMap<>();

    private NoticeController(Context context) {
        mBroadcastManager = LocalBroadcastManager.getInstance(context);
        mContext = context.getApplicationContext();
    }

    protected static synchronized NoticeController getInstance(Context context) {
        if (sNoticeController == null) {
            sNoticeController = new NoticeController(context);
        }
        return sNoticeController;
    }

    public boolean addNotice(final NoticeInfo noticeInfo) {
        Log.d(TAG, "Adding notice: " + noticeInfo.getId());
        if (mNotices.containsKey(noticeInfo.getId())) {
            Log.d(TAG, "Notice (" + noticeInfo.getId() + ") already added");
            Notice noticeAdded = mNotices.get(noticeInfo.getId());
            noticeAdded.setImageUrl(noticeInfo.getImageUrl());
            return false;
        }
        Notice notice = new Notice(noticeInfo);
        mNotices.put(notice.getId(), new Notice(notice));
        return true;
    }

    public List<NoticeInfo> getNotices() {
        List<NoticeInfo> notices = new ArrayList<>();
        for (Notice notice :mNotices.values()) {
            notices.add(notice);
        }
        return notices;
    }

    void notifyUpdateChange(String id) {
        Intent intent = new Intent();
        intent.setAction(ACTION_NOTICE_REMOVED);
        intent.putExtra(EXTRA_NOTICE_ID, id);
        mBroadcastManager.sendBroadcast(intent);
    }

    public NoticeInfo getNotice(String id) {
        Notice notice = mNotices.get(id);
        return notice != null ? notice : null;
    }
}