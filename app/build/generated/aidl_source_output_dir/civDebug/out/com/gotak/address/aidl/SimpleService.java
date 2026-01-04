/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package com.gotak.address.aidl;
public interface SimpleService extends android.os.IInterface
{
  /** Default implementation for SimpleService. */
  public static class Default implements com.gotak.address.aidl.SimpleService
  {
    /**
         * Pass a logging mechanism over to the Service so that the logs can be written to the
         * appropriate logger.
         */
    @Override public void registerLogger(com.gotak.address.aidl.ILogger log) throws android.os.RemoteException
    {
    }
    /**
         * Adds two numbers and returns the result.
         */
    @Override public int add(int a, int b) throws android.os.RemoteException
    {
      return 0;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.gotak.address.aidl.SimpleService
  {
    private static final java.lang.String DESCRIPTOR = "com.gotak.address.aidl.SimpleService";
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.gotak.address.aidl.SimpleService interface,
     * generating a proxy if needed.
     */
    public static com.gotak.address.aidl.SimpleService asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.gotak.address.aidl.SimpleService))) {
        return ((com.gotak.address.aidl.SimpleService)iin);
      }
      return new com.gotak.address.aidl.SimpleService.Stub.Proxy(obj);
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
        case TRANSACTION_registerLogger:
        {
          data.enforceInterface(descriptor);
          com.gotak.address.aidl.ILogger _arg0;
          _arg0 = com.gotak.address.aidl.ILogger.Stub.asInterface(data.readStrongBinder());
          this.registerLogger(_arg0);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_add:
        {
          data.enforceInterface(descriptor);
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          int _result = this.add(_arg0, _arg1);
          reply.writeNoException();
          reply.writeInt(_result);
          return true;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
    }
    private static class Proxy implements com.gotak.address.aidl.SimpleService
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
      /**
           * Pass a logging mechanism over to the Service so that the logs can be written to the
           * appropriate logger.
           */
      @Override public void registerLogger(com.gotak.address.aidl.ILogger log) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongBinder((((log!=null))?(log.asBinder()):(null)));
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerLogger, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().registerLogger(log);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
           * Adds two numbers and returns the result.
           */
      @Override public int add(int a, int b) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(a);
          _data.writeInt(b);
          boolean _status = mRemote.transact(Stub.TRANSACTION_add, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            return getDefaultImpl().add(a, b);
          }
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      public static com.gotak.address.aidl.SimpleService sDefaultImpl;
    }
    static final int TRANSACTION_registerLogger = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_add = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    public static boolean setDefaultImpl(com.gotak.address.aidl.SimpleService impl) {
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
    public static com.gotak.address.aidl.SimpleService getDefaultImpl() {
      return Stub.Proxy.sDefaultImpl;
    }
  }
  /**
       * Pass a logging mechanism over to the Service so that the logs can be written to the
       * appropriate logger.
       */
  public void registerLogger(com.gotak.address.aidl.ILogger log) throws android.os.RemoteException;
  /**
       * Adds two numbers and returns the result.
       */
  public int add(int a, int b) throws android.os.RemoteException;
}
