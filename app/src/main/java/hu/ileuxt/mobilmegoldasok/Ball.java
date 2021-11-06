package hu.ileuxt.mobilmegoldasok;

import android.graphics.RectF;
import java.util.Random;

public class Ball {
    RectF rect;
    float xVelocity;
    float yVelocity;
    private final float ballWidth = 40;
    public final float ballHeight = 40;
    private Random generator = new Random();

    public Ball(){
        xVelocity = (generator.nextInt(500) * (generator.nextBoolean() ? 1 : -1)) + (generator.nextBoolean() ? -100 : 100);
        yVelocity = -400;
        rect = new RectF();
    }

    public RectF getRect(){
        return rect;
    }

    public void update(long fps){
        rect.left = rect.left + (xVelocity / fps);
        rect.top = rect.top + (yVelocity / fps);
        rect.right = rect.left + ballWidth;
        rect.bottom = rect.top - ballHeight;
    }

    public void reverseYVelocity(){
        yVelocity = -yVelocity;
    }

    public void reverseXVelocity(){
        xVelocity = - xVelocity;
    }

    public void setRandomXVelocity(){
        if(generator.nextInt(10) <= 5){
            reverseXVelocity();
        }
    }

    public void clearObstacleY(float y){
        rect.bottom = y;
        rect.top = y - ballHeight;
    }

    public void clearObstacleX(float x){
        rect.left = x;
        rect.right = x + ballWidth;
    }

    public void reset(int x, int y){
        rect.left = (x / (float)2) + 20;
        rect.top = y - 40;
        rect.right = x / (float)2 - 20;
        rect.bottom = y - 40 - ballHeight;
    }
}