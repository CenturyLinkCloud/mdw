/*
 * Copyright (C) 2018 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.kafka;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.listener.ListenerHelper;
import com.centurylink.mdw.model.listener.Listener;

/**
 * Dynamic Java workflow asset.
 */
public class MDWKafkaListener {

    // Properties outside of kafka consumer
    public static final String KAFKAPOOL_CLASS_NAME = "className";
    public static final String TOPIC_LIST = "topics";
    public static final String POLL_TIMEOUT = "poll";
    public static final String USE_THREAD_POOL = "useThreadPool";
    private static final String XML_WRAPPER = "xmlWrapper";

    // Kafka consumer properties
    public static final String GROUP_ID = "group.id";
    public static final String HOST_LIST = "bootstrap.servers";
    public static final String AUTO_COMMIT = "enable.auto.commit";
    public static final String KEY_DESERIALIZER = "key.deserializer";
    public static final String VALUE_DESERIALIZER = "value.deserializer";

    private static final String MDW_KAFKA_PKG = "com.centurylink.mdw.kafka";

    protected List<String> topics;
    protected int poll_timeout;
    protected boolean use_thread_pool;
    protected boolean auto_commit;
    protected String kafkaListenerName;
    protected List<String> hostList;
    protected Properties initParameters = null;
    protected String xmlWrapper;
    protected KafkaConsumer<String, String> consumer = null;

    // STANDALONE private StandaloneLogger logger;
    /* WITHENGINE */private StandardLogger logger;

    private boolean _terminating;
    private ThreadPoolProvider thread_pool; // null if processing using the same thread

    public void init(String listenerName, Properties parameters) throws PropertyException {

        try {
            if (!parameters.containsKey(HOST_LIST))
                throw new Exception("Missing bootstrap.servers property for Kafka listener");
            else {
                String[] hostarray = parameters.getProperty(HOST_LIST).split(",");
                if (hostarray != null && hostarray.length > 0)
                    hostList = Arrays.asList(hostarray);
                else
                    throw new Exception("Missing value for bootstrap.servers property for Kafka listener");
            }
        }
        catch (Exception e) {
            throw new PropertyException(e.getMessage());
        }

        // STANDALONE logger = StandaloneLogger.getSingleton();
        /* WITHENGINE */logger = LoggerUtil.getStandardLogger();

        logger.info("Starting Kafka consumer Listener name = " + listenerName);

        kafkaListenerName = listenerName;

        if (!parameters.containsKey(TOPIC_LIST))
            topics = Arrays.asList(listenerName);
        else
        {
            String[] topicList = parameters.getProperty(TOPIC_LIST).split(",");
            if (topicList != null && topicList.length > 0)
                topics = Arrays.asList(topicList);
            else
                topics = Arrays.asList(listenerName);
        }

        if (!parameters.containsKey(GROUP_ID)) {
            logger.warn("No group.id property specified for Kafka consumer " + kafkaListenerName + ", using \"" + kafkaListenerName + "\"...");
            parameters.put(GROUP_ID, kafkaListenerName);
        }

        if (!parameters.containsKey(AUTO_COMMIT))
            parameters.put(AUTO_COMMIT, "true");

        if (!parameters.containsKey(KEY_DESERIALIZER))
            parameters.put(KEY_DESERIALIZER, "org.apache.kafka.common.serialization.StringDeserializer");

        if (!parameters.containsKey(VALUE_DESERIALIZER))
            parameters.put(VALUE_DESERIALIZER, "org.apache.kafka.common.serialization.StringDeserializer");

  //      hostList = parameters.getProperty(HOST_LIST);
        auto_commit = getBooleanProperty(parameters, AUTO_COMMIT, true);
        poll_timeout = 1000 * getIntegerProperty(parameters, POLL_TIMEOUT, 60);
        use_thread_pool = getBooleanProperty(parameters, USE_THREAD_POOL, false);
        xmlWrapper = parameters.getProperty(XML_WRAPPER);

        initParameters = parameters;
        // Remove non-Kafka properties
        initParameters.remove(KAFKAPOOL_CLASS_NAME);
        initParameters.remove(USE_THREAD_POOL);
        initParameters.remove(TOPIC_LIST);
        initParameters.remove(POLL_TIMEOUT);
        initParameters.remove(XML_WRAPPER);
    }

    protected int getIntegerProperty(Properties parameters, String propname, int defval) {
        String v = parameters.getProperty(propname);
        if (v == null)
            return defval;
        try {
            return Integer.parseInt(v);
        }
        catch (Exception e) {
            return defval;
        }
    }

    protected boolean getBooleanProperty(Properties parameters, String propname, boolean defval) {
        String v = parameters.getProperty(propname);
        if (defval)
            return !"false".equalsIgnoreCase(v);
        else
            return "true".equalsIgnoreCase(v);
    }

    public void shutdown() {
        _terminating = true;
        consumer.wakeup();
    }

