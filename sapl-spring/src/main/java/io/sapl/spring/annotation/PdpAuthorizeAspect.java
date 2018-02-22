package io.sapl.spring.annotation;

import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.sapl.api.pdp.Decision;
import io.sapl.api.pdp.Response;
import io.sapl.spring.SAPLAuthorizator;
import io.sapl.spring.marshall.Action;
import io.sapl.spring.marshall.Resource;
import io.sapl.spring.marshall.Subject;
import io.sapl.spring.marshall.action.HttpAction;
import io.sapl.spring.marshall.action.SimpleAction;
import io.sapl.spring.marshall.resource.HttpResource;
import io.sapl.spring.marshall.resource.StringResource;
import io.sapl.spring.marshall.subject.AuthenticationSubject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
public class PdpAuthorizeAspect {

	private static final String DEFAULT = "default";

	private final SAPLAuthorizator pep;

	@Autowired
	public PdpAuthorizeAspect(SAPLAuthorizator pep) {
		super();
		this.pep = pep;
	}

	@Autowired
	private TokenStore tokenStore;
	
	@Around("@annotation(pdpAuthorize) && execution(* *(..))")
	public Object around(ProceedingJoinPoint pjp, PdpAuthorize pdpAuthorize) throws Throwable {
		Subject subject;
		Action action;
		Resource resource;
		Object httpRequest = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		JsonNode details = new ObjectMapper().convertValue(authentication.getDetails(), JsonNode.class);
		
		if (!(DEFAULT).equals(pdpAuthorize.subject())) {
			LOGGER.debug("Using subject from manual input");
			authentication = new UsernamePasswordAuthenticationToken(pdpAuthorize.subject(),
					pdpAuthorize.subject());
			subject = new AuthenticationSubject(authentication);
			
		} else if (details.findValue("tokenValue") != null) {
			LOGGER.debug("Using subject from JWT");
			String token = details.findValue("tokenValue").textValue();
			OAuth2AccessToken parsedToken = tokenStore.readAccessToken(token);
			Map<String, Object> claims = parsedToken.getAdditionalInformation();
			for (Entry<String, Object> kvp : claims.entrySet()) {
				LOGGER.debug("Single claim: {} -> {}", kvp.getKey(), kvp.getValue());
			}
			LOGGER.debug("Retrieved token: {}", token);
			subject = new AuthenticationSubject(authentication, claims);
		
		} else { 
			LOGGER.debug("Using default subject");
			subject = new AuthenticationSubject(authentication);
		}

		
				
		if (!(DEFAULT).equals(pdpAuthorize.action())) {
			LOGGER.debug("Using action from manual input");
			action = new SimpleAction(pdpAuthorize.action());
			
		} else if (HttpServletRequest.class.isInstance(httpRequest)) {
			LOGGER.debug("Using action from HttpServletRequest");
			action = new HttpAction(RequestMethod.valueOf(((HttpServletRequest) httpRequest).getMethod()));
		
		} else {
			LOGGER.debug("Using default action");
			action = new SimpleAction(pjp.getSignature().getName());
		}
		
		
		if (!(DEFAULT).equals(pdpAuthorize.resource())) {
			LOGGER.debug("Using resource from manual input");
			resource = new StringResource(pdpAuthorize.resource());
			
		} else if (HttpServletRequest.class.isInstance(httpRequest)){
			LOGGER.debug("Using resource from HttpServletRequest");
			resource = new HttpResource((HttpServletRequest) httpRequest);
			
		} else {
			LOGGER.debug("Using default resource");
			resource = new StringResource(pjp.getTarget().getClass().getSimpleName());
		}
		
		Response response = pep.getResponse(subject, action, resource);

		if (response.getDecision() == Decision.DENY) {
			LOGGER.debug("Access denied");
			throw new AccessDeniedException("Insufficient Permission");
		}

		LOGGER.debug("Access granted");
		return pjp.proceed();
	}

}
