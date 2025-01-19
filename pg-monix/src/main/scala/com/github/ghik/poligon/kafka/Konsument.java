package com.github.ghik.poligon.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;

public class Konsument {
    public static void main(String[] args) throws IOException {
        var props = new Properties();
        props.load(Konsument.class.getClassLoader().getResourceAsStream("kafka.properties"));
        try (var consumer = new KafkaConsumer<>(props, new StringDeserializer(), new StringDeserializer())) {
            consumer.subscribe(List.of("topik"));
            ConsumerRecords<String, String> records;
            while(true) {
                records = consumer.poll(Duration.ofSeconds(1));
                if(records.isEmpty()) {
                    break;
                }
                System.out.println(records.count());
            }
            System.out.println("zi end");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
