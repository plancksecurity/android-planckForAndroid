package com.fsck.k9.pEp.filepicker;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.fsck.k9.pEp.filepicker.AbstractFilePickerActivity.EXTRA_ALLOW_MULTIPLE;
import static com.fsck.k9.pEp.filepicker.AbstractFilePickerActivity.EXTRA_PATHS;

/**
 * Some utility methods
 */
public class Utils {

    private static final String SEP = "/";

    /**
     * Name is validated to be non-null, non-empty and not containing any
     * slashes.
     *
     * @param name The name of the folder the user wishes to create.
     */
    public static boolean isValidFileName(@Nullable String name) {
        return !TextUtils.isEmpty(name)
                && !name.contains("/")
                && !name.equals(".")
                && !name.equals("..");
    }

    /**
     * Append the second pathString to the first. The result will not end with a /.
     * In case two absolute paths are given, e.g. /A/B/, and /C/D/, then the result
     * will be /A/B/C/D
     *
     * Multiple slashes will be shortened to a single slash, so /A///B is equivalent to /A/B
     */
    @NonNull
    public static String appendPath(@NonNull String first,
                                    @NonNull String second) {
        String result = first + SEP + second;

        while (result.contains("//")) {
            result = result.replaceAll("//", "/");
        }

        if (result.length() > 1 && result.endsWith(SEP)) {
            return result.substring(0, result.length() - 1);
        } else {
            return result;
        }
    }

    /**
     * Convert a uri generated by a fileprovider, like content://AUTHORITY/ROOT/actual/path
     * to a file pointing to file:///actual/path
     *
     * Note that it only works for paths generated with `ROOT` as the path element. This is done if
     * nnf_provider_paths.xml is used to define the file provider in the manifest.
     *
     * @param uri generated from a file provider
     * @return Corresponding {@link File} object
     */
    @NonNull
    public static File getFileForUri(@NonNull Uri uri) {
        String path = uri.getEncodedPath();
        final int splitIndex = path.indexOf('/', 1);
        final String tag = Uri.decode(path.substring(1, splitIndex));
        path = Uri.decode(path.substring(splitIndex + 1));

        if (!"root".equalsIgnoreCase(tag)) {
            throw new IllegalArgumentException(
                    String.format("Can't decode paths to '%s', only for 'root' paths.",
                            tag));
        }

        final File root = new File("/");

        File file = new File(root, path);
        try {
            file = file.getCanonicalFile();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to resolve canonical path for " + file);
        }

        if (!file.getPath().startsWith(root.getPath())) {
            throw new SecurityException("Resolved path jumped beyond configured root");
        }

        return file;
    }

    /**
     * Parses the returned files from a filepicker activity into a nice list
     *
     * @param data returned by the {@link AbstractFilePickerActivity}
     * @return a {@link List<Uri>} of files (uris) which the user selected in the picker.
     */
    @NonNull
    public static List<Uri> getSelectedFilesFromResult(@NonNull Intent data) {
        List<Uri> result = new ArrayList<>();
        if (data.getBooleanExtra(EXTRA_ALLOW_MULTIPLE, false)) {
            List<String> paths = data.getStringArrayListExtra(EXTRA_PATHS);
            if (paths != null) {
                for (String path : paths) {
                    result.add(Uri.parse(path));
                }
            }
        } else {
            result.add(data.getData());
        }
        return result;
    }
}
