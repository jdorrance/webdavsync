package be.re.cache;

public class BasicMedium implements CacheToMedium

{

  public boolean
  canDispose(Object value)
  {
    return true;
  }



  public void
  dispose(Object value)
  {
    return;
  }



  public boolean
  isDirtyMustRead(Object value)
  {
    return false;
  }



  public boolean
  isDirtyMustWrite(Object value)
  {
    return false;
  }



  public Object
  read(Object key)
  {
    return null;
  }



  public void
  write(Object value)
  {
    return;
  }

} // BasicMedium
