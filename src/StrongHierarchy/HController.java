/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package StrongHierarchy;

import Agents.AgentJobShop;
import agents.LARVAFirstAgent;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import console.Console;
import static console.Console.blue;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import java.util.HashMap;
import jobshop.Operations;
import jobshop.Product;

/**
 *
 * @author Anatoli Grishenko <Anatoli.Grishenko@gmail.com>
 */
public class HController extends AgentJobShop {

    protected Product prod;
    protected HashMap<String, Product> productQueue;
    ArrayList<Product> doneProduct;
    ACLMessage producer;
    String nextmachine;
    Console terminal;
    int colors[] = {Console.red, Console.blue, Console.green, Console.lightblue, Console.lightred, Console.lightgreen};

    @Override
    public void setup() {
        super.setup();
        productQueue = new HashMap();
        doneProduct = new ArrayList();
        this.DFAddMyServices(new String[]{"Controller"});
        Info("Setup ");
        Info("Waiting for machines to register in DF");
        this.LARVAwait(1000);
        this.getLayout();
        Info("\n" + layout.toString());
        terminal = new Console("Production", 180, 25, 7);
    }

    @Override
    public void Execute() {
//        Info(this.layout.toString());
        Info("Ready");
        inbox = this.LARVAblockingReceive(1000);
        if (inbox != null) {
            switch (inbox.getPerformative()) {
                case ACLMessage.REQUEST:
                    prod = new Product(inbox.getContent());
                    if (prod.getCurrentOperation() == Operations.BEGIN) {
                        prod.nextOperation();
                        productQueue.put(prod.getID(), prod);
                        producer = inbox;
                        outbox = producer.createReply();
                        outbox.setPerformative(ACLMessage.AGREE);
                        outbox.setContent(prod.getID());
                        this.LARVAsend(outbox);
                    }
                    break;
                case ACLMessage.INFORM:
                    prod = new Product(inbox.getContent());
                    productQueue.put(prod.getID(), prod);
                    layout.Machines.get(inbox.getSender().getLocalName()).setAvailable(true);
                    layout.Machines.get(nextmachine).setProcessing(null);
                    break;
                default:
                    Error("Unrecognizable performative " + ACLMessage.getPerformative(inbox.getPerformative()));
                    outbox = producer.createReply();
                    outbox.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                    outbox.setContent("Unrecognizable performative " + ACLMessage.getPerformative(inbox.getPerformative()));
                    LARVAsend(outbox);

            }
        }
        if (!productQueue.isEmpty()) {
            prod = productQueue.get(new ArrayList<String>(productQueue.keySet()).get(0));
            productQueue.remove(prod.getID());
            Info("Trying product " + prod.getID());

            if (prod.getCurrentOperation() == Operations.END) {
                doneProduct.add(prod);
                Info("Done processing product " + prod.getID());
                outbox = producer.createReply();
                outbox.setPerformative(ACLMessage.INFORM);
                outbox.setContent(prod.getID());
                this.LARVAsend(outbox);

            } else {
                Info("Continue processing product " + prod.getID());
                if (layout.Capabilities2Machine.get(prod.getCurrentOperation()) == null) {
                    this.Error("Operation " + prod.getCurrentOperation().name() + " not supported");
                    outbox = producer.createReply();
                    outbox.setPerformative(ACLMessage.REFUSE);
                    outbox.setContent("Operation " + prod.getCurrentOperation().name() + " not supported");
                    this.LARVAsend(outbox);
                } else {
                    if (!this.getAvailableMachines(prod.getCurrentOperation()).isEmpty()) {
                        nextmachine = outOf(this.getAvailableMachines(prod.getCurrentOperation()));
                        Info("Sending product " + prod.getID() + " to machine " + nextmachine);
                        outbox = new ACLMessage(ACLMessage.REQUEST);
                        outbox.setSender(this.getAID());
                        outbox.addReceiver(new AID(nextmachine, AID.ISLOCALNAME));
                        outbox.setContent(prod.toString());
                        this.LARVAsend(outbox);
                        layout.Machines.get(nextmachine).setAvailable(false);
                        layout.Machines.get(nextmachine).setProcessing(prod);
                    } else {
                        Info("Unable to continue with product " + prod.getID() + ": all machines are busy");
                        productQueue.put(prod.getID(), prod);
                    }
                }
            }
        }
        this.saveSequenceDiagram("jobshop.seqd");
        showSummary();
//        this.LARVAwait(1000);
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

    public void showSummary() {
        ArrayList<String> machines = new ArrayList(layout.Machines.keySet()),
                products = new ArrayList(this.productQueue.keySet());
//        terminal.clearScreen();
        terminal.setCursorXY(1, 1);
        for (String s : products) {
            terminal.print("[" + printProduct(productQueue.get(s)) + "|" + productQueue.get(s).getCurrentOperation().name() + "]  ");
        }
        terminal.print("             ");
        for (int i = 0; i < machines.size(); i++) {
            terminal.setCursorXY(1, 3 + i);
            terminal.print(machines.get(i) + ": ");
            terminal.setCursorXY(5 + ((int) this.getNCycles() * 4) % (terminal.getWidth() - 25), 3 + i);
            if (layout.Machines.get(machines.get(i)).isAvailable()) {
                terminal.print("|AVA");
            } else {
                terminal.print("|" + printProduct(layout.Machines.get(machines.get(i)).getProcessing()));
            }
        }
        int y = 1;
        for (Product p : doneProduct) {
            terminal.setCursorXY(terminal.getWidth() - 10, y++);
            terminal.print(p.getID());
        }

    }

    public String printProduct(Product p) {
        String res = "";
        int color = Integer.parseInt(p.getID());
        res+=Console.defText(colors[color])+p.getID();
        return res;
    }
}
