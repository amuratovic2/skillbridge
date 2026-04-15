package com.skillbridge.communication.repository;

import com.skillbridge.communication.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Integer> {

    @Query("SELECT m FROM Message m WHERE " +
           "(m.senderId = :userId AND m.receiverId = :otherUserId) OR " +
           "(m.senderId = :otherUserId AND m.receiverId = :userId) " +
           "ORDER BY m.sentAt DESC")
    Page<Message> findConversation(
        @Param("userId") Integer userId,
        @Param("otherUserId") Integer otherUserId,
        Pageable pageable
    );

    Page<Message> findByOrderId(Integer orderId, Pageable pageable);

    @Query(value = """
        SELECT partner_id, last_message, last_at, unread_count FROM (
            SELECT
                CASE WHEN m.sender_id = :userId THEN m.receiver_id ELSE m.sender_id END AS partner_id,
                FIRST_VALUE(m.content) OVER (
                    PARTITION BY CASE WHEN m.sender_id = :userId THEN m.receiver_id ELSE m.sender_id END
                    ORDER BY m.sent_at DESC
                ) AS last_message,
                MAX(m.sent_at) OVER (
                    PARTITION BY CASE WHEN m.sender_id = :userId THEN m.receiver_id ELSE m.sender_id END
                ) AS last_at,
                SUM(CASE WHEN m.is_read = false AND m.receiver_id = :userId THEN 1 ELSE 0 END) OVER (
                    PARTITION BY CASE WHEN m.sender_id = :userId THEN m.receiver_id ELSE m.sender_id END
                ) AS unread_count,
                ROW_NUMBER() OVER (
                    PARTITION BY CASE WHEN m.sender_id = :userId THEN m.receiver_id ELSE m.sender_id END
                    ORDER BY m.sent_at DESC
                ) AS rn
            FROM communication.messages m
            WHERE m.sender_id = :userId OR m.receiver_id = :userId
        ) sub WHERE rn = 1
        ORDER BY last_at DESC
        """, nativeQuery = true)
    List<Object[]> findConversationList(@Param("userId") Integer userId);

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.senderId = :senderId AND m.receiverId = :userId AND m.isRead = false")
    int markAsRead(@Param("userId") Integer userId, @Param("senderId") Integer senderId);
}
