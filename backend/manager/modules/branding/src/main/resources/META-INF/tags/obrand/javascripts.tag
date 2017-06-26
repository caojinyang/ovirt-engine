<%@ tag language="java" pageEncoding="UTF-8"%>
<%@ tag body-content="empty" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="baseTheme" value="${requestScope['brandingStyle'][0]}" />
<script type="text/javascript" src="${pageContext.request.contextPath}${initParam['obrandThemePath']}${baseTheme.path}/ovirt-js-dependencies/jquery/dist/jquery.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}${initParam['obrandThemePath']}${baseTheme.path}/ovirt-js-dependencies/jquery-ui-dist/jquery-ui.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}${initParam['obrandThemePath']}${baseTheme.path}/ovirt-js-dependencies/bootstrap/dist/js/bootstrap.min.js"></script>
<%-- TODO remove temporary local custom patternfly when patternfly PR 673 is released
<script type="text/javascript" src="${pageContext.request.contextPath}${initParam['obrandThemePath']}${baseTheme.path}/ovirt-js-dependencies/patternfly/dist/js/patternfly.min.js"></script>
--%>
<script type="text/javascript" src="${pageContext.request.contextPath}${initParam['obrandThemePath']}${baseTheme.path}/patternfly-3.25.1-custom.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}${initParam['obrandThemePath']}${baseTheme.path}/ovirt-js-dependencies/insertion-query/insQ.min.js"></script>
