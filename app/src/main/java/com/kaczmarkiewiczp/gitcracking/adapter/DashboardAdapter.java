package com.kaczmarkiewiczp.gitcracking.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.kaczmarkiewiczp.gitcracking.R;
import org.eclipse.egit.github.core.event.CreatePayload;
import org.eclipse.egit.github.core.event.DeletePayload;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.ForkPayload;
import org.eclipse.egit.github.core.event.IssueCommentPayload;
import org.eclipse.egit.github.core.event.IssuesPayload;
import org.eclipse.egit.github.core.event.PullRequestPayload;
import org.eclipse.egit.github.core.event.PullRequestReviewCommentPayload;
import org.ocpsoft.prettytime.PrettyTime;
import java.util.ArrayList;
import java.util.Date;

public class DashboardAdapter extends RecyclerView.Adapter<DashboardAdapter.ViewHolder> {

    private final String LOG_TAG = "#DashboardAdapter";

    private ArrayList<Event> events;
    private Context context;

    public DashboardAdapter() {
        events = new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();

        int layoutIdForListItem = R.layout.dashboard_list_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutIdForListItem, parent, shouldAttachToParentImmediately);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Event currentEvent = events.get(position);

        String userIconUrl = currentEvent.getActor().getAvatarUrl();
        String user = currentEvent.getActor().getLogin();
        Date date = currentEvent.getCreatedAt();
        PrettyTime prettyTime = new PrettyTime();

        Glide
                .with(context)
                .load(userIconUrl)
                .error(context.getDrawable(android.R.drawable.sym_def_app_icon))
                .placeholder(R.drawable.progress_animation)
                .crossFade()
                .into(holder.imageViewUserIcon);
        holder.textViewDate.setText(prettyTime.format(date));
        holder.textViewUser.setText(user);

        resetViewsVisibilityToGone(holder);

