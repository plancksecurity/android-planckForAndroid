package com.fsck.k9.pEp.filepicker;

import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.fsck.k9.R;

import butterknife.Bind;
import butterknife.ButterKnife;

public class SelectPathFragment extends Fragment {

    public static final String SECONDARY_STORAGE = "SECONDARY_STORAGE";
    @Bind(R.id.path_list) ListView pathList;
    @Bind(R.id.toolbar) Toolbar toolbar;
    private ArrayAdapter<String> pathsAdapter;
    private OnPathClickListener onPathClickListener;

    public SelectPathFragment() {
        // Required empty public constructor
    }

    public static SelectPathFragment newInstance() {
        SelectPathFragment fragment = new SelectPathFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        toolbar.setTitle(R.string.settings_attachment_default_path);
        String externalStoragePath = Environment.getExternalStorageDirectory().getPath();
        String secondaryStorage = System.getenv(SECONDARY_STORAGE);
        pathsAdapter =
                new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1);
        pathsAdapter.add(externalStoragePath);
        pathsAdapter.add(secondaryStorage);
        pathList.setAdapter(pathsAdapter);
        pathList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedPath = pathsAdapter.getItem(position);
                onPathClickListener.onClick(selectedPath);
            }
        });
    }


    public void setPathClickListener(OnPathClickListener onPathClickListener) {
        this.onPathClickListener = onPathClickListener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_select_path, container, false);
        ButterKnife.bind(this, view);
        return view;
    }
}
