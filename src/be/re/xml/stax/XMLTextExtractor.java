package be.re.xml.stax;

import java.io.IOException;
import java.io.Reader;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;



/**
 * Collects all PCDATA from an XML stream.
 * @author Werner Donn\u00e9
 */

public class XMLTextExtractor extends Reader

{

  private StringBuffer		buffer = new StringBuffer();
  private boolean		eof;
  private XMLEventReader	reader;



  public
  XMLTextExtractor(XMLEventReader reader)
  {
    this.reader = reader;
  }



  public void
  close() throws IOException
  {
    try
    {
      reader.close();
    }

    catch (XMLStreamException e)
    {
      throw new be.re.io.IOException(e);
    }
  }



  public void
  mark(int readAheadLimit) throws IOException
  {
  }



  public boolean
  markSupported()
  {
    return false;
  }



  public int
  read() throws IOException
  {
    char[]	b = new char[1];

    return read(b, 0, b.length) == -1 ? -1 : ((int) 0xffff & b[0]);
  }



  public int
  read(char[] buf) throws IOException
  {
    return read(buf, 0, buf.length);
  }



  public int
  read(char[] buf, int off, int len) throws IOException
  {
    if (eof)
    {
      return -1;
    }

    while (!eof && len > buffer.length())
    {
      try
      {
        if (reader.hasNext())
        {
          XMLEvent	event = reader.nextEvent();

          if (event.isCharacters())
          {
            buffer.append(event.asCharacters().getData());
          }
        }
        else
        {
          eof = true;
        }
      }

      catch (XMLStreamException e)
      {
        throw new be.re.io.IOException(e);
      }
    }

    int	result = Math.min(len, buffer.length());

    buffer.getChars(0, result, buf, off);
    buffer.delete(0, result);

    return result;
  }


  public boolean
  ready() throws IOException
  {
    return false;
  }



  public void
  reset() throws IOException
  {
  }



  public long
  skip(long n) throws IOException
  {
    long	result = read(new char[(int) n]);

    return result == -1 ? 0 : result;
  }

} // XMLTextExtractor
