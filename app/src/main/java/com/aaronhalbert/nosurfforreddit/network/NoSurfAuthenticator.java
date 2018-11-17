package com.aaronhalbert.nosurfforreddit.network;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.aaronhalbert.nosurfforreddit.BuildConfig;
import com.aaronhalbert.nosurfforreddit.Event;
import com.aaronhalbert.nosurfforreddit.exceptions.NoSurfAccessDeniedLoginException;
import com.aaronhalbert.nosurfforreddit.exceptions.NoSurfLoginException;
import com.aaronhalbert.nosurfforreddit.network.redditschema.AppOnlyOAuthToken;
import com.aaronhalbert.nosurfforreddit.network.redditschema.UserOAuthToken;

import java.util.UUID;

import androidx.lifecycle.MutableLiveData;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import static com.aaronhalbert.nosurfforreddit.network.Repository.NetworkErrors.APP_ONLY_AUTH_CALL_ERROR;
import static com.aaronhalbert.nosurfforreddit.network.Repository.NetworkErrors.REFRESH_AUTH_CALL_ERROR;
import static com.aaronhalbert.nosurfforreddit.network.Repository.NetworkErrors.USER_AUTH_CALL_ERROR;

/* see https://github.com/reddit-archive/reddit/wiki/OAuth2 for Reddit login API documentation */

public class NoSurfAuthenticator {
    private static final String RESPONSE_TYPE = "code";
    private static final String DURATION = "permanent";
    private static final String SCOPE = "identity mysubreddits read";
    private static final String AUTH_URL_RESPONSE_TYPE = "&response_type=";
    private static final String AUTH_URL_STATE = "&state=";
    private static final String AUTH_URL_REDIRECT_URI = "&redirect_uri=";
    private static final String AUTH_URL_DURATION = "&duration=";
    private static final String AUTH_URL_SCOPE = "&scope=";
    private static final String USER_GRANT_TYPE = "authorization_code";
    private static final String USER_REFRESH_GRANT_TYPE = "refresh_token";
    private static final String DEVICE_ID = "DO_NOT_TRACK_THIS_DEVICE";
    private static final String AUTH_HEADER = okhttp3.Credentials.basic(BuildConfig.CLIENT_ID, "");
    private static final String APP_ONLY_AUTH_CALL_FAILED = "App-only auth call failed";
    private static final String USER_AUTH_CALL_FAILED = "User auth call failed";
    private static final String REFRESH_AUTH_CALL_FAILED = "Refresh auth call failed";
    private static final String KEY_USER_OAUTH_ACCESS_TOKEN = "userAccessToken";
    private static final String KEY_USER_OAUTH_REFRESH_TOKEN = "userAccessRefreshToken";

    // caches to let us keep working during asynchronous writes to SharedPrefs
    private String userOAuthAccessTokenCache = "";
    private String userOAuthRefreshTokenCache = "";
    private String appOnlyOAuthTokenCache = "";
    private boolean isUserLoggedInCache;

    // user login status
    final MutableLiveData<Boolean> isUserLoggedInLiveData = new MutableLiveData<>();

    // other
    private final Application application;
    private final RetrofitAuthenticationInterface ri;
    private final SharedPreferences preferences;
    private Repository repository;

    public NoSurfAuthenticator(Application application,
                               Retrofit retrofit,
                               SharedPreferences preferences) {
        this.application = application;
        ri = retrofit.create(RetrofitAuthenticationInterface.class);
        this.preferences = preferences;
    }

    // region network auth calls -------------------------------------------------------------------

