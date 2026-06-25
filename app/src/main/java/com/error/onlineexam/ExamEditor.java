package com.error.onlineexam;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.*;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.*;

public class ExamEditor extends AppCompatActivity {

    private ArrayList<Question> data;
    private int quizID;
    private String quizTitle;
    private CustomAdapter adapter;
    private boolean isEditMode = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_exam_editor);

        View root = findViewById(R.id.exam_editor);
        if (root != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        quizTitle = getIntent().getStringExtra("Quiz title");
        String existingId = getIntent().getStringExtra("Quiz ID");
        
        TextView title = findViewById(R.id.title);
        title.setText(quizTitle);

        Button submit = findViewById(R.id.submit);
        RecyclerView listview = findViewById(R.id.listview);
        listview.setLayoutManager(new LinearLayoutManager(this));

        data = new ArrayList<>();
        adapter = new CustomAdapter(data);
        listview.setAdapter(adapter);

        if (existingId != null) {
            isEditMode = true;
            quizID = Integer.parseInt(existingId);
            loadQuizData(existingId);
            submit.setText("Update Quiz");
        } else {
            data.add(new Question());
            adapter.notifyDataSetChanged();
        }

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(listview);

        submit.setOnClickListener(v -> {
            if (isEditMode) {
                saveQuizData();
            } else {
                fetchAndSaveQuiz();
            }
        });
    }

    private void loadQuizData(String qId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Quizzes").document(qId).collection("Questions").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    data.clear();
                    // Sort by document ID (0, 1, 2...)
                    List<QueryDocumentSnapshot> sortedDocs = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) sortedDocs.add(doc);
                    Collections.sort(sortedDocs, (d1, d2) -> {
                        try {
                            return Integer.compare(Integer.parseInt(d1.getId()), Integer.parseInt(d2.getId()));
                        } catch (Exception e) { return d1.getId().compareTo(d2.getId()); }
                    });

                    for (QueryDocumentSnapshot doc : sortedDocs) {
                        data.add(doc.toObject(Question.class));
                    }
                    if (data.isEmpty()) data.add(new Question());
                    adapter.notifyDataSetChanged();
                });
    }

    private void fetchAndSaveQuiz() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference lastIdRef = db.collection("Metadata").document("QuizCounter");

        db.runTransaction(transaction -> {
            Long currentID = transaction.get(lastIdRef).getLong("lastId");
            if (currentID == null) currentID = 10000L;
            long newID = currentID + 1;
            transaction.set(lastIdRef, Collections.singletonMap("lastId", newID));
            return newID;
        }).addOnSuccessListener(result -> {
            quizID = result.intValue();
            saveQuizData();
        }).addOnFailureListener(e -> {
            Toast.makeText(ExamEditor.this, "Failed to generate Quiz ID: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void saveQuizData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String quizKey = String.valueOf(quizID);

        Map<String, Object> quizData = new HashMap<>();
        quizData.put("title", quizTitle);
        quizData.put("totalQuestions", data.size());
        quizData.put("creatorId", Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid());

        db.collection("Quizzes").document(quizKey)
                .set(quizData)
                .addOnSuccessListener(aVoid -> {
                    for (int i = 0; i < data.size(); i++) {
                        db.collection("Quizzes").document(quizKey).collection("Questions")
                                .document(String.valueOf(i))
                                .set(data.get(i));
                    }

                    String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    Map<String, Object> userUpdate = new HashMap<>();
                    userUpdate.put("quizzesCreated", FieldValue.arrayUnion(quizKey));
                    db.collection("Users").document(uid).set(userUpdate, com.google.firebase.firestore.SetOptions.merge());

                    if (isEditMode) {
                        Toast.makeText(this, "Quiz updated successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("Quiz ID", quizKey);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(this, "Quiz ID " + quizID + " copied to clipboard", Toast.LENGTH_LONG).show();
                    }
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save quiz: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private final ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder dragged, @NonNull RecyclerView.ViewHolder target) {
            int fromPos = dragged.getAdapterPosition();
            int toPos = target.getAdapterPosition();
            Collections.swap(data, fromPos, toPos);
            adapter.notifyItemMoved(fromPos, toPos);
            return true;
        }

        @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
    };

    public static class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {
        private final ArrayList<Question> data;

        public CustomAdapter(ArrayList<Question> data) {
            this.data = data;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            EditText question, option1et, option2et, option3et, option4et;
            RadioButton option1rb, option2rb, option3rb, option4rb;
            LinearLayout newQuestion;
            RadioGroup radioGroup;

            public ViewHolder(View view) {
                super(view);
                question = view.findViewById(R.id.question);
                option1rb = view.findViewById(R.id.option1rb);
                option2rb = view.findViewById(R.id.option2rb);
                option3rb = view.findViewById(R.id.option3rb);
                option4rb = view.findViewById(R.id.option4rb);
                option1et = view.findViewById(R.id.option1et);
                option2et = view.findViewById(R.id.option2et);
                option3et = view.findViewById(R.id.option3et);
                option4et = view.findViewById(R.id.option4et);
                newQuestion = view.findViewById(R.id.new_question);
                radioGroup = view.findViewById(R.id.radio_group);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.question_edit, parent, false);
            return new ViewHolder(view);
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Question q = data.get(position);

            holder.question.setText(q.getQuestion());
            holder.option1et.setText(q.getOption1());
            holder.option2et.setText(q.getOption2());
            holder.option3et.setText(q.getOption3());
            holder.option4et.setText(q.getOption4());

            switch (q.getCorrectAnswer()) {
                case 1: holder.option1rb.setChecked(true); break;
                case 2: holder.option2rb.setChecked(true); break;
                case 3: holder.option3rb.setChecked(true); break;
                case 4: holder.option4rb.setChecked(true); break;
            }

            addTextWatcher(holder.question, q::setQuestion);
            addTextWatcher(holder.option1et, q::setOption1);
            addTextWatcher(holder.option2et, q::setOption2);
            addTextWatcher(holder.option3et, q::setOption3);
            addTextWatcher(holder.option4et, q::setOption4);

            holder.radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                if (holder.option1rb.isChecked()) q.setCorrectAnswer(1);
                else if (holder.option2rb.isChecked()) q.setCorrectAnswer(2);
                else if (holder.option3rb.isChecked()) q.setCorrectAnswer(3);
                else if (holder.option4rb.isChecked()) q.setCorrectAnswer(4);
            });

            if (position == data.size() - 1) {
                holder.newQuestion.setVisibility(View.VISIBLE);
                holder.newQuestion.setOnClickListener(v -> {
                    data.add(new Question());
                    notifyItemInserted(data.size() - 1);
                    ((RecyclerView) holder.itemView.getParent()).scrollToPosition(data.size() - 1);
                });
            } else {
                holder.newQuestion.setVisibility(View.GONE);
            }
        }

        private void addTextWatcher(EditText editText, TextUpdateListener listener) {
            editText.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    listener.onTextChanged(s.toString());
                }
            });
        }

        interface TextUpdateListener {
            void onTextChanged(String text);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }
}
