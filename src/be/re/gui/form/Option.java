package be.re.gui.form;

/**
 * Represents an option in a selection control.
 * @author Werner Donn\u00e9
 */

public class Option

{

  public String		label;
  public boolean	selected;
  public Object		value;



  public
  Option(Object value, String label, boolean selected)
  {
    this.value = value;
    this.label = label;
    this.selected = selected;
  }



  public String
  toString()
  {
    return label;
  }

} // Option
