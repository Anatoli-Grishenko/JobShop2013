/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Network;

import StrongHierarchy.*;
import jobshop.Operations;

/**
 *
 * @author Anatoli Grishenko <Anatoli.Grishenko@gmail.com>
 */
public class NMCutterPRO extends NMachine{
    
    @Override
    public void setup(){
        super.setup();
        myMachine.addCapability(Operations.SLOWCUT, 3, 7);
        myMachine.addCapability(Operations.FASTCUT, 1, 9);
        this.DFAddMyServices(new String []{Operations.SLOWCUT.name(), Operations.FASTCUT.name()});
        Info("Setup");
    }
    
}
