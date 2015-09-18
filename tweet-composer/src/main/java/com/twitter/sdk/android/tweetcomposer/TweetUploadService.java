/*
 * Copyright (C) 2015 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.twitter.sdk.android.tweetcomposer;

import android.app.IntentService;
import android.content.Intent;

import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterAuthToken;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.models.Tweet;

import io.fabric.sdk.android.Fabric;

public class TweetUploadService extends IntentService {
    public static final String UPLOAD_SUCCESS
            = "com.twitter.sdk.android.tweetcomposer.UPLOAD_SUCCESS";
    public static final String UPLOAD_FAILURE
            = "com.twitter.sdk.android.tweetcomposer.UPLOAD_FAILURE";
    static final String EXTRA_USER_TOKEN = "EXTRA_USER_TOKEN";
    static final String EXTRA_TWEET_TEXT = "EXTRA_TWEET_TEXT";
    static final String EXTRA_TWEET_CARD = "EXTRA_TWEET_CARD";
    static final String EXTRA_TWEET_CALL_TO_ACTION = "EXTRA_TWEET_CALL_TO_ACTION";
    static final String EXTRA_FAILED_INTENT = "EXTRA_FAILED_INTENT";
    private static final String TAG = "TweetUploadService";
    private static final int PLACEHOLDER_ID = -1;
    private static final String PLACEHOLDER_SCREEN_NAME = "";
    DependencyProvider dependencyProvider;

    TwitterSession twitterSession;
    String tweetText;
    Card tweetCard;
    String cardCallToAction;
    Intent intent;

    public TweetUploadService() {
        this(new DependencyProvider());
    }

    // testing purposes
    TweetUploadService(DependencyProvider dependencyProvider) {
        super("TweetUploadService");
        this.dependencyProvider = dependencyProvider;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final TwitterAuthToken token = intent.getParcelableExtra(EXTRA_USER_TOKEN);
        this.intent = intent;
        twitterSession = new TwitterSession(token, PLACEHOLDER_ID, PLACEHOLDER_SCREEN_NAME);
        tweetText = intent.getStringExtra(EXTRA_TWEET_TEXT);
        tweetCard = (Card) intent.getSerializableExtra(EXTRA_TWEET_CARD);
        cardCallToAction = intent.getStringExtra(EXTRA_TWEET_CALL_TO_ACTION);

        if (Card.isAppCard(tweetCard)) {
            uploadAppCardTweet(twitterSession, tweetText, tweetCard, cardCallToAction);
        } else {
            uploadTweet(twitterSession, tweetText);
        }
    }

    void uploadTweet(TwitterSession session, final String text) {
        final ComposerApiClient client = dependencyProvider.getComposerApiClient(session);
        client.getComposerStatusesService().update(text, null, new Callback<Tweet>() {
            @Override
            public void success(Result<Tweet> result) {
                sendSuccessBroadcast();
                stopSelf();
            }

            @Override
            public void failure(TwitterException exception) {
                fail(exception);
            }
        });
    }

    void uploadAppCardTweet(TwitterSession session, final String text, final Card card,
            final String cardCallToAction) {
        // Not implemented
        uploadTweet(session, text);
    }

    void fail(TwitterException e) {
        sendFailureBroadcast(intent);
        Fabric.getLogger().d(TAG, "Post Tweet failed", e);
        stopSelf();
    }

    void sendSuccessBroadcast() {
        final Intent intent = new Intent(UPLOAD_SUCCESS);
        sendBroadcast(intent);
    }

    void sendFailureBroadcast(Intent original) {
        final Intent intent = new Intent(UPLOAD_FAILURE);
        intent.putExtra(EXTRA_FAILED_INTENT, original);
        sendBroadcast(intent);
    }

    /*
     * Mockable class that provides ComposerController dependencies.
     */
    static class DependencyProvider {

        ComposerApiClient getComposerApiClient(TwitterSession session) {
            return TweetComposer.getInstance().getApiClient(session);
        }
    }
}