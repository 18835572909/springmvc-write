package com.rhb.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.rhb.annotation.RhbController;
import com.rhb.annotation.RhbQualifier;
import com.rhb.annotation.RhbRequestMapping;
import com.rhb.annotation.RhbRequestParam;
import com.rhb.annotation.RhbService;
import com.rhb.common.CommonUtils;

/**
 * @ClassName:  DefaultDispatcherServlet   
 * @Description: SpringMVC核心DispatcherServlet自定义实现
 * @author: renhuibo
 * @date:   2019年8月19日 上午11:49:26
 */
public class DefaultDispatcherServlet extends HttpServlet{

	private static final long serialVersionUID = -2012779787830384096L;

	/**
	 * 自定义组件
	 */
	
	private List<String> beanNames = new ArrayList<>();					//储存扫描的类
	
	private Map<String, Object> instanceMap = new HashMap<>(); 			//Controller和Service层实例对象 < key-beanName,value-class(Object) >
		
	private Map<String, Object> handlerMaps = new HashMap<>();			//类似HandlerMapping，存储url处理方法：< key-url,value-method(Method) >	
	
	private Map<String, Object> controllerMaps = new HashMap<>();		//存储controller ：　< key-url,value-controller(Object) >
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		System.out.println("mvc init start...");
		
		/*
		 * 1. 获取web.xml文件配置信息
		 * 2. 扫描基础包
		 * 3. 对基础包中使用注解的 类,初始化实例map。   --SpringIOC的装载完毕
		 * 4. 实现SpringIOC-DI的功能				 --SpringIOC的DI完毕
		 * 5. 初始化HandlerMapping
		 */
		String mvcConfigPath = config.getInitParameter("contextConfigLocation");
		System.out.println("mvc-config-path:"+mvcConfigPath);
		String basePath = "";
		try {
			basePath = CommonUtils.getBaseScanPackageName(mvcConfigPath,"base-package");
			System.out.println("base-path:"+basePath);
		} catch (Exception e) {
			e.printStackTrace();
		}
		scanBasePackage(basePath);
		initInstanceMap();
		doSpringIOC();
		initHandlerMapping();
		
		System.out.println("mvc init end...");
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		this.doDispatch(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		this.doDispatch(request, response);
	}

