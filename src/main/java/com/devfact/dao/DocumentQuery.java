package com.devfact.dao;

import com.devfact.model.DocumentStatus;
import com.devfact.model.DocumentType;
import com.devfact.util.Texts;

/** Critères de recherche de la liste des documents. */
public class DocumentQuery {

    private DocumentType type = DocumentType.DEVIS;
    private DocumentStatus status;
    private Long clientId;
    private String text;
    private int page = 1;
    private int pageSize = 20;

    public DocumentType getType() {
        return type;
    }

    public void setType(DocumentType type) {
        this.type = type == null ? DocumentType.DEVIS : type;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = Texts.trimToNull(text);
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page < 1 ? 1 : page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize < 1 ? 20 : (pageSize > 200 ? 200 : pageSize);
    }

    public int getOffset() {
        return (page - 1) * pageSize;
    }

    public boolean hasFilters() {
        return status != null || clientId != null || text != null;
    }
}
