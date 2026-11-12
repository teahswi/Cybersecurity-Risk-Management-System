package com.cybershield.repository;

import com.cybershield.entity.Notification;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    boolean existsByRecipientIdAndDedupKey(Long recipientId, String dedupKey);

    @Query("select n from Notification n where n.recipient.id = :recipientId order by n.createdAt desc, n.id desc")
    List<Notification> findRecent(@Param("recipientId") Long recipientId, Pageable pageable);

    @Query(value = "select n from Notification n where n.recipient.id = :recipientId "
            + "and (:unreadOnly = false or n.readAt is null)",
            countQuery = "select count(n) from Notification n where n.recipient.id = :recipientId "
            + "and (:unreadOnly = false or n.readAt is null)")
    Page<Notification> findForRecipient(@Param("recipientId") Long recipientId, @Param("unreadOnly") boolean unreadOnly,
                                        Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notification n set n.readAt = :now where n.recipient.id = :recipientId and n.readAt is null")
    int markAllRead(@Param("recipientId") Long recipientId, @Param("now") LocalDateTime now);
}
