/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package StrongHierarchy;

import Agents.AgentMachine;
import jobshop.Operations;

/**
 *
 * @author Anatoli Grishenko <Anatoli.Grishenko@gmail.com>
 */
public class HMCutterPRO extends AgentMachine{
    
    @Override
    public void setup(){
        super.setup();
        myMachine.addCapability(Operations.SLOWCUT, 5);
        myMachine.addCapability(Operations.FASTCUT, 2);
        myMachine.setAvailable(false);
        this.DFAddMyServices(new String []{Operations.SLOWCUT.name(), Operations.FASTCUT.name()});
        Info("Setup");
    }
    
}
