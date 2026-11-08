package cn.local.nufeschedule;

import java.io.*;

/** Byte-based progress for both APK and JSON backup responses. */
final class UpdateTransfer {
 interface Progress { void bytes(long received,long total); }
 static long copy(InputStream in,OutputStream out,long expected,long limit,Progress progress)throws IOException{
  long received=0;byte[] buffer=new byte[32768];int count;
  progress.bytes(0,expected);
  while((count=in.read(buffer))!=-1){received+=count;if(received>limit||(expected>0&&received>expected))throw new IOException("下载大小异常");out.write(buffer,0,count);progress.bytes(received,expected);}
  if(expected>0&&received!=expected)throw new IOException("下载不完整，请重试");
  return received;
 }
 static int percent(long received,long total){return total>0?(int)Math.min(100,received*100/total):-1;}
}
