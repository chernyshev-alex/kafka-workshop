-- set 'commit.interval.ms'='2000';
-- set 'cache.max.bytes.buffering'='10000000';
-- set 'auto.offset.reset'='earliest';

-- System design :
-- 1. [csv-producer] -> topic:stock_csv -> stream:stocks -> ELK connect:elastic -> graphana
-- 2. stream:stocks -> spark:stream-app -> propheat:predict  -> topic:predictions -> topic:stocks_predictions
-- 3. topic:stocks_predictions -> ELK connect:elastic  -> graphana

--  This stream reads from input CSV topic : stocks-csv

CREATE STREAM stocks_csv(DT STRING, OPENED DOUBLE, HIHG DOUBLE, LOW DOUBLE, CLOSED DOUBLE, ADJ_CLOSED DOUBLE, VOLUME BIGINT) 
     WITH (kafka_topic='stocks-csv', value_format='DELIMITED');

--  This stream converts CSV data from stocks-csv stream & writes to topic 'stocks' in AVRO format 
--  ELK connector puts data to elastic search from this topic

CREATE STREAM stocks WITH (kafka_topic='stocks', VALUE_FORMAT='avro', timestamp='DT', timestamp_format='yyy-MM-dd') AS
     SELECT DT, 'AAPL' AS TICKER, CLOSED FROM stocks_csv PARTITION BY TICKER;
 
--  Spark app. writes predictions to this topic in JSON format

CREATE STREAM predictions(DT STRING, TICKER STRING, CLOSED DOUBLE) 
     WITH (kafka_topic='predictions', VALUE_FORMAT='json', timestamp='DT', timestamp_format='yyy-MM-dd');
 
-- Workshop task --
-- This stream should deliver predictions in AVRO format to ELK connector.
-- Stream should read JSON data from the topic 'predictions'  and write data to the topic 'stocks-predictions' in AVRO
-- We have create stream 'stocks_predictions' and topic 'stocks-predictions' with given properties (format, key)
-- Add SELECT expression that reads data from stream 'predictions' 
-- Columns :  DT, TICKER + '_P', CLOSED 
-- CLOSED > 0.0
-- PARTITIONED BY TICKER
-- Check ELK connector consumed data with no errors and you can find them in ELK cluster under 'market' index
--
--  curl localhost:8083/connectors/elk/status    # Check connector status
--  curl localhost:9200/market/_search?pretty    # Find predicted data
--  

CREATE STREAM stocks_predictions WITH (kafka_topic='stocks-predictions', VALUE_FORMAT='avro', 
          timestamp='DT', timestamp_format='yyy-MM-dd') AS
     -- ASK to write this statement 
     SELECT DT, CONCAT(TICKER, '_P') AS TICKER, CLOSED FROM predictions 
          WHERE CLOSED > 0.0
          PARTITION BY TICKER;
     -- End ask to write this statement 