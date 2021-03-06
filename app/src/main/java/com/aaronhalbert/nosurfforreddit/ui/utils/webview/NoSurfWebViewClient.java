/*
 * Copyright (c) 2018-present, Aaron J. Halbert.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package com.aaronhalbert.nosurfforreddit.ui.utils.webview;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.fragment.app.FragmentActivity;

import static com.aaronhalbert.nosurfforreddit.BuildConfig.REDIRECT_URI;

// also see NoSurfWebViewClientApiLevelBelow24; Dagger injects correct version based on API level

public class NoSurfWebViewClient extends WebViewClient {
    private final FragmentActivity hostFragmentActivity;

    public NoSurfWebViewClient(FragmentActivity hostFragmentActivity) {
        this.hostFragmentActivity = hostFragmentActivity;
    }

    /* hook into link clicks to check if the activity should capture a click as an intent
     *
     * currently only used during user login */
    @Override
    @TargetApi(24)
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();

        return processUrl(url);
    }

    //checks if URL is a custom NoSurf redirect URI
    boolean processUrl(String url) {
        if (url.contains(REDIRECT_URI)) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            hostFragmentActivity.startActivity(intent);
            return true;
        } else {
            return false;
        }
    }
}