	private void doDispatch(HttpServletRequest request,HttpServletResponse response) {
		System.out.println("doDispatch...");
		if(handlerMaps.isEmpty()) {
			return;
		}
		
		String uri = request.getRequestURI();
		String contextPath = request.getContextPath();
		String url = uri.replace(contextPath, "");
		System.out.println("URI："+uri+"\nContextPath："+contextPath+"\nURL："+url);

		Method handMethod = (Method) handlerMaps.get(url);
		if(handMethod == null) {
			try {
				response.getWriter().println("404!资源不存在");
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		
		Parameter[] params = handMethod.getParameters();
		Object[] pValue =new Object[params.length];
		for(int i=0 ; i<pValue.length ; i++) {
			
			//对controller中HttpServletRequest\HttpServletResponse参数先做处理
			if(ServletRequest.class.isAssignableFrom(params[i].getType())) {
				pValue[i] = request;
			}
			if(ServletResponse.class.isAssignableFrom(params[i].getType())) {
				pValue[i] = response;
			}
			
			//如果使用@RequestParam 则使用value作为参数名，否则使用handler-method中的原本参数名作为参数名
			String pName = params[i].getName();
			if(params[i].isAnnotationPresent(RhbRequestParam.class)) {
				RhbRequestParam requestParam = params[i].getAnnotation(RhbRequestParam.class);
				pName = requestParam.value();
			}
			
			String pVal = request.getParameter(pName);
			System.out.println("URL参数解析："+i+"、"+pName+" - "+pVal);
			if(Integer.class.isAssignableFrom(params[i].getType())) {
				pValue[i] = Integer.parseInt(pVal);
			}
			if(Double.class.isAssignableFrom(params[i].getType())) {
				pValue[i] = Double.parseDouble(pVal);
			}
			if(String.class.isAssignableFrom(params[i].getType())) {
				pValue[i] = pVal;
			}
			if(Date.class.isAssignableFrom(params[i].getType())) {
				try {
					pValue[i] = new SimpleDateFormat("yyyy-MM-dd HH:mm:s").parse(pVal);
				} catch (ParseException e) {
					try {
						response.getWriter().println("500! 日期类型格式错误");
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		}
		try {
			Object obj = handMethod.invoke(controllerMaps.get(url), pValue);
			if(obj == null) return;
			PrintWriter writer = response.getWriter();
			//响应解析
			if(obj instanceof String) {
				writer.println((String)obj);
				return;
			}
			if(obj instanceof Map || obj instanceof List || obj instanceof Set) {
				writer.println(JSON.toJSONString(obj));
				return;
			}
			
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @Description: 扫描配置中配置的所有类  
	 * @author: renhuibo
	 * @date:   2019年8月19日 下午4:19:52
	 */
	private void scanBasePackage(String basePackage) {
		String basePath = CommonUtils.exchangePointToPath(basePackage);
		System.out.println("scan-path:"+basePath);
		URL baseUrl = null;
		try {
			baseUrl = ResourceUtils.getURL("classpath:"+basePath);
			File file = new File(baseUrl.getFile());
			File[] listFiles = file.listFiles();
			for(File child : listFiles) {
				if(child.isDirectory()) {
					scanBasePackage(basePackage+"."+child.getName());
				}else {
					//Class.forName():需要完整路径名
					String beanName =basePackage +"."+ child.getName().replaceAll(".class", "");
					beanNames.add(beanName);
					System.out.println("SCAN CLASS: "+beanName);
				}
			}
		} catch (Exception e) {
			System.out.println("ScanBasePackage is error!");
		}
	}
	
	/**
	 * @Description: 利用扫描到的类，初始化instanceMap来存储 (@Controller\@Service -> instanceMap)
	 * @author: renhuibo
	 * @date:   2019年8月19日 下午4:03:08
	 */
	@SuppressWarnings({"unchecked","rawtypes"})
	private void initInstanceMap() {
		Assert.notEmpty(beanNames, "No scanned class files!");
		
		this.beanNames.forEach(x -> {
			try {
				Class cla = Class.forName(x);
				String beanName = CommonUtils.exchangePointToPath(cla.getSimpleName());
				if(cla.isAnnotationPresent(RhbController.class)) {
					RhbController controller = (RhbController) cla.getAnnotation(RhbController.class);
					if(StringUtils.hasText(controller.value())) {
						beanName = controller.value();
					}
					instanceMap.put(CommonUtils.toLowerFirstChar(beanName), cla.newInstance());
				}
				if(cla.isAnnotationPresent(RhbService.class)) {
					RhbService service = (RhbService) cla.getAnnotation(RhbService.class);
					if(StringUtils.hasText(service.value())) {
						beanName = service.value();
					}
					instanceMap.put(CommonUtils.toLowerFirstChar(beanName), cla.newInstance());
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("BasePackage hava Class Reflection is Error!");
			}
		});
		
		System.out.println("Controller|Service-instance-map:"+this.instanceMap);
	}
	
	/**
	 * @Description: 实现SpringIOC-DI的功能，依赖注入@Qualifier（by name）标识的属性
	 * @author: renhuibo
	 * @date:   2019年8月19日 下午4:26:19
	 */
	private void doSpringIOC() {
		Assert.notEmpty(instanceMap, "No instance injection!");
		
		instanceMap.forEach( (x,y) -> {
			Field[] qualifierInstances = y.getClass().getDeclaredFields();
			for(Field field : qualifierInstances) {
				String instanceName = field.getName();
				if(field.isAnnotationPresent(RhbQualifier.class)) {
					RhbQualifier qualifierAnnotation = field.getAnnotation(RhbQualifier.class);
					instanceName = StringUtils.hasText(qualifierAnnotation.value()) ? qualifierAnnotation.value() : instanceName;
					Object instanceValue = instanceMap.get(instanceName);
					
					field.setAccessible(true);
					try {
						System.out.println("fieldName:"+instanceName+"\nfieldValue:"+instanceValue);
						field.set(y, instanceValue);
						System.out.println(y.getClass().getSimpleName()+"-"+field.getName()+",赋值:  "+instanceValue);
					} catch (IllegalArgumentException|IllegalAccessException e) {
						System.out.println("IOC-DI Failure!");
					} 
				}
			}
		});
		
		System.out.println("Spring IOC注入完成...");
	}
	
	/**
	 * @Description: 初始化装载 ： Controller集合、url集合
	 * @author: renhuibo
	 * @date:   2019年8月19日 下午4:32:37
	 */
	@SuppressWarnings("unchecked")
	private void initHandlerMapping() {
		Assert.notEmpty(instanceMap, "No instance injection!");
		
		instanceMap.forEach( (x,y) -> {
			Class curCla = y.getClass();
			if(curCla.isAnnotationPresent(RhbController.class)) {
				String urlPrefix = "";
				String urlMethod = "";
				if(curCla.isAnnotationPresent(RhbRequestMapping.class)) {
					RhbRequestMapping mapping = (RhbRequestMapping) curCla.getAnnotation(RhbRequestMapping.class);
					urlPrefix = mapping.value().endsWith("/") ? mapping.value().substring(0,mapping.value().length()-1) : mapping.value();
				}
				Method[] declaredMethods = curCla.getDeclaredMethods();
				for(Method m : declaredMethods) {
					if(m.isAnnotationPresent(RhbRequestMapping.class)) {
						RhbRequestMapping mapping = m.getAnnotation(RhbRequestMapping.class);
						urlMethod = mapping.value().startsWith("/") ? mapping.value() : "/"+mapping.value();
						
						handlerMaps.put(urlPrefix+urlMethod, m);
						controllerMaps.put(urlPrefix+urlMethod, y);
					}
				}
			}
		});
		
		System.out.println("HandlerMaps:"+handlerMaps);
		System.out.println("ControllerMaps:"+controllerMaps);
		
		System.out.println("HandMapping Init Over...");
	}
	
}
