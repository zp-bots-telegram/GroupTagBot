package pro.zackpollard.telegrambot.grouptagbot.data;

import lombok.Data;
import pro.zackpollard.telegrambot.grouptagbot.Group;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
