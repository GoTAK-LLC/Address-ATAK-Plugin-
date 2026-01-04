/*
 * This file is auto-generated.  DO NOT MODIFY.
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
    private static final java.lang.String DESCRIPTOR = "com.gotak.address.aidl.ILogger";
    /** Construct the stub at attach it to the interface. */
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
      switch (code)
      {
        case INTERFACE_TRANSACTION:
        {
          reply.writeString(descriptor);
          return true;
        }
        case TRANSACTION_e:
        {
          data.enforceInterface(descriptor);
          java.lang.String _arg0;
          _arg0 = data.readString();
          java.lang.String _arg1;
          _arg1 = data.readString();
          java.lang.String _arg2;
          _arg2 = data.readString();
          this.e(_arg0, _arg1, _arg2);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_d:
        {
          data.enforceInterface(descriptor);
          java.lang.String _arg0;
          _arg0 = data.readString();
          java.lang.String _arg1;
          _arg1 = data.readString();
          java.lang.String _arg2;
          _arg2 = data.readString();
          this.d(_arg0, _arg1, _arg2);
          reply.writeNoException();
          return true;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
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
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().e(tag, msg, exception);
            return;
          }
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
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().d(tag, msg, exception);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      public static com.gotak.address.aidl.ILogger sDefaultImpl;
    }
    static final int TRANSACTION_e = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_d = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    public static boolean setDefaultImpl(com.gotak.address.aidl.ILogger impl) {
      // Only one user of this interface can use this function
      // at a time. This is a heuristic to detect if two different
      // users in the same process use this function.
      if (Stub.Proxy.sDefaultImpl != null) {
        throw new IllegalStateException("setDefaultImpl() called twice");
      }
      if (impl != null) {
        Stub.Proxy.sDefaultImpl = impl;
        return true;
      }
      return false;
    }
    public static com.gotak.address.aidl.ILogger getDefaultImpl() {
      return Stub.Proxy.sDefaultImpl;
    }
  }
  public void e(java.lang.String tag, java.lang.String msg, java.lang.String exception) throws android.os.RemoteException;
  public void d(java.lang.String tag, java.lang.String msg, java.lang.String exception) throws android.os.RemoteException;
}
