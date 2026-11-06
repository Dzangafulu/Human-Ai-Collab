
package com.hacms.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.MyViewHolder> {

    private final List<UsersModel> userModelList;
    private final OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(UsersModel userModel);
    }

    public UsersAdapter(Context context, OnUserClickListener listener) {
        this.userModelList = new ArrayList<>();
        this.listener = listener;
    }

    public void add(UsersModel userModel) {
        userModelList.add(userModel);
        notifyItemInserted(userModelList.size() - 1);
    }

    public void clear() {
        int size = userModelList.size();
        userModelList.clear();

        if (size > 0) {
            notifyItemRangeRemoved(0, size);
        }
    }

    public void setUsers(List<UsersModel> users) {
        userModelList.clear();
        userModelList.addAll(users);
        notifyDataSetChanged();
    }

    public List<UsersModel> getUserModelList() {
        return userModelList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_row, parent, false);

        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull MyViewHolder holder,
            int position) {

        UsersModel userModel = userModelList.get(position);

        holder.name.setText(userModel.getUserName());
        holder.email.setText(userModel.getUserEmail());

        holder.itemView.setOnClickListener(view -> {
            if (listener != null) {
                listener.onUserClick(userModel);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userModelList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        private final TextView name;
        private final TextView email;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.username);
            email = itemView.findViewById(R.id.useremail);
        }
    }
}
