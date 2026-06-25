package com.error.onlineexam;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ListQuizzes extends AppCompatActivity {

    private final ArrayList<String> data = new ArrayList<>();
    private final ArrayList<String> ids = new ArrayList<>();
    private final ArrayList<String> grades = new ArrayList<>();
    private final ArrayList<String> solvedIds = new ArrayList<>();
    private ListAdapter listAdapter;

    private boolean showGrade = false, solvedQuizzes = false, createdQuiz = false, quizGrade = false, allQuizzes = false;
    private String uid, quizID;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_list_quizzes);

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.list_quizzes), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView title = findViewById(R.id.title);
        ListView listView = findViewById(R.id.listview);

        uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (uid == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        listAdapter = new ListAdapter(data);
        listView.setAdapter(listAdapter);

        // Pre-load solved quizzes to show icons
        db.collection("Users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            List<String> solved = (List<String>) documentSnapshot.get("quizzesSolved");
            if (solved != null) {
                solvedIds.clear();
                solvedIds.addAll(solved);
                listAdapter.notifyDataSetChanged();
            }
        });

        String oper = getIntent().getStringExtra("Operation");
        if (oper == null) {
            Toast.makeText(this, "No operation specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        switch (oper) {
            case "List Created Quizzes":
                createdQuiz = true;
                title.setText("Created Quizzes");
                loadCreatedQuizzes();
                break;

            case "List Solved Quizzes":
                solvedQuizzes = true;
                title.setText("Solved Quizzes");
                loadSolvedQuizzes();
                break;

            case "List Quiz Grade":
                quizGrade = true;
                showGrade = true;
                quizID = getIntent().getStringExtra("Quiz ID");
                String quizTitle = getIntent().getStringExtra("Quiz Title");

                if (quizID == null) {
                    Toast.makeText(this, "Quiz ID not provided", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                title.setText(quizTitle);
                setupQuizIDCopy(title);
                loadQuizGrades();
                break;

            case "List All Quizzes":
                allQuizzes = true;
                title.setText("Explore Quizzes");
                loadAllQuizzes();
                break;

            default:
                Toast.makeText(this, "Unknown operation", Toast.LENGTH_SHORT).show();
                finish();
        }
    }

    private void setupQuizIDCopy(TextView title) {
        title.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Quiz ID", quizID));
            Toast.makeText(this, "Quiz ID copied", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadCreatedQuizzes() {
        db.collection("Users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            List<String> quizzesCreated = (List<String>) documentSnapshot.get("quizzesCreated");
            if (quizzesCreated == null || quizzesCreated.isEmpty()) {
                Toast.makeText(ListQuizzes.this, "No created quizzes found", Toast.LENGTH_SHORT).show();
                return;
            }

            data.clear();
            ids.clear();
            for (String qId : quizzesCreated) {
                db.collection("Quizzes").document(qId).get().addOnSuccessListener(quizDoc -> {
                    if (quizDoc.exists()) {
                        String title = quizDoc.getString("title");
                        if (title != null) {
                            ids.add(qId);
                            data.add(title);
                            listAdapter.notifyDataSetChanged();
                        }
                    }
                }).addOnFailureListener(e -> {
                    Log.e("ListQuizzes", "Error loading quiz: " + qId, e);
                });
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(ListQuizzes.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("ListQuizzes", "Error loading user quizzes", e);
        });
    }

    private void loadSolvedQuizzes() {
        db.collection("Users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            List<String> quizzesSolved = (List<String>) documentSnapshot.get("quizzesSolved");
            if (quizzesSolved == null || quizzesSolved.isEmpty()) {
                Toast.makeText(ListQuizzes.this, "No solved quizzes found", Toast.LENGTH_SHORT).show();
                return;
            }

            data.clear();
            ids.clear();
            for (String qId : quizzesSolved) {
                db.collection("Quizzes").document(qId).get().addOnSuccessListener(quizDoc -> {
                    if (quizDoc.exists()) {
                        String title = quizDoc.getString("title");
                        if (title != null) {
                            ids.add(qId);
                            data.add(title);
                            listAdapter.notifyDataSetChanged();
                        }
                    }
                }).addOnFailureListener(e -> {
                    Log.e("ListQuizzes", "Error loading solved quiz: " + qId, e);
                });
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(ListQuizzes.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void loadQuizGrades() {
        db.collection("Quizzes").document(quizID).get().addOnSuccessListener(quizSnapshot -> {
            Long totalQCount = quizSnapshot.getLong("totalQuestions");
            String totalQ = totalQCount != null ? totalQCount.toString() : "0";

            db.collection("Quizzes").document(quizID).collection("Answers").get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        data.clear();
                        ids.clear();
                        grades.clear();

                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            String userId = doc.getId();
                            Long points = doc.getLong("points");

                            db.collection("Users").document(userId).get().addOnSuccessListener(userSnapshot -> {
                                String fName = userSnapshot.getString("firstName");
                                String lName = userSnapshot.getString("lastName");
                                if (fName != null && lName != null) {
                                    ids.add(userId);
                                    data.add(fName + " " + lName);
                                    grades.add((points != null ? points : 0) + "/" + totalQ);
                                    listAdapter.notifyDataSetChanged();
                                }
                            });
                        }
                    });
        });
    }

    private void loadAllQuizzes() {
        db.collection("Quizzes").get().addOnSuccessListener(queryDocumentSnapshots -> {
            data.clear();
            ids.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                if (doc.getId().equals("Last ID")) continue; // Skip metadata if any
                String title = doc.getString("title");
                if (title != null) {
                    ids.add(doc.getId());
                    data.add(title);
                }
            }
            listAdapter.notifyDataSetChanged();
            if (data.isEmpty()) {
                Toast.makeText(ListQuizzes.this, "No quizzes found", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(ListQuizzes.this, "Error loading quizzes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    public class ListAdapter extends BaseAdapter {
        private final ArrayList<String> list;

        ListAdapter(ArrayList<String> list) {
            this.list = list;
        }

        @Override public int getCount() { return list.size(); }
        @Override public Object getItem(int position) { return list.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            @SuppressLint("ViewHolder") View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.quizzes_listitem, parent, false);

            TextView title = view.findViewById(R.id.quiz);
            TextView grade = view.findViewById(R.id.grade);
            ImageView solvedIndicator = view.findViewById(R.id.solved_indicator);
            RelativeLayout item = view.findViewById(R.id.item);

            title.setText(list.get(position));

            if (solvedIds.contains(ids.get(position))) {
                solvedIndicator.setVisibility(View.VISIBLE);
            } else {
                solvedIndicator.setVisibility(View.GONE);
            }

            if (showGrade) {
                grade.setVisibility(View.VISIBLE);
                grade.setText(grades.get(position));
            } else {
                grade.setVisibility(View.GONE);
            }

            item.setOnClickListener(v -> {
                String qId = ids.get(position);
                String qTitle = list.get(position);

                if (createdQuiz) {
                    showQuizIdDialog(qId, qTitle);
                } else if (solvedQuizzes || quizGrade || solvedIds.contains(qId)) {
                    // Redirect to Result if already solved or in grade list
                    Intent intent = new Intent(ListQuizzes.this, Result.class);
                    intent.putExtra("Quiz ID", quizGrade ? quizID : qId);
                    if (quizGrade) intent.putExtra("User UID", qId);
                    startActivity(intent);
                } else if (allQuizzes) {
                    // For All Quizzes not yet solved, go to Exam
                    Intent intent = new Intent(ListQuizzes.this, Exam.class);
                    intent.putExtra("Quiz ID", qId);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(ListQuizzes.this, ListQuizzes.class);
                    intent.putExtra("Operation", "List Quiz Grade");
                    intent.putExtra("Quiz ID", qId);
                    intent.putExtra("Quiz Title", qTitle);
                    startActivity(intent);
                }
            });

            return view;
        }

        private void showQuizIdDialog(String qId, String qTitle) {
            String[] options = {"Copy Quiz ID", "View Grades", "Edit Quiz", "Delete Quiz"};

            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(ListQuizzes.this);
            builder.setTitle(qTitle);
            builder.setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // Copy ID
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboard.setPrimaryClip(ClipData.newPlainText("Quiz ID", qId));
                        Toast.makeText(ListQuizzes.this, "Quiz ID copied to clipboard", Toast.LENGTH_SHORT).show();
                        break;
                    case 1: // View Grades
                        Intent intent = new Intent(ListQuizzes.this, ListQuizzes.class);
                        intent.putExtra("Operation", "List Quiz Grade");
                        intent.putExtra("Quiz ID", qId);
                        intent.putExtra("Quiz Title", qTitle);
                        startActivity(intent);
                        break;
                    case 2: // Edit Quiz
                        Intent editIntent = new Intent(ListQuizzes.this, ExamEditor.class);
                        editIntent.putExtra("Quiz ID", qId);
                        editIntent.putExtra("Quiz title", qTitle);
                        startActivity(editIntent);
                        break;
                    case 3: // Delete Quiz
                        confirmDelete(qId);
                        break;
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        }

        private void confirmDelete(String qId) {
            new androidx.appcompat.app.AlertDialog.Builder(ListQuizzes.this)
                    .setTitle("Delete Quiz")
                    .setMessage("Are you sure you want to delete this quiz? This action cannot be undone.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        db.collection("Quizzes").document(qId).delete().addOnSuccessListener(aVoid -> {
                            db.collection("Users").document(uid).update("quizzesCreated", com.google.firebase.firestore.FieldValue.arrayRemove(qId));
                            Toast.makeText(ListQuizzes.this, "Quiz deleted", Toast.LENGTH_SHORT).show();
                            loadCreatedQuizzes(); // Refresh list
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }
}
