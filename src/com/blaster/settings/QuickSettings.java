/*
 * Copyright (C) 2020 Project-Awaken
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
package com.blaster.settings;

import static android.os.UserHandle.USER_SYSTEM;

import android.app.AlertDialog;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;

import android.os.SystemProperties;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.SwitchPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.*;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;

import com.android.settings.custom.preference.SystemSettingListPreference;
import com.android.settings.custom.preference.SystemSettingSwitchPreference;
import com.android.internal.util.custom.Utils;

import com.android.internal.logging.nano.MetricsProto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SearchIndexable
public class QuickSettings extends SettingsPreferenceFragment {

    private IOverlayManager mOverlayManager;
    private PackageManager mPackageManager;    
    private static final String SLIDER_STYLE  = "slider_style";
    private static final String CLEAR_ALL_ICON_STYLE  = "clear_all_icon_style";

    private SystemSettingListPreference mClearAll;
    private SystemSettingListPreference mSlider;
    private Handler mHandler;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.quick_settings);
        PreferenceScreen prefSet = getPreferenceScreen();
        final ContentResolver resolver = getActivity().getContentResolver();
        Context mContext = getContext();
        mOverlayManager = IOverlayManager.Stub.asInterface(
                ServiceManager.getService(Context.OVERLAY_SERVICE));
        mClearAll = (SystemSettingListPreference) findPreference(CLEAR_ALL_ICON_STYLE);        
        mCustomSettingsObserver.observe();        

    }

    private CustomSettingsObserver mCustomSettingsObserver = new CustomSettingsObserver(mHandler);
    private class CustomSettingsObserver extends ContentObserver {

        CustomSettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            Context mContext = getContext();
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.CLEAR_ALL_ICON_STYLE  ),
                    false, this, UserHandle.USER_ALL);                    
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.System.getUriFor(Settings.System.CLEAR_ALL_ICON_STYLE))) {
                updateClearAll();                
            }
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mClearAll) {
            mCustomSettingsObserver.observe();
             Utils.showSystemUiRestartDialog(getContext());
            return true;            
        }
        return false;
    }


    private void updateClearAll() {
        ContentResolver resolver = getActivity().getContentResolver();
        boolean ClearAllDefault = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.CLEAR_ALL_ICON_STYLE , 0, UserHandle.USER_CURRENT) == 0;
        boolean ClearAllOOS = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.CLEAR_ALL_ICON_STYLE , 0, UserHandle.USER_CURRENT) == 1;

        if (ClearAllDefault) {
            setDefaultClearAll(mOverlayManager);
        } else if (ClearAllOOS) {
            enableClearAll(mOverlayManager, "com.android.theme.systemui_clearall_oos");
        }
    }

    public static void setDefaultClearAll(IOverlayManager overlayManager) {
        for (int i = 0; i < CLEAR_ALL_ICONS.length; i++) {
            String icons = CLEAR_ALL_ICONS[i];
            try {
                overlayManager.setEnabled(icons, false, USER_SYSTEM);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static void enableClearAll(IOverlayManager overlayManager, String overlayName) {
        try {
            for (int i = 0; i < CLEAR_ALL_ICONS.length; i++) {
                String icons = CLEAR_ALL_ICONS[i];
                try {
                    overlayManager.setEnabled(icons, false, USER_SYSTEM);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            overlayManager.setEnabled(overlayName, true, USER_SYSTEM);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void handleOverlays(String packagename, Boolean state, IOverlayManager mOverlayManager) {
        try {
            mOverlayManager.setEnabled(packagename,
                    state, USER_SYSTEM);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static final String[] CLEAR_ALL_ICONS = {
        "com.android.theme.systemui_clearall_oos"
    };

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.CUSTOM_SETTINGS;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.quick_settings;
                    return Arrays.asList(sir);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = super.getNonIndexableKeys(context);
                    return keys;
                }
            };
}
