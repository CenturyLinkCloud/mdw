/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */

package com.centurylink.mdw.kafka;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.adapter.PoolableAdapterBase;

/**
 * Dynamic Java workflow asset.
 */
@Tracked(LogLevel.TRACE)
public class KafkaAdapter extends PoolableAdapterBase implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final static String TOPIC = "test";   //TODO
    private final static String BOOTSTRAP_SERVERS =
            "localhost:9092,localhost:9093,localhost:9094"; //TODO

    protected KafkaProducer<Long, String> kafkaProducer;



    void runProducer(final int sendMessageCount) throws Exception {
        this.init();
        final Producer<Long, String> producer = kafkaProducer;
        long time = System.currentTimeMillis();

        try {
            for (long index = time; index < time + sendMessageCount; index++) {
                final ProducerRecord<Long, String> record =
                        new ProducerRecord<>(TOPIC, index,
                                    "Hello MDW " + index);

                RecordMetadata metadata = producer.send(record).get();

                long elapsedTime = System.currentTimeMillis() - time;
                System.out.printf("sent record(key=%s value=%s) " +
                                "meta(partition=%d, offset=%d) time=%d\n",
                        record.key(), record.value(), metadata.partition(),
                        metadata.offset(), elapsedTime);

            }
        } finally {
            producer.flush();
            producer.close();
        }
    }

    public static void main(String... args) throws Exception {
        KafkaAdapter kafkaAdapter = new KafkaAdapter();
        if (args.length == 0) {
            kafkaAdapter.runProducer(5);
        } else {
            kafkaAdapter.runProducer(Integer.parseInt(args[0]));
        }
    }


    @Override
    public void init(Properties parameters) {
        Thread.currentThread().setContextClassLoader(null);
        kafkaProducer =  new KafkaProducer<>(parameters);
    }


    @Override
    public void init() throws ConnectionException, AdapterException {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                                            BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "KafkaMDWProducer"); //TODO
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                                        LongSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                                    StringSerializer.class.getName());
        init(props);
    }


    @Override
    public Object openConnection() throws ConnectionException, AdapterException {
        return null;
    }


    @Override
    public void closeConnection(Object connection) {

    }

    protected ProducerRecord<Long, String> createKafkaMessage(String pRequestString, String pTopicName, long key) {

        if (key == 0L)
            key = System.currentTimeMillis();

        return  new ProducerRecord<>(pTopicName, key, pRequestString);

    }

    @Override
    public String invoke(Object connection, String request, int timeout,
            Map<String, String> headers) throws AdapterException, ConnectionException {
        String requestSent = null;
        try {
            long time = System.currentTimeMillis();
        final ProducerRecord<Long, String> record = createKafkaMessage(request, TOPIC, 0L);
        RecordMetadata metadata = (RecordMetadata) this.kafkaProducer.send(record).get();

        long elapsedTime = System.currentTimeMillis() - time;
        requestSent = "sent record(key= " + record.key() + " value=" + record.value() + ") " +
                ", meta(partition=" + metadata.partition() + " offset=" + metadata.offset() + ") time=" + elapsedTime + "\n";
        }
        catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            kafkaProducer.flush();
            kafkaProducer.close();
        }
        return requestSent;
    }


    @Override
    public boolean ping(int timeout) {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    protected boolean canBeSynchronous() {
        return true;
    }


    @Override
    protected boolean canBeAsynchronous() {
        // TODO Auto-generated method stub
        return true;
    }


}
