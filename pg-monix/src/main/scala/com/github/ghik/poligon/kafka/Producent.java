package com.github.ghik.poligon.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.Future;

public class Producent {
    public static void main(String[] args) throws IOException {
        var props = new Properties();
        props.load(Producent.class.getClassLoader().getResourceAsStream("kafka.properties"));
        var futures = new ArrayList<Future<?>>();
        try (var producer = new KafkaProducer<>(props, new StringSerializer(), new StringSerializer())) {
            var start = System.nanoTime();
            for (int i = 0; i < 10000000; i++) {
                var record = new ProducerRecord<>("topik", "kej" + i, "walue" + i);
                futures.add(producer.send(record));
            }
            for (Future<?> f : futures) {
                f.get();
            }
            var duration = System.nanoTime() - start;
            System.out.printf("Took %s", duration / 1000000);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
