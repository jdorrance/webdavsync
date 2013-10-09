package be.re.xml.stax;

import be.re.io.QueueIOStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;



/**
 * Provides an XML source consisting of StAX events as an InputStream.
 * @author Werner Donn\u00e9
 */

public class SyntheticEventInputStream extends InputStream

{

  private XMLEventReader	reader;
  private QueueIOStream		stream = new QueueIOStream();
  private XMLEventWriter	writer;



  public
  SyntheticEventInputStream(XMLEventReader reader)
  {
    this.reader = reader;

    try
    {
      writer =
        Util.outputFactory.createXMLEventWriter(stream.getOutputStream());
    }

    catch (XMLStreamException e)
    {
      throw new RuntimeException(e);
    }
  }



  public int
  available() throws IOException
  {
    return stream.getInputStream().available();
  }



  private void
  copyEvents(int length) throws IOException
  {
    try
    {
      while (reader.hasNext() && stream.getInputStream().available() < length)
      {
        writer.add(reader.nextEvent());
      }

      writer.flush();
    }

    catch (XMLStreamException e)
    {
      throw new be.re.io.IOException(e);
    }
  }



  public int
  read() throws IOException
  {
    if (available() == 0)
    {
      copyEvents(0x10000);
    }

    return stream.getInputStream().read();
  }



  public int
  read(byte b[]) throws IOException
  {
    return read(b, 0, b.length);
  }



  public int
  read(byte b[], int off, int len) throws IOException
  {
    if (available() < len)
    {
      copyEvents(len);
    }

    return stream.getInputStream().read(b, off, len);
  }

} // SyntheticEventInputStream
