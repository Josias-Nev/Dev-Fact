<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="fragments/head.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="active" value="clients"/>
<div class="app-shell">
    <%@ include file="fragments/sidebar.jspf" %>
    <main class="content">
        <div class="topbar">
            <div>
                <h1>Clients</h1>
                <div class="subtitle">${fn:length(clients)} client(s) affiché(s)</div>
            </div>
            <div class="topbar-actions">
                <a class="btn" href="${ctx}/clients/new">+ Nouveau client</a>
            </div>
        </div>

        <div class="page">
            <%@ include file="fragments/flash.jspf" %>

            <div class="card">
                <form class="toolbar" method="get" action="${ctx}/clients">
                    <input type="search" name="q" value="<c:out value='${q}'/>"
                           placeholder="Nom, e-mail, ville, n° fiscal…" aria-label="Rechercher un client">
                    <label class="small muted" style="display:flex;align-items:center;gap:.4rem;">
                        <input type="checkbox" name="inactive" value="true" ${showInactive ? 'checked' : ''}
                               style="width:auto;">
                        Afficher les clients désactivés
                    </label>
                    <button class="btn btn-secondary" type="submit">Rechercher</button>
                    <c:if test="${not empty q}">
                        <a class="btn btn-ghost" href="${ctx}/clients">Réinitialiser</a>
                    </c:if>
                    <span class="grow"></span>
                </form>

                <c:choose>
                    <c:when test="${empty clients}">
                        <div class="empty">
                            <h3>Aucun client</h3>
                            <p>Ajoutez vos clients pour pouvoir établir des devis et des factures.</p>
                            <a class="btn" href="${ctx}/clients/new">+ Nouveau client</a>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="table-wrap">
                            <table class="table">
                                <thead>
                                <tr>
                                    <th>Raison sociale</th>
                                    <th>Contact</th>
                                    <th>Coordonnées</th>
                                    <th>N° fiscal</th>
                                    <th class="num">Documents</th>
                                    <th>État</th>
                                    <th class="actions">Actions</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="client" items="${clients}">
                                    <tr>
                                        <td>
                                            <a class="strong" href="${ctx}/clients/edit?id=${client.id}">
                                                <c:out value="${client.name}"/>
                                            </a>
                                            <c:if test="${not empty client.city}">
                                                <div class="muted small"><c:out value="${client.city}"/></div>
                                            </c:if>
                                        </td>
                                        <td class="muted"><c:out value="${client.contactName}"/></td>
                                        <td class="muted small">
                                            <c:if test="${not empty client.phone}"><div><c:out value="${client.phone}"/></div></c:if>
                                            <c:if test="${not empty client.email}"><div><c:out value="${client.email}"/></div></c:if>
                                        </td>
                                        <td class="muted mono small"><c:out value="${client.taxId}"/></td>
                                        <td class="num"><c:out value="${client.documentCount}"/></td>
                                        <td>
                                            <span class="badge ${client.active ? 'badge-success' : 'badge-neutral'}">
                                                ${client.active ? 'Actif' : 'Désactivé'}
                                            </span>
                                        </td>
                                        <td class="actions">
                                            <a href="${ctx}/clients/edit?id=${client.id}">Modifier</a>
                                            <form class="inline-form" method="post" action="${ctx}/clients/action">
                                                <input type="hidden" name="_csrf" value="${csrfToken}">
                                                <input type="hidden" name="id" value="${client.id}">
                                                <input type="hidden" name="action" value="${client.active ? 'deactivate' : 'activate'}">
                                                <button class="btn btn-ghost btn-sm" type="submit">
                                                    ${client.active ? 'Désactiver' : 'Réactiver'}
                                                </button>
                                            </form>
                                            <c:if test="${client.documentCount == 0}">
                                                <form class="inline-form" method="post" action="${ctx}/clients/action"
                                                      onsubmit="return confirm('Supprimer ce client ?');">
                                                    <input type="hidden" name="_csrf" value="${csrfToken}">
                                                    <input type="hidden" name="id" value="${client.id}">
                                                    <input type="hidden" name="action" value="delete">
                                                    <button class="btn btn-ghost btn-sm" type="submit">Supprimer</button>
                                                </form>
                                            </c:if>
                                        </td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </main>
</div>
<%@ include file="fragments/foot.jspf" %>
