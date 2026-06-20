package com.pollybreak.footballcore.repository;

import com.pollybreak.footballcore.domain.entity.TelegramKnownChat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelegramKnownChatRepository extends JpaRepository<TelegramKnownChat, Long> {

    List<TelegramKnownChat> findAllByActiveTrueAndChatTypeInOrderByTitleAscChatIdAsc(List<String> chatTypes);
}
