/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Network;

import Agents.AgentJobShop;
import appboot.XUITTY;
import appboot.XUITTY.HTMLColor;
import static appboot.XUITTY.HTMLColor.Green;
import static appboot.XUITTY.HTMLColor.White;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.ArrayList;
import java.util.HashMap;
import jobshop.Operations;
import jobshop.Product;
import messaging.ACLMessageTools;

/**
 *
 * @author Anatoli Grishenko <Anatoli.Grishenko@gmail.com>
 */
public class NController extends AgentJobShop {

    public enum Status {
        WAITPRODUCT, FORWARDPRODUCT, CANCEL, EXIT
    }
    public Status myStatus;

//    protected Product prod;
//    protected HashMap<String, Product> productQueue;
//    ArrayList<Product> doneProduct, receivedProduct;
//    ACLMessage producer;
    String nextMachine, conversationID;

    @Override
    public void setup() {
        super.setup();
        this.DFAddMyServices(new String[]{"Controller"});
        Info("Setup ");
        Info("Waiting for machines to register in DF");
        this.LARVAwait(1000);
        this.getLayout();
        myStatus = Status.WAITPRODUCT;
    }

    @Override
    public void takeDown() {
        super.takeDown();
    }

    @Override
    public void Execute() {
        Info("Ready. Status: " + myStatus.name());
        switch (myStatus) {
            case WAITPRODUCT:
                myStatus = myWaitProduct();
                break;
            case FORWARDPRODUCT:
                myStatus = myForwardProduct();
                break;
            case CANCEL:
                myStatus = myCancel();
                break;
            case EXIT:
                doExit();
                break;
        }
    }

    public Status myWaitProduct() {
        inbox = this.LARVAblockingReceive(SHORTWAIT);
        if (inbox != null) {
            switch (inbox.getPerformative()) {
                case ACLMessage.CANCEL:
                    return Status.CANCEL;
                case ACLMessage.REQUEST:
                    product = new Product(inbox.getContent());
                    Info("Request " + product.toString());
                    product.nextOperation();
                    if (conversationID == null) {
                        conversationID = inbox.getConversationId();
                    }
                    queuedProducts.add(product);
                    return Status.FORWARDPRODUCT;
                default:
                    Info("Ignoring " + ACLMessageTools.fancyWriteACLM(inbox, true));
                    return Status.WAITPRODUCT;
            }
        } else {
            return Status.FORWARDPRODUCT;
        }

    }

    public Status myForwardProduct() {
        if (!queuedProducts.isEmpty()) {
            Info("Forwarding product "+product);
            product = queuedProducts.get(0);
            nextMachine = this.callForProposals(product, conversationID);
            if (nextMachine != null) {
                queuedProducts.remove(0);
                Info("Accept propose from " + nextMachine + " due to " + myOpt.name());
                outbox = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                outbox.setSender(this.getAID());
                outbox.setContent(product.toString());
                outbox.setConversationId(conversationID);
                outbox.addReceiver(new AID(nextMachine, AID.ISLOCALNAME));
                LARVAsend(outbox);
            } else {
                Info("Waiting for a machine to be AVAILABLE for op "+product.getCurrentOperation().name());
            }
        }
        return Status.WAITPRODUCT;
    }

    public Status myCancel() {
        Info("Cancelling");
        outbox = new ACLMessage(ACLMessage.CANCEL);
        outbox.setSender(getAID());
        outbox.setContent("");
        for (String s : Machines) {
            outbox.addReceiver(new AID(s, AID.ISLOCALNAME));
        }
        LARVAsend(outbox);
        return Status.EXIT;
    }

}
