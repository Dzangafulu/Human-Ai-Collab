package com.hacms.app;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NoteFragment extends Fragment {

    FloatingActionButton bcreatenotetab;
    RecyclerView recyclerView;
    StaggeredGridLayoutManager staggeredNotesAdapter;
    FirebaseUser firebaseUser;
    FirebaseFirestore firebaseFirestore;

    FirestoreRecyclerAdapter<firebasemodel, NoteViewHolder> noteAdapter;

    @NonNull
    @Override
    public View onCreateView( @NonNull LayoutInflater inflater,
                              @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_note, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bcreatenotetab = view.findViewById(R.id.createnotetab);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        firebaseFirestore = FirebaseFirestore.getInstance();

        if (firebaseUser == null) {
            Toast.makeText(requireContext(), "Please sign in first", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }

        Toolbar toolbar = view.findViewById(R.id.toolbarofnotes);
        toolbar.setTitle("Notes");

        FloatingActionButton addNoteButton = view.findViewById(R.id.createnotetab);

        addNoteButton.setOnClickListener(v ->
                NavHostFragment.findNavController(NoteFragment.this).navigate(R.id.createNoteFragment));

        Query query = firebaseFirestore.collection("notes").document(firebaseUser.getUid()).collection("myNotes").orderBy("title", Query.Direction.ASCENDING);

        FirestoreRecyclerOptions<firebasemodel> allusernotes = new FirestoreRecyclerOptions.Builder<firebasemodel>().setQuery(query, firebasemodel.class).build();

        noteAdapter = new FirestoreRecyclerAdapter<firebasemodel, NoteViewHolder>(allusernotes) {
            @Override
            protected void onBindViewHolder(
                    @NonNull NoteViewHolder holder,
                    int position,
                    @NonNull firebasemodel note) {

                ImageView popupbutton =
                        holder.itemView.findViewById(R.id.menupopbutton);

                holder.mnotetitle.setText(note.getTitle());
                holder.mnotecontent.setText(note.getContent());

                int colorCode = getRandomColor();
                holder.mnote.setBackgroundColor(
                        holder.itemView.getResources().getColor(colorCode, null)
                );

                // Open the note when the card is clicked.
                holder.itemView.setOnClickListener(v -> {
                    int currentPosition = holder.getBindingAdapterPosition();

                    if (currentPosition == RecyclerView.NO_POSITION) {
                        return;
                    }

                    String docId = getSnapshots()
                            .getSnapshot(currentPosition)
                            .getId();

                    Bundle args = new Bundle();
                    args.putString("title", note.getTitle());
                    args.putString("content", note.getContent());
                    args.putString("noteId", docId);

                    NavHostFragment.findNavController(NoteFragment.this)
                            .navigate(R.id.noteDetailsFragment, args);
                });

                // Show the Edit and Delete menu.
                popupbutton.setOnClickListener(v -> {
                    PopupMenu popupMenu = new PopupMenu(requireContext(), v, Gravity.END);
                    popupMenu.setGravity(Gravity.END);

                    popupMenu.getMenu().add("Edit")
                            .setOnMenuItemClickListener(item -> {
                                int currentPosition =
                                        holder.getBindingAdapterPosition();

                                if (currentPosition == RecyclerView.NO_POSITION
                                        || currentPosition >= getItemCount()) {
                                    return true;
                                }

                                String docId = getSnapshots()
                                        .getSnapshot(currentPosition).getId();

                                Bundle args = new Bundle();
                                args.putString("title", note.getTitle());
                                args.putString("content", note.getContent());
                                args.putString("noteId", docId);
                                NavHostFragment.findNavController(NoteFragment.this).navigate(R.id.editNoteFragment, args);
                                return true;
                            });

                    popupMenu.getMenu().add("Delete")
                            .setOnMenuItemClickListener(item -> {
                                int currentPosition =
                                        holder.getBindingAdapterPosition();

                                if (currentPosition == RecyclerView.NO_POSITION
                                        || currentPosition >= getItemCount()) {
                                    return true;
                                }

                                String docId = getSnapshots()
                                        .getSnapshot(currentPosition).getId();

                                firebaseFirestore.collection("notes")
                                        .document(firebaseUser.getUid())
                                        .collection("myNotes")
                                        .document(docId)
                                        .delete()
                                        .addOnSuccessListener(unused ->
                                                Toast.makeText(requireContext(),
                                                        "Note deleted",
                                                        Toast.LENGTH_SHORT
                                                ).show()
                                        )
                                        .addOnFailureListener(e ->
                                                Toast.makeText(requireContext(),
                                                        "Failed to delete: "
                                                                + e.getMessage(),
                                                        Toast.LENGTH_LONG
                                                ).show()
                                        );

                                return true;
                            });

                    popupMenu.show();
                });
            }

            @NonNull
            @Override
            public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notes_layout, parent, false);
                return new NoteViewHolder(view);
            }
        };

        recyclerView = view.findViewById(R.id.recyclerview);

        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        recyclerView.setItemAnimator(null);

        recyclerView.setAdapter(noteAdapter);


    }

    public class NoteViewHolder extends RecyclerView.ViewHolder {

        private TextView mnotetitle;
        private TextView mnotecontent;
        LinearLayout mnote;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            mnotetitle = itemView.findViewById(R.id.notetitle);
            mnotecontent = itemView.findViewById(R.id.notecontent);
            mnote = itemView.findViewById(R.id.note);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        noteAdapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (noteAdapter != null) {
            noteAdapter.stopListening();
        }
    }

    private int getRandomColor()
    {
        List<Integer> colorCode = new ArrayList<>();
        colorCode.add(R.color.gray);
        colorCode.add(R.color.green);
        colorCode.add(R.color.lightgreen);
        colorCode.add(R.color.skyblue);
        colorCode.add(R.color.pink);
        colorCode.add(R.color.color1);
        colorCode.add(R.color.color2);
        colorCode.add(R.color.color3);
        colorCode.add(R.color.color4);
        colorCode.add(R.color.color5);

        Random random = new Random();
        int number = random.nextInt(colorCode.size());
        return colorCode.get(number);
    }

}
