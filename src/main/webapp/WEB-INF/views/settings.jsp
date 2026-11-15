<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="fragments/head.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="active" value="settings"/>
<div class="app-shell">
    <%@ include file="fragments/sidebar.jspf" %>
    <main class="content">
        <div class="topbar">
            <div>
                <h1>Paramètres</h1>
                <div class="subtitle">Coordonnées de la société, devise et règles de numérotation.</div>
            </div>
        </div>

        <div class="page">
            <%@ include file="fragments/flash.jspf" %>

            <form method="post" action="${ctx}/settings">
                <input type="hidden" name="_csrf" value="${csrfToken}">

                <div class="card">
                    <div class="card-head"><h2>Société émettrice</h2></div>
                    <div class="card-body">
                        <div class="form-grid">
                            <div class="field">
                                <label for="companyName">Raison sociale *</label>
                                <input type="text" id="companyName" name="companyName" maxlength="150" required
                                       value="<c:out value='${settings.companyName}'/>">
                            </div>
                            <div class="field">
                                <label for="taxId">Numéro fiscal (IFU / NIF / TVA)</label>
                                <input type="text" id="taxId" name="taxId" maxlength="40"
                                       value="<c:out value='${settings.taxId}'/>">
                            </div>
                            <div class="field">
                                <label for="address">Adresse</label>
                                <input type="text" id="address" name="address" maxlength="255"
                                       value="<c:out value='${settings.address}'/>">
                            </div>
                            <div class="field">
                                <label for="city">Ville</label>
                                <input type="text" id="city" name="city" maxlength="80"
                                       value="<c:out value='${settings.city}'/>">
                            </div>
                            <div class="field">
                                <label for="phone">Téléphone</label>
                                <input type="text" id="phone" name="phone" maxlength="40"
                                       value="<c:out value='${settings.phone}'/>">
                            </div>
                            <div class="field">
                                <label for="email">E-mail</label>
                                <input type="email" id="email" name="email" maxlength="120"
                                       value="<c:out value='${settings.email}'/>">
                            </div>
                            <div class="field span-2">
                                <label for="bankDetails">Coordonnées bancaires</label>
                                <textarea id="bankDetails" name="bankDetails" rows="2"><c:out value="${settings.bankDetails}"/></textarea>
                            </div>
                            <div class="field span-2">
                                <label for="footerNote">Mention de pied de document</label>
                                <input type="text" id="footerNote" name="footerNote" maxlength="255"
                                       value="<c:out value='${settings.footerNote}'/>">
                            </div>
                        </div>
                    </div>
                </div>

                <div class="card">
                    <div class="card-head"><h2>Facturation</h2></div>
                    <div class="card-body">
                        <div class="form-grid">
                            <div class="field">
                                <label for="currency">Devise (code ISO)</label>
                                <input type="text" id="currency" name="currency" maxlength="3" required
                                       value="<c:out value='${settings.currency}'/>">
                                <div class="hint">XOF (FCFA, sans décimale), EUR, USD…</div>
                            </div>
                            <div class="field">
                                <label for="defaultTaxRate">Taux de TVA par défaut (%)</label>
                                <input type="text" id="defaultTaxRate" name="defaultTaxRate" inputmode="decimal"
                                       value="${settings.defaultTaxRate}">
                            </div>
                            <div class="field">
                                <label for="quotePrefix">Préfixe des devis</label>
                                <input type="text" id="quotePrefix" name="quotePrefix" maxlength="10" required
                                       value="<c:out value='${settings.quotePrefix}'/>">
                                <div class="hint">Numérotation : DEV-2026-0001</div>
                            </div>
                            <div class="field">
                                <label for="invoicePrefix">Préfixe des factures</label>
                                <input type="text" id="invoicePrefix" name="invoicePrefix" maxlength="10" required
                                       value="<c:out value='${settings.invoicePrefix}'/>">
                                <div class="hint">Numérotation : FAC-2026-0001</div>
                            </div>
                            <div class="field">
                                <label for="paymentTermsDays">Délai de paiement (jours)</label>
                                <input type="number" id="paymentTermsDays" name="paymentTermsDays" min="0" max="365"
                                       value="${settings.paymentTermsDays}">
                            </div>
                            <div class="field">
                                <label for="quoteValidityDays">Validité des devis (jours)</label>
                                <input type="number" id="quoteValidityDays" name="quoteValidityDays" min="0" max="365"
                                       value="${settings.quoteValidityDays}">
                            </div>
                        </div>
                    </div>
                </div>

                <div class="form-actions">
                    <button class="btn" type="submit">Enregistrer les paramètres</button>
                </div>
            </form>
        </div>
    </main>
</div>
<%@ include file="fragments/foot.jspf" %>
