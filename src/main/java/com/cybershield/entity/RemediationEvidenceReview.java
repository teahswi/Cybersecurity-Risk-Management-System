package com.cybershield.entity;

import com.cybershield.enums.ReviewDecision;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.Immutable;

/**
 * One review decision on one evidence file. Every decision is kept.
 * Table: remediation_evidence_reviews
 */
@Entity
@Immutable
@Table(name = "remediation_evidence_reviews")
public class RemediationEvidenceReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evidence_id", nullable = false)
    private RemediationEvidence evidence;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private ReviewDecision decision;

    @Column(name = "comments", nullable = true, columnDefinition = "TEXT")
    private String comments;

    @Column(name = "reviewed_at", nullable = false)
    private LocalDateTime reviewedAt;

    public RemediationEvidenceReview() {
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

    public RemediationEvidence getEvidence() {
        return evidence;
    }

    public void setEvidence(RemediationEvidence evidence) {
        this.evidence = evidence;
    }

    public User getReviewer() {
        return reviewer;
    }

    public void setReviewer(User reviewer) {
        this.reviewer = reviewer;
    }

    public ReviewDecision getDecision() {
        return decision;
    }

    public void setDecision(ReviewDecision decision) {
        this.decision = decision;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
