package com.tfd.study.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * AOP切面类
 *
 * @author TangFD
 * @since 2019/8/14.
 */
@Aspect
@Component
public class TestAspectj {

	@Pointcut("execution(* * ..*(..))")
	public void pointCut() {

	}

	@Before("pointCut()")
	public void before(JoinPoint joinPoint) {
		Object[] args = joinPoint.getArgs();
		System.out.println(Arrays.asList(args));
	}

	@After("pointCut()")
	public void after(JoinPoint joinPoint) {
		Signature signature = joinPoint.getSignature();
		System.out.println(signature);
	}

	@AfterReturning(value = "pointCut()", returning = "result")
	public void afterReturning(JoinPoint joinPoint, Object result) {
		System.out.println("afterReturning : " + result);
	}

	@AfterThrowing(value = "pointCut()", throwing = "throwable")
	public void afterThrowing(JoinPoint joinPoint, Throwable throwable) {
		System.out.println("afterThrowing : " + throwable.getMessage());
	}

	@Around(value = "pointCut()")
	public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
		System.out.println("--------around start--------");
		Object proceed = joinPoint.proceed();
		System.out.println("--------around end--------");
		return proceed;
	}
}
