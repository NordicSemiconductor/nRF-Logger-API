nRF-Logger-API
==============

![Icon](https://github.com/NordicSemiconductor/nRF-Logger-API/blob/master/art/icon.png)

The library allows to easily create custom log entries from your application in the nRF Logger. It is being used by nRF Master Control Panel and nRF Toolbox, that are available on Google Play.
The logger may be used for debugging purposes, as LogCat is not always available.

nRF Logger is available for Android 4.1.* and newer.

###Features
1. Create log session from your application
2. Append log entries to the session
3. 5 log levels are available: DEBUG, VERBOSE, INFO, WARNING, ERROR
4. Open the log in nRF Logger for viewing

###Example
The example project may be found in samples folder. It contains a simple application that shows how to create a log session:

    mLogSession = Logger.newSession(getActivity(), key, name);
	
and add entries:

    Logger.log(mLogSession, Level.INFO, text);
    Logger.e(mLogSession, R.string.error, someArg);
   
If nRF Logger application is not installed on the device those methods does nothing.