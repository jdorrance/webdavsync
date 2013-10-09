package be.re.net;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;



/**
 * This class represents an origin server cookie according to RFC 2109.
 * @author Werner Donn\u00e9
 */

public class ServerCookie

{

  private String	comment = null;
  private String	domain = null;
  private int		maxAge = -1;
  private String	name;
  private String	path = null;
  private boolean	secure = false;
  private String	value;
  private int   	version = 0;



  public
  ServerCookie(String name, String value)
  {
    this.name = name;
    this.value = value;
  }



  public void
  addToHeaders(Headers headers)
  {
    headers.add("Set-Cookie", toString());
  }



  /**
   * The default value is <code>null</code>.
   */

  public String
  getComment()
  {
    return comment;
  }



  public static ServerCookie[]
  getCookies(Headers headers)
  {
    String[]	cookies = headers.get("Set-Cookie");
    List	result = new ArrayList();

    for (int i = 0; i < cookies.length; ++i)
    {
      StringTokenizer	cookieTokenizer = new StringTokenizer(cookies[i], ",");

      while (cookieTokenizer.hasMoreTokens())
      {
        StringTokenizer	attributeTokenizer =
          new StringTokenizer(cookieTokenizer.nextToken(), ";");
        ServerCookie	cookie = null;

        while (attributeTokenizer.hasMoreTokens())
        {
          String	attribute = attributeTokenizer.nextToken();
          String	name = attribute.substring(0, attribute.indexOf('='));
          String	value =
            attribute.substring(attribute.indexOf('=') + 1);

          if (cookie == null)
          {
            cookie = new ServerCookie(name, value);
          }
          else
          {
            if (name.equalsIgnoreCase("Comment"))
            {
              cookie.setComment(value);
            }
            else
            {
              if (name.equalsIgnoreCase("Domain"))
              {
                cookie.setDomain(value);
              }
              else
              {
                if (name.equalsIgnoreCase("Max-Age"))
                {
                  cookie.setMaxAge(Integer.parseInt(value));
                }
                else
                {
                  if (name.equalsIgnoreCase("Path"))
                  {
                    cookie.setPath(value);
                  }
                  else
                  {
                    if (name.equalsIgnoreCase("Secure"))
                    {
                      cookie.setSecure(true);
                    }
                    else
                    {
                      if (name.equalsIgnoreCase("Version"))
                      {
                        cookie.setVersion(Integer.parseInt(value));
                      }
                    }
                  }
                }
              }
            }
          }
        }

        result.add(cookie);
      }
    }

    return (ServerCookie[]) result.toArray(new ServerCookie[result.size()]);
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



  /**
   * The default value is -1, which means the attribute is excluded.
   */

  public int
  getMaxAge()
  {
    return maxAge;
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



  /**
   * The default value is <code>false</code>, which means the attibute is
   * excluded.
   */

  public boolean
  getSecure()
  {
    return secure;
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
  setComment(String comment)
  {
    this.comment = comment;
  }



  public void
  setDomain(String domain)
  {
    this.domain = domain;
  }



  public void
  setMaxAge(int maxAge)
  {
    this.maxAge = maxAge;
  }



  public void
  setPath(String path)
  {
    this.path = path;
  }



  public void
  setSecure(boolean secure)
  {
    this.secure = secure;
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



  public String
  toString()
  {
    String	result = "NAME=" + getName();

    if (getComment() != null)
    {
      result += ";Coment=" + getComment();
    }

    if (getDomain() != null)
    {
      result += ";Domain=" + getDomain();
    }

    if (getMaxAge() != -1)
    {
      result += ";Max-Age=" + String.valueOf(getMaxAge());
    }

    if (getPath() != null)
    {
      result += ";Path=" + getPath();
    }

    if (getSecure())
    {
      result += ";Secure";
    }

    return result + ";Version=" + String.valueOf(getVersion());
  }

} // ServerCookie
