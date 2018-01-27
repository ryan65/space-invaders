package com.example.hardoon.spaceinvaders;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.SoundPool;
import android.support.annotation.RequiresPermission;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * Created by hardoon on 1/23/2018.
 */


public class SpaceInvadersView  extends SurfaceView implements Runnable{
    Context context;

    private  static final float DEFAULT_VOLUME = (float)0.3;
    //max number of bullets that can be shot together
    private static final int NUM_OF_SHIP_BULLETS = 2;
    // This is our thread
    private Thread gameThread = null;

    // Our SurfaceHolder to lock the surface before we draw our graphics
    private SurfaceHolder ourHolder;

    // A boolean which we will set and unset
    // when the game is running- or not.
    private volatile boolean playing;

    // Game is paused at the start
    private boolean paused = true;

    // A Canvas and a Paint object
    private Canvas canvas;
    private Paint paint;

    // This variable tracks the game frame rate
    private long fps;

    private Boolean lastResultWon = null;
    // This is used to help calculate the fps
    private long timeThisFrame;

    // The size of the screen in pixels
    private int screenX;
    private int screenY;

    // The players ship
    private PlayerShip playerShip;

    // The player's bullet
    private Bullet[] shipBullets = new Bullet[NUM_OF_SHIP_BULLETS];

    // The invaders bullets
    private Bullet[] invadersBullets = new Bullet[200];
    private int nextBullet;
    private int maxInvaderBullets = 10;

    // Up to 60 invaders
    Invader[] invaders = new Invader[60];
    int numInvaders = 0;

    // The player's shelters are built from bricks
    private DefenceBrick[] bricks = new DefenceBrick[400];
    private int numBricks;

    // For sound FX
    private SoundPool soundPool;
    private int playerExplodeID = -1;
    private int invaderExplodeID = -1;
    private int shootID = -1;
    private int damageShelterID = -1;
    private int uhID = -1;
    private int ohID = -1;
    private int victorySoundID = -1;
    private int loosingSoundID = -1;
    private int gameLevel = 0;

    // The score
    int score = 0;

    // Lives
    private int lives = 3;

    // How menacing should the sound be?
    private long menaceInterval = 1000;
    // Which menace sound should play next
    private boolean uhOrOh;
    // When did we last play a menacing sound
    private long lastMenaceTime = System.currentTimeMillis();
    private long highScore = 0;
    private SharedPreferences sharedPreferences;
    private long startTime = 0;
    private long gameTime = 0;

    // When the we initialize (call new()) on gameView
// This special constructor method runs
    public SpaceInvadersView(Context context, int x, int y, SharedPreferences sP) {


        // The next line of code asks the
        // SurfaceView class to set up our object.
        // How kind.
        super(context);
        sharedPreferences = sP;
        // Make a globally available copy of the context so we can use it in another method
        this.context = context;

        // Initialize ourHolder and paint objects
        ourHolder = getHolder();
        paint = new Paint();

        screenX = x;
        screenY = y;

        // This SoundPool is deprecated but don't worry
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC,0);

