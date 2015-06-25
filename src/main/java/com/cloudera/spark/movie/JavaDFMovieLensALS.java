package com.cloudera.spark.movie;

import com.cloudera.spark.mllib.SparkConfUtil;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.ml.recommendation.ALSModel;
import org.apache.spark.mllib.recommendation.ALS;
import org.apache.spark.mllib.recommendation.MatrixFactorizationModel;
import org.apache.spark.mllib.recommendation.Rating;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import scala.Function1;

import java.util.HashMap;

/**
 * Created by jayant on 6/24/15.
 */
public final class JavaDFMovieLensALS {

    public static void main(String[] args) {

        // usage
        if (args.length < 3) {
            System.err.println(
                    "Usage: JavaDFMovieLensALS <input_file> <rank> <num_iterations> [<lambda>]");
            System.exit(1);
        }

        // input parameters
        String inputFile = args[0];
        int rank = Integer.parseInt(args[1]);
        int iterations = Integer.parseInt(args[2]);
        double lambda = 1;

        if (args.length >= 4) {
            lambda = Double.parseDouble(args[3]);
        }

        // spark context
        SparkConf sparkConf = new SparkConf().setAppName("JavaMovieLensALS");
        SparkConfUtil.setConf(sparkConf);
        JavaSparkContext sc = new JavaSparkContext(sparkConf);
        SQLContext sqlContext = new SQLContext(sc);

        // options
        HashMap<String, String> options = new HashMap<String, String>();
        options.put("header", "false");
        options.put("path", inputFile);
        options.put("delimiter", ",");

        // create dataframe from input file
        DataFrame df = sqlContext.load("com.databricks.spark.csv", options);
        df.printSchema();

        // name the columns
        DataFrame newdf = df.toDF("user", "movie", "rating");
        newdf.printSchema();

        // register as a temporary table
        newdf.registerTempTable("ratings");

        // convert to proper types
        DataFrame results = sqlContext.sql("SELECT cast(user as int) user, cast(movie as int) movie, cast(rating as int) rating FROM ratings");
        results.printSchema();
        results.show();

        org.apache.spark.ml.recommendation.ALS als = new org.apache.spark.ml.recommendation.ALS();
        als.setUserCol("user").setItemCol("movie").setRank(rank).setMaxIter(iterations);
        ALSModel model =  als.fit(results);

        DataFrame pred = model.transform(results);
        pred.show();
        
        /*
        DataFrame ddddf = sqlContext.sql("CREATE TEMPORARY TABLE ratings (user int, movie int, rating int)\n" +
                "USING com.databricks.spark.csv\n" +
                "OPTIONS (path \"/Users/jayant/oldmac/workcodejayant/spark-ml-graphx/data/movielens/ratings/ratings.csv\", header \"false\")");
        */

        sc.stop();

    }

}