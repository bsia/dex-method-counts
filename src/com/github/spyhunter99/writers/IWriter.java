package com.github.spyhunter99.writers;

import com.github.spyhunter99.model.CountData;

import java.util.List;

/**
 * Created by alex on 10/7/16.
 */
public interface IWriter {
    public String  getReport(CountData data);
    public String  getReport(List<CountData> data);
}
