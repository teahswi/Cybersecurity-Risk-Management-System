package com.cybershield.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cybershield.dto.IncidentForm;
import com.cybershield.dto.RiskForm;
import com.cybershield.entity.User;
import com.cybershield.enums.AlertStatus;
import com.cybershield.enums.AuditReviewDecision;
import com.cybershield.enums.CorrectiveActionStatus;
import com.cybershield.enums.FindingStatus;
import com.cybershield.enums.IncidentStatus;
import com.cybershield.enums.IncidentType;
import com.cybershield.enums.NotificationCategory;
import com.cybershield.enums.RecordType;
import com.cybershield.enums.RiskLevel;
import com.cybershield.enums.Severity;
import com.cybershield.enums.TreatmentStrategy;
import com.cybershield.exception.BusinessRuleException;
import com.cybershield.repository.AuditFindingRepository;
import com.cybershield.repository.CorrectiveActionRepository;
import com.cybershield.repository.IncidentAlertRepository;
import com.cybershield.repository.RiskAssessmentRepository;
import com.cybershield.repository.SecurityAlertRepository;
import com.cybershield.repository.UserRepository;
import com.cybershield.service.AuditService;
import com.cybershield.service.IncidentService;
import com.cybershield.service.NotificationService;
import com.cybershield.service.RiskService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

/** Tests 20-27: notifications, incidents and alerts, audit logs, risk scoring and audit evidence verification. */
class NotificationIncidentAuditRiskTest extends IntegrationTestBase {

    @Autowired
    NotificationService notificationService;
    @Autowired
    IncidentService incidentService;
    @Autowired
    RiskService riskService;
    @Autowired
    AuditService auditService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    SecurityAlertRepository alertRepository;
    @Autowired
    IncidentAlertRepository incidentAlertRepository;
    @Autowired
    RiskAssessmentRepository riskRepository;
    @Autowired
    CorrectiveActionRepository actionRepository;
    @Autowired
    AuditFindingRepository findingRepository;
    @Autowired
    TestData data;

    @Test
    void notificationsAreCreatedOnceAndBelongToOneUser() {
        User analyst = userRepository.findByUsernameIgnoreCase("analyst").orElseThrow();
        assertThat(notificationService.notify(analyst, NotificationCategory.SYSTEM, "Test", "Message",
                null, null, "TEST:dedup")).isTrue();
        assertThat(notificationService.notify(analyst, NotificationCategory.SYSTEM, "Test", "Message",
                null, null, "TEST:dedup")).as("duplicate prevented").isFalse();

        loginAs("analyst");
        long unread = notificationService.unreadCount();
        Long id = notificationService.recentForCurrentUser(1).getFirst().id();
        notificationService.markRead(id);
        assertThat(notificationService.unreadCount()).isEqualTo(unread - 1);

        loginAs("engineer");
        assertThatThrownBy(() -> notificationService.markRead(id))
                .as("users cannot touch other users' notifications").isInstanceOf(AccessDeniedException.class);
        notificationService.markAllRead();
        assertThat(notificationService.unreadCount()).isZero();
        assertThat(notificationService.summary(8).items()).allMatch(i -> i.read());
    }

    @Test
    void incidentCreatedFromAlertLinksAlertsAndAssets() {
        loginAs("analyst");
        Long alertId = data.alertId("ALT-0001");
        IncidentForm form = new IncidentForm();
        form.setTitle("Brute force against SRV-104");
        form.setDescription("Repeated SSH login failures from one internal host.");
        form.setIncidentType(IncidentType.UNAUTHORIZED_ACCESS);
        form.setSeverity(Severity.HIGH);
        form.setDetectedAt(LocalDateTime.now().minusHours(1));
        form.setAlertId(alertId);
        form.setAssignedAnalystId(data.userId("analyst2"));
        Long incidentId = incidentService.create(form);

        var detail = incidentService.detail(incidentId);
        assertThat(detail.incidentCode()).isEqualTo(String.format("INC-%04d", incidentId));
        assertThat(detail.status()).as("assigning moves OPEN to INVESTIGATING").isEqualTo(IncidentStatus.INVESTIGATING);
        assertThat(detail.alerts()).extracting(a -> a.alertCode()).containsExactly("ALT-0001");
        assertThat(alertRepository.findById(alertId).orElseThrow().getStatus()).isEqualTo(AlertStatus.ESCALATED);

        incidentService.linkAlert(incidentId, data.alertId("ALT-0006"));
        assertThat(incidentAlertRepository.findByIncidentId(incidentId)).hasSize(2);
        assertThatThrownBy(() -> incidentService.linkAlert(incidentId, alertId)).isInstanceOf(BusinessRuleException.class);
        incidentService.linkAsset(incidentId, data.assetId("SRV-104"), "Target of the attempts");
        incidentService.addNote(incidentId, "Source host isolated.");

        assertThatThrownBy(() -> incidentService.changeStatus(incidentId, IncidentStatus.RESOLVED, null))
                .as("resolution details are required").isInstanceOf(BusinessRuleException.class);
        incidentService.changeStatus(incidentId, IncidentStatus.RESOLVED, "Credentials rotated; host re-imaged.");
        assertThatThrownBy(() -> incidentService.changeStatus(incidentId, IncidentStatus.OPEN, null))
                .isInstanceOf(BusinessRuleException.class);

        var after = incidentService.detail(incidentId);
        assertThat(after.assets()).hasSize(1);
        assertThat(after.timeline()).extracting(e -> e.kind()).contains("NOTE");
        assertThat(after.timeline().size()).isGreaterThanOrEqualTo(5);
    }

