
package io.moquette.server;

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import io.moquette.interception.HazelcastMsg;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by mackristof on 28/05/2016.
 */
public class HazelcastListener implements MessageListener<HazelcastMsg> {

    private static final Logger LOG = LoggerFactory.getLogger(HazelcastListener.class);

    private final Server server;

    public HazelcastListener(Server server) {
        this.server = server;
    }

    @Override
    public void onMessage(Message<HazelcastMsg> msg) {
        try {
            if (!msg.getPublishingMember().equals(server.getHazelcastInstance().getCluster().getLocalMember())) {
                HazelcastMsg hzMsg = msg.getMessageObject();
                LOG.info(
                        "{} received from hazelcast for topic {} message: {}",
                        hzMsg.getClientId(),
                        hzMsg.getTopic(),
                        hzMsg.getPayload());
                // TODO pass forward this information in somehow publishMessage.setLocal(false);

                MqttQoS qos = MqttQoS.valueOf(hzMsg.getQos());
                MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, qos, false, 0);
                MqttPublishVariableHeader varHeader = new MqttPublishVariableHeader(hzMsg.getTopic(), 0);
                ByteBuf payload = Unpooled.wrappedBuffer(hzMsg.getPayload());
                MqttPublishMessage publishMessage = new MqttPublishMessage(fixedHeader, varHeader, payload);
                server.internalPublish(publishMessage, hzMsg.getClientId());
            }
        } catch (Exception ex) {
            LOG.error("error polling hazelcast msg queue", ex);
        }
    }
}
