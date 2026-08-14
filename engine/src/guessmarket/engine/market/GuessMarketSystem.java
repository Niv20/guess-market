package guessmarket.engine.market;

import guessmarket.engine.exception.EventNotFoundException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything the system currently knows about: the events that were loaded from the last valid
 * file, in the order that file listed them.
 *
 * <p>A new instance is built for every successful load, which is what makes a load replace the
 * previous data completely while a failed load leaves the running system untouched.
 */
public class GuessMarketSystem implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<Integer, Event> eventsById = new LinkedHashMap<>();

    /** Adds an event to the system. Ids are known to be unique by the time this is called. */
    public void addEvent(Event event) {
        eventsById.put(event.getId(), event);
    }

    /** @return every event, in the order in which it appeared in the loaded file. */
    public Collection<Event> getEvents() {
        return eventsById.values();
    }

    /** @return only the events that are still open for trading, in file order. */
    public List<Event> getActiveEvents() {
        List<Event> activeEvents = new ArrayList<>();
        for (Event event : eventsById.values()) {
            if (event.isActive()) {
                activeEvents.add(event);
            }
        }
        return activeEvents;
    }

    public int getEventCount() {
        return eventsById.size();
    }

    /**
     * @return the event with the given id
     * @throws EventNotFoundException if no event in the system carries that id
     */
    public Event getEvent(int eventId) {
        Event event = eventsById.get(eventId);
        if (event == null) {
            throw new EventNotFoundException(eventId);
        }
        return event;
    }
}
