package gossipLearning.protocols;

import gossipLearning.evaluators.FactorizationResultAggregator;
import gossipLearning.interfaces.ModelHolder;
import gossipLearning.interfaces.models.Model;
import gossipLearning.interfaces.models.Partializable;
import gossipLearning.messages.ModelMessage;
import gossipLearning.utils.InstanceHolder;
import gossipLearning.utils.VectorEntry;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class RapidFactorizationProtocol extends FactorizationProtocol {
  protected static final String PAR_MODELPROB = "initModelProbability";
  protected int numberOfSentModels;
  protected final double maxdelay;

  public RapidFactorizationProtocol(String prefix) {
    super(prefix);
    numberOfSentModels = 0;
    maxdelay = (Configuration.getDouble("MAXDELAY")/Network.size()) * Configuration.getDouble("UNITSINSTEP"); ///((MobilTraceChurn)currentNode.getProtocol(churnProtocolID)).unitsInStep;
  }
  
  public RapidFactorizationProtocol(RapidFactorizationProtocol a) {
    super(a);
    numberOfSentModels = a.numberOfSentModels;
    maxdelay = a.maxdelay;
  }
  
  @Override
  public Object clone() {
    return new RapidFactorizationProtocol(this);
  }
  
  @Override
  public void activeThread() {
    //System.out.println(currentNode.getID() + "\t" + numberOfIncomingModels + "\t" + numberOfSentModels);
    // evaluate
    for (int i = 0; i < modelHolders.length; i++) {
      //if (CommonState.r.nextDouble() < evaluationProbability) {
        ((FactorizationResultAggregator)resultAggregator).push(currentProtocolID, i, (int)currentNode.getID(), userModels[i], modelHolders[i], ((ExtractionProtocol)currentNode.getProtocol(exrtactorProtocolID)).getModel());
      //}
    }
    
    // get indices of rated items
    if (indices == null) {
      indices = new TreeSet<Integer>();
    } else {
      indices.clear();
    }
    InstanceHolder instances = ((ExtractionProtocol)currentNode.getProtocol(exrtactorProtocolID)).getInstances();
    for (int i = 0; i < instances.size(); i++) {
      for (VectorEntry e : instances.getInstance(i)) {
        indices.add(e.index);
      }
    }
    
    // send
    if (numberOfIncomingModels == 0) {
      numberOfWaits ++;
    } else {
      numberOfWaits = 0;
    }
    if (numberOfWaits == numOfWaitingPeriods) {
      numberOfIncomingModels = 1;
      numberOfWaits = 0;
      //System.out.println(getClass().getCanonicalName());
    }
    
    for (int id = Math.min(numberOfIncomingModels-numberOfSentModels, capacity); id > 0; id --) {
      for (int i = 0; i < modelHolders.length; i++) {  
        // store the latest models in a new modelHolder
        //Model latestModel = (MatrixBasedModel)modelHolders[i].getModel(modelHolders[i].size() - id);
        Model latestModel = ((Partializable<?>)modelHolders[i].getModel(modelHolders[i].size() - id)).getModelPart(indices);
        latestModelHolder.add(latestModel);
      }
      if (latestModelHolder.size() == modelHolders.length) {
        //System.out.println("SEND: " + currentNode.getID());
        // send the latest models to a random neighbor
        sendToRandomNeighbor(new ModelMessage(currentNode, latestModelHolder, currentProtocolID, false));
        //System.out.println(currentNode.getID() + " SEND act");
      }
      latestModelHolder.clear();
    }
    numberOfIncomingModels = 0;
    numberOfSentModels = 0;
  }
  
  @Override
  protected void updateModels(ModelHolder modelHolder){
    //System.out.println(currentNode.getID() + " RECV");
    super.updateModels(modelHolder);
    for (int i = 0; i < modelHolders.length; i++) {
      // store the latest models in a new modelHolder
      //Model latestModel = (MatrixBasedModel)modelHolders[i].getModel(modelHolders[i].size() - id);
      Model latestModel = ((Partializable<?>)modelHolders[i].getModel(modelHolders[i].size() - 1)).getModelPart(indices);
      latestModelHolder.add(latestModel);
    }
    if (latestModelHolder.size() == modelHolders.length) {
      //System.out.println("SEND(U): " + currentNode.getID());
      // send the latest models to a random neighbor
      sendToRandomNeighbor(new ModelMessage(currentNode, latestModelHolder, currentProtocolID,false));
      //System.out.println(currentNode.getID() + " SEND curr " + currentProtocolID);
    }
    latestModelHolder.clear();
    numberOfSentModels ++;
    //System.out.println(currentNode.getID());
    //System.out.println(numberOfIncomingModels);
  }

  @Override
  protected void sendToRandomNeighbor(ModelMessage message) {
    Linkable overlay = getOverlay();
    List<Node> neighbors = new ArrayList<Node>();
    for (int i = 0; i < overlay.degree(); i++) {
      if (overlay.getNeighbor(i).isUp() && overlay.getNeighbor(i).getID() != currentNode.getID()){
        if (((LearningProtocol)overlay.getNeighbor(i).getProtocol(currentProtocolID)).getSessionLength()-maxdelay >= 0) {
          neighbors.add(overlay.getNeighbor(i));
        }
      }
    }
    if (neighbors.size() > 0) {
      Node randomNode = neighbors.get(CommonState.r.nextInt(neighbors.size()));
      getTransport().send(currentNode, randomNode, message, currentProtocolID);
    }
  }
  
}