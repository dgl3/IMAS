package cat.urv.imas.agent.communication.util;

import cat.urv.imas.onthology.MessageContent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility class to create ACLMessages with an message object.
 *
 * Created by Philipp Oliver on 29/1/15.
 */
public class MessageCreatorUtil {

    public static ACLMessage createRequestMessage(Set<AID> receivers, String contentType, Object contentObj) {
        return createMessage(ACLMessage.REQUEST, receivers, contentType, contentObj);
    }

    public static ACLMessage createProposeMessage(AID receiver, String contentType, Object contentObj) {
        HashSet<AID> receivers = new HashSet<>();
        receivers.add(receiver);
        return createMessage(ACLMessage.PROPOSE, receivers, contentType, contentObj);
    }

    public static ACLMessage createMessage(int type, AID receiver, String contentType, Object contentObj){
        HashSet<AID> receivers = new HashSet<>();
        receivers.add(receiver);
        return createMessage(type, receivers, contentType, contentObj);
    }

    public static ACLMessage createMessage(int type, Set<AID> receivers, String contentType, Object contentObj){

        ACLMessage message = new ACLMessage(type);

        for(AID receiver: receivers){
            message.addReceiver(receiver);
        }

        try {
            Map<String, Object> content = new HashMap<>();
            content.put(contentType, contentObj);
            message.setContentObject((Serializable) content);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return message;
    }
}
