/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package thjread.epic;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Date;
import java.util.TimeZone;

import retrofit.ErrorHandler;
import retrofit.RestAdapter;
import retrofit.RetrofitError;

import static thjread.epic.EpicService.Photo;

public class EpicArtSource extends RemoteMuzeiArtSource {
    private static final String TAG = "Epic";
    private static final String SOURCE_NAME = "EpicArtSource";

    private static final int ROTATE_TIME_MILLIS = 30 * 60 * 1000; // rotate every half hour

    public EpicArtSource() {
        super(SOURCE_NAME);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);
    }

    private long dateDiff(Date d) {
        Date now = new Date();
        long day = 1000*60*60*24;
        long diff = Math.abs(now.getTime()-d.getTime()) % day;
        long ans = Math.min(diff, day-diff);//Put on same day
        return ans;
    }

    private Photo getBestPhoto(float timeRange /*in minutes*/) {
        Date date = new Date();
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("http://epic.gsfc.nasa.gov/")
                .setErrorHandler(new ErrorHandler() {
                    @Override
                    public Throwable handleError(RetrofitError retrofitError) {
                        int statusCode = retrofitError.getResponse().getStatus();
                        if (retrofitError.getKind() == RetrofitError.Kind.NETWORK
                                || (500 <= statusCode && statusCode < 600)) {
                            return new RetryException();
                        }
                        scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
                        return retrofitError;
                    }
                })
                .build();
        EpicService service = restAdapter.create(EpicService.class);
        SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-M-d");

        float bestDate = -1.0f;
        Photo photo = null;
        int days = 0;
        while ((bestDate < 0.0f || bestDate > timeRange*60*1000) && (days <= 6)) {
            List<Photo> photos = service.getPhotos(apiFormat.format(date));
            if (photos.size() == 0) {
                date.setTime(date.getTime()-24*60*60*1000);
                days++;
                continue;
            }
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            try {
                for (Photo p : photos) {
                    Date d = format.parse(p.date);
                    if (bestDate < 0.0f) {
                        photo = p;
                        bestDate = dateDiff(d);
                    } else {
                        if (dateDiff(d) < bestDate) {
                            photo = p;
                            bestDate = dateDiff(d);
                        }
                    }
                }
            } catch (ParseException e) {
                photo = photos.get(photos.size() - 1);
            }

            date.setTime(date.getTime()-24*60*60*1000);
            days++;
        }

        return photo;
    }

    @Override
    protected void onTryUpdate(int reason) throws RetryException {
        String oldToken = (getCurrentArtwork() != null) ? getCurrentArtwork().getToken() : null;

        Photo photo = getBestPhoto(60);

        String image_url = "http://i0.wp.com/"
                + "epic.gsfc.nasa.gov/epic-archive/jpg/"
                + photo.image + ".jpg"
                + "?lb=2048,4096";

        SimpleDateFormat outFormat = new SimpleDateFormat("yyyy-M-d h:mm aa");
        outFormat.setTimeZone(TimeZone.getDefault());
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = null;
        try {
            date = outFormat.format(format.parse(photo.date));
        } catch (ParseException e) {
            date = photo.date;
        }

        if (!photo.image.equals(oldToken)) {
            publishArtwork(new Artwork.Builder()
                    .title("Earth")
                    .byline("Taken " + date)
                    .imageUri(Uri.parse(image_url))
                    .token(photo.image)
                    .viewIntent(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://epic.gsfc.nasa.gov/")))
                    .build());
        }

        scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
    }
}

