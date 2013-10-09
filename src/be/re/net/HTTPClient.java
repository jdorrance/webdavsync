package be.re.net;

import be.re.gui.util.InteractiveAuthenticator;
import be.re.io.DevNullOutputStream;
import be.re.io.LimitedLengthInputStream;
import be.re.io.ReadLineInputStream;
import be.re.io.StreamConnector;
import be.re.io.TeeOutputStream;
import be.re.io.Tracer;
import be.re.pool.ConnectionPool;
import be.re.pool.Resource;
import be.re.pool.ResourceAdapter;
import be.re.pool.ResourceException;
import be.re.pool.ResourceFactory;
import be.re.util.Base64;
import be.re.util.Equal;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;


/**
 * Provides functions to perform HTTP 1.1 requests. The status code is always
 * put in the header "Status-Code". Status codes of 400 or more cause the
 * <code>HTTPProtocolException</code> to be thrown. Codes in the 300 range can
 * also cause the exception if redirection conditions are not satisfying.
 *
 * When the returned <code>InputStream</code> is completely read the output
 * headers may have changed if the transfer encoding was "chunked".
 *
 * Pipelining, content negotiation and HTTP caching are not implemented.
 * Request and response headers are part of the <code>request</code> method,
 * which enables a content negotiation and HTTP caching implementation outside
 * of this class.
 * @author Werner Donn\u00e9
 */

public class HTTPClient

{

  public static final String	ACL = "ACL";
  public static final String	AUDIT = "AUDIT";
  public static final String	BIND = "BIND";
  public static final String	CHECKIN = "CHECKIN";
  public static final String	CHECKOUT = "CHECKOUT";
  public static final String	COLLAPSE = "COLLAPSE";
  public static final String	COPY = "COPY";
  public static final String	DELETE = "DELETE";
  public static final String	GET = "GET";
  public static final String	HEAD = "HEAD";
  public static final String	INDEX = "INDEX";
  public static final String	LABEL = "LABEL";
  public static final String	LOCK = "LOCK";
  public static final String	MERGE = "MERGE";
  public static final String	MKACTIVITY = "MKACTIVITY";
  public static final String	MKCOL = "MKCOL";
  public static final String	MKVIEW = "MKVIEW";
  public static final String	MOVE = "MOVE";
  public static final String	OPTIONS = "OPTIONS";
  public static final String	POST = "POST";
  public static final String	PROPFIND = "PROPFIND";
  public static final String	PROPPATCH = "PROPPATCH";
  public static final String	PUT = "PUT";
  public static final String	REBIND = "REBIND";
  public static final String	REPORT = "REPORT";
  public static final String	TRACE = "TRACE";
  public static final String	UNBIND = "UNBIND";
  public static final String	UNCHECKOUT = "UNCHECKOUT";
  public static final String	UNLOCK = "UNLOCK";
  public static final String	VERSION_CONTROL = "VERSION-CONTROL";

  private static final int	CHUNK_SIZE = 0x10000;
  private static final int	MAX_REDIRECTIONS = 5;
  private static final int	MAX_RETRIES = 2;
  private static final int	TIME_DELTA = 10000;

  private static Authenticate		authenticator;
  private static final String[]		compressedTypes =
    {
      "application/pdf", "application/zip", "audio", "image/png", "image/jpg",
        "image/jpeg", "video"
    };
  private static final ConnectionPool	connectionPool =
    new ConnectionPool
    (
      new ResourceFactory()
      {
        public ResourceAdapter
        newInstance(Object criterion) throws ResourceException
        {
          try
          {
            return new Connection((URL) criterion);
          }

          catch (IOException e)
          {
            throw new ResourceException(e);
          }
        }
      },
      100
    );
  private static Equal                  equal =
    new Equal()
    {
      public boolean
      equal(Object object, Object refData)
      {
        return
          ((Connection) object).getUrl().getAuthority().equals(((URL) refData).
            getAuthority()) &&
            ((Connection) object).getUrl().getProtocol().equals(((URL) refData).
              getProtocol());
      }
    };
  private static boolean		interactive = false;
  private static KeyManager		keyManager;
  private static Set			noCompress = new HashSet();
  private static Set			noExpect100 = new HashSet();
  private static boolean		shouldCompress =
    System.getProperty("be.re.http.no-compress") == null;
  private static boolean		shouldExpect100 =
    System.getProperty("be.re.http.no-expect100") == null;
  private static SSLContext		sslContext = null;
  private static int			timeout = 0;
  private static Tracer			tracer;
  private static TrustManager		trustManager;
  private static String			userAgent = null;



