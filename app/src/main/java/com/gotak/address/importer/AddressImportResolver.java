package com.gotak.address.importer;

import android.widget.Toast;

import com.atakmap.android.importfiles.sort.ImportInternalSDResolver;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.Set;


/**
 * Will match/import a file with extension .hwi e.g. "sample.hwi"
 * With a body containing 'AddressSample' e.g. {"AddressSample" : "Sample File"}
 */
public class AddressImportResolver extends ImportInternalSDResolver {

    private static final String TAG = "AddressImportResolver";

    public static final String TOOL_NAME = FileSystemUtils.TOOL_DATA_DIRECTORY + File.separator +
            "Address";

    public static final String MATCHER = "AddressSample";
    public static final String FILE_EXT = ".hwi";
    private final MapView mapView;

    public AddressImportResolver(MapView mapView) {
        super(FILE_EXT, TOOL_NAME, true, false, "Hello Importer");
        this.mapView = mapView;
    }

    public boolean match(File file) {
        return super.match(file) && isAddress(file);
    }

    private boolean isAddress(File file) {
        if (!FileSystemUtils.isFile(file))
            return false;

        try {
            String contents = FileSystemUtils.copyStreamToString(file);
            return !FileSystemUtils.isEmpty(contents) && contents.contains(MATCHER);
        } catch (IOException e) {
            Log.w(TAG, "Unable to read file", e);
            return false;
        }
    }

    protected void onFileSorted(File src, File dst, Set<SortFlags> flags) {
        super.onFileSorted(src, dst, flags);
        Log.d(TAG, "Sorted, now initiating import");
        importAddress(dst);
    }

    private void importAddress(File file) {
        Log.d(TAG, "importAddress: " + file.getAbsolutePath());

        try {
            String contents = FileSystemUtils.copyStreamToString(file);
            if (FileSystemUtils.isEmpty(contents) || !contents.contains(MATCHER)) {
                Log.w(TAG, "importAddress file invalid");
                toast("Invalid or corrupt Hello World sample file");
                return;
            }

            Log.d(TAG, "importAddress: " + contents);
            toast("Hello World import: " + contents);

        } catch (IOException e) {
            Log.w(TAG, "importAddress Unable to read file", e);
        }
    }

    private void toast(String str) {
        mapView.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mapView.getContext(), str, Toast.LENGTH_LONG).show();
            }
        });
    }
}
