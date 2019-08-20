package com.rhb.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rhb.annotation.RhbController;
import com.rhb.annotation.RhbQualifier;
import com.rhb.annotation.RhbRequestMapping;
import com.rhb.annotation.RhbRequestParam;
import com.rhb.service.HelloService;

@RhbController
@RhbRequestMapping("/test")
public class TestController {
	
	@RhbQualifier
	HelloService helloService;
	
	@RhbRequestMapping("/sayHello")
	public void test() {
		helloService.sayHello();
	}
	
	@RhbRequestMapping("/say")
	public String test2(@RhbRequestParam(value="a")String sasd,@RhbRequestParam(value="b") Integer b,@RhbRequestParam(value="c")Double c,@RhbRequestParam(value="d")Date d) {
		return helloService.doSay(sasd, b, c, d);
	}
	
	@RhbRequestMapping("/respTest1")
	public Map<String, String> test3() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("one", "BeiJing");
		map.put("two", "ShangHai");
		map.put("three", "GuangZhou");
		return map;
	}
	
	@RhbRequestMapping("/respTest2")
	public List<String> test4() {
		List<String> list = new ArrayList<String>();
		list.add("BeiJing");
		list.add("ShangHai");
		list.add("GuangZhou");
		return list;
	}
}
