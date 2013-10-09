package be.re.cls;

import be.re.util.Compare;
import be.re.util.Sort;
import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;



public class ClassFile

{

  private static final byte	CONSTANT_Class = 7;
  private static final byte	CONSTANT_Fieldref = 9;
  private static final byte	CONSTANT_Methodref = 10;
  private static final byte	CONSTANT_InterfaceMethodref = 11;
  private static final byte	CONSTANT_String = 8;
  private static final byte	CONSTANT_Integer = 3;
  private static final byte	CONSTANT_Float = 4;
  private static final byte	CONSTANT_Long = 5;
  private static final byte	CONSTANT_Double = 6;
  private static final byte	CONSTANT_NameAndType = 12;
  private static final byte	CONSTANT_Utf8 = 1;

  private Attribute[]   attributes;
  private Object[]	constantPool;
  private Field[]	fields;
  private String[]	interfaces;
  private boolean	isDeprecated;
  private Method[]	methods;
  private int		modifiers;
  private String	name;
  private String	sourceFile;
  private String	superClass;
  private String	version;



  private
  ClassFile()
  {
  }



  public Attribute[]
  getAttributes()
  {
    return attributes;
  }



  private String
  getClassName(short index)
  {
    return (String) constantPool[((ClassInfo) constantPool[index]).name];
  }



  public Field[]
  getFields()
  {
    return fields;
  }



  public String[]
  getInterfaceNames()
  {
    return interfaces;
  }



  public String[]
  getInterfaceTypes()
  {
    String[]	result = new String[interfaces.length];

    for (int i = 0; i < interfaces.length; ++i)
    {
      result[i] = Util.getType(interfaces[i]);
    }

    return result;
  }



  public Method[]
  getMethods()
  {
    return methods;
  }



  public int
  getModifiers()
  {
    return modifiers;
  }



  public String
  getName()
  {
    return name;
  }



  public String
  getSourceFile()
  {
    return sourceFile;
  }



  public String
  getSuperClassName()
  {
    return superClass;
  }



  public String
  getSuperClassType()
  {
    return superClass == null ? null : Util.getType(superClass);
  }



  public String
  getType()
  {
    return Util.getType(name);
  }



  public String
  getVersion()
  {
    return version;
  }



  public boolean
  isArray()
  {
    return name.charAt(0) == '[';
  }



  public boolean
  isDeprecated()
  {
    return isDeprecated;
  }



  public boolean
  isInterface()
  {
    return Modifier.isInterface(getModifiers());
  }



  private static ClassFile
  parse(DataInputStream in) throws IOException
  {
    if (in.readInt() != 0xcafebabe)
    {
      throw new NotAClassException();
    }

    ClassFile	c = new ClassFile();

    c.version = String.valueOf(in.readShort());
    c.version = String.valueOf(in.readShort()) + "." + c.version;
    c.constantPool = readConstantPool(in, in.readShort());
    c.modifiers = in.readShort();
    c.name = c.getClassName(in.readShort());

    short	superClass = in.readShort();

    c.superClass = superClass == 0 ?  null : c.getClassName(superClass);
    c.interfaces = c.readClassNames(in, in.readShort());
    c.fields = c.readFields(in, in.readShort());
    c.methods = c.readMethods(in, in.readShort());

    short	attLength = in.readShort();
    List	attributes = new ArrayList();

    for (short i = 0; i < attLength; ++i)
    {
      String	name = (String) c.constantPool[in.readShort()];
      int	length = in.readInt();

      if (name.equals("Deprecated"))
      {
        c.isDeprecated = true;
      }
      else
      {
        if (name.equals("SourceFile"))
        {
          c.sourceFile = (String) c.constantPool[in.readShort()];
        }
        else
        {
          attributes.add(c.readAttribute(in, name, length));
        }
      }
    }

    c.attributes =
      (Attribute[]) attributes.toArray(new Attribute[attributes.size()]);
    c.constantPool = null;

    return c;
  }



