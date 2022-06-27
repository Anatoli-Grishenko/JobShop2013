/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package StrongHierarchy;

import Agents.AgentJobShop;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import jobshop.Operations;
import jobshop.Product;

/**
 *
 * @author Anatoli Grishenko <Anatoli.Grishenko@gmail.com>
 */
public class HController extends AgentJobShop {

    enum Status {
        WAIT, REQUEST, SELECTMACHINE,
        SENDPRODUCT, RECEIVEPRODUCT, CANCEL, EXIT
    };
    Status myStatus;
    String nextMachine;

    @Override
    public void setup() {
        super.setup();
        this.DFAddMyServices(new String[]{"Controller"});
        Info("Setup ");
        Info("Waiting for machines to register in DF");
        this.LARVAwait(1000);
        this.getLayout();
        myStatus = Status.WAIT;
    }

    @Override
    public void takeDown() {
        super.takeDown();
    }

    @Override
    public void Execute() {
        Info("Ready. Status: " + myStatus.name());
        switch (myStatus) {
            case WAIT:
                myStatus = myWait();
                break;
            case REQUEST:
                myStatus = myRequest();
                break;
            case SELECTMACHINE:
                myStatus = mySelectMachine();
                break;
            case SENDPRODUCT:
                myStatus = mySendProduct();
                break;
            case RECEIVEPRODUCT:
                myStatus = myReceiveProduct();
                break;
            case CANCEL:
                myStatus = myCancel();
                break;
            case EXIT:
                doExit();
                break;
        }
    }

    public Status myWait() {
        inbox = this.LARVAblockingReceive(SHORTWAIT);
        if (inbox != null) {
            switch (inbox.getPerformative()) {
                case ACLMessage.CANCEL:
                    return Status.CANCEL;
                case ACLMessage.REQUEST:
                    return Status.REQUEST;
                case ACLMessage.INFORM:
                    return Status.RECEIVEPRODUCT;
                default:
                    Error("Unrecognizable performative " + ACLMessage.getPerformative(inbox.getPerformative()));
                    outbox = inbox.createReply();
                    outbox.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                    outbox.setContent("Unrecognizable performative " + ACLMessage.getPerformative(inbox.getPerformative()));
                    LARVAsend(outbox);
                    return myStatus;
            }
        } else {
            return Status.SELECTMACHINE;
        }
    }

    public Status myRequest() {
        product = new Product(inbox.getContent());
        Info("Request received " + product.toString());
        product.nextOperation();
        queuedProducts.add(product);
        ACLMessage aux = inbox;
        fromWho.put("PRODUCER", aux);
        outbox = inbox.createReply();
        outbox.setPerformative(ACLMessage.AGREE);
        outbox.setContent(product.toString());
        outbox.setInReplyTo(product.getID());
        this.LARVAsend(outbox);
        return Status.SELECTMACHINE;
    }

    public Status mySelectMachine() {
        if (!queuedProducts.isEmpty()) {
            product = queuedProducts.get(0);
            queuedProducts.remove(0);
            Info("Trying product " + product.getID());
            if (product.getCurrentOperation() == Operations.END) {
                doneProducts.add(product);
                Info("Done processing product " + product.getID());
                outbox = fromWho.get("PRODUCER").createReply();
                outbox.setPerformative(ACLMessage.INFORM);
                outbox.setContent(product.toString());
                outbox.setInReplyTo(product.getID());
                this.LARVAsend(outbox);
            } else {
                Info("Continue processing product " + product.getID());
                if (layout.Capabilities2Machine.get(product.getCurrentOperation()) == null) {
                    this.Error("Operation " + product.getCurrentOperation().name() + " not supported");
                    outbox = fromWho.get("PRODUCER").createReply();
                    outbox.setPerformative(ACLMessage.REFUSE);
                    outbox.setContent("Operation " + product.getCurrentOperation().name() + " not supported");
                    this.LARVAsend(outbox);
                } else {
                    if (!this.getAllAvailableMachines(product.getCurrentOperation()).isEmpty()) {
//                        if (myOpt == Optimizations.FASTEST) {
//                            nextMachine = this.getFastestAvailableMachine(product.getCurrentOperation());
//                        } else if (myOpt == Optimizations.CHEAPEST) {
//                            nextMachine = this.getCheapestAvailableMachine(product.getCurrentOperation());
//                        } else {
                            nextMachine = outOf(this.getAllAvailableMachines(product.getCurrentOperation()));
//                        }
                        Info("Selected machine: "+nextMachine);
                        return Status.SENDPRODUCT;
                    } else {
                        Info("Unable to continue with product " + product.getID() + ": all machines are busy");
                        queuedProducts.add(product);
                    }
                }
            }
        }
        return Status.WAIT;
    }

    public Status myReceiveProduct() {
        product = new Product(inbox.getContent());
        queuedProducts.add(product);
        layout.Machines.get(inbox.getSender().getLocalName()).setAvailable(true);
        layout.Machines.get(inbox.getSender().getLocalName()).setProcessing(null);
        return myStatus.SELECTMACHINE;
    }

    public Status mySendProduct() {
        Info("Sending product " + product.getID() + " to machine " + nextMachine);
        outbox = new ACLMessage(ACLMessage.REQUEST);
        outbox.setSender(this.getAID());
        outbox.addReceiver(new AID(nextMachine, AID.ISLOCALNAME));
        outbox.setContent(product.toString());
        this.LARVAsend(outbox);
        layout.Machines.get(nextMachine).setAvailable(false);
        layout.Machines.get(nextMachine).setProcessing(product);
        return Status.WAIT;
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
