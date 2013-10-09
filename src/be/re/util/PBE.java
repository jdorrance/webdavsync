package be.re.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;



/**
 * The class contains password-based encryption functions. It is placed in a
 * separate class, because it depends on JCE classes which may be not
 * installed.
 * @author Werner Donn\u00e9
 */

public class PBE

{

  private static Cipher	decryptCipher = null;
  private static Cipher	encryptCipher = null;
  private static char[]	password = null;



  public static byte[]
  crypt(byte[] data, Cipher cipher) throws PBEException
  {
    try
    {
      return cipher.doFinal(data);
    }

    catch (Exception e)
    {
      throw new PBEException();
    }
  }



  public static byte[]
  decrypt(byte[] data, char[] password) throws PBEException
  {
    try
    {
      return getCipher(password, false).doFinal(data);
    }

    catch (BadPaddingException e)
    {
      throw new PBEException(Util.getResource("password_error"));
    }

    catch (IllegalBlockSizeException e)
    {
      throw new PBEException(Util.getResource("password_error"));
    }

    catch (Exception e)
    {
      throw new RuntimeException(e); // Would be a bug.
    }
  }



  public static synchronized byte[]
  decrypt(byte[] data) throws PBEException
  {
    boolean	cancelled = false;

    while (!cancelled)
    {
      try
      {
        try
        {
          if (getPassword() != null)
          {
            if (decryptCipher == null)
            {
              decryptCipher = getCipher(getPassword(), false);
            }

            return crypt(data, decryptCipher);
          }

          cancelled = true;
        }

        catch (PBEException e)
        {
          decryptCipher = null;
          password = null;
        }
      }

      catch (Exception e)
      {
        throw new RuntimeException(e); // Would be a bug.
      }
    }

    throw new PBEException(Util.getResource("password_error"));
  }



  public static byte[]
  encrypt(byte[] data, char[] password)
  {
    try
    {
      return getCipher(password, true).doFinal(data);
    }

    catch (Exception e)
    {
      throw new RuntimeException(e); // Would be a bug.
    }
  }



  public static synchronized byte[]
  encrypt(byte[] data) throws PBEException
  {
    try
    {
      if (getPassword() != null)
      {
        if (encryptCipher == null)
        {
          encryptCipher = getCipher(getPassword(), true);
        }

        return crypt(data, encryptCipher);
      }
    }

    catch (Exception e)
    {
      throw new RuntimeException(e); // Would be a bug.
    }

    throw new PBEException(Util.getResource("password_error"));
  }



  public static Cipher
  getCipher(char[] password, boolean encrypt)
  {
    try
    {
      Cipher	cipher =
        Cipher.getInstance("PBEWithMD5AndDES/CBC/PKCS5Padding");

      cipher.init
      (
        encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE,
        SecretKeyFactory.getInstance("PBEWithMD5AndDES").
          generateSecret(new PBEKeySpec(password)),
        new PBEParameterSpec
        (
          new byte[]
          {
            (byte) 0x34, (byte) 0xfe, (byte) 0x9a, (byte) 0x01, (byte) 0x33,
              (byte) 0x12, (byte) 0x98, (byte) 0x55
          },
          20
        )
      );

      return cipher;
    }

    catch (NoClassDefFoundError e)
    {
      throw new RuntimeException(new Exception("JCE is not installed"));
    }

    catch (NoSuchAlgorithmException e)
    {
      throw
        new RuntimeException
        (
          new Exception("No provider for PBEWithMD5AndDES algorithm")
        );
    }

    catch (Exception e)
    {
      throw new RuntimeException(e); // Would be a bug.
    }
  }



  public static InputStream
  getInputStream(InputStream in, char[] password)
  {
    return new CipherInputStream(in, getCipher(password, false));
  }



  public static InputStream
  getInputStream(InputStream in) throws PBEException
  {
    if (getPassword() != null)
    {
      if (decryptCipher == null)
      {
        decryptCipher = getCipher(getPassword(), false);
      }

      return new CipherInputStream(in, decryptCipher);
    }

    throw new PBEException(Util.getResource("password_error"));
  }



  public static OutputStream
  getOutputStream(OutputStream out, char[] password)
  {
    return new CipherOutputStream(out, getCipher(password, true));
  }



  public static OutputStream
  getOutputStream(OutputStream out) throws PBEException
  {
    if (getPassword() != null)
    {
      if (encryptCipher == null)
      {
        encryptCipher = getCipher(getPassword(), true);
      }

      return new CipherOutputStream(out, encryptCipher);
    }

    throw new PBEException(Util.getResource("password_error"));
  }



  public static synchronized char[]
  getPassword()
  {
    if (password == null)
    {
      try
      {
        // Use reflection in order to avoid draging in the GUI environment for
        // non-interactive clients.

        Class	c = Class.forName("be.re.gui.util.InteractiveAuthenticator");
        String	s =
          (String)
            c.getMethod
            (
              "getPassword",
              new Class[]{String.class, String.class, String.class}
            ).invoke
              (
                c.newInstance(),
                new Object[]
                {
                  (String)
                    Class.forName("be.re.gui.util.Util").
                      getMethod("getResource", new Class[]{String.class}).
                      invoke(null, new Object[]{"password_label"}),
                  "PBE",
                  ""
                }
              );

        if (s != null)
        {
          password = s.toCharArray();
        }
      }

      catch (Exception e)
      {
        throw new RuntimeException(e);
      }
    }

    return password;
  }

} // PBE
