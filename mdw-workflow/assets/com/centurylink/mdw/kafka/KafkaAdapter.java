/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */

package com.centurylink.mdw.kafka;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.adapter.PoolableAdapterBase;

/**
 * Dynamic Java workflow asset.
 */
@Tracked(LogLevel.TRACE)
public class KafkaAdapter extends PoolableAdapterBase implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private static final String KAFKA_TOPIC_NAME = "topic";
    private static final String RECORD_KEY = "key";
    private static final String RECORD_PARTITION = "partition";


    public static final String MDW_KAFKA_PKG = "com.centurylink.mdw.kafka";
    private static final String PRODUCER_VARIABLE = "ProducerVariable";
    private static final String RECORD_VARIABLE = "ProducerRecordVariable";

    protected KafkaProducer<Object, Object> kafkaProducer;
    private String bootstrap_servers;

    private Properties producerProps;
    private Properties recordProps;
    private static Map<String, KafkaProducer<Object, Object>> producerMap = new ConcurrentHashMap<String, KafkaProducer<Object, Object>>();


    @Override
    public void init(Properties parameters) {
        bootstrap_servers = parameters.getProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG);
        // This is so that all dependent class from kafka-clients jar are found during consumer creation
        Package pkg = PackageCache.getPackage(MDW_KAFKA_PKG);
        if (pkg == null)
            pkg = PackageCache.getPackages().get(0);
        Thread.currentThread().setContextClassLoader(pkg.getCloudClassLoader());
        producerProps = parameters;
    }

    @Override
    public void init() throws ConnectionException, AdapterException {
        producerProps = getProducerProps();
        init(producerProps);
    }


    @Override
    public Object openConnection() throws ConnectionException, AdapterException {
        synchronized(producerMap) {
            if (producerMap.get(bootstrap_servers) == null)
            {
                kafkaProducer =  new KafkaProducer<>(producerProps);
                producerMap.put(bootstrap_servers, kafkaProducer);
                return kafkaProducer;
            }
            else
                return producerMap.get(bootstrap_servers);
        }
    }

    @Override
    public void closeConnection(Object connection) {
    }

    public void closeProducer(KafkaProducer<Object, Object> producer) {
        producer.flush();
        producer.close();
    }

    /*
     * Object should be based on what is set as KEY_SERIALIZER_CLASS_CONFIG, We are assuming value is of String type
     */
    protected ProducerRecord<Object, Object> createKafkaMessage(Object pRequestString) {
        recordProps = getRecordProps();
        return  new ProducerRecord<>((String)recordProps.get(KAFKA_TOPIC_NAME), (Integer)recordProps.get(RECORD_PARTITION), recordProps.get(RECORD_KEY), pRequestString);
    }

    @SuppressWarnings("unchecked")
    @Override
    public String invoke(Object connection, String request, int timeout,
            Map<String, String> headers) throws AdapterException, ConnectionException {
        String requestSent = null;
        final Producer<Object, Object> producer = (KafkaProducer<Object, Object>)connection;

        try {
            long time = System.currentTimeMillis();
            final ProducerRecord<Object, Object> record = createKafkaMessage(request);
            RecordMetadata metadata = null;
            if (isSynchronous()) {
                /* Produce a record and wait for server to reply. Throw an exception if something goes wrong */
                metadata = producer.send(record).get();
             }
            else {
                /* Produce a record without waiting for server. This includes a callback that will print an error if something goes wrong */
                SendCallback callback = new SendCallback();
                Future<RecordMetadata> future = producer.send(record, callback);
                metadata = future.get();
            }
            long elapsedTime = System.currentTimeMillis() - time;
            requestSent = "sent record(key=" + record.key() + " value=" + record.value() + ") " +
                    ", meta(partition=" + metadata.partition() + " offset=" + metadata.offset() + ") time=" + elapsedTime;

        }
        catch (InterruptedException ex){
            producer.close();
            producerMap.remove(bootstrap_servers);
            throw new AdapterException(-1, ex.getMessage(), ex);
        }
        catch (ExecutionException ex) {
            producer.close();
            producerMap.remove(bootstrap_servers);
            throw new ConnectionException(ConnectionException.CONNECTION_DOWN, ex.getMessage(), ex);
        }
        finally {
            producer.flush();
        }
        return requestSent;
    }

    public Properties getProducerProps() {
        Properties properties = new Properties();
        try {
            String producerVar = getAttributeValueSmart(PRODUCER_VARIABLE);
            if (producerVar != null) {
                Process processVO = getProcessDefinition();
                Variable variableVO = processVO.getVariable(producerVar);
                if (variableVO == null)
                    throw new ActivityException("Producer Config variable '" + producerVar + "' is not defined for process " + processVO.getLabel());
                if (!variableVO.getVariableType().startsWith("java.util.Map") && !variableVO.getVariableType().startsWith("java.lang.Object"))
                    throw new ActivityException("Producer Config variable '" + producerVar + "' must be of type java.util.Map or java.lang.Object");
                Object producerObj = getVariableValue(producerVar);
                if (producerObj != null) {
                    if (producerObj instanceof Properties){
                        return (Properties)producerObj;
                    }
                    else if (producerObj instanceof Map) {
                        for (Object key : ((Map<?,?>)producerObj).keySet()) {
                            properties.put(key.toString(), ((Map<?,?>)producerObj).get(key).toString());
                        }
                    }
                }
            }
            return properties;
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return properties;
        }
    }

    public Properties getRecordProps() {
        Properties properties = new Properties();
        try {
            String recordVar = getAttributeValueSmart(RECORD_VARIABLE);
            if (recordVar != null) {
                Process processVO = getProcessDefinition();
                Variable variableVO = processVO.getVariable(recordVar);
                if (variableVO == null)
                    throw new ActivityException("Record variable '" + recordVar + "' is not defined for process " + processVO.getLabel());
                if (!variableVO.getVariableType().startsWith("java.lang.Object"))
                    throw new ActivityException("Record variable '" + recordVar + "' must be of type java.lang.Object");
                Object recordObj = getVariableValue(recordVar);
                if (recordObj != null) {
                    if (recordObj instanceof Properties){
                        return (Properties)recordObj;
                    }
                }
            }
            return properties;
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return properties;
        }
    }

    @Override
    public boolean ping(int timeout) {
        return false;
    }

    @Override
    protected boolean canBeSynchronous() {
        return true;
    }

    @Override
    protected boolean canBeAsynchronous() {
        return false;
    }

    private static Producer<Object, Object> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "localhost:9092,localhost:9093,localhost:9094");
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "KafkaMDWProducer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                LongSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName());
        return new KafkaProducer<>(props);
    }
    void runProducerSync(final int sendMessageCount) throws Exception {
        final Producer<Object, Object> producer = createProducer();
        long time = System.currentTimeMillis();

        try {
            for (long index = time; index < time + sendMessageCount; index++) {
                final ProducerRecord<Object, Object> record =
                        new ProducerRecord<>("kafkaTopic", index,
                                "Hello MDW Sync " + index);

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

    void runProducerAsync(final int sendMessageCount) throws Exception {
        final Producer<Object, Object> producer = createProducer();
        long time = System.currentTimeMillis();

        try {
            for (long index = time; index < time + sendMessageCount; index++) {
                final ProducerRecord<Object, Object> record =
                        new ProducerRecord<>("kafkaTopic", index, "Hello MDW Async " + index);
                SendCallback callback = new SendCallback();
                producer.send(record, callback);
            }
        }finally {
            producer.flush();
            producer.close();
        }
    }

    private static class SendCallback implements Callback {
        @Override
        public void onCompletion(RecordMetadata recordMetadata, Exception e) {
            if (e != null) {
                logger.severeException("Error while producing message to topic :" + recordMetadata + e.getMessage(), e);
            } else {
                String message = String.format("sent message to topic:%s partition:%s  offset:%s", recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset());
                logger.mdwDebug(message);
            }
        }
    }

    public static void main(String... args) throws Exception {
        KafkaAdapter kafkaAdapter = new KafkaAdapter();
        if (args.length == 0) {
            kafkaAdapter.runProducerAsync(5);
        } else {
            kafkaAdapter.runProducerAsync(Integer.parseInt(args[0]));
        }
    }
}
