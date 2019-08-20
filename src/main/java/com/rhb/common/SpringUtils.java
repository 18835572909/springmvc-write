package com.rhb.common;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value="singleton")
public class SpringUtils implements ApplicationContextAware{

	private ApplicationContext ac;
	private static volatile SpringUtils su;
	private SpringUtils() {}
	public static synchronized SpringUtils getInstance() {
		if(su == null) {
			synchronized (su) {
				if(su==null)
					su = new SpringUtils();
			}
		}
		return su;
	}
	
	@Override
	public void setApplicationContext(ApplicationContext ac) throws BeansException {
		this.ac=ac;
	}

	public Object getBean(String name) throws BeansException {
		return ac.getBean(name);
	}

	public <T> T getBean(Class<T> requiredType) throws BeansException {
		return ac.getBean(requiredType);
	}
	
}
