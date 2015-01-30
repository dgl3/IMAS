package cat.urv.imas.agent.communication.util;

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
}
