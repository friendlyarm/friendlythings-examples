package com.friendlyarm.GPIODemo;
import com.friendlyarm.FriendlyThings.GPIOEnum;

public class GPIOPin {
    private int sysPinNum = 0;
    private String name = null;
    private int value = GPIOEnum.LOW;
     
    public GPIOPin(int sysPinNum, String name, int value) {
        super();
        this.sysPinNum = sysPinNum;
        this.name = name;
        this.value = value;
    }
    public int getSysPinNum() {
        return sysPinNum;
    }
    public void setSysPinNum(int sysPinNum) {
        this.sysPinNum = sysPinNum;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    
    public int getValue() {
        return this.value;
    }
    public void setValue(int newValue) {
        this.value = newValue;
    }
}