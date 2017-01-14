package com.yichiuan.onelook.presentation.main;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.yichiuan.onelook.R;
import com.yichiuan.onelook.data.remote.model.OLResponse;

import timber.log.Timber;


public class MainFragment extends Fragment implements MainContract.View {

    private MainContract.Presenter presenter;
    private SearchView searchView;
    private MenuItem menuSearchItem;

    private ProgressBar progressBar;

    public static MainFragment newInstance() {
        MainFragment fragment = new MainFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_main, container, false);
        setHasOptionsMenu(true);

        progressBar = (ProgressBar)container.findViewById(R.id.progressBar);
        progressBar.post(() -> progressBar.bringToFront());
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.subscribe();
    }

    @Override
    public void onPause() {
        presenter.unsubscribe();
        super.onPause();
    }

    @Override
    public void setPresenter(@NonNull MainContract.Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.menu_main, menu);
        menuSearchItem = menu.findItem(R.id.menu_search);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getContext().getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menuSearchItem.getActionView();

        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        searchView.setIconifiedByDefault(true);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_search) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void handleNewIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Timber.i(query);

            presenter.fetchWordInfo(query);

            if (searchView!=null && !searchView.isIconified()) {

                searchView.clearFocus();
                searchView.setQuery("", false);
                searchView.setIconified(true);

                menuSearchItem.collapseActionView();
            }
        }
    }

    @Override
    public void setLoadingIndicator(boolean active) {
        if (active) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void showDefinition(String word, OLResponse response) {

        TextView definitionView = (TextView) getActivity().findViewById(R.id.text_definition);

        SpannableStringBuilder builder = new SpannableStringBuilder(word);
        builder.setSpan(new RelativeSizeSpan(1.5f), 0, word.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append("\n\n");

        if (response.resList() == null) {
            builder.append(getString(R.string.main_definition_not_found));
            definitionView.setText(builder);
            return;
        }

        if (response.quickDefs() == null) {
            builder.append(getString(R.string.main_definition_no_quick_def));
            definitionView.setText(builder);
            return;
        }

        if (response.quickDefs().size() > 0) {
            String origin = response.quickDefs().get(0).quickDef();

            int boldStart = builder.length();
            int speechEnd = origin.indexOf(':');
            builder.append(origin.substring(0, speechEnd));
            builder.setSpan(new StyleSpan(Typeface.BOLD), boldStart, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            String str = Html.fromHtml(origin.substring(speechEnd)).toString();
            String res = Html.fromHtml(str).toString();
            builder.append(Html.fromHtml(res));

            definitionView.setText(builder);
        }
    }
}