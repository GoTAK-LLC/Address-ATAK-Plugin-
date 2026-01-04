
package com.gotak.address;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;

import com.atakmap.android.gui.ImportFileBrowserDialog;
import com.atakmap.android.gui.PanEditTextPreference;
import com.gotak.address.plugin.R;
import com.atakmap.android.preference.PluginPreferenceFragment;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import com.atakmap.android.util.PdfHelper;


import java.io.File;

public class AddressPreferenceFragment extends PluginPreferenceFragment {

    private static Context staticPluginContext;
    public static final String TAG = "HellWorldPreferenceFragment";

    public static final String CREDENTIALS_PREF_KEY = "Address.credentials";
    public static final String CREDENTIALS_DB_KEY = "AddressCredentials";

    public static final String USER_GUIDE_NAME = "userguide.pdf";

    public static final String USER_GUIDE_FULL_PATH = FileSystemUtils.getRoot() + File.separator + FileSystemUtils.TOOL_DATA_DIRECTORY + File.separatorChar
            + "Address" + File.separatorChar + "Address_userguide.pdf"; 
                


    /**
     * Only will be called after this has been instantiated with the 1-arg constructor.
     * Fragments must has a zero arg constructor.
     */
    public AddressPreferenceFragment() {
        super(staticPluginContext, R.xml.preferences);
    }

    @SuppressLint("ValidFragment")
    public AddressPreferenceFragment(final Context pluginContext) {
        super(pluginContext, R.xml.preferences);
        staticPluginContext = pluginContext;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            ((PanEditTextPreference) findPreference("key_for_Address"))
                    .checkValidInteger();
        } catch (Exception ignored) {
        }
        findPreference("test_file_browser")
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference pref) {

                        ImportFileBrowserDialog.show("Test File Browser",
                                null,
                                new String[] {
                                        ".txt"
                        },
                                new ImportFileBrowserDialog.DialogDismissed() {
                                    public void onFileSelected(
                                            final File file) {
                                        if (FileSystemUtils.isFile(file)) {
                                            Toast.makeText(getActivity(),
                                                    "file: " + file,
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    public void onDialogClosed() {
                                        //Do nothing
                                    }
                                }, AddressPreferenceFragment.this
                                        .getActivity());
                        return true;
                    }
                });

        // launch nested pref screen on click
        findPreference("nested_pref")
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference p) {
                        AddressPreferenceFragment.this
                                .showScreen(new AddressSubPreferenceFragment(
                                        staticPluginContext));
                        return true;
                    }
                });

        findPreference("userguide")
                .setOnPreferenceClickListener(
                        new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(
                                    Preference preference) {
                                PdfHelper.extractAndShow(pluginContext,getActivity(),USER_GUIDE_NAME,USER_GUIDE_FULL_PATH,true);
                                return true;
                            }
                        });

    }

    @Override
    public String getSubTitle() {
        return getSubTitle("Tool Preferences", "Hello World Preferences");
    }
}
