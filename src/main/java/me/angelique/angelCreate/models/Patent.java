package me.angelique.angelCreate.models;

import java.util.UUID;

public class Patent {

    private UUID id;
    private UUID productId;
    private UUID companyId;
    private long issuedAt;
    private long expiresAt;

    public Patent(UUID id, UUID productId, UUID companyId, long issuedAt, long expiresAt) {
        this.id = id;
        this.productId = productId;
        this.companyId = companyId;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() { return System.currentTimeMillis() > expiresAt; }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public UUID getCompanyId() { return companyId; }
    public long getIssuedAt() { return issuedAt; }
    public long getExpiresAt() { return expiresAt; }
    public void setId(UUID id) { this.id = id; }
}
