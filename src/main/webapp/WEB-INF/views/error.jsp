<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="false" %>
<%@ include file="fragments/head.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="active" value=""/>
<div class="app-shell">
    <c:if test="${not empty currentUser}">
        <%@ include file="fragments/sidebar.jspf" %>
    </c:if>
    <main class="content">
        <div class="page" style="padding-top:2.5rem;">
            <div class="card" style="max-width:680px;margin:0 auto;">
                <div class="card-body">
                    <div class="badge badge-danger" style="margin-bottom:.8rem;">Erreur</div>
                    <h1 style="margin-bottom:.4rem;">
                        <c:choose>
                            <c:when test="${not empty errorTitle}"><c:out value="${errorTitle}"/></c:when>
                            <c:when test="${not empty requestScope['jakarta.servlet.error.status_code']}">
                                Erreur <c:out value="${requestScope['jakarta.servlet.error.status_code']}"/>
                            </c:when>
                            <c:otherwise>Une erreur est survenue</c:otherwise>
                        </c:choose>
                    </h1>
                    <p class="muted">
                        <c:choose>
                            <c:when test="${not empty errorMessage}"><c:out value="${errorMessage}"/></c:when>
                            <c:when test="${not empty requestScope['jakarta.servlet.error.message']}">
                                <c:out value="${requestScope['jakarta.servlet.error.message']}"/>
                            </c:when>
                            <c:otherwise>La page demandée n'est pas accessible. Vérifiez l'adresse ou revenez à l'accueil.</c:otherwise>
                        </c:choose>
                    </p>
                    <div class="form-actions">
                        <a class="btn" href="${ctx}/documents?type=DEVIS">Retour à l'accueil</a>
                        <a class="btn btn-secondary" href="javascript:history.back()">Page précédente</a>
                    </div>
                </div>
            </div>
        </div>
    </main>
</div>
<%@ include file="fragments/foot.jspf" %>
