package com.up.upsi;

import android.animation.Animator;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class QuestionsActivity extends AppCompatActivity {

    public static final String FILE_NAME = "UPSI";
    public static final String KEY_NAME = "QUESTIONS";

    private TextView question, noIndicator;
    private FloatingActionButton favoriteBTN;
    private LinearLayout optionContainer;
    private Button preBtn, nextBtn;
    private int count=0;
    private int position=0;
    private List<QuestionsModel> list;
    private int score =0;
    private String category;
    private int setNo;
    private Dialog loadingdialog;

    private  List<QuestionsModel> favoriteList;

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private Gson gson;
    private int matchedQuestionPosition;

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questions);

        Toolbar toolbar = findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);

       question  = findViewById(R.id.question);
       noIndicator  = findViewById(R.id.noIndicator);
       favoriteBTN  = findViewById(R.id.favoriteBtn);
       optionContainer  = findViewById(R.id.optionContainer);
       preBtn  = findViewById(R.id.previousBtn);
       nextBtn = findViewById(R.id.nextBtn);

       preferences = getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
       editor = preferences.edit();
       gson = new Gson();

       //
        getFavorite();
        favoriteBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(modelMatch()){
                    favoriteList.remove(matchedQuestionPosition);
                    favoriteBTN.setImageDrawable(getDrawable(R.drawable.favorite_border));
                }else{
                    favoriteList.add(list.get(position));
                    favoriteBTN.setImageDrawable(getDrawable(R.drawable.favorite));
                }
            }
        });

       //Receive category and setNo from previous activity for match query
       category = getIntent().getStringExtra("category");
       setNo = getIntent().getIntExtra("setNo", 1);

        //loading dialog
        loadingdialog = new Dialog(this);
        loadingdialog.setContentView(R.layout.loading);
        loadingdialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingdialog.setCancelable(false);

        list = new ArrayList<>();

        myRef.child("SETS").child(category).child("questions").orderByChild("setNo").equalTo(setNo).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot snapshot1:snapshot.getChildren()){
                    list.add(snapshot1.getValue(QuestionsModel.class));
                }
                if(list.size() > 0){
                    for(int i=0; i<4; i++){
                        optionContainer.getChildAt(i).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                checkAnswer((Button)v);
                            }
                        });
                    }
                    playAnimation(question, 0, list.get(position).getQuestion());

                    nextBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            nextBtn.setEnabled(false);
                            nextBtn.setAlpha(0.7f);
                            enableOption(true);
                            position++;
                            if(position == list.size()){
                                //score activity send on Score activity page
                                Intent scoreIntent = new Intent(QuestionsActivity.this, ScoreActivity.class);
                                scoreIntent.putExtra("score", score);
                                scoreIntent.putExtra("total", list.size());
                                startActivity(scoreIntent);
                                finish();
                                return;
                            }
                            count=0;
                            playAnimation(question, 0, list.get(position).getQuestion());
                        }
                    });

                }else{
                    finish();
                    Toast.makeText(QuestionsActivity.this, "No More Question!!", Toast.LENGTH_SHORT).show();
                }
                loadingdialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(QuestionsActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                loadingdialog.dismiss();
                finish();
            }
        });
    }

    //on pause method
    @Override
    protected void onPause() {
        super.onPause();
        storeFavorite();
    }

    //set animation for change question when start or press prevbtn or nextbtn
    private void playAnimation(View view, int value, final String data){
        view.animate().alpha(value).scaleX(value).scaleY(value).setDuration(250).setStartDelay(100)
                .setInterpolator(new DecelerateInterpolator()).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (value == 0 && count < 4) {
                    String option = "";
                    if(count == 0){
                        option = list.get(position).getOptA();
                    }else if(count == 1){
                        option = list.get(position).getOptB();
                    }else if(count == 2){
                        option = list.get(position).getOptC();
                    }else if(count == 3){
                        option = list.get(position).getOptD();
                    }

                    playAnimation(optionContainer.getChildAt(count), 0, option);
                    count++;
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                //change data
                if(value == 0){
                    try {
                        ((TextView)view).setText(data);
                        noIndicator.setText(position+1+"/"+list.size());
                        //End of Animation

                        if(modelMatch()){
                            favoriteBTN.setImageDrawable(getDrawable(R.drawable.favorite));
                        }else{
                            favoriteBTN.setImageDrawable(getDrawable(R.drawable.favorite_border));
                        }

                    }
                    catch (ClassCastException ex){
                        ((Button)view).setText(data);
                    }
                    view.setTag(data);
                    playAnimation(view, 1, data);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    private void checkAnswer(Button selectedOption){
        enableOption(false);
        nextBtn.setEnabled(true);
        nextBtn.setAlpha(1);

        if(selectedOption.getText().toString().equals(list.get(position).getCorrectANS())){
            //correct
            score++;
            selectedOption.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));

        }else{
            //incorrect
            selectedOption.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF0000")));
            Button correctOption =  (Button) optionContainer.findViewWithTag(list.get(position).getCorrectANS());
            correctOption.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        }
    }

    private void enableOption(boolean enable){
        for(int i=0; i<4; i++){
            optionContainer.getChildAt(i).setEnabled(enable);

            if(enable){
                optionContainer.getChildAt(i).setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#989898")));
            }
        }
    }

    //
    private void getFavorite(){
        String json = preferences.getString(KEY_NAME, "");

        Type type = new TypeToken<List<QuestionsModel>>(){}.getType();

        favoriteList = gson.fromJson(json, type);

        if(favoriteList == null){
            favoriteList = new ArrayList<>();
        }
    }

    private boolean modelMatch(){
        boolean matched = false;
        int i=0;
        for (QuestionsModel model:favoriteList){
            if(model.getQuestion().equals(list.get(position).getQuestion())
            && model.getCorrectANS().equals(list.get(position).getCorrectANS())
            && model.getSetNo() == list.get(position).getSetNo()){
                matched = true;
                matchedQuestionPosition = i;
            }
            i++;
        }
        return matched;
    }

    private void storeFavorite(){
        String json = gson.toJson(favoriteList);
        editor.putString(KEY_NAME,json);
        editor.commit();
    }
}
