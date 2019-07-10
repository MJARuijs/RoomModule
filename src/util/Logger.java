package util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * Logger class, which handles both normal logs, as well as error messages.
 * Both types of logs can be written to the terminal or to a file.
 */
public final class Logger {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("hh;mm;ss_dd-MM-yyyy", Locale.ENGLISH);
    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("hh:mm:ss", Locale.ENGLISH);

    private static PrintStream errOutStream = System.err;
    private static PrintStream fileOutStream = System.out;
    private static PrintStream loggerOutStream = System.out;

    private static List<String> errors = new ArrayList<>();

    private static boolean printTag = true;
    private static boolean printColored = true;
    private static boolean printTimeStamp = true;
    private static boolean writeErrorToFile;

    /**
     * Private constructor, since the Logger should never be instantiated.
     */
    private Logger() {

    }

    /**
     * Sets whether of not the error messages should be written to a file.
     * @param writeErrorToFile whether or not the error messages should be written to a file.
     */
    public static void setWriteErrorToFile(final boolean writeErrorToFile) {
        Logger.writeErrorToFile = writeErrorToFile;
    }

    /**
     * Setter for the printColored variable.
     * @param printColored determines whether or not the Logger should print colored text or not.
     */
    public static void setPrintColored(final boolean printColored) {
        Logger.printColored = printColored;
    }

    /**
     * Setter for the printTimeStamp variable.
     * @param printTimeStamp determines whether or not the Logger should print a timestamp in front of each line.
     */
    public static void setPrintTimeStamp(final boolean printTimeStamp) {
        Logger.printTimeStamp = printTimeStamp;
    }

    /**
     * Setter for the printTag variable.
     * @param printTag determines whether or not the Logger should print the tag that corresponds to the given severity.
     */
    public static void setPrintTag(final boolean printTag) {
        Logger.printTag = printTag;
    }

    /**
     * Set the print stream of the Logger.
     * @param printStream the print stream for the Logger.
     */
    public static void setOut(final PrintStream printStream) {
        loggerOutStream = printStream;
    }

    /**
     * Set the print stream for the specified type.
     * @param printStream the print stream.
     * @param type the type of the print stream.
     */
    public static void setOut(final PrintStream printStream, final PrintStreamType type) {
        if (type == PrintStreamType.FILE) {
            fileOutStream = printStream;
        } else {
            loggerOutStream = printStream;
        }
    }

    /**
     * Set the print stream of the Logger to write to a file, specified by the given file name.
     * @param fileName the file name to which the Logger should write.
     * @param overWrite whether or not the Logger can overwrite, if the given file already exists.
     * @param type the type of the print stream that needs to be set.
     */
    public static void setOut(final String fileName, final boolean overWrite, final PrintStreamType type) {
        if (fileName == null || fileName.isEmpty()) {
            return;
        }
        final File file = new File(fileName);

        if (!file.exists() || overWrite) {
            try {
                if (!file.exists() && !file.createNewFile()) {
                    err("Could not create output file: " + fileName);
                    return;
                }

                if (type == PrintStreamType.FILE) {
                    fileOutStream = new PrintStream(file);
                } else {
                    loggerOutStream = new PrintStream(file);
                }
            } catch (IOException e) {
                err("Could not create output file: " + fileName);
            }
        } else {
            err("Could not write output to file: " + fileName + " because it already exists.\n"
                    + "The output is written to the console instead.\n"
                    + "Use -x to allow for overwriting of existing files.");
        }
    }

    /**
     * Standard log method, which simply prints a message to the terminal.
     * @param message the message to be written to the terminal.
     */
    public static void log(final String message) {
        if (message == null) {
            err("Cannot print null string!");
            return;
        }

        if (loggerOutStream == System.out && fileOutStream == System.out) {
            fileOutStream.print(Color.DEFAULT.getCode());
        }

        if (printTimeStamp) {
            fileOutStream.print(createTimeStamp());
        }

        if (printTag) {
            fileOutStream.print(createTag(Severity.INFO));
        }

        fileOutStream.print(message + "\n");
    }

