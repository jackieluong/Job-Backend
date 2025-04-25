package com.example.Job.security;

import com.example.Job.entity.Account;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

// Util class for JWT actions

@Component
public class JwtUtil {

    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS512;

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    @Value("${app.jwt-secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration-seconds}")
    private long accessTokenExpiration;


    @Value("${app.jwt.refresh-token-expiration-seconds}")
    private long refreshTokenExpiration;


    public JwtUtil(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
    }

//    private SecretKey getSecretKey(){
//        byte[] keyBytes = Base64.from(jwtSecret).decode();
//        return new SecretKeySpec(keyBytes, 0, keyBytes.length , JwtTokenProvider.JWT_ALGORITHM.getName());
//    }

    public static boolean isAuthenticated() {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && !authentication.getPrincipal().equals("anonymousUser");
    }

    public Jwt verifyToken(String token){
//        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(
//                getSecretKey()).macAlgorithm(JwtTokenProvider.JWT_ALGORITHM).build();

        try{
            Jwt decodedToken =  jwtDecoder.decode(token);
            return decodedToken;
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    // Function to extractClaim from JWT token
    public <T> T extractClaim(String token, String claimName){
        try{
            Jwt decodedToken = verifyToken(token);
            return (T) decodedToken.getClaim(claimName);
        }catch(RuntimeException e){
            throw new RuntimeException(e);
        }
    }


    public static String getAccessToken(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)){
            throw new RuntimeException("User is not authenticated with JWT Token");
        }

        String token = jwt.getTokenValue();
        return token;
    }

    public String extractUserIdFromToken(){

       String token = getAccessToken();

        try{
            Map<String, Object> userClaim = extractClaim(token, "user");

            if(userClaim != null && userClaim.containsKey("id")){
                return userClaim.get("id").toString();
            }
            return null;

        }catch(RuntimeException e){
            return null;
        }
    }

    public String extractUserIdFromToken(String token){
        try{
            Map<String, Object> userClaim = extractClaim(token, "user");

            if(userClaim != null && userClaim.containsKey("id")){
                return userClaim.get("id").toString();
            }
            return null;

        }catch(RuntimeException e){
            return null;
        }
    }

    public String createAccessToken( Account user){

        Instant now = Instant.now();
        Instant validity = now.plus(accessTokenExpiration, ChronoUnit.SECONDS);

        JwtAuthResponse.UserLogin userLogin = new JwtAuthResponse.UserLogin(user.getId(), user.getEmail(), user.getName(), user.getRole());


        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(user.getEmail())
                .claim("user", userLogin)
                .build();

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();

    }

    public String createRefreshToken(String email, JwtAuthResponse jwtAuthResponse){
        Instant now = Instant.now();
        Instant validity = now.plus(refreshTokenExpiration, ChronoUnit.SECONDS);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(email)
                .claim("user", jwtAuthResponse.getUserLogin())
                .build();

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();

    }

    /**
     * Get the login of the current user.
     *
     * @return the login of the current user.
     */
    public static Optional<String> getCurrentUserLogin() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(extractPrincipal(securityContext.getAuthentication()));
    }

    private static String extractPrincipal(Authentication authentication) {

        if (authentication == null) {
            return null;
        } else if (authentication.getPrincipal() instanceof UserDetails springSecurityUser) {
            return springSecurityUser.getUsername();
        } else if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        } else if (authentication.getPrincipal() instanceof String s) {
            return s;
        }
        return null;
    }

    /**
     * Get the JWT of the current user.
     *
     * @return the JWT of the current user.
     */
    public static Optional<String> getCurrentUserJWT() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(securityContext.getAuthentication())
                .filter(authentication -> authentication.getCredentials() instanceof String)
                .map(authentication -> (String) authentication.getCredentials());
    }

//    /**
//     * Check if a user is authenticated.
//     *
//     * @return true if the user is authenticated, false otherwise.
//     */
//    public static boolean isAuthenticated() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        return authentication != null && getAuthorities(authentication).noneMatch(AuthoritiesConstants.ANONYMOUS::equals);
//    }

//    /**
//     * Checks if the current user has any of the authorities.
//     *
//     * @param authorities the authorities to check.
//     * @return true if the current user has any of the authorities, false otherwise.
//     */
//    public static boolean hasCurrentUserAnyOfAuthorities(String... authorities) {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        return (
//                authentication != null && getAuthorities(authentication).anyMatch(authority -> Arrays.asList(authorities).contains(authority))
//        );
//    }
//
//    /**
//     * Checks if the current user has none of the authorities.
//     *
//     * @param authorities the authorities to check.
//     * @return true if the current user has none of the authorities, false otherwise.
//     */
//    public static boolean hasCurrentUserNoneOfAuthorities(String... authorities) {
//        return !hasCurrentUserAnyOfAuthorities(authorities);
//    }
//
//    /**
//     * Checks if the current user has a specific authority.
//     *
//     * @param authority the authority to check.
//     * @return true if the current user has the authority, false otherwise.
//     */
//    public static boolean hasCurrentUserThisAuthority(String authority) {
//        return hasCurrentUserAnyOfAuthorities(authority);
//    }
//
//    private static Stream<String> getAuthorities(Authentication authentication) {
//        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority);
//    }
}
