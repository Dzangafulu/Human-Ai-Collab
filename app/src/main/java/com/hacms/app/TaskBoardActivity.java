package com.hacms.app;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TaskBoardActivity extends AppCompatActivity {

    private LinearLayout taskContainer;

    private final List<Task> tasks = new ArrayList<>();

    private int nextTaskId = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createScreen();
    }

    private void createScreen() {

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.rgb(245, 247, 250));



        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(25, 20, 25, 20);
        header.setBackgroundColor(Color.rgb(25, 118, 210));

        TextView title = new TextView(this);
        title.setText("Task Board");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setTypeface(null, Typeface.BOLD);

        header.addView(
                title,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        Button addButton = new Button(this);
        addButton.setText("+ Add Task");

        addButton.setOnClickListener(v ->
                showCreateTaskDialog()
        );

        header.addView(addButton);

        mainLayout.addView(header);


        // TASK LIST


        ScrollView scrollView = new ScrollView(this);

        taskContainer = new LinearLayout(this);
        taskContainer.setOrientation(LinearLayout.VERTICAL);
        taskContainer.setPadding(20, 20, 20, 20);

        scrollView.addView(taskContainer);

        mainLayout.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        // =========================
        // SUGGESTIONS BUTTON
        // =========================

        Button suggestionsButton = new Button(this);
        suggestionsButton.setText("Task Suggestions");

        suggestionsButton.setOnClickListener(v ->
                showTaskSuggestions()
        );

        mainLayout.addView(
                suggestionsButton
        );

        setContentView(mainLayout);

        displayTasks();
    }


    // 1. CREATE TASK


    private void showCreateTaskDialog() {

        LinearLayout layout = createTaskForm();

        EditText title =
                (EditText) layout.getChildAt(0);

        EditText description =
                (EditText) layout.getChildAt(1);

        Spinner status =
                (Spinner) layout.getChildAt(2);

        Spinner priority =
                (Spinner) layout.getChildAt(3);

        EditText dueDate =
                (EditText) layout.getChildAt(4);

        EditText assigned =
                (EditText) layout.getChildAt(5);

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("Create Task")
                        .setView(layout)
                        .setNegativeButton(
                                "Cancel",
                                null
                        )
                        .setPositiveButton(
                                "Create",
                                null
                        )
                        .create();

        dialog.setOnShowListener(d -> {

            dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(v -> {

                if (title.getText().toString().trim().isEmpty()) {

                    title.setError(
                            "Enter a task title"
                    );

                    return;
                }

                Task task = new Task(
                        nextTaskId++,
                        title.getText().toString(),
                        description.getText().toString(),
                        assigned.getText().toString().isEmpty()
                                ? "Not assigned"
                                : assigned.getText().toString(),
                        status.getSelectedItem().toString(),
                        priority.getSelectedItem().toString(),
                        dueDate.getText().toString().isEmpty()
                                ? "No due date"
                                : dueDate.getText().toString()
                );

                tasks.add(task);

                displayTasks();

                Toast.makeText(
                        this,
                        "Task created",
                        Toast.LENGTH_SHORT
                ).show();

                dialog.dismiss();
            });
        });

        dialog.show();
    }


    // TASK FORM


    private LinearLayout createTaskForm() {

        LinearLayout layout = new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                30,
                10,
                30,
                10
        );

        EditText title = new EditText(this);
        title.setHint("Task title");

        layout.addView(title);

        EditText description = new EditText(this);
        description.setHint("Task description");
        description.setMinLines(3);

        layout.addView(description);

        // STATUS

        Spinner status = new Spinner(this);

        String[] statuses = {
                "To Do",
                "In Progress",
                "Completed"
        };

        status.setAdapter(
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        statuses
                )
        );

        layout.addView(status);

        // PRIORITY

        Spinner priority = new Spinner(this);

        String[] priorities = {
                "Low",
                "Medium",
                "High"
        };

        priority.setAdapter(
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        priorities
                )
        );

        layout.addView(priority);

        // DUE DATE

        EditText dueDate = new EditText(this);
        dueDate.setHint("Select due date");
        dueDate.setFocusable(false);

        dueDate.setOnClickListener(v ->
                showDatePicker(dueDate)
        );

        layout.addView(dueDate);

        // ASSIGNED MEMBER

        EditText assigned = new EditText(this);
        assigned.setHint("Assign to team member");

        layout.addView(assigned);

        return layout;
    }


    // 2. VIEW TASKS

    private void displayTasks() {

        taskContainer.removeAllViews();

        if (tasks.isEmpty()) {

            TextView empty = new TextView(this);

            empty.setText(
                    "No tasks yet.\nTap '+ Add Task' to create one."
            );

            empty.setTextSize(18);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(20, 50, 20, 50);

            taskContainer.addView(empty);

            return;
        }

        for (Task task : tasks) {

            addTaskCard(task);
        }
    }

    private void addTaskCard(Task task) {

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setPadding(
                25,
                25,
                25,
                25
        );

        card.setBackgroundColor(
                Color.WHITE
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                0,
                0,
                0,
                20
        );

        card.setLayoutParams(params);

        TextView title = new TextView(this);

        title.setText(
                task.getTitle()
        );

        title.setTextSize(20);
        title.setTypeface(
                null,
                Typeface.BOLD
        );

        card.addView(title);

        TextView description =
                new TextView(this);

        description.setText(
                "Description: " +
                        task.getDescription()
        );

        card.addView(description);

        TextView status =
                new TextView(this);

        status.setText(
                "Status: " +
                        task.getStatus()
        );

        card.addView(status);

        TextView priority =
                new TextView(this);

        priority.setText(
                "Priority: " +
                        task.getPriority()
        );

        card.addView(priority);

        TextView dueDate =
                new TextView(this);

        dueDate.setText(
                "Due date: " +
                        task.getDueDate()
        );

        card.addView(dueDate);

        TextView assigned =
                new TextView(this);

        assigned.setText(
                "Assigned to: " +
                        task.getAssignedTo()
        );

        card.addView(assigned);

        // BUTTON ROW

        LinearLayout buttons =
                new LinearLayout(this);

        buttons.setGravity(
                Gravity.END
        );

        Button assignButton =
                new Button(this);

        assignButton.setText(
                "Assign"
        );

        assignButton.setOnClickListener(
                v -> assignTask(task)
        );

        buttons.addView(assignButton);

        Button editButton =
                new Button(this);

        editButton.setText(
                "Edit"
        );

        editButton.setOnClickListener(
                v -> editTask(task)
        );

        buttons.addView(editButton);

        Button deleteButton =
                new Button(this);

        deleteButton.setText(
                "Delete"
        );

        deleteButton.setOnClickListener(
                v -> deleteTask(task)
        );

        buttons.addView(deleteButton);

        card.addView(buttons);

        taskContainer.addView(card);
    }


    // 3. EDIT TASK


    private void editTask(Task task) {

        LinearLayout layout =
                createTaskForm();

        EditText title =
                (EditText) layout.getChildAt(0);

        EditText description =
                (EditText) layout.getChildAt(1);

        Spinner status =
                (Spinner) layout.getChildAt(2);

        Spinner priority =
                (Spinner) layout.getChildAt(3);

        EditText dueDate =
                (EditText) layout.getChildAt(4);

        EditText assigned =
                (EditText) layout.getChildAt(5);

        title.setText(
                task.getTitle()
        );

        description.setText(
                task.getDescription()
        );

        dueDate.setText(
                task.getDueDate()
        );

        assigned.setText(
                task.getAssignedTo()
        );

        setSpinner(
                status,
                task.getStatus()
        );

        setSpinner(
                priority,
                task.getPriority()
        );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("Edit Task")
                        .setView(layout)
                        .setNegativeButton(
                                "Cancel",
                                null
                        )
                        .setPositiveButton(
                                "Save",
                                null
                        )
                        .create();

        dialog.setOnShowListener(d -> {

            dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(v -> {

                task.setTitle(
                        title.getText().toString()
                );

                task.setDescription(
                        description.getText().toString()
                );

                task.setStatus(
                        status.getSelectedItem().toString()
                );

                task.setPriority(
                        priority.getSelectedItem().toString()
                );

                task.setDueDate(
                        dueDate.getText().toString()
                );

                task.setAssignedTo(
                        assigned.getText().toString()
                );

                displayTasks();

                Toast.makeText(
                        this,
                        "Task updated",
                        Toast.LENGTH_SHORT
                ).show();

                dialog.dismiss();
            });
        });

        dialog.show();
    }


    // 4. DELETE TASK


    private void deleteTask(Task task) {

        new AlertDialog.Builder(this)
                .setTitle("Delete Task")
                .setMessage(
                        "Delete \"" +
                                task.getTitle() +
                                "\"?"
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Delete",
                        (dialog, which) -> {

                            tasks.remove(task);

                            displayTasks();

                            Toast.makeText(
                                    this,
                                    "Task deleted",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                )
                .show();
    }


    // 5. ASSIGN TASK


    private void assignTask(Task task) {

        String[] members = {
                "Rinaye",
                "Team Member 2",
                "Team Member 3",
                "Team Member 4",
                "Team Member 5"
        };

        new AlertDialog.Builder(this)
                .setTitle("Assign Task")
                .setItems(
                        members,
                        (dialog, which) -> {

                            task.setAssignedTo(
                                    members[which]
                            );

                            displayTasks();

                            Toast.makeText(
                                    this,
                                    "Task assigned to " +
                                            members[which],
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                )
                .show();
    }


    // 6. TASK SUGGESTIONS


    private void showTaskSuggestions() {

        String[] suggestions = {
                "Review project requirements",
                "Update project documentation",
                "Review questionnaire",
                "Analyse survey responses",
                "Prepare presentation",
                "Test the application",
                "Complete project report",
                "Review team progress"
        };

        new AlertDialog.Builder(this)
                .setTitle("Task Suggestions")
                .setItems(
                        suggestions,
                        (dialog, which) -> {

                            showSuggestedTask(
                                    suggestions[which]
                            );
                        }
                )
                .show();
    }

    private void showSuggestedTask(
            String suggestion
    ) {

        LinearLayout layout =
                createTaskForm();

        EditText title =
                (EditText) layout.getChildAt(0);

        title.setText(
                suggestion
        );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "Suggested Task"
                        )
                        .setView(layout)
                        .setNegativeButton(
                                "Cancel",
                                null
                        )
                        .setPositiveButton(
                                "Create",
                                null
                        )
                        .create();

        dialog.setOnShowListener(d -> {

            dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(v -> {

                EditText description =
                        (EditText) layout.getChildAt(1);

                Spinner status =
                        (Spinner) layout.getChildAt(2);

                Spinner priority =
                        (Spinner) layout.getChildAt(3);

                EditText dueDate =
                        (EditText) layout.getChildAt(4);

                EditText assigned =
                        (EditText) layout.getChildAt(5);

                Task task = new Task(
                        nextTaskId++,
                        suggestion,
                        description.getText().toString(),
                        assigned.getText().toString().isEmpty()
                                ? "Not assigned"
                                : assigned.getText().toString(),
                        status.getSelectedItem().toString(),
                        priority.getSelectedItem().toString(),
                        dueDate.getText().toString().isEmpty()
                                ? "No due date"
                                : dueDate.getText().toString()
                );

                tasks.add(task);

                displayTasks();

                dialog.dismiss();
            });
        });

        dialog.show();
    }


    // 7. DATE


    private void showDatePicker(
            EditText field
    ) {

        Calendar calendar =
                Calendar.getInstance();

        DatePickerDialog picker =
                new DatePickerDialog(
                        this,
                        (view, year, month, day) -> {

                            String date =
                                    day +
                                            "/" +
                                            (month + 1) +
                                            "/" +
                                            year;

                            field.setText(date);
                        },
                        calendar.get(
                                Calendar.YEAR
                        ),
                        calendar.get(
                                Calendar.MONTH
                        ),
                        calendar.get(
                                Calendar.DAY_OF_MONTH
                        )
                );

        picker.show();
    }


    // SPINNER


    private void setSpinner(
            Spinner spinner,
            String value
    ) {

        ArrayAdapter adapter =
                (ArrayAdapter) spinner.getAdapter();

        int position =
                adapter.getPosition(value);

        if (position >= 0) {
            spinner.setSelection(position);
        }
    }
}