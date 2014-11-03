/*
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 */

package ru.yandex.antonk.photoframe;

import android.os.Bundle;
import android.os.Handler;
import android.app.Fragment;

public class IODialogRetainedFragment extends Fragment {

    protected Handler handler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
        handler = new Handler();
    }

    protected void sendException(final Exception ex) {
        handler.post(new Runnable() {
            @Override
            public void run () {
                IODialogFragment targetFragment = (IODialogFragment) getTargetFragment();
                if (targetFragment != null) {
                    targetFragment.sendException(ex);
                }
            }
        });
    }
}
