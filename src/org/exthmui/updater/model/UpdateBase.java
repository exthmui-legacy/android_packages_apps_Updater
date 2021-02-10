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
package org.exthmui.updater.model;

public class UpdateBase implements UpdateBaseInfo {

    private String mVersionName;
    private String mDevice;
    private String mPackageType;
    private long mRequirement;
    private String mChangeLog;
    private long mTimestamp;
    private String mFileName;
    private String mDownloadId;
    private String mROMType;
    private long mFileSize;
    private String mDownloadUrl;
    private String mMaintainer;

    public UpdateBase() {
    }

    public UpdateBase(UpdateBaseInfo updateBaseInfo) {
        mVersionName = updateBaseInfo.getVersionName();
        mDevice = updateBaseInfo.getDevice();
        mPackageType = updateBaseInfo.getPackageType();
        mRequirement = updateBaseInfo.getRequirement();
        mChangeLog = updateBaseInfo.getChangeLog();
        mTimestamp = updateBaseInfo.getTimestamp();
        mFileName = updateBaseInfo.getFileName();
        mDownloadId = updateBaseInfo.getDownloadId();
        mROMType = updateBaseInfo.getROMType();
        mFileSize = updateBaseInfo.getFileSize();
        mDownloadUrl = updateBaseInfo.getDownloadUrl();
        mMaintainer = updateBaseInfo.getMaintainer();
    }

    public String getVersionName() {
        return mVersionName;
    }

    public void setVersionName(String versionName) {
        this.mVersionName = versionName;
    }

    public String getDevice() {
        return mDevice;
    }

    public void setDevice(String device) {
        this.mDevice = device;
    }

    public String getPackageType() {
        return mPackageType;
    }

    public void setPackageType(String packageType) {
        this.mPackageType = packageType;
    }

    public long getRequirement() {
        return mRequirement;
    }

    public void setRequirement(long requirement) {
        this.mRequirement = requirement;
    }

    public String getChangeLog() {
        return mChangeLog;
    }

    public void setChangeLog(String changeLog) {
        this.mChangeLog = changeLog;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public void setTimestamp(long timestamp) {
        this.mTimestamp = timestamp;
    }

    public String getFileName() {
        return mFileName;
    }

    public void setFileName(String fileName) {
        this.mFileName = fileName;
    }

    public String getDownloadId() {
        return mDownloadId;
    }

    public void setDownloadId(String downloadId) {
        this.mDownloadId = downloadId;
    }

    public String getROMType() {
        return mROMType;
    }

    public void setROMType(String ROMType) {
        this.mROMType = ROMType;
    }

    public long getFileSize() {
        return mFileSize;
    }

    public void setFileSize(long fileSize) {
        this.mFileSize = fileSize;
    }

    public String getDownloadUrl() {
        return mDownloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.mDownloadUrl = downloadUrl;
    }

    public String getMaintainer() {
        return mMaintainer;
    }

    public void setMaintainer(String maintainer) {
        this.mMaintainer = maintainer;
    }
}
