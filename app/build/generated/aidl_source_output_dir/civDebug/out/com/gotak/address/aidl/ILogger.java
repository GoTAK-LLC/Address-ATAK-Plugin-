/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: C:\Users\matth\AppData\Local\Android\Sdk\build-tools\35.0.0\aidl.exe -pC:\Users\matth\AppData\Local\Android\Sdk\platforms\android-34\framework.aidl -oC:\Users\matth\Development\ATAK-Plugins\ATAK-CIV-5.4.0.24-SDK\plugins\Address-ATAK-Plugin\app\build\generated\aidl_source_output_dir\civDebug\out -IC:\Users\matth\Development\ATAK-Plugins\ATAK-CIV-5.4.0.24-SDK\plugins\Address-ATAK-Plugin\app\src\main\aidl -IC:\Users\matth\Development\ATAK-Plugins\ATAK-CIV-5.4.0.24-SDK\plugins\Address-ATAK-Plugin\app\src\civ\aidl -IC:\Users\matth\Development\ATAK-Plugins\ATAK-CIV-5.4.0.24-SDK\plugins\Address-ATAK-Plugin\app\build-types\debug\aidl -IC:\Users\matth\Development\ATAK-Plugins\ATAK-CIV-5.4.0.24-SDK\plugins\Address-ATAK-Plugin\app\src\civDebug\aidl -IC:\Users\matth\.gradle\caches\8.13\transforms\2e2e92604551e251ca248b6fee2e75ac\transformed\core-1.3.2\aidl -IC:\Users\matth\.gradle\caches\8.13\transforms\eaad224776ad6783ee76bef2fb91ad54\transformed\versionedparcelable-1.1.0\aidl -dC:\Users\matth\AppData\Local\Temp\aidl8111753505376978517.d C:\Users\matth\Development\ATAK-Plugins\ATAK-CIV-5.4.0.24-SDK\plugins\Address-ATAK-Plugin\app\src\main\aidl\com\gotak\address\aidl\ILogger.aidl
 */
package com.gotak.address.aidl;
// Declare any non-default types here with import statements
public interface ILogger extends android.os.IInterface
{
  /** Default implementation for ILogger. */
  public static class Default implements com.gotak.address.aidl.ILogger
  {
    @Override public void e(java.lang.String tag, java.lang.String msg, java.lang.String exception) throws android.os.RemoteException
    {
    }
    @Override public void d(java.lang.String tag, java.lang.String msg, java.lang.String exception) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.gotak.address.aidl.ILogger
  {
    /** Construct the stub at attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.gotak.address.aidl.ILogger interface,
     * generating a proxy if needed.
     */
    public static com.gotak.address.aidl.ILogger asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.gotak.address.aidl.ILogger))) {
        return ((com.gotak.address.aidl.ILogger)iin);
      }
      return new com.gotak.address.aidl.ILogger.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
        data.enforceInterface(descriptor);
      }
      if (code == INTERFACE_TRANSACTION) {
        reply.writeString(descriptor);
        return true;
      }
      switch (code)
      {
        case TRANSACTION_e:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          java.lang.String _arg1;
          _arg1 = data.readString();
          java.lang.String _arg2;
          _arg2 = data.readString();
          this.e(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_d:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          java.lang.String _arg1;
          _arg1 = data.readString();
          java.lang.String _arg2;
          _arg2 = data.readString();
          this.d(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements com.gotak.address.aidl.ILogger
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      @Override public void e(java.lang.String tag, java.lang.String msg, java.lang.String exception) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(tag);
          _data.writeString(msg);
          _data.writeString(exception);
          boolean _status = mRemote.transact(Stub.TRANSACTION_e, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void d(java.lang.String tag, java.lang.String msg, java.lang.String exception) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(tag);
          _data.writeString(msg);
          _data.writeString(exception);
          boolean _status = mRemote.transact(Stub.TRANSACTION_d, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_e = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_d = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "com.gotak.address.aidl.ILogger";
  public void e(java.lang.String tag, java.lang.String msg, java.lang.String exception) throws android.os.RemoteException;
  public void d(java.lang.String tag, java.lang.String msg, java.lang.String exception) throws android.os.RemoteException;
}
