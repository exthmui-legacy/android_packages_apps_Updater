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

import org.exthmui.updater.ui.OnlineImageView;

public interface UpdateBaseInfo {
    String getVersionName();
    String getDevice();
    String getPType();
    long getRequirement();
    String getIncr();
    String getChangeLog();
    long getTimestamp();
    String getName();
    String getDownloadId();
    String getType();
    long getFileSize();
    String getDownloadUrl();
    String getImageUrl();
    String getVersion();
}
