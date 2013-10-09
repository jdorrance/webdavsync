package be.re.xml.stax;

import be.re.xml.stax.EventReaderDelegateBase;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;



/**
 * Records and replays events. If while replaying the recorded events are
 * consumed, the filter continues to read from the delegate.
 * @author Werner Donn\u00e9
 */

public class RecorderEventReaderDelegate extends EventReaderDelegateBase

{

  private List		buffer = new ArrayList();
  private boolean	recording = false;
  private int		replayPosition = 0;



  public
  RecorderEventReaderDelegate()
  {
  }



  public
  RecorderEventReaderDelegate(XMLEventReader reader)
  {
    super(reader);
  }



  public void
  clear()
  {
    buffer.clear();
    replayPosition = 0;
  }



  public boolean
  hasNext()
  {
    return replayPosition < buffer.size() || getParent().hasNext();
  }



  public XMLEvent
  nextEvent() throws XMLStreamException
  {
    XMLEvent	event =
      replayPosition < buffer.size() ?
        (XMLEvent) buffer.get(replayPosition++) : getParent().nextEvent();

    if (recording)
    {
      buffer.add(event);
      ++replayPosition;
    }

    setCurrentEvent(event);

    return event;
  }



  public XMLEvent
  peek() throws XMLStreamException
  {
    return
      replayPosition < buffer.size() ?
        (XMLEvent) buffer.get(replayPosition) : getParent().peek();
  }



  public void
  replay()
  {
    if (!recording)
    {
      replayPosition = 0;
    }
  }



  public boolean
  replayDone()
  {
    return replayPosition == buffer.size();
  }



  public void
  stopRecording()
  {
    recording = false;
  }



  public void
  startRecording()
  {
    recording = true;
  }

} // RecorderEventReaderDelegate
