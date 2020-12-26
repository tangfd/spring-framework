/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.*;
import org.springframework.context.ApplicationListener;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.lang.Nullable;

import java.util.*;

/**
 * Delegate for AbstractApplicationContext's post-processor handling.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
final class PostProcessorRegistrationDelegate {

	private PostProcessorRegistrationDelegate() {
	}

	/**
	 * 1. 实例化并执行所有已注册的BeanFactory后置处理器组件 {@link BeanFactoryPostProcessor}<br/>
	 * 2. BeanFactory后置处理器过程中，按照实现 PriorityOrdered，Ordered，未实现优先级接口的顺序
	 * 进行实例化（通过调用beanFactory的getBean方法进行实例化）及执行 <ul>
	 * <li>
	 * 1. 如果bean工厂实现了BeanDefinitionRegistry接口，先执行类型为{@link BeanDefinitionRegistryPostProcessor}的BeanFactory后置处理器 <ul>
	 * <li>1. 首先获取类型为 BeanDefinitionRegistryPostProcessor 的BeanFactory后置处理器，找到实现PriorityOrdered接口的，
	 * 通过bean工厂的getBean方法实例化，执行{@link BeanDefinitionRegistryPostProcessor#postProcessBeanDefinitionRegistry(BeanDefinitionRegistry)}方法</li>
	 * <li>2. 重新获取bean工厂中类型为BeanDefinitionRegistryPostProcessor 的BeanFactory后置处理器，执行实现了Ordered接口的，
	 * 之前的BeanDefinitionRegistryPostProcessor执行过程中，可能会注册新的BeanDefinitionRegistryPostProcessor，
	 * 所以需要从bean工厂中重新获取</li>
	 * <li>3. 重复获取bean工厂中类型为BeanDefinitionRegistryPostProcessor 的BeanFactory后置处理器，执行没有实现优先级接口的</li>
	 * <li>4. 执行完BeanDefinitionRegistryPostProcessor的接口后，再执行所有BeanFactory后置处理器
	 * 的{@link BeanDefinitionRegistryPostProcessor#postProcessBeanFactory(ConfigurableListableBeanFactory)}方法</li>
	 * </ul>
	 * </li>
	 * <li>2. 获取所有类型BeanFactoryPostProcessor的BeanFactory后置处理器，同样是调用bean工厂的getBean方法进行实例化,
	 * 排除上面已经执行过的BeanFactory后置处理器,按照 PriorityOrdered，Ordered，未实现优先级接口的顺序进行实例化及执行</li>
	 * <li>3. 第二步执行{@link BeanFactoryPostProcessor#postProcessBeanFactory(ConfigurableListableBeanFactory)}时
	 * 不需要重复从bean工厂中获取，只需要按照优先级进行分组，实例化，执行</li>
	 * </ul>
	 * 3. 此方法必须在单实例bean实例化之前执行
	 *
	 * @param beanFactory               bean工厂
	 * @param beanFactoryPostProcessors 已注册的BeanFactory后置处理器
	 */
	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// Invoke BeanDefinitionRegistryPostProcessors first, if any.
		//记录已执行的BeanFactoryPostProcessor，不重复执行
		Set<String> processedBeans = new HashSet<>();
		// 如果bean工厂实现了BeanDefinitionRegistry接口，则进行以下操作，
		// AnnotationConfigApplicationContext 类实现了此接口，则进入if条件中
		// 1. 首先执行已注册的并且实现了BeanDefinitionRegistryPostProcessor接口的BeanFactory后置处理器
		if (beanFactory instanceof BeanDefinitionRegistry) {
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();
			// 遍历所有已注册且实例化的BeanFactory后置处理器，找到类型为BeanDefinitionRegistryPostProcessor的
			// 并执行postProcessBeanDefinitionRegistry()方法

			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					registryProcessors.add(registryProcessor);
				} else {
					regularPostProcessors.add(postProcessor);
				}
			}

			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement
			// PriorityOrdered, Ordered, and the rest.
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();
			// 首先获取类型为 BeanDefinitionRegistryPostProcessor 的BeanFactory后置处理器，
			// 并按照 PriorityOrdered，Ordered，未实现优先级接口的顺序进行实例化及执行
			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			registryProcessors.addAll(currentRegistryProcessors);
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
			currentRegistryProcessors.clear();

			// 重新获取bean工厂中类型为BeanDefinitionRegistryPostProcessor 的BeanFactory后置处理器，执行实现了Ordered接口的
			// 之前的BeanDefinitionRegistryPostProcessor执行过程中，可能会注册新的BeanDefinitionRegistryPostProcessor，
			// 所以需要从bean工厂中重新获取
			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			registryProcessors.addAll(currentRegistryProcessors);
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
			currentRegistryProcessors.clear();

			// 重新获取bean工厂中类型为BeanDefinitionRegistryPostProcessor 的BeanFactory后置处理器，执行没有实现优先级接口的，
			// 此处重复执行的原因与上面一致性，处理BeanFactory后置处理器执行过程中新注册的BeanFactory后置处理器
			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
			boolean reiterate = true;
			while (reiterate) {
				reiterate = false;
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				for (String ppName : postProcessorNames) {
					if (!processedBeans.contains(ppName)) {
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						processedBeans.add(ppName);
						reiterate = true;
					}
				}
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				registryProcessors.addAll(currentRegistryProcessors);
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
				currentRegistryProcessors.clear();
			}

			// 执行完BeanDefinitionRegistryPostProcessor的接口后，
			// 在执行所以BeanFactory后置处理器的postProcessBeanFactory方法
			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		} else {
			// 如果bean工厂没有实现 BeanDefinitionRegistry 接口，直接执行postProcessBeanFactory方法
			// Invoke factory processors registered with the context instance.
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

		/***************************************************************************************
		 * 以上是执行所以 类型为BeanDefinitionRegistryPostProcessor的BeanFactory后置处理器的       *
		 * postProcessBeanDefinitionRegistry方法和                                              *
		 * postProcessBeanFactory方法                                                           *
		 * 下面开始执行已注册的BeanFactoryPostProcessor类型的BeanFactory后置处理器的                *
		 * **************************************************************************************/
		// 获取所有类型BeanFactoryPostProcessor的BeanFactory后置处理器，同样是调用bean工厂的getBean方法进行实例化
		// 排除上面已经执行过的BeanFactory后置处理器
		// 按照 PriorityOrdered，Ordered，未实现优先级接口的顺序进行实例化及执行
		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		// 通过迭代每一个BeanFactory后置处理器，按照优先级进行分组
		for (String ppName : postProcessorNames) {
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
			} else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			} else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			} else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// 执行实现了PriorityOrdered接口的BeanFactory后置处理器
		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// 实例化并执行实现了Ordered接口的BeanFactory后置处理器
		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// 实例化并执行剩下的BeanFactory后置处理器
		// Finally, invoke all other BeanFactoryPostProcessors.
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
		beanFactory.clearMetadataCache();
	}

	/**
	 * 注册(实例化)所有的{@link BeanPostProcessor}，存储再bean工厂中，再bean创建的过程中进行执行
	 * <ul>
	 * <li>1. 从bean工厂中获取所有的{@link BeanPostProcessor}（bean后置处理器）</li>
	 * <li>2. 遍历所有的BeanPostProcessor，按照 实现{@link PriorityOrdered}，{@link Ordered}，未实现顺序接口 进行分组,实例化，注册</li>
	 * <li>3. 单独记录所有的{@link MergedBeanDefinitionPostProcessor}类型的BeanPostProcessor</li>
	 * <li>4. 重新注册{@link MergedBeanDefinitionPostProcessor}类型的BeanPostProcessor，
	 * bean工厂注册过程中，会先删除已注册的BeanPostProcessor，再重新注册BeanPostProcessor</li>
	 * <li>5. 重新注册{@link ApplicationListener}的bean后置处理器，将其移动到处理器链的末尾，用于检查代理类，
	 * 此后置处理在{@link AbstractApplicationContext#prepareBeanFactory(ConfigurableListableBeanFactory)}方法中已经注册过</li>
	 * </ul>
	 *
	 * @param beanFactory        bean工厂
	 * @param applicationContext 应用上下文
	 */
	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {
		// 从bean工厂中获取所有的BeanPostProcessor（bean后置处理器）
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// Register BeanPostProcessorChecker that logs an info message when
		// a bean is created during BeanPostProcessor instantiation, i.e. when
		// a bean is not eligible for getting processed by all BeanPostProcessors.
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// 遍历所有的BeanPostProcessor，按照 实现PriorityOrdered，Ordered，未实现顺序接口 进行分组
		// 单独记录所有的MergedBeanDefinitionPostProcessor类型的BeanPostProcessor
		// Separate between BeanPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				priorityOrderedPostProcessors.add(pp);
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					internalPostProcessors.add(pp);
				}
			} else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			} else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// 先注册实现了PriorityOrdered接口的，并记录MergedBeanDefinitionPostProcessor类型的BeanPostProcessor
		// First, register the BeanPostProcessors that implement PriorityOrdered.
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

		// 再实例并注册实现了Ordered接口的，并记录MergedBeanDefinitionPostProcessor类型的BeanPostProcessor
		// Next, register the BeanPostProcessors that implement Ordered.
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String ppName : orderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			orderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);

		// 最后实例并注册剩下的，并记录MergedBeanDefinitionPostProcessor类型的BeanPostProcessor
		// Now, register all regular BeanPostProcessors.
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String ppName : nonOrderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			nonOrderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		// 重新注册MergedBeanDefinitionPostProcessor类的BeanPostProcessor
		// bean工厂注册过程中，会先删除已注册的BeanPostProcessor，再重新注册BeanPostProcessor
		// Finally, re-register all internal BeanPostProcessors.
		sortPostProcessors(internalPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// 重新注册ApplicationListener的bean后置处理器，将其移动到处理器链的末尾，用于检查代理类
		// 此后置处理在AbstractApplicationContext.prepareBeanFactory方法中已经注册过
		// Re-register post-processor for detecting inner beans as ApplicationListeners,
		// moving it to the end of the processor chain (for picking up proxies etc).
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}

	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
		// Nothing to sort?
		if (postProcessors.size() <= 1) {
			return;
		}
		Comparator<Object> comparatorToUse = null;
		if (beanFactory instanceof DefaultListableBeanFactory) {
			comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
		}
		if (comparatorToUse == null) {
			comparatorToUse = OrderComparator.INSTANCE;
		}
		postProcessors.sort(comparatorToUse);
	}

	/**
	 * Invoke the given BeanDefinitionRegistryPostProcessor beans.
	 */
	private static void invokeBeanDefinitionRegistryPostProcessors(
			Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry, ApplicationStartup applicationStartup) {

		for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
			StartupStep postProcessBeanDefRegistry = applicationStartup.start("spring.context.beandef-registry.post-process")
					.tag("postProcessor", postProcessor::toString);
			postProcessor.postProcessBeanDefinitionRegistry(registry);
			postProcessBeanDefRegistry.end();
		}
	}

	/**
	 * Invoke the given BeanFactoryPostProcessor beans.
	 */
	private static void invokeBeanFactoryPostProcessors(
			Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

		for (BeanFactoryPostProcessor postProcessor : postProcessors) {
			StartupStep postProcessBeanFactory = beanFactory.getApplicationStartup().start("spring.context.bean-factory.post-process")
					.tag("postProcessor", postProcessor::toString);
			postProcessor.postProcessBeanFactory(beanFactory);
			postProcessBeanFactory.end();
		}
	}

	/**
	 * Register the given BeanPostProcessor beans.
	 */
	private static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {

		if (beanFactory instanceof AbstractBeanFactory) {
			// Bulk addition is more efficient against our CopyOnWriteArrayList there
			((AbstractBeanFactory) beanFactory).addBeanPostProcessors(postProcessors);
		}
		else {
			for (BeanPostProcessor postProcessor : postProcessors) {
				beanFactory.addBeanPostProcessor(postProcessor);
			}
		}
	}


	/**
	 * 当bean在beanPostProcessor实例化过程中，bean不能被所有beanPostProcessor处理时，记录一条信息。
	 * BeanPostProcessor that logs an info message when a bean is created during
	 * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
	 * getting processed by all BeanPostProcessors.
	 */
	private static final class BeanPostProcessorChecker implements BeanPostProcessor {

		private static final Log logger = LogFactory.getLog(BeanPostProcessorChecker.class);

		private final ConfigurableListableBeanFactory beanFactory;

		private final int beanPostProcessorTargetCount;

		public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount) {
			this.beanFactory = beanFactory;
			this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (!(bean instanceof BeanPostProcessor) && !isInfrastructureBean(beanName) &&
					this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
				if (logger.isInfoEnabled()) {
					logger.info("Bean '" + beanName + "' of type [" + bean.getClass().getName() +
							"] is not eligible for getting processed by all BeanPostProcessors " +
							"(for example: not eligible for auto-proxying)");
				}
			}
			return bean;
		}

		private boolean isInfrastructureBean(@Nullable String beanName) {
			if (beanName != null && this.beanFactory.containsBeanDefinition(beanName)) {
				BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
				return (bd.getRole() == RootBeanDefinition.ROLE_INFRASTRUCTURE);
			}
			return false;
		}
	}

}
