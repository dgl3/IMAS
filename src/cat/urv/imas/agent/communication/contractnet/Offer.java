package cat.urv.imas.agent.communication.contractnet;

import cat.urv.imas.agent.communication.util.MessageCreator;
import cat.urv.imas.map.Cell;
import cat.urv.imas.onthology.MessageContent;
import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;

/**
 * Created by Philipp Oliver on 29/1/15.
 */
public class Offer implements java.io.Serializable {
    private AID contractor;
    private int contractNetID;
    private Cell cell;

    public Offer(AID contractor, int contractNetID, Cell cell) {
        this.contractNetID = contractNetID;
        this.contractor = contractor;
        this.cell = cell;
    }

    public Cell getCell() {
        return cell;
    }

    public AID getContractor() {
        return contractor;
    }

    public void reply(Agent sender, int bidValue){
        System.out.println("########## ContractNet Fireman Replies ##########");
        Bid bid = new Bid(contractNetID, bidValue);
        ACLMessage bidMsg = MessageCreator.createPropose(contractor, MessageContent.FIRMEN_CONTRACTNET, bid);
        sender.send(bidMsg);
    }

    public int getContractNetID() {
        return contractNetID;
    }
}
