package com.github.spyhunter99.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by alex on 10/6/16.
 */
public class CountData {
    public String fileName;
    public Node packageTree;
    public Map<String, Metric> packageCount = new HashMap<String, Metric>();

    public Metric overallMetrics = new Metric();

}
