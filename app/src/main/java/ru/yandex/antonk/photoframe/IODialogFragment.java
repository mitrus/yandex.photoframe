/*
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 */

package ru.yandex.antonk.photoframe;

import android.app.ProgressDialog;
import android.app.Fragment;
import android.widget.Toast;

public class IODialogFragment extends Fragment {

    protected static final String CREDENTIALS = "photoframe.credentials";

    public void sendException(final Exception ex) {
        Toast.makeText(getActivity(), ex.getMessage(), Toast.LENGTH_LONG).show();
    }
}