    /* Logged-out (aka anonymous, aka app-only users require an anonymous token to interact with
     * the Reddit API and view public posts and comments from r/all. This token is provided by
     * fetchAppOnlyOAuthTokenASync() and does not require any user credentials.
     *
     *  Also called to refresh this anonymous token when it expires */
    void fetchAppOnlyOAuthTokenASync(final Repository.NetworkCallbacks callback, final String id) {
        ri.fetchAppOnlyOAuthTokenASync(
                BuildConfig.OAUTH_BASE_URL,
                BuildConfig.APP_ONLY_GRANT_TYPE,
                DEVICE_ID,
                AUTH_HEADER)
                .enqueue(new Callback<AppOnlyOAuthToken>() {

                    @Override
                    public void onResponse(Call<AppOnlyOAuthToken> call,
                                           Response<AppOnlyOAuthToken> response) {
                        appOnlyOAuthTokenCache = response.body().getAccessToken();
                        // don't bother saving this ephemeral token into sharedprefs

                        switch (callback) {
                            case FETCH_ALL_POSTS_ASYNC:
                                repository.fetchAllPostsASync();
                                break;
                            case FETCH_POST_COMMENTS_ASYNC:
                                repository.fetchPostCommentsASync(id);
                                break;
                            case FETCH_SUBSCRIBED_POSTS_ASYNC:
                                // do nothing, as an app-only token is for logged-out users only
                                break;
                            default:
                                break;
                        }
                    }

                    @Override
                    public void onFailure(Call<AppOnlyOAuthToken> call, Throwable t) {
                        Log.e(getClass().toString(), APP_ONLY_AUTH_CALL_FAILED, t);
                        repository.setNetworkErrorsLiveData(new Event<>(APP_ONLY_AUTH_CALL_ERROR));
                    }
                });
    }

    /* Logged-in users can view posts from their subscribed subreddits in addition to r/all,
     * but this requires a user OAuth token, which is provided by fetchUserOAuthTokenASync().
     *
     * Note that after the user is logged in, their user token is now also used for viewing
     * r/all, instead of the previously-fetched anonymous token from fetchAppOnlyOAuthTokenASync */
    void fetchUserOAuthTokenASync(String code) {
        ri.fetchUserOAuthTokenASync(
                BuildConfig.OAUTH_BASE_URL,
                USER_GRANT_TYPE,
                code,
                BuildConfig.REDIRECT_URI,
                AUTH_HEADER)
                .enqueue(new Callback<UserOAuthToken>() {

                    @Override
                    public void onResponse(Call<UserOAuthToken> call,
                                           Response<UserOAuthToken> response) {
                        userOAuthAccessTokenCache = response.body().getAccessToken();
                        userOAuthRefreshTokenCache = response.body().getRefreshToken();

                        preferences
                                .edit()
                                .putString(KEY_USER_OAUTH_ACCESS_TOKEN, userOAuthAccessTokenCache)
                                .putString(KEY_USER_OAUTH_REFRESH_TOKEN, userOAuthRefreshTokenCache)
                                .apply();

                        repository.setUserLoggedIn();
                        repository.fetchAllPostsASync();
                        repository.fetchSubscribedPostsASync();
                    }

                    @Override
                    public void onFailure(Call<UserOAuthToken> call, Throwable t) {
                        Log.e(getClass().toString(), USER_AUTH_CALL_FAILED, t);
                        repository.setNetworkErrorsLiveData(new Event<>(USER_AUTH_CALL_ERROR));
                    }
                });
    }

    void refreshExpiredUserOAuthTokenASync(final Repository.NetworkCallbacks callback, final String id) {
        ri.refreshExpiredUserOAuthTokenASync(
                BuildConfig.OAUTH_BASE_URL,
                USER_REFRESH_GRANT_TYPE,
                userOAuthRefreshTokenCache,
                AUTH_HEADER)
                .enqueue(new Callback<UserOAuthToken>() {

                    @Override
                    public void onResponse(Call<UserOAuthToken> call, Response<UserOAuthToken> response) {
                        userOAuthAccessTokenCache = response.body().getAccessToken();

                        preferences
                                .edit()
                                .putString(KEY_USER_OAUTH_ACCESS_TOKEN, userOAuthAccessTokenCache)
                                .apply();

                        switch (callback) {
                            case FETCH_ALL_POSTS_ASYNC:
                                repository.fetchAllPostsASync();
                                break;
                            case FETCH_SUBSCRIBED_POSTS_ASYNC:
                                repository.fetchSubscribedPostsASync();
                                break;
                            case FETCH_POST_COMMENTS_ASYNC:
                                repository.fetchPostCommentsASync(id);
                                break;
                            default:
                                break;
                        }
                    }

                    @Override
                    public void onFailure(Call<UserOAuthToken> call, Throwable t) {
                        Log.e(getClass().toString(), REFRESH_AUTH_CALL_FAILED, t);
                        repository.setNetworkErrorsLiveData(new Event<>(REFRESH_AUTH_CALL_ERROR));
                    }
                });
    }

