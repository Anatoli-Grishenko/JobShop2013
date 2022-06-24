/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package StrongHierarchy;

import Agents.AgentMachine;
import Agents.AgentMachine;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import java.util.HashMap;
import jobshop.Machine;
import jobshop.Operations;

/**
 *
 * @author Anatoli Grishenko <Anatoli.Grishenko@gmail.com>
 */
public class HMDrillA extends AgentMachine{
    
    @Override
    public void setup(){
        super.setup();
        myMachine.addCapability(Operations.DRILL, 10);
        myMachine.setAvailable(false);
        this.DFAddMyServices(new String []{Operations.DRILL.name()});
        Info("Setup "+this.getClass().getName());
    }

}