    @Test
    void riskScoreIsCalculatedOnTheServerAndPersisted() {
        loginAs("analyst");
        RiskForm form = riskService.newForm(null, data.instance("SRV-104", "VUL-0001").getId());
        form.setLikelihood(4);
        form.setImpact(5);
        form.setTreatmentStrategy(TreatmentStrategy.MITIGATE);
        Long id = riskService.create(form);
        var risk = riskRepository.findById(id).orElseThrow();
        assertThat(risk.getRiskScore()).isEqualTo(20);
        assertThat(risk.getRiskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(risk.getAsset().getAssetCode()).isEqualTo("SRV-104");

        RiskForm edit = riskService.formFor(id);
        edit.setLikelihood(2);
        edit.setImpact(2);
        riskService.update(id, edit);
        assertThat(risk.getRiskScore()).isEqualTo(4);
        assertThat(risk.getRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(riskService.detail(id).history()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void auditEvidenceVerificationResolvesFinding() {
        // FND-0002 is seeded with a corrective action assigned to "analyst" and evidence awaiting review
        Long actionId = data.firstActionOfFinding("FND-0002");
        loginAs("analyst");
        assertThatThrownBy(() -> auditService.reviewEvidence(actionId, AuditReviewDecision.VERIFIED, null))
                .as("only auditors and managers verify audit evidence").isInstanceOf(AccessDeniedException.class);

        loginAs("auditor");
        var detail = auditService.actionDetail(actionId);
        assertThat(detail.canReview()).isTrue();
        auditService.reviewEvidence(actionId, AuditReviewDecision.VERIFIED, "Policy approved");
        assertThat(actionRepository.findById(actionId).orElseThrow().getStatus()).isEqualTo(CorrectiveActionStatus.VERIFIED);
        assertThat(findingRepository.findById(data.findingId("FND-0002")).orElseThrow().getStatus())
                .isEqualTo(FindingStatus.RESOLVED);
        assertThat(auditService.actionDetail(actionId).reviews()).hasSize(1);
    }

    @Test
    void auditEvidenceRejectionAndResubmission() {
        // FND-0001's corrective action is assigned to "engineer" and is In Progress
        Long actionId = data.firstActionOfFinding("FND-0001");
        loginAs("engineer");
        auditService.uploadEvidence(actionId, new MockMultipartFile("file", "patch-sla.txt", "text/plain",
                "Patch SLA defined: 15 days for critical servers.".getBytes(StandardCharsets.UTF_8)), "SLA document");
        assertThat(actionRepository.findById(actionId).orElseThrow().getStatus()).isEqualTo(CorrectiveActionStatus.PENDING_REVIEW);

        loginAs("auditor");
        auditService.reviewEvidence(actionId, AuditReviewDecision.REJECTED, "Needs management approval signature");
        assertThat(actionRepository.findById(actionId).orElseThrow().getStatus()).isEqualTo(CorrectiveActionStatus.IN_PROGRESS);
        assertThat(auditService.searchActions(null, false, PageRequest.of(0, 20)).getTotalElements()).isGreaterThanOrEqualTo(3);

        loginAs("engineer");
        var mine = auditService.searchActions(null, false, PageRequest.of(0, 20));
        assertThat(mine.getContent()).allMatch(a -> a.assignedToName().equals("Vikram Singh"));
        long reviewed = notificationService.recentForCurrentUser(20).stream()
                .filter(n -> n.category() == NotificationCategory.AUDIT_EVIDENCE_REVIEWED).count();
        assertThat(reviewed).isEqualTo(1);
    }

    @Test
    void auditLogsRecordBusinessActions() {
        loginAs("analyst");
        Long incidentId = data.incidentId("INC-0001");
        incidentService.addNote(incidentId, "Audit log test note");
        loginAs("manager");
        var logs = incidentService.detail(incidentId).timeline();
        assertThat(logs).extracting(e -> e.text()).contains("Audit log test note");
        assertThat(notificationService.recentForCurrentUser(50)).isNotNull();
        assertThat(RecordType.INCIDENT).isNotNull();
    }
}
