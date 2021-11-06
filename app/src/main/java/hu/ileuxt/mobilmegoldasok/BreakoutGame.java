package hu.ileuxt.mobilmegoldasok;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.IOException;
import java.util.Objects;

public class BreakoutGame extends Activity {
    BreakoutView breakoutView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        breakoutView = new BreakoutView(this);
        setContentView(breakoutView);
    }

    class BreakoutView extends SurfaceView implements Runnable {
        Thread gameThread = null;
        SurfaceHolder ourHolder;
        Canvas canvas;
        Paint paint;
        Paddle paddle;
        Ball ball;
        Brick[] bricks = new Brick[200];
        SoundPool soundPool;
        MediaPlayer m;
        Typeface customTypeface;
        volatile boolean playing;
        boolean paused = true;
        long fps;
        int screenX, screenY;
        int numBricks, score = 0;
        int lives = 3;
        int beep1ID, beep2ID, beep3ID, loseLifeID, explodeID, gameOverID, victoryID = -1;

        public BreakoutView(Context context) {
            super(context);

            ourHolder = getHolder();
            paint = new Paint();

            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            screenX = size.x;
            screenY = size.y;

            paddle = new Paddle(screenX, screenY);
            ball = new Ball();
            m = new MediaPlayer();
            customTypeface = context.getResources().getFont(R.font.font);

            soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC,0);
            try{
                AssetManager assetManager = context.getAssets();
                AssetFileDescriptor descriptor;
                descriptor = assetManager.openFd("beep1.ogg");
                beep1ID = soundPool.load(descriptor, 0);
                descriptor = assetManager.openFd("beep2.ogg");
                beep2ID = soundPool.load(descriptor, 0);
                descriptor = assetManager.openFd("beep3.ogg");
                beep3ID = soundPool.load(descriptor, 0);
                descriptor = assetManager.openFd("loseLife.ogg");
                loseLifeID = soundPool.load(descriptor, 0);
                descriptor = assetManager.openFd("explode.ogg");
                explodeID = soundPool.load(descriptor, 0);
                descriptor = assetManager.openFd("gameover.ogg");
                gameOverID = soundPool.load(descriptor, 0);
                descriptor = assetManager.openFd("victory.ogg");
                victoryID = soundPool.load(descriptor, 0);
                descriptor.close();
            }catch(IOException e){
                Log.e("error", "failed to load sound files");
            }

            createBricksAndRestart();
        }

        public void createBricksAndRestart() {
            ball.reset(screenX, screenY);
            int brickWidth = screenX / 5;
            int brickHeight = screenY / 24;
            numBricks = 0;

            for (int column = 0; column < 5; column++) {
                for (int row = 0; row < 8; row++) {
                    bricks[numBricks] = new Brick(row, column, brickWidth, brickHeight);
                    numBricks++;
                }
            }
            if (lives == 0) {
                score = 0;
                lives = 3;
            }
            if (score > 0){
                score = 0;
                lives = 3;
            }
            if (!playing) playing = true;
            if (!paused) paused = true;
        }

        @Override
        public void run() {
            while (playing) {
                long startFrameTime = System.currentTimeMillis();
                if (!paused) {
                    update();
                    playSong();
                }
                draw();
                long timeThisFrame = System.currentTimeMillis() - startFrameTime;
                if (timeThisFrame >= 1) {
                    fps = 600 / timeThisFrame;
                }
            }
        }

        public void playSong() {
            try {
                if (m.isPlaying()) return;
                AssetFileDescriptor afd = getAssets().openFd("bg.ogg");
                m.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
                m.prepare();
                m.setVolume(1f, 1f);
                m.setLooping(true);
                m.start();
                afd.close();
            }
            catch (Exception e) {
                Log.e("IOEX", Objects.requireNonNull(e.getMessage()));
            }
        }

        public void update() {
            paddle.update(fps, screenX);
            ball.update(fps);

            // ball hits brick
            for (int i = 0; i < numBricks; i++) {
                if (bricks[i].getVisibility()) {
                    if (RectF.intersects(bricks[i].getRect(), ball.getRect())) {
                        bricks[i].setInvisible();
                        ball.reverseYVelocity();
                        score = score + 10;
                        soundPool.play(explodeID, 1, 1, 0, 0, 1);
                    }
                }
            }

            // ball hits paddle
            if (ball.getRect().intersects(paddle.getRect().left,paddle.getRect().top,paddle.getRect().right,paddle.getRect().bottom+ball.ballHeight)) {
                ball.setRandomXVelocity();
                ball.reverseYVelocity();
                ball.clearObstacleY(paddle.getRect().top);
                soundPool.play(beep1ID, 1, 1, 0, 0, 1);
            }

            // ball hits bottom
            if (ball.getRect().bottom > screenY) {
                ball.reverseYVelocity();
                ball.clearObstacleY(screenY);
                lives--;
                soundPool.play(loseLifeID, 1, 1, 0, 0, 1);
            }

            // ball hits top
            if (ball.getRect().top < 0)
            {
                ball.reverseYVelocity();
                ball.clearObstacleY(ball.ballHeight);
                soundPool.play(beep2ID, 1, 1, 0, 0, 1);
            }

            // ball hits left
            if (ball.getRect().left < 0)
            {
                ball.reverseXVelocity();
                ball.clearObstacleX(0);
                soundPool.play(beep3ID, 1, 1, 0, 0, 1);
            }

            // ball hits right
            if (ball.getRect().right > screenX) {
                ball.reverseXVelocity();
                ball.clearObstacleX(screenX-ball.ballHeight);
                soundPool.play(beep3ID, 1, 1, 0, 0, 1);
            }

            //game end conditions check
            if (lives == 0) {
                playing = false;
                paused = true;
                draw();
                if (m.isPlaying()) m.pause();
                m.reset();
                soundPool.play(gameOverID, 1, 1, 0, 0, 1);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                createBricksAndRestart();
            }
            if (score >= numBricks*10)
            {
                playing = false;
                paused = true;
                draw();
                if (m.isPlaying()) m.pause();
                m.reset();
                soundPool.play(victoryID, 1, 1, 0, 0, 1);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                createBricksAndRestart();
            }
        }

        public void draw() {
            // CHECK IF SURFACE IS VALID OR APP CRASHES
            if (ourHolder.getSurface().isValid()) {
                canvas = ourHolder.lockCanvas();

                //draw background
                //canvas.drawColor(Color.argb(255, 25, 50, 50));
                Paint p = new Paint();
                p.setDither(true);
                p.setShader(new LinearGradient(0, 0, 0, screenY, Color.argb(255,0,30,80),
                        Color.argb(255,20,20,20), Shader.TileMode.CLAMP));
                canvas.drawPaint(p);

                //draw paddle
                paint.setColor(Color.argb(255, 200, 200, 200));
                canvas.drawRoundRect(paddle.getRect(),10,10, paint);

                //draw ball
                paint.setColor(Color.argb(255, 255, 255, 255));
                canvas.drawRoundRect(ball.getRect(),20,20, paint);

                //draw bricks
                Paint paint1 = new Paint();
                paint1.setDither(true);
                int [] colors = {
                        Color.rgb(255,100,150),
                        Color.rgb(200,40,30),
                        Color.rgb(230,100,40),
                        Color.rgb(255,200,0),
                        Color.rgb(90,200,0),
                        Color.rgb(30,50,200),
                        Color.rgb(130,50,230),
                        Color.rgb(120,240,240)};
                int [] colors2 = {
                        Color.rgb(200,50,100),
                        Color.rgb(150,10,10),
                        Color.rgb(180,50,10),
                        Color.rgb(200,150,0),
                        Color.rgb(40,150,0),
                        Color.rgb(10,10,150),
                        Color.rgb(80,10,180),
                        Color.rgb(70,190,190)};
                int j = 0;
                for (int i = 0; i < numBricks; i++) {
                        paint1.setShader(new LinearGradient(0,0,0,screenX/(float)24,colors2[j],colors[j],Shader.TileMode.MIRROR));
                        if (bricks[i].getVisibility()) {
                            canvas.drawRoundRect(bricks[i].getRect(),8, 8, paint1);
                        }
                        j++;
                        if (j == 8) j = 0;
                }

                paint.setColor(Color.argb(255, 255, 255, 255));
                paint.setTextSize(50);
                paint.setTypeface(customTypeface);
                paint.setTextAlign(Paint.Align.CENTER);
                int xPos = (canvas.getWidth() / 2);
                int yPos = (int) ((canvas.getHeight() / 2) - ((paint.descent() + paint.ascent())/2));
                //draw score and lives
                canvas.drawText("Score: " + score + "   Lives: " + lives, xPos, 60, paint);

                paint.setTextSize(70);
                //game end conditions check
                if (score >= numBricks*10) {
                    canvas.drawText("You win", xPos, yPos, paint);
                }
                if (lives <= 0) {
                    canvas.drawText("Game over", xPos, yPos, paint);
                }
                if (paused && lives != 0 && score < numBricks*10){
                    canvas.drawText("Touch to start", xPos,yPos,paint);
                }
                ourHolder.unlockCanvasAndPost(canvas);
            }
        }

        // if paused/stopped shutdown thread
        public void pause() {
            playing = false;
            paused = true;
            if (m.isPlaying()) m.pause();
            try {
                gameThread.join();
            } catch (InterruptedException e) {
                Log.e("Error:", "joining thread");
            }
        }

        // start thread
        public void resume() {
            playing = true;
            gameThread = new Thread(this);
            gameThread.start();
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent motionEvent) {
            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    paused = false;
                    playing = true;
                    if(!m.isPlaying()) m.start();
                    if (motionEvent.getX() > screenX / (float)2) {
                        paddle.setMovementState(paddle.RIGHT);
                    } else
                    {
                        paddle.setMovementState(paddle.LEFT);
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    paddle.setMovementState(paddle.STOPPED);
                    break;
            }
            return true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        breakoutView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        breakoutView.pause();
    }

    @Override
    public void onBackPressed() {
        breakoutView.paused = true;
        if(breakoutView.m.isPlaying()) breakoutView.m.pause();
    }
}