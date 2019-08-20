package com.rhb.service.impl;

import java.util.Date;

import com.rhb.annotation.RhbService;
import com.rhb.service.HelloService;

@RhbService("HelloService")
public class HelloServiceImpl implements HelloService {

	@Override
	public void sayHello() {
		System.out.println("Hello!");
	}

	@Override
	public String doSay(String say,int n,double d,Date date) {
		return say+"-"+n+"-"+d+"-"+date;
	}

}
