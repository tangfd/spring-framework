package com.tfd.study.tx;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.UUID;

/**
 * @since TangFD@HF 2019/9/4.
 */
@Configuration
@EnableTransactionManagement
@ComponentScan("com.tfd.study.tx")
public class TestTx {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext context
				= new AnnotationConfigApplicationContext(TestTx.class);
		TestService service = context.getBean(TestService.class);
		String name = UUID.randomUUID().toString();
		service.save(name);
		service.get(name);
		name = UUID.randomUUID().toString();
		try {
			service.rollback(name);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		service.getNoTransactional(name);
	}

	@Bean
	public PlatformTransactionManager platformTransactionManager(DataSource dataSource) {
		return new DataSourceTransactionManager(dataSource);
	}

	@Bean
	public DataSource dataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://192.168.0.69:3306/veln?characterEncoding=utf-8&useSSL=false");
		dataSource.setUsername("qguser");
		dataSource.setPassword("qguser");
		return dataSource;
	}

	@Bean
	public JdbcTemplate jdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}
}
