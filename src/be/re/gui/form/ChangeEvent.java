package be.re.gui.form;

import be.re.util.DispatchableEvent;
import java.util.EventListener;
import java.util.Map;



/**
 * The change event.
 * @author Werner Donn\u00e9
 */

public class ChangeEvent extends DispatchableEvent

{

  private String	field;
  private Map		fields;



  public
  ChangeEvent(Object source, Map fields, String field)
  {
    super(source);
    this.fields = fields;
    this.field = field;
  }



  public void
  dispatch(EventListener listener)
  {
    ((ChangeListener) listener).changed(this);
  }



  public String
  getField()
  {
    return field;
  }



  public Map
  getFields()
  {
    return fields;
  }

} // ChangeEvent
