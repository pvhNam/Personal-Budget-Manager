package vn.sotay.thuchi;

import android.graphics.Bitmap;

/** A small separable blur for downsampled backgrounds, also available on Android 8–11. */
final class SoftBlur {
    static final class Workspace {
        int[] pixels,scratch;
        void ensure(int size){if(pixels==null||pixels.length!=size){pixels=new int[size];scratch=new int[size];}}
        void clear(){pixels=null;scratch=null;}
    }
    static void apply(Bitmap bitmap,int radius){apply(bitmap,radius,new Workspace(),1);}
    static void apply(Bitmap bitmap,int radius,Workspace work,int iterations) {
        int w=bitmap.getWidth(),h=bitmap.getHeight();
        work.ensure(w*h);int[] src=work.pixels,tmp=work.scratch;bitmap.getPixels(src,0,w,0,0,w,h);
        for(int iteration=0;iteration<iterations;iteration++) {
        for(int pass=0;pass<2;pass++) {
            int lines=pass==0?h:w,length=pass==0?w:h;
            for(int line=0;line<lines;line++) {
                int a=0,r=0,g=0,b=0;
                for(int j=-radius;j<=radius;j++) {
                    int p=src[index(pass,line,Math.max(0,Math.min(length-1,j)),w)];
                    a+=p>>>24;r+=(p>>16)&255;g+=(p>>8)&255;b+=p&255;
                }
                int n=radius*2+1;
                for(int x=0;x<length;x++) {
                    tmp[index(pass,line,x,w)]=(a/n<<24)|(r/n<<16)|(g/n<<8)|b/n;
                    int old=src[index(pass,line,Math.max(0,x-radius),w)];
                    int next=src[index(pass,line,Math.min(length-1,x+radius+1),w)];
                    a+=(next>>>24)-(old>>>24);r+=((next>>16)&255)-((old>>16)&255);
                    g+=((next>>8)&255)-((old>>8)&255);b+=(next&255)-(old&255);
                }
            }
            int[] swap=src;src=tmp;tmp=swap;
        }
        }
        bitmap.setPixels(src,0,w,0,0,w,h);
    }
    private static int index(int pass,int line,int x,int width){return pass==0?line*width+x:x*width+line;}
}
