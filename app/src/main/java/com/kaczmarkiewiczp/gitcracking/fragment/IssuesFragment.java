package com.kaczmarkiewiczp.gitcracking.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v4.view.PagerAdapter;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.kaczmarkiewiczp.gitcracking.AccountUtils;
import com.kaczmarkiewiczp.gitcracking.Consts;
import com.kaczmarkiewiczp.gitcracking.IssueDetail;
import com.kaczmarkiewiczp.gitcracking.R;
import com.kaczmarkiewiczp.gitcracking.adapter.IssuesAdapter;

import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.UserService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jp.wasabeef.recyclerview.animators.SlideInUpAnimator;

public class IssuesFragment extends Fragment implements IssuesAdapter.IssueClickListener {
    public final String ARG_SECTION_NUMBER = "sectionNumber";
    private final int NETWORK_ERROR = 0;
    private final int API_ERROR = 1;
    private final int USER_CANCELLED_ERROR = 3;

    private final int SECTION_CREATED = 0;
    private final int SECTION_ASSIGNED = 1;
    private Context context;
    private View rootView;
    private int tabSection;
    private ProgressBar loadingIndicator;
    private IssuesAdapter issuesAdapter;
    private GitHubClient gitHubClient;
    private AccountUtils accountUtils;
    private SwipeRefreshLayout swipeRefreshLayout;
    private AsyncTask backgroundTask;
    private LinearLayout errorView;
    private LinearLayout emptyView;
    private IssueCountListener countListener;
    private IssueChangeListener changeListener;

    public interface IssueCountListener {
        void onIssueCountHasChanged(int tabSection, int count);
    }

    public interface IssueChangeListener {
        void onIssueDataHasChanged(boolean dataHasChanged);
    }

