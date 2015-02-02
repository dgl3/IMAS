package cat.urv.imas.agent.communication.util;

import cat.urv.imas.agent.AgentType;
import cat.urv.imas.constants.AgentNames;
import jade.core.AID;

import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Philipp Oliver on 30/1/15.
 */
public class AIDUtil extends AID {

    public static int getLocalId(AID aid){
        return new Scanner(aid.getLocalName()).useDelimiter("\\D+").nextInt();
    }

    public static int getLocalId(String localName){
        return new Scanner(localName).useDelimiter("\\D+").nextInt();
    }

    public static AgentType getType(String localName){
        String type = localName.replaceAll("\\d", "");

        switch (type){
            case AgentNames.ambulance:
                return AgentType.AMBULANCE;
            case AgentNames.hospital:
                return AgentType.HOSPITAL;
            case AgentNames.fireman:
                return AgentType.FIREMAN;
            case AgentNames.car:
                return AgentType.CAR;
            default:
                throw new IllegalArgumentException("Unknown agent type" + type);
        }
    }
}
