package be.re.xml.stax;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.NoSuchElementException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;



/**
 * An XMLEventReader wrapper around an TransformerHandler.
 * @author Werner Donn\u00e9
 */

public class TransformerHandlerEventReaderDelegate extends EventReaderDelegate

{

  private boolean		closed;
  private FileInputStream	in;
  private boolean		initialized;
  private File			tmpFile;
  private TransformerHandler	handler;
  private XMLEventReader	reader;



  public
  TransformerHandlerEventReaderDelegate(TransformerHandler handler)
  {
    this(handler, null);
  }



  public
  TransformerHandlerEventReaderDelegate
  (
    TransformerHandler	handler,
    XMLEventReader	reader
  )
  {
    super(reader);
    this.handler = handler;
  }



  public void
  close() throws XMLStreamException
  {
    if (!closed)
    {
      reader.close();

      try
      {
        in.close();
      }

      catch (IOException e)
      {
        throw new XMLStreamException(e);
      }

      tmpFile.delete();
      closed = true;
    }
  }



  protected void
  finalize() throws Throwable
  {
    close();
  }



  public String
  getElementText() throws XMLStreamException
  {
    return reader.getElementText();
  }



  public boolean
  hasNext()
  {
    if (!initialized)
    {
      try
      {
        XMLEventWriter	writer = new ContentHandlerEventWriter(handler);

        tmpFile =
          File.createTempFile("TransformerHandlerEventReaderDelegate.", ".xml");
        tmpFile.deleteOnExit();
        handler.setResult(new StreamResult(tmpFile));
        writer.add(getParent());
        writer.flush();
        writer.close();

        in = new FileInputStream(tmpFile);
        reader = Util.inputFactory.createXMLEventReader(in);
        initialized = true;
      }
  
      catch (Exception e)
      {
        throw new RuntimeException(e);
      }
    }

    return reader.hasNext();
  }



  public XMLEvent
  nextEvent() throws XMLStreamException
  {
    if (!hasNext())
    {
      throw new NoSuchElementException();
    }

    return reader.nextEvent();
  }



  public XMLEvent
  nextTag() throws XMLStreamException
  {
    return reader.nextTag();
  }



  public XMLEvent
  peek() throws XMLStreamException
  {
    return reader.peek();
  }

} // TransformerHandlerEventReaderDelegate