        try{
            // Create objects of the 2 required classes
            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor descriptor;

            // Load our fx in memory ready for use
            descriptor = assetManager.openFd("shoot.ogg");
            shootID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("invaderexplode.ogg");
            invaderExplodeID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("damageshelter.ogg");
            damageShelterID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("playerexplode.ogg");
            playerExplodeID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("damageshelter.ogg");
            damageShelterID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("uh.ogg");
            uhID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("oh.ogg");
            ohID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("victorySound.ogg");
            victorySoundID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("loosingSound.ogg");
            loosingSoundID = soundPool.load(descriptor, 0);

        }catch(IOException e){
            // Print an error message to the console
            Log.e("error", "failed to load sound files");
        }
        prepareLevel();
    }

    private Boolean haveWeStartedToPlay (){
        return gameLevel > 0;
    }

    private void playSound(int id) {
        soundPool.play(id, DEFAULT_VOLUME, DEFAULT_VOLUME, 0, 0, 1);
    }
    private void playSoundLoud(int id) {
        soundPool.play(id, 0.99f,0.99f, 5, 0, 1);
    }
    private void prepareLevel(){

        score = 0;
        lives = 3;
        // Here we will initialize all the game objects
        // Reset the menace level
        menaceInterval = 1000;
        // Make a new player space ship
        playerShip = new PlayerShip(context, screenX, screenY);

        // Initialize the shipBullets array
        for(int i = 0; i < shipBullets.length; i++){
            shipBullets[i] = new Bullet(screenY);
        }

        // Initialize the invadersBullets array
        for(int i = 0; i < invadersBullets.length; i++){
            invadersBullets[i] = new Bullet(screenY);
        }

        // Build an army of invaders
        numInvaders = 0;
        for(int column = 0; column < 6; column ++ ){
            for(int row = 0; row < 5; row ++ ){
                invaders[numInvaders] = new Invader(context, row, column, screenX, screenY,gameLevel);
                numInvaders ++;
            }
        }
// Build the shelters
        numBricks = 0;
        for(int shelterNumber = 0; shelterNumber < 4; shelterNumber++){
            for(int column = 0; column < 10; column ++ ) {
                for (int row = 0; row < 5; row++) {
                    bricks[numBricks] = new DefenceBrick(row, column, shelterNumber, screenX, screenY);
                    numBricks++;
                }
            }
        }
    }
    public void startGame(int level){
        gameLevel = level;
        this.highScore = sharedPreferences.getLong("FastestTime"+gameLevel, 0);
    }

    @Override
    public void run() {
        while (playing) {

            // Capture the current time in milliseconds in startFrameTime
            long startFrameTime = System.currentTimeMillis();

            // Update the frame
            if (!paused) {
                update();
            }

            // Draw the frame
            draw();

            // Calculate the fps this frame
            // We can then use the result to
            // time animations and more.
            timeThisFrame = System.currentTimeMillis() - startFrameTime;
            if (timeThisFrame >= 1) {
                fps = 1000 / timeThisFrame;
            }

            // We will do something new here towards the end of the project
            // Play a sound based on the menace level
            if(!paused) {
                if ((startFrameTime - lastMenaceTime) > menaceInterval) {
                    playSound(uhOrOh ? uhID: ohID);
                    // Reset the last menace time
                    lastMenaceTime = System.currentTimeMillis();
                    // Alter value of uhOrOh
                    uhOrOh = !uhOrOh;
                }
            }
        }
    }

    private void checkIfFirstDownForNewGame(){
        if(paused){
            startTime = System.currentTimeMillis();
            paused = false;
            soundPool.stop(victorySoundID);
            soundPool.stop(loosingSoundID);
            prepareLevel();
        }
    }
    private void gameEnded(Boolean won){
        gameTime = System.currentTimeMillis() - startTime;
        if(won){
            if(gameTime < highScore || highScore == 0){
                highScore = gameTime;
                SharedPreferences.Editor e = sharedPreferences.edit();
                e.putLong("FastestTime"+gameLevel,highScore);
                e.commit();
            }
        }
        lastResultWon = won;
        playSoundLoud(won ? victorySoundID : loosingSoundID);
        paused = true;
    }
    private void update(){

        // Did an invader bump into the side of the screen
        boolean bumped = false;

        // Has the player lost
        boolean lost = false;
        // Move the player's ship
        playerShip.update(fps);

        // Update all the invaders if visible
        for(int i = 0; i < numInvaders; i++){

            if(invaders[i].getVisibility()) {
                // Move the next invader
                invaders[i].update(fps);

                // Does he want to take a shot?
                if(invaders[i].takeAim(playerShip.getX(),
                        playerShip.getLength())){

                    // If so try and spawn a bullet
                    if(invadersBullets[nextBullet].shoot(invaders[i].getX()
                                    + invaders[i].getLength() / 2,
                            invaders[i].getY(), Bullet.DOWN)) {

                        // Shot fired
                        // Prepare for the next shot
                        nextBullet++;

                        // Loop back to the first one if we have reached the last
                        if (nextBullet == maxInvaderBullets) {
                            // This stops the firing of another bullet until one completes its journey
                            // Because if bullet 0 is still active shoot returns false.
                            nextBullet = 0;
                        }
                    }
                }

                // If that move caused them to bump the screen change bumped to true
                if (invaders[i].getX() > screenX - invaders[i].getLength()
                        || invaders[i].getX() < 0){

                    bumped = true;

                }
            }
        }
        // Update all the invaders bullets if active
        for(int i = 0; i < invadersBullets.length; i++){
            if(invadersBullets[i].getStatus()) {
                invadersBullets[i].update(fps);
            }
        }

        // Did an invader bump into the edge of the screen
        if(bumped){

            // Move all the invaders down and change direction
            for(int i = 0; i < numInvaders; i++){
                invaders[i].dropDownAndReverse();
                // Have the invaders landed
                if(invaders[i].getY() > screenY - screenY / 10){
                    lost = true;
                }
            }

            // Increase the menace level
            // By making the sounds more frequent
            menaceInterval = menaceInterval - 80;
        }
        if(lost){
            gameEnded(false);
        }

        for(int i = 0; i < shipBullets.length; i++){
            Bullet currBullet = shipBullets[i];
            if(currBullet.getStatus()) {
                currBullet.update(fps);
            }
            // Has the player's bullet hit the top of the screen
            if(currBullet.getImpactPointY() < 0){
                currBullet.setInactive();
            }
            // Has the player's bullet hit an invader
            if(currBullet.getStatus()) {
                for (int j = 0; j < numInvaders; j++) {
                    if (invaders[j].getVisibility()) {
                        if (RectF.intersects(currBullet.getRect(), invaders[j].getRect())) {
                            invaders[j].setInvisible();
                            playSound(invaderExplodeID);
                            currBullet.setInactive();
                            score = score + 10;

                            // Has the player won
                             if(score == numInvaders * 10){
                                gameEnded(true);
                            }
                        }
                    }
                }
            }
            // Has a player bullet hit a shelter brick
            if(currBullet.getStatus()){
                for(int j = 0; j < numBricks; j++){
                    if(bricks[j].getVisibility()){
                        if(RectF.intersects(currBullet.getRect(), bricks[j].getRect())){
                            // A collision has occurred
                            currBullet.setInactive();
                            bricks[j].setInvisible();
                            playSound(damageShelterID);
                        }
                    }
                }
            }
        }
        //
        for(int i = 0; i < invadersBullets.length; i++){
            //Has an invaders bullet hit the bottom of the screen
            Bullet currBullet = invadersBullets[i];
            if(currBullet.getImpactPointY() > screenY){
                currBullet.setInactive();
            }
            // Has an alien bullet hit a shelter brick
            if(currBullet.getStatus()){
                for(int j = 0; j < numBricks; j++){
                    if(bricks[j].getVisibility()){
                        if(RectF.intersects(currBullet.getRect(), bricks[j].getRect())){
                            // A collision has occurred
                            currBullet.setInactive();
                            bricks[j].setInvisible();
                            playSound(damageShelterID);
                            break;
                        }
                    }
                }
            }
            if(currBullet.getStatus()){
                if(RectF.intersects(playerShip.getRect(), currBullet.getRect())){
                    currBullet.setInactive();
                    lives --;
                    playSound(playerExplodeID);
                    // Is it game over?
                    if(lives == 0){
                        gameEnded(false);
                    }
                }
            }
        }
    }

    private void draw(){
        // Make sure our drawing surface is valid or we crash
        if (ourHolder.getSurface().isValid()) {
            // Lock the canvas ready to draw
            canvas = ourHolder.lockCanvas();

            // Draw the background color
            canvas.drawColor(Color.argb(255, 26, 128, 182));

            // Choose the brush color for drawing
            paint.setColor(Color.argb(255,  255, 255, 255));

            // Draw the player spaceship
            // Now draw the player spaceship
            canvas.drawBitmap(playerShip.getBitmap(), playerShip.getX(), playerShip.getY(), paint);
            // Draw the invaders
            for(int i = 0; i < numInvaders; i++){
                if(invaders[i].getVisibility()) {
                    if(uhOrOh) {
                        canvas.drawBitmap(invaders[i].getBitmap(), invaders[i].getX(), invaders[i].getY(), paint);
                    }else{
                        canvas.drawBitmap(invaders[i].getBitmap2(), invaders[i].getX(), invaders[i].getY(), paint);
                    }
                }
            }
            // Draw the bricks if visible
            for(int i = 0; i < numBricks; i++){
                if(bricks[i].getVisibility()) {
                    canvas.drawRect(bricks[i].getRect(), paint);
                }
            }
            // Draw the players bullet if active
            for(int i = 0; i < shipBullets.length; i++){
                Bullet currBullet = shipBullets[i];
                if(currBullet.getStatus()){
                    canvas.drawRect(currBullet.getRect(), paint);
                }
            }
            // Draw the invaders bullets if active
            // Update all the invader's bullets if active
            for(int i = 0; i < invadersBullets.length; i++){
                if(invadersBullets[i].getStatus()) {
                    canvas.drawRect(invadersBullets[i].getRect(), paint);
                }
            }
            // Draw the score and remaining lives
            // Change the brush color
            paint.setColor(Color.argb(255,  249, 129, 0));
            paint.setTextSize(60);
            canvas.drawText("Lives:" + lives + " Time:" + (!paused ? Math.round((System.currentTimeMillis() - startTime)/100)/10.0 : Math.round(gameTime/100)/10.0) + "  Best Time:" + Math.round(highScore/100)/10.0 + (haveWeStartedToPlay() ? " Level:" + gameLevel : ""), 10,50, paint);

            if(paused && lastResultWon != null){
                paint.setTextSize(100);
                paint.setColor((lastResultWon ? Color.GREEN : Color.BLACK));
                canvas.drawText("GAME OVER " + (lastResultWon ? "CHAMP" : "LOOSER"), 10,screenY/2, paint);
            }

            // Draw everything to the screen
            ourHolder.unlockCanvasAndPost(canvas);
        }
    }

    // If SpaceInvadersActivity is paused/stopped
    // shutdown our thread.
    public void pause() {
        playing = false;
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            Log.e("Error:", "joining thread");
        }

    }

    // If SpaceInvadersActivity is started then
    // start our thread.
    public void resume() {
        playing = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    public boolean isShipMoveLocation(float y){
        return y > screenY - playerShip.getHeight();
    }
    // The SurfaceView class implements onTouchListener
    // So we can override this method and detect screen touches.
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if(!haveWeStartedToPlay()){
            return true;
        }
        float yPoint;
        int maskedAction = motionEvent.getActionMasked();
        switch (maskedAction) {
            // Player has touched the screen
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                checkIfFirstDownForNewGame();
                yPoint = motionEvent.getY(motionEvent.getActionIndex());
                if(maskedAction == MotionEvent.ACTION_DOWN && isShipMoveLocation(yPoint)) {
                    if (motionEvent.getX() > screenX / 2) {
                        playerShip.setMovementState(playerShip.RIGHT);
                    } else {
                        playerShip.setMovementState(playerShip.LEFT);
                    }

                }
                else if(!isShipMoveLocation(yPoint)){ //pressed above ship , lets shoot bullets
                    // Shots fired
                    for(int i = 0; i <shipBullets.length ; i++){
                        Bullet currBullet = shipBullets[i];
                        //look for one bullet that can be shot and then break if shot.
                        if(currBullet.shoot(playerShip.getX()+
                                playerShip.getLength()/2,screenY - playerShip.getHeight(),currBullet.UP)){

                            playSound(shootID);
                            break;
                        }

                    }
                }
                break;
            // Player has removed finger from screen
            case MotionEvent.ACTION_UP:
                if(isShipMoveLocation(motionEvent.getY())) {
                    playerShip.setMovementState(playerShip.STOPPED);
                }

                break;
        }
        return true;
    }
}
