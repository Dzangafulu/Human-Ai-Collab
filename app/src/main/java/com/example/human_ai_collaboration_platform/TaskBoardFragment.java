package com.example.human_ai_collaboration_platform;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class TaskBoardFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(40, 40, 40, 40);

        TextView title = new TextView(requireContext());
        title.setText("Task Board");
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);

        layout.addView(title);

        TextView description = new TextView(requireContext());
        description.setText(
                "Manage assignments, deadlines, priorities and tasks."
        );
        description.setGravity(Gravity.CENTER);
        description.setPadding(0, 25, 0, 30);

        layout.addView(description);

        Button openButton = new Button(requireContext());
        openButton.setText("Open Task Board");

        openButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    requireContext(),
                    TaskBoardActivity.class
            );
            startActivity(intent);
        });

        layout.addView(openButton);

        return layout;
    }
}