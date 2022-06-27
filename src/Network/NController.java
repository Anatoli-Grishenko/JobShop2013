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

    protected Product prod;
    protected HashMap<String, Product> productQueue;
    ArrayList<Product> doneProduct, receivedProduct;
    ACLMessage producer;
    String nextmachine, conversationID;

    @Override
    public void setup() {
        super.setup();
        productQueue = new HashMap();
        doneProduct = new ArrayList();
        receivedProduct = new ArrayList();
        this.DFAddMyServices(new String[]{"Controller"});
        Info("Setup ");
        Info("Waiting for machines to register in DF");
        this.LARVAwait(1000);
        this.getLayout();
        Info("\n" + layout.toString());
//        this.openXUITTY();
//        this.openRemote();
    }

    @Override
    public void takeDown() {
        super.takeDown();
    }

    @Override
    public void Execute() {
//        Info(this.layout.toString());
        Info("Ready. Queue"); //=" + receivedProduct.size() + " prods.");
//        showSummary();
        inbox = this.LARVAblockingReceive(1000);
        if (inbox != null) {
            switch (inbox.getPerformative()) {
                case ACLMessage.CANCEL:
                    outbox = new ACLMessage(ACLMessage.CANCEL);
                    outbox.setSender(getAID());
                    outbox.setContent("");
                    for (String s : layout.Machines.keySet()) {
                        outbox.addReceiver(new AID(s, AID.ISLOCALNAME));
                    }
                    LARVAsend(outbox);
                    doExit();
                    break;
                case ACLMessage.REQUEST:
                    prod = new Product(inbox.getContent());
                    if (conversationID == null) {
                        conversationID = inbox.getConversationId();
                    }
                    Info("Request " + prod.toString());
                    receivedProduct.add(new Product(inbox.getContent()));
                    prod.nextOperation();
                    productQueue.put(prod.getID(), prod);
                    break;
                case ACLMessage.INFORM:
                default:
                    Info("Ignoring " + ACLMessageTools.fancyWriteACLM(inbox, true));

            }
        } else {
            //Info("Received null");
        }
        if (!productQueue.isEmpty()) {
            prod = productQueue.get(new ArrayList<String>(productQueue.keySet()).get(0));
            Info("Trying to continue " + prod.toString());
            nextmachine = this.callForProposals(prod, conversationID);
            if (nextmachine != null) {
                productQueue.remove(prod.getID());

                Info("Accept propose from " + nextmachine + " due to " + myOpt.name());
                outbox = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                outbox.setSender(this.getAID());
                outbox.setContent(prod.toString());
                outbox.setConversationId(conversationID);
                outbox.addReceiver(new AID(nextmachine, AID.ISLOCALNAME));
                LARVAsend(outbox);
            }
//            this.saveSequenceDiagram("jobshop.seqd");
        }
    }

    public ArrayList<String> getAvailableMachines(Operations op) {
        ArrayList<String> res = new ArrayList();
        for (String s : layout.Capabilities2Machine.get(op)) {
            if (layout.Machines.get(s).isAvailable()) {
                res.add(s);
            }
        }
        return res;
    }

//    public void showSummary() {
//        getLayout();
//        ArrayList<String> machines = new ArrayList(layout.Machines.keySet()),
//                products = new ArrayList(this.productQueue.keySet());
//
//        xuitty.setCursorXY(1, 1);
//        xuitty.textColor(White);
//        xuitty.noprint("QUEUE  ");
//        for (String s : products) {
//            xuitty.noprint("[");
//            printProduct(productQueue.get(s));
//            xuitty.noprint("|" + productQueue.get(s).getCurrentOperation().name() + "]  ");
//        }
//        xuitty.noprint("             ");
//        for (int i = 0; i < machines.size(); i++) {
//            xuitty.setCursorXY(1, 3 + i);
//            xuitty.noprint(machines.get(i) + ": ");
//            xuitty.setCursorXY(10 + ((int) this.getNCycles() * 4) % (xuitty.getXUIWidth() - 30), 3 + i);
//            if (layout.Machines.get(machines.get(i)).getProcessing() == null) { //.isAvailable()) {
//                xuitty.noprint("|   " + "    ");
//            } else {
//                xuitty.noprint("|");
//                printProduct(layout.Machines.get(machines.get(i)).getProcessing());
//                xuitty.noprint("    ");
//            }
//        }
//        int y = 3;
//        xuitty.setCursorXY(xuitty.getXUIWidth() - 30, y++);
//        xuitty.textColor(Green);
//        xuitty.noprint("RECEIVED");
//
//        for (Product p : receivedProduct) {
//            xuitty.setCursorXY(xuitty.getXUIWidth() - 30, y++);
//            printProduct(p);
//            xuitty.noprint(">");
//            for (Operations op : p.getSequence()) {
//                xuitty.noprint(op.name().substring(0, 1));
//            }
//        }
//        y = 3;
//        xuitty.setCursorXY(xuitty.getXUIWidth() - 15, y++);
//        xuitty.textColor(Green);
//        xuitty.noprint("DONE");
//
//        for (Product p : doneProduct) {
//            xuitty.setCursorXY(xuitty.getXUIWidth() - 15, y++);
//            printProduct(p);
//        }
//        xuitty.print("");
//    }
//
//    public void printProduct(Product p) {
//        int color;
//        try {
//            color = Integer.parseInt(p.getID());
//        } catch (Exception ex) {
//            color = 0;
//        }
//        color = color % HTMLColor.values().length + 1;
////        xuitty.noprint(p.getID());
//        xuitty.textColor(HTMLColor.values()[color]).noprint(p.getID()).textColor(White);
//
//    }
}
