package pro.zackpollard.telegrambot.grouptagbot.data;

import lombok.Data;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Zack Pollard
 */
@Data
public class Tag {

    private final String tag;
    private final Set<Long> users;

    public Tag(String tag) {

        this.tag = tag;
        this.users = new HashSet<>();
    }
}
