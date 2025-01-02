package com.example.shareplate;

public class Volunteering {

    private final float timeCommitment;

    private final int seats;

    public Volunteering(String title, String desc, String loc, String date, int image, float time, int seat){
//        super(title, desc, loc, date, image);
        timeCommitment = time;
        seats = seat;
    }

    public float getTimeCommitment(){
        return timeCommitment;
    }

    public int getSeats(){
        return seats;
    }

}
