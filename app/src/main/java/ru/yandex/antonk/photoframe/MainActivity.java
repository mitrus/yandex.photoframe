package ru.yandex.antonk.photoframe;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorDescription;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.DialogFragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.view.Window;
import android.widget.Toast;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends Activity {

    private static String TAG = "ExampleActivity";

    public static String FRAGMENT_TAG = "list";

    // create your own client id/secret pair with callback url on oauth.yandex.ru
    public static final String CLIENT_ID = "82e97a6ea25547478f7824c572e7c625";
    public static final String CLIENT_SECRET = "33073e7ca854421583445c0e792b8f6a";

    public static final String AUTH_URL = "https://oauth.yandex.ru/authorize?response_type=token&client_id="+CLIENT_ID;

    public static String USERNAME = "photoframe.username";
    public static String TOKEN = "photoframe.token";

    public int tmp = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        tmp = 4;
        super.onCreate(savedInstanceState);
        if (getIntent() != null && getIntent().getData() != null) {
            processToken();
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String token = preferences.getString(TOKEN, null);
        if (token == null) {
            requestToken();
            return;
        }

        if (savedInstanceState == null) {
            openDisk();
        }
    }

    private void processToken() {
        Uri data = getIntent().getData();
        setIntent(null);
        Pattern pattern = Pattern.compile("access_token=(.*?)($|&)");
        Matcher matcher = pattern.matcher(data.toString());
        if (matcher.find()) {
            String token = matcher.group(1);
            if (!TextUtils.isEmpty(token)) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
                editor.putString(USERNAME, "");
                editor.putString(TOKEN, token);
                editor.commit();
                Log.d(TAG, "Token has been successfully saved");
            } else {
                Log.w(TAG, "Empty token");
            }
        } else {
            Log.w(TAG, "Token hasn't been found in return uri");
        }
    }


    private void requestToken() {
        new AuthDialogFragment().show(getFragmentManager(), "auth");
    }

    private void openDisk() {
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new ListPhotoframeFragment(), FRAGMENT_TAG)
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class AuthDialogFragment extends DialogFragment {

        public AuthDialogFragment () {
            super();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.example_auth_title)
                    .setMessage(R.string.example_auth_message)
                    .setPositiveButton(R.string.example_auth_positive_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick (DialogInterface dialog, int which) {
                            dialog.dismiss();
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(AUTH_URL)));
                        }
                    })
                    .setNegativeButton(R.string.example_auth_negative_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick (DialogInterface dialog, int which) {
                            dialog.dismiss();
                            getActivity().finish();
                        }
                    })
                    .create();
        }
    };

}
