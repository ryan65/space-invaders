package com.example.hardoon.spaceinvaders;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.SoundPool;

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
    int movingSoundStreamId;
    int hitSoundID;
    private boolean isVisible;
    float saucerSpeed;
    SoundPool soundPool;
    RectF rect;
    float getX() {
        return x;
    }

    public FlyingSaucer(Context context, float x, float y, int screenX, int screenY, int gameLevel, int movingSoundID, int hitSoundID, SoundPool soundPool) {

        length = screenX / 20;
        height = screenY / 20;
        isVisible = true;
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
        saucerSpeed = 150 + Math.max(0, 10 * (gameLevel - 5));
        // This SoundPool is deprecated but don't worry
        this.soundPool = soundPool;
        this.hitSoundID = hitSoundID;
        movingSoundStreamId = soundPool.play(movingSoundID,0.4f,0.4f,1, -1,1);
        soundPool.stop(movingSoundID);

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

    public float getLength(){
        return length;
    }
    public void update(float fps){
        advanceTo(x + saucerSpeed/ fps);
    }
    public void advanceTo(float newX){
        x = newX;
        rect.offsetTo(newX,rect.top);
    }

    public boolean isVisible(){
        return isVisible;
    }

    public void kill(boolean wasHit){
        isVisible = false;
        if(wasHit){
            soundPool.play(hitSoundID, 0.3f, 0.3f,0, 0, 1 );
        }
        soundPool.stop(movingSoundStreamId);
    }

    public boolean checkIfHit(RectF bullet){
        if(!isVisible){
            return false;
        }
        if(RectF.intersects(bullet,rect)){
            kill(true);
            return true;
        }
        return false;
    }

}
