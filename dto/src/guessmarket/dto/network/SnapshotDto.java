package guessmarket.dto.network;

import guessmarket.dto.AccountEntryDto;
import guessmarket.dto.BalancePointDto;
import guessmarket.dto.EventTradingStatusDto;
import guessmarket.dto.PricePointDto;
import guessmarket.dto.UserDto;
import guessmarket.dto.UserEventInvolvementDto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One coherent, immutable pull response. Private account details always belong to the session. */
public record SnapshotDto(List<EventTradingStatusDto> events,
                          Map<Integer, List<PricePointDto>> priceHistories,
                          List<PublicUserDto> users,
                          UserDto self,
                          List<UserEventInvolvementDto> involvements,
                          List<BalancePointDto> balanceHistory,
                          List<AccountEntryDto> accountEntries,
                          List<ChatMessageDto> chat) {
    public SnapshotDto {
        events = List.copyOf(events);
        Map<Integer, List<PricePointDto>> histories = new LinkedHashMap<>();
        priceHistories.forEach((id, points) -> histories.put(id, List.copyOf(points)));
        priceHistories = Map.copyOf(histories);
        users = List.copyOf(users);
        involvements = List.copyOf(involvements);
        balanceHistory = List.copyOf(balanceHistory);
        accountEntries = List.copyOf(accountEntries);
        chat = List.copyOf(chat);
    }
}
