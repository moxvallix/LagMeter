package main.java.com.webkonsept.minecraft.lagmeter;

import java.util.LinkedList;

public class LagMeterStack{
    private int maxSize = 0;
    private final LinkedList<Float> stack = new LinkedList<Float>();

    public void add(final Float item){
        if((item != null) && (item <= 20) && (item >= 0)){
            this.stack.add(item);
            if(this.stack.size() > this.maxSize){
                this.stack.poll();
            }
        }
    }

    public void clear(final int i){
        this.stack.clear();
    }

    public float getAverage(){
        float total = 0f;
        if(this.stack.size() == 0){
            return -1F;
        }
        for(final Float f : this.stack){
            if(f != null){
                total += f;
            }
        }
        if(total != 0){
            return total / this.stack.size();
        }else{
            return 0;
        }
    }

    public int getMaxSize(){
        return this.maxSize;
    }

    public void remove(final int i){
        this.stack.remove(i);
    }

    public void setMaxSize(final int maxSize){
        this.maxSize = maxSize;
    }

    public int size(){
        return this.stack.size();
    }

    LagMeterStack(){
        this.maxSize = 10;
    }

    LagMeterStack(final int maxSize){
        this.maxSize = maxSize;
    }
}