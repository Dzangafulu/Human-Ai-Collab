package com.hacms.app;

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

public class CreateNoteFragment extends Fragment {

    EditText mcreatetitlerofnote, mcreatecontentrofnote;
    FloatingActionButton msavenote;
    FirebaseAuth firebaseAuth;
    FirebaseUser firebaseuser;
    FirebaseFirestore firebaseFirestore;

    public CreateNoteFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,@NonNull ViewGroup container,
                             @NonNull Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_create_note, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @NonNull Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        mcreatetitlerofnote = view.findViewById(R.id.createtitlerofnote);
        mcreatecontentrofnote = view.findViewById(R.id.createcontentrofnote);
        msavenote = view.findViewById(R.id.savenote);
        view.findViewById(R.id.schedulerBack).setOnClickListener(v->
                NavHostFragment.findNavController(this).popBackStack());


        firebaseAuth = FirebaseAuth.getInstance();
        firebaseuser = firebaseAuth.getInstance().getCurrentUser();
        firebaseFirestore = FirebaseFirestore.getInstance();

        msavenote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = mcreatetitlerofnote.getText().toString();
                String content = mcreatecontentrofnote.getText().toString();
                if (title.isEmpty() || content.isEmpty()) {
                    Toast.makeText(requireContext(), "Both fields are required", Toast.LENGTH_SHORT).show();
                    return;
                }
                else
                {
                    DocumentReference documentReference = firebaseFirestore.collection("notes").document(firebaseuser.getUid()).collection("myNotes").document();
                    Map<String, Object> note = new HashMap<>();
                    note.put("title", title);
                    note.put("content", content);

                    documentReference.set(note).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Toast.makeText(requireContext(), "Note added successfully", Toast.LENGTH_SHORT).show();

                            NavHostFragment.findNavController(CreateNoteFragment.this).popBackStack();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(requireContext(), "Failed to add note", Toast.LENGTH_SHORT).show();
                        }
                    });


                }
            }
        });

        Toolbar toolbar = view.findViewById(R.id.toolbarofnotedetail);

        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                requireActivity().getSupportFragmentManager().popBackStack();
            });
        }

    }
}
