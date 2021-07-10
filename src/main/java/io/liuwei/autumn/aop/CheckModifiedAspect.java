package io.liuwei.autumn.aop;

import io.liuwei.autumn.annotation.CheckModified;
import io.liuwei.autumn.model.RevisionContent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Aspect
public class CheckModifiedAspect {
    @Around("@annotation(checkEtag)")
    public Object doAround(ProceedingJoinPoint joinPoint, CheckModified checkEtag) throws Throwable {
        ServletRequestAttributes sra = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes());
        HttpServletRequest request = sra.getRequest();
        HttpServletResponse response = sra.getResponse();
        ServletWebRequest webRequest = new ServletWebRequest(request, response);

        Object result = joinPoint.proceed();
        if (result instanceof RevisionContent) {
            RevisionContent rc = (RevisionContent) result;
            if (webRequest.checkNotModified(rc.getEtag())) {
                return null;
            }
            return rc.getContent();
        }

        return result;
    }
}