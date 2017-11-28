/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.kafka;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.test.MockRuntimeContext;

/**
 * This is a sample Test program to show how Kafka adapter can be called from a stand alone program
 * -Dmdw.config.location="C:\workspaces\mdw6\mdw\config" -Dmdw.test.user=aa56486
 */
public class KafkaProducerTest {

    private MockRuntimeContext runtimeContext;

    public static void main(String[] args) {
        KafkaProducerTest kafkaTest = new KafkaProducerTest();

        kafkaTest.runtimeContext = new MockRuntimeContext("KafkaTest Activity");
        PropertyManager.getInstance().setStringProperty(PropertyNames.MDW_ASSET_LOCATION, "C:\\workspaces\\mdw6\\mdw-workflow\\assets");

        KafkaAdapter kAdapter = new KafkaAdapter();
        kAdapter.prepare(kafkaTest.runtimeContext);
        Properties producerProps = new Properties();

        //NOTE: coma separated list of server:port ex. localhost:9092,localhost:9093
        String server = "vlddkafkats001.test.intranet:9092,vlddkafkats002.test.intranet:9092,vlddkafkats003.test.intranet:9092";
        System.out.println("BOOTSTRAP_SERVERS_CONFIG : " + server);
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, server);
        producerProps.put(ProducerConfig.CLIENT_ID_CONFIG, "KafkaMDWProducer");
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put("timeout.ms", "100");

        Map<String, String> recordProps = new HashMap<String, String>();
        recordProps.put(KafkaAdapter.KAFKA_TOPIC_NAME, "testMdwInbound");
        String key = "" + System.currentTimeMillis();
        recordProps.put(KafkaAdapter.RECORD_KEY, key);
        recordProps.put(KafkaAdapter.RECORD_PARTITION,  "0");

        String request= "KafkaTest-Request:" + key;
        try {
            kAdapter.directInvoke(producerProps, request, 0, recordProps);
        }
        catch (AdapterException | ConnectionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
