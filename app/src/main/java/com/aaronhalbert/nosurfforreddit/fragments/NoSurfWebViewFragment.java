package com.aaronhalbert.nosurfforreddit.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.aaronhalbert.nosurfforreddit.R;
import com.aaronhalbert.nosurfforreddit.ShareHelper;
import com.aaronhalbert.nosurfforreddit.viewmodel.MainActivityViewModel;
import com.aaronhalbert.nosurfforreddit.viewmodel.NoSurfWebViewFragmentViewModel;
import com.aaronhalbert.nosurfforreddit.viewmodel.ViewModelFactory;
import com.aaronhalbert.nosurfforreddit.webview.NoSurfWebViewClient;

import javax.inject.Inject;

import androidx.lifecycle.ViewModelProviders;

public class NoSurfWebViewFragment extends BaseFragment {
    @SuppressWarnings("WeakerAccess") @Inject ViewModelFactory viewModelFactory;
    @SuppressWarnings("WeakerAccess") @Inject NoSurfWebViewClient noSurfWebViewClient;
    private NoSurfWebViewFragmentViewModel viewModel;
    private MainActivityViewModel mainActivityViewModel;
    private String url;

    // region lifecycle methods --------------------------------------------------------------------

    @Override
    public void onCreate(Bundle savedInstanceState) {
        getPresentationComponent().inject(this);
        super.onCreate(savedInstanceState);

        setupMenu();

        viewModel = ViewModelProviders.of(requireActivity(), viewModelFactory).get(NoSurfWebViewFragmentViewModel.class);
        mainActivityViewModel = ViewModelProviders.of(requireActivity()).get(MainActivityViewModel.class);
        setupObserverHack();

        url = NoSurfWebViewFragmentArgs.fromBundle(getArguments()).getUrl();
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.fragment_nosurf_webview, container, false);

        WebView browser = result.findViewById(R.id.nosurf_webview_fragment_webview);
        WebSettings browserSettings = browser.getSettings();
        browserSettings.setDomStorageEnabled(true); // Imgur needs this
        browserSettings.setJavaScriptEnabled(true);
        browserSettings.setBuiltInZoomControls(true);
        browserSettings.setDisplayZoomControls(false);
        browserSettings.setLoadWithOverviewMode(true);
        browserSettings.setUseWideViewPort(true);
        browser.setWebViewClient(noSurfWebViewClient);
        browser.loadUrl(url);

        return result;
    }

    // endregion lifecycle methods -----------------------------------------------------------------

    // region menu ---------------------------------------------------------------------------------

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_share_action_provider, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_item_share:
                ShareHelper shareHelper = new ShareHelper(getContext());
                shareHelper.createShareIntent(getLastClickedPostPermalink());
                shareHelper.launchShareIntent();

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // endregion menu ------------------------------------------------------------------------------

    // region helper methods -----------------------------------------------------------------------

    private void setupObserverHack() {
        /* these observers are necessary to ensure that a post is properly displayed as read
         * (i.e., struck/grayed-out) when the user clicks the image thumbnail to directly
         * go to a link post's URL. Without them, when this fragment is added, no call to
         * Repository.mergeClickedPostIdsWithCleanedPostsRawLiveData will be triggered, and
         * so when the user goes BACK to the list of posts, the post that was just clicked
         * will be incorrectly shown as unread. We don't do anything else with them. */
        viewModel.getAllPostsViewStateLiveData().observe(this, postsViewState -> {
            // do nothing
        });
        viewModel.getSubscribedPostsViewStateLiveData().observe(this, postsViewState -> {
            // do nothing
        });
    }

    private String getLastClickedPostPermalink() {
        return mainActivityViewModel
                .getLastClickedPostMetadata()
                .lastClickedPostPermalink;
    }

    void setupMenu() {
        setHasOptionsMenu(true);
    }

    // endregion helper methods --------------------------------------------------------------------
}