    /**
     * Log a message with certain severity.
     * @param message the message to be logged.
     * @param severity the severity of the message.
     */
    private static void log(final String message, final Severity severity) {
        if (printColored) {
            loggerOutStream.print(severity.getColor().getCode());
        }

        if (printTimeStamp) {
            loggerOutStream.print(createTimeStamp());
        }

        if (printTag) {
            loggerOutStream.print(createTag(severity));
        }

        loggerOutStream.print(message + "\n");
    }

    /**
     * Helper method that creates a tag for the given severity.
     * @param severity the severity for which a tag must be created.
     * @return the string representation of the given severity.
     */
    private static String createTag(final Severity severity) {
        return "[" + severity.getType() + "] ";
    }

    /**
     * Helper method that creates a timestamp.
     * @return the string representation of the timestamp.
     */
    private static String createTimeStamp() {
        final Date date = new Date();

        synchronized (TIME_FORMAT) {
            final String timeStamp = TIME_FORMAT.format(date);
            return "[" + timeStamp + "] ";
        }
    }

    /**
     * Overload log method. Automatically prints with INFO severity.
     * @param message the message to be logged.
     */
    public static void info(final String message) {
        log(message, Severity.INFO);
    }

    /**
     * Overload log method. Automatically prints with WARNING severity.
     * @param message the message to be logged.
     */
    public static void warn(final String message) {
        log(message, Severity.WARNING);
    }

    /**
     * Overload log method. Automatically prints with DEBUG severity.
     * @param message the message to be logged.
     */
    public static void debug(final String message) {
        log(message, Severity.DEBUG);
    }

    /**
     * Prints an error message to either the terminal, or to a file, depending on the writeErrorToFile parameter.
     * If the message should be written to a file, create a crash report file, with the title of the file being
     * the current date and time.
     * @param message the error message to be printed to either the terminal, or to a file.
     */
    public static void err(final String message) {
        if (message == null) {
            err("Cannot print a null string!");
            return;
        }

        errors.add(message);
    }

    /**
     * Sets the PrintStream for the errorOutStream.
     * @param outStream the new PrintStream.
     */
    public static void setErr(final PrintStream outStream) {
        errOutStream = outStream;
    }

    /**
     * Prints the errors to the console or crash report file.
     */
    public static void printErrors() {
        if (!errors.isEmpty()) {
            final StringBuilder output = new StringBuilder();

            if (printColored) {
                output.append(Color.RED.getCode());
            }

            if (printTimeStamp) {
                output.append(createTimeStamp());
            }

            if (printTag) {
                output.append("[ERROR] ");
            }

            final StringBuilder errorMessage = new StringBuilder();

            for (final String error : errors) {
                errorMessage.append(error).append("\n\n\n");
            }

            output.append(errorMessage.toString().trim());

            if (writeErrorToFile) {
                final Date date = new Date();
                synchronized (DATE_FORMAT) {
                    final String fileName = "crash_report_" + DATE_FORMAT.format(date) + ".txt";
                    setErrorStream(fileName);
                }
            }

            errOutStream.print(output.toString() + "\n");
            errors.clear();
        }
    }

    /**
     * Sets the errOutStream to the file at the given fileName.
     * @param fileName name of the error file.
     */
    static void setErrorStream(final String fileName) {
        try {
            if (writeErrorToFile) {
                errOutStream.print(Color.RED.getCode()
                        + "An error occurred! Check the file to see what went wrong: " + fileName + '\n');
            }
            errOutStream = new PrintStream(new File(fileName));
        } catch (FileNotFoundException e) {
            err("Could not create error file: " + fileName + ". " + e.getMessage());
        }
    }

    /**
     * Flushes the output stream.
     */
    public static void flush() {
        fileOutStream.flush();
        loggerOutStream.flush();
        errOutStream.flush();
    }

    /**
     * First flushes the output stream, then closes it.
     */
    public static void close() {
        fileOutStream.flush();
        if (fileOutStream != System.out) {
            fileOutStream.close();
        }

        loggerOutStream.flush();
        if (loggerOutStream != System.out) {
            loggerOutStream.close();
        }

        errors.clear();
        errOutStream.flush();
        errOutStream.close();
    }

}
