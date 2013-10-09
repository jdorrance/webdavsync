package be.re.net;

import be.re.util.Compare;
import be.re.util.Sort;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;



/**
 * This class represents a user agent cookie according to RFC 2109.
 * @author Werner Donn\u00e9
 */

public class ClientCookie

{

  private String	domain = null;
  private String	name;
  private String	path = null;
  private String	value;
  private int   	version = 0;



  public
  ClientCookie(String name, String value)
  {
    this.name = name;
    this.value = value;
  }



  public
  ClientCookie(ServerCookie cookie)
  {
    name = cookie.getName();
    value = cookie.getValue();
    domain = cookie.getDomain();
    path = cookie.getPath();
    version = cookie.getVersion();
  }



  public static void
  addToHeaders(Headers headers, ClientCookie[] cookies)
  {
    ClientCookie[]	copy = new ClientCookie[cookies.length];

    System.arraycopy(cookies, 0, copy, 0, copy.length);

    Sort.qsort
    (
      copy,
      new Compare()
      {
        public int
        compare(Object object1, Object object2)
        {
          return
            ((ClientCookie) object1).getPath() == null &&
              ((ClientCookie) object2).getPath() != null ?
              1 :
              (
                ((ClientCookie) object2).getPath() == null &&
                  ((ClientCookie) object1).getPath() != null ?
                  -1 :
                  (
                    ((ClientCookie) object1).getPath() == null &&
                      ((ClientCookie) object2).getPath() == null ?
                      0 :
                      ((ClientCookie) object2).getPath().length() -
                        ((ClientCookie) object1).getPath().length()
                  )
              );
        }
      }
    );

    String	value = "$Version=" + String.valueOf(getLowestVersion(cookies));

    for (int i = 0; i < copy.length; ++i)
    {
      value += ";" + copy[i].toString();
    }

    headers.add("Cookie", value);
  }



  public static ClientCookie[]
  getCookies(Headers headers)
  {
    String[]	cookies = headers.get("Cookie");
    List	result = new ArrayList();
    int		version = -1;

    for (int i = 0; i < cookies.length; ++i)
    {
      StringTokenizer	tokenizer = new StringTokenizer(cookies[i], ";,");

      while (tokenizer.hasMoreTokens())
      {
        String		attribute = tokenizer.nextToken();
        ClientCookie	cookie = null;
        String		name = attribute.substring(0, attribute.indexOf('='));
        String		value =
          attribute.substring(attribute.indexOf('=') + 1);

        if (name.equalsIgnoreCase("$Version"))
        {
          if (version != -1)
          {
            break; // Garbage because there already is a version in the header.
          }
          else
          {
            version = Integer.parseInt(value);
          }
        }
        else
        {
          if
          (
            !name.equalsIgnoreCase("$Path")	&&
            !name.equalsIgnoreCase("$Domain")
          )
          {
            cookie = new ClientCookie(name, value);

            if (version == -1)
            {
              break; // Garbage because $Version must come first.
            }
            else
            {
              cookie.setVersion(version);
              result.add(cookie);
            }
          }
          else
          {
            if (cookie == null)
            {
              break; // Garbage because NAME=VALUE must come first.
            }
            else
            {
              if (name.equalsIgnoreCase("$Path"))
              {
                if (cookie.getDomain() != null)
                {
                  break; // Garbage because $Path must come first.
                }
                else
                {
                  cookie.setPath(value);
                }
              }
              else
              {
                if (cookie.getPath() == null)
                {
                  break; // Garbage because $Path must come first.
                }
                else
                {
                  cookie.setDomain(value);
                }
              }
            }
          }
        }
      }
    }

    return (ClientCookie[]) result.toArray(new ClientCookie[result.size()]);
  }



  /**
   * The default value is <code>null</code>, which means the attribute is
   * excluded.
   */

  public String
  getDomain()
  {
    return domain;
  }



  private static int
  getLowestVersion(ClientCookie[] cookies)
  {
    int	result = Integer.MAX_VALUE;

    for (int i = 0; i < cookies.length; ++i)
    {
      if (cookies[i].getVersion() < result)
      {
        result = cookies[i].getVersion();
      }
    }

    return result;
  }



  public String
  getName()
  {
    return name;
  }



  /**
   * The default value is <code>null</code>, which means the attribute is
   * excluded.
   */

  public String
  getPath()
  {
    return path;
  }



  public String
  getValue()
  {
    return value;
  }



  /**
   * The default value is 0.
   */

  public int
  getVersion()
  {
    return version;
  }



  public void
  setDomain(String domain)
  {
    this.domain = domain;
  }



  public void
  setPath(String path)
  {
    this.path = path;
  }



  public void
  setValue(String value)
  {
    this.value = value;
  }



  public void
  setVersion(int version)
  {
    this.version = version;
  }



  /**
   * The version is excluded from the result because it a mechanism parameter
   * for a user agent cookie, not a cookie parameter.
   */

  public String
  toString()
  {
    String	result = getName() + "=" + getValue();

    if (getPath() != null)
    {
      result += ";Path=" + getPath();
    }

    if (getDomain() != null)
    {
      result += ";$Domain=" + getDomain();
    }

    return result;
  }

} // ClientCookie
