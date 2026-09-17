package io.github.simonl777.common;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
public final class Json {
 public static final ObjectMapper MAPPER=new ObjectMapper();
 public static String write(Object value){try{return MAPPER.writeValueAsString(value);}catch(Exception e){throw new IllegalArgumentException("JSON_SERIALIZATION",e);}}
 public static JsonNode read(String value){try{return MAPPER.readTree(value);}catch(Exception e){throw new IllegalArgumentException("INVALID_JSON",e);}}
 public static <T>T decode(String value,Class<T> type){try{return MAPPER.readValue(value,type);}catch(Exception e){throw new IllegalArgumentException("INVALID_JSON",e);}}
 public static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 public static String canonical(Object value){return write(sorted(MAPPER.valueToTree(value)));}
 private static JsonNode sorted(JsonNode n){
  if(n.isObject()){ObjectNode out=MAPPER.createObjectNode();List<String> keys=new ArrayList<>();n.fieldNames().forEachRemaining(keys::add);Collections.sort(keys);for(String k:keys)out.set(k,sorted(n.get(k)));return out;}
  if(n.isArray()){ArrayNode out=MAPPER.createArrayNode();for(JsonNode item:n)out.add(sorted(item));return out;}return n;
 }
}
