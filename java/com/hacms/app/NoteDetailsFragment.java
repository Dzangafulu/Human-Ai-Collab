package com.hacms.app;

import static android.content.Intent.getIntent;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;


public class NoteDetailsFragment extends Fragment {

    private TextView mtitleofnotedetail, mcontentofnotedetail;
    FloatingActionButton mgotoeditnote;

    public NoteDetailsFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_note_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mtitleofnotedetail = view.findViewById(R.id.titlenotedetail);
        mcontentofnotedetail = view.findViewById(R.id.contentofnotedetail);
        mgotoeditnote = view.findViewById(R.id.gotoeditnote);

        view.findViewById(R.id.schedulerBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        Bundle data = getArguments();
        if (data == null) {
            Toast.makeText(requireContext(), "Note not found", Toast.LENGTH_SHORT).show();
            NavHostFragment.findNavController(this).popBackStack();
            return;
        }

        String title = data.getString("title");
        String content = data.getString("content");
        String noteId = data.getString("noteId");

        mtitleofnotedetail.setText(title);
        mcontentofnotedetail.setText(content);

        View.OnClickListener openEditor = v -> {
            Bundle editData = new Bundle();
            editData.putString("title", title);
            editData.putString("content", content);
            editData.putString("noteId", noteId);
            NavHostFragment.findNavController(this).navigate(R.id.editNoteFragment, editData);
        };
        mcontentofnotedetail.setOnClickListener(openEditor);
        mgotoeditnote.setOnClickListener(openEditor);
    }

}
