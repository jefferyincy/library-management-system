package util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {
    public static <T> void save(String filename, List<T> list) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(list);
        } catch (Exception e) {
            System.out.println("Error saving file: " + filename);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> load(String filename) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            return (List<T>) ois.readObject();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
