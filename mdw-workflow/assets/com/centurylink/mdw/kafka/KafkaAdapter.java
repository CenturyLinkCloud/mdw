/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */

package com.centurylink.mdw.kafka;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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

    private static final String PROP_TOPIC = "topic";
    private final static String BOOTSTRAP_SERVERS = "bootstrap_servers";
    private final static String CLIENT_ID_CONFIG = "client_id";
    private static final String KEY_SERIALIZER_CLASS_CONFIG = "key_serializer";
    private static final String VALUE_SERIALIZER_CLASS_CONFIG = "value_serializer";
    public static final String MDW_KAFKA_PKG = "com.centurylink.mdw.kafka";
    private static final String PRODUCER_VARIABLE = "ProducerVariable";
    private static final String RECORD_VARIABLE = "RecordVariable";

    protected KafkaProducer<Long, String> kafkaProducer;
    private String topicName;
    private String bootstrap_servers;
    private String client_id_config;
    private String key_serializer_class_config;
    private String value_serializer_class_config;
    private Properties props;
    private static Map<String, KafkaProducer<Long, String>> producerMap = new ConcurrentHashMap<String, KafkaProducer<Long, String>>();


    @Override
    public void init(Properties parameters) {
        topicName = parameters.getProperty(PROP_TOPIC);

        // This is so that all dependent class from kafka-clients jar are found during consumer creation
        Package pkg = PackageCache.getPackage(MDW_KAFKA_PKG);
        if (pkg == null)
            pkg = PackageCache.getPackages().get(0);
        Thread.currentThread().setContextClassLoader(pkg.getCloudClassLoader());
        props = parameters;
    }

/*    private Map<String,String> getMap(Properties properties) {
        if (properties == null)
            return null;

        Map<String,String> params = new HashMap<String,String>();
        for (Object name : properties.keySet()) {
            if (name != null)
              params.put(name.toString(), properties.getProperty(name.toString()));
        }
        return params;
    }*/

    @Override
    public void init() throws ConnectionException, AdapterException {
        props = new Properties();
        topicName = getAttribute(PROP_TOPIC, "kafkaTopic", true);
        props.put(PROP_TOPIC, topicName);

        bootstrap_servers = getAttribute(BOOTSTRAP_SERVERS, "localhost:9092,localhost:9093,localhost:9094", true);
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap_servers);

        client_id_config = getAttribute(CLIENT_ID_CONFIG, "KafkaMDWProducer", false);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, client_id_config);

        key_serializer_class_config = getAttribute(KEY_SERIALIZER_CLASS_CONFIG, "Long", false);
        if (key_serializer_class_config.equalsIgnoreCase("Long"))
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
        else if (key_serializer_class_config.equalsIgnoreCase("String"))
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        else
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, key_serializer_class_config);

        value_serializer_class_config = getAttribute(VALUE_SERIALIZER_CLASS_CONFIG, "String", false);
        if (value_serializer_class_config.equalsIgnoreCase("String"))
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        else if (value_serializer_class_config.equalsIgnoreCase("Long"))
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
        else
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, value_serializer_class_config);

        init(props);
    }


    @Override
    public Object openConnection() throws ConnectionException, AdapterException {
        synchronized(producerMap) {
            if (producerMap.get(bootstrap_servers) == null)
            {
                kafkaProducer =  new KafkaProducer<>(props);
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

    protected ProducerRecord<Long, String> createKafkaMessage(String pRequestString, String pTopicName, long key) {
        if (key == 0L)
            key = System.currentTimeMillis();

        return  new ProducerRecord<>(pTopicName, key, pRequestString);
    }

    @Override
    public String invoke(Object connection, String request, int timeout,
            Map<String, String> headers) throws AdapterException, ConnectionException {
        String requestSent = null;
        final Producer<Long, String> producer = (KafkaProducer<Long, String>)connection;

        try {
            long time = System.currentTimeMillis();
            final ProducerRecord<Long, String> record = createKafkaMessage(request, topicName, 0L);
            RecordMetadata metadata = null;
            if (isSynchronous()) {
                /* Produce a record and wait for server to reply. Throw an exception if something goes wrong */
                metadata = producer.send(record).get();
            }
            else
            {
                /* Produce a record without waiting for server. This includes a callback that will print an error if something goes wrong */
                final CountDownLatch countDownLatch = new CountDownLatch(100);
                producer.send(record, (metadataAsync, exception) -> {
                    if (metadataAsync != null) {
                        long elapsedTime = System.currentTimeMillis() - time;
                        logger.debug("sent record(key= " + record.key() + " value=" + record.value() + ") " +
                                ", meta(partition=" + metadataAsync.partition() + " offset=" + metadataAsync.offset() + ") time=" + elapsedTime + "\n");

                    } else {
                        logger.severeException("Exception occured sending the message" + request, exception);
                    }
                    countDownLatch.countDown();
                });
            }
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

    public Map<String,String> getProducerProps() {
        try {
            Map<String,String> properties = null;
            String producerVar = getAttributeValueSmart(PRODUCER_VARIABLE);
            if (producerVar != null) {
                Process processVO = getProcessDefinition();
                Variable variableVO = processVO.getVariable(producerVar);
                if (variableVO == null)
                    throw new ActivityException("Producer variable '" + producerVar + "' is not defined for process " + processVO.getLabel());
                if (!variableVO.getVariableType().startsWith("java.util.Map"))
                    throw new ActivityException("Producer variable '" + producerVar + "' must be of type java.util.Map");
                Object producerObj = getVariableValue(producerVar);
                if (producerObj != null) {
                    properties = new HashMap<String,String>();
                    for (Object key : ((Map<?,?>)producerObj).keySet()) {
                        properties.put(key.toString(), ((Map<?,?>)producerObj).get(key).toString());
                    }
                }
            }
            return properties;
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
    }

    public Map<String,String> getRecordProps() {
        try {
            Map<String,String> properties = null;
            String producerVar = getAttributeValueSmart(RECORD_VARIABLE);
            if (producerVar != null) {
                Process processVO = getProcessDefinition();
                Variable variableVO = processVO.getVariable(producerVar);
                if (variableVO == null)
                    throw new ActivityException("Record variable '" + producerVar + "' is not defined for process " + processVO.getLabel());
                if (!variableVO.getVariableType().startsWith("java.util.Map"))
                    throw new ActivityException("Record variable '" + producerVar + "' must be of type java.util.Map");
                Object producerObj = getVariableValue(producerVar);
                if (producerObj != null) {
                    properties = new HashMap<String,String>();
                    for (Object key : ((Map<?,?>)producerObj).keySet()) {
                        properties.put(key.toString(), ((Map<?,?>)producerObj).get(key).toString());
                    }
                }
            }
            return properties;
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return null;
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

    void runProducerSync(final int sendMessageCount) throws Exception {
        this.init();
        final Producer<Long, String> producer = kafkaProducer;
        long time = System.currentTimeMillis();

        try {
            for (long index = time; index < time + sendMessageCount; index++) {
                final ProducerRecord<Long, String> record =
                        new ProducerRecord<>(topicName, index,
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
                        new ProducerRecord<>(topicName, index, "Hello MDW Async " + index);
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
    }
}
