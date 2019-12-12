package com.example.acme.streamApp

import org.apache.spark.{ SparkConf, SparkContext }
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka010._
import za.co.absa.abris.avro.read.confluent.SchemaManager
import za.co.absa.abris.avro.functions.from_confluent_avro
import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import java.time.LocalDate
import java.time.format._
import requests._
import ujson._

// === Model ====

case class Quote(dt: String, ticker: String, var closed: Double)
object Quote {
  def apply(r: Row): Quote = Quote(r.getAs[String]('DT.name), r.getAs[String]('TICKER.name), r.getAs[Double]('CLOSED.name))
}

//  end Model ===

object KafkaStreamApp {

  def main(args: Array[String]) {
    if (args.length < 5) {
      System.err.println(s"""Usage: KafkaStreamApp <brokers> <groupId> <topic_quotes> <topic_predictions> <url_predictor>
        |  <brokers> is a list of one or more Kafka brokers
        |  <groupId> is a consumer group name to consume from topics (optional)
        |  <topic_quotes> is a topics to consume from
        |  <topic_predictions> is topic to produce predicted quote
        |  <url_predictor> url to predictor service
        """.stripMargin)
      System.exit(1)
    }

    val Array(brokers, groupId, topic_quotes, topic_predictions, url_predictor) = args

    val spark = SparkSession.builder.appName("WorkshopStreamApp").getOrCreate()
    spark.sparkContext.setLogLevel("ERROR")

    import spark.implicits._

    val df = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", brokers)
      .option("subscribe", topic_quotes)
      .option("startingOffsets", "earliest")
      .load()

    val schemaRegistryConfig = Map(
      SchemaManager.PARAM_SCHEMA_REGISTRY_URL -> "http://schema-registry:8081",
      SchemaManager.PARAM_SCHEMA_REGISTRY_TOPIC -> topic_quotes,
      SchemaManager.PARAM_VALUE_SCHEMA_NAMING_STRATEGY -> SchemaManager.SchemaStorageNamingStrategies.TOPIC_NAME,
      SchemaManager.PARAM_VALUE_SCHEMA_ID -> "latest")

    val data = df.select(from_confluent_avro(col("value"), schemaRegistryConfig) as 'data).select("data.*")

    //  Workshop task ====================
    // 
    //  1. Handle response from predictor
    //
  
    val predictions = data map (row => {

      val quote = Quote(row)
      val nextDayQuote = nextDay(quote)

      // call predictor 
      val response = requests.get(url_predictor + s"/predict/${nextDayQuote.dt}")

      // Workshop :  Handle response 
      //
      nextDayQuote.closed = response.statusCode match {
        case 200 => ujson.read(response.text).obj('CLOSED.name).num
        case _   => 0.0
      }
      nextDayQuote
    })

    predictions.createOrReplaceTempView("predictions")

    // Workshop :  Can you explain this one ?
    val result = spark.sql("select * from predictions")
      .select(to_json(struct($"*"))
        .alias("value"))

    //  write to kafka topic
    val stream = result.writeStream.format("kafka")
      .outputMode("append")
      .option("kafka.bootstrap.servers", brokers)
      .option("checkpointLocation", "./checkpoint")
      .option("topic", topic_predictions)
      .start()
      .awaitTermination()

    // Use console to debug
    /*
    val stream = data
          .writeStream
          .format("console")
          .option("truncate", "false")
          .start()
          .awaitTermination()
    */
  }

  def nextDay(q: Quote): Quote = {
    val formatter = new DateTimeFormatterBuilder()
      .appendPattern("yyyy-MM-dd")
      .toFormatter();

    val dt = LocalDate.parse(q.dt, formatter);
    val nextDay = LocalDate.from(dt).plusDays(1)
    q.copy(dt = nextDay.toString())
  }
}