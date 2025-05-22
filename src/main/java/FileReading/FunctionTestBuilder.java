package FileReading;

import TestObjects.FunctionTest;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


public class FunctionTestBuilder {
    private final JSONArray jsonArray;
    private static final Map<String,Class<?>> PRIMITIVES = Map.of(
            "byte",    byte.class,
            "short",   short.class,
            "int",     int.class,
            "long",    long.class,
            "float",   float.class,
            "double",  double.class,
            "boolean", boolean.class,
            "char",    char.class
    );

    public FunctionTestBuilder(String resourcePath){
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            System.err.println("Resource not found: " + resourcePath);
            System.exit(1);
        }

        String JSONString;
        try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
            JSONString = scanner.useDelimiter("\\A").next();
        }
        jsonArray = new JSONObject(JSONString).getJSONArray("functionTests");
    }
    public JSONArray getJsonArray() {
        return jsonArray;
    }

    public FunctionTest[] buildFunctionTests(){
        FunctionTest[] functionTests = new FunctionTest[jsonArray.length()];
        for(int i = 0; i < jsonArray.length(); i++){
            JSONObject obj = jsonArray.getJSONObject(i);
            String className = obj.getString("className");
            String methodName = obj.getString("methodName");
            Class<?>[] paramTypes = null;
            try {
                 paramTypes = readParamTypes(obj.getJSONArray("paramTypes"));
            } catch (ClassNotFoundException e){
                System.err.println("Invalid parameter type at: ");
                System.err.println(obj);
                System.exit(2);
            }
            Object[] args = readArgs(obj.getJSONArray("args"), paramTypes);
            Object expected = obj.get("expected");
            functionTests[i] = new FunctionTest(className, methodName, paramTypes, args, expected);
        }
        return functionTests;
    }

    private Class<?>[] readParamTypes(JSONArray array) throws ClassNotFoundException {
        Class<?>[] paramTypes = new Class<?>[array.length()];
        for(int i = 0; i < array.length(); i++){
            String param = array.getString(i).trim();

            if(param.endsWith("[]")){
                String paramName = param.substring(0, param.length() - 2);
                Class<?> paramType;

                if(PRIMITIVES.containsKey(paramName)){
                    paramType = PRIMITIVES.get(paramName);
                }
                else{
                    paramType = Class.forName(paramName);
                }
                paramTypes[i] = Array.newInstance(paramType, 0).getClass();
            }
            else{
                if(PRIMITIVES.containsKey(param)){
                    paramTypes[i] = PRIMITIVES.get(param);
                }
                else{
                    paramTypes[i] = Class.forName(param);
                }
            }
        }
        return paramTypes;
    }
    private Object[] readArgs(JSONArray argsJson, Class<?>[] paramTypes) {
        Object[] args = new Object[paramTypes.length];
        for (int j = 0; j < paramTypes.length; j++) {
            Class<?> pt = paramTypes[j];
            Object raw  = argsJson.get(j);

            if (pt.isArray()) {
                if (pt.equals(int[].class)) {
                    JSONArray arr = (JSONArray) raw;
                    int[] ia = new int[arr.length()];
                    for (int k = 0; k < arr.length(); k++) {
                        ia[k] = arr.getInt(k);
                    }
                    args[j] = ia;
                } else {
                    List<?> list = ((JSONArray) raw).toList();
                    Object[] oa = (Object[]) Array
                            .newInstance(pt.getComponentType(), list.size());
                    for (int k = 0; k < list.size(); k++) {
                        oa[k] = list.get(k);
                    }
                    args[j] = oa;
                }

            } else if (pt.equals(int.class) || pt.equals(Integer.class)) {
                args[j] = ((Number) raw).intValue();

            } else if (pt.equals(long.class) || pt.equals(Long.class)) {
                args[j] = ((Number) raw).longValue();

            } else if (pt.equals(double.class) || pt.equals(Double.class)) {
                args[j] = ((Number) raw).doubleValue();

            } else if (pt.equals(boolean.class) || pt.equals(Boolean.class)) {
                args[j] = raw;

            } else if (pt.equals(String.class)) {
                args[j] = raw.toString();

            } else {
                args[j] = raw;
            }
        }
        return args;
    }
}
