/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */

package com.centurylink.mdw.kafka;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.adapter.PoolableAdapterBase;

/**
 * Dynamic Java workflow asset.
 */
@Tracked(LogLevel.TRACE)
public class KafkaAdapter extends PoolableAdapterBase implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static final String KAFKA_TOPIC_NAME = "topic";
    private static final String RECORD_KEY = "key";
    private static final String RECORD_PARTITION = "partition";


    public static final String MDW_KAFKA_PKG = "com.centurylink.mdw.kafka";
    private static final String PRODUCER_VARIABLE = "ProducerVariable";
    private static final String RECORD_VARIABLE = "ProducerRecordVariable";

    protected KafkaProducer<Long, String> kafkaProducer;
    private String bootstrap_servers;

    private Properties producerProps;
    private Properties recordProps;
    private static Map<String, KafkaProducer<Long, String>> producerMap = new ConcurrentHashMap<String, KafkaProducer<Long, String>>();


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

    public void closeProducer(KafkaProducer<Long, String> producer) {
        producer.flush();
        producer.close();
    }

    protected ProducerRecord<Long, String> createKafkaMessage(String pRequestString) {
        recordProps = getRecordProps();
        return  new ProducerRecord<>((String)recordProps.get(KAFKA_TOPIC_NAME), (Integer)recordProps.get(RECORD_PARTITION), (long)recordProps.get(RECORD_KEY), pRequestString);
    }

    @SuppressWarnings("unchecked")
    @Override
    public String invoke(Object connection, String request, int timeout,
            Map<String, String> headers) throws AdapterException, ConnectionException {
        String requestSent = null;
        final Producer<Long, String> producer = (KafkaProducer<Long, String>)connection;

        try {
            long time = System.currentTimeMillis();
            final ProducerRecord<Long, String> record = createKafkaMessage(request);

            /* Produce a record and wait for server to reply. Throw an exception if something goes wrong */
            RecordMetadata metadata = producer.send(record).get();

            long elapsedTime = System.currentTimeMillis() - time;
            requestSent = "sent record(key= " + record.key() + " value=" + record.value() + ") " +
                    ", meta(partition=" + metadata.partition() + " offset=" + metadata.offset() + ") time=" + elapsedTime + "\n";
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
        return true;
    }

 /*   void runProducerSync(final int sendMessageCount) throws Exception {
        this.init();
        final Producer<Long, String> producer = kafkaProducer;
        long time = System.currentTimeMillis();

        try {
            for (long index = time; index < time + sendMessageCount; index++) {
                final ProducerRecord<Long, String> record =
                        new ProducerRecord<>("topic", index,
                                "Hello MDW Sync" + index);

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
        this.init();
        final Producer<Long, String> producer = kafkaProducer;
        long time = System.currentTimeMillis();
        final CountDownLatch countDownLatch = new CountDownLatch(sendMessageCount);

        try {
            for (long index = time; index < time + sendMessageCount; index++) {
                final ProducerRecord<Long, String> record =
                        new ProducerRecord<>("topic", index, "Hello MDW Async " + index);
                producer.send(record, (metadata, exception) -> {
                    long elapsedTime = System.currentTimeMillis() - time;
                    if (metadata != null) {
                        System.out.printf("sent record(key=%s value=%s) " +
                                "meta(partition=%d, offset=%d) time=%d\n",
                                record.key(), record.value(), metadata.partition(),
                                metadata.offset(), elapsedTime);
                    } else {
                        exception.printStackTrace();
                    }
                    countDownLatch.countDown();
                });
            }
            countDownLatch.await(25, TimeUnit.SECONDS);
        }finally {
            producer.flush();
            producer.close();
        }
    }

    public static void main(String... args) throws Exception {
        KafkaAdapter kafkaAdapter = new KafkaAdapter();
        if (args.length == 0) {
            kafkaAdapter.runProducerAsync(5);
        } else {
            kafkaAdapter.runProducerAsync(Integer.parseInt(args[0]));
        }
    }*/
}
