// INotificationService.aidl
package com.gotak.address.aidl;

import com.gotak.address.aidl.ILogger;


interface SimpleService {



    /**
     * Pass a logging mechanism over to the Service so that the logs can be written to the
     * appropriate logger.
     */
    void registerLogger(ILogger log);

    /**
     * Adds two numbers and returns the result.
     */
    int add(int a, int b);
}
