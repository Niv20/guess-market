package guessmarket.dto;

import java.util.List;

/**
 * One user of the system, as the users screen lists them.
 *
 * <p>A user is blocked the moment an action leaves their balance below zero. From then on they can
 * do nothing at all, which is why the flag travels with every copy of them rather than being
 * worked out from the balance: a user who was blocked and later received money from a settlement
 * stays blocked.
 *
 * @param name                 the user's name, which is unique in the system
 * @param balance              what their account holds now
 * @param initialBalance       what the file said it held to begin with
 * @param blocked              whether they have been shut out after going below zero
 * @param marketMakerEventIds  the events they run, in event order
 * @param participatingEventIds the events they have acted in, in event order
 */
public record UserDto(String name,
                      double balance,
                      double initialBalance,
                      boolean blocked,
                      List<Integer> marketMakerEventIds,
                      List<Integer> participatingEventIds) {

    public UserDto {
        marketMakerEventIds = List.copyOf(marketMakerEventIds);
        participatingEventIds = List.copyOf(participatingEventIds);
    }

    /** @return how much the user has made or lost since the file was loaded. */
    public double netResult() {
        return balance - initialBalance;
    }

    public boolean isMarketMaker() {
        return !marketMakerEventIds.isEmpty();
    }
}
