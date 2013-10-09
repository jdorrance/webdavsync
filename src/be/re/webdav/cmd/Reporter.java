package be.re.webdav.cmd;

import be.re.gui.util.InteractiveWriter;
import be.re.io.StreamConnector;
import be.re.net.Headers;
import be.re.util.Mailcap;
import be.re.webdav.Client;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import org.w3c.dom.Element;



/**
 * @author Werner Donn\u00e9
 */

public class Reporter

{

  private InteractiveWriter	interactive;
  private PrintWriter		writer;



  public
  Reporter()
  {
    interactive =
      InteractiveWriter.
        open(be.re.gui.util.Util.getResource("title_execution_report"));
    writer = new PrintWriter(interactive);
  }



  public
  Reporter(Writer writer)
  {
    this.writer = new PrintWriter(writer);
  }



  public void
  close() throws IOException
  {
    writer.close();
  }



  private void
  displayInternalServerError(Client.Response response) throws IOException
  {
    InputStream	body = response.getBody();

    if (body == null)
    {
      return;
    }

    File	tmp =
      be.re.io.Util.
        createTempFile("error.", getExtension(response.getHeaders()));

    try
    {
      StreamConnector.copy(body, new FileOutputStream(tmp));

      Process	process =
        Mailcap.exec
        (
          tmp.toURL(),
          response.getHeaders().get("Content-Type").length == 0 ?
            null : response.getHeaders().get("Content-Type")[0]
        );

      if (process != null)
      {
        process.waitFor();
      }
      else
      {
        writer.println(be.re.webdav.Util.getReasonPhrase(response));
      }
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }

    finally
    {
      tmp.delete();
    }
  }



  private static String
  extractWarning(String warning)
  {
    int	index = warning.indexOf(' ');

    return
      index == -1 || !be.re.util.Util.isInteger(warning.substring(0, index)) ?
        null :
        (
          warning.substring(index).trim().equals("") ?
            null : warning.substring(index).trim()
        );
  }



  private static String
  getExtension(Headers headers)
  {
    String	type =
      headers.get("Content-Type").length == 0 ?
        "" : headers.get("Content-Type")[0];

    return
      type == null ?
        "" :
        (
          type.equalsIgnoreCase("text/plain") ?
            ".txt" :
            (
              type.equalsIgnoreCase("text/html") ?
                ".html" :
                (type.equalsIgnoreCase("application/xhtml+xml") ? ".xhtml" : "")
            )
        );
  }



  public void
  raise()
  {
    if (interactive != null)
    {
      interactive.raise();
    }
  }



  public boolean
  report(URL url, Client.Response response) throws IOException
  {
    return report(url, response);
  }



  public boolean
  report(URL url, Client.Response response, boolean reportAsError)
    throws IOException
  {
    return report(url, response, reportAsError);
  }



  public boolean
  report(URL url, Client.Response response, String subject) throws IOException
  {
    return report(url, response, subject);
  }



  public void
  report(URL url, String message, boolean error) throws IOException
  {
    if (error && interactive != null)
    {
      interactive.setErrorMode(true);
      interactive.raise();
    }

    writer.println
    (
      (
        url != null ?
          (
            be.re.net.Util.unescapeUriSpecials
            (
              be.re.net.Util.isComposedUrl(url) ?
                be.re.net.Util.extractComposedUrlEntry(url) : url.getFile()
            ) + (message != null ? ": " : "")
          ) : ""
      ) + (message != null && error ? (Util.getResource("ERROR") + ": ") : "") +
        (message != null ? message : "")
    );

    if (interactive != null)
    {
      interactive.setErrorMode(false);
    }
  }



  public boolean
  report
  (
    URL			url,
    Client.Response	response,
    String		subject,
    boolean		reportAsError
  ) throws IOException
  {
    if (response.getStatusCode() == 207)
    {
      return reportMultistatus(url, response, subject, reportAsError);
    }

    String	prefix = null;

    if (url != null)
    {
      prefix =
        be.re.net.Util.unescapeUriSpecials
        (
          be.re.net.Util.isComposedUrl(url) ?
            ("/" + be.re.net.Util.extractComposedUrlEntry(url)) :
            url.getFile()
        ) + (subject != null ? (", " + subject) : "") + ": ";

      writer.print(prefix);
    }

    boolean	error = false;

    if (response.getStatusCode() >= 400 && reportAsError)
    {
      if (interactive != null)
      {
        interactive.setErrorMode(true);
        interactive.raise();
      }

      writer.print(Util.getResource("ERROR") + ": ");
      error = true;
    }

    if
    (
      response.getStatusCode() == 403	||
      response.getStatusCode() == 409	||
      response.getStatusCode() == 507
    )
    {
      String	message = be.re.webdav.Util.getPreconditionMessage(response);

      writer.println
      (
        !"".equals(message) ?
          message : be.re.webdav.Util.getReasonPhrase(response)
      );
    }
    else
    {
      writer.println(be.re.webdav.Util.getReasonPhrase(response));
    }

    if (response.getStatusCode() == 500)
    {
      displayInternalServerError(response);
    }

    String[]	warnings = response.getHeaders().get("Warning");

    for (int i = 0; i < warnings.length; ++i)
    {
      String	message = extractWarning(warnings[i]);

      if (message != null)
      {
        if (prefix != null)
        {
          writer.print(prefix);
        }

        writer.print(Util.getResource("WARNING") + ": ");
        writer.println(message);
      }
    }

    if (interactive != null)
    {
      interactive.setErrorMode(false);
    }

    return error;
  }



  private boolean
  reportMultistatus
  (
    URL			url,
    Client.Response	response,
    String		subject,
    boolean		reportAsError
  ) throws IOException
  {
    final boolean[]		errors = new boolean[1];
    final Client.Response	r = response;
    final boolean		rep = reportAsError;
    final String		s = subject;
    final PrintWriter		w = writer;

    be.re.webdav.Util.reportMultistatus
    (
      response,
      new be.re.webdav.Util.ReportMultistatus()
      {
        public boolean
        report(URL href, int code, Element error, String description)
          throws IOException
        {
          if (href != null)
          {
            w.print
            (
              be.re.net.Util.unescapeUriSpecials
              (
                be.re.net.Util.isComposedUrl(href) ?
                  ("/" + be.re.net.Util.extractComposedUrlEntry(href)) :
                  href.getFile()
              ) + (s != null ? (", " + s) : "") + ": "
            );
          }

          if (code >= 400 && rep)
          {
            if (interactive != null)
            {
              interactive.setErrorMode(true);
              interactive.raise();
            }

            w.print(Util.getResource("ERROR") + ": ");
            errors[0] = true;
          }

          if ((code == 403 || code == 409) && error != null)
          {
            w.println(be.re.webdav.Util.getPreconditionMessage(error));
          }
          else
          {
            w.println
            (
              description != null ?
                description :
                be.re.webdav.Util.getReasonPhrase(r.getMethod(), code)
            );
          }

          if (interactive != null)
          {
            interactive.setErrorMode(false);
          }

          return true;
        }
      }
    );

    return errors[0];
  }

} // Reporter
