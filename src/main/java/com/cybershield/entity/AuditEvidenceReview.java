package com.cybershield.entity;

import com.cybershield.enums.AuditReviewDecision;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.Immutable;

/**
 * Review decision on audit evidence.
 * Table: audit_evidence_reviews
 */
@Entity
@Immutable
@Table(name = "audit_evidence_reviews")
public class AuditEvidenceReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evidence_id", nullable = false)
    private AuditEvidence evidence;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private AuditReviewDecision decision;

    @Column(name = "review_notes", nullable = true, columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "reviewed_at", nullable = false)
    private LocalDateTime reviewedAt;

    public AuditEvidenceReview() {
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (reviewedAt == null) {
            reviewedAt = now;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AuditEvidence getEvidence() {
        return evidence;
    }

    public void setEvidence(AuditEvidence evidence) {
        this.evidence = evidence;
    }

    public User getReviewer() {
        return reviewer;
    }

    public void setReviewer(User reviewer) {
        this.reviewer = reviewer;
    }

    public AuditReviewDecision getDecision() {
        return decision;
    }

    public void setDecision(AuditReviewDecision decision) {
        this.decision = decision;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
