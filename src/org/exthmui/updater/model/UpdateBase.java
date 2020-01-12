/*
 * Copyright (C) 2017 The LineageOS Project
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
package org.exthmui.updater.model;

import android.media.Image;

import org.exthmui.updater.ui.OnlineImageView;

public class UpdateBase implements UpdateBaseInfo {

    private String mVersionName;
    private String mName;
    private String mDevice;
    private long mRequirement;
    private String mIncr;
    private String mPType;//Packagetype:full/...
    private String mChangeLog;
    private String mImageUrl;
    private String mDownloadUrl;
    private String mDownloadId;
    private long mTimestamp;
    private String mType;
    private String mVersion;
    private long mFileSize;

    public UpdateBase() {
    }

    public UpdateBase(UpdateBaseInfo update) {
        mVersionName = update.getVersionName();
        mName = update.getName();
        mDevice = update.getDevice();
        mIncr = update.getIncr();
        mPType = update.getPType();
        mChangeLog = update.getChangeLog();
        mImageUrl = update.getImageUrl();
        mDownloadUrl = update.getDownloadUrl();
        mDownloadId = update.getDownloadId();
        mTimestamp = update.getTimestamp();
        mType = update.getType();
        mVersion = update.getVersion();
        mFileSize = update.getFileSize();
    }

    @Override
    public String getVersionName() {
        return mVersionName;
    }

    public void setVersionName(String vName) {
        mVersionName = vName;
    }

    @Override
    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    @Override
    public long getRequirement() {
        return mRequirement;
    }

    public void setRequirement(long requirement) { mRequirement = requirement; }

    @Override
    public String getDevice() {
        return mDevice;
    }

    public void setDevice(String device) { mDevice = device; }

    @Override
    public String getPType() {
        return mPType;
    }

    public void setPType(String packageType) { mPType = packageType; }

    @Override
    public String getIncr() {
        return mIncr;
    }

    public void setIncr(String incr) { mIncr = incr; }

    @Override
    public String getChangeLog() {
        return mChangeLog;
    }

    public void setChangeLog(String changeLog) { mChangeLog = changeLog; }

    @Override
    public String getImageUrl() {
        return mImageUrl;
    }

    public void setImageUrl(String imageUrl) { mImageUrl = imageUrl; }

    @Override
    public String getDownloadId() {
        return mDownloadId;
    }

    public void setDownloadId(String downloadId) {
        mDownloadId = downloadId;
    }

    @Override
    public long getTimestamp() {
        return mTimestamp;
    }

    public void setTimestamp(long timestamp) {
        mTimestamp = timestamp;
    }

    @Override
    public String getType() {
        return mType;
    }

    public void setType(String type) {
        mType = type;
    }

    @Override
    public String getVersion() {
        return mVersion;
    }

    public void setVersion(String version) {
        mVersion = version;
    }

    @Override
    public String getDownloadUrl() {
        return mDownloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        mDownloadUrl = downloadUrl;
    }

    @Override
    public long getFileSize() {
        return mFileSize;
    }

    public void setFileSize(long fileSize) {
        mFileSize = fileSize;
    }
}