    public void start() {
        try {
            if (hostList == null || hostList.isEmpty())
                throw new Exception("Empty list of Kafka servers");

            if (topics == null || topics.isEmpty())
                throw new Exception("Topics not specified for Kafka listener");

            if (logger.isMdwDebugEnabled())
            {
                logger.mdwDebug("Kafka listener=" + kafkaListenerName);
                logger.mdwDebug("hostList*=" + hostList);
                logger.mdwDebug("topics*=" + topics);
            }

            // This is so that all dependent class from kafka-clients jar are found during consumer creation
            ClassLoader cl = ApplicationContext.setContextCloudClassLoader(PackageCache.getPackage(MDW_KAFKA_PKG));

            consumer = new KafkaConsumer<>(initParameters);
            consumer.subscribe(topics);

            // Create a dummy producer on this same thread to prevent IllegalAccessError when sending messages, which was blocking
            initParameters.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            initParameters.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            KafkaProducer<Object, Object> dummyProducer = new KafkaProducer<>(initParameters);

            ApplicationContext.resetContextClassLoader(cl);

            dummyProducer.close();

            _terminating = false;
            if (!use_thread_pool) {
                thread_pool = null;
                while (!_terminating) {
                    try {
                        ConsumerRecords<String, String> records = consumer.poll(Long.MAX_VALUE);
                        for (ConsumerRecord<String, String> record : records) {
                            process_message(record);
                            if (!auto_commit)
                                consumer.commitSync(Collections.singletonMap(new TopicPartition(record.topic(), record.partition()), new OffsetAndMetadata(record.offset() + 1)));
                        }
                    }
                    catch (WakeupException e) {
                        if (!_terminating) throw e;
                        consumer.close();
                    }
                }
            }
            else {
                thread_pool = ApplicationContext.getThreadPoolProvider();
                ConsumerRecords<String, String> records = null;
                KafkaRunnable runnable = null;
                while (!_terminating) {

                    try {
                        records = consumer.poll(Long.MAX_VALUE);

                        for (ConsumerRecord<String, String> record : records) {
                            runnable = new KafkaRunnable(record);

                            // Loop while runnable != null, which would mean no thread available to process record
                            while (runnable != null) {
                                if (thread_pool.execute(ThreadPoolProvider.WORKER_LISTENER, "MDWKafkaListener", runnable)) {
                                    runnable = null;
                                    if (!auto_commit)
                                        consumer.commitSync(Collections.singletonMap(new TopicPartition(record.topic(), record.partition()), new OffsetAndMetadata(record.offset() + 1)));
                                }
                                else {
                                    String msg = "MDWKafka listener " + ThreadPoolProvider.WORKER_LISTENER + " has no thread available";
                                    // make this stand out
                                    logger.severeException(msg, new Exception(msg));
                                    logger.info(thread_pool.currentStatus());
                                    Thread.sleep(poll_timeout);  // Will try to process same request after waking up
                                }
                            }
                        }
                    }
                    catch (WakeupException e) {
                        if (!_terminating) throw e;
                        consumer.close();
                    }
                    catch (InterruptedException e) {
                        logger.info(this.getClass().getName() + " interrupted.");
                    }
                }
            }
            consumer.close();
        }
        catch (Exception e) {
            logger.severeException(e.getMessage(), e);
        }
        finally {
            if (consumer != null) consumer.close();  // Cleanup consumer resources
            if (!_terminating) this.start();  // Restart the consumer if a failure occurred, besides instance is shutting down
        }
    }

    protected void process_message(ConsumerRecord<String, String> record) {
        try {
            String message = record.value();
            if (xmlWrapper != null)
                message = wrapXml(message, xmlWrapper);

            if (logger.isDebugEnabled())
            {
                logger.debug("Kafka listener " + kafkaListenerName + " consuming record:");
                logger.debug("  Topic=" + record.topic());
                logger.debug("  Key=" + record.key());
                logger.debug("  Value=" + record.value());
                logger.debug("  Offset=" + record.offset());
            }

            Map<String, String> metaInfo = new HashMap<String, String>();
            /* WITHENGINE */metaInfo.put(Listener.METAINFO_PROTOCOL, Listener.METAINFO_PROTOCOL_KAFKA);
            /* WITHENGINE */metaInfo.put(Listener.METAINFO_SERVICE_CLASS, this.getClass().getName());
            /* WITHENGINE */metaInfo.put(Listener.METAINFO_REQUEST_ID, record.key());
            /* WITHENGINE */metaInfo.put("Topic", record.topic());
            /* WITHENGINE */ListenerHelper helper = new ListenerHelper();
            /* WITHENGINE */String response = helper.processEvent(message, metaInfo);

            if (logger.isDebugEnabled())
            {
                logger.debug("Kafka listener " + kafkaListenerName + " completed processing of record with key " + record.key() + " on topic " + record.topic());
                logger.debug("  Response="+ response);
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }
    }

    protected class KafkaRunnable implements Runnable {
        private ConsumerRecord<String, String> _record ;

        KafkaRunnable(ConsumerRecord<String, String> record) {
            _record = record;
        }

        public void run() {
            process_message(_record);
        }
    }

    protected String wrapXml(String data, String wrapper) {
        StringBuffer sb = new StringBuffer();
        sb.append("<").append(wrapper).append("><![CDATA[");
        sb.append(data);
        sb.append("]]></").append(wrapper).append(">");
        return sb.toString();
    }

    private static Properties loadConfig(String configFile) throws Exception {
        Properties props = new Properties();
        FileInputStream in = new FileInputStream(configFile);
        props.load(in);
        in.close();
        return props;
    }

    public static void main(String args[]) throws Exception {
        MDWKafkaListener kafkaserver = new MDWKafkaListener();
        Properties config = loadConfig("kafkalistener.config");
        kafkaserver.init("StandAlone", config);
        kafkaserver.start();
    }
}