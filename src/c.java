package src;
import java.nio.file.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import javax.net.ssl.HttpsURLConnection;
import java.net.*;
import javax.net.ssl.*;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.OutputStream; 

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.regex.*;  
class c
{

	static TrustManager[] trustAllCerts = new TrustManager[]{
			new X509TrustManager() {

				public java.security.cert.X509Certificate[] getAcceptedIssuers()
				{
					return null;
				}
				public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType)
				{
					//No need to implement.
				}
				public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType)
				{
					//No need to implement.
				}
			}
	};
	
	static java.util.HashSet alltld = new java.util.HashSet(5000);
	static int firstwrite = 1;
	static int firstwriteNA = 1;
	
	public static final String CONTENT_ENCODING_GZIP = "gzip";
	static
	{
		try
		{
			BufferedReader br = new BufferedReader(new FileReader("./config/tld.txt"));
			String line = br.readLine();
			while(line != null)
			{
				line = line.toLowerCase().trim();
				if(!line.equals("") && !line.startsWith("//")) 
					alltld.add(line);
				line = br.readLine();
			}
			System.out.println("Total Tld = "+alltld.size());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}

	public static void main(String a[])
	{
			BufferedReader br = null;
			if(a.length == 0)
			{
				System.out.println("Usage : java c <filename>");
				System.exit(0);
			}
			System.out.println("File name = "+a[0]);
			try
			{
				Path path = Paths.get(a[0]);

				br = Files.newBufferedReader(path, StandardCharsets.UTF_8);
				String line = br.readLine();
				int i = 0;
				while(line != null)
				{
					if(i > 0)
					{

						System.out.println("------------------------------------------------- "+i+" -----------------------------------------------------------------");
						
						System.out.println(line);							

						// callURL(line,0);
						
						callingURL(line,line,0);

						
						//if(i > 4)
						//	break;
					}

					line = br.readLine();
					i++;
				}

			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
	}
    


	public  static String callingURL(String publisher,String url,int noofredirect) throws Exception {
        HttpURLConnection http = null;
        OutputStreamWriter wr = null;
        BufferedReader rd = null;
        StringBuilder sb = null;
        String line = null;
        URL serverAddress = null;
        try {
           
            //set up out communications stuff
            http = null;
            //Set up the initial connection

			if (url.startsWith("\""))
			{
				url = url.replaceAll("\"","");
			}
			url = url.toLowerCase();

            if (!url.startsWith("http://") && !url.startsWith("https://"))
				url = "http://"+url;
			
			System.out.println("calling of url : " + url);

			 serverAddress = new URL(url);

            if (url.startsWith("https://")) {

				try 
				{
					SSLContext sc = SSLContext.getInstance("SSL");
					sc.init(null, trustAllCerts, new java.security.SecureRandom());
					HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
				} 
				catch (Exception e) 
				{
					System.out.println(e);
				}

                http = (HttpsURLConnection) serverAddress.openConnection();
            } else {
                http = (HttpURLConnection) serverAddress.openConnection();
            }

            http.setRequestMethod("GET");
			http.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
            //http.setDoOutput(true);
			//http.setRequestProperty("Accept-Encoding", "gzip");
            http.setConnectTimeout(50000);
            http.setReadTimeout(50000);
            http.connect();
           
			int resCode = http.getResponseCode();

			 if (resCode == HttpURLConnection.HTTP_SEE_OTHER
                || resCode == HttpURLConnection.HTTP_MOVED_PERM
                || resCode == HttpURLConnection.HTTP_MOVED_TEMP) {
            String Location = http.getHeaderField("Location");

			System.out.println( "Redirect Location="+Location );
            if (Location.startsWith("/")) {
                Location = serverAddress.getProtocol() + "://" + serverAddress.getHost() + Location;
            }
            return callingURL(publisher,Location,noofredirect + 1);

			 }

			


			String contentEncoding = http.getContentEncoding();
			System.out.println( "contentEncoding==="+contentEncoding );
			if (contentEncoding != null && CONTENT_ENCODING_GZIP.equalsIgnoreCase(contentEncoding)) {
				InputStream inStream = new GZIPInputStream(http.getInputStream());
				rd = new BufferedReader(new InputStreamReader(inStream));
			} else {
				rd = new BufferedReader(new InputStreamReader(http.getInputStream()));
			}

			
			

            sb = new StringBuilder();
			int isfound = 0;
			int found = 0;
            while ((line = rd.readLine()) != null) {
				
				//System.out.println(line );
               	if(line.indexOf("appstore:developer_url") != -1)
				{

					String devurl[] =  extractDevUrl(line);
					System.out.println("devurl === "+devurl);
					isfound = 1;
					
					if(devurl != null)
					{
						int di = 0;
						while(di < devurl.length)
						{
							found = callingFinalURL(publisher,devurl[di]+"/app-ads.txt");
							if(found == 1)							
								break;
							
							di++;
						}
					}
					else
						isfound = 0;

					
					
					break;
				}
            }
			if(isfound == 0)
				writeNAData(publisher+"  -   developer_url notfound");
			else if(found == 0)
				writeNAData(publisher+"  -   app-ads.txt notfound");
            //System.out.println("response of url : " + sb.toString());
			//if(sb.toString().indexOf("<h1>301 Moved Permanently</h1>") != -1)

			return null;
        }
		catch(Exception e)
		{
			writeNAData(publisher+"  -   Exception in dev_url"+e.toString());
			e.printStackTrace();
			return null;
		}
		finally {
				//close the connection, set all objects to null
			try
			{
				rd.close();
			}
			catch (Exception e)
			{
			}
			try
			{
				 http.disconnect();
			}
			catch (Exception e)
			{
			}
           
            rd = null;
            sb = null;
            wr = null;
            http = null;
        }

    } 


	public  static int callingFinalURL(String publisher,String url) throws Exception {
        HttpURLConnection http = null;
        OutputStreamWriter wr = null;
        BufferedReader rd = null;
        StringBuilder sb = null;
        String line = null;
        URL serverAddress = null;
        try {
           
            //set up out communications stuff
            http = null;
            //Set up the initial connection

			if (url.startsWith("\""))
			{
				url = url.replaceAll("\"","");
			}
			url = url.toLowerCase();

            if (!url.startsWith("http://") && !url.startsWith("https://"))
				url = "http://"+url;
			
			System.out.println("calling of url : " + url);

			 serverAddress = new URL(url);

            if (url.startsWith("https://")) {

				try 
				{
					SSLContext sc = SSLContext.getInstance("SSL");
					sc.init(null, trustAllCerts, new java.security.SecureRandom());
					HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
				} 
				catch (Exception e) 
				{
					System.out.println(e);
				}

                http = (HttpsURLConnection) serverAddress.openConnection();
            } else {
                http = (HttpURLConnection) serverAddress.openConnection();
            }

            http.setRequestMethod("GET");
			http.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
            //http.setDoOutput(true);
			http.setRequestProperty("Accept-Encoding", "gzip");
            http.setConnectTimeout(50000);
            http.setReadTimeout(50000);
            http.connect();
           
			int resCode = http.getResponseCode();

			 if (resCode == HttpURLConnection.HTTP_SEE_OTHER
                || resCode == HttpURLConnection.HTTP_MOVED_PERM
                || resCode == HttpURLConnection.HTTP_MOVED_TEMP) {
            String Location = http.getHeaderField("Location");

			System.out.println( "Redirect Location="+Location );
            if (Location.startsWith("/")) {
                Location = serverAddress.getProtocol() + "://" + serverAddress.getHost() + Location;
            }
            return callingFinalURL(publisher,Location);

			 }

			//System.out.println( responseCode );
			//String location = con.getHeaderField( "Location" );
			//System.out.println( location );

            //rd = new BufferedReader(new InputStreamReader(http.getInputStream()));
			
			String contentEncoding = http.getContentEncoding();
			System.out.println( "contentEncoding==="+contentEncoding );
			if (contentEncoding != null && CONTENT_ENCODING_GZIP.equalsIgnoreCase(contentEncoding)) {
				InputStream inStream = new GZIPInputStream(http.getInputStream());
				rd = new BufferedReader(new InputStreamReader(inStream));
			} else {
				rd = new BufferedReader(new InputStreamReader(http.getInputStream()));
			}

            sb = new StringBuilder();

			JSONObject temp = null;
			JSONObject finaldata = new JSONObject();
			JSONArray data = new JSONArray();
            while ((line = rd.readLine()) != null) {
				
				line = line.trim();
				//System.out.println(line );
				
				if(!line.startsWith("#") && !line.equals("") && line.indexOf(",") != -1)
				{
					temp = new JSONObject();
					line = line.replaceAll(" ",",");
					while(line.indexOf(",,") != -1)
						line = line.replaceAll(",,",",");
					String arr[] = line.split("[,\\s]");
					if(arr.length > 2)
					{
						temp.put("domain",arr[0].trim());
						temp.put("publisherAccountId",arr[1].trim());
						temp.put("typeOfAccount",arr[2].trim());

						if(arr.length > 3)
							temp.put("authorityID",arr[3].trim());

						//System.out.println("interdata : " + temp.toString());
						data.put(temp);
					}
					//"domain":"google.com","publisherAccountId":" pub-1991679624331369","typeOfAccount":" DIRECT","authorityID":" f08c47fec0942fa0"
				}
               
            }
			finaldata.put("publisherURL",publisher.replaceAll("\"",""));
			finaldata.put("contentList",data);
            //System.out.println("finaldata : " + finaldata.toString());
			writeData( finaldata.toString());
			//if(sb.toString().indexOf("<h1>301 Moved Permanently</h1>") != -1)

			return 1;
        }
		catch(Exception e)
		{
			//writeNAData( publisher+"   -  Exception in ads.txt"+e.toString());
			e.printStackTrace();
			return 0;
		}
		finally {
				//close the connection, set all objects to null
			try
			{
				rd.close();
			}
			catch (Exception e)
			{
			}
			try
			{
				 http.disconnect();
			}
			catch (Exception e)
			{
			}
           
            rd = null;
            sb = null;
            wr = null;
            http = null;
        }

    } 




	static String extractDevUrl_old(String str)
	{
		String strarr[] = str.split("[\\s<>]");
		try
		{
			int si = 0;
			while(si < strarr.length)
			{
				String tstr = strarr[si];
				if(tstr.toLowerCase().indexOf("content=") != -1)
				{
					tstr = tstr.substring(8);
					tstr = tstr.replaceAll("\"","").replaceAll(">","");
					
					URL serverAddress = new URL(tstr);
					String host = serverAddress.getHost();
					String protocol = serverAddress.getProtocol();
					
					System.out.println(protocol +"   "+host);
					while(host.indexOf(".") != -1)
					{
						String f1 = host.substring(0,host.indexOf("."));
						String f2 = host.substring(host.indexOf(".") + 1);
						System.out.println(f1 +"   "+f2);

						if(alltld.contains(f2))
						{
							return protocol+"://"+f1+"."+f2;
						}
						
						host = f2;
					}
					
					return tstr;
				}
				si++;
			}
			return null;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	static String[] extractDevUrl(String str)
	{
		Pattern p = Pattern.compile("<(.*?)>");
		Matcher m = p.matcher(str);
		String astr = null;
		while(m.find()) {
			if(m.group(0).indexOf("appstore:developer_url") != -1)
			{
			   System.out.println(m.group(0));
			   System.out.println(m.group(1));
			   astr = m.group(0);
			   break;
			}
		}

		String strarr[] = astr.split("[\\s<>]");
		try
		{
			int si = 0;
			while(si < strarr.length)
			{
				String tstr = strarr[si];
				if(tstr.toLowerCase().indexOf("content=") != -1)
				{
					tstr = tstr.substring(8);
					tstr = tstr.replaceAll("\"","").replaceAll(">","");
					
					URL serverAddress = new URL(tstr);
					String host = serverAddress.getHost();
					String protocol = serverAddress.getProtocol();
					
					System.out.println(protocol +"   "+host);
					String previous = null;
					String adsurl[] = null;
					while(host.indexOf(".") != -1)
					{
						String f1 = host.substring(0,host.indexOf("."));
						String f2 = host.substring(host.indexOf(".") + 1);
						System.out.println(f1 +"   "+f2);

						if(alltld.contains(f2))
						{
							
							if(previous != null && !previous.equals("www") && !previous.equals("m"))
							{
								adsurl = new String[2];
								adsurl[0] = protocol+"://"+f1+"."+f2;
								adsurl[1] = protocol+"://"+previous+"."+f1+"."+f2;
							}
							else
							{
								adsurl = new String[1];
								adsurl[0] = protocol+"://"+f1+"."+f2;
							}

							//return protocol+"://"+f1+"."+f2;
							return adsurl;
						}
						previous = f1.toLowerCase();
						host = f2;
					}
					
					adsurl = new String[1];
					adsurl[0] = protocol+"://"+host;
					return adsurl;
				}
				si++;
			}
			return null;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	public static void writeData(String data)
	{
		if(firstwrite == 1)
		{
			File f = new File("./output/output.txt");
			f.delete();
			firstwrite = 0;
		}
		
		Path path = Paths.get("./output/output.txt");
		try(BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                         StandardOpenOption.WRITE, StandardOpenOption.APPEND)){
		writer.write(data+"\r\n");



		}catch(IOException ex){
		ex.printStackTrace();
		}

	}

	public static void writeNAData(String data)
	{
		if(firstwriteNA == 1)
		{
			File f = new File("./output/outputNA.txt");
			f.delete();
			firstwriteNA = 0;
		}
		
		Path path = Paths.get("./output/outputNA.txt");
		try(BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                         StandardOpenOption.WRITE, StandardOpenOption.APPEND)){
		writer.write(data+"\r\n");



		}catch(IOException ex){
		ex.printStackTrace();
		}

	}
}