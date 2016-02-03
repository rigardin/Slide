package me.ccrama.redditslide.util;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import me.ccrama.redditslide.Activities.FullscreenImage;
import me.ccrama.redditslide.Activities.GifView;
import me.ccrama.redditslide.Activities.Website;
import me.ccrama.redditslide.Adapters.AlbumView;
import me.ccrama.redditslide.R;
import me.ccrama.redditslide.Views.OpenImgurLink;
import me.ccrama.redditslide.Views.PreCachingLayoutManager;

/**
 * Created by carlo_000 on 2/1/2016.
 */
public class AlbumUtils {

    boolean slider;

    private static String getHash(String s) {
        String next = s.substring(s.lastIndexOf("/"), s.length());
        if (next.length() < 5) {
            return getHash(s.replace(next, ""));
        } else {
            return next;
        }

    }

    private static String cutEnds(String s) {
        if (s.endsWith("/")) {
            return s.substring(0, s.length() - 1);
        } else {
            return s;
        }
    }


    public static class LoadAlbumFromUrl extends AsyncTask<String, Void, Void> {

        public boolean gallery;
        public String hash;
        public boolean finishIfNone;
        public ActionBar supportActionBar;
        public boolean openExternalNotAlbum;
        public Activity baseActivity;
        public RecyclerView recyclerView;

        /**
         * Loads an imgur album/gallery and opens it in the appropriate activity
         *
         * Types that can be opened: Album, Gallery(can be album or image)
         * @param url An imgur url
         * @param baseActivity Activity
         * @param finishIfNone Closes the given activity if the album couldn't be loaded
         * @param openExternalNotAlbum Opens the url in an external browser if the album couldn't be loaded
         * @param bar For setting the toolbar title
         * @param recyclerView
         */

        public LoadAlbumFromUrl(@NotNull String url, @NotNull Activity baseActivity, boolean finishIfNone, boolean openExternalNotAlbum, @Nullable ActionBar bar, @NotNull RecyclerView recyclerView) {

            this.finishIfNone = finishIfNone;
            this.recyclerView = recyclerView;
            this.supportActionBar = bar;
            this.openExternalNotAlbum = openExternalNotAlbum;
            this.baseActivity = baseActivity;

            String rawDat = cutEnds(url);
            if (rawDat.contains("gallery")) {
                gallery = true;
            }
            cutEnds(rawDat);
            String rawdat2 = rawDat;
            if (rawdat2.substring(rawDat.lastIndexOf("/"), rawdat2.length()).length() < 4) {
                rawDat = rawDat.replace(rawDat.substring(rawDat.lastIndexOf("/"), rawdat2.length()), "");
            }
            if (rawDat.isEmpty()) {
                if (finishIfNone)
                    baseActivity.finish();
            } else {

                hash = getHash(rawDat);

            }
        }

        @Override
        protected Void doInBackground(final String... sub) {
            if (gallery) {
                Ion.with(baseActivity)
                        //Galleries can be both, albums and images
                        .load("https://api.imgur.com/3/gallery/" + hash + ".json")
                        .addHeader("Authorization", OpenImgurLink.IMGUR_CLIENT_ID)
                        .asJsonObject()
                        .setCallback(new FutureCallback<JsonObject>() {
                            @Override
                            public void onCompleted(Exception e, JsonObject result) {
                                if (result != null && result.has("data")) {
                                    Log.v(LogUtil.getTag(), result.toString());

                                    JsonObject resultData = result.getAsJsonObject("data");
                                    if (resultData != null && !resultData.isJsonNull()) {

                                        if (resultData.get("is_album").getAsBoolean()) {
                                            //is album
                                            setupAlbumView(resultData);
                                        } else if (resultData.get("animated").getAsBoolean()) {
                                            //is gif
                                            Intent i = new Intent(baseActivity, GifView.class);
                                            i.putExtra(GifView.EXTRA_URL, resultData.get("link").getAsString());
                                            baseActivity.startActivity(i);
                                            if (finishIfNone) baseActivity.finish();
                                        } else {
                                            //is image
                                            Intent i = new Intent(baseActivity, FullscreenImage.class);
                                            i.putExtra(FullscreenImage.EXTRA_URL, resultData.get("link").getAsString());
                                            baseActivity.startActivity(i);
                                            if (finishIfNone) baseActivity.finish();
                                        }

                                    } else {
                                        openFailedRequestInBrowser();
                                        Log.w(LogUtil.getTag(), "Cannot open gallery, result has no data");
                                    }
                                } else {
                                    //Catch failed api call
                                    openFailedRequestInBrowser();
                                    Log.w(LogUtil.getTag(), "Cannot open gallery, no result");
                                }

                            }

                        });
            } else {
                Log.v(LogUtil.getTag(), "http://api.imgur.com/3/album" + hash + ".json");
                Ion.with(baseActivity)
                        .load("https://api.imgur.com/3/album" + hash + ".json")
                        .addHeader("Authorization", OpenImgurLink.IMGUR_CLIENT_ID)
                        .asJsonObject()
                        .setCallback(new FutureCallback<JsonObject>() {
                            @Override
                            public void onCompleted(Exception e, JsonObject result) {


                                if (result != null && result.has("data")) {
                                    Log.v(LogUtil.getTag(), result.toString());

                                    final JsonObject resultData = result.getAsJsonObject("data");

                                    if (resultData != null && !resultData.isJsonNull() && resultData.has("images")) {

                                        setupAlbumView(resultData);

                                    } else {
                                        openFailedRequestInBrowser();
                                        Log.w(LogUtil.getTag(), "Cannot open album, result has no data");
                                    }

                                } else {
                                    //Catch failed api call
                                    openFailedRequestInBrowser();
                                    Log.w(LogUtil.getTag(), "Cannot open album, no result");
                                }
                            }
                        });
            }
            return null;
        }

