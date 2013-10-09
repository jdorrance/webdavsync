package be.re.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Stack;



/**
 * Some JSON utilities.
 * @author Werner Donn\u00e9
 */

public class Util

{

  /**
   * Parses a UTF-8 encoded JSON-stream. It returns either a
   * <code>java.util.Map</code>, a <code>java.util.List</code> or a value as a
   * <code>java.lang.Object</code>.
   */

  public static Object
  readJson(InputStream in) throws IOException
  {
    JsonFactory		factory = new JsonFactory();

    factory.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

    String		fieldName = null;
    JsonParser		parser = factory.createJsonParser(in);
    Object		result = null;
    Stack		stack = new Stack();
    JsonToken		token;
    Object		value = null;

    while ((token = parser.nextToken()) != null)
    {
      switch (token)
      {
        case END_ARRAY:
        case END_OBJECT:
          stack.pop();
          break;

        case FIELD_NAME:
          fieldName = parser.getCurrentName();
          break;

        case START_ARRAY:
          value = new ArrayList();
          break;

        case START_OBJECT:
          value = new HashMap<String,Object>();
          break;

        case VALUE_FALSE:
          value = new Boolean(false);
          break;

        case VALUE_NULL:
          value = null;
          break;

        case VALUE_NUMBER_FLOAT:
          value = new Double(parser.getDoubleValue());
          break;

        case VALUE_NUMBER_INT:
          value = new Long(parser.getLongValue());
          break;

        case VALUE_STRING:
          value = parser.getText();
          break;

        case VALUE_TRUE:
          value = new Boolean(true);
          break;
      }

      if (fieldName != null && value != null)
      {
        ((Map<String,Object>) stack.peek()).put(fieldName, value);
        fieldName = null;
      }
      else
      {
        if (value != null)
        {
          if (stack.isEmpty())
          {
            result = value;
          }
          else
          {
            ((List) stack.peek()).add(value);
          }
        }
      }

      if (value != null)
      {
        if (value instanceof Map || value instanceof List)
        {
          stack.push(value);
        }

        value = null;
      }
    }

    return result;
  }



  public static void
  writeJson(Object data, OutputStream out) throws IOException
  {
    PrintWriter	writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));

    writeJson(data, writer);
    writer.flush();
  }



  private static void
  writeJson(Object data, PrintWriter out)
  {
    if (data instanceof Map)
    {
      writeJson((Map<String,Object>) data, out);
    }
    else
    {
      if (data instanceof List)
      {
        writeJson((List) data, out);
      }
      else
      {
        if (data instanceof String)
        {
          out.print('"');
        }

        out.print(data);

        if (data instanceof String)
        {
          out.print('"');
        }
      }
    }
  }



  private static void
  writeJson(Map<String,Object> data, PrintWriter out)
  {
    String[]	names = data.keySet().toArray(new String[0]);

    out.println('{');

    for (int i = 0; i < names.length; ++i)
    {
      out.print("  \"");
      out.print(names[i]);
      out.print("\": ");
      writeJson(data.get(names[i]), out);

      if (i < names.length - 1)
      {
        out.print(',');
      }

      out.println();
    }

    out.print('}');
  }



  private static void
  writeJson(List data, PrintWriter out)
  {
    out.println('[');

    for (int i = 0; i < data.size(); ++i)
    {
      writeJson(data.get(i), out);

      if (i < data.size() - 1)
      {
        out.print(',');
      }

      out.println();
    }

    out.print(']');
  }

} // Util
