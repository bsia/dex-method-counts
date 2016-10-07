package com.github.spyhunter99.writers;

import com.github.spyhunter99.model.CountData;
import com.github.spyhunter99.model.Metric;
import com.github.spyhunter99.model.Node;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * suitable for printing to stdout or to a plain text file
 * Created by alex on 10/6/16.
 */
public class FormattedText {
    public static String getReport(List<CountData> data){
        StringBuilder sb = new StringBuilder();
        for (int i=0; i < data.size(); i++){
            CountData countData = data.get(i);
            sb.append("Method and File count for file: ").
                    append(countData.fileName).
                    append("\n\tMethods ").
                    append(countData.overallMetrics.methodCount).
                    append("\n\tFields ").
                    append(countData.overallMetrics.fieldCount).
                    append("\n");
            sb.append("----------------------------------\n");
            sb.append("Package details:\n");

            Node ptr = countData.packageTree;
            print(ptr, "default","   ",sb);

        }
        return sb.toString();
    }

    private static void print(Node ptr, String pgk, String indent, StringBuilder sb) {
        if (ptr!=null){
            Metric count = ptr.count;
            sb.append(indent).append(pgk).append(" Methods ").
                    append(count.methodCount).
                    append(" Fields ").
                    append(count.fieldCount).
                    append("\n");
            String indent2=indent + "   ";
            Iterator<Map.Entry<String, Node>> iterator = ptr.children.entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry<String, Node> next = iterator.next();
                print (next.getValue(), next.getKey(), indent2,sb);
            }

        }
    }
}
