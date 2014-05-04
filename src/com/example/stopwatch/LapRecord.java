/*
 * tata inc. 
 * Copyright (c) 2011-2014 All Rights Reserved.
 */
package com.example.stopwatch;

/**
 * <p>TODO</p>
 *
 * @author <a href="mailto:yangkaifeng1985@126.com">Kaifeng Yang</a>
 * @version $Id: LapRecord.java, v0.1 Apr 24, 2014 8:28:26 PM, Kaifeng Yang Exp $
 */
public class LapRecord {
    public String lapNO;
    public String lapTime;
    public String deltaTime;
    
    public LapRecord(String lapNO, String lapTime, String deltaTime) {
    	this.lapNO = lapNO;
    	this.lapTime = lapTime;
    	this.deltaTime = deltaTime;
    }
}
