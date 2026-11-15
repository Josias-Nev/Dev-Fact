<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="fragments/head.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="active" value="${doc.type.code eq 'FACTURE' ? 'factures' : 'devis'}"/>
<div class="app-shell">
    <%@ include file="fragments/sidebar.jspf" %>
    <main class="content">
        <div class="topbar">
            <div>
                <h1><c:out value="${doc.type.label}"/> <span class="mono"><c:out value="${doc.number}"/></span></h1>
                <div class="subtitle">
                    <c:out value="${doc.clientName}"/> —
                    <span class="badge badge-${doc.status.style}"><c:out value="${doc.status.label}"/></span>
                    <c:if test="${invoiced}">
                        <span class="badge badge-success">Déjà facturé</span>
                    </c:if>
                </div>
            </div>
            <div class="topbar-actions no-print">
                <a class="btn btn-secondary" href="${ctx}/documents?type=${doc.type.code}">Retour</a>
                <a class="btn btn-secondary" href="${ctx}/documents/pdf?id=${doc.id}" target="_blank" rel="noopener">Voir le PDF</a>
                <a class="btn btn-secondary" href="${ctx}/documents/pdf?id=${doc.id}&download=1">Télécharger</a>
                <button class="btn btn-secondary" type="button" onclick="window.print()">Imprimer</button>
                <c:if test="${doc.editable}">
                    <a class="btn" href="${ctx}/documents/edit?id=${doc.id}">Modifier</a>
                </c:if>
            </div>
        </div>

        <div class="page">
            <%@ include file="fragments/flash.jspf" %>

            <div class="card">
                <div class="card-body">
                    <div class="doc-header">
                        <div class="doc-identity">
                            <div class="muted small">Document</div>
                            <h2><c:out value="${doc.type.label}"/> <span class="doc-number"><c:out value="${doc.number}"/></span></h2>
                            <div class="small muted" style="margin-top:.35rem;">
                                Émis le <c:out value="${doc.issueDateText}"/>
                                <c:if test="${not empty doc.dueDate}">
                                    · <c:out value="${doc.dueDateLabel}"/> <c:out value="${doc.dueDateText}"/>
                                </c:if>
                                <c:if test="${not empty doc.sourceDocumentNumber}">
                                    · Réf. devis <span class="mono"><c:out value="${doc.sourceDocumentNumber}"/></span>
                                </c:if>
                            </div>
                        </div>
                        <div class="right">
                            <div class="muted small">Total TTC</div>
                            <div style="font-size:1.5rem;font-weight:700;color:var(--brand);">
                                <c:out value="${doc.totalWithSymbol}"/>
                            </div>
                        </div>
                    </div>

                    <div class="doc-parties">
                        <div class="party">
                            <div class="party-label">Client</div>
                            <strong><c:out value="${doc.clientName}"/></strong>
                            <div class="party-lines">
                                <c:if test="${not empty client.addressLine}"><div><c:out value="${client.addressLine}"/></div></c:if>
                                <c:if test="${not empty client.contactName}"><div><c:out value="${client.contactName}"/></div></c:if>
                                <c:if test="${not empty client.phone}"><div><c:out value="${client.phone}"/></div></c:if>
                                <c:if test="${not empty client.email}"><div><c:out value="${client.email}"/></div></c:if>
                                <c:if test="${not empty client.taxId}"><div>N° fiscal : <c:out value="${client.taxId}"/></div></c:if>
                            </div>
                        </div>
                        <div class="party">
                            <div class="party-label">Émetteur</div>
                            <strong><c:out value="${settings.companyName}"/></strong>
                            <div class="party-lines">
                                <c:if test="${not empty settings.addressLine}"><div><c:out value="${settings.addressLine}"/></div></c:if>
                                <c:if test="${not empty settings.phone}"><div><c:out value="${settings.phone}"/></div></c:if>
                                <c:if test="${not empty settings.email}"><div><c:out value="${settings.email}"/></div></c:if>
                                <c:if test="${not empty settings.taxId}"><div>N° fiscal : <c:out value="${settings.taxId}"/></div></c:if>
                            </div>
                        </div>
                    </div>

                    <c:if test="${not empty doc.subject}">
                        <p style="margin:1.1rem 0 .2rem;"><span class="muted small">Objet :</span>
                            <c:out value="${doc.subject}"/></p>
                    </c:if>
                </div>

                <div class="table-wrap">
                    <table class="table">
                        <thead>
                        <tr>
                            <th style="width:3rem;">#</th>
                            <th>Désignation</th>
                            <th class="num">Qté</th>
                            <th class="num">Prix unitaire</th>
                            <th class="num">Montant</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="line" items="${doc.lines}" varStatus="status">
                            <tr>
                                <td class="muted"><c:out value="${status.count}"/></td>
                                <td><c:out value="${line.description}"/></td>
                                <td class="num"><c:out value="${line.quantityText}"/></td>
                                <td class="num"><c:out value="${line.unitPriceText}"/></td>
                                <td class="num strong"><c:out value="${line.lineTotalText}"/></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>

                <div class="card-body">
                    <div class="totals">
                        <div class="row"><span class="muted">Sous-total HT</span><span><c:out value="${doc.subtotalText}"/> <c:out value="${settings.currencySymbol}"/></span></div>
                        <div class="row"><span class="muted">TVA <c:out value="${doc.taxRateText}"/> %</span><span><c:out value="${doc.taxAmountText}"/> <c:out value="${settings.currencySymbol}"/></span></div>
                        <div class="row total"><span>TOTAL TTC</span><span><c:out value="${doc.totalText}"/> <c:out value="${settings.currencySymbol}"/></span></div>
                    </div>
                    <div class="amount-words"><c:out value="${doc.amountInWords}"/></div>

                    <c:if test="${not empty doc.notes}">
                        <hr class="divider">
                        <div class="small"><span class="muted">Notes :</span> <c:out value="${doc.notes}"/></div>
                    </c:if>
                </div>
            </div>

            <div class="card no-print">
                <div class="card-head"><h2>Actions</h2></div>
                <div class="card-body">
                    <div class="pill-list">
                        <c:forEach var="next" items="${nextStatuses}">
                            <form class="inline-form" method="post" action="${ctx}/documents/action">
                                <input type="hidden" name="_csrf" value="${csrfToken}">
                                <input type="hidden" name="id" value="${doc.id}">
                                <input type="hidden" name="action" value="status">
                                <input type="hidden" name="status" value="${next.code}">
                                <button class="btn btn-secondary btn-sm" type="submit">Marquer « <c:out value="${next.label}"/> »</button>
                            </form>
                        </c:forEach>

                        <c:if test="${canConvert}">
                            <form class="inline-form" method="post" action="${ctx}/documents/action">
                                <input type="hidden" name="_csrf" value="${csrfToken}">
                                <input type="hidden" name="id" value="${doc.id}">
                                <input type="hidden" name="action" value="convert">
                                <button class="btn btn-sm" type="submit">Convertir en facture</button>
                            </form>
                        </c:if>

                        <c:if test="${doc.editable}">
                            <form class="inline-form" method="post" action="${ctx}/documents/action"
                                  onsubmit="return confirm('Supprimer définitivement ce brouillon ?');">
                                <input type="hidden" name="_csrf" value="${csrfToken}">
                                <input type="hidden" name="id" value="${doc.id}">
                                <input type="hidden" name="action" value="delete">
                                <input type="hidden" name="type" value="${doc.type.code}">
                                <button class="btn btn-danger btn-sm" type="submit">Supprimer le brouillon</button>
                            </form>
                        </c:if>
                    </div>
                    <c:if test="${empty nextStatuses and not canConvert and not doc.editable}">
                        <p class="muted small" style="margin:0;">Ce document est clôturé : aucune action supplémentaire n'est possible.</p>
                    </c:if>
                </div>
            </div>
        </div>
    </main>
</div>
<%@ include file="fragments/foot.jspf" %>
