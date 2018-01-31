package com.example.hardoon.spaceinvaders;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

// SpaceInvadersActivity is the entry point to the game.
// It will handle the lifecycle of the game by calling
// methods of spaceInvadersView when prompted to so by the OS.
public class SpaceInvadersActivity extends Activity {

    // spaceInvadersView will be the view of the game
    // It will also hold the logic of the game
    // and respond to screen touches as well
    SpaceInvadersView spaceInvadersView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the resolution into a Point object
        Point size = new Point();
        // Get a Display object to access screen details
        Display display = getWindowManager().getDefaultDisplay();
        display.getSize(size);
        final SharedPreferences sharedPref = getPreferences(MODE_PRIVATE);
        final FrameLayout game = new FrameLayout(this);
        final LinearLayout gameButtons = new LinearLayout(this);
        EditText levelEdit = new EditText(this);
        final int oldLevel = sharedPref.getInt("GameLevel", 1);
        levelEdit.setHint("Select level (1 - 9). Previous level = " + oldLevel);
        levelEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
        gameButtons.addView(levelEdit);
        // Initialize gameView and set it as the view
        levelEdit.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }
            // the user's changes are saved here
            public void onTextChanged(CharSequence c, int start, int before, int count) {
            }

            public void afterTextChanged(Editable c) {
                String s = c.toString();
                int level = Integer.parseInt(s);
                level = Math.min(9,Math.max(1, level));
                if(level != oldLevel){
                    SharedPreferences.Editor e = sharedPref.edit();
                    e.putInt("GameLevel",level);
                    e.commit();
                }
                //gameButtons.setVisibility(View.INVISIBLE);
                // Then just use the following:
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(gameButtons.getWindowToken(), 0);
                gameButtons.setVisibility(View.INVISIBLE);
                spaceInvadersView.startGame(level);

            }
        });
        spaceInvadersView = new SpaceInvadersView(this, size.x, size.y,sharedPref);
        game.addView(spaceInvadersView);
        game.addView(gameButtons);
        setContentView(game);
    }
    // This method executes when the player starts the game
    @Override
    protected void onResume() {
        super.onResume();

        // Tell the gameView resume method to execute
        spaceInvadersView.resume();
    }

    // This method executes when the player quits the game
    @Override
    protected void onPause() {
        super.onPause();

        // Tell the gameView pause method to execute
        spaceInvadersView.pause();
    }

}