    public IssuesFragment() {
        // requires empty constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_issues, container, false);
        rootView = view;
        context = view.getContext();
        loadingIndicator = (ProgressBar) view.findViewById(R.id.pb_loading_indicator);
        emptyView = (LinearLayout) view.findViewById(R.id.ll_empty_view);
        errorView = (LinearLayout) view.findViewById(R.id.ll_connection_err);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.rv_issues);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);
        issuesAdapter = new IssuesAdapter(this);
        recyclerView.setAdapter(issuesAdapter);
        recyclerView.setItemAnimator(new SlideInUpAnimator());
        recyclerView.getItemAnimator().setAddDuration(1000);
        recyclerView.getItemAnimator().setRemoveDuration(1000);
        recyclerView.setVisibility(View.VISIBLE);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.srl_issues);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (backgroundTask.getStatus() == AsyncTask.Status.RUNNING) {
                    backgroundTask.cancel(true);
                }
                backgroundTask = new GetIssues().execute(gitHubClient);
            }
        });

        tabSection = getArguments().getInt(ARG_SECTION_NUMBER);

        accountUtils = new AccountUtils(context);
        gitHubClient = accountUtils.getGitHubClient();

        setHasOptionsMenu(true);

        backgroundTask = new GetIssues().execute(gitHubClient);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        countListener = (IssueCountListener) context;
        changeListener = (IssueChangeListener) context;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.actions, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                if (backgroundTask.getStatus() == AsyncTask.Status.RUNNING) {
                    backgroundTask.cancel(true);
                }
                backgroundTask = new GetIssues().execute(gitHubClient);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showErrorMessage(int errorType) {
        TextView message = (TextView) rootView.findViewById(R.id.tv_error_message);
        TextView retry = (TextView) rootView.findViewById(R.id.tv_try_again);

        retry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (backgroundTask.getStatus() == AsyncTask.Status.RUNNING) {
                    backgroundTask.cancel(true);
                }
                backgroundTask = new GetIssues().execute(gitHubClient);
            }
        });

        if (errorType == NETWORK_ERROR) {
            message.setText(getString(R.string.network_connection_error));
        } else if (errorType == API_ERROR) {
            message.setText(getString(R.string.loading_failed));
        }

        swipeRefreshLayout.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);
    }

    private void showEmptyView() {
        TextView message = (TextView) rootView.findViewById(R.id.tv_empty_view);
        message.setText(getString(R.string.no_issues));
        emptyView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onIssueClick(Issue clickedIssue, Repository issueRepository) {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putSerializable("issue", clickedIssue);
        bundle.putSerializable("repository", issueRepository);
        intent.putExtras(bundle);
        intent.setClass(context, IssueDetail.class);
        startActivityForResult(intent, Consts.ISSUE_DETAIL_INTENT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Consts.ISSUE_DETAIL_INTENT:
                if (resultCode == Consts.DATA_MODIFIED) {
                    changeListener.onIssueDataHasChanged(true);
                }
                return;
            default:
                return;
        }
    }

    public void reloadFragment() {
        if (rootView == null) {
            return;
        }
        if (backgroundTask != null && backgroundTask.getStatus() == AsyncTask.Status.RUNNING) {
            backgroundTask.cancel(true);
        }
        backgroundTask = new GetIssues().execute(gitHubClient);
    }

    public class GetIssues extends AsyncTask<GitHubClient, Void, Boolean> {

        private ArrayList<Issue> issues;
        private ArrayList<Repository> repositories;
        private int errorType;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            issues = new ArrayList<>();
            repositories = new ArrayList<>();
            issuesAdapter.clearIssues();

            swipeRefreshLayout.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            errorView.setVisibility(View.GONE);
            if (!swipeRefreshLayout.isRefreshing()) {
                loadingIndicator.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected Boolean doInBackground(GitHubClient... params) {
            GitHubClient gitHubClient = params[0];
            UserService userService = new UserService(gitHubClient);
            IssueService issueService = new IssueService(gitHubClient);
            RepositoryService repositoryService = new RepositoryService(gitHubClient);

            try {
                String user = userService.getUser().getLogin();
                for (Repository repository : repositoryService.getRepositories()) {
                    if (isCancelled()) {
                        errorType = USER_CANCELLED_ERROR;
                        return false;
                    }
                    if (repository.getOpenIssues() < 1) {
                        continue;
                    }
                    List<Issue> repositoryIssues = issueService.getIssues(repository, null);
                    for (Issue issue : repositoryIssues) {
                        if (issue.getPullRequest() != null) {
                            // github treats pull requests as issues, if this is a PR, skip it
                            continue;
                        }
                        if (tabSection == SECTION_ASSIGNED) {
                            if (issue.getAssignee() != null && issue.getAssignee().getLogin().equals(user)) {
                                issues.add(issue);
                                repositories.add(repository);
                            }
                        } else if (tabSection == SECTION_CREATED) {
                            issues.add(issue);
                            repositories.add(repository);
                        }

                    }
                }
            } catch (RequestException e) {
                if (e.getMessage().equals("Bad credentials")) {
                    // TODO token is invalid - tell user to login again
                } else {
                    errorType = API_ERROR;
                }
                return false;
            } catch (IOException e) {
                errorType = NETWORK_ERROR;
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean noError) {
            super.onPostExecute(noError);

            if (noError && !issues.isEmpty()) {
                for (int i = 0; i < issues.size(); i++) {
                    Issue issue = issues.get(i);
                    Repository repository = repositories.get(i);
                    issuesAdapter.addIssue(issue, repository);
                }
                issuesAdapter.showIssues();
                countListener.onIssueCountHasChanged(tabSection, issues.size());

            } else if (noError && issues.isEmpty()) {
                showEmptyView();
            } else if (errorType != USER_CANCELLED_ERROR) {
                showErrorMessage(errorType);
            }

            if (loadingIndicator.getVisibility() == View.VISIBLE) {
                loadingIndicator.setVisibility(View.GONE);
            }
            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            if (loadingIndicator.getVisibility() == View.VISIBLE) {
                loadingIndicator.setVisibility(View.GONE);
            }
            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
        }
    }
}
