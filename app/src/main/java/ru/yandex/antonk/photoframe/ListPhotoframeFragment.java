package ru.yandex.antonk.photoframe;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.DialogFragment;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.yandex.disk.client.Credentials;
import com.yandex.disk.client.ListItem;


import com.yandex.disk.client.Credentials;
import com.yandex.disk.client.ListItem;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by antonk on 01.11.14.
 */
public class ListPhotoframeFragment extends ListFragment implements LoaderManager.LoaderCallbacks<List<ListItem>>{

    private static final String TAG = "ListExampleFragment";

    private static final String CURRENT_DIR_KEY = "photoframe.current.dir";

    private static final String ROOT = "/";

    private static int totalShows = 0;

    private Credentials credentials;
    private String currentDir;

    private ListPhotoframeAdapter adapter;
    private List<ListItem> data = new ArrayList<ListItem>();
    private int currentImageId = 0;
    private SlideshowFragment currentShow = null;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setDefaultEmptyText();

        setHasOptionsMenu(true);

        registerForContextMenu(getListView());

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String username = preferences.getString(MainActivity.USERNAME, null);
        String token = preferences.getString(MainActivity.TOKEN, null);

        credentials = new Credentials(username, token);

        Bundle args = getArguments();
        if (args != null) {
            currentDir = args.getString(CURRENT_DIR_KEY);
        }
        if (currentDir == null) {
            currentDir = ROOT;
        }
        getActivity().getActionBar().setSubtitle("");
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(!ROOT.equals(currentDir));

        adapter = new ListPhotoframeAdapter(getActivity());
        setListAdapter(adapter);
        setListShown(false);
        getLoaderManager().initLoader(0, null, this);
        totalShows++;
    }

    public void restartLoader() {
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.setHeaderTitle(getListItem(menuInfo).getDisplayName());
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.photoframe_context_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                getFragmentManager().popBackStack();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ListItem listItem = getListItem(item.getMenuInfo());
        switch (item.getItemId()) {

            default:
                return super.onContextItemSelected(item);
        }
    }

    private ListItem getListItem(ContextMenu.ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        return (ListItem) getListAdapter().getItem(info.position);
    }

    @Override
    public Loader<List<ListItem>> onCreateLoader(int i, Bundle bundle) {
        return new ListPhotoframeLoader(getActivity(), credentials, currentDir);
    }

    @Override
    public void onLoadFinished(final Loader<List<ListItem>> loader, List<ListItem> data) {
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
        if (data.isEmpty()) {
            Exception ex = ((ListPhotoframeLoader) loader).getException();
            if (ex != null) {
                setEmptyText(((ListPhotoframeLoader) loader).getException().getMessage());
            } else {
                setDefaultEmptyText();
            }
        } else {
            adapter.setData(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<ListItem>> loader) {
        adapter.setData(null);
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        ListItem item = (ListItem) getListAdapter().getItem(position);
        Log.d(TAG, "onListItemClick(): "+item);
        if (item.isCollection()) {
            changeDir(item.getFullPath());
        } else {
            data = new ArrayList<ListItem>();
            for (int i = position, count = 0; count < getListAdapter().getCount();
                i = (i + 1) % getListAdapter().getCount(), count++)
                if (!((ListItem) getListAdapter().getItem(i)).isCollection())
                    data.add((ListItem) getListAdapter().getItem(i));
            currentShow = SlideshowFragment.newInstance(new ArrayList<ListItem>(data), totalShows);
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, currentShow, SlideshowFragment.TAG_PREFIX + totalShows)
                    .addToBackStack(null)
                    .commit();
        }
    }

    protected void changeDir(String dir) {
        Bundle args = new Bundle();
        args.putString(CURRENT_DIR_KEY, dir);

        ListPhotoframeFragment fragment = new ListPhotoframeFragment();
        fragment.setArguments(args);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, fragment, MainActivity.FRAGMENT_TAG)
                .addToBackStack(null)
                .commit();
    }


    private void setDefaultEmptyText() {
        setEmptyText(getString(R.string.example_no_files));
    }

    public static class ListPhotoframeAdapter extends ArrayAdapter<ListItem> {
        private final LayoutInflater inflater;

        public ListPhotoframeAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_2);
            inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setData(List<ListItem> data) {
            clear();
            if (data != null) {
                addAll(data);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;

            if (convertView == null) {
                view = inflater.inflate(R.layout.listitem, parent, false);
            } else {
                view = convertView;
            }

            ListItem item = getItem(position);
            ((TextView)view.findViewById(R.id.list_text)).setText(item.getDisplayName());
            if (item.isCollection()) {
                ((ImageView) view.findViewById(R.id.list_icon)).setImageResource(R.drawable.folder_103);
            } else {
                ((ImageView) view.findViewById(R.id.list_icon)).setImageResource(R.drawable.icon_image_103);
            }
//            ((TextView)view.findViewById(android.R.id.text2)).setText(item.isCollection() ? "" : ""+item.getContentLength());

            return view;
        }
    }

}
