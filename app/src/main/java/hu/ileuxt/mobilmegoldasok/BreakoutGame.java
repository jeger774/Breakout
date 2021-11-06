package hu.ileuxt.mobilmegoldasok;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

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
        volatile boolean playing;
        boolean paused = true;
        long fps;
        int screenX;
        int screenY;
        int numBricks = 0;
        int score = 0;
        int lives = 3;

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
            ball = new Ball(screenX, screenY);
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
                }
                draw();
                long timeThisFrame = System.currentTimeMillis() - startFrameTime;
                if (timeThisFrame >= 1) {
                    fps = 600 / timeThisFrame;
                }
            }
        }

        public void update() {
            paddle.update(fps);
            ball.update(fps);
            // ball hits brick
            for (int i = 0; i < numBricks; i++) {
                if (bricks[i].getVisibility()) {
                    if (RectF.intersects(bricks[i].getRect(), ball.getRect())) {
                        bricks[i].setInvisible();
                        ball.reverseYVelocity();
                        score = score + 10;
                    }
                }
            }
            // ball hits paddle
            if (RectF.intersects(paddle.getRect(), ball.getRect())) {
                ball.setRandomXVelocity();
                ball.reverseYVelocity();
                ball.clearObstacleY(paddle.getRect().top - 2);
            }
            // ball hits bottom
            if (ball.getRect().bottom > screenY) {
                ball.reverseYVelocity();
                ball.clearObstacleY(screenY);
                lives--;
            }

            // ball hits top
            if (ball.getRect().top < 0)
            {
                ball.reverseYVelocity();
                ball.clearObstacleY(30);
            }

            // ball hits left
            if (ball.getRect().left < 0)
            {
                ball.reverseXVelocity();
                ball.clearObstacleX(0);
            }

            // ball hits right
            if (ball.getRect().right > screenX) {
                ball.reverseXVelocity();
                ball.clearObstacleX(screenX-30);
            }

            //game end conditions check
            if (lives == 0) {
                playing = false;
                paused = true;
                draw();
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
                paint.setColor(Color.argb(255, 0, 240, 255));
                for (int i = 0; i < numBricks; i++) {
                    if (bricks[i].getVisibility()) {
                        canvas.drawRoundRect(bricks[i].getRect(),8, 8, paint);
                    }
                }


                paint.setColor(Color.argb(255, 255, 255, 255));
                paint.setTextSize(50);
                paint.setTextAlign(Paint.Align.CENTER);
                int xPos = (canvas.getWidth() / 2);
                int yPos = (int) ((canvas.getHeight() / 2) - ((paint.descent() + paint.ascent())/2));
                //draw score and lives
                canvas.drawText("Score: " + score + "   Lives: " + lives, xPos, 60, paint);

                paint.setTextSize(70);
                //game end conditions check
                if (score >= numBricks*10) {
                    canvas.drawText("YOU WIN!", xPos, yPos, paint);
                }
                if (lives <= 0) {
                    canvas.drawText("GAME OVER!", xPos, yPos, paint);
                }
                if (paused && lives != 0){
                    canvas.drawText("Touch to start.", xPos,yPos,paint);
                }
                ourHolder.unlockCanvasAndPost(canvas);
            }
        }

        // if paused/stopped shutdown thread
        public void pause() {
            playing = false;
            paused = true;
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
                    if (motionEvent.getX() > screenX / (float)2) {
                        if (paddle.getRect().left == screenX-200) paddle.setMovementState(paddle.STOPPED);
                        else if (paddle.getRect().left < screenX - 200) paddle.setMovementState(paddle.RIGHT);
                    } else
                    {
                        if (paddle.getRect().left == 0) paddle.setMovementState(paddle.STOPPED);
                        else if(paddle.getRect().left > 0) paddle.setMovementState(paddle.LEFT);
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
        breakoutView.paused=true;
    }
}