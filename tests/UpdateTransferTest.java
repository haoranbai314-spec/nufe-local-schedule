package cn.local.nufeschedule;
import java.io.*;
import java.util.*;
public final class UpdateTransferTest {
 static void check(boolean ok){if(!ok)throw new AssertionError();}
 public static void main(String[] args)throws Exception{
  byte[] input=new byte[100000];new Random(42).nextBytes(input);ByteArrayOutputStream result=new ByteArrayOutputStream();List<Long> positions=new ArrayList<>();
  check(UpdateTransfer.copy(new ByteArrayInputStream(input),result,input.length,200000,(n,total)->positions.add(n))==input.length);
  check(Arrays.equals(input,result.toByteArray()));check(positions.get(0)==0&&positions.get(positions.size()-1)==100000);check(positions.size()>3);
  for(int i=1;i<positions.size();i++)check(positions.get(i)>=positions.get(i-1));
  check(UpdateTransfer.percent(50000,100000)==50);check(UpdateTransfer.percent(0,-1)==-1);
  result.reset();UpdateTransfer.copy(new ByteArrayInputStream(input),result,-1,200000,(n,total)->check(total==-1));check(Arrays.equals(input,result.toByteArray()));
  try{UpdateTransfer.copy(new ByteArrayInputStream(new byte[4]),new ByteArrayOutputStream(),10,10,(n,total)->{});throw new AssertionError("Truncation accepted");}catch(IOException expected){}
  try{UpdateTransfer.copy(new ByteArrayInputStream(new byte[11]),new ByteArrayOutputStream(),10,10,(n,total)->{});throw new AssertionError("Oversize accepted");}catch(IOException expected){}
  System.out.println("Update transfer regression checks passed");
 }
}
