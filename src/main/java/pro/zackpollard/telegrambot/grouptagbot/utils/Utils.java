package pro.zackpollard.telegrambot.grouptagbot.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * @author Zack Pollard
 */
public class Utils {

    public static boolean writeFile(File location, String contents) {

        FileOutputStream tempOutputStream;

        try {
            File tempFile = new File(location.getPath() + ".tmp");
            tempOutputStream = new FileOutputStream(tempFile);
            tempOutputStream.write(contents.getBytes());
            tempOutputStream.close();

            Files.move(tempFile.toPath(), location.toPath(), StandardCopyOption.REPLACE_EXISTING);

            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.err.println("The file could not be saved as the file couldn't be found on the storage device. Please check the directories read/write permissions and contact the developer!");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("The file could not be written to as an error occurred. Please check the directories read/write permissions and contact the developer!");
        }

        return false;
    }
}
