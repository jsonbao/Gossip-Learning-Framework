package gossipLearning.evaluators;

/**
 * This class can compute the root mean squared error.
 * 
 * @author István Hegedűs
 */
public class RMSError extends ValueBasedEvaluator {
  private static final long serialVersionUID = 5356691632259042142L;
  
  public RMSError() {
    super();
  }
  
  public RMSError(RMSError a) {
    super(a);
  }

  @Override
  public Object clone() {
    return new RMSError(this);
  }

  @Override
  public double getValue(double expected, double predicted) {
    return (expected - predicted) * (expected - predicted);
  }

  @Override
  public double postProcess(double meanValue) {
    return Math.sqrt(meanValue);
  }

}