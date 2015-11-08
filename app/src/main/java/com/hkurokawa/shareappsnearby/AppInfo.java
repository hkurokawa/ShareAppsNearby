package com.hkurokawa.shareappsnearby;

import android.content.pm.ApplicationInfo;

import java.io.Serializable;

class AppInfo implements Serializable {
    private final String packageName;
    private final String label;

    AppInfo(String packageName, String label) {
        this.packageName = packageName;
        this.label = label;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getLabel() {
        return label;
    }
}
