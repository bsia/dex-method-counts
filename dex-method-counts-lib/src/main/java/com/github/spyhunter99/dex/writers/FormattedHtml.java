package com.github.spyhunter99.dex.writers;

import com.github.spyhunter99.dex.model.CountData;
import com.github.spyhunter99.dex.model.Metric;
import com.github.spyhunter99.dex.model.Node;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * creates a standalone, self contained .html report contents
 * Created by alex on 10/6/16.
 */
public class FormattedHtml implements IWriter {
    @Override
    public String getReport(CountData data) {
        List<CountData> r = new ArrayList<CountData>();
        r.add(data);
        return getReport(r);
    }

    public String getReport(List<CountData> data) {
        StringBuilder sb = new StringBuilder();

        //table with covers counts for all data elements, followed up individual sections for each file analyzed with anchors
        sb.append("<html>\n" +
                "<head>\n" +
                "<META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n" +
                "<style type=\"text/css\">\n" +
                "    .bannercell {\n" +
                "      border: 0px;\n" +
                "      padding: 0px;\n" +
                "    }\n" +
                "    body {\n" +
                "      margin-left: 10;\n" +
                "      margin-right: 10;\n" +
                "      font:normal 80% arial,helvetica,sanserif;\n" +
                "      background-color:#FFFFFF;\n" +
                "      color:#000000;\n" +
                "    }\n" +
                "    .a td {\n" +
                "      background: #efefef;\n" +
                "    }\n" +
                "    .b td {\n" +
                "      background: #fff;\n" +
                "    }\n" +
                "    th, td {\n" +
                "      text-align: left;\n" +
                "      vertical-align: top;\n" +
                "    }\n" +
                "    th {\n" +
                "      font-weight:bold;\n" +
                "      background: #ccc;\n" +
                "      color: black;\n" +
                "    }\n" +
                "    table, th, td {\n" +
                "      font-size:100%;\n" +
                "      border: none\n" +
                "    }\n" +
                "    table.log tr td, tr th {\n" +
                "\n" +
                "    }\n" +
                "    h2 {\n" +
                "      font-weight:bold;\n" +
                "      font-size:140%;\n" +
                "      margin-bottom: 5;\n" +
                "    }\n" +
                "    h3 {\n" +
                "      font-size:100%;\n" +
                "      font-weight:bold;\n" +
                "      background: #525D76;\n" +
                "      color: white;\n" +
                "      text-decoration: none;\n" +
                "      padding: 5px;\n" +
                "      margin-right: 2px;\n" +
                "      margin-left: 2px;\n" +
                "      margin-bottom: 0;\n" +
                "    }\n" +
                "\t\t</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<a name=\"top\"></a>\n" +
                "<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n" +
                "<tr>\n" +
                "<td class=\"bannercell\" rowspan=\"2\"></td><td class=\"text-align:right\">\n" +
                "<h2>Field and Method Count (Dex Report)</h2>\n" +
                "</td>\n" +
                "</tr>\n" +
                "<tr>\n" +
                "<td class=\"text-align:right\">Designed for use with <a href=\"https://github.com/spyhunter99/dex-method-counts\">dex-methods-count</a> plugin.</td>\n" +
                "</tr>\n" +
                "</table>\n" +
                "<hr size=\"1\">\n" +
                "<h3>Summary</h3>\n");

        //summary table
        sb.append(
                "<table id=\"summary\" class=\"log\" border=\"0\" cellpadding=\"5\" cellspacing=\"2\" width=\"100%\">\n" +
                        "<tr>\n" +
                        "<th>File</th><th>Fields</th><th>Methods</th>\n" +
                        "</tr>\n");

        //foreach file summary
        for (int i = 0; i < data.size(); i++) {
            CountData countData = data.get(i);
            sb.append(
                    "<tr class=\"a\">\n" +
                            "<td><a href=\"#" + countData.fileName + "\">" + countData.fileName + "</a></td><td>" + countData.overallMetrics.fieldCount + "</td>" +
                            "<td>" + countData.overallMetrics.methodCount + "</td>\n" +
                            "</tr>\n");

        }

        //end of summary table
        sb.append(
                "</table>\n" +
                        "<hr size=\"1\" width=\"100%\" align=\"left\">\n");

        for (int i = 0; i < data.size(); i++) {
            CountData countData = data.get(i);


            Node ptr = countData.packageTree;

            //foreach file
            sb.append( "<a name=\"" + countData.fileName + "\"></a>" +
                    "<h3>" + countData.fileName + " - Package Details</h3>\n" +

                            "<table class=\"log\" border=\"0\" cellpadding=\"5\" cellspacing=\"2\" width=\"100%\">\n" +
                            "<tr>\n" +
                            "<th>Package</th><th>Fields</th><th>Methods</th>\n");
            Iterator<Map.Entry<String, Node>> iterator = ptr.children.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Node> next = iterator.next();
                printFlat(next.getValue(), next.getKey(), sb);
            }

            sb.append("</tr>\n");
            sb.append(
                    "</table>\n" +
                            "<a href=\"#top\">Back to top</a>\n" +
                            "<hr size=\"1\" width=\"100%\" align=\"left\">\n");


        }

        sb.append("</body>\n" +
                "</html>\n" +
                "\n");

        return sb.toString();
    }

    private void printFlat(Node ptr, String parentPackage, StringBuilder sb) {
        if (ptr != null) {
            Metric count = ptr.count;
            sb.append("<tr><td>");
            if ("".equals(parentPackage))
                sb.append("(default)");
            else
                sb.append(parentPackage);
            sb.append("</td><td>").append(count.fieldCount).append("</td><td>").append(count.methodCount).append("</td></tr>");

            Iterator<Map.Entry<String, Node>> iterator = ptr.children.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Node> next = iterator.next();
                if ("".equals(parentPackage)) {
                    printFlat(next.getValue(), next.getKey(), sb);
                } else {
                    printFlat(next.getValue(), parentPackage + "." + next.getKey(), sb);
                }
            }

        }
    }


}
