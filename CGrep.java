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
import java.util.* ;

import akka.actor.*;

public class CGrep {
	
	// Actors and ActorSystem
	static private ActorRef[] scanActors;
    static private ActorRef collectionActor;
    private static ActorSystem system = ActorSystem.create("MySystem");
    
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
        fis.close();
        
        //Return found object
        return found;
    }
    
    static class ScanActor extends UntypedActor {
    	
    	public void onReceive(Object message) {
    		// Receive Configure, check if it's a file, and run grep
    		String filename = ((Configure) message).getFile();
    		File file = new File(filename);
    		if (file.isFile())
				try {
					grep(file);
				} catch (IOException e) {
					e.printStackTrace();
				}
			else
    			grep(filename);
    	}
    }
    
    static class CollectionActor extends UntypedActor {
    	
    	private int count;
    	
    	public void onReceive(Object message) {
    		// Receive FileCount
    		if (message instanceof FileCount)
    			count = ((FileCount) message).getCount();
    		// Receive Found and output
    		else if (message instanceof Found) {
    			((Found) message).printIndex();
    			count--;
    			if (count == 0)
    				system.shutdown();
    		}
    	}
    }

    public static void main(String[] args) {
        // No arguments provided
        if (args.length < 2) {
            System.err.println("Usage: java Grep pattern file/String...");
            return;
        }

        // Check for valid regex pattern
        compile(args[0]);
        
        // Create CollectionActor and start it
        collectionActor = system.actorOf(Props.create(CollectionActor.class));
        collectionActor.start();

        // Send FileCount message to CollectionActor
        int count = 0;
        ArrayList<Configure> configures = new ArrayList<Configure>();
        for(int a = 1; a < args.length; a++) {
        	if (new File(args[a]).isFile()) {
        		count++;
        		Configure config = new Configure(args[a], collectionActor);
        		configures.add(config);
        	}
        }
        collectionActor.tell(new FileCount(count), collectionActor);
        
        
        if (count == 0) { // Create ScanActor for standard input (no files)
        	Configure config = new Configure(args[1], collectionActor);
        	scanActors = new ActorRef[1];
        	scanActors[0] = system.actorOf(Props.create(ScanActor.class));
        	scanActors[0].start();
        	scanActors[0].tell(config, collectionActor);
        } else { // Create one ScanActor for each file, start it, and send each a Configure
	        scanActors = new ActorRef[count];
	        for(int b = 0; b < count; b++) {
	        	scanActors[b] = system.actorOf(Props.create(ScanActor.class));
	        	scanActors[b].start();
	        	scanActors[b].tell(configures.get(b), collectionActor);
	        }
        }

    }

}
