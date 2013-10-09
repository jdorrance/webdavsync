package be.re.util;

/**
 * The Compare interface is meant for generic algorithms that need to be able to
 * compare two objects. The comparison criterion is kept out of the algorithm.
 * It is also kept out of the objects themselves. This would not be the case
 * if <code> Object.equals </code> were used. That method implies only one
 * criterion.
 *
 * In short, with this interface you can define an order relation on a
 * collection.
 */

public interface Compare
{
  /**
   * The actual compare method.
   * @param object1 the first object in the comparison.
   * @param object2 the second object in the comparison.
   * @return if the first object is smaller than the second the method should
   * return a negative value. If it is larger than the second the value should
   * be positive. If the objects are equal zero should be returned.
   */

  public int	compare	(Object object1, Object object2);
}
