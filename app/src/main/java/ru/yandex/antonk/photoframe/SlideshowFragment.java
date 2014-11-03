package ru.yandex.antonk.photoframe;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.yandex.disk.client.Credentials;
import com.yandex.disk.client.ListItem;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by antonk on 01.11.14.
 */
public class SlideshowFragment extends IODialogFragment {

//    public static final String PHOTO_FILES = "photoframe.photos";

    public static String TAG_PREFIX = "photoframe.slideshow";

    public static String ITEMS = "image.items";

    public static String TOTAL_SHOWS = "total.shows";

    public static int MIN_DELAY = 3 * 1000;

    public static int currentImageViewId = 1;

    private ArrayList<ListItem> photoFiles = null;

    private int currentPhoto = 0;

    private int totalShows = 0;

    private Credentials credentials = null;

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);

    private Handler handler = null;

    private int currentIteration = 0;

    private long lastUpdate;

    private String nameOfCurrentImage = null;

    public SlideshowFragment() {
    }

    public static SlideshowFragment newInstance(ArrayList<ListItem> images, int totalShows) {
        SlideshowFragment fragment = new SlideshowFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ITEMS, images);
        args.putInt(TOTAL_SHOWS, totalShows);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
        currentPhoto = 0;
        handler = new Handler();
        Bundle args = getArguments();
        photoFiles = args.getParcelableArrayList(ITEMS);
        this.totalShows = args.getInt(TOTAL_SHOWS);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String username = preferences.getString(MainActivity.USERNAME, null);
        String token = preferences.getString(MainActivity.TOKEN, null);

        credentials = new Credentials(username, token);

        lastUpdate = System.currentTimeMillis();
        getFragmentManager().beginTransaction()
                .add(DownloadFileFragment.newInstance(credentials, photoFiles.get(currentPhoto), totalShows), "downloader")
                .commit();
        nameOfCurrentImage = photoFiles.get(currentPhoto).getName();
        currentPhoto = (currentPhoto + 1) % photoFiles.size();
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(false);
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View slideshowView = inflater.inflate(R.layout.slideshow, container, false);
        slideshowView.findViewById(R.id.imageView1);
        slideshowView.findViewById(R.id.imageView2);
        slideshowView.setBackgroundColor(Color.BLACK);
        return slideshowView;
    }



    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmap(String filePath, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath,options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath,options);
    }

    private void processPhoto(final File photo) {
        swapImages(photo);
        if (getFragmentManager() != null) {
            if (getActivity() != null && getActivity().getActionBar() != null) {
                getActivity().getActionBar().setSubtitle(nameOfCurrentImage);
            }
            getFragmentManager().beginTransaction()
                    .add(DownloadFileFragment.newInstance(credentials, photoFiles.get(currentPhoto), totalShows), "downloader")
                    .commit();
            nameOfCurrentImage = photoFiles.get(currentPhoto).getName();
            currentPhoto = (currentPhoto + 1) % photoFiles.size();
            lastUpdate = System.currentTimeMillis();
        }
    }

    public void addPhoto(final File photo) {
        long currentTime = System.currentTimeMillis();
        int diff = (int) (currentTime - lastUpdate);
        if (currentTime - lastUpdate >= MIN_DELAY) {
            processPhoto(photo);
        } else {
            scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            processPhoto(photo);
                        }
                    });
                }
            }, MIN_DELAY - diff, TimeUnit.MILLISECONDS);
        }
    }

    private void transformImage(final View fromImage, final View toImage) {
        Animation fromImageAnimation = new AlphaAnimation(1f, 0f);
        fromImageAnimation.setFillAfter(true);
        fromImageAnimation.setDuration(1000);
        fromImage.startAnimation(fromImageAnimation);

        Animation toImageAnimation = new AlphaAnimation(0f, 1f);
        toImageAnimation.setFillAfter(true);
        toImageAnimation.setDuration(1000);
        toImage.startAnimation(toImageAnimation);

    }

    private void scaleImage(ImageView view)
    {
        Drawable drawing = view.getDrawable();
        if (drawing == null) {
            return; // Checking for null & return, as suggested in comments
        }
        Bitmap bitmap = ((BitmapDrawable)drawing).getBitmap();

        // Get current dimensions AND the desired bounding box
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int bounding = dpToPx(1080 / 3);
        Log.i("Test", "original width = " + Integer.toString(width));
        Log.i("Test", "original height = " + Integer.toString(height));
        Log.i("Test", "bounding = " + Integer.toString(bounding));

        // Determine how much to scale: the dimension requiring less scaling is
        // closer to the its side. This way the image always stays inside your
        // bounding box AND either x/y axis touches it.
        float xScale = ((float) bounding) / width;
        float yScale = ((float) bounding) / height;
        float scale = (xScale <= yScale) ? xScale : yScale;
        Log.i("Test", "xScale = " + Float.toString(xScale));
        Log.i("Test", "yScale = " + Float.toString(yScale));
        Log.i("Test", "scale = " + Float.toString(scale));

        // Create a matrix for the scaling and add the scaling data
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        // Create a new bitmap and convert it to a format understood by the ImageView
        Bitmap scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        width = scaledBitmap.getWidth(); // re-use
        height = scaledBitmap.getHeight(); // re-use
        // Apply the scaled bitmap
        view.setImageBitmap(scaledBitmap);

        // Now change ImageView's dimensions to match the scaled image
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
        params.width = width;
        params.height = height;
        view.setLayoutParams(params);

        Log.i("Test", "done");
    }

    private int dpToPx(int dp)
    {
        float density = getActivity().getResources().getDisplayMetrics().density;
        return Math.round((float)dp * density);
    }

    private void swapImages(File photo) {
        if (photo.exists()) {
            if (getView() != null) {
                Bitmap myBitmap = decodeSampledBitmap(photo.getAbsolutePath(), getView().getWidth(),
                        getView().getHeight());
                ImageView myImage = null;
                ImageView notMyImage = null;
                if (currentImageViewId == 0) {
                    myImage = (ImageView) getView().findViewById(R.id.imageView1);
                    notMyImage = (ImageView) getView().findViewById(R.id.imageView2);
                } else {
                    myImage = (ImageView) getView().findViewById(R.id.imageView2);
                    notMyImage = (ImageView) getView().findViewById(R.id.imageView1);
                }
                myImage.setImageBitmap(myBitmap);
                scaleImage(myImage);
                transformImage(notMyImage, myImage);

                currentImageViewId ^= 1;
            }
        }
    }

}
