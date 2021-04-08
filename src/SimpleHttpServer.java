import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class SimpleHttpServer 
{
    public static void main(String[] args) throws Exception {
        start();
    }
    //处理HttpRequest的线程池
    static ThreadPool threadPool = new ThreadPool(2, 2000l,TimeUnit.MILLISECONDS,5,(queue,task)->{
        task.run();
    });
    //根目录
    static String basePath="C:/Users/22326/Desktop/HttpServer/html";

    static ServerSocket serverSocket;
    //监听端口
    static int port = 8080;
    public static void setPort(int port)
    {
        if (port > 0)
        {
            SimpleHttpServer.port = port;
        }
    }

    public static void setBasePath(String basePath)
    {
        if (basePath != null)
        {
            File f = new File(basePath);
            if (f.exists() && f.isDirectory())
                SimpleHttpServer.basePath = basePath;
        }
    }
    //启动
    public static void start() throws Exception
    {
        serverSocket = new ServerSocket(port);
        Socket socket = null;
        while ((socket = serverSocket.accept()) != null) 
        {
            //接受一个客户端socket，封装成HttpRequest放入线程池
            threadPool.execute(new HttpRequestHandler(socket));

        }
        serverSocket.close();
    }
    //HttpRequest需实现Runnable
    static class HttpRequestHandler implements Runnable 
    {
        private Socket socket;

        public HttpRequestHandler(Socket socket)
        {
            this.socket = socket;
        }

        @Override
        public void run() 
        {
            String line = null;
            BufferedReader br = null;   //字符输入流，读取静态资源文件字符流（html）
            BufferedReader reader = null; //字符输入流，读取请求的字符流
            PrintWriter out = null; //字符输出流，输出响应内容
            InputStream in = null;  //文件字节输入流，读取静态资源文件（图片）
            try 
            {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String header = reader.readLine();
                //由相对路径计算出绝对路径
                String filePath = basePath + header.split(" ")[1];
                out = new PrintWriter(socket.getOutputStream());
                //后缀为jpg or ico
                if (filePath.endsWith("jpg") || filePath.endsWith("ico") || filePath.endsWith("png"))
                {
                    System.out.println("图片");
                    in = new FileInputStream(filePath);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int i = 0;
                    while ((i = in.read()) != -1) 
                    {
                        baos.write(i);    
                    }
                    byte[] array = baos.toByteArray();
                    //HTTP状态行、响应头
                    out.println("HTTP/1.1 200 OK");
                    out.println("Server: Molly");
                    out.println("Content-Type: image/jpeg");
                    out.println("Content-Length: " + array.length);
                    out.println("");
                    socket.getOutputStream().write(array, 0, array.length);                    
                }
                else
                {
                    System.out.println("html");
                    br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));
                    out = new PrintWriter(socket.getOutputStream());
                    out.println("HTTP/1.1 200 OK");
                    out.println("Server: Molly");
                    out.println("Content-Type: text/html; charset = UTF-8");
                    out.println("");
                    while ((line = br.readLine()) != null) 
                    {
                        out.println(line);
                    }
                }
                out.flush();

            }catch (FileNotFoundException e){
                System.out.println("没有该资源");
                out.println("HTTP/1.1 404");
                out.println("Server: Molly");
                out.println("Content-Type:text/html;charset=UTF-8");
                out.println();
                out.println("<h2>您访问的资源跑丢啦</h2>");
                out.flush();
            }
            catch (Exception e) {
                out.println("HTTP/1.1 500");
                out.println("Content-Type:text/html;charset=UTF-8");
                out.println();
                out.println("服务器出错啦");
                out.flush();
                e.printStackTrace();
            } finally {
                close(br, in, reader, out, socket);
            }
        }              
    }

    private static void close(Closeable... closeables) 
    {
        if (closeables != null)
        {
            for (Closeable closeable : closeables)
            {
                try 
                {
                    closeable.close();    
                } 
                catch (Exception e) 
                {
                }
            }
        }
    }

}


//一个基于线程池技术的简单Web服务器
/*
我先写了一个简单的线程池，有execute，shutdown，addWorker，removeWorker方法。
使用这个线程池构造了一个简单的Web服务器，用于处理HTTP请求，目前只能处理简单的文本和JPG图片。
使用main线程不断接受客户端Socket的连接，封装成HttpRequestHandler交线程池处理。
*/