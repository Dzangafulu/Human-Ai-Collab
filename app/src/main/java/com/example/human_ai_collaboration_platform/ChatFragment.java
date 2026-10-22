package com.example.human_ai_collaboration_platform;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatFragment extends Fragment {

    private String receiverId;
    private String receiverName;
    private String senderRoom;
    private String receiverRoom;

    private DatabaseReference dbReferenceSender;
    private DatabaseReference dbReferenceReceiver;

    private ImageView sendBtn;
    private EditText messageText;
    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;

    private ValueEventListener messageListener;

    public ChatFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.activity_chat, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        // Get the receiver's details passed to this Fragment
        Bundle args = getArguments();

        if (args != null) {
            receiverId = args.getString("id");
            receiverName = args.getString("name");
        }

        // Find the views in fragment_chat.xml
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        sendBtn = view.findViewById(R.id.sendmessageIcon);
        messageText = view.findViewById(R.id.messageEdit);
        recyclerView = view.findViewById(R.id.chatrecycler);

        // Display the receiver's name
        toolbar.setTitle(
                receiverName != null ? receiverName : "Chat"
        );

        // Toolbar menu
        toolbar.inflateMenu(R.menu.main_menu);

        toolbar.setOnMenuItemClickListener(
                new Toolbar.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {

                        if (item.getItemId() == R.id.Return) {
                            androidx.navigation.fragment.NavHostFragment.findNavController(ChatFragment.this)
                                    .navigateUp();

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

        // Check that Firebase has a logged-in user
        String currentUserId = FirebaseAuth.getInstance().getUid();

        if (currentUserId == null || receiverId == null
                || receiverId.isEmpty()) {

            Toast.makeText(
                    requireContext(),
                    "Unable to open chat. User information is missing.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Create chat room IDs
        senderRoom = currentUserId + receiverId;
        receiverRoom = receiverId + currentUserId;

        // Set up the RecyclerView
        messageAdapter = new MessageAdapter(requireContext());

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(requireContext());

        layoutManager.setStackFromEnd(true);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(messageAdapter);

        // Firebase references
        dbReferenceSender = FirebaseDatabase.getInstance()
                .getReference("chats")
                .child(senderRoom);

        dbReferenceReceiver = FirebaseDatabase.getInstance()
                .getReference("chats")
                .child(receiverRoom);

        // Listen for messages and display them in timestamp order
        messageListener = new ValueEventListener() {

            @Override
            public void onDataChange(
                    @NonNull DataSnapshot snapshot) {

                List<MessageModel> messages = new ArrayList<>();

                for (DataSnapshot dataSnapshot :
                        snapshot.getChildren()) {

                    MessageModel message =
                            dataSnapshot.getValue(MessageModel.class);

                    if (message != null) {
                        messages.add(message);
                    }
                }

                messageAdapter.clear();

                for (MessageModel message : messages) {
                    messageAdapter.add(message);
                }

                messageAdapter.notifyDataSetChanged();

                if (messageAdapter.getItemCount() > 0) {
                    recyclerView.scrollToPosition(
                            messageAdapter.getItemCount() - 1
                    );
                }
            }

            @Override
            public void onCancelled(
                    @NonNull DatabaseError error) {

                if (isAdded()) {
                    Toast.makeText(
                            requireContext(),
                            "Failed to load messages.",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
        };

        dbReferenceSender.orderByChild("timestamp")
                .addValueEventListener(messageListener);

        // Send button
        sendBtn.setOnClickListener(v -> {

            String message = messageText.getText()
                    .toString()
                    .trim();

            if (!message.isEmpty()) {
                sendMessage(message);
            } else {
                Toast.makeText(
                        requireContext(),
                        "Message cannot be empty",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void sendMessage(String message) {

        String currentUserId = FirebaseAuth.getInstance().getUid();

        if (currentUserId == null) {
            return;
        }

        String messageId = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();

        MessageModel messageModel = new MessageModel(
                currentUserId,
                message,
                messageId,
                timestamp
        );

        dbReferenceSender.child(messageId)
                .setValue(messageModel)
                .addOnFailureListener(e -> {

                    if (isAdded()) {
                        Toast.makeText(
                                requireContext(),
                                "Failed to send message",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });

        dbReferenceReceiver.child(messageId)
                .setValue(messageModel);

        messageText.setText("");
    }

    @Override
    public void onDestroyView() {

        // Stop listening when the Fragment's view is destroyed
        if (dbReferenceSender != null && messageListener != null) {
            dbReferenceSender.removeEventListener(messageListener);
        }

        if (sendBtn != null) {
            sendBtn.setOnClickListener(null);
        }

        recyclerView = null;
        sendBtn = null;
        messageText = null;
        messageAdapter = null;

        super.onDestroyView();
    }
}