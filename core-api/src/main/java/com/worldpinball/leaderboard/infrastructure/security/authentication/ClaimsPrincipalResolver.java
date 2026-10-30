package com.worldpinball.leaderboard.infrastructure.security.authentication;

import com.worldpinball.leaderboard.infrastructure.security.authentication.ClaimsPrincipal.Claim;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class ClaimsPrincipalResolver {

    private final OperatorTokenAuthenticationHandler operatorTokenHandler;
    private final MachineCertificateAuthenticator machineCertAuthenticator;
    private final MachineClaimsTransformation machineClaimsTransformation;

    public ClaimsPrincipalResolver(
            OperatorTokenAuthenticationHandler operatorTokenHandler,
            MachineCertificateAuthenticator machineCertAuthenticator,
            MachineClaimsTransformation machineClaimsTransformation
    ) {
        this.operatorTokenHandler = operatorTokenHandler;
        this.machineCertAuthenticator = machineCertAuthenticator;
        this.machineClaimsTransformation = machineClaimsTransformation;
    }

    public ClaimsPrincipal resolveFromSecurityContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }

        List<Claim> claims = new ArrayList<>();
        List<String> roles = new ArrayList<>();

        if (auth.getPrincipal() instanceof Jwt jwt) {
            String sub = jwt.getSubject();
            if (sub != null) {
                claims.add(new Claim(ClaimsPrincipal.CLAIM_NAME_IDENTIFIER, sub));
            }
            String name = jwt.getClaimAsString("preferred_username");
            if (name != null) {
                claims.add(new Claim(ClaimsPrincipal.CLAIM_NAME, name));
            }
            extractClaimValues(jwt.getClaim("permission"), "permission", claims);
            extractClaimValues(jwt.getClaim("permissions"), "permissions", claims);
            extractClaimValues(jwt.getClaim("scope"), "scope", claims);
        } else {
            if (auth.getName() != null) {
                claims.add(new Claim(ClaimsPrincipal.CLAIM_NAME_IDENTIFIER, auth.getName()));
                claims.add(new Claim(ClaimsPrincipal.CLAIM_NAME, auth.getName()));
            }
        }

        if (auth.getAuthorities() != null) {
            for (GrantedAuthority ga : auth.getAuthorities()) {
                String authority = ga.getAuthority();
                if (authority == null) continue;
                if (authority.startsWith("ROLE_")) {
                    roles.add(authority.substring("ROLE_".length()));
                } else if (authority.startsWith("SCOPE_")) {
                    claims.add(new Claim("scope", authority.substring("SCOPE_".length())));
                } else {
                    roles.add(authority);
                }
            }
        }

        return new ClaimsPrincipal("Bearer", claims, roles);
    }

    public ClaimsPrincipal resolveOperatorPrincipal(HttpServletRequest request) {
        if (request == null) return null;
        String opHeader = request.getHeader(OperatorTokenAuthenticationHandler.HEADER_NAME);
        String authHeader = request.getHeader("Authorization");
        var res = operatorTokenHandler.authenticate(opHeader, authHeader);
        if (res instanceof OperatorTokenAuthenticationHandler.AuthenticateResult.Success s) {
            return s.principal();
        }
        return null;
    }

    public ClaimsPrincipal resolveMachinePrincipal(HttpServletRequest request) {
        if (request == null) return null;
        Object attr = request.getAttribute(MachineCertificateAuthenticator.SERVLET_CERT_ATTRIBUTE);
        if (attr instanceof X509Certificate[] certs && certs.length > 0 && certs[0] != null) {
            var res = machineCertAuthenticator.authenticate(certs[0]);
            if (res instanceof MachineCertificateAuthenticator.CertificateAuthResult.Success s) {
                return machineClaimsTransformation.transform(s.principal());
            }
        }
        return null;
    }

    public ClaimsPrincipal resolveAnyPrincipal(HttpServletRequest request) {
        ClaimsPrincipal jwtPrincipal = resolveFromSecurityContext();
        if (jwtPrincipal != null) {
            return jwtPrincipal;
        }
        ClaimsPrincipal machinePrincipal = resolveMachinePrincipal(request);
        if (machinePrincipal != null) {
            return machinePrincipal;
        }
        return resolveOperatorPrincipal(request);
    }

    private void extractClaimValues(Object raw, String type, List<Claim> target) {
        if (raw == null) return;
        if (raw instanceof Collection<?> col) {
            for (Object item : col) {
                if (item != null) {
                    target.add(new Claim(type, item.toString()));
                }
            }
        } else {
            target.add(new Claim(type, raw.toString()));
        }
    }
}
