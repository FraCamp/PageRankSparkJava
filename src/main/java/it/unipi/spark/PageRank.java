package it.unipi.spark;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PageRank {
    private static int iterations = 3;
    private static Double alpha = 0.05;
    private static String inputFile = "wiki-micro.txt";
    private static String outputFile = "PageRankSparkJava";
    private static long nodesNumber;

    public static void main(String[] args){
        //Taking arguments as configuration parameters
        if(args.length == 4){
            iterations = Integer.parseInt(args[0]);
            alpha = Double.parseDouble(args[1]);
            inputFile = args[2];
            outputFile = args[3];
        }

        // import context and execution in cluster mode
        SparkConf sc = new SparkConf().setAppName("pageRankJava").setMaster("yarn");
        JavaSparkContext javaSparkContext = new JavaSparkContext(sc);

        // Get the input data from the input file
        JavaRDD<String> input = javaSparkContext.textFile(inputFile);
        // We parse the input data to form the graph
        JavaPairRDD<String, ArrayList<String>> graph = input.mapToPair(GraphBuilder::buildGraph).cache();
        // We get the number of nodes in the graph
        nodesNumber = graph.count();
        /*
         We create this list with the page titles present in the initial dataset, we will use this list later
         to filter the page that we don't need
        */
        List<String> datasetKeys = graph.keys().collect();
        // Here we create a new RDD adding the initial rank = 1/nodesNumber
        Double n = 1.0d/((double)nodesNumber);
        JavaPairRDD<String, Double> rankedNodes = graph.mapValues(value -> n);

        for (int i = 0; i < iterations; i++){
            /*
             We calculate the contribute to send to the nodes' neighbors and filter the contributes sent to the pages
             that do not belong to the dataset
            */
            JavaPairRDD<String, Double> contribution = graph.join(rankedNodes).flatMapToPair(PageRank::sendContributes)
                    .filter(x -> datasetKeys.contains(x._1));
            // Here we sum the contributions per node
            JavaPairRDD<String, Double> summedContributes = contribution.reduceByKey(PageRank::addContributes);
            // Here we use the algorithm formula to compute the new Page Rank value per each node
            rankedNodes = summedContributes.mapValues(value->n * alpha + (1.0d - alpha)*value);
        }
        /*
         To sort the pages, we initially swap the values and keys, than we perform the sorting on the new keys
         so we swap again in order to get the initial RDD structure back
        */
        JavaPairRDD<String, Double> sortedPageRank = rankedNodes.mapToPair(Tuple2::swap).sortByKey(false).mapToPair(Tuple2::swap);
        // We save the RDD in a specified location
        sortedPageRank.saveAsTextFile(outputFile);
    }

    /**
     * Function that compute the contribute per node and send it to each neighbors (if present)
     * The contribute is computed by dividing the rank for the number of node neighbors
     */
    private static Iterator<Tuple2<String, Double>> sendContributes(Tuple2<String, Tuple2<ArrayList<String>, Double>> tuple) {
        ArrayList<Tuple2<String, Double>> contributes = new ArrayList<>();
        String title = tuple._1;
        ArrayList<String> neighbors = tuple._2._1;
        Double rank = tuple._2._2;

        if(neighbors.size() > 0) {
            double contribute = rank/neighbors.size();
            for(String neighbor: neighbors) {
                contributes.add(new Tuple2<>(neighbor, contribute));
            }
        }
        contributes.add(new Tuple2<>(title, 0.0));
        return contributes.iterator();
    }

    /**
     * Function that check if the numbers passed are actual number and if they are return the sum of them in order to
     * calculate the node rank
     */
    private static Double addContributes(Double c1, Double c2){
        if(!c1.isNaN() && !c2.isNaN())
            return c1 + c2;
        if(!c1.isNaN())
            return c1;
        if(!c2.isNaN())
            return c2;
        return 0.0;
    }
}