package be.re.cache;

/**
 * The exception can be thrown by an implementation that works
 * with a fixed number of entries and ran out of them. This can happen
 * because the CacheToMedium interface provides for the possibility to keep
 * objects locked into the cache.
 * @see CacheToMedium
 * @author Werner Donn\u00e9
 */

public class CacheFullException extends RuntimeException
{
}
