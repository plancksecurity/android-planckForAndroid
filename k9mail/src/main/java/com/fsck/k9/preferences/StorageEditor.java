package com.fsck.k9.preferences;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.os.SystemClock;

import androidx.annotation.NonNull;

import timber.log.Timber;


public class StorageEditor {
    private Storage storage;
    private PassphraseStorage passphraseStorage;

    private Map<String, String> changes = new HashMap<>();
    private List<String> removals = new ArrayList<>();

    Map<String, String> snapshot = new HashMap<>();


    StorageEditor(Storage storage,PassphraseStorage passphraseStorage) {
        this.storage = storage;
        this.passphraseStorage = passphraseStorage;
        snapshot.putAll(storage.getAll());
    }

    public void copy(android.content.SharedPreferences input) {
        Map < String, ? > oldVals = input.getAll();
        for (Entry < String, ? > entry : oldVals.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key != null && value != null) {
                Timber.d("Copying key '%s', value '%s'", key, value);
                changes.put(key, "" + value);
            } else {
                Timber.d("Skipping copying key '%s', value '%s'", key, value);
            }
        }
    }

    public boolean commit() {
        try {
            commitChanges();
            return true;
        } catch (Exception e) {
            Timber.e(e, "Failed to save preferences");
            return false;
        }
    }

    private void commitChanges() {
        long startTime = SystemClock.elapsedRealtime();
        Timber.i("Committing preference changes");
        Runnable committer = () -> {
            for (String removeKey : removals) {
                storage.remove(removeKey);
            }
            Map<String, String> insertables = new HashMap<>();
            for (Entry<String, String> entry : changes.entrySet()) {
                String key = entry.getKey();
                String newValue = entry.getValue();
                String oldValue = snapshot.get(key);
                if (removals.contains(key) || !newValue.equals(oldValue)) {
                    insertables.put(key, newValue);
                }
            }
            storage.put(insertables);
        };
        storage.doInTransaction(committer);
        long endTime = SystemClock.elapsedRealtime();
        Timber.i("Preferences commit took %d ms", endTime - startTime);

    }

    public StorageEditor putBoolean(String key,
            boolean value) {
        changes.put(key, "" + value);
        return this;
    }

    public StorageEditor putInt(String key, int value) {
        changes.put(key, "" + value);
        return this;
    }

    public StorageEditor putLong(String key, long value) {
        changes.put(key, "" + value);
        return this;
    }

    public StorageEditor putString(String key, String value) {
        if (value == null) {
            remove(key);
        } else {
            changes.put(key, value);
        }
        return this;
    }

    public void putPassphrase(String passphrase){
        passphraseStorage.putPassphrase(passphrase);
    }

    public StorageEditor remove(String key) {
        removals.add(key);
        return this;
    }
}
