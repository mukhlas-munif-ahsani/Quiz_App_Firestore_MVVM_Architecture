package com.example.quizapp_firestoremvvmarchitecture;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class QuizFragment extends Fragment implements View.OnClickListener {

    //declere
    private static final String TAG = "QUIZ_FRAGMENT_LOG";
    private FirebaseFirestore firebaseFirestore;
    private String quizId;

    //ui element
    private TextView quizTitle;
    private Button optionOneBtn;
    private Button optionTwoBtn;
    private Button optionThreeBtn;
    private Button nextBtn;
    private ImageButton closeBtn;
    private TextView questionFeedback;
    private TextView questionText;
    private TextView questionTime;
    private ProgressBar questionProgress;
    private TextView questionNumber;
    private CountDownTimer countDownTimer;
    //firebase data
    private List<QuestionsModel> allQuestionsList = new ArrayList<>();
    private long totalQuestionssToAnswer = 10;
    private List<QuestionsModel> questionsToAnswer = new ArrayList<>();

    private boolean canAnswer = false;
    private int currentQuestion = 0;

    public QuizFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_quiz, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //intialize
        firebaseFirestore = FirebaseFirestore.getInstance();

        //UI initialize
        quizTitle = view.findViewById(R.id.quiz_title);
        optionOneBtn = view.findViewById(R.id.quiz_option_one);
        optionTwoBtn = view.findViewById(R.id.quiz_option_two);
        optionThreeBtn = view.findViewById(R.id.quiz_option_three);
        nextBtn = view.findViewById(R.id.quiz_next_btn);
        questionFeedback = view.findViewById(R.id.quiz_question_feedback);
        questionText = view.findViewById(R.id.quiz_question);
        questionTime = view.findViewById(R.id.quiz_question_time);
        questionProgress = view.findViewById(R.id.quiz_question_progress);
        questionNumber = view.findViewById(R.id.quiz_question_number);


        //get quizId
        quizId = QuizFragmentArgs.fromBundle(getArguments()).getQuizId();
        totalQuestionssToAnswer = QuizFragmentArgs.fromBundle(getArguments()).getTotalQuestions();

        //Get all questions from this quiz
        firebaseFirestore.collection("QuizList")
                .document(quizId).collection("Questions")
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {

                    allQuestionsList = task.getResult().toObjects(QuestionsModel.class);
                    //Log.d(TAG, "Quiztions list ; " + allQuestionsList.get(0).getQuestion());
                    pickQuestions();
                    loadUi();

                } else {
                    //error
                    quizTitle.setText("Error : " + task.getException().getMessage());

                }

            }
        });

        //set button on click listener
        optionOneBtn.setOnClickListener(this);
        optionTwoBtn.setOnClickListener(this);
        optionThreeBtn.setOnClickListener(this);
    }

    private void loadUi() {
        //quiz data loaded, load the UI
        quizTitle.setText("quiz data loaded");
        questionNumber.setText("1");
        questionText.setText("Load first question");

        //Enable options
        enableOptions();

        //Load first question
        loadQuestion(1);
    }

    private void loadQuestion(int questionNum) {
        //Set question number
        questionNumber.setText(questionNum + "");

        //Load questions text
        questionText.setText(questionsToAnswer.get(questionNum).getQuestion());

        //Load options
        optionOneBtn.setText(questionsToAnswer.get(questionNum).getOption_a());
        optionTwoBtn.setText(questionsToAnswer.get(questionNum).getOption_b());
        optionThreeBtn.setText(questionsToAnswer.get(questionNum).getOption_c());

        //Question loaded, set can answer
        canAnswer = true;
        currentQuestion = questionNum;

        //Start question timer
        startTimer(questionNum);
    }

    private void startTimer(int questionNumber) {
        //Set timer text
        final Long timeToAnswer = questionsToAnswer.get(questionNumber).getTimer();
        questionTime.setText(timeToAnswer.toString());

        //Start timer progress bar
        questionProgress.setVisibility(View.VISIBLE);

        //Start countdown
        countDownTimer = new CountDownTimer(timeToAnswer * 1000, 10) {

            @Override
            public void onTick(long l) {
                //Update time
                questionTime.setText(l / 1000 + "");

                //progress in percent
                Long percent = l / (timeToAnswer * 10);
                questionProgress.setProgress(percent.intValue());
            }

            @Override
            public void onFinish() {
                //time up, cannot answer question
                canAnswer = false;
            }
        };

        countDownTimer.start();
    }

    private void enableOptions() {
        optionOneBtn.setVisibility(View.VISIBLE);
        optionTwoBtn.setVisibility(View.VISIBLE);
        optionThreeBtn.setVisibility(View.VISIBLE);

        optionOneBtn.setEnabled(true);
        optionTwoBtn.setEnabled(true);
        optionThreeBtn.setEnabled(true);

        questionFeedback.setVisibility(View.VISIBLE);
        nextBtn.setVisibility(View.INVISIBLE);
        nextBtn.setEnabled(false);
    }

    private void pickQuestions() {
        for (int i = 0; i < totalQuestionssToAnswer; i++) {
            int randomNumber = getRandomInteger(allQuestionsList.size(), 0);
            questionsToAnswer.add(allQuestionsList.get(randomNumber));
            allQuestionsList.remove(randomNumber);

            Log.d(TAG, "Quiztions " + i + " : " + questionsToAnswer.get(i).getQuestion());
        }
    }

    public static int getRandomInteger(int maximum, int minimum) {
        return ((int) (Math.random() * (maximum - minimum))) + minimum;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.quiz_option_one:
                answerSelected(optionOneBtn.getText());
                break;
            case R.id.quiz_option_two:
                answerSelected(optionTwoBtn.getText());
                break;
            case R.id.quiz_option_three:
                answerSelected(optionThreeBtn.getText());
                break;
        }
    }

    private void answerSelected(CharSequence selectedAnswer) {
        //check answer
        if (canAnswer){
            if (questionsToAnswer.get(currentQuestion).getAnswer().equals(selectedAnswer)){
                //correct answer
                Log.d(TAG, "Correct answer");
            }else {
                //wrong answer
                Log.d(TAG, "Wrong Answer");
            }
            canAnswer = false;
        }
    }
}
