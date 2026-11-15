<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="fragments/head.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="active" value="${doc.type.code eq 'FACTURE' ? 'factures' : 'devis'}"/>
<div class="app-shell">
    <%@ include file="fragments/sidebar.jspf" %>
    <main class="content">
        <div class="topbar">
            <div>
                <h1><c:out value="${isNew ? 'Nouveau' : 'Modifier le'}"/> <c:out value="${fn:toLowerCase(doc.type.label)}"/></h1>
                <div class="subtitle">
                    <c:choose>
                        <c:when test="${isNew}">Le numéro définitif sera attribué à l'enregistrement.</c:when>
                        <c:otherwise>Document <span class="mono"><c:out value="${doc.number}"/></span> — statut
                            <span class="badge badge-${doc.status.style}"><c:out value="${doc.status.label}"/></span>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
            <div class="topbar-actions">
                <a class="btn btn-secondary" href="${ctx}/documents?type=${doc.type.code}">Retour à la liste</a>
            </div>
        </div>

        <div class="page">
            <%@ include file="fragments/flash.jspf" %>

            <form method="post" action="${ctx}/documents/save" id="documentForm" novalidate>
                <input type="hidden" name="_csrf" value="${csrfToken}">
                <input type="hidden" name="id" value="${doc.id}">
                <input type="hidden" name="type" value="${doc.type.code}">

                <div class="card">
                    <div class="card-head"><h2>Informations générales</h2></div>
                    <div class="card-body">
                        <div class="form-grid">
                            <div class="field">
                                <label for="clientId">Client *</label>
                                <select id="clientId" name="clientId" required>
                                    <option value="">— Sélectionner un client —</option>
                                    <c:forEach var="client" items="${clients}">
                                        <option value="${client.id}" ${doc.clientId eq client.id ? 'selected' : ''}>
                                            <c:out value="${client.name}"/>
                                        </option>
                                    </c:forEach>
                                </select>
                                <div class="hint">
                                    <a href="${ctx}/clients/new">Créer un nouveau client</a>
                                </div>
                            </div>
                            <div class="field">
                                <label for="subject">Objet</label>
                                <input type="text" id="subject" name="subject" maxlength="255"
                                       value="<c:out value='${doc.subject}'/>"
                                       placeholder="Prestation, fourniture, mission…">
                            </div>
                            <div class="field">
                                <label for="issueDate">Date d'émission *</label>
                                <input type="date" id="issueDate" name="issueDate"
                                       value="${doc.issueDate}" required>
                            </div>
                            <div class="field">
                                <label for="dueDate">
                                    <c:choose>
                                        <c:when test="${doc.type.code eq 'DEVIS'}">Valable jusqu'au</c:when>
                                        <c:otherwise>Date d'échéance</c:otherwise>
                                    </c:choose>
                                </label>
                                <input type="date" id="dueDate" name="dueDate" value="${doc.dueDate}">
                            </div>
                            <div class="field">
                                <label for="taxRate">Taux de TVA (%)</label>
                                <input type="text" id="taxRate" name="taxRate" inputmode="decimal"
                                       value="${doc.taxRate}">
                                <div class="hint">0 si la TVA ne s'applique pas.</div>
                            </div>
                            <div class="field">
                                <label>Devise</label>
                                <input type="text" value="${settings.currency} — ${settings.currencySymbol}" readonly>
                                <div class="hint">Modifiable dans les paramètres de la société.</div>
                            </div>
                            <div class="field span-2">
                                <label for="notes">Notes / conditions</label>
                                <textarea id="notes" name="notes" rows="3"
                                          placeholder="Conditions de règlement, mentions particulières…"><c:out value="${doc.notes}"/></textarea>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="card">
                    <div class="card-head">
                        <h2>Lignes du document</h2>
                        <button class="btn btn-secondary btn-sm" type="button" id="addLine">+ Ajouter une ligne</button>
                    </div>
                    <div class="table-wrap">
                        <table class="table table-lines" id="linesTable">
                            <thead>
                            <tr>
                                <th class="col-desc">Désignation *</th>
                                <th class="col-qty">Qté</th>
                                <th class="col-price">Prix unitaire</th>
                                <th class="col-total">Montant</th>
                                <th class="col-action"><span class="sr-only">Supprimer</span></th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach var="line" items="${lines}" varStatus="status">
                                <tr class="line-row">
                                    <td><input type="text" name="lineDescription" maxlength="255"
                                               value="<c:out value='${line.description}'/>" placeholder="Désignation"></td>
                                    <td><input type="text" name="lineQuantity" inputmode="decimal" class="js-qty"
                                               value="${line.quantityText}" placeholder="1"></td>
                                    <td><input type="text" name="lineUnitPrice" inputmode="decimal" class="js-price"
                                               value="${line.unitPriceText}" placeholder="0"></td>
                                    <td class="col-total js-line-total">—</td>
                                    <td class="col-action">
                                        <button class="btn btn-ghost btn-sm js-remove-line" type="button" title="Supprimer la ligne">✕</button>
                                    </td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>
                    <div class="card-foot">
                        <div class="totals" data-decimals="${settings.currencyDecimals}">
                            <div class="row"><span>Sous-total HT</span><span class="js-subtotal">—</span></div>
                            <div class="row"><span>TVA (<span class="js-rate">0</span> %)</span><span class="js-tax">—</span></div>
                            <div class="row total"><span>Total TTC</span><span class="js-total">—</span></div>
                        </div>
                        <div class="amount-words" id="amountWords"></div>
                    </div>
                </div>

                <div class="form-actions">
                    <button class="btn" type="submit">Enregistrer le brouillon</button>
                    <a class="btn btn-secondary" href="${ctx}/documents?type=${doc.type.code}">Annuler</a>
                </div>
            </form>
        </div>
    </main>
