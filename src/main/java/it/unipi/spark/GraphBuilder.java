package it.unipi.spark;

import scala.Tuple2;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GraphBuilder {
    private static final Pattern link = Pattern.compile("\\[\\[(.*?)\\]\\]");
    private static final Pattern title = Pattern.compile("<title>(.*?)</title>");
    private static Matcher t;
    private static Matcher l;

    public static Tuple2<String, ArrayList<String>> buildGraph(String page){
        t = title.matcher(page);
        if(t.find()){
            l = link.matcher(page);
            ArrayList<String> list = new ArrayList<>();
            while(l.find())
                list.add(l.group(1));
            return new Tuple2<String, ArrayList<String>>(t.group(1), list);
        }
        return new Tuple2<String, ArrayList<String>>("", new ArrayList<>());
    }
}
