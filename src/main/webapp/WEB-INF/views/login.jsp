<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="fragments/head.jspf" %>
<div class="auth">
    <div>
        <div class="auth-card">
            <div class="brand">
                <div class="brand-mark">DF</div>
                <div class="brand-text">
                    <strong>Dev-Fact</strong>
                    <span>Devis &amp; Factures</span>
                </div>
            </div>

            <h1>Connexion</h1>
            <p class="lead">Accédez à la gestion de vos devis et de vos factures.</p>

            <c:if test="${not empty error}">
                <div class="alert alert-error" role="alert"><c:out value="${error}"/></div>
            </c:if>
            <c:if test="${not databaseAvailable and not empty statusMessage}">
                <div class="alert alert-info" role="status"><c:out value="${statusMessage}"/></div>
            </c:if>

            <form method="post" action="${pageContext.request.contextPath}/login" autocomplete="on">
                <input type="hidden" name="next" value="<c:out value='${next}'/>">
                <div class="field" style="margin-bottom: .9rem;">
                    <label for="username">Identifiant</label>
                    <input type="text" id="username" name="username" value="<c:out value='${username}'/>"
                           required autofocus autocomplete="username" placeholder="admin">
                </div>
                <div class="field" style="margin-bottom: 1.2rem;">
                    <label for="password">Mot de passe</label>
                    <input type="password" id="password" name="password" required
                           autocomplete="current-password" placeholder="••••••••">
                </div>
                <button class="btn btn-block" type="submit">Se connecter</button>
            </form>
        </div>
        <p class="auth-foot">Dev-Fact · Jakarta EE 10 · WildFly · PostgreSQL</p>
    </div>
</div>
<%@ include file="fragments/foot.jspf" %>
