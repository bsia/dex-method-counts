package com.github.spyhunter99.model;

import java.io.PrintWriter;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Created by alex on 10/6/16.
 */
public class Node {
    public Metric count = new Metric();
    public NavigableMap<String, Node> children = new TreeMap<String, Node>();
}

