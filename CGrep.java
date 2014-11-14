
import java.util.concurrent.*;
import java.util.regex.*;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;

public class CGrep {
    
    // Charset and decoder for ISO-8859-15
    final private static Charset charset = Charset.forName("ISO-8859-15");
    final private static CharsetDecoder decoder = charset.newDecoder();

    // Pattern used to parse lines
    final private static Pattern linePattern
        = Pattern.compile(".*\r?\n");

    // The input pattern that we're looking for
    private static Pattern pattern;

    // Compile the pattern from the command line
    //
    private static void compile(String pat) {
        try {
            pattern = Pattern.compile(pat);
        } catch (PatternSyntaxException x) {
            System.err.println(x.getMessage());
            System.exit(1);
        }
    }
    
    // Use the linePattern to break the given CharBuffer into lines, applying
    // the input pattern to each line to see if we have a match
    //
    private static Found grep(String s) {
        CharBuffer cb = CharBuffer.wrap(s+"\n");
        Matcher lm = linePattern.matcher(cb);   // Line matcher
        Matcher pm = null;                      // Pattern matcher
        Found found = new Found();
        
        found.setName(s);
        int lines = 0;
        while (lm.find()) {
            lines++;
            CharSequence cs = lm.group();       // The current line
            if (pm == null)
                pm = pattern.matcher(cs);
            else
                pm.reset(cs);
            if (pm.find())
                found.addToIndex(cs.toString(),lines);
            if (lm.end() == cb.limit())
                break;
        }
        return found;
    }

    // Use the linePattern to break the given CharBuffer into lines, applying
    // the input pattern to each line to see if we have a match
    //
    private static Found grep(File f, CharBuffer cb) {
        Matcher lm = linePattern.matcher(cb);   // Line matcher
        Matcher pm = null;                      // Pattern matcher
        Found found = new Found();
        
        found.setName(f.getName());
        int lines = 0;
        while (lm.find()) {
            lines++;
            CharSequence cs = lm.group();       // The current line
            if (pm == null)
                pm = pattern.matcher(cs);
            else
                pm.reset(cs);
            if (pm.find())
                found.addToIndex(cs.toString(),lines);
            if (lm.end() == cb.limit())
                break;
        }
        return found;
    }

    // Search for occurrences of the input pattern in the given file
    //
    private static Found grep(File f) throws IOException {

        // Open the file and then get a channel from the stream
        FileInputStream fis = new FileInputStream(f);
        FileChannel fc = fis.getChannel();

        // Get the file's size and then map it into memory
        int sz = (int)fc.size();
        MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);

        // Decode the file into a char buffer
        CharBuffer cb = decoder.decode(bb);

        // Perform the search
        Found found = grep(f, cb);

        // Close the channel and the stream
        fc.close();
        
        //Return found object
        return found;
    }

    public static void main(String[] args) {
        final int NTHREADS = 3;
        final ExecutorService es
                = Executors.newFixedThreadPool(NTHREADS);
        List<Future<Found>> list = new ArrayList<Future<Found>>();
        boolean firstArg = true;
        Future<Found> future;
        
        // No arguments provided
        if (args.length < 2) {
            System.err.println("Usage: java Grep pattern file/String...");
            return;
        }

        // Check for valid regex pattern
        compile(args[0]);

        // Check each subsequent arguement for files vs strings
        for (final String arg : args) {
            if (firstArg) {
                firstArg = false; // Skip pattern argeument
            } else {
                if (new File(arg).isFile()) {
                    // Perform string mining
                    future = es.submit(new Callable<Found>() {
                        @Override
                        public Found call() throws Exception {
                            File file = new File(arg);
                            return grep(file);
                        }
                    });
                    list.add(future);
                } else {
                    // Regular regex match
                    future = es.submit(new Callable<Found>() {
                        @Override
                        public Found call() throws Exception {
                            return grep(arg);                            
                        }
                    });
                    list.add(future);
                }
            }
        }

        // Display Results
        for (Future<Found> fut : list) {
            try {
                if(!fut.get().isEmpty())
                {
                   System.out.println("Found Match: " + fut.get().getName());
                   fut.get().printIndex(); 
                }                
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        // When all jobs are complete, end the ExecutorService
        es.shutdown();

    }

}