  public static ClassFile
  parse(InputStream in) throws IOException
  {
    return parse(new DataInputStream(in));
  }



  public static ClassFile
  parse(byte[] b) throws IOException
  {
    return parse(new ByteArrayInputStream(b));
  }



  private Attribute
  readAttribute(DataInput in, String name, int length) throws IOException
  {
    Attribute	attribute = new Attribute();

    attribute.name = name;
    attribute.value = new byte[length];
    in.readFully(attribute.value);

    return attribute;
  }



  private String[]
  readClassNames(DataInput in, short size) throws IOException
  {
    String[]	result = new String[size];

    for (short i = 0; i < size; ++i)
    {
      result[i] = getClassName(in.readShort());
    }

    return result;
  }



  private Code
  readCode(DataInput in) throws IOException
  {
    Code	result = new Code();

    result.maxStack = in.readShort();
    result.maxLocals = in.readShort();
    result.code = new byte[in.readInt()];
    in.readFully(result.code);
    result.exceptions = readExceptionHandlers(in, in.readShort());

    short	attLength = in.readShort();
    List	attributes = new ArrayList();
    List	localVariables = new ArrayList();

    for (short i = 0; i < attLength; ++i)
    {
      String	name = (String) constantPool[in.readShort()];
      int	length = in.readInt(); // The attribute length.

      if (name.equals("LocalVariableTable"))
      {
        localVariables.addAll(readLocalVariables(in, in.readShort()));
      }
      else
      {
        attributes.add(readAttribute(in, name, length));
      }
    }

    result.attributes =
      (Attribute[]) attributes.toArray(new Attribute[attributes.size()]);

    result.localVariables =
      (LocalVariable[])
        Sort.qsort
        (
          localVariables.toArray(new LocalVariable[localVariables.size()]),
          new Compare()
          {
            public int
            compare(Object object1, Object object2)
            {
              return
                ((LocalVariable) object1).getIndex() -
                  ((LocalVariable) object2).getIndex();
            }
          }
        );

    return result;
  }



  private static Object[]
  readConstantPool(DataInput in, short size) throws IOException
  {
    Object[]	result = new Object[size];

    for (short i = 1; i < size; ++i)
    {
      byte	tag = in.readByte();

      switch (tag)
      {
        case CONSTANT_Class:
          result[i] = new ClassInfo(in.readShort());
          break;

        case CONSTANT_Fieldref:
        case CONSTANT_Methodref:
        case CONSTANT_InterfaceMethodref:
          result[i] = new RefInfo(tag, in.readShort(), in.readShort());
          break;

        case CONSTANT_String:
          result[i] = new StringInfo(in.readShort());
          break;

        case CONSTANT_Integer:
          result[i] = new Integer(in.readInt());
          break;

        case CONSTANT_Float:
          result[i] = new Float(in.readFloat());
          break;

        case CONSTANT_Long:
          result[i++] = new Long(in.readLong());
          break;

        case CONSTANT_Double:
          result[i++] = new Double(in.readDouble());
          break;

        case CONSTANT_NameAndType:
          result[i] = new NameAndType(in.readShort(), in.readShort());
          break;

        case CONSTANT_Utf8:
          result[i] = readString(in);
          break;

      }
    }

    return result;
  }



  private ExceptionHandler[]
  readExceptionHandlers(DataInput in, short size) throws IOException
  {
    ExceptionHandler[]	result = new ExceptionHandler[size];

    for (short i = 0; i < size; ++i)
    {
      result[i] = new ExceptionHandler();
      result[i].startPC = in.readShort();
      result[i].endPC = in.readShort();
      result[i].handlerPC = in.readShort();

      short	index = in.readShort();

      result[i].type = index == 0 ? null : getClassName(index);
    }

    return result;
  }



