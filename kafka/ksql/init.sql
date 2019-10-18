-- set 'commit.interval.ms'='2000';
-- set 'cache.max.bytes.buffering'='10000000';
-- set 'auto.offset.reset'='earliest';

CREATE STREAM stocks_csv(DT STRING, OPENED DOUBLE, HIHG DOUBLE, LOW DOUBLE, CLOSED DOUBLE, ADJ_CLOSED DOUBLE, VOLUME BIGINT) 
     WITH (kafka_topic='stocks-csv', value_format='DELIMITED');

CREATE STREAM stocks WITH (kafka_topic='stocks', VALUE_FORMAT='avro', timestamp='DT', timestamp_format='yyy-MM-dd') AS
     SELECT DT, 'AAPL' AS TICKER, CLOSED FROM stocks_csv PARTITION BY TICKER;

--  auxiliary stream to present predictions from ML 
CREATE STREAM predictions(DT STRING, TICKER STRING, CLOSED DOUBLE) 
     WITH (kafka_topic='predictions', VALUE_FORMAT='json', timestamp='DT', timestamp_format='yyy-MM-dd');
 
--  stream in avro format for ELK connector 
CREATE STREAM stocks_predictions WITH (kafka_topic='stocks-predictions', VALUE_FORMAT='avro', 
          timestamp='DT', timestamp_format='yyy-MM-dd') AS
     SELECT DT, CONCAT(TICKER, '_P') AS TICKER, CLOSED FROM predictions 
          WHERE CLOSED > 0.0
          PARTITION BY TICKER;