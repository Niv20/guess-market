package guessmarket.dto;

/**
 * Everything one user has to do with one event.
 *
 * <p>Exactly one of the two involvement records is present, matching the trading method of the
 * event, because what there is to say about a participant is genuinely different under the two
 * methods: an LMSR participant has a list of purchases, while an order book participant has a
 * position and orders waiting in the market.
 *
 * @param event        the event, so that a screen showing an involvement need not fetch it again
 * @param marketMaker  whether this user is the one who runs the event
 * @param participant  whether this user has acted in the event at all
 * @param lmsr         what they have done, for an LMSR event; null otherwise
 * @param orderBook    what they have going on, for an order book event; null otherwise
 */
public record UserEventInvolvementDto(EventDto event,
                                      boolean marketMaker,
                                      boolean participant,
                                      UserLmsrInvolvementDto lmsr,
                                      UserOrderBookInvolvementDto orderBook) {
}
