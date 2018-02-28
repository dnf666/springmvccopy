package com.ims.mvcframework.servlet;

import com.ims.mvcframework.annotation.*;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author demo
 */
public class DispatcherServlet extends HttpServlet {
    private Properties p = new Properties();
    private List<String> classNames = new ArrayList<String>();
    private Map<String, Object> ioc = new HashMap<String, Object>();
    private List<Handler> handlermapping = new ArrayList<Handler>();
    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        //1.加载配置文件
        doLoadXml(servletConfig.getInitParameter("contextapplication")
        );
        //2.扫描到所有相关的类
        doSanner(p.getProperty("scanpackage"));
        //3.把扫描到的类实例化，并且加入到ioc容器
        doInstance();
        //4.检查注入
        doIoc();
        //5.获取用户的请求，根据用户的请求找到对应的方法。反射
        doHandleMapping();
        //handlemapping 用handlemapping 保存关系
        //等待请求，反射将结果返回
    }
//只能requestmap（"web"）这种
    private void doHandleMapping() {
        String baseUrl = "";
        if(ioc.isEmpty()){return;}
        for(Map.Entry<String,Object> entry :ioc.entrySet()){
            Class clazz = entry.getValue().getClass();
            if(clazz.isAnnotationPresent(Controller.class))
            {
                RequestMapping requestMapping = (RequestMapping) clazz.getAnnotation(RequestMapping.class);
                 baseUrl = requestMapping.value();
            Method[] methods = clazz.getMethods();
            for(Method method:methods){
               if( method.isAnnotationPresent(RequestMapping.class)){
                    String mapping = method.getAnnotation(RequestMapping.class).value();
                    mapping ="/" + baseUrl +"/"+ mapping.replaceAll("/+","/");
                    Pattern p = Pattern.compile(mapping);
                   try {
                       handlermapping.add(new Handler(entry.getValue(),method,p));
                   } catch (Exception e) {
                       e.printStackTrace();
                   }
               }
            }
        }}
    }

    private void doIoc() {
        if (ioc.isEmpty()) {
            return;
        }
        //开始注入
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(Auwired.class)) {
                    continue;
                }
                //如果有值
                Auwired auwired = field.getAnnotation(Auwired.class);
                //获取值时请注意去掉两端空格
                String beanName = auwired.value().trim();
                //如果没值
                if ("".equals(beanName)) {
                    beanName = field.getType().getName().trim();
                }
                field.setAccessible(true);
                Object entryValue= null;
                try {
                    entryValue = entry.getValue();
                    Object object = ioc.get(beanName);
                    field.set(entry.getValue(),ioc.get(beanName) );
                    System.out.println(field.toString());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

            }
        }

    }

    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }
        try {
            for (String classname : classNames) {
                Class<?> clazz = Class.forName(classname);
                //初始化需要实例化的类
                if (clazz.isAnnotationPresent(Controller.class)) {
                    //如果自定义了id，就使用id，如果没有，默认首字母小写
                    Controller controller = clazz.getAnnotation(Controller.class);
                    String name = controller.value();
                    if (!name.equals("")) {
                        ioc.put(name, clazz.newInstance());
                    } else {
                        //gaigai
                        String beanName = lowerFirst(clazz.getSimpleName());
                        ioc.put(beanName, clazz.newInstance());
                    }

                } else if (clazz.isAnnotationPresent(Service.class)) {
                    //无接口实现
                    Service service = clazz.getAnnotation(Service.class);
                    String name = service.value();
                    if (!name.equals("")) {
                        ioc.put(name, clazz.newInstance());
                    } else {
                        String beanName = clazz.getName();
                        ioc.put(beanName, clazz.newInstance());
                    }
                    //如果有接口实现怎么办
                    Class<?>[] classes = clazz.getInterfaces();
                    for (Class cla : classes) {
                        ioc.put(cla.getName(), cla.newInstance());
                    }
                } else {
                    continue;
                }


            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String lowerFirst(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    private void doSanner(String scanpackage) {
        URL url = this.getClass().getClassLoader().getResource("/" + scanpackage.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                //递归查找
                doSanner(scanpackage + "." + file.getName());
            } else {
                //将找到的类名保存下来
                classNames.add(scanpackage + "." + file.getName().replace(".class", ""));
            }
        }
    }

    private void doLoadXml(String contextapplication) {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(contextapplication);
        try {
            p.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doDispatch(request,response);
    }

    private void doDispatch(HttpServletRequest request, HttpServletResponse response) {
       String uri = request.getRequestURI();
        System.out.println("uri:"+uri);
        String context = request.getContextPath();
       uri = uri.replace(context,"").replaceAll("/+","/");
      Handler handler = getHandler(uri);
      Class<?>[] paramTypes = handler.method.getParameterTypes();
      Object[] paramValues = new Object[paramTypes.length];
      Map<String, String[]> map = request.getParameterMap();
      for(Map.Entry<String,String[]> entry : map.entrySet()){
         String value = Arrays.toString(entry.getValue()).replaceAll("\\[|\\]","").replaceAll(",\\s",",");
         if(handler.paramIndexMapping.containsKey(entry.getKey())){
             int index = handler.paramIndexMapping.get(entry.getKey());
             paramValues[index] = covert(paramTypes[index],value);
         }
        int reqIndex = handler.paramIndexMapping.get(HttpServletRequest.class.getName());
         paramValues[reqIndex] = request;
          int resIndex = handler.paramIndexMapping.get(HttpServletResponse.class.getName());
          paramValues[resIndex] = response;

      }
      //没有匹配到就返回错误
      if(handler==null){
          try {
              response.getWriter().write("404");
          } catch (IOException e) {
              e.printStackTrace();
          }
      }
      //不等于null就执行
        try {
            handler.method.invoke(handler.controller,paramValues);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private Object covert(Class<?> paramType, String value) {
        if(Integer.class == paramType)
        {
            return Integer.valueOf(value);
        }
        return value;
    }

    private Handler getHandler(String uri) {
        for (Handler handler : handlermapping) {
            Matcher matcher = handler.pattern.matcher(uri);
            if (!matcher.matches()) {
                continue;
            } else {
                return handler;
            }
        }
        return null;

    }

    /**
     * handlermapping和controller的关系
     */
    private class Handler{
        protected Object controller;//方法的对象
        protected Method method;//方法
        protected Pattern pattern;//正则表达式
        protected Map<String,Integer> paramIndexMapping;//方法参数顺序

        protected Handler(Object controller, Method method, Pattern pattern) {
            this.controller = controller;
            this.method = method;
            this.pattern = pattern;
            paramIndexMapping = new HashMap<String, Integer>();
            putParamIndexMapping(method);
        }
        //获取加了注解的方法参数
        private void putParamIndexMapping(Method method) {
            int j =0;
         Parameter[] parameters = method.getParameters();
         Parameter[] annotationParameters = new Parameter[10];
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i].isAnnotationPresent(RequestParam.class)){
                    annotationParameters[j] = parameters[i];
                    j++;
                }
            }
           Annotation[][] annotations = method.getParameterAnnotations();
           for(int i = 0; i < annotations.length; i++)
           {
               for (Annotation a: annotations[i]
                    ) {
                   if(a instanceof RequestParam){
                        String requestParam = ((RequestParam) a).value();
                        //如果定义了值
                        if(!"".equals(requestParam.trim())){
                            paramIndexMapping.put(requestParam,i);
                            //如果没有定义值
                        }else {
                            paramIndexMapping.put(parameters[i].getName(),i);
                        }
                   }
               }
           }
           Class<?>[] parameterTypes = method.getParameterTypes();
            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> type = parameterTypes[i];
                if(type == HttpServletRequest.class || type == HttpServletResponse.class){
                    paramIndexMapping.put(type.getName(),i);

                }
            }
        }
    }
}
