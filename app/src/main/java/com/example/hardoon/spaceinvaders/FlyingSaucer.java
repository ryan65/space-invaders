package com.example.hardoon.spaceinvaders;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import java.util.Random;
import java.util.TimerTask;

/**
 * Created by hardoon on 1/23/2018.
 */

public class FlyingSaucer{

    private Bitmap bitmap1;
    private Bitmap bitmap2;
    private float length;
    private float height;

    private float x;

    private float y;

    boolean isVisible;
    float saucerSpeed;

    RectF rect;
    float getX() {
        return x;
    }

    public FlyingSaucer(Context context, int x, int y, int screenX, int screenY, int gameLevel) {

        length = screenX / 20;
        height = screenY / 20;
        isVisible = true;
        int padding = screenX / 25;

        this.x = x;
        this.y = y;
        rect = new RectF(x,y,x+length,y + height);

        // Initialize the bitmap
        bitmap1 = BitmapFactory.decodeResource(context.getResources(), R.drawable.flyingsaucer1);
        bitmap2 = BitmapFactory.decodeResource(context.getResources(), R.drawable.flyingsaucer2);

        // stretch the first bitmap to a size appropriate for the screen resolution
        bitmap1 = Bitmap.createScaledBitmap(bitmap1,
                (int) (length),
                (int) (height),
                false);
        bitmap2 = Bitmap.createScaledBitmap(bitmap2,
                (int) (length),
                (int) (height),
                false);

        // How fast is the invader in pixels per second . after level 5 we we increase the speed
        saucerSpeed = 130 + Math.max(0, 8 * (gameLevel - 5));
    }
    public void setInvisible(){
        isVisible = false;
    }

    public boolean getVisibility(){
        return isVisible;
    }


    public void draw(Canvas canvas, Paint paint){
        if(!isVisible){
            return;
        }
        Bitmap b = bitmap1;
        if(System.currentTimeMillis() % 1000 > 500){
            b = bitmap2;
        }
        canvas.drawBitmap(b, x, y, paint);
    }

    public void update(float fps){
        x = x + saucerSpeed/ fps;
        rect.offsetTo(x,rect.top);
    }
    public boolean checkIfHit(RectF bullet){
        if(!isVisible){
            return false;
        }
        if(RectF.intersects(bullet,rect)){
            isVisible = false;
            return true;
        }
        return false;
    }

}
