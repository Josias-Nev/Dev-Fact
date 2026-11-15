<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="fragments/head.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="active" value="clients"/>
<div class="app-shell">
    <%@ include file="fragments/sidebar.jspf" %>
    <main class="content">
        <div class="topbar">
            <div>
                <h1>${isNew ? 'Nouveau client' : 'Modifier le client'}</h1>
                <div class="subtitle">Les informations reprises ici apparaissent sur les devis et les factures.</div>
            </div>
            <div class="topbar-actions">
                <a class="btn btn-secondary" href="${ctx}/clients">Retour à la liste</a>
            </div>
        </div>

        <div class="page">
            <%@ include file="fragments/flash.jspf" %>

            <form method="post" action="${ctx}/clients/save">
                <input type="hidden" name="_csrf" value="${csrfToken}">
                <input type="hidden" name="id" value="${client.id}">

                <div class="card">
                    <div class="card-head"><h2>Identité</h2></div>
                    <div class="card-body">
                        <div class="form-grid">
                            <div class="field">
                                <label for="name">Raison sociale *</label>
                                <input type="text" id="name" name="name" maxlength="150" required
                                       value="<c:out value='${client.name}'/>" placeholder="Société Exemple SARL">
                            </div>
                            <div class="field">
                                <label for="contactName">Personne de contact</label>
                                <input type="text" id="contactName" name="contactName" maxlength="120"
                                       value="<c:out value='${client.contactName}'/>">
                            </div>
                            <div class="field">
                                <label for="email">E-mail</label>
                                <input type="email" id="email" name="email" maxlength="120"
                                       value="<c:out value='${client.email}'/>">
                            </div>
                            <div class="field">
                                <label for="phone">Téléphone</label>
                                <input type="text" id="phone" name="phone" maxlength="40"
                                       value="<c:out value='${client.phone}'/>">
                            </div>
                            <div class="field">
                                <label for="taxId">Numéro fiscal (IFU / NIF / TVA)</label>
                                <input type="text" id="taxId" name="taxId" maxlength="40"
                                       value="<c:out value='${client.taxId}'/>">
                            </div>
                            <div class="field">
                                <label for="city">Ville</label>
                                <input type="text" id="city" name="city" maxlength="80"
                                       value="<c:out value='${client.city}'/>">
                            </div>
                            <div class="field span-2">
                                <label for="address">Adresse</label>
                                <input type="text" id="address" name="address" maxlength="255"
                                       value="<c:out value='${client.address}'/>">
                            </div>
                            <div class="field span-2">
                                <label for="notes">Notes internes</label>
                                <textarea id="notes" name="notes" rows="3"><c:out value="${client.notes}"/></textarea>
                            </div>
                            <div class="field span-2">
                                <label style="display:flex;align-items:center;gap:.5rem;text-transform:none;letter-spacing:0;">
                                    <input type="hidden" name="active" value="false">
                                    <input type="checkbox" name="active" value="true" ${client.active ? 'checked' : ''}
                                           style="width:auto;">
                                    Client actif (proposé lors de la création d'un document)
                                </label>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="form-actions">
                    <button class="btn" type="submit">Enregistrer</button>
                    <a class="btn btn-secondary" href="${ctx}/clients">Annuler</a>
                </div>
            </form>
        </div>
    </main>
</div>
<%@ include file="fragments/foot.jspf" %>
