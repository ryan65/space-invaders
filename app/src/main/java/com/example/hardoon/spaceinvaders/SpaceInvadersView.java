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
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by hardoon on 1/23/2018.
 */


public class SpaceInvadersView extends SurfaceView implements Runnable {

    class SaucerTimerTask extends TimerTask {
        @Override
        public void run() {
            SpaceInvadersView.this.startFlyingSaucer();
        }
    }

    class StartSpecialInvaderTimerTask extends TimerTask {
        @Override
        public void run() {
            SpaceInvadersView.this.startSpecialInvader(false);
        }
    }

    class StopSpecialInvaderTimerTask extends TimerTask {
        @Override
        public void run() {
            SpaceInvadersView.this.stopSpecialInvader();
        }
    }

    Context context;


    private static final float DEFAULT_VOLUME = (float) 0.3;
    //max number of bullets that can be shot together
    private static final int NUM_OF_SHIP_BULLETS = 2;
    // This is our thread
    private Thread gameThread = null;

    private static final int INITIAL_NUM_OF_INVADERS_ROWS = 5;
    private static final int INITIAL_NUM_OF_INVADERS_COLUMNS = 6;
    private static final int INITIAL_NUM_OF_INVADERS = INITIAL_NUM_OF_INVADERS_ROWS * INITIAL_NUM_OF_INVADERS_COLUMNS;
    private static final int NUM_OF_LIVES = 3;
    private static final int FLYING_SAUCER_TIMEOUT = 10000;
    private static final int SPECIAL_INVADER_TIMEOUT = 12000;
    // The max number of bullets currently shot
    private final int MAX_INVADER_BULLETS = 10;

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

    private Invader selectedSpecialInvader = null;
    // The players ship
    private PlayerShip playerShip;

    // The player's bullet
    private Bullet[] shipBullets = new Bullet[NUM_OF_SHIP_BULLETS];

    private Bullet[] invadersBullets = new Bullet[MAX_INVADER_BULLETS];
    private int nextBullet;

    ArrayList<Invader> invaders;
    float invaderTouchDownPos;

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
    private int flyingSaucerHitSoundID = -1;
    private int flyingSaucerMoveSoundID = -1;
    private int gameLevel = 0;


    private Timer saucerTimer;
    private Timer specialInvaderTimer;
    private FlyingSaucer flyingSaucer = null;

    // The score
    int score = 0;

    // Lives
    private int lives;

    // How menacing should the sound be?
    private long menaceInterval = 1000;
    // Which menace sound should play next
    private boolean uhOrOh;
    // When did we last play a menacing sound
    private long lastMenaceTime = System.currentTimeMillis();
    private long highScore = 0;
    private long fastestTime = 0;
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
        soundPool = new SoundPool(15, AudioManager.STREAM_MUSIC, 0);

        try {
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

            descriptor = assetManager.openFd("victorySound.mp3");
            victorySoundID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("loosingSound.mp3");
            loosingSoundID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("ufo_hit.wav");
            flyingSaucerHitSoundID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("ufo_moving.wav");
            flyingSaucerMoveSoundID = soundPool.load(descriptor, 0);

        } catch (IOException e) {
            // Print an error message to the console
            Log.e("error", "failed to load sound files");
        }

