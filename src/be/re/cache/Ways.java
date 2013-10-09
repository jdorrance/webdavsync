package be.re.cache;

/**
 * The Ways interface can be used by a multi-way set associative cache. It is
 * represented by a property that must be mutable at all times.
 *
 * Note that a one-way SACache can not hold two objects in the same
 * HashEntry in memory at the same time. Doing comparisons with two such
 * objects requires copying at least one of them after retrieval.
 * @author Werner Donn\u00e9
 */

public interface Ways
{
  public int	getWays	();
  public void	setWays	(int value);
}
