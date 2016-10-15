package com.github.spyhunter99.dex.model;

/**
 * Created by alex on 10/6/16.
 */
public class CountData {
    public String fileName;
    public boolean isMultiDex=false;
    public Node packageTree;
    //public Map<String, Metric> packageCount = new HashMap<String, Metric>();

    public Metric overallMetrics = new Metric();

}
