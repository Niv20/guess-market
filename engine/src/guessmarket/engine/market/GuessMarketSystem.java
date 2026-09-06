package guessmarket.engine.market;

import guessmarket.engine.exception.EventNotFoundException;
import guessmarket.engine.exception.UserNotFoundException;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Everything the system currently knows about: the users and the events that came out of the last
 * file it accepted, each in the order that file listed them.
 *
 * <p>A whole new instance is built for every successful load, and that single fact is what makes a
 * load replace the previous data completely while a load that fails leaves the running system
 * untouched: the new system is only put in place once it has been built from end to end.
 *
 * <p>Users are found by name without regard to letter case, because every name the system is asked
 * about comes from somebody typing or picking it, and {@code Avrum} and {@code avrum} are plainly
 * meant to be the same person. The name a user was given in the file is the one that is shown.
 */
public class GuessMarketSystem implements Serializable, UserDirectory {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Map<Integer, Event> eventsById = new LinkedHashMap<>();
    private final Map<String, User> usersByName = new LinkedHashMap<>();

    // ------------------------------------------------------------------ building the system

    /** Adds an event. Its id is known to be free by the time this is called. */
    public void addEvent(Event event) {
        eventsById.put(event.getId(), event);
    }

    /** Adds a user. Their name is known to be free by the time this is called. */
    public void addUser(User user) {
        usersByName.put(key(user.getName()), user);
    }

    /** Records that a user runs an event, which is what the file's market maker element says. */
    public void assignMarketMaker(String userName, int eventId) {
        requireUser(userName).addMarketMakerEvent(eventId);
    }

    // ------------------------------------------------------------------ reading the system

    /** @return every event, in the order in which it appeared in the loaded file. */
    public List<Event> getEvents() {
        return List.copyOf(eventsById.values());
    }

    /** @return every user, in the order in which they appeared in the loaded file. */
    public List<User> getUsers() {
        return List.copyOf(usersByName.values());
    }

    public int getEventCount() {
        return eventsById.size();
    }

    public int getUserCount() {
        return usersByName.size();
    }

    public boolean hasEvent(int eventId) {
        return eventsById.containsKey(eventId);
    }

    public boolean hasUser(String userName) {
        return userName != null && usersByName.containsKey(key(userName));
    }

    /**
     * @return the event with the given id
     * @throws EventNotFoundException if no event in the system carries that id
     */
    public Event requireEvent(int eventId) {
        Event event = eventsById.get(eventId);
        if (event == null) {
            throw new EventNotFoundException(eventId);
        }
        return event;
    }

    @Override
    public User requireUser(String userName) {
        User user = userName == null ? null : usersByName.get(key(userName));
        if (user == null) {
            throw new UserNotFoundException(userName);
        }
        return user;
    }

    /**
     * @return an id no event is using, for an event somebody creates while the program is running.
     *         It carries on from the highest id in the file, so a new event never takes the number
     *         of one that was loaded.
     */
    public int nextFreeEventId() {
        int highest = 0;
        for (Integer id : eventsById.keySet()) {
            highest = Math.max(highest, id);
        }
        return highest + 1;
    }

    private static String key(String userName) {
        return userName.trim().toLowerCase(Locale.ROOT);
    }
}
