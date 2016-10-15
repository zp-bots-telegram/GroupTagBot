package pro.zackpollard.telegrambot.grouptagbot.data;

import lombok.Data;

import java.util.TreeMap;

/**
 * @author Zack Pollard
 */
@Data
public class UsernameCache {

    private final TreeMap<Long, String> usernameCache;

    public UsernameCache() {

        this.usernameCache = new TreeMap<>();
    }

    // returns true if an update operation was performed
    public boolean updateUsername(Long userID, String newUsername) {

        if(newUsername != null && !newUsername.equals("")) {
            if (newUsername.charAt(0) == '@') newUsername = newUsername.substring(1);
            return this.usernameCache.put(userID, newUsername.toLowerCase()) != null;
        }

        return false;
    }
}
