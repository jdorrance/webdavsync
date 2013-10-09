package be.re.util;

public class List

{

  private Element	head = null;
  private int		size = 0;
  private Element	tail = null;



  public void
  clear()
  {
    head = tail = null;
    size = 0;
  }



  public Element
  getHead()
  {
    return head;
  }



  public int
  getSize()
  {
    return size;
  }



  public Element
  getTail()
  {
    return tail;
  }



  public class Element

  {

    private Element	next = null;
    private Element	previous = null;
    private Object	value;



    public
    Element(Object value)
    {
      this.value = value;
    }



    public void
    append(Element element)
    {
      if (element == null)
      {
        if (tail == null)
        {
          head = tail = this;
        }
        else
        {
          tail.next = this;
          previous = tail;
          next = null;
          tail = this;
        }
      }
      else
      {
        next = element.next;

        if (next != null)
        {
          next.previous = this;
        }

        element.next = this;
        previous = element;

        if (element == tail)
        {
          tail = this;
        }
      }

      ++size;
    }



    public Element
    getNext()
    {
      return next;
    }



    public Element
    getPrevious()
    {
      return previous;
    }



    public Object
    getValue()
    {
      return value;
    }



    public void
    insert(Element element)
    {
      if (element == null)
      {
        if (head == null)
        {
          head = tail = this;
        }
        else
        {
          head.previous = this;
          next = head;
          previous = null;
          head = this;
        }
      }
      else
      {
        previous = element.previous;

        if (previous != null)
        {
          previous.next = this;
        }

        element.previous = this;
        next = element;

        if (element == head)
        {
          head = this;
        }
      }

      ++size;
    }



    public void
    remove()
    {
      if (previous != null)
      {
        previous.next = next;
      }

      if (next != null)
      {
        next.previous = previous;
      }

      if (this == head)
      {
        head = next;
      }

      if (this == tail)
      {
        tail = previous;
      }

      --size;
    }



    public void
    setValue(Object value)
    {
      this.value = value;
    }

  } // Element

} // List
