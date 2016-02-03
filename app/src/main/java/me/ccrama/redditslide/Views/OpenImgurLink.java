package me.ccrama.redditslide.Views;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import me.ccrama.redditslide.Activities.Album;
import me.ccrama.redditslide.Activities.AlbumPager;
import me.ccrama.redditslide.Activities.FullscreenImage;
import me.ccrama.redditslide.Activities.GifView;
import me.ccrama.redditslide.SettingValues;
import me.ccrama.redditslide.util.LogUtil;
import me.ccrama.redditslide.util.NetworkUtil;

/**
 * Created by carlo_000 on 1/16/2016.
 */
public class OpenImgurLink {
    public static final String IMGUR_CLIENT_ID = "Client-ID " + "bef87913eb202e9";

    /**
     * Determines which content type an imgur link is and opens it in the appropriate class
     * Types: Album, Gif, Image
     * @param c Context
     * @param url Imgur link in the format http://www.imgur.com/ID
     */

    public static void openImgurLink(final Context c, String url) {
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        String hash = url.substring(url.lastIndexOf("/"), url.length());
        String apiUrl = "https://api.imgur.com/3/gallery" + hash + ".json";
        Log.v(LogUtil.getTag(), "Opening: " + apiUrl);

        if (NetworkUtil.isConnected(c))
            Ion.with(c).load(apiUrl)
                    .addHeader("Authorization", OpenImgurLink.IMGUR_CLIENT_ID)
                    .asJsonObject()
                    .setCallback(new FutureCallback<JsonObject>() {
                        @Override
                        public void onCompleted(Exception e, JsonObject result) {
                            if (result != null && result.has("data") && !result.has("error")) {
                                Log.v(LogUtil.getTag(), result.toString());
                                JsonObject resultData = result.getAsJsonObject("data");

                                String url = resultData.get("link").getAsString();

                                if (resultData.get("is_album").getAsBoolean()) {
                                    if (SettingValues.album) {
                                        if (/*fixme SettingValues.albumSwipe*/false) {
                                            Intent i = new Intent(c, AlbumPager.class);
                                            i.putExtra(AlbumPager.EXTRA_URL, url);
                                            c.startActivity(i);
                                        } else {
                                            Intent i = new Intent(c, Album.class);
                                            i.putExtra(Album.EXTRA_URL, url);
                                            c.startActivity(i);
                                        }
                                    }
                                } else if (resultData.get("animated").getAsBoolean()) {
                                    Intent i = new Intent(c, GifView.class);
                                    i.putExtra(GifView.EXTRA_URL, url);
                                    c.startActivity(i);
                                    ((Activity) c).overridePendingTransition(0, 0);

                                } else {
                                    Intent i = new Intent(c, FullscreenImage.class);
                                    i.putExtra(FullscreenImage.EXTRA_URL, url);
                                    c.startActivity(i);
                                    ((Activity) c).overridePendingTransition(0, 0);
                                }
                                ((Activity) c).finish();
                            } else ((Activity) c).finish();
                        }
                    });


    }
}
