/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package StrongHierarchy;

import Agents.AgentJobShop;
import agents.LARVAFirstAgent;
import jade.lang.acl.ACLMessage;
import jobshop.Machine;
import jobshop.Product;
import messaging.ACLMessageTools;
import tools.TimeHandler;

/**
 *
 * @author Anatoli Grishenko <Anatoli.Grishenko@gmail.com>
 */
public class HMachine extends AgentJobShop {

    protected enum Status {
        AVAILABLE, BUSY, CANCEL, EXIT
    }
    protected Status myStatus;
    protected Machine myMachine;
    protected TimeHandler timer;

    @Override
    public void setup() {
        super.setup();
        myMachine = new Machine(getLocalName());
        this.DFSetMyServices(new String[]{"Machine"});
        this.setAvailable();
        timer = null;
        myStatus = Status.AVAILABLE;
    }

    @Override
    public void Execute() {
//        Info("Status: " + myStatus.name());
        switch (myStatus) {
            case AVAILABLE:
                myStatus = myAvailable();
                break;
            case BUSY:
                myStatus = myBusy();
                break;
            case CANCEL:
            case EXIT:
                doExit();
                break;
        }

    }

    public Status myAvailable() {
        inbox = this.LARVAblockingReceive(SHORTWAIT);
        if (inbox != null) {
            switch (inbox.getPerformative()) {
                case ACLMessage.CANCEL:
                    return Status.CANCEL;
                case ACLMessage.REQUEST:
                    product = new Product(inbox.getContent());
                    if (myMachine.isAvailable()) {
//                        if (myOpt == Optimizations.FASTEST) {
//                            product.setCost(product.getCost() + myMachine.processingTime(product.getCurrentOperation()));
//                        } else {
//                            product.setCost(product.getCost() + myMachine.processingCost(product.getCurrentOperation()) * myMachine.processingTime(product.getCurrentOperation()));
//                        }
                        product.setCost(product.getCost() + myMachine.processingCost(product.getCurrentOperation()) * myMachine.processingTime(product.getCurrentOperation()));
                        product.setStart(TimeHandler.Now());
                        Info("Processing product " + product.toString());
                        this.setUnAvailable(product);
                        fromWho.put("CONTROLLER", inbox);
                        timer = new TimeHandler();
                    } else {
                        outbox = inbox.createReply();
                        outbox.setPerformative(ACLMessage.REFUSE);
                        outbox.setContent(product.getID());
                        this.LARVAsend(outbox);
                    }
                    break;
                default:
                    Info("Ignoring " + ACLMessageTools.fancyWriteACLM(inbox, true));
            }
        }
        if (timer != null) {
            return Status.BUSY;
        } else {
            return Status.AVAILABLE;
        }
    }

    public Status myBusy() {
        if (timer.elapsedTimeSecs(new TimeHandler()) >= (int) (myMachine.processingTime(product.getCurrentOperation()))) {
            this.setAvailable();
            timer = null;
            product.nextOperation();
            Info("Done processing product " + product.toString());
            product.setEnd(TimeHandler.Now());
            outbox = fromWho.get("CONTROLLER").createReply();
            outbox.setPerformative(ACLMessage.INFORM);
            outbox.setContent(product.toString());
            outbox.setInReplyTo(product.getID());
            this.LARVAsend(outbox);
        }
        return Status.AVAILABLE;
    }

    public void setAvailable() {
        myMachine.setAvailable(true);
        myStatus = Status.AVAILABLE;
        if (!this.DFHasService(getLocalName(), "AVAILABLE")) {
            this.DFAddMyServices(new String[]{"AVAILABLE"});
        }
        for (String service : this.DFGetAllServicesProvidedBy(getLocalName())) {
            if (service.startsWith(Product.HEAD)) {
                this.DFRemoveMyServices(new String[]{service});
            }
        }
    }

    public void setUnAvailable(Product p) {
        myMachine.setAvailable(false);
        myStatus = Status.BUSY;
        if (this.DFHasService(getLocalName(), "AVAILABLE")) {
            this.DFRemoveMyServices(new String[]{"AVAILABLE"});
        }
        this.DFAddMyServices(new String[]{p.toProcess()});
    }

}
