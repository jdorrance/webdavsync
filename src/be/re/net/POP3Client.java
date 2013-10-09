package be.re.net;

import be.re.gui.util.InteractiveAuthenticator;
import be.re.io.ReadLineInputStream;
import be.re.util.PBEException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.net.ssl.SSLSocketFactory;



public class POP3Client

{

  private static Authenticate	authenticator;
  private ReadLineInputStream	in = null;
  private Message[]		messages = null;
  private OutputStream		out = null;



  static
  {
    try
    {
      authenticator = new BasicAuthenticator(new InteractiveAuthenticator());
    }

    catch (PBEException e)
    {
      throw new RuntimeException(e);
    }
  }



  private static void
  checkLine(String line) throws ProtocolException
  {
    if (line == null)
    {
      throw new ProtocolException("Socket closed by peer");
    }

    if (line.substring(0, Math.min(line.length(), 4)).equalsIgnoreCase("-ERR"))
    {
      throw new ProtocolException(line.substring(5));
    }
  }



  public synchronized void
  close() throws IOException
  {
    if (out != null)
    {
      out.write("quit\r\n".getBytes());
      out.close();
      out = null;
    }

    if (in != null)
    {
      in.close();
      in = null;
    }
  }



  protected void
  finalize() throws Throwable
  {
    try
    {
      close();
    }

    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }



  public static Authenticate
  getAuthenticator()
  {
    return authenticator;
  }



  public Message[]
  getMessages()
  {
    return messages == null ? new Message[0] : messages;
  }



  private static int
  getNumberOfMessages(String s)
  {
    StringTokenizer	tokenizer = new StringTokenizer(s, " ");

    tokenizer.nextToken();

    return Integer.parseInt(tokenizer.nextToken());
  }



  public synchronized void
  open(String host, int port) throws IOException, ProtocolException
  {
    open(host, port, null, null, false);
  }



  public synchronized void
  open(String host, int port, boolean ssl) throws IOException, ProtocolException
  {
    open(host, port, null, null, true);
  }



  public synchronized void
  open(String host, int port, String username)
    throws IOException, ProtocolException
  {
    open(host, port, username, null, false);
  }



  public synchronized void
  open(String host, int port, String username, boolean ssl)
    throws IOException, ProtocolException
  {
    open(host, port, username, null, true);
  }



  public synchronized void
  open(String host, int port, String username, String password)
    throws IOException, ProtocolException
  {
    open(host, port, username, password, false);
  }



  public synchronized void
  open(String host, int port, String username, String password, boolean ssl)
    throws IOException, ProtocolException
  {
    if (username == null || password == null)
    {
      Authenticate	auth = getAuthenticator();

      if (auth != null)
      {
        if (username == null)
        {
          User	user = auth.getUser(host, "pop3", null);

          if (user != null)
          {
            username = user.getUsername();
            password = user.getPassword();
          }
        }
        else
        {
          password = auth.getPassword(host, "pop3", username);
        }
      }
    }

    Socket	socket =
      ssl ?
        SSLSocketFactory.getDefault().createSocket(host, port) :
        new Socket(host, port);

    in = new ReadLineInputStream(socket.getInputStream());
    out = socket.getOutputStream();

    checkLine(readLine());
    sendCommand("user " + username);
    sendCommand("pass " + password);

    String	s = sendCommand("stat");

    try
    {
      messages = new Message[getNumberOfMessages(s)];
    }

    catch (NumberFormatException e)
    {
      close();
      throw e;
    }

    for (int i = 0; i < messages.length; ++i)
    {
      messages[i] = new Message(i + 1);
    }
  }



  private String
  readLine() throws IOException
  {
    try
    {
      byte[]	line = in.readLine();

      return line == null ? null : new String(line);
    }

    catch (IOException e)
    {
      close();
      throw e;
    }
  }



  private String
  sendCommand(String command) throws IOException
  {
    try
    {
      out.write((command + "\r\n").getBytes());
      out.flush();

      String	response = readLine();

      checkLine(response);

      return response;
    }

    catch (IOException e)
    {
      close();
      throw e;
    }
  }



  public static void
  setAuthenticator(Authenticate value)
  {
    authenticator = value;
  }



  public class Message

  {

    private boolean	deleted = false;
    private Headers	headers = null;
    private int		number;



    private
    Message(int number)
    {
      this.number = number;
    }



    public void
    delete() throws IOException
    {
      sendCommand("dele " + String.valueOf(number));
      deleted = true;
    }



    public InputStream
    getBody() throws IOException, ProtocolException
    {
      if (deleted)
      {
        throw new ProtocolException(Util.getResource("pop3_deleted_error"));
      }

      sendCommand("retr " + String.valueOf(number));

      for
      (
        String s = readLine();
        s != null && !s.trim().equals("");
        s = readLine()
      );

      return new UntilDotInputStream(in, false);
    }



    public Headers
    getHeaders() throws IOException
    {
      if (headers == null)
      {
        synchronized (POP3Client.this)
        {
          headers = new Headers();
          readHeaders();
        }
      }

      return headers;
    }



    public int
    getNumber()
    {
      return number;
    }



    public boolean
    isDeleted()
    {
      return deleted;
    }



    private void
    readHeaders() throws IOException
    {
      sendCommand("top " + String.valueOf(number) + " 0");

      String	currentName = null;
      String	currentValue = "";

      for (String s = readLine(); s != null && !s.equals("."); s = readLine())
      {
        if (!s.trim().equals(""))
        {
          if (Character.isWhitespace(s.charAt(0)) || s.indexOf(':') <= 0)
          {
            currentValue += "\n" + s;
          }
          else
          {
            if (currentName != null)
            {
              headers.set(currentName, currentValue);
            }

            currentName = s.substring(0, s.indexOf(':')).trim();
            currentValue = s.substring(s.indexOf(':') + 1).trim();
          }
        }
      }

      if (currentName != null)
      {
        headers.set(currentName, currentValue);
      }

      if (headers.get("X-UIDL").length == 0)
      {
        StringTokenizer	tokenizer =
          new StringTokenizer
          (
            sendCommand("uidl " + String.valueOf(number)),
            " "
          );

        if (tokenizer.countTokens() == 3)
        {
          tokenizer.nextToken();
          tokenizer.nextToken();
          headers.set("X-UIDL", tokenizer.nextToken());
        }
      }
    }

  } // Message

} // POP3Client
