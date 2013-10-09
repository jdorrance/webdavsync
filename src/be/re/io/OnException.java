package be.re.io;

/**
 * This interface goes with the <code>OnExceptionInputStream</code> and
 * <code>OnExceptionOutputStream</code> classes. It can be used to act in a
 * general way when a failure on a communication line occurs.
 * @author Werner Donn\u00e9
 */

public interface OnException

{

  public void	handle	(Throwable e);

} // OnException