</div>

<script>
(function () {
    var table = document.getElementById('linesTable');
    var body = table.querySelector('tbody');
    var decimals = parseInt(document.querySelector('.totals').getAttribute('data-decimals'), 10);
    if (isNaN(decimals)) { decimals = 0; }

    function parseNumber(value) {
        if (!value) { return 0; }
        var cleaned = value.replace(/\u00a0/g, '').replace(/\s/g, '');
        if (cleaned.indexOf(',') > -1 && cleaned.indexOf('.') > -1) {
            cleaned = cleaned.replace(/\./g, '').replace(',', '.');
        } else {
            cleaned = cleaned.replace(',', '.');
        }
        var parsed = parseFloat(cleaned);
        return isNaN(parsed) ? 0 : parsed;
    }

    function formatNumber(value) {
        var rounded = value.toFixed(decimals);
        var parts = rounded.split('.');
        parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, '\u00a0');
        return parts.join(',');
    }

    function recalc() {
        var subtotal = 0;
        body.querySelectorAll('.line-row').forEach(function (row) {
            var qty = parseNumber(row.querySelector('.js-qty').value);
            var price = parseNumber(row.querySelector('.js-price').value);
            var total = qty * price;
            subtotal += total;
            row.querySelector('.js-line-total').textContent = (qty && price) ? formatNumber(total) : '—';
        });
        var rate = parseNumber(document.getElementById('taxRate').value);
        var tax = subtotal * rate / 100;
        document.querySelector('.js-subtotal').textContent = formatNumber(subtotal);
        document.querySelector('.js-tax').textContent = formatNumber(tax);
        document.querySelector('.js-total').textContent = formatNumber(subtotal + tax);
        document.querySelector('.js-rate').textContent = rate;
    }

    function newRow() {
        var row = document.createElement('tr');
        row.className = 'line-row';
        row.innerHTML = '<td><input type="text" name="lineDescription" maxlength="255" placeholder="Désignation"></td>'
            + '<td><input type="text" name="lineQuantity" inputmode="decimal" class="js-qty" placeholder="1"></td>'
            + '<td><input type="text" name="lineUnitPrice" inputmode="decimal" class="js-price" placeholder="0"></td>'
            + '<td class="col-total js-line-total">—</td>'
            + '<td class="col-action"><button class="btn btn-ghost btn-sm js-remove-line" type="button" title="Supprimer la ligne">✕</button></td>';
        body.appendChild(row);
    }

    document.getElementById('addLine').addEventListener('click', function () {
        newRow();
        recalc();
    });

    body.addEventListener('click', function (event) {
        var button = event.target.closest('.js-remove-line');
        if (!button) { return; }
        if (body.querySelectorAll('.line-row').length <= 1) {
            var row = button.closest('tr');
            row.querySelectorAll('input').forEach(function (input) { input.value = ''; });
        } else {
            button.closest('tr').remove();
        }
        recalc();
    });

    body.addEventListener('input', recalc);
    document.getElementById('taxRate').addEventListener('input', recalc);
    recalc();
})();
</script>
<%@ include file="fragments/foot.jspf" %>
