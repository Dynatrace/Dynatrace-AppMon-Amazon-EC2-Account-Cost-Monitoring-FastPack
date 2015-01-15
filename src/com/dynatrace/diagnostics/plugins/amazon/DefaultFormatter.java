package com.dynatrace.diagnostics.plugins.amazon;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Logs a record with the pattern "YYYY-MM-DD HH:MM:SS LEVEL [CLASS] MESSAGE".
 * Additionally {@link Throwable}s are logged in a separate line.
 *
 * @author martin.wurzinger
 */
public class DefaultFormatter extends Formatter {

    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    // TODO: move to BaseConstants
    public static final String WS = " ";
    public static final String COLON = ":";
    public static final String BRACKET_LEFT = "[";
    public static final String BRACKET_RIGHT = "]";
    public static final String DOT = ".";

    private static final StringBuilder BUILDER = new StringBuilder();
    private static final Date DATE = new Date();

    private static final int APP_DEFAULT_LENGTH = 7;
    private static final int LEVEL_DEFAULT_LENGTH = 7;
    private static final int CLASS_DEFAULT_LENGTH = 12;

    private static String appId = null;

    /** Allows to provide an application id for cases
     * where multiple applications log to the same output, e.g.
     * in the Launcher output window.
     *
     * @param appId
     * @author dominik.stadler
     */
	public static void setAppId(String appId) {
		DefaultFormatter.appId = appId;
	}

	/**
     * Format the given LogRecord.
     *
     * @param record the log record to be formatted.
     * @return a formatted log record
     */
    @Override
	public synchronized String format(LogRecord record) {
        // clear string builder content of previous runs
        BUILDER.setLength(0);

        // build log message
        appendDateTime(record);
        appendApplication();
        appendLevel(record);
        appendClass(record);
        appendMessage(record);
        appendThrowable(record);
        appendNewLine();

        return BUILDER.toString();
    }

	private void appendDateTime(LogRecord record) {
        DATE.setTime(record.getMillis());
        BUILDER.append(DATE_TIME_FORMAT.format(DATE));
        BUILDER.append(WS);
    }

    public static String repeat(String str, int repeat)
    {
        if(str == null)
            return null;
        if(repeat <= 0)
            return "";
        int inputLength = str.length();
        if(repeat == 1 || inputLength == 0)
            return str;
        /*if(inputLength == 1 && repeat <= 8192)
            return padding(repeat, str.charAt(0));*/
        int outputLength = inputLength * repeat;
        switch(inputLength)
        {
        case 1: // '\001'
            char ch = str.charAt(0);
            char output1[] = new char[outputLength];
            for(int i = repeat - 1; i >= 0; i--)
                output1[i] = ch;

            return new String(output1);

        case 2: // '\002'
            char ch0 = str.charAt(0);
            char ch1 = str.charAt(1);
            char output2[] = new char[outputLength];
            for(int i = repeat * 2 - 2; i >= 0; i--)
            {
                output2[i] = ch0;
                output2[i + 1] = ch1;
                i--;
            }

            return new String(output2);
        }
        StringBuilder buf = new StringBuilder(outputLength);
        for(int i = 0; i < repeat; i++)
            buf.append(str);

        return buf.toString();
    }

    private void appendApplication() {
		if(appId != null) {
			BUILDER.append(appId.substring(0, appId.length() > APP_DEFAULT_LENGTH ? APP_DEFAULT_LENGTH : appId.length()));
	        BUILDER.append(repeat(WS, APP_DEFAULT_LENGTH - appId.length()));
	        BUILDER.append(WS);
		}
	}

    private synchronized void appendLevel(LogRecord record) {
        String levelName = record.getLevel().getName();

        BUILDER.append(levelName);
        BUILDER.append(repeat(WS, LEVEL_DEFAULT_LENGTH - levelName.length()));
        BUILDER.append(WS);
    }

    private void appendClass(LogRecord record) {
        String className = record.getLoggerName();
        int lastdot = className.lastIndexOf(DOT);
        if (lastdot > 0) {
            className = className.substring(lastdot + 1);
        }

        BUILDER.append(BRACKET_LEFT);
        BUILDER.append(className);
        BUILDER.append(BRACKET_RIGHT);

        BUILDER.append(repeat(WS, CLASS_DEFAULT_LENGTH - className.length()));

        BUILDER.append(WS);
    }

    private void appendMessage(LogRecord record) {
        BUILDER.append(formatMessage(record));
    }

    private void appendThrowable(LogRecord record) {
        // log exception
        Throwable throwable = record.getThrown();
        if (throwable == null) {
            return;
        }

        appendThrowableSource(record);
        appendNewLine();
        appendStackTrace(throwable);
    }

    private void appendThrowableSource(LogRecord record) {
        BUILDER.append(COLON);
        BUILDER.append(WS);

        if (record.getSourceClassName() != null) {
            BUILDER.append(record.getSourceClassName());
        }
        if (record.getSourceMethodName() != null) {
            BUILDER.append(WS);
            BUILDER.append(record.getSourceMethodName());
        }
    }


    private void appendStackTrace(Throwable throwable) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            pw.close();
            BUILDER.append(sw.toString());
        } catch (Exception ex) {
            //ok to ignore this
        }
    }

    private void appendNewLine() {
        BUILDER.append(LINE_SEPARATOR);
    }
}