  static
  {
    try
    {
      tracer =
        System.getProperty("be.re.http.trace") != null ?
          (
            System.getProperty("be.re.http.trace.file") != null ?
              new Tracer
              (
                new FileOutputStream
                (
                  System.getProperty("be.re.http.trace.file")
                )
              ) : Tracer.getDefault()
          ) : null;
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  private static void
  flushResponse
  (
    ReadLineInputStream	in,
    Resource		connection,
    Headers		headers,
    Headers		trailer
  ) throws IOException
  {
    StreamConnector.copy
    (
      searchForData(in, connection, headers, trailer),
      new DevNullOutputStream()
    );
  }



  public static Authenticate
  getAuthenticator()
  {
    if (authenticator == null)
    {
      try
      {
        authenticator =
          new URLAuthenticator
          (
            interactive ?
              new BasicAuthenticator(new InteractiveAuthenticator()) :
              new BasicAuthenticator()
          );
      }

      catch (Exception e)
      {
        throw new RuntimeException(e);
      }
    }

    return authenticator;
  }



  public static boolean
  getCompress()
  {
    return shouldCompress;
  }



  public static boolean
  getExpect100()
  {
    return shouldExpect100;
  }



  public static boolean
  getInteractive()
  {
    return interactive;
  }



  public static KeyManager
  getKeyManager()
  {
    return keyManager;
  }



  private static URL
  getRedirectUrl(URL currentUrl, String location)
  {
    try
    {
      return
        new URL
        (
          location.startsWith("http:") || location.startsWith("https:") ||
            location.startsWith("feed:") ?
            location : (currentUrl.toString() + "/" + location)
        );
    }

    catch (MalformedURLException e)
    {
      return currentUrl;
    }
  }



  private static String
  getResponseMessage(int responseCode)
  {
    try
    {
      return be.re.net.Util.getResource("http_" + String.valueOf(responseCode));
    }

    catch (Throwable e)
    {
      return be.re.net.Util.getResource("http_unknown");
    }
  }



  private static SSLContext
  getSSLContext()
  {
    if (sslContext == null)
    {
      sslContext = getSSLContext("TLS");

      if (sslContext == null)
      {
        sslContext = getSSLContext("SSL");
      }

      if (sslContext != null)
      {
        if (keyManager == null)
        {
          keyManager = new ClientKeyManager(interactive);
        }

        if (trustManager == null)
        {
          trustManager = new ClientTrustManager(interactive);
        }

        try
        {
          sslContext.init
          (
            new KeyManager[]{keyManager},
            new TrustManager[]{trustManager},
            null
          );
        }

        catch (Exception e)
        {
          throw new RuntimeException(e);
        }
      }
    }

    return sslContext;
  }



  private static SSLContext
  getSSLContext(String protocol)
  {
    try
    {
      return SSLContext.getInstance(protocol);
    }

    catch (Exception e)
    {
      return null;
    }
  }



  public static int
  getTimeout()
  {
    return timeout;
  }



  public static Tracer
  getTracer()
  {
    return tracer;
  }



  public static TrustManager
  getTrustManager()
  {
    return trustManager;
  }



  public static String
  getUserAgent()
  {
    return userAgent;
  }



  private static String
  hostHeader(URL url)
  {
    return "Host:" + hostValue(url) + "\r\n";
  }



  private static String
  hostValue(URL url)
  {
    return
      url.getHost() +
        (url.getPort() != -1 ? (":" + String.valueOf(url.getPort())) : "");
  }



  public static void
  markCompress(URL url)
  {
    markUrl(url, noCompress);
  }



  public static void
  markExpect100(URL url)
  {
    markUrl(url, noExpect100);
  }



  private static void
  markUrl(URL url, Set set)
  {
    set.add
    (
      url.getHost() +
        (url.getPort() != -1 ? (":" + String.valueOf(url.getPort())) : "")
    );
  }



  private static void
  release(Resource resource) throws IOException
  {
    try
    {
      resource.release();
    }

    catch (ResourceException e)
    {
      if (e.getException() instanceof IOException)
      {
        throw (IOException) e.getException();
      }

      throw new RuntimeException(e);
    }
  }



  public static InputStream
  request(String method, URL url) throws HTTPProtocolException, IOException
  {
    return request(method, url, null, null, null, null, null, null, null, true);
  }



  public static InputStream
  request(String method, URL url, String username, String password)
    throws HTTPProtocolException, IOException
  {
    return
      request
      (
        method,
        url,
        null,
        username,
        password,
        null,
        null,
        null,
        null,
        true
      );
  }



  public static InputStream
  request(String method, URL url, InputStream body)
    throws HTTPProtocolException, IOException
  {
    return request(method, url, body, null, null, null, null, null, null, true);
  }



  public static InputStream
  request
  (
    String	method,
    URL		url,
    InputStream	body,
    String	username,
    String	password
  ) throws HTTPProtocolException, IOException
  {
    return
      request
      (
        method,
        url,
        body,
        username,
        password,
        null,
        null,
        null,
        null,
        true
      );
  }



  public static InputStream
  request
  (
    String	method,
    URL		url,
    InputStream	body,
    Headers	headersIn,
    Headers	headersOut,
    Headers	trailerIn,
    Headers	trailerOut
  ) throws HTTPProtocolException, IOException
  {
    return
      request
      (
        method,
        url,
        body,
        null,
        null,
        headersIn,
        headersOut,
        trailerIn,
        trailerOut,
        true
      );
  }


  public static InputStream
  request
  (
    String	method,
    URL		url,
    InputStream	body,
    Headers	headersIn,
    Headers	headersOut,
    Headers	trailerIn,
    Headers	trailerOut,
    boolean	chunkedRequest
  ) throws HTTPProtocolException, IOException
  {
    return
      request
      (
        method,
        url,
        body,
        null,
        null,
        headersIn,
        headersOut,
        trailerIn,
        trailerOut,
        chunkedRequest
      );
  }



  public static InputStream
  request
  (
    String	method,
    URL		url,
    InputStream	body,
    String	username,
    String	password,
    Headers	headersIn,
    Headers	headersOut,
    Headers	trailerIn,
    Headers	trailerOut
  ) throws HTTPProtocolException, IOException
  {
    return
      request
      (
        method,
        url,
        body,
        username,
        password,
        headersIn,
        headersOut,
        trailerIn,
        trailerOut,
        true
      );
  }



  public static InputStream
  request
  (
    String	method,
    URL		url,
    InputStream	body,
    String	username,
    String	password,
    Headers	headersIn,
    Headers	headersOut,
    Headers	trailerIn,
    Headers	trailerOut,
    boolean	chunkedRequest
  ) throws HTTPProtocolException, IOException
  {
    return
      request
      (
        method,
        url,
        body,
        username,
        password,
        headersIn,
        headersOut,
        trailerIn,
        trailerOut,
        chunkedRequest,
        -1
      );
  }



  public static InputStream
  request
  (
    String	method,
    URL		url,
    InputStream	body,
    String	username,
    String	password,
    Headers	headersIn,
    Headers	headersOut,
    Headers	trailerIn,
    Headers	trailerOut,
    boolean	chunkedRequest,
    long	contentLength
  ) throws HTTPProtocolException, IOException
  {
    if
    (
      !url.getProtocol().equals("http")		&&
      !url.getProtocol().equals("https")	&&
      !url.getProtocol().equals("feed")		&&
      !(
        url.getProtocol().equals("ftp")		&&
        (
          method.equals("GET")			||
          method.equals("PUT")
        )
      )
    )
    {
      throw
        new HTTPProtocolException
        (
          be.re.net.Util.getResource("http_protocol_error")
        );
    }

    if (body != null && (method.equals("TRACE") || method.equals("DELETE")))
    {
      throw
        new HTTPProtocolException
        (
          MessageFormat.format
          (
            be.re.net.Util.getResource("http_body_error"),
            new Object[] {method}
          )
        );
    }

    return
      new Request
      (
        method,
        url,
        body,
        username,
        password,
        headersIn,
        headersOut,
        trailerIn,
        trailerOut,
        chunkedRequest,
        contentLength
      ).getInputStream();
  }



  private static String
  requestLine(String method, URL url, URL proxy, Socket socket)
  {
    return
      method + " " +
        (
          url.getHost().equals(socket.getInetAddress().getHostName()) &&
            proxy == null ?
            url.getFile() : url.toString()
        ) + " HTTP/1.1\r\n";
  }



  private static URL
  resolvePath(URL url) throws MalformedURLException
  {
    return
      url.getFile().indexOf("../") != -1 ?
        be.re.net.Util.resolvePath(url) : url;
  }



  private static InputStream
  searchForData
  (
    ReadLineInputStream	in,
    Resource		connection,
    Headers		headers,
    Headers		trailer
  ) throws IOException
  {
    return
      headers.get("Content-Length").length > 0 ?
        new FixedLengthInputStream
        (
          in,
          connection,
          Integer.parseInt(headers.get("Content-Length")[0])
        ) :
        (
          headers.get("Transfer-Encoding").length > 0 &&
            headers.get("Transfer-Encoding")[0].equalsIgnoreCase("chunked") ?
            (InputStream)
              new TransferInputStream(in, connection, headers, trailer) :
            (InputStream) new NormalInputStream(in, connection)
        );
  }



  public static void
  setAuthenticator(Authenticate value)
  {
    authenticator = value;
  }



  public static void
  setCompress(boolean value)
  {
    shouldCompress = value;
  }



  public static void
  setExpect100(boolean value)
  {
    shouldExpect100 = value;
  }



  public static void
  setInteractive(boolean value)
  {
    interactive = value;
  }



  public static void
  setKeyManager(KeyManager value)
  {
    keyManager = value;
  }



  public static void
  setTimeout(int value)
  {
    timeout = value;
  }



  public static void
  setTracer(Tracer value)
  {
    tracer = value;
  }



  public static void
  setTrustManager(TrustManager value)
  {
    trustManager = value;
  }



  public static void
  setUserAgent(String value)
  {
    userAgent = value;
  }



  private static boolean
  supportsCompress(URL url)
  {
    return supportsFeature(url, noCompress);
  }



  private static boolean
  supportsExpect100(URL url)
  {
    return supportsFeature(url, noExpect100);
  }



  private static boolean
  supportsFeature(URL url, Set unsupported)
  {
    return
      !unsupported.contains
      (
        url.getHost() +
          (url.getPort() != -1 ? (":" + String.valueOf(url.getPort())) : "")
      );
  }



  private static class Connection implements ResourceAdapter

  {

    private boolean	closed = false;
    private boolean	mustClose = false;
    private Socket	socket;
    private long	timestamp = new Date().getTime();
    private URL         url;



    private
    Connection(URL url) throws IOException
    {
      this.socket =
        "https".equals(url.getProtocol()) ?
          getSSLContext().getSocketFactory().createSocket
          (
            url.getHost(),
            url.getPort() == -1 ? 443 : url.getPort()
          ) :
          new Socket(url.getHost(), url.getPort() == -1 ? 80 : url.getPort());
      this.url = url;

      if (socket instanceof SSLSocket)
      {
        ((SSLSocket) socket).setUseClientMode(true);
      }

      socket.setSoTimeout(timeout);
    }



    public void
    acquired()
    {
    }



    public synchronized void
    close() throws ResourceException
    {
      if (!closed)
      {
        closed = true;

        try
        {
          if (socket != null)
          {
            socket.close();
          }
        }

        catch (IOException e)
        {
          throw new ResourceException(e);
        }
      }
    }



    protected void
    finalize() throws Throwable
    {
      close();
    }



    public Object
    getConnection()
    {
      return this;
    }



    private Socket
    getSocket()
    {
      return socket;
    }



    public URL
    getUrl()
    {
      return url;
    }



    public boolean
    isAlive()
    {
      long	newTimestamp = new Date().getTime();

      if (newTimestamp - timestamp <= TIME_DELTA)
      {
        timestamp = newTimestamp;
        return true;
      }

      return false;
    }



    public boolean
    mustClose()
    {
      return mustClose;
    }



    public void
    released()
    {
      timestamp = new Date().getTime();
    }



    public void
    setMustClose(boolean value)
    {
      mustClose = value;
    }

  } // Connection



  private static class ConsumeResponseInputStream extends FilterInputStream

  {

    private boolean	closed = false;



    private
    ConsumeResponseInputStream(InputStream in)
    {
      super(in);
    }



    public void
    close() throws IOException
    {
      if (!closed)
      {
        closed = true;
        StreamConnector.copy(in, new DevNullOutputStream());
      }
    }

  } // ConsumeResponseInputStream



  /**
   * The class gets rid of the socket connection as soon as possible.
   */

  private static class FixedLengthInputStream extends LimitedLengthInputStream

  {

    private boolean	closed = false;
    private Resource	connection;



    private
    FixedLengthInputStream(InputStream in, Resource connection, int length)
      throws IOException
    {
      super(in, length);
      this.connection = connection;
    }



    public void
    close() throws IOException
    {
      if (!closed)
      {
        closed = true;

        if (connection != null)
        {
          release(connection);
        }
      }
    }

  } // FixedLengthInputStream



  private static class NormalInputStream extends FilterInputStream

  {

    private boolean	closed = false;
    private Resource	connection;



    private
    NormalInputStream(InputStream in, Resource connection)
    {
      super(in);
      this.connection = connection;
    }



    public void
    close() throws IOException
    {
      if (!closed)
      {
        closed = true;

        if (connection != null)
        {
          release(connection);
        }
      }
    }

  } // NormalInputStream



  private static class Request

  {

    private InputStream	body;
    private boolean	chunkedRequest;
    private boolean	compress;
    private long	contentLength;
    private byte[]	data;
    private Headers	headersIn;
    private Headers	headersOut;
    private String	method;
    private URL		proxy;
    private int		redirectionCount = 0;
    private int		retryCount = 0;
    private boolean	shouldTunnel = false;
    private Headers	trailerIn;
    private Headers	trailerOut;
    private URL		url;



    private
    Request
    (
      String		method,
      URL		url,
      InputStream	body,
      String		username,
      String		password,
      Headers		headersIn,
      Headers		headersOut,
      Headers		trailerIn,
      Headers		trailerOut,
      boolean		chunkedRequest,
      long		contentLength
    ) throws IOException, MalformedURLException
    {
      this.method = method;
      this.url = be.re.net.Util.stripUserInfo(resolvePath(url));
      this.body = body;
      this.headersIn = headersIn != null ? headersIn : new Headers();
      this.headersOut = headersOut != null ? headersOut : new Headers();
      this.trailerIn = trailerIn;
      this.trailerOut = trailerOut;
      this.chunkedRequest =
        chunkedRequest && (PUT.equals(method) || POST.equals(method)) &&
          shouldExpect100 && supportsExpect100(url);
      compress =
        shouldCompress && supportsCompress(url) &&
          (chunkedRequest || contentLength == -1);
          // If the contentLength is given the client wants to stream the body.
          // In that case we can't predict the size of the compressed stream.
      this.contentLength = contentLength;

      try
      {
        proxy = ProxyManager.getProxy(url.getHost(), "http");

        if
        (
          proxy != null						&&
          this.headersIn.get("Proxy-Authorization").length == 0
        )
        {
          // Authenticate ahead for proxies instead of waiting for code 407
          // because it is most likely necessary. This saves a round trip in
          // most cases, which is important if a message body is present.

          authenticate(proxy, "Proxy-Authorization", false);
        }
      }

      catch (Throwable e)
      {
        throw new RuntimeException(e);
      }

      setUser(username, password, url);

      if (this.headersIn.get("User-Agent").length == 0)
      {
        this.headersIn.add
        (
          "User-Agent",
          getUserAgent() != null ?
            getUserAgent() :
            ("be.re.net.HTTPClient (" + System.getProperty("os.name") + ")")
        );
      }

      if (this.headersIn.get("Accept-Encoding").length == 0 && shouldCompress)
      {
        this.headersIn.
          add("Accept-Encoding", "gzip;q=1.0, identity;q=0.5, *;q=0");
      }

      if (body != null)
      {
        setData();
      }
    }



    private boolean
    authenticate(URL url, String header, boolean wasBad)
    {
      Authenticate	auth = getAuthenticator();

      if (wasBad && auth != null && headersIn.get(header).length > 0)
      {
        User	user = auth.getUser(url.toString(), url.getProtocol());

        if (user != null)
        {
          auth.badUser(url.toString(), url.getProtocol(), user);
        }
      }

      headersIn.remove(header);
      authenticate(null, null, url, header);

      return headersIn.get(header).length > 0;
    }



    private void
    authenticate(String username, String password, URL url, String header)
    {
      if (username == null || password == null)
      {
        Authenticate	auth = getAuthenticator();

        if (auth != null)
        {
          if (username == null)
          {
            User	user = auth.getUser(url.toString(), url.getProtocol());

            if (user != null)
            {
              username = user.getUsername();
              password = user.getPassword();
            }
          }
          else
          {
            password =
              auth.getPassword(url.toString(), url.getProtocol(), username);
          }
        }
      }

      if (username != null && password != null)
      {
        try
        {
          headersIn.set
          (
            header,
            "Basic " +
              new String
              (
                Base64.encode((username + ":" + password).getBytes("ASCII")),
                "ASCII"
              )
          );
        }

        catch (UnsupportedEncodingException e)
        {
          throw new RuntimeException(e);
        }
      }
    }



    private Resource
    connect(URL url) throws IOException
    {
      if (proxy != null)
      {
        try
        {
          return connectionPool.get(this, equal, proxy);
        }

        catch (Throwable e)
        {
        }
      }

      try
      {
        return connectionPool.get(this, equal, url);
      }

      catch (ResourceException e)
      {
        throw new RuntimeException(e);
      }
    }



    private void
    createTunnel(ReadLineInputStream in, OutputStream out) throws IOException
    {
      out.write(("CONNECT " + hostValue(url) + " HTTP/1.1\r\n").getBytes());
      out.write(hostHeader(url).getBytes());

      String[]	header = headersIn.get("Proxy-Authorization");

      if (header.length > 0)
      {
        out.write(("Proxy-Authorization: " + header[0] + "\r\n").getBytes());
      }

      out.write("\r\n".getBytes());
      out.flush();

      int	response = processHeaders(in, null);

      if (response < 200 || response >= 300)
      {
        throw
          new HTTPProtocolException
          (
            response,
            getResponseMessage(response)
          );
      }
    }



    private InputStream
    decode(InputStream in, Headers headersOut) throws IOException
    {
      return
        headersOut.get("Content-Length").length > 0 &&
          "0".equals(headersOut.get("Content-Length")[0]) ?
          in : decode(in, headersOut.get("Content-Encoding"), 0);
    }



    private InputStream
    decode(InputStream in, String[] encodings, int i) throws IOException
    {
      return
        encodings.length > 0 && i >= 0 ?
          (
            encodings[i].equalsIgnoreCase("gzip") ||
              encodings[i].equalsIgnoreCase("x-gzip") ?
              new GZIPInputStream(decode(in, encodings, i - 1)) :
              (
                encodings[i].equalsIgnoreCase("deflate") ?
                  new InflaterInputStream(decode(in, encodings, i - 1)) :
                  decode(in, encodings, i - 1)
              )
          ) : in;
    }



    private void
    decompressData() throws IOException
    {
      ByteArrayOutputStream	out = new ByteArrayOutputStream();

      StreamConnector.
        copy(new GZIPInputStream(new ByteArrayInputStream(data)), out);
      data = out.toByteArray();
      headersIn.set("Content-Length", String.valueOf(data.length));
    }



    private InputStream
    getInputStream() throws HTTPProtocolException, IOException
    {
      Resource	connection = connect(url);

      try
      {
        ReadLineInputStream	in = wrapIn(connection);
        OutputStream		out = wrapOut(connection);

        if (shouldTunnel)
        {
          createTunnel(in, out);
        }

        setContentEncoding();

        writeRequest
        (
          out,
          ((Connection) connection.getConnection()).getSocket()
        );

        int	response = -1;

        if (chunkedRequest)
        {
          response = processHeaders(in, connection);

          if (response == -1)
          {
            tryNotChunked();

            return retryRequest(connection, new IOException("No status line"));
          }

          if (response == 100)
          {
            writeBody(out);
            response = processHeaders(in, connection);
          }
        }
        else
        {
          if (data != null || contentLength != -1)
          {
            writeBody(out);
          }

          response = processHeaders(in, connection);
        }

        if (processResponse(response))
        {
          if (++redirectionCount > MAX_REDIRECTIONS)
          {
            throw
              new HTTPProtocolException
              (
                be.re.net.Util.getResource("http_redirect")
              );
          }

          if
          (
            response == -1						||
            (
              "https".equals(url.getProtocol())				&&
              response >= 400						&&
              (
                !(keyManager instanceof ClientKeyManager)		||
                ((ClientKeyManager) keyManager).failed
                (
                  ((Connection) connection.getConnection()).getSocket()
                )
              )
            )
          )
          {
            ((Connection) connection.getConnection()).setMustClose(true);
            release(connection);

            try
            {
              Thread.sleep(redirectionCount * 1000);
            }

            catch (InterruptedException e)
            {
            }
          }
          else
          {
            flushResponse(in, connection, headersOut, trailerOut);
          }

          return getInputStream();
        }

        return
          new ConsumeResponseInputStream
          (
            method.equals("HEAD") || response == 204 || response == 205 ?
              new FixedLengthInputStream(in, connection, 0) :
              decode
              (
                searchForData(in, connection, headersOut, trailerOut),
                headersOut
              )
          );
      }

      catch (HTTPProtocolException e)
      {
        ((Connection) connection.getConnection()).setMustClose(true);
        release(connection);
        throw e;
      }

      catch (IOException e)
      {
        if
        (
          e instanceof SSLHandshakeException		&&
          keyManager instanceof ClientKeyManager
        )
        {
          ((ClientKeyManager) keyManager).
            failed(((Connection) connection.getConnection()).getSocket());
        }

        return retryRequest(connection, e);
      }

      catch (Throwable e)
      {
        ((Connection) connection.getConnection()).setMustClose(true);
        release(connection);

        throw new RuntimeException(e);
      }
    }



    private boolean
    isCompressed()
    {
      String[]	mimeType = headersIn.get("Content-Type");

      if (mimeType.length == 0)
      {
        return false;
      }

      mimeType[0] = mimeType[0].toLowerCase();

      for (int i = 0; i < compressedTypes.length; ++i)
      {
        if (mimeType[0].startsWith(compressedTypes[i]))
        {
          return true;
        }
      }

      return false;
    }



    private boolean
    isNotPincette()
    {
      String[]	server = headersOut.get("Server");

      for (int i = 0; i < server.length; ++i)
      {
        if (server[i].indexOf("Pincette") != -1)
        {
          return false;
        }
      }

      return true;
    }



    private int
    processHeaders(ReadLineInputStream in, Resource connection)
      throws IOException
    {
      byte[]	line;

      while ((line = in.readLine()) != null && line.length == 0);

      if (line == null)
      {
        return -1;
      }

      StringTokenizer	tokenizer = new StringTokenizer(new String(line));

      if (tokenizer.countTokens() < 2)
      {
        throw new IOException("Bad status line");
      }

      headersOut.clear();
      headersOut.add(be.re.net.Util.readHeaders(in));

      String	protocol = tokenizer.nextToken();

      if
      (
        connection != null						&&
        (
          protocol.equals("HTTP/1.0")					||
          (
            headersOut.get("Connection").length > 0			&&
            headersOut.get("Connection")[0].equalsIgnoreCase("close")
          )
        )
      )
      {
        ((Connection) connection.getConnection()).setMustClose(true);
      }

      int	responseCode = Integer.parseInt(tokenizer.nextToken());
      String	message = "";

      headersOut.set("Status-Code", String.valueOf(responseCode));

      while (tokenizer.hasMoreTokens())
      {
        message += (message.equals("") ? "" : " ") + tokenizer.nextToken();
      }

      headersOut.set("Status-Message", message);

      return responseCode;
    }



    private boolean
    processResponse(int response) throws HTTPProtocolException, IOException
    {
      if (response == -1)
      {
        return true; // Unknown failure.
      }

      if
      (
        response == 300	||
        response == 301	||
        response == 302	||
        response == 303	||
        response == 305	||
        response == 307
      )
      {
        String[]	location = headersOut.get("location");

        if (location.length == 0)
        {
          throw
            new HTTPProtocolException
            (
              response,
              getResponseMessage(response)
            );
        }

        URL	newUrl = resolvePath(getRedirectUrl(url, location[0]));

        if (url.equals(newUrl))
        {
          return false;
        }

        switch (response)
        {
          case 300: case 301: case 302: case 307:
            url = newUrl;
            break;

          case 303:
            url = newUrl;
            method = "GET";
            break;

          case 305:
            proxy = newUrl;
            headersIn.remove("Proxy-Authorization");
            authenticate(proxy, "Proxy-Authorization", false);
            break;
        }

        return true; // This is not compliant because it is automatic.
      }

      if (response == 401)
      {
        return
          (headersIn.get("Authorization").length == 0 || interactive) &&
            authenticate(url, "Authorization", true);
      }

      if (response == 407 && proxy != null)
      {
        return authenticate(proxy, "Proxy-Authorization", true);
      }

      if
      (
        compress		&&
        (
          response == 415	||
          (
            response == 400	&&
            isNotPincette()
          )
        )
      )
      {
        compress = false;
        markCompress(url);

        if (data != null)
        {
          decompressData();
        }

        return true;
      }

      if (response == 411 || response == 417)
      {
        tryNotChunked();

        return true;
      }

      if (response == 501 && proxy != null)
      {
        shouldTunnel = true;

        return true;
      }

      return false; // Don't retry the request.
    }



    private InputStream
    retryRequest(Resource connection, IOException e) throws IOException
    {
      ((Connection) connection.getConnection()).setMustClose(true);
      release(connection);

      if (++retryCount > MAX_RETRIES)
      {
        throw e;
      }

      try
      {
        Thread.sleep(retryCount * 1000);
      }

      catch (InterruptedException ex)
      {
      }

      return getInputStream();
    }



    private void
    setContentEncoding()
    {
      if (body != null)
      {
        headersIn.remove("Content-Encoding", "gzip");

        if (compress && !isCompressed())
        {
          headersIn.add("Content-Encoding", "gzip");
        }
      }
    }



    private void
    setData() throws IOException
    {
      if (chunkedRequest)
      {
        headersIn.set("Expect", "100-continue");
        headersIn.set("Transfer-Encoding", "chunked");
        headersIn.set("TE", "trailers");
      }
      else
      {
        if (contentLength == -1)
        {
          ByteArrayOutputStream	accu = new ByteArrayOutputStream();

          StreamConnector.copy
          (
            body,
            compress && !isCompressed() ?
              (OutputStream) new GZIPOutputStream(accu) : (OutputStream) accu,
            false,
            true
          );

          data = accu.toByteArray();
          headersIn.set("Content-Length", String.valueOf(data.length));
        }
        else
        {
          headersIn.set("Content-Length", String.valueOf(contentLength));
        }
      }
    }



    private void
    setUser(String username, String password, URL url)
      throws MalformedURLException
    {
      if (username == null || password == null)
      {
        User	user = be.re.net.Util.userFromUrl(url);

        if
        (
          user.getUsername() != null		&&
          (
            username == null			||
            user.getUsername().equals(username)
          )
        )
        {
          username = user.getUsername();
          password = user.getPassword();
        }

        if (username == null && getAuthenticator() != null)
        {
          user =
            getAuthenticator().
              usedPreviously(url.toString(), url.getProtocol());

          if (user != null)
          {
            username = user.getUsername();
            password = user.getPassword();
          }
        }

        if (username != null && password == null)
        {
          this.url = be.re.net.Util.setUserInfo(this.url, username);
        }
      }

      if (username != null)
      {
        authenticate(username, password, url, "Authorization");
      }
    }



    private void
    tryNotChunked() throws IOException
    {
      chunkedRequest = false;
      headersIn.remove("Expect", "100-continue");
      headersIn.remove("Transfer-Encoding", "chunked");
      headersIn.remove("TE", "trailers");
      markExpect100(url);
      setData();
    }



    private ReadLineInputStream
    wrapIn(Resource connection) throws IOException
    {
      Tracer	tr = getTracer();

      return
        new ReadLineInputStream
        (
          tr != null ?
            new ResponseTracer
            (
              ((Connection) connection.getConnection()).getSocket().
                getInputStream(),
              tr
            ) :
            ((Connection) connection.getConnection()).getSocket().
              getInputStream()
        );
    }



    private OutputStream
    wrapOut(Resource connection) throws IOException
    {
      Tracer	tr = getTracer();

      return
        tr != null ?
          new TeeOutputStream
          (
            new OutputStream[]
            {
              ((Connection) connection.getConnection()).getSocket().
                getOutputStream(),
              tr
            }
          ) :
          ((Connection) connection.getConnection()).getSocket().
            getOutputStream();
    }



    private void
    writeBody(OutputStream out)
    {
      try
      {
        if (chunkedRequest)
        {
          out =
            compress && !isCompressed() ?
              (OutputStream)
                new GZIPOutputStream(new TransferOutputStream(out, trailerIn)) :
              (OutputStream) new TransferOutputStream(out, trailerIn);

          StreamConnector.copy(body, out, false, true);
            // out is closed because of the trailers.
        }
        else
        {
          if (contentLength == -1)
          {
            out.write(data);
          }
          else
          {
            StreamConnector.copy(body, out, false, false);
          }
        }

        out.flush();

        if (out instanceof GZIPOutputStream)
        {
          ((GZIPOutputStream) out).finish();
        }
      }

      catch (IOException e)
      {
        // This can mean two things. Either the server has already detected a
        // problem in the body in which case we will receive a proper status
        // code, or there was a physical link failure in which case the same
        // exception will occur when reading the response.
      }
    }



    private void
    writeRequest(OutputStream out, Socket socket) throws IOException
    {
      if
      (
        body == null			&&
        (
          method.equals("OPTIONS")	||
          method.equals("POST")		||
          method.equals("PUT")
        )
      )
      {
        headersIn.add("Content-Length", "0");
      }

      out.write
      (
        (
          requestLine(method, url, proxy, socket) +
            (headersIn.get("Host").length == 0 ? hostHeader(url) : "") +
            headersIn.toString() + "\r\n"
        ).getBytes()
      );

      out.flush();
    }

  } // Request



  private static class ResponseTracer extends FilterInputStream

  {

    private OutputStream	out;



    private
    ResponseTracer(InputStream in, OutputStream out)
    {
      super(in);
      this.out = out;
    }



    public int
    read() throws IOException
    {
      int	result = super.read();

      if (result != -1)
      {
        out.write(result);
        out.flush();
      }

      return result;
    }



    public int
    read(byte[] b) throws IOException
    {
      int	result = super.read(b);

      if (result != -1)
      {
        out.write(b, 0, result);
        out.flush();
      }

      return result;
    }



    public int
    read(byte[] b, int off, int len) throws IOException
    {
      int	result = super.read(b, off, len);

      if (result != -1)
      {
        out.write(b, off, result);
        out.flush();
      }

      return result;
    }

  } // ResponseTracer



  private static class TransferInputStream extends ChunkedInputStream

  {

    private Headers	headersOut;
    private int		length = 0;
    private boolean	closed = false;
    private Resource	connection;



    private
    TransferInputStream
    (
      ReadLineInputStream	in,
      Resource			connection,
      Headers			headersOut,
      Headers			trailerOut
    )
    {
      super(in, trailerOut);
      this.connection = connection;
      this.headersOut = headersOut;
    }



    public void
    close() throws IOException
    {
      if (!closed)
      {
        closed = true;

        if (headersOut != null)
        {
          headersOut.remove("Transfer-Encoding", "chunked");
          headersOut.add("Content-Length", String.valueOf(length));
        }

        if (connection != null)
        {
          release(connection);
        }
      }
    }



    protected void
    finalize() throws Throwable
    {
      close();
    }



    public int
    read(byte[] b, int off, int len) throws IOException
    {
      int	count = super.read(b, off, len);

      if (count != -1)
      {
        length += count;
      }

      return count;
    }

  } // TransferInputStream



  private static class TransferOutputStream extends ChunkedOutputStream

  {

    private boolean	closed = false;
    private Headers	trailerIn;



    private
    TransferOutputStream(OutputStream out, Headers trailerIn)
    {
      super(new BufferedOutputStream(out, 0x1000));
      this.trailerIn = trailerIn;
    }



    public void
    close() throws IOException
    {
      if (!closed)
      {
        closed = true;

        out.write
        (
          ("0\r\n" + (trailerIn != null ? trailerIn.toString() : "") + "\r\n").
            getBytes()
        );

        super.flush();
      }
    }

  } // TransferOutputStream



  public static class Util

  {

    public static long
    getLastModified(URL url)
    {
      try
      {
        Headers	headers = new Headers();

        HTTPClient.
          request(HTTPClient.HEAD, url, null, null, headers, null, null).
          close();

        return be.re.net.Util.getTimeHeader(headers, "Last-Modified");
      }

      catch (Exception e)
      {
        return -1;
      }
    }

  } // Util

} // HTTPClient
