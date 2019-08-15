/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation;

import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.aop.aspectj.autoproxy.AspectJAwareAdvisorAutoProxyCreator;
import org.springframework.aop.config.AopConfigUtils;
import org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

import java.lang.annotation.*;

/**
 * 1. {@link EnableAspectJAutoProxy @EnableAspectJAutoProxy}功能解析
 * <ul>
 * <li>1. 通过{@link Import @Import}注解向容器中注入{@link AspectJAutoProxyRegistrar}组件</li>
 * <li>2. {@link AspectJAutoProxyRegistrar} 是一个 {@link ImportBeanDefinitionRegistrar}，
 * spring容器在执行{@link BeanDefinitionRegistryPostProcessor#postProcessBeanDefinitionRegistry}方法时，
 * 进行调用{@link ImportBeanDefinitionRegistrar#registerBeanDefinitions}方法
 * 实际执行的为（{@link ConfigurationClassPostProcessor#postProcessBeanDefinitionRegistry}），
 * 并向spring容器中注册{@link AnnotationAwareAspectJAutoProxyCreator}组件，
 * 它实际是一个{@link InstantiationAwareBeanPostProcessor},在bean创建之前尝试获取代理对象进行返回，
 * bean名称为：{@link AopConfigUtils#AUTO_PROXY_CREATOR_BEAN_NAME}</li>
 * </ul>
 * 2. {@link AnnotationAwareAspectJAutoProxyCreator} 功能解析
 * <p>
 * 继承关系：
 * {@link AnnotationAwareAspectJAutoProxyCreator}[initBeanFactory]
 * <p>
 * > {@link AspectJAwareAdvisorAutoProxyCreator}
 * <p>
 * > {@link AbstractAdvisorAutoProxyCreator}[setBeanFactory --> initBeanFactory]
 * <p>
 * > {@link AbstractAutoProxyCreator}[setBeanFactory,postProcessBeforeInstantiation,postProcessAfterInitialization]
 * <p>
 * > {@link SmartInstantiationAwareBeanPostProcessor}
 * <p>
 * > {@link InstantiationAwareBeanPostProcessor}
 * <p>
 * > {@link BeanPostProcessor}
 * <ul>
 * <li>1.</li>
 * <li></li>
 * <li></li>
 * <li></li>
 * <li></li>
 * <li></li>
 * <li></li>
 * <li></li>
 * <li></li>
 * <li></li>
 * <li></li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AspectJAutoProxyRegistrar.class)
public @interface EnableAspectJAutoProxy {

	/**
	 * Indicate whether subclass-based (CGLIB) proxies are to be created as opposed
	 * to standard Java interface-based proxies. The default is {@code false}.
	 */
	boolean proxyTargetClass() default false;

	/**
	 * Indicate that the proxy should be exposed by the AOP framework as a {@code ThreadLocal}
	 * for retrieval via the {@link org.springframework.aop.framework.AopContext} class.
	 * Off by default, i.e. no guarantees that {@code AopContext} access will work.
	 *
	 * @since 4.3.1
	 */
	boolean exposeProxy() default false;

}
