package com.kaczmarkiewiczp.gitcracking.adapter;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kaczmarkiewiczp.gitcracking.R;

import java.util.ArrayList;

public class PRDiffAdapter extends RecyclerView.Adapter<PRDiffAdapter.ViewHolder> {

    private ArrayList<String> files;
    private ArrayList<String> diffs;
    private int fileChanges;
    private int lineAdditions;
    private int lineDeletions;
    private Context context;

    public PRDiffAdapter() {
        files = new ArrayList<>();
        diffs = new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();

        int layoutIdForListItem = R.layout.diff_list_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutIdForListItem, parent, shouldAttachToParentImmediately);
        return new ViewHolder(view);
    }

    @SuppressWarnings("deprecation") // for getColor -- check in code for android version
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (position == 0) {
            insertSummary(holder);
        } else {
            insertDiff(holder, position - 1);
        }
    }

    private void insertDiff(final ViewHolder holder, int position) {
        String filename = files.get(position);
        String diff = diffs.get(position);

        holder.linearLayoutDiff.setVisibility(View.VISIBLE);
        holder.linearLayoutDiffSummary.setVisibility(View.GONE);

        holder.textViewDiffFile.setText(filename);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        String[] diffLines = diff.split("\\r?\\n");
        for (String line : diffLines) {
            TextView textView = new TextView(context);
            textView.setText(line);
            textView.setLayoutParams(layoutParams);
            textView.setPadding(0, 0, 0, 4);

            if (line.startsWith("+")) {
                int color;
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    color = context.getColor(R.color.diff_addition);
                } else {
                    color = context.getResources().getColor(R.color.diff_addition);
                }
                textView.setBackgroundColor(color);
            } else if (line.startsWith("-")) {
                int color;
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    color = context.getColor(R.color.diff_deletion);
                } else {
                    color = context.getResources().getColor(R.color.diff_deletion);
                }
                textView.setBackgroundColor(color);
            } else if (line.startsWith("@@")) {
                int color;
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    color = context.getColor(R.color.diff_hunk_range);
                } else {
                    color = context.getResources().getColor(R.color.diff_hunk_range);
                }
                textView.setBackgroundColor(color);
                textView.setTextColor(Color.WHITE);
            }
            holder.linearLayoutDiffLines.addView(textView);
        }
        holder.imageViewExpand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.linearLayoutDiffLines.getVisibility() == View.VISIBLE) {
                    holder.linearLayoutDiffLines.setVisibility(View.GONE);
                } else {
                    holder.linearLayoutDiffLines.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void insertSummary(ViewHolder holder) {
        String changedFiles;
        if (fileChanges == 1) {
            changedFiles = String.valueOf(fileChanges) + " changed file";
        } else {
            changedFiles = String.valueOf(fileChanges) + " changed files";
        }
        String additions = String.valueOf(lineAdditions) + " additions";
        String deletions = String.valueOf(lineDeletions) + " deletions";
        holder.linearLayoutDiff.setVisibility(View.GONE);
        holder.linearLayoutDiffSummary.setVisibility(View.VISIBLE);
        holder.textViewChangedFiles.setText(changedFiles);
        holder.textViewDiffAdditions.setText(additions);
        holder.textViewDiffDeletions.setText(deletions);
    }

    @Override
    public int getItemCount() {
        if (files == null) {
            return 0;
        }
        return files.size() + 1;
    }

    public void addFileDiff(String filename, String diff) {
        files.add(filename);
        diffs.add(diff);
        notifyDataSetChanged();
    }

    public void addSummary(int fileChanges, int additions, int deletions) {
        this.fileChanges = fileChanges;
        lineAdditions = additions;
        lineDeletions = deletions;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        public final LinearLayout linearLayoutDiff;
        public final TextView textViewDiffFile;
        public final ImageView imageViewExpand;
        public final LinearLayout linearLayoutDiffLines;
        public final LinearLayout linearLayoutDiffSummary;
        public final TextView textViewChangedFiles;
        public final TextView textViewDiffAdditions;
        public final TextView textViewDiffDeletions;

        public ViewHolder(View view) {
            super(view);
            linearLayoutDiff = (LinearLayout) view.findViewById(R.id.ll_diff);
            textViewDiffFile = (TextView) view.findViewById(R.id.tv_diff_file);
            linearLayoutDiffLines = (LinearLayout) view.findViewById(R.id.ll_diff_lines);
            imageViewExpand = (ImageView) view.findViewById(R.id.iv_expand);
            linearLayoutDiffSummary = (LinearLayout) view.findViewById(R.id.ll_diff_summary);
            textViewChangedFiles = (TextView) view.findViewById(R.id.tv_changed_files);
            textViewDiffAdditions = (TextView) view.findViewById(R.id.tv_diff_additions);
            textViewDiffDeletions = (TextView) view.findViewById(R.id.tv_diff_deletions);
        }
    }
}