        private void setupAlbumView(JsonObject resultData) {
            final ArrayList<JsonElement> jsons = new ArrayList<>();
            JsonArray images = resultData.get("images").getAsJsonArray();
            if (images != null && !images.isJsonNull() && images.size() > 0) {

                for (JsonElement o : images) {
                    jsons.add(o);
                }

                if (supportActionBar != null) {
                    if (resultData.has("title") && !resultData.get("title").isJsonNull()) {
                        supportActionBar.setTitle(resultData.get("title").getAsString());
                    } else {
                        supportActionBar.setTitle(baseActivity.getString(R.string.album_title_count, jsons.size()));
                    }
                }

                if (recyclerView != null) {
                    final PreCachingLayoutManager mLayoutManager;
                    mLayoutManager = new PreCachingLayoutManager(baseActivity);
                    recyclerView.setLayoutManager(mLayoutManager);
                    recyclerView.setAdapter(new AlbumView(baseActivity, jsons, true));
                }
            }
        }

        private void openFailedRequestInBrowser() {
            if (openExternalNotAlbum) {
                Intent i = new Intent(baseActivity, Website.class);
                i.putExtra(Website.EXTRA_URL, "https://imgur.com/gallery/" + hash);

                baseActivity.startActivity(i);
            }
            if (finishIfNone) baseActivity.finish();
        }
    }


    public static class GetAlbumJsonFromUrl extends AsyncTask<String, Void, ArrayList<JsonElement>> {

        public boolean gallery;
        public String hash;

        public Activity baseActivity;
        ArrayList<JsonElement> jsonFinal;

        public GetAlbumJsonFromUrl(@NotNull String url, @NotNull Activity baseActivity) {

            this.baseActivity = baseActivity;

            String rawDat = cutEnds(url);
            if (rawDat.contains("gallery")) {
                gallery = true;
            }
            if (rawDat.endsWith("/")) {
                rawDat = rawDat.substring(0, rawDat.length() - 1);
            }
            String rawdat2 = rawDat;
            if (rawdat2.substring(rawDat.lastIndexOf("/"), rawdat2.length()).length() < 4) {
                rawDat = rawDat.replace(rawDat.substring(rawDat.lastIndexOf("/"), rawdat2.length()), "");
            }
            {

                hash = getHash(rawDat);

            }
        }

        @Override
        protected ArrayList<JsonElement> doInBackground(final String... sub) {

            Ion.with(baseActivity)
                    .load("https://api.imgur.com/3/gallery/" + hash + ".json")
                    .addHeader("Authorization", OpenImgurLink.IMGUR_CLIENT_ID)
                    .asJsonObject()
                    .setCallback(new FutureCallback<JsonObject>() {
                        @Override
                        public void onCompleted(Exception e, JsonObject result) {
                            if (result != null && result.has("data")) {
                                Log.v(LogUtil.getTag(), result.toString());

                                JsonArray images = result.getAsJsonObject("data").get("images").getAsJsonArray();
                                if (images != null && !images.isJsonNull() && images.size() > 0) {

                                    for (JsonElement o : images) {
                                        jsonFinal.add(o);
                                    }

                                }
                            }
                        }
                    });

            return jsonFinal;
        }
    }

}
