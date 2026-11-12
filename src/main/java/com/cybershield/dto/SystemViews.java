package com.cybershield.dto;

public final class SystemViews {

    private SystemViews() {
    }

    /** System overview for the Security Manager. Never contains credentials or connection strings. */
    public record Health(boolean databaseUp, String databaseVersion, String schemaVersion, long registeredUsers,
                         long activeUsers, long totalAssets, long openIncidents, long pendingTasks, long auditLogEntries,
                         boolean uploadDirectoryWritable, String javaVersion, String uptime) {
    }
}
