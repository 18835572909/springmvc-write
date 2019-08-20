package com.rhb.common;

import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CommonUtils {
	
	/**
	 * @Description: 获取资源路径下xml中属性值
	 * @author: renhuibo
	 * @date:   2019年8月19日 下午3:30:38
	 */
	public static String getBaseScanPackageName(String sourcePath,String attributeName) throws Exception {
		Assert.hasText(sourcePath, "资源文件名不为空");
		if(!StringUtils.startsWithIgnoreCase(sourcePath,"classpath:")) {
			throw new Exception("资源路径以\"classpath:\"开头");
		}
		
		File file = ResourceUtils.getFile(sourcePath);
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(file);
		
		Element root = doc.getDocumentElement();
		NodeList childNodes = root.getChildNodes();
		for(int i=0;i<childNodes.getLength();i++) {
			Node node = childNodes.item(i);
			if (node instanceof Element) {
                Element element = (Element) node;
	            System.out.println(element.getTagName());
                String attributeValue = element.getAttribute(attributeName);
                if (attributeValue != null || "".equals(attributeValue.trim())) {
                    return attributeValue.trim();
                }
            }
		}
		return "";
	}
	
	/**
	 * @Description: 原本点状路径转斜杠路径 eg: com.cc -> com/cc
	 * @author: renhuibo
	 * @date:   2019年8月19日 下午3:31:03
	 */
	public static String exchangePointToPath(String srcPath){
		Assert.hasText(srcPath, "空串不能转换");
		return srcPath.replaceAll("\\.", "/");
	}
	
	/**
	 * @Description: 首字母小写   eg: ASdsad -> aSdsad
	 * @author: renhuibo
	 * @date:   2019年8月19日 下午3:32:08
	 */
	public static String toLowerFirstChar(String src) {
		Assert.hasText(src, "空串不能转换");
		char first = src.charAt(0);
		return src.replace(first,(char) (first+32));
	}
	
}
