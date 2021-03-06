package com.yichiuan.onelook.presentation.main;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;


public class MainFragment extends Fragment implements MainContract.View {

    @BindView(R.id.recyclerview_main_dictionary)
    HeaderRecyclerView dictionaryRecyclerview;

    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    private MainContract.Presenter presenter;
    private SearchView searchView;
    private MenuItem menuSearchItem;
    private View headerView;
    private TextView definitionView;
    private DictionariesAdapter dictionariesAdapter;
    private String currentWord;
    private OLResponse currentResponse;

    private String processText;

    public void setProcessText(String text) {
        processText = text;
    }

    public static MainFragment newInstance() {
        MainFragment fragment = new MainFragment();
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.bind(this, root);

        dictionaryRecyclerview.setLayoutManager(new LinearLayoutManager(getContext()));
        headerView = inflater.inflate(R.layout.item_header, dictionaryRecyclerview, false);
        definitionView = (TextView) headerView.findViewById(R.id.text_definition);
        dictionaryRecyclerview.setHeaderView(headerView);

        // Configuration Change
        if (savedInstanceState != null && currentResponse != null) {
            dictionariesAdapter = new DictionariesAdapter(getContext(), currentResponse.resList());
            dictionaryRecyclerview.setVisibility(View.VISIBLE);
        } else {

            dictionariesAdapter = new DictionariesAdapter(getContext(), null);
            dictionaryRecyclerview.setVisibility(View.INVISIBLE);
        }

        dictionaryRecyclerview.setAdapter(dictionariesAdapter);

        // Configuration Change
        if (savedInstanceState != null && currentResponse != null) {
            showDefinition();
        }

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.subscribe();
        if (processText != null) {
            presenter.fetchWordInfo(processText);
            collapseSearchView();
            processText = null;
        }
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
            collapseSearchView();

        } else if (Intent.ACTION_PROCESS_TEXT.equals(intent.getAction())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                String query = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT);
                Timber.i(query);

                presenter.fetchWordInfo(query);
                collapseSearchView();
            }
        }
    }

    private void collapseSearchView() {
        if (searchView != null && !searchView.isIconified()) {

            searchView.clearFocus();
            searchView.setQuery("", false);
            searchView.setIconified(true);

            menuSearchItem.collapseActionView();
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
    public void showWordInfo(String word, OLResponse response) {
        currentWord = word;
        currentResponse = response;

        dictionariesAdapter.setResources(response.resList());

        showDefinition();

        ((LinearLayoutManager)dictionaryRecyclerview.getLayoutManager()).scrollToPositionWithOffset(0, 0);
        dictionaryRecyclerview.setVisibility(View.VISIBLE);
    }

    private void showDefinition() {

        SpannableStringBuilder builder = new SpannableStringBuilder(currentWord);
        builder.setSpan(new RelativeSizeSpan(1.5f), 0, currentWord.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append("\n\n");

        if (currentResponse.resList() == null || currentResponse.resList().isEmpty()) {
            builder.append(getString(R.string.main_definition_not_found));
            definitionView.setText(builder);
            return;
        }

        if (currentResponse.quickDefs() == null) {
            builder.append(getString(R.string.main_definition_no_quick_def));
            definitionView.setText(builder);
            return;
        }

        if (currentResponse.quickDefs().size() > 0) {
            String origin = currentResponse.quickDefs().get(0).quickDef();

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