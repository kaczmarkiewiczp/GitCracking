package com.kaczmarkiewiczp.gitcracking;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.kaczmarkiewiczp.gitcracking.adapter.DashboardAdapter;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.NoSuchPageException;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.service.EventService;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.eclipse.egit.github.core.service.RepositoryService;

import java.io.IOException;
import java.util.Collection;

import jp.wasabeef.recyclerview.animators.SlideInUpAnimator;

public class Dashboard extends AppCompatActivity {

    private ProgressBar loadingIndicator;
    private RecyclerView recyclerView;
    private LinearLayout emptyView;
    private TextView pullRequestsWidget;
    private TextView issuesWidget;
    private TextView repositoriesWidget;
    private AccountUtils accountUtils;
    private GitHubClient gitHubClient;
    private DashboardAdapter dashboardAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private AsyncTask widgetBackgroundTask;
    private AsyncTask newsFeedBackgroundTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        Toolbar toolbar = (Toolbar) findViewById(R.id.activity_dashboard_toolbar);
        toolbar.setTitle("Dashboard");
        setSupportActionBar(toolbar);
        new NavBarUtils(this, toolbar, NavBarUtils.DASHBOARD);

        accountUtils = new AccountUtils(this);
        gitHubClient = accountUtils.getGitHubClient();

        loadingIndicator = (ProgressBar) findViewById(R.id.pb_loading_indicator);
        pullRequestsWidget = (TextView) findViewById(R.id.tv_pull_request_count);
        issuesWidget = (TextView) findViewById(R.id.tv_issues_count);
        repositoriesWidget = (TextView) findViewById(R.id.tv_repositories_count);
        emptyView = (LinearLayout) findViewById(R.id.ll_empty_view);

        recyclerView = (RecyclerView) findViewById(R.id.rv_dashboard);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        dashboardAdapter = new DashboardAdapter();
        recyclerView.setAdapter(dashboardAdapter);
        recyclerView.setItemAnimator(new SlideInUpAnimator());
        recyclerView.getItemAnimator().setAddDuration(1000);
        recyclerView.getItemAnimator().setRemoveDuration(1000);
        recyclerView.setVisibility(View.VISIBLE);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.srl_dashboard);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (widgetBackgroundTask.getStatus() == AsyncTask.Status.RUNNING) {
                    widgetBackgroundTask.cancel(true);
                }
                if (newsFeedBackgroundTask.getStatus() == AsyncTask.Status.RUNNING) {
                    newsFeedBackgroundTask.cancel(true);
                }
                widgetBackgroundTask = new GetDashboardData().execute(gitHubClient);
                newsFeedBackgroundTask = new GetNewsFeedData().execute(gitHubClient);
            }
        });

        widgetBackgroundTask = new GetDashboardData().execute(gitHubClient);
        newsFeedBackgroundTask = new GetNewsFeedData().execute(gitHubClient);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                Animation rotate = AnimationUtils.loadAnimation(this, R.anim.rotate);
                findViewById(R.id.action_refresh).startAnimation(rotate);

                if (widgetBackgroundTask.getStatus() == AsyncTask.Status.RUNNING) {
                    widgetBackgroundTask.cancel(true);
                }
                if (newsFeedBackgroundTask.getStatus() == AsyncTask.Status.RUNNING) {
                    newsFeedBackgroundTask.cancel(true);
                }
                widgetBackgroundTask = new GetDashboardData().execute(gitHubClient);
                newsFeedBackgroundTask = new GetNewsFeedData().execute(gitHubClient);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void goToPullRequests(View view) {
        Intent intent = new Intent(this, PullRequests.class);
        intent.putExtra("hasParent", true);
        startActivity(intent);
    }

    public void goToIssues(View view) {
        Intent intent = new Intent(this, Issues.class);
        intent.putExtra("hasParent", true);
        startActivity(intent);
    }

    public void goToRepositories(View view) {
        Intent intent = new Intent(this, Repositories.class);
        intent.putExtra("hasParent", true);
        startActivity(intent);
    }


    private void showEmptyView() {
        TextView message = (TextView) findViewById(R.id.tv_empty_view);
        message.setText(getString(R.string.no_news_feed));
        swipeRefreshLayout.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
    }

    public class GetNewsFeedData extends AsyncTask<GitHubClient, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            swipeRefreshLayout.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            dashboardAdapter.clearEvents();
            if (!swipeRefreshLayout.isRefreshing()) {
                loadingIndicator.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected Boolean doInBackground(GitHubClient... params) {
            GitHubClient gitHubClient = params[0];
            EventService eventService = new EventService(gitHubClient);
            String user = accountUtils.getLogin();

            try {
                PageIterator<Event> eventPageIterator = eventService.pageUserReceivedEvents(user);
                Collection<Event> eventCollection = eventPageIterator.next();
                if (eventCollection.isEmpty()) {
                    return false;
                }
                for (Event anEventCollection : eventCollection) {
                    if (isCancelled()) {
                        return false;
                    }

                    dashboardAdapter.addEvent(anEventCollection);
                }
            } catch (NoSuchPageException e) {
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if (loadingIndicator.getVisibility() == View.VISIBLE) {
                loadingIndicator.setVisibility(View.INVISIBLE);
            }
            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            if (!success) {
                dashboardAdapter.clearEvents();
                showEmptyView();
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            if (loadingIndicator.getVisibility() == View.VISIBLE) {
                loadingIndicator.setVisibility(View.INVISIBLE);
            }
            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            dashboardAdapter.clearEvents();
            showEmptyView();
        }
    }


    public class GetDashboardData extends AsyncTask<GitHubClient, Void, Boolean> {

        private int pullRequestsCount;
        private int repositoriesCount;
        private int issuesCount;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pullRequestsCount = 0;
            repositoriesCount = 0;
            issuesCount = 0;

            if (pullRequestsWidget.getText().toString().isEmpty()) {
                pullRequestsWidget.setText("-");
            }
            if (issuesWidget.getText().toString().isEmpty()) {
                issuesWidget.setText("-");
            }
            if (repositoriesWidget.getText().toString().isEmpty()) {
                repositoriesWidget.setText("-");
            }
        }

        @Override
        protected Boolean doInBackground(GitHubClient... params) {
            GitHubClient gitHubClient = params[0];
            RepositoryService repositoryService = new RepositoryService(gitHubClient);
            PullRequestService pullRequestService = new PullRequestService(gitHubClient);

            try {
                for (Repository repository : repositoryService.getRepositories()) {
                    if (isCancelled()) {
                        return false;
                    }

                    issuesCount += repository.getOpenIssues();
                    repositoriesCount++;
                    pullRequestsCount += pullRequestService.getPullRequests(repository, PullRequestService.PR_STATE).size();
                }
            } catch (IOException e) {
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if (success) {
                pullRequestsWidget.setText(String.valueOf(pullRequestsCount));
                issuesWidget.setText(String.valueOf(issuesCount));
                repositoriesWidget.setText(String.valueOf(repositoriesCount));
            }
        }
    }
}
