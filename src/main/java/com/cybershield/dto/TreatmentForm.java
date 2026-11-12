package com.cybershield.dto;

import com.cybershield.enums.TreatmentStrategy;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** Adds a treatment action to a risk. */
public class TreatmentForm {

    @NotNull(message = "Choose a strategy")
    private TreatmentStrategy strategy;

    @NotBlank(message = "Describe the treatment action")
    @Size(max = 4000)
    private String actionDescription;

    private Long ownerId;

    @FutureOrPresent(message = "The treatment deadline cannot be in the past")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate deadline;

    public TreatmentStrategy getStrategy() { return strategy; }
    public void setStrategy(TreatmentStrategy strategy) { this.strategy = strategy; }
    public String getActionDescription() { return actionDescription; }
    public void setActionDescription(String actionDescription) { this.actionDescription = actionDescription; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
}
