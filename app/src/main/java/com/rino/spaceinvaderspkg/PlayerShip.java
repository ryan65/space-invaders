package com.rino.spaceinvaderspkg;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;

/**
 * Created by hardoon on 1/23/2018.
 */

public class PlayerShip {
    RectF rect;

    // The player ship will be represented by a Bitmap
    private Bitmap bitmap;

    // How long and high our ship will be
    private float length;
    private float height;

    // X is the far left of the rectangle which forms our ship
    private float x;

    // Y is the top coordinate
    private float y;

    // This will hold the pixels per second speed that the ship will move
    private float shipSpeed;

    // Which ways can the ship move
    public final int STOPPED = 0;
    public final int LEFT = 1;
    public final int RIGHT = 2;

    // Is the ship moving and in which direction
    private int shipMoving = STOPPED;
    private int _screenX;

    // This the the constructor method
    // When we create an object from this class we will pass
    // in the screen width and height
    public PlayerShip(Context context, int screenX, int screenY,int gameLevel){

        _screenX = screenX;
        // Initialize a blank RectF
        rect = new RectF();

        length = screenX/10;
        height = screenY/10;

        // Start ship in roughly the screen centre
        x = (screenX - length)/2;
        y = screenY - height - 5;
        // Initialize the bitmap
        bitmap = BitmapFactory.decodeResource(context.getResources(), gameLevel == 9 ? R.drawable.playership9 : R.drawable.playership );

        // stretch the bitmap to a size appropriate for the screen resolution
        bitmap = Bitmap.createScaledBitmap(bitmap,
                (int) (length),
                (int) (height),
                false);

        // How fast is the spaceship in pixels per second
        shipSpeed = 350;
    }
    public RectF getRect(){
        return rect;
    }

    // This is a getter method to make the rectangle that
    // defines our ship available in SpaceInvadersView class
    public Bitmap getBitmap(){
        return bitmap;
    }

    public float getX(){
        return x;
    }
    public float getY(){
        return y;
    }

    public float getLength(){
        return length;
    }

    public float getHeight(){
        return height;
    }

    // This method will be used to change/set if the ship is going left, right or nowhere
    public void setMovementState(int state){
        shipMoving = state;
    }

    public int getMovementState(){
        return shipMoving ;
    }

    // This update method will be called from update in SpaceInvadersView
    // It determines if the player ship needs to move and changes the coordinates
    // contained in x if necessary
    public void update(long fps){
        if(shipMoving == LEFT){
            x = x - shipSpeed / fps;
            x = Math.max(x,-length/2);
        }

        if(shipMoving == RIGHT){
            x = x + shipSpeed / fps;
            x = Math.min(x,_screenX - length/2);
        }

        // Update rect which is used to detect hits
        rect.top = y;
        rect.bottom = y + height;
        rect.left = x;
        rect.right = x + length;

    }
}
