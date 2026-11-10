package dev.xiaoyu.useonce.duck;

public interface FurnaceResultTracker {
    int useonce$getResultCountBefore();

    void useonce$setResultCountBefore(int count);
}