        saucerTimer = new Timer();
        prepareNewGame(false);

    }

    private Boolean haveWeStartedToPlay() {
        return gameLevel > 0;
    }

    private void playSound(int id) {
        soundPool.play(id, DEFAULT_VOLUME, DEFAULT_VOLUME, 0, 0, 1);
    }

    private void playSoundLoud(int id) {
        soundPool.play(id, 0.90f, 0.90f, 5, 0, 1);
    }

    private void prepareNewGame(boolean started) {

        score = 0;
        lives = NUM_OF_LIVES;
        // Here we will initialize all the game objects
        // Reset the menace level
        menaceInterval = 1000;

        invaders = new ArrayList<>(INITIAL_NUM_OF_INVADERS);
        // Make a new player space ship
        playerShip = new PlayerShip(context, screenX, screenY, gameLevel);
        if (started) {
            relaunchFlyingSaucer();
            specialInvaderTimer = new Timer();
            startSpecialInvader(true);
        }

        // Initialize the shipBullets array
        for (int i = 0; i < shipBullets.length; i++) {
            shipBullets[i] = new Bullet(screenY);
        }

        // Initialize the invadersBullets array
        for (int i = 0; i < invadersBullets.length; i++) {
            invadersBullets[i] = new Bullet(screenY);
        }

        // Build an army of invaders
        int numInvaders = 0;
        for (int column = 0; column < INITIAL_NUM_OF_INVADERS_COLUMNS; column++) {
            for (int row = 0; row < INITIAL_NUM_OF_INVADERS_ROWS; row++) {
                invaders.add(new Invader(context, row, column, screenX, screenY, gameLevel));
                numInvaders++;
            }
        }
        // Build the shelters
        numBricks = 0;
        int brickHeight = screenY / 40;
        int shelterRowNum = gameLevel > 8 ? 4 : 5;
        int shelterColumnNumber = 10;
        int numOfShelters = 4;
        float shelterTop = getPlayerShipAreaTop() - brickHeight * shelterRowNum - 30;
        invaderTouchDownPos = shelterTop + 1;

        for (int shelterNumber = 0; shelterNumber < numOfShelters; shelterNumber++) {
            for (int column = 0; column < shelterColumnNumber; column++) {
                for (int row = 0; row < shelterRowNum; row++) {
                    bricks[numBricks] = new DefenceBrick(row, column, shelterNumber, screenX, screenY, shelterTop, brickHeight);
                    if (column % 4 >= 2 && row == 0) {
                        bricks[numBricks].setInvisible();
                    }
                    numBricks++;
                }
            }
        }
    }

    public void startGame(int level) {
        gameLevel = level;
        this.fastestTime = sharedPreferences.getLong("FastestTime" + gameLevel, 0);
        this.highScore = sharedPreferences.getLong("HighScore" + gameLevel, 0);

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
            if (!paused) {
                if ((startFrameTime - lastMenaceTime) > menaceInterval) {
                    playSound(uhOrOh ? uhID : ohID);
                    // Reset the last menace time
                    lastMenaceTime = System.currentTimeMillis();
                    // Alter value of uhOrOh
                    uhOrOh = !uhOrOh;
                }
            }
        }
    }

    private void checkIfFirstDownForNewGame() {
        if (paused) {
            startTime = System.currentTimeMillis();
            paused = false;
            prepareNewGame(true);
        }
    }

    private void relaunchFlyingSaucer() {
        killFlyingSaucer();
        saucerTimer.schedule(new SaucerTimerTask(), FLYING_SAUCER_TIMEOUT);
    }

    private void cleanupAndPause() {
        paused = true;
        killFlyingSaucer();
        if (specialInvaderTimer != null) {
            specialInvaderTimer.cancel();
            specialInvaderTimer = null;
        }
    }

    private void gameEnded(Boolean won) {
        gameTime = System.currentTimeMillis() - startTime;
        if (won) {
            if (gameTime < fastestTime || fastestTime == 0) {
                fastestTime = gameTime;
                SharedPreferences.Editor e = sharedPreferences.edit();
                e.putLong("FastestTime" + gameLevel, fastestTime);
                e.commit();
            }
        }
        if (score > highScore) {
            highScore = score;
            SharedPreferences.Editor e = sharedPreferences.edit();
            e.putLong("HighScore" + gameLevel, highScore);
            e.commit();

        }
        lastResultWon = won;
        playSoundLoud(won ? victorySoundID : loosingSoundID);
        cleanupAndPause();
    }

    private void killFlyingSaucer() {
        if (flyingSaucer != null) {
            flyingSaucer.kill();
        }
        flyingSaucer = null;
    }

    private void update() {

        // Did an invader bump into the side of the screen
        boolean bumped = false;

        // Has the player lost
        boolean lost = false;
        // Move the player's ship
        playerShip.update(fps);


        if (isFlyingSaucerVisible()) {
            if (flyingSaucer.getX() > screenX) {
                relaunchFlyingSaucer();
            } else {
                flyingSaucer.update(fps);
            }
        }

        // Update all the invaders if visible
        //we want to increase the shooting when we have less invaders
        float ratioVisibleInvaders = (float) invaders.size() / INITIAL_NUM_OF_INVADERS;
        for (int i = 0; i < invaders.size(); i++) {

            Invader currInvader = invaders.get(i);
            // Move the next invader
            currInvader.update(fps);
            // Does he want to take a shot?
            if (currInvader.takeAim(playerShip.getX(),
                    playerShip.getLength(), ratioVisibleInvaders)) {

                // If so try and spawn a bullet
                if (invadersBullets[nextBullet].shoot(currInvader.getX() + currInvader.getLength() / 2,
                        currInvader.getY(), Bullet.DOWN)) {
                    // Shot fired
                    // Prepare for the next shot
                    nextBullet = (nextBullet + 1) % MAX_INVADER_BULLETS;
                }
            }
            // If that move caused them to bump the screen change bumped to true
            if (currInvader.bumpedIntoScreen()) {
                bumped = true;
            }
        }
        // Update all the invaders bullets if active
        for (int i = 0; i < invadersBullets.length; i++) {
            if (invadersBullets[i].isActive()) {
                invadersBullets[i].update(fps);
            }
        }

        // Did an invader bump into the edge of the screen
        if (bumped) {
            // Move all the invaders down and change direction
            for (int i = 0; i < invaders.size(); i++) {
                invaders.get(i).dropDownAndReverse();
                if (invaders.get(i).getBottom() > invaderTouchDownPos) {
                    // Invader has landed
                    lost = true;
                }
            }
            // Increase the menace level
            // By making the sounds more frequent
            menaceInterval = menaceInterval - 80;
        }
        if (lost) {
            gameEnded(false);
            return;
        }

        for (int i = 0; i < shipBullets.length; i++) {
            Bullet currBullet = shipBullets[i];
            if (currBullet.isActive()) {
                currBullet.update(fps);
            }
            // Has the player's bullet hit the top of the screen
            if (currBullet.getImpactPointY() < 0) {
                currBullet.setInactive();
            }
            if (!currBullet.isActive()) {
                continue;
            }
            if (isFlyingSaucerVisible() && flyingSaucer.checkIfHit(currBullet.getRect())) {
                score += 100;
                currBullet.setInactive();
                playSound(flyingSaucerHitSoundID);
                relaunchFlyingSaucer();
                continue;
            }
            //check if bullet hit invader
            for (int j = 0; j < invaders.size(); j++) {
                if (invaders.get(j).checkIfHit(currBullet.getRect())) {
                    if (invaders.get(j).isSpecial()) {
                        lives++;
                    }
                    invaders.remove(j);
                    playSound(invaderExplodeID);
                    currBullet.setInactive();
                    score = score + 10;
                    // Has the player won
                    if (invaders.size() == 0) {
                        gameEnded(true);
                    }
                    break;
                }
            }
            // Has a player bullet hit a shelter brick
            for (int j = 0; j < numBricks; j++) {
                if (bricks[j].getVisibility()) {
                    if (RectF.intersects(currBullet.getRect(), bricks[j].getRect())) {
                        // A collision has occurred
                        currBullet.setInactive();
                        bricks[j].setInvisible();
                        playSound(damageShelterID);
                        break;
                    }
                }
            }
        }
        //
        for (int i = 0; i < invadersBullets.length; i++) {
            //Has an invaders bullet hit the bottom of the screen
            Bullet currBullet = invadersBullets[i];
            if (currBullet.getImpactPointY() > screenY) {
                currBullet.setInactive();
            }
            // Has an alien bullet hit a shelter brick
            if (currBullet.isActive()) {
                for (int j = 0; j < numBricks; j++) {
                    if (bricks[j].getVisibility()) {
                        if (RectF.intersects(currBullet.getRect(), bricks[j].getRect())) {
                            // A collision has occurred
                            currBullet.setInactive();
                            bricks[j].setInvisible();
                            playSound(damageShelterID);
                            break;
                        }
                    }
                }
            }
            if (currBullet.isActive()) {
                if (RectF.intersects(playerShip.getRect(), currBullet.getRect())) {
                    currBullet.setInactive();
                    lives--;
                    playSound(playerExplodeID);
                    // Is it game over?
                    if (lives == 0) {
                        gameEnded(false);
                    }
                }
            }
        }
    }

    private void startSpecialInvader(boolean first) {
        if (paused) {
            return;
        }
        if (!first) {
            Random generator = new Random();
            int res = generator.nextInt((int) Math.round(invaders.size()));
            for (int i = 0, invader = 0; i < invaders.size(); i++) {
                if (invader == res) {
                    res = i;
                    break;
                }
                invader++;
            }
            if (res < invaders.size()) {
                invaders.get(res).setSpecial(true);
                selectedSpecialInvader = invaders.get(res);
            }
        }
        specialInvaderTimer.schedule(new StopSpecialInvaderTimerTask(), 2000 + (10 - gameLevel) * 500);
    }

    private void stopSpecialInvader() {
        if (paused) {
            return;
        }
        if (selectedSpecialInvader != null) {
            selectedSpecialInvader.setSpecial(false);
            selectedSpecialInvader = null;
        }
        specialInvaderTimer.schedule(new StartSpecialInvaderTimerTask(), SPECIAL_INVADER_TIMEOUT);
    }

    private void startFlyingSaucer() {
        if (paused) {
            return;
        }
        killFlyingSaucer();
        flyingSaucer = new FlyingSaucer(context, 0, 5, screenX, screenY, gameLevel, flyingSaucerMoveSoundID, soundPool);
        flyingSaucer.advanceTo(-flyingSaucer.getLength());
    }

    private void draw() {
        // Make sure our drawing surface is valid or we crash
        if (ourHolder.getSurface().isValid()) {
            // Lock the canvas ready to draw
            canvas = ourHolder.lockCanvas();

            // Draw the background color
            canvas.drawColor(Color.argb(255, 26, 128, 182));

            int colorShipArea = Color.argb(255, 26, 128, 120);
            int colorShipAreaPressed = Color.argb(255, 26, 180, 120);
            // Draw ship area for moving ship
            paint.setColor(playerShip.getMovementState() == playerShip.LEFT ? colorShipAreaPressed : colorShipArea);
            canvas.drawRect(0, getPlayerShipAreaTop(), screenX / 2, screenY, paint);

            paint.setColor(playerShip.getMovementState() == playerShip.RIGHT ? colorShipAreaPressed : colorShipArea);
            canvas.drawRect(screenX / 2, getPlayerShipAreaTop(), screenX, screenY, paint);

            //border ship move left right
            paint.setColor(colorShipAreaPressed);
            paint.setStrokeWidth(8);
            canvas.drawLine(screenX / 2, getPlayerShipAreaTop(), screenX / 2, screenY, paint);
            float tipYBaselineLocation = screenY - (screenY - getPlayerShipAreaTop()) / 4;
            paint.setTextSize((screenY - getPlayerShipAreaTop()) / 2);
            String leftStr = "<< Move Left";
            String rightStr = "Move Right >>";
            float widthLeft = paint.measureText(leftStr);
            canvas.drawText(leftStr, (screenX / 2 - widthLeft) / 2, tipYBaselineLocation, paint);
            float widthRight = paint.measureText(rightStr);
            canvas.drawText("Move Right >>", screenX / 2 + (screenX / 2 - widthRight) / 2, tipYBaselineLocation, paint);


            // Choose the brush color for drawing
            paint.setColor(Color.argb(255, 255, 255, 255));

            // Draw the player spaceship
            // Now draw the player spaceship
            canvas.drawBitmap(playerShip.getBitmap(), playerShip.getX(), playerShip.getY(), paint);
            // Draw the invaders
            for (int i = 0; i < invaders.size(); i++) {
                Paint invaderPaint = paint;
                if (invaders.get(i).isSpecial()) {
                    invaderPaint = new Paint();
                    invaderPaint.setAlpha(65);
                }
                if (uhOrOh) {
                    canvas.drawBitmap(invaders.get(i).getBitmap(), invaders.get(i).getX(), invaders.get(i).getY(), invaderPaint);
                } else {
                    canvas.drawBitmap(invaders.get(i).getBitmap2(), invaders.get(i).getX(), invaders.get(i).getY(), invaderPaint);
                }
            }
            // Draw the bricks if visible
            for (int i = 0; i < numBricks; i++) {
                if (bricks[i].getVisibility()) {
                    canvas.drawRect(bricks[i].getRect(), paint);
                }
            }
            // Draw the players bullet if active
            for (int i = 0; i < shipBullets.length; i++) {
                Bullet currBullet = shipBullets[i];
                if (currBullet.isActive()) {
                    canvas.drawRect(currBullet.getRect(), paint);
                }
            }
            // Draw the invaders bullets if active
            // Update all the invader's bullets if active
            for (int i = 0; i < invadersBullets.length; i++) {
                if (invadersBullets[i].isActive()) {
                    canvas.drawRect(invadersBullets[i].getRect(), paint);
                }
            }
            // Draw the score and remaining lives
            // Change the brush color
            paint.setColor(Color.argb(255, 249, 129, 0));
            paint.setTextSize(60);
            canvas.drawText("score:" + score + " lives:" + lives + " t:" + (!paused ? Math.round((System.currentTimeMillis() - startTime) / 100) / 10.0 : Math.round(gameTime / 100) / 10.0) + " Best:" + Math.round(fastestTime / 100) / 10.0 + "/" + highScore + (haveWeStartedToPlay() ? " Level:" + gameLevel : ""), 10, 50, paint);

            if (isFlyingSaucerVisible()) {
                flyingSaucer.draw(canvas, paint);
            }

            if (paused && lastResultWon != null) {
                paint.setTextSize(100);
                paint.setColor((lastResultWon ? Color.GREEN : Color.RED));
                if (lastResultWon && gameLevel == 9) {
                    canvas.drawText("YOU'RE THE MASTER!!", 10, screenY / 2, paint);
                } else {
                    canvas.drawText("GAME OVER " + (lastResultWon ? "CHAMP" : "LOOSER!!!"), 10, screenY / 2, paint);
                }
            }

            // Draw everything to the screen
            ourHolder.unlockCanvasAndPost(canvas);
        }
    }

    public boolean isFlyingSaucerVisible() {
        return (flyingSaucer != null && flyingSaucer.isVisible());
    }

    // If SpaceInvadersActivity is paused/stopped
    // shutdown our thread.
    public void pause() {
        playing = false;
        cleanupAndPause();
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            Log.e("Error:", "joining thread");
        }

    }

    public void stop() {
        pause();
    }

    // If SpaceInvadersActivity is started then
    // start our thread.
    public void resume() {
        playing = true;
        paused = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    public boolean isShipMoveLocation(float y) {
        return y > getPlayerShipAreaTop();
    }

    public float getPlayerShipAreaTop() {
        return screenY - playerShip.getHeight();
    }


    // The SurfaceView class implements onTouchListener
    // So we can override this method and detect screen touches.
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!haveWeStartedToPlay()) {
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
                if (maskedAction == MotionEvent.ACTION_DOWN && isShipMoveLocation(yPoint)) {
                    if (motionEvent.getX() > screenX / 2) {
                        playerShip.setMovementState(playerShip.RIGHT);
                    } else {
                        playerShip.setMovementState(playerShip.LEFT);
                    }

                } else if (!isShipMoveLocation(yPoint)) { //pressed above ship , lets shoot bullets
                    // Shots fired
                    for (int i = 0; i < shipBullets.length; i++) {
                        Bullet currBullet = shipBullets[i];
                        //look for one bullet that can be shot and then break if shot.
                        if (currBullet.shoot(playerShip.getX() +
                                playerShip.getLength() / 2, getPlayerShipAreaTop(), currBullet.UP)) {

                            playSound(shootID);
                            break;
                        }

                    }
                }
                break;
            // Player has removed finger from screen
            case MotionEvent.ACTION_UP:
                if (isShipMoveLocation(motionEvent.getY())) {
                    playerShip.setMovementState(playerShip.STOPPED);
                }

                break;
        }
        return true;
    }
}
