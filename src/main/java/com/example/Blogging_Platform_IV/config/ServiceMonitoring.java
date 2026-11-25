
package com.example.Blogging_Platform_IV.config;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(0)
public class ServiceMonitoring {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceMonitoring.class);
    // The business services. JwtService and AccountUserDetailsService run on every request, so they are left out.
    private static final String SERVICES = "execution(public * com.example.Blogging_Platform_IV.service.*Service.*(..))"
            + " && !within(com.example.Blogging_Platform_IV.service.JwtService)"
            + " && !within(com.example.Blogging_Platform_IV.service.AccountUserDetailsService)";

    @Before(SERVICES)
    public void entering(JoinPoint call) {
        LOG.debug("Entering {}", call.getSignature().toShortString());
    }

    @After(SERVICES)
    public void leaving(JoinPoint call) {
        LOG.debug("Leaving {}", call.getSignature().toShortString());
    }

    @Around(SERVICES)
    public Object time(ProceedingJoinPoint call) throws Throwable {
        long start = System.nanoTime();
        String outcome = "error";
        try {
            Object result = call.proceed();
            outcome = "success";
            return result;
        } finally {
            long elapsed = System.nanoTime() - start;
            String operation = call.getSignature().toShortString();
            LOG.debug("{} {} in {} ms", operation, outcome, elapsed / 1_000_000.0);
        }
    }
}
