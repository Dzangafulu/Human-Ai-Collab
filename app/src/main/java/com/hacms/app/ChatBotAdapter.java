package com.hacms.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ChatBotAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final ArrayList<ChatBotModel> list;

    // Constructor
    public ChatBotAdapter(ArrayList<ChatBotModel> list) {
        this.list = list;
    }

    @Override
    public int getItemViewType(int position) {
        // Determine the type of view
        if (list.get(position).getSender().equals("user")) {

            // User message
            return 0;
        }
        else if (list.get(position).getSender().equals("bot")) {

            // Bot message
            return 1;
        }
        else {
            return -1;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == 0) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_user_messages, parent, false);
            return new UserViewHolder(view);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_bot_messages, parent, false);
            return new BotViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatBotModel chatModel = list.get(position);

        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).userTV.setText(chatModel.getMessage());
        } else if (holder instanceof BotViewHolder) {
            ((BotViewHolder) holder).botTV.setText(chatModel.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // ViewHolder for User messages
    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView userTV;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userTV = itemView.findViewById(R.id.idTVBot);
        }
    }

    // ViewHolder for Bot messages
    static class BotViewHolder extends RecyclerView.ViewHolder {
        TextView botTV;

        public BotViewHolder(@NonNull View itemView) {
            super(itemView);
            botTV = itemView.findViewById(R.id.idTVBot);
        }
    }
}
