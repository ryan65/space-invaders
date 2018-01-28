package com.example.hardoon.spaceinvaders;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;

import java.util.Random;

/**
 * Created by hardoon on 1/23/2018.
 */

public class Invader {
    RectF rect;

    Random generator = new Random();

    // The player ship will be represented by a Bitmap
    private Bitmap bitmap1;
    private Bitmap bitmap2;

    // How long and high our invader will be
    private float length;
    private float height;

    // X is the far left of the rectangle which forms our invader
    private float x;

    // Y is the top coordinate
    private float y;

    private int gameLevel;
    // This will hold the pixels per second speedthat the invader will move
    private float invaderSpeed;

    public final int LEFT = 1;
    public final int RIGHT = 2;


    // Is the ship moving and in which direction
    private int invaderMoving = RIGHT;

    boolean isVisible;
    public Invader(Context context, int row, int column, int screenX, int screenY, int gameLevel) {

        // Initialize a blank RectF
        rect = new RectF();

        length = screenX / 20;
        height = screenY / 20;
        this.gameLevel = gameLevel;
        isVisible = true;

        int padding = screenX / 25;

        x = column * (length + padding);
        y = row * (length + padding/4);

        // Initialize the bitmap
        bitmap1 = BitmapFactory.decodeResource(context.getResources(), R.drawable.invader1);
        bitmap2 = BitmapFactory.decodeResource(context.getResources(), R.drawable.invader2);

        // stretch the first bitmap to a size appropriate for the screen resolution
        bitmap1 = Bitmap.createScaledBitmap(bitmap1,
                (int) (length),
                (int) (height),
                false);

        // stretch the first bitmap to a size appropriate for the screen resolution
        bitmap2 = Bitmap.createScaledBitmap(bitmap2,
                (int) (length),
                (int) (height),
                false);

        // How fast is the invader in pixels per second . after level 5 we we increase the speed
        invaderSpeed = 40 + Math.max(0, 8 * (5 - gameLevel));
    }
    public void setInvisible(){
        isVisible = false;
    }

    public boolean getVisibility(){
        return isVisible;
    }

    public RectF getRect(){
        return rect;
    }

    public Bitmap getBitmap(){
        return bitmap1;
    }

    public Bitmap getBitmap2(){
        return bitmap2;
    }

    public float getX(){
        return x;
    }

    public float getY(){
        return y;
    }

    public float getBottom(){
        return y + height;
    }

    public float getLength(){
        return length;
    }
    public void update(long fps){
        if(invaderMoving == LEFT){
            x = x - invaderSpeed / fps;
        }

        if(invaderMoving == RIGHT){
            x = x + invaderSpeed / fps;
        }

        // Update rect which is used to detect hits
        rect.top = y;
        rect.bottom = y + height;
        rect.left = x;
        rect.right = x + length;

    }
    public void dropDownAndReverse(){
        if(invaderMoving == LEFT){
            invaderMoving = RIGHT;
        }else{
            invaderMoving = LEFT;
        }

        y = y + height;

        invaderSpeed = invaderSpeed * 1.18f;
    }
    public boolean takeAim(float playerShipX, float playerShipLength){

        int randomNumber = -1;

        // If near the player
        if((playerShipX + playerShipLength > x &&
                playerShipX + playerShipLength < x + length) || (playerShipX > x && playerShipX < x + length)) {

            int shootChance = (int)Math.round(10 + 30 * (9 - gameLevel));
            randomNumber = generator.nextInt(shootChance);
            if(randomNumber == 0) {
                return true;
            }

        }

        // If firing randomly (not near the player) a 1 in 5000 chance
        int notNearPlayer = (int)Math.round(200 + 900 * (9 - gameLevel));
        randomNumber = generator.nextInt(notNearPlayer);
        if(randomNumber == 0){
            return true;
        }

        return false;
    }
}
