package com.hacms.app;

import android.os.Bundle;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

            TextView tv = new TextView(getContext());

            tv.setText("Task Board");
            tv.setTextSize(20);
            tv.setPadding(32, 32, 32, 32);

            tv.setOnClickListener(v -> {
                Intent intent = new Intent(
                        requireContext(),
                        TaskBoardActivity.class
                );

                startActivity(intent);
            });

            return tv;
        }
    }
