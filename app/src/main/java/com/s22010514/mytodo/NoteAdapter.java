package com.s22010514.mytodo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private List<Note> noteList;
    private Context context;
    private DatabaseHelper dbHelper;
    private OnNoteDeletedListener onNoteDeletedListener;
    private FirebaseAuth mAuth;

    public interface OnNoteDeletedListener {
        void onNoteDeleted();
    }

    public NoteAdapter(List<Note> noteList, Context context) {
        this.noteList = noteList;
        this.context = context;
        this.dbHelper = new DatabaseHelper(context);
        this.mAuth = FirebaseAuth.getInstance();
    }

    public void setOnNoteDeletedListener(OnNoteDeletedListener listener) {
        this.onNoteDeletedListener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_item, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = noteList.get(position);
        holder.bind(note, position);
    }

    @Override
    public int getItemCount() {
        return noteList.size();
    }

    private void deleteNote(Note note, int position) {
        // Check user authentication
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(context, "❌ Authentication error. Please login again.", Toast.LENGTH_LONG).show();
            return;
        }

        String currentUserId = currentUser.getUid();

        new AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("🗑️ Delete Note")
                .setMessage("Are you sure you want to delete this note?\n\n\"" + note.getTitle() + "\"\n\nThis action cannot be undone.")
                .setPositiveButton("DELETE", (dialog, which) -> {
                    // Use secure delete method with user ID verification
                    boolean isDeleted = dbHelper.deleteNoteById(String.valueOf(note.getId()), currentUserId);
                    if (isDeleted) {
                        noteList.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, noteList.size());

                        if (onNoteDeletedListener != null) {
                            onNoteDeletedListener.onNoteDeleted();
                        }

                        Toast.makeText(context, "✅ Note deleted successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "❌ Failed to delete note or unauthorized access", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("CANCEL", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void editNote(Note note) {
        Intent intent = new Intent(context, AddNotePage.class);
        intent.putExtra("noteId", note.getId());
        intent.putExtra("noteTitle", note.getTitle());
        intent.putExtra("noteBody", note.getBody());
        context.startActivity(intent);
    }

    class NoteViewHolder extends RecyclerView.ViewHolder {

        private TextView noteTitle;
        private TextView noteBody;
        private TextView noteBodyExpanded;
        private MaterialButton editNoteBtn;
        private MaterialButton deleteNoteBtn;
        private boolean isExpanded = false;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            noteTitle = itemView.findViewById(R.id.noteTitle);
            noteBody = itemView.findViewById(R.id.noteBody);
            noteBodyExpanded = itemView.findViewById(R.id.noteBodyExpanded);
            editNoteBtn = itemView.findViewById(R.id.editNoteBtn);
            deleteNoteBtn = itemView.findViewById(R.id.deleteNoteBtn);
        }

        public void bind(Note note, int position) {
            noteTitle.setText(note.getTitle());

            // Handle body visibility and expansion
            String body = note.getBody();
            if (body != null && !body.trim().isEmpty()) {
                noteBody.setText(body);
                noteBody.setVisibility(View.VISIBLE);
                noteBodyExpanded.setText(body);

                // Set up click listener for expanding/collapsing body
                itemView.setOnClickListener(v -> toggleBodyExpansion());
            } else {
                noteBody.setVisibility(View.GONE);
                noteBodyExpanded.setVisibility(View.GONE);
                itemView.setOnClickListener(null);
            }

            // Reset expanded state when recycling views
            isExpanded = false;
            noteBodyExpanded.setVisibility(View.GONE);

            // Set up click listeners for action buttons
            editNoteBtn.setOnClickListener(v -> editNote(note));
            deleteNoteBtn.setOnClickListener(v -> deleteNote(note, position));
        }

        private void toggleBodyExpansion() {
            if (isExpanded) {
                // Collapse
                noteBodyExpanded.setVisibility(View.GONE);
                noteBody.setVisibility(View.VISIBLE);
                isExpanded = false;
            } else {
                // Expand
                noteBody.setVisibility(View.GONE);
                noteBodyExpanded.setVisibility(View.VISIBLE);
                isExpanded = true;
            }
        }
    }
}
