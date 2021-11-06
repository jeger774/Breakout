package hu.ileuxt.mobilmegoldasok;

import android.graphics.RectF;

public class Paddle {
    private RectF rect;
    private final float length = 200;
    private final float height = 40;
    private final float paddleSpeed = 600;
    private float x;
    private float y;
    public final int STOPPED = 0;
    public final int LEFT = 1;
    public final int RIGHT = 2;

    private int paddleMoving = STOPPED;

    public Paddle(int screenX, int screenY){
        x = (screenX / (float)2)-(length/2);
        y = screenY - height;
        rect = new RectF(x, y, x + length, y + height);
    }

    public RectF getRect(){
        return rect;
    }

    public void setMovementState(int state){
        paddleMoving = state;
    }

    public void update(long fps, int screenX){
        if(paddleMoving == LEFT && x > 0){
            x = x - paddleSpeed / fps;
        }
        if(paddleMoving == RIGHT && x+length < screenX){
            x = x + paddleSpeed / fps;
        }
        rect.left = x;
        rect.right = x + length;
    }
}