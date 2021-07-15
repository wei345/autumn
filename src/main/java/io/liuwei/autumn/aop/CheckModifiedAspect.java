package io.liuwei.autumn.aop;

import io.liuwei.autumn.annotation.CheckModified;
import io.liuwei.autumn.model.RevisionContent;
import io.liuwei.autumn.util.WebUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 检查 ETag，如果没有变化则返回 304。
 *
 * @author liuwei
 * @since 2021-07-08
 */
@SuppressWarnings("ConstantConditions")
@Component
@Aspect
public class CheckModifiedAspect {
    @Around("@annotation(checkModified)")
    public Object doAround(ProceedingJoinPoint joinPoint, CheckModified checkModified) throws Throwable {
        Object result = joinPoint.proceed();

        if (result instanceof RevisionContent) {
            RevisionContent rc = (RevisionContent) result;
            ServletRequestAttributes sra = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes());
            if (WebUtil.checkNotModified(rc.getRevision(), rc.getEtag(), sra.getRequest(), sra.getResponse())) {
                return ResponseEntity
                        .status(HttpStatus.NOT_MODIFIED)
                        .eTag(rc.getEtag())
                        .build();
            }
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .eTag(rc.getEtag())
                    .contentType(rc.getMediaType())
                    .body(rc.getContent());
        }

        if (result == null) {
            ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                    .getResponse()
                    .sendError(404);
        }

        return result;
    }
}