    // endregion network auth calls ----------------------------------------------------------------

    // region init/de-init methods -----------------------------------------------------------------

    /* login credentials are stored in SharedPreferences to survive not only config changes,
     *  but process termination and reboots, so user doesn't have to re-login every time the app
     *  exits
     *
     *  This method should run on app initialization, to see if the user's credentials have been
     *  previously saved */
    void checkIfLoginCredentialsAlreadyExist() {
        userOAuthAccessTokenCache = preferences
                .getString(KEY_USER_OAUTH_ACCESS_TOKEN, "");

        userOAuthRefreshTokenCache = preferences
                .getString(KEY_USER_OAUTH_REFRESH_TOKEN, "");

        if (!"".equals(userOAuthAccessTokenCache) && !"".equals(userOAuthRefreshTokenCache)) {
            setUserLoggedIn();
        } else {
            // need to explicitly call this here to help ContainerFragment set itself up
            setUserLoggedOut();
        }
    }

    void setUserLoggedIn() {
        isUserLoggedInCache = true;
        isUserLoggedInLiveData.setValue(true);
    }

    void setUserLoggedOut() {
        clearCookies();

        isUserLoggedInCache = false;
        isUserLoggedInLiveData.setValue(false);

        userOAuthAccessTokenCache = "";
        userOAuthRefreshTokenCache = "";

        preferences
                .edit()
                .putString(KEY_USER_OAUTH_ACCESS_TOKEN, "")
                .putString(KEY_USER_OAUTH_REFRESH_TOKEN, "")
                .apply();
    }

    // endregion init/de-init methods --------------------------------------------------------------

    // region helper methods -----------------------------------------------------------------------

    // clears cookies in the app's WebView
    private void clearCookies() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        } else {
            CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(application);
            cookieSyncManager.startSync();
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookie();
            cookieManager.removeSessionCookie();
            cookieSyncManager.stopSync();
            cookieSyncManager.sync();
        }
    }

    // endregion helper methods --------------------------------------------------------------------

    // region getters and setters ------------------------------------------------------------------

    String getUserOAuthAccessTokenCache() {
        return userOAuthAccessTokenCache;
    }

    String getAppOnlyOAuthTokenCache() {
        return appOnlyOAuthTokenCache;
    }

    boolean isUserLoggedInCache() {
        return isUserLoggedInCache;
    }

    void setRepository(Repository repository) {
        this.repository = repository;
    }

    // endregion getters and setters ---------------------------------------------------------------

    // region static methods -----------------------------------------------------------------------

    public static String buildAuthUrl() {
        return BuildConfig.AUTH_URL_BASE
                + BuildConfig.CLIENT_ID
                + AUTH_URL_RESPONSE_TYPE
                + RESPONSE_TYPE
                + AUTH_URL_STATE
                + UUID.randomUUID().toString()
                + AUTH_URL_REDIRECT_URI
                + BuildConfig.REDIRECT_URI
                + AUTH_URL_DURATION
                + DURATION
                + AUTH_URL_SCOPE
                + SCOPE;
    }

    /* get the one-time use code returned by the Reddit auth server. We later exchange it for a
     * bearer token */
    public static String extractCodeFromIntent(Intent i) throws NoSurfLoginException {
        final String ERROR = "error";
        final String CODE = "code";
        final String ACCESS_DENIED_ERROR_CODE = "access_denied";

        Uri uri;
        String error;
        String code;

        if (i.getData() != null) {
            uri = i.getData();
        } else {
            throw new NoSurfLoginException();
        }

        if (uri != null) {
            error = uri.getQueryParameter(ERROR);
            code = uri.getQueryParameter(CODE);
        } else {
            throw new NoSurfLoginException();
        }

        if (ACCESS_DENIED_ERROR_CODE.equals(error)) {
            throw new NoSurfAccessDeniedLoginException();
        } else if ("".equals(error)) {
            throw new NoSurfLoginException();
        }

        if (code != null && !"".equals(code)) {
            return code;
        } else {
            throw new NoSurfLoginException();
        }
    }

    // endregion static methods --------------------------------------------------------------------
}