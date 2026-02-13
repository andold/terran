package kr.andold.terran;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ApplicationContextProvider implements ApplicationContextAware {
	private static ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		ApplicationContextProvider.applicationContext = applicationContext;
	}

	public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

	public static Object getBean(String beanName) {
		return applicationContext.getBean(beanName);
	}

	public static Object getBean(Class<?> classType) {
        return applicationContext.getBean(classType); // Container에서 param의 class에 해당하는 bean 가져오고 return
    }

}
