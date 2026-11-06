package com.hacms.app;

import static android.content.Intent.getIntent;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditNoteFragment extends Fragment {
    EditText medittitleofnote, meditcontentofnote;
    FloatingActionButton msaveeditnote;

    FirebaseAuth firebaseAuth;
    FirebaseUser firebaseuser;
    FirebaseFirestore firebaseFirestore;

    public EditNoteFragment() {
        // Required empty public constructor
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,@NonNull ViewGroup container,
                             @NonNull Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_edit_note, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @NonNull Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        medittitleofnote = view.findViewById(R.id.edittitleofnote);
        meditcontentofnote = view.findViewById(R.id.editcontentofnote);
        msaveeditnote = view.findViewById(R.id.saveeditnote);

        view.findViewById(R.id.schedulerBack).setOnClickListener(v->
                NavHostFragment.findNavController(this).popBackStack());


        Bundle data = getArguments();

        if (data == null) {
         Toast.makeText(requireContext(),"Note not found", Toast.LENGTH_SHORT).show();

         requireActivity().getSupportFragmentManager().popBackStack();
         return;
        }

        firebaseuser = firebaseAuth.getInstance().getCurrentUser();
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        msaveeditnote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newtitle = medittitleofnote.getText().toString();
                String newcontent = meditcontentofnote.getText().toString();

                if (newtitle.isEmpty() || newcontent.isEmpty()) {
                    Toast.makeText(requireContext(), "Both fields are required", Toast.LENGTH_SHORT).show();
                    return;
                }
                else
                {
                    String noteId = data.getString("noteId");

                    if (firebaseuser == null || noteId == null || noteId.isEmpty()) {
                        Toast.makeText(requireContext(), "Note not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DocumentReference documentReference = firebaseFirestore.collection("notes").document(firebaseuser.getUid()).collection("myNotes").document(noteId);

                    Map<String, Object> note = new HashMap<>();
                    note.put("title", newtitle);
                    note.put("content", newcontent);
                    documentReference.set(note).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Toast.makeText(requireContext(), "Note updated successfully", Toast.LENGTH_SHORT).show();

                            NavHostFragment.findNavController(EditNoteFragment.this).popBackStack(R.id.notesFragment, false);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(requireContext(), "Failed to update note", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

        });

        String noteTitle = data.getString("title");
        String noteContent = data.getString("content");

        medittitleofnote.setText(noteTitle);
        meditcontentofnote.setText(noteContent);

        Toolbar toolbar = view.findViewById(R.id.toolbarofnotedetail);

        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                requireActivity().getSupportFragmentManager().popBackStack();
            });
        }
    }

}
