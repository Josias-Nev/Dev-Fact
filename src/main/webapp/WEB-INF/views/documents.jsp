<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="fragments/head.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="active" value="${query.type.code eq 'FACTURE' ? 'factures' : 'devis'}"/>
<div class="app-shell">
    <%@ include file="fragments/sidebar.jspf" %>
    <main class="content">
        <div class="topbar">
            <div>
                <h1><c:out value="${query.type.pluralLabel}"/></h1>
                <div class="subtitle"><c:out value="${total}"/> document(s) enregistré(s)</div>
            </div>
            <div class="topbar-actions">
                <div class="tabs">
                    <a class="${query.type.code eq 'DEVIS' ? 'is-active' : ''}" href="${ctx}/documents?type=DEVIS">Devis</a>
                    <a class="${query.type.code eq 'FACTURE' ? 'is-active' : ''}" href="${ctx}/documents?type=FACTURE">Factures</a>
                </div>
                <a class="btn" href="${ctx}/documents/new?type=${query.type.code}">
                    + Nouveau <c:out value="${query.type.label}"/>
                </a>
            </div>
        </div>

        <div class="page">
            <%@ include file="fragments/flash.jspf" %>

            <div class="card">
                <form class="toolbar" method="get" action="${ctx}/documents">
                    <input type="hidden" name="type" value="${query.type.code}">
                    <input type="search" name="q" value="<c:out value='${query.text}'/>"
                           placeholder="Numéro, client ou objet…" aria-label="Rechercher">
                    <select name="status" aria-label="Statut">
                        <option value="">Tous les statuts</option>
                        <c:forEach var="st" items="${statuses}">
                            <option value="${st.code}" ${query.status eq st ? 'selected' : ''}>
                                <c:out value="${st.label}"/>
                            </option>
                        </c:forEach>
                    </select>
                    <select name="clientId" aria-label="Client">
                        <option value="">Tous les clients</option>
                        <c:forEach var="client" items="${clients}">
                            <option value="${client.id}" ${query.clientId eq client.id ? 'selected' : ''}>
                                <c:out value="${client.name}"/>
                            </option>
                        </c:forEach>
                    </select>
                    <button class="btn btn-secondary" type="submit">Filtrer</button>
                    <c:if test="${not empty query.status or not empty query.clientId or not empty query.text}">
                        <a class="btn btn-ghost" href="${ctx}/documents?type=${query.type.code}">Réinitialiser</a>
                    </c:if>
                    <span class="grow"></span>
                </form>

                <c:choose>
                    <c:when test="${empty documents}">
                        <div class="empty">
                            <h3>Aucun <c:out value="${query.type.label}"/> pour le moment</h3>
                            <p>Créez votre premier <c:out value="${query.type.label}"/>
                               ou modifiez les filtres de recherche.</p>
                            <a class="btn" href="${ctx}/documents/new?type=${query.type.code}">
                                + Nouveau <c:out value="${query.type.label}"/>
                            </a>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="table-wrap">
                            <table class="table">
                                <thead>
                                <tr>
                                    <th>Numéro</th>
                                    <th>Client</th>
                                    <th>Objet</th>
                                    <th>Date</th>
                                    <th><c:out value="${query.type.code eq 'DEVIS' ? 'Validité' : 'Échéance'}"/></th>
                                    <th class="num">Total TTC</th>
                                    <th>Statut</th>
                                    <th class="actions">Actions</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="doc" items="${documents}">
                                    <tr>
                                        <td>
                                            <a class="strong mono" href="${ctx}/documents/view?id=${doc.id}">
                                                <c:out value="${doc.number}"/>
                                            </a>
                                        </td>
                                        <td><c:out value="${doc.clientName}"/></td>
                                        <td class="muted"><c:out value="${doc.subject}"/></td>
                                        <td class="muted"><c:out value="${doc.issueDateText}"/></td>
                                        <td class="muted"><c:out value="${doc.dueDateText}"/></td>
                                        <td class="num strong"><c:out value="${doc.totalWithSymbol}"/></td>
                                        <td><span class="badge badge-${doc.status.style}"><c:out value="${doc.status.label}"/></span></td>
                                        <td class="actions">
                                            <a href="${ctx}/documents/view?id=${doc.id}">Voir</a>
                                            <a href="${ctx}/documents/pdf?id=${doc.id}" target="_blank" rel="noopener">PDF</a>
                                        </td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>

                        <div class="pagination">
                            <span class="page-info">
                                Page <c:out value="${query.page}"/> sur <c:out value="${pageCount}"/>
                                — <c:out value="${total}"/> résultat(s)
                            </span>
                            <c:if test="${query.page > 1}">
                                <c:url var="prevUrl" value="/documents">
                                    <c:param name="type" value="${query.type.code}"/>
                                    <c:param name="page" value="${query.page - 1}"/>
                                    <c:if test="${not empty query.status}"><c:param name="status" value="${query.status.code}"/></c:if>
                                    <c:if test="${not empty query.clientId}"><c:param name="clientId" value="${query.clientId}"/></c:if>
                                    <c:if test="${not empty query.text}"><c:param name="q" value="${query.text}"/></c:if>
                                </c:url>
                                <a class="btn btn-secondary btn-sm" href="${prevUrl}">Précédent</a>
                            </c:if>
                            <c:if test="${query.page < pageCount}">
                                <c:url var="nextUrl" value="/documents">
                                    <c:param name="type" value="${query.type.code}"/>
                                    <c:param name="page" value="${query.page + 1}"/>
                                    <c:if test="${not empty query.status}"><c:param name="status" value="${query.status.code}"/></c:if>
                                    <c:if test="${not empty query.clientId}"><c:param name="clientId" value="${query.clientId}"/></c:if>
                                    <c:if test="${not empty query.text}"><c:param name="q" value="${query.text}"/></c:if>
                                </c:url>
                                <a class="btn btn-secondary btn-sm" href="${nextUrl}">Suivant</a>
                            </c:if>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </main>
</div>
<%@ include file="fragments/foot.jspf" %>
