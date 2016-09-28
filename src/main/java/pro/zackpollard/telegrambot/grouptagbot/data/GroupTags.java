package pro.zackpollard.telegrambot.grouptagbot.data;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Zack Pollard
 */
@Data
public class GroupTags {

    private final Map<Long, Group> groups;

    public GroupTags() {

        this.groups = new HashMap<>();
    }
}
