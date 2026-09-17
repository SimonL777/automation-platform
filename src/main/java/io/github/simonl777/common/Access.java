package io.github.simonl777.common;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
@Configuration
public class Access {
 public record Actor(String ownerId,boolean internal){}
 public static Actor actor(Authentication a){return (Actor)a.getPrincipal();}
 public static String owner(Authentication a){return actor(a).ownerId();}
 public static void requireInternal(Authentication a){if(!actor(a).internal())throw new ApiFailure(403,"SERVICE_AUTH_REQUIRED");}
 @Bean SecurityFilterChain security(HttpSecurity http,@Value("${app.api-token}")String api,@Value("${app.service-token}")String service,@Value("${app.jwt-issuer}")String issuer,@Value("${app.jwt-jwks}")String jwks,@Value("${app.jwt-audience}")String audience)throws Exception{
  if(api.isBlank()&&issuer.isBlank())throw new IllegalStateException("Configure API_TOKEN or SUPABASE_JWT_ISSUER");
  if(!api.isBlank()&&api.length()<24)throw new IllegalStateException("API_TOKEN must have at least 24 characters");
  if(!service.isBlank()&&service.length()<24)throw new IllegalStateException("SERVICE_TOKEN must have at least 24 characters");
  JwtDecoder decoder=null;
  if(!issuer.isBlank()){
   if(!issuer.startsWith("https://")||!jwks.startsWith("https://"))throw new IllegalStateException("JWT issuer and JWKS must use HTTPS");
   NimbusJwtDecoder d=NimbusJwtDecoder.withJwkSetUri(jwks).jwsAlgorithms(a->{a.add(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256);a.add(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.ES256);}).build();
   d.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer),new JwtClaimValidator<List<String>>("aud",v->v!=null&&v.contains(audience))));decoder=d;
  }
  JwtDecoder jwt=decoder;
  OncePerRequestFilter filter=new OncePerRequestFilter(){
   protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
    String header=req.getHeader("Authorization");
    if(header!=null&&header.startsWith("Bearer ")&&header.length()<8192){
     String token=header.substring(7);Actor who=null;
     if(matches(service,token)){String owner=req.getHeader("X-Owner-Id");if(owner!=null&&owner.matches("[A-Za-z0-9_-]{1,128}"))who=new Actor(owner,true);}
     else if(matches(api,token))who=new Actor("demo",false);
     else if(jwt!=null){try{Jwt claims=jwt.decode(token);String sub=claims.getSubject();if(sub!=null&&sub.matches("[A-Za-z0-9_-]{1,128}"))who=new Actor(sub,false);}catch(JwtException ignored){}}
     if(who!=null)SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(who,null,List.of(new SimpleGrantedAuthority(who.internal()?"ROLE_SERVICE":"ROLE_USER"))));
    }
    chain.doFilter(req,res);
   }
  };
  return http.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .authorizeHttpRequests(a->a.requestMatchers("/actuator/health","/actuator/health/**").permitAll().anyRequest().authenticated())
   .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->{res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"error\":\"AUTH_REQUIRED\"}");}))
   .addFilterBefore(filter,AnonymousAuthenticationFilter.class).build();
 }
 private static boolean matches(String expected,String actual){return !expected.isBlank()&&MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),actual.getBytes(StandardCharsets.UTF_8));}
}