        String type = currentEvent.getType();
        switch (type) {
            case Event.TYPE_ISSUES:
                issueEvent(holder, currentEvent);
                break;
            case Event.TYPE_CREATE:
                createEvent(holder, currentEvent);
                break;
            case Event.TYPE_DELETE:
                deleteEvent(holder, currentEvent);
                break;
            case Event.TYPE_FORK:
                forkEvent(holder, currentEvent);
                break;
            case Event.TYPE_ISSUE_COMMENT:
                issueCommentEvent(holder, currentEvent);
                break;
            case Event.TYPE_PUBLIC:
                publicEvent(holder, currentEvent);
                break;
            case Event.TYPE_PULL_REQUEST:
                pullRequestEvent(holder, currentEvent);
                break;
            case Event.TYPE_PUSH:
                pushEvent(holder, currentEvent);
                break;
            case Event.TYPE_WATCH:
                watchEvent(holder, currentEvent);
                break;
            case Event.TYPE_PULL_REQUEST_REVIEW_COMMENT:
                pullRequestReviewCommentEvent(holder, currentEvent);
                break;
            default:
                Log.d(LOG_TAG, "unknown event of type '" + type + "', payload '" + currentEvent.getPayload().toString() + "'");
                break;
        }
    }

    @Override
    public int getItemCount() {
        if (events == null) {
            return 0;
        }
        return events.size();
    }

    public void addEvent(Event event) {
        events.add(event);
        notifyItemInserted(events.size() - 1);
    }

    public void clearEvents() {
        events.clear();
        notifyDataSetChanged();
    }

    private void resetViewsVisibilityToGone(ViewHolder holder) {
        holder.textViewRef.setVisibility(View.GONE);
        holder.textViewRefType.setVisibility(View.GONE);
        holder.textViewRepository.setVisibility(View.GONE);
        holder.textViewPreposition.setVisibility(View.GONE);
        holder.linearLayoutDescription.setVisibility(View.GONE);
    }

    private void createEvent(ViewHolder holder, Event event) {
        CreatePayload createPayload = (CreatePayload) event.getPayload();
        String repository = event.getRepo().getName();
        String refType = "Created " + createPayload.getRefType();
        String ref = createPayload.getRef();
        String preposition = "at";

        holder.textViewRefType.setVisibility(View.VISIBLE);
        holder.textViewRefType.setText(refType);

        holder.textViewRef.setVisibility(View.VISIBLE);
        holder.textViewRef.setText(ref);

        holder.textViewPreposition.setVisibility(View.VISIBLE);
        holder.textViewPreposition.setText(preposition);

        holder.textViewRepository.setVisibility(View.VISIBLE);
        holder.textViewRepository.setText(repository);
    }

    private void deleteEvent(ViewHolder holder, Event event) {
        DeletePayload deletePayload = (DeletePayload) event.getPayload();
        String repository = event.getRepo().getName();
        String refType = "Deleted " + deletePayload.getRefType();
        String ref = deletePayload.getRef();
        String preposition = "at";

        holder.textViewRefType.setVisibility(View.VISIBLE);
        holder.textViewRefType.setText(refType);

        holder.textViewRef.setVisibility(View.VISIBLE);
        holder.textViewRef.setText(ref);

        holder.textViewPreposition.setVisibility(View.VISIBLE);
        holder.textViewPreposition.setText(preposition);

        holder.textViewRepository.setVisibility(View.VISIBLE);
        holder.textViewRepository.setText(repository);
    }

    private void forkEvent(ViewHolder holder, Event event) {
        ForkPayload forkPayload = (ForkPayload) event.getPayload();
        String repository = event.getRepo().getName();
        String refType = "Forked";
        String forkedRepoUrl = forkPayload.getForkee().getHtmlUrl();
        int forkedRepoStartIndex = 19;
        String description = "Forked repository is at " + forkedRepoUrl.substring(forkedRepoStartIndex);

        holder.textViewRefType.setVisibility(View.VISIBLE);
        holder.textViewRefType.setText(refType);

        holder.textViewRepository.setVisibility(View.VISIBLE);
        holder.textViewRepository.setText(repository);

        holder.linearLayoutDescription.setVisibility(View.VISIBLE);
        holder.textViewDescription.setText(description);
    }

    private void issueCommentEvent(ViewHolder holder, Event event) {
        IssueCommentPayload issueCommentPayload = (IssueCommentPayload) event.getPayload();
        String repository = event.getRepo().getName();
        String ref = String.valueOf(issueCommentPayload.getIssue().getNumber());
        String description = issueCommentPayload.getIssue().getTitle();
        String preposition = "in";
        String refType;

        switch (issueCommentPayload.getAction()) {
            case "created":
                refType = "Commented on issue";
                break;
            case "edited":
                refType = "Edited comment in issue";
                break;
            case "deleted":
                refType = "Deleted comment in issue";
                break;
            default:
                refType = "";
        }

        holder.textViewRefType.setVisibility(View.VISIBLE);
        holder.textViewRefType.setText(refType);

        holder.textViewRef.setVisibility(View.VISIBLE);
        holder.textViewRef.setText(ref);

        holder.textViewRepository.setVisibility(View.VISIBLE);
        holder.textViewRepository.setText(repository);

        holder.textViewPreposition.setVisibility(View.VISIBLE);
        holder.textViewPreposition.setText(preposition);

        holder.linearLayoutDescription.setVisibility(View.VISIBLE);
        holder.textViewDescription.setText(description);
    }

    private void issueEvent(ViewHolder holder, Event event) {
        IssuesPayload issuesPayload = (IssuesPayload) event.getPayload();
        String repository = event.getRepo().getName();
        String action = issuesPayload.getAction();
        action = action.substring(0, 1).toUpperCase() + action.substring(1);
        String issueNumber = String.valueOf(issuesPayload.getIssue().getNumber());
        String refType = action + " issue";
        String issueTitle = issuesPayload.getIssue().getTitle();
        String preposition = "on";

        holder.textViewRefType.setVisibility(View.VISIBLE);
        holder.textViewRefType.setText(refType);

        holder.textViewRef.setVisibility(View.VISIBLE);
        holder.textViewRef.setText(issueNumber);

        holder.textViewPreposition.setVisibility(View.VISIBLE);
        holder.textViewPreposition.setText(preposition);

        holder.textViewRepository.setVisibility(View.VISIBLE);
        holder.textViewRepository.setText(repository);

        holder.linearLayoutDescription.setVisibility(View.VISIBLE);
        holder.textViewDescription.setText(issueTitle);
    }

    private void publicEvent(ViewHolder holder, Event event) {
        String repository = event.getRepo().getName();
        String refType = "Open sourced";

        holder.textViewRefType.setVisibility(View.VISIBLE);
        holder.textViewRefType.setText(refType);

        holder.textViewRepository.setVisibility(View.VISIBLE);
        holder.textViewRepository.setText(repository);
    }

    private void pullRequestEvent(ViewHolder holder, Event event) {
        PullRequestPayload pullRequestPayload = (PullRequestPayload) event.getPayload();
        String repository = event.getRepo().getName();
        String refType = pullRequestPayload.getAction();
        refType = refType.substring(0, 1).toUpperCase() + refType.substring(1);
        refType = refType + " pull request";
        String ref = String.valueOf(pullRequestPayload.getPullRequest().getNumber());
        String preposition = "on";
        String description = pullRequestPayload.getPullRequest().getTitle();

        holder.textViewRefType.setVisibility(View.VISIBLE);
        holder.textViewRefType.setText(refType);

        holder.textViewRef.setVisibility(View.VISIBLE);
        holder.textViewRef.setText(ref);

        holder.textViewPreposition.setVisibility(View.VISIBLE);
        holder.textViewPreposition.setText(preposition);

        holder.textViewRepository.setVisibility(View.VISIBLE);
        holder.textViewRepository.setText(repository);


        holder.linearLayoutDescription.setVisibility(View.VISIBLE);
        holder.textViewDescription.setText(description);
    }

    private void pullRequestReviewCommentEvent(ViewHolder holder, Event event) {
        PullRequestReviewCommentPayload pullRequestReviewCommentPayload = (PullRequestReviewCommentPayload) event.getPayload();
        String repository = event.getRepo().getName();
        String ref = String.valueOf(pullRequestReviewCommentPayload.getPullRequest().getNumber());
        String preposition = "in";
        String description = pullRequestReviewCommentPayload.getPullRequest().getTitle();
        String refType;

        switch (pullRequestReviewCommentPayload.getAction()) {
            case "created":
                refType = "Commented on pull request";
                break;
            case "edited":
                refType = "Edited comment on pull request";
                break;
            case "deleted":
                refType = "Deleted comment on pull request";
                break;
            default:
                refType = ""; // this will never happen

        }

        holder.textViewRefType.setVisibility(View.VISIBLE);
        holder.textViewRefType.setText(refType);

        holder.textViewRef.setVisibility(View.VISIBLE);
        holder.textViewRef.setText(ref);

        holder.textViewRepository.setVisibility(View.VISIBLE);
        holder.textViewRepository.setText(repository);

        holder.textViewPreposition.setVisibility(View.VISIBLE);
        holder.textViewPreposition.setText(preposition);

        holder.linearLayoutDescription.setVisibility(View.VISIBLE);
        holder.textViewDescription.setText(description);
    }

    private void pushEvent(ViewHolder holder, Event event) {
        String repository = event.getRepo().getName();
        String refType = "Pushed";
        String preposition = "to";

        holder.textViewRefType.setVisibility(View.VISIBLE);
        holder.textViewRefType.setText(refType);

        holder.textViewPreposition.setVisibility(View.VISIBLE);
        holder.textViewPreposition.setText(preposition);

        holder.textViewRepository.setVisibility(View.VISIBLE);
        holder.textViewRepository.setText(repository);
    }

    private void watchEvent(ViewHolder holder, Event event) {
        String refType = "Starred";
        String repository = event.getRepo().getName();

        holder.textViewRefType.setVisibility(View.VISIBLE);
        holder.textViewRefType.setText(refType);

        holder.textViewRepository.setVisibility(View.VISIBLE);
        holder.textViewRepository.setText(repository);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public final ImageView imageViewUserIcon;
        public final TextView textViewUser;
        public final TextView textViewDate;
        public final TextView textViewRefType;
        public final TextView textViewRef;
        public final TextView textViewPreposition;
        public final TextView textViewRepository;
        public final LinearLayout linearLayoutDescription;
        public final TextView textViewDescription;

        public ViewHolder(View view) {
            super(view);
            imageViewUserIcon = (ImageView) view.findViewById(R.id.iv_user_icon);
            textViewUser = (TextView) view.findViewById(R.id.tv_user);
            textViewDate = (TextView) view.findViewById(R.id.tv_date);
            textViewRefType = (TextView) view.findViewById(R.id.tv_ref_type);
            textViewRef = (TextView) view.findViewById(R.id.tv_ref);
            textViewPreposition = (TextView) view.findViewById(R.id.tv_preposition);
            textViewRepository = (TextView) view.findViewById(R.id.tv_repository);
            linearLayoutDescription = (LinearLayout) view.findViewById(R.id.ll_description);
            textViewDescription = (TextView) view.findViewById(R.id.tv_description);
        }
    }
}
