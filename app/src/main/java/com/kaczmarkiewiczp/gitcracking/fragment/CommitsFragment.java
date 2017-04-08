package com.kaczmarkiewiczp.gitcracking.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kaczmarkiewiczp.gitcracking.AccountUtils;
import com.kaczmarkiewiczp.gitcracking.R;
import com.kaczmarkiewiczp.gitcracking.adapter.CommitsAdapter;

import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CommitsFragment extends Fragment {

    private View rootView;
    private Context context;
    private PullRequest pullRequest;
    private Repository repository;
    private CommitsAdapter commitsAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private AccountUtils accountUtils;
    private GitHubClient gitHubClient;

    public CommitsFragment() {
        // requires empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_commits, container, false);
        rootView = view;
        context = view.getContext();

        Bundle bundle = getArguments();
        pullRequest = (PullRequest) bundle.getSerializable("pull request");
        repository = (Repository) bundle.getSerializable("repository");

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.rv_commits);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);
        commitsAdapter = new CommitsAdapter();
        recyclerView.setAdapter(commitsAdapter);
        recyclerView.setVisibility(View.VISIBLE);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.srl_commits);
        // TODO refreshing

        accountUtils = new AccountUtils(context);
        gitHubClient = accountUtils.getGitHubClient();

        new GetCommits().execute(gitHubClient);

        return view;
    }

    private class GetCommits extends AsyncTask<GitHubClient, Void, Boolean> {

        private List<RepositoryCommit> commits;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            commits = new ArrayList<>();
        }

        @Override
        protected Boolean doInBackground(GitHubClient... params) {
            GitHubClient gitHubClient = params[0];
            PullRequestService pullRequestService = new PullRequestService(gitHubClient);

            try {
                commits = pullRequestService.getCommits(repository, pullRequest.getNumber());
            } catch (IOException e) {
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if (!success) {
                return;
            }

            for (RepositoryCommit repositoryCommit : commits) {
                commitsAdapter.addCommit(repositoryCommit);
            }
        }
    }
}
