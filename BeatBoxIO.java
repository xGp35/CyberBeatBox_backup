import java.io.*;
import java.nio.file.*;

public class BeatBoxIO {
    // private boolean[] checkboxState;

    public boolean[] loadFromFile(File file) throws IOException {

        boolean[] checkboxState;

        try (ObjectInputStream is = new ObjectInputStream(new FileInputStream(file))) {
            checkboxState = (boolean[]) is.readObject(); // readObject() returns reference type Object
            return checkboxState;
        } catch (ClassNotFoundException ex) {
            // readObject can throw this. As this method only throws IO exception
            // we are handling the other exception type that may occur here.
            // Also ChatGPT said, users don't care about ClassNotFound
            throw new IOException("The save file is invalid or incompatible.", ex);
            // In other words - Whatever class was saved in this file, doesn't exist in this program
        }
    }

    public void saveToFile(File file, boolean[] checkboxState) throws IOException {

        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(file))) {
            os.writeObject(checkboxState);
        }
    }
}