  private Field[]
  readFields(DataInput in, short size) throws IOException
  {
    Field[]	result = new Field[size];

    for (int i = 0; i < size; ++i)
    {
      result[i] = new Field();
      result[i].modifiers = in.readShort();
      result[i].name = (String) constantPool[in.readShort()];
      result[i].descriptor = (String) constantPool[in.readShort()];

      short	attLength = in.readShort();
      List	attributes = new ArrayList();

      for (short j = 0; j < attLength; ++j)
      {
        String	name = (String) constantPool[in.readShort()];
        int	length = in.readInt(); // The attribute length.

        if (name.equals("ConstantValue"))
        {
          result[i].value = constantPool[in.readShort()];
        }
        else
        {
          if (name.equals("Synthetic"))
          {
            result[i].isSynthetic = true;
          }
          else
          {
            if (name.equals("Deprecated"))
            {
              result[i].isDeprecated = true;
            }
            else
            {
              attributes.add(readAttribute(in, name, length));
            }
          }
        }
      }

      result[i].attributes =
        (Attribute[]) attributes.toArray(new Attribute[attributes.size()]);
    }

    return result;
  }



  private List
  readLocalVariables(DataInput in, short size) throws IOException
  {
    List	result = new ArrayList();

    for (short i = 0; i < size; ++i)
    {
      LocalVariable	localVariable = new LocalVariable();

      localVariable.startPC = in.readShort();
      localVariable.length = in.readShort();
      localVariable.name = (String) constantPool[in.readShort()];
      localVariable.descriptor = (String) constantPool[in.readShort()];
      localVariable.index = in.readShort();
      result.add(localVariable);
    }

    return result;
  }



  private Method[]
  readMethods(DataInput in, short size) throws IOException
  {
    Method[]	result = new Method[size];

    for (short i = 0; i < size; ++i)
    {
      result[i] = new Method();
      result[i].modifiers = in.readShort();
      result[i].name = (String) constantPool[in.readShort()];
      result[i].descriptor = (String) constantPool[in.readShort()];
      result[i].exceptions = new String[0];
      result[i].className = name;

      short	attLength = in.readShort();
      List	attributes = new ArrayList();

      for (short j = 0; j < attLength; ++j)
      {
        String	name = (String) constantPool[in.readShort()];
        int	length = in.readInt(); // The attribute length.

        if (name.equals("Code"))
        {
          result[i].code = readCode(in);
        }
        else
        {
          if (name.equals("Exceptions"))
          {
            result[i].exceptions = readClassNames(in, in.readShort());
          }
          else
          {
            if (name.equals("Synthetic"))
            {
              result[i].isSynthetic = true;
            }
            else
            {
              if (name.equals("Deprecated"))
              {
                result[i].isDeprecated = true;
              }
              else
              {
                attributes.add(readAttribute(in, name, length));
              }
            }
          }
        }
      }

      result[i].attributes =
        (Attribute[]) attributes.toArray(new Attribute[attributes.size()]);
    }

    return result;
  }



  private static String
  readString(DataInput in) throws IOException
  {
    byte[]	b = new byte[in.readShort()];

    in.readFully(b);

    try
    {
      return new String(b, "UTF-8");
    }

    catch (UnsupportedEncodingException e)
    {
      throw new RuntimeException(e);
    }
  }



  private static class ClassInfo

  {

    private short	name;



    private
    ClassInfo(short name)
    {
      this.name = name;
    }

  } // ClassInfo



  private static class NameAndType

  {

    private short	name;
    private short	type;



    private
    NameAndType(short name, short type)
    {
      this.name = name;
      this.type = type;
    }

  } // NameAndType



  private static class RefInfo

  {

    private short	classInfo;
    private short	nameAndType;
    private byte	tag;



    private
    RefInfo(byte tag, short classInfo, short nameAndType)
    {
      this.tag = tag;
      this.classInfo = classInfo;
      this.nameAndType = nameAndType;
    }

  } // RefInfo



  private static class StringInfo

  {

    private short	name;



    private
    StringInfo(short name)
    {
      this.name = name;
    }

  } // StringInfo

} // ClassFile
