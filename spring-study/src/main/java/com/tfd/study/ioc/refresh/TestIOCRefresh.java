package com.tfd.study.ioc.refresh;

import com.tfd.study.ioc.refresh.domain.Blue;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @since TangFD@HF 2019/7/25.
 */
@Configuration
public class TestIOCRefresh {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestIOCRefresh.class);
		Blue bean = context.getBean(Blue.class);
		bean = context.getBean(Blue.class);
		context.close();
	}

	@Bean
	public Blue blue() {
		return new Blue();
	}
}
