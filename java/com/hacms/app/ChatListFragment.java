package com.hacms.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ChatListFragment extends Fragment {

    private RecyclerView recyclerView;
    private UsersAdapter usersAdapter;
    private DatabaseReference databaseReference;
    private ValueEventListener usersListener;

    public ChatListFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.activity_chat_list,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        // Find the views in the Fragment's layout
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        recyclerView = view.findViewById(R.id.recycler);

        // Set the toolbar title
        toolbar.setTitle("Chats");

        // Set up the toolbar menu
        toolbar.inflateMenu(R.menu.main_menu);

        toolbar.setOnMenuItemClickListener(
                new Toolbar.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {

                        if (item.getItemId() == R.id.Return) {

                            requireActivity()
                                    .getSupportFragmentManager()
                                    .popBackStack();

                            return true;
                        }

                        if (item.getItemId() == R.id.logout) {

                            FirebaseAuth.getInstance().signOut();

                            Intent intent = new Intent(
                                    requireActivity(),
                                    LoginActivity.class
                            );



                            intent.setFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK
                                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
                            );

                            startActivity(intent);

                            return true;
                        }


                        return false;
                    }
                }
        );

        // Set up the RecyclerView

        usersAdapter = new UsersAdapter(
                requireContext(),
                userModel -> {

                    ChatFragment chatFragment = new ChatFragment();

                    Bundle bundle = new Bundle();
                    bundle.putString("id", userModel.getUserId());
                    bundle.putString("name", userModel.getUserName());

                    chatFragment.setArguments(bundle);

                    requireActivity()
                            .getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.navHostFragment, chatFragment)
                            .addToBackStack(null)
                            .commit();
                }
        );


        recyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        recyclerView.setAdapter(usersAdapter);

        loadUsers();


        // Listen for changes to the users list
        usersListener = new ValueEventListener() {


            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                List<UsersModel> users = new ArrayList<>();

                String currentUserId = FirebaseAuth.getInstance().getUid();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {

                    UsersModel userModel =
                            dataSnapshot.getValue(UsersModel.class);

                    if (userModel == null) {
                        continue;
                    }

                    String userId = userModel.getUserId();

                    if (userId == null || userId.isEmpty()) {
                        userId = dataSnapshot.getKey();
                    }

                    if (userId != null && !userId.equals(currentUserId)) {

                        // Ensure the model has the ID used by the chat
                        userModel.setUserId(userId);

                        users.add(userModel);
                    }
                }

                usersAdapter.setUsers(users);
            }


            @Override
            public void onCancelled(
                    @NonNull DatabaseError error) {

                if (isAdded()) {
                    Toast.makeText(
                            requireContext(),
                            "Failed to load users.",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
        };
    }

    @Override
    public void onDestroyView() {

        // Remove the Firebase listener when the view is destroyed
        if (databaseReference != null && usersListener != null) {
            databaseReference.removeEventListener(usersListener);
        }

        recyclerView = null;
        usersAdapter = null;

        super.onDestroyView();
    }

    private void loadUsers() {
        String currentUserId = FirebaseAuth.getInstance().getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<UsersModel> users = new ArrayList<>();

                    for (QueryDocumentSnapshot document
                            : querySnapshot) {

                        // Don't display the currently logged-in user
                        if (document.getId().equals(currentUserId)) {
                            continue;
                        }

                        UsersModel user = new UsersModel();
                        user.setUserId(document.getId());
                        user.setUserName(document.getString("name"));
                        user.setUserEmail(document.getString("email"));

                        users.add(user);
                    }

                    usersAdapter.setUsers(users);
                })
                .addOnFailureListener(error -> {
                    Toast.makeText(
                            requireContext(),
                            "Could not load users: " + error.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}
