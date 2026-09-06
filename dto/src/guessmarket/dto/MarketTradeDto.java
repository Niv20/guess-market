package guessmarket.dto;

/**
 * One completed transaction in an event, as the trading history shows it.
 *
 * <p>The same record describes every way shares can change hands, which is why both names may be
 * absent: an LMSR purchase has no seller because the shares come from the event itself, and a mint
 * has no seller either because the shares did not exist a moment earlier.
 *
 * @param serialNumber  the position of this trade in the history of its event, starting at 1
 * @param kind          how the trade came about
 * @param optionName    the option whose shares moved
 * @param shares        how many of them moved
 * @param pricePerShare what one share went for
 * @param totalPrice    what the shares cost altogether, before any commission
 * @param commission    the commission paid on top of that price, which is zero unless the event
 *                      collects its commission on every purchase
 * @param buyerName     who received the shares
 * @param sellerName    who gave them up, or null when they came from the event or were minted
 */
public record MarketTradeDto(int serialNumber,
                             TradeKind kind,
                             String optionName,
                             long shares,
                             double pricePerShare,
                             double totalPrice,
                             double commission,
                             String buyerName,
                             String sellerName) {

    /** @return what the buyer parted with altogether. */
    public double totalPaidByBuyer() {
        return totalPrice + commission;
    }
}
