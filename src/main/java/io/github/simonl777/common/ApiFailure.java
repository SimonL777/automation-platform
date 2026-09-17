package io.github.simonl777.common;
public class ApiFailure extends RuntimeException {
 public final int status; public final String code;
 public ApiFailure(int status,String code){ super(code);this.status=status;this.code=code; }
}
