package com.cybershield.entity;

import com.cybershield.enums.HistoryAction;
import com.cybershield.enums.TaskStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.Immutable;

/**
 * Append-only history of a remediation task. The application never updates or deletes these rows.
 * Table: remediation_history
 */
@Entity
@Immutable
@Table(name = "remediation_history")
public class RemediationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private RemediationTask task;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    private HistoryAction actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = true, length = 30, columnDefinition = "VARCHAR(30)")
    private TaskStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = true, length = 30, columnDefinition = "VARCHAR(30)")
    private TaskStatus newStatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "previous_assignee_id", nullable = true)
    private User previousAssignee;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "new_assignee_id", nullable = true)
    private User newAssignee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "performed_by_id", nullable = false)
    private User performedBy;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public RemediationHistory() {
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RemediationTask getTask() {
        return task;
    }

    public void setTask(RemediationTask task) {
        this.task = task;
    }

    public HistoryAction getActionType() {
        return actionType;
    }

    public void setActionType(HistoryAction actionType) {
        this.actionType = actionType;
    }

    public TaskStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(TaskStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public TaskStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(TaskStatus newStatus) {
        this.newStatus = newStatus;
    }

    public User getPreviousAssignee() {
        return previousAssignee;
    }

    public void setPreviousAssignee(User previousAssignee) {
        this.previousAssignee = previousAssignee;
    }

    public User getNewAssignee() {
        return newAssignee;
    }

    public void setNewAssignee(User newAssignee) {
        this.newAssignee = newAssignee;
    }

    public User getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(User performedBy) {
        this.performedBy = performedBy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
