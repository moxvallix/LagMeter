package com.moxvallix;

import java.io.Serializable;
import java.util.LinkedList;

public class LagMeterStack extends LinkedList<Double> implements Serializable{
	private static final long serialVersionUID = -1386094521320L;
	private int maxSize = 0;

	@Override
	public boolean add(final Double item){
		if((item != null) && (item <= 20) && (item >= 0)){
			super.add(item);
			if(super.size() > this.maxSize){
				super.poll();
			}
			return true;
		}
		return false;
	}

	public double getAverage(){
		double total = 0D;
		if(super.size() == 0){
			return -1D;
		}
		for(Double f : this){
			if(f != null){
				total += f;
			}
		}
		return total / super.size();
	}

	public int getMaxSize(){
		return this.maxSize;
	}

	public boolean remove(final Double i){
		return super.remove(i);
	}

	public void setMaxSize(final int maxSize){
		this.maxSize = maxSize;
	}

	LagMeterStack(){
		this(10);
	}

	LagMeterStack(final int maxSize){
		super();
		this.maxSize = maxSize;
	}

	public String toString(){
		StringBuilder s = new StringBuilder("[ ");
		if(super.size() != 0)
			for(Double d : this)
				s.append(String.format("%.2f, ", d));
		else
			s = new StringBuilder("[ ]");
		return "LagMeterStack@"+hashCode()+"{\n\tdata = "+(super.size() != 0 ? s.toString().substring(0, s.length() - 2) + " ]" : s.toString())+"\n\tmaxSize = "+this.maxSize+"\n}";
	}
}