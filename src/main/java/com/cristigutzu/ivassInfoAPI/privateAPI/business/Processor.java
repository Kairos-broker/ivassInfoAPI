package com.cristigutzu.ivassInfoAPI.privateAPI.business;

import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Processor {
    Map<String, String[]> data;

    String id;

    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Processor processor = (Processor)o;
        return Objects.equals(this.id, processor.id);
    }

    public int hashCode() {
        return Objects.hash(new Object[] { this.id });
    }

    public RequestBuilder buildRequest(String url) {
        RequestBuilder request = RequestBuilder.get().setUri(url).setHeader("Content-Type", "application/json");
        return request;
    }

    public Processor(String resourceID) {
        this.id = resourceID;
    }

    public static Map<String, String[]> getSellerData(String dealerIVASSid, String sellerUSERNAME) {
        Processor temp = new Processor(dealerIVASSid);
        Map<String, String[]> data_dealer = temp.getData();
        String sellerFirstName = sellerUSERNAME.substring(1).toUpperCase();
        for (Map.Entry<String, String[]> entry : data_dealer.entrySet()) {
            String key = entry.getKey();
            String[] value = entry.getValue();
            if (key.contains("Responsabili"))
                for (String accountable : value) {
                    if (accountable.contains(sellerFirstName)) {
                        Map<String, String[]> map = (new Processor(accountable.split("@")[1].trim())).getData();
                        return map;
                    }
                }
        }
        return (Map)new HashMap<>();
    }

    public Map<String, String[]> getData() {
        TrustManager[] trustAllCerts = { new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {}

            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        } };
        SSLContext sc = null;
        try {
            sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            CloseableHttpClient client = HttpClientBuilder.create().setSSLContext(sc).build();
            this.data = (Map)new HashMap<>();
            Document doc = null;
            RequestBuilder request_builder = buildRequest("https://servizi.ivass.it/RuirPubblica/DetailResult.faces?id=" + this.id);
            HttpUriRequest request = request_builder.build();
            CloseableHttpResponse closeableHttpResponse = client.execute(request);
            String body = EntityUtils.toString(closeableHttpResponse.getEntity());
            doc = Jsoup.parseBodyFragment(body);
            Elements tableRows = doc.select("tr");
            for (Element row : tableRows) {
                System.out.println(row.text());
                if (row.getElementsByClass("detailTableColDesc").size() > 0) {
                    String prop_name = ((Element)row.getElementsByClass("detailTableColDesc").get(0)).text().replace(":", "");
                    String prop_value = ((Element)row.getElementsByTag("td").get(1)).text();
                    if (prop_name.contains("Responsabili") || prop_name.contains("Intermediari") || prop_name.contains("Addetti")) {
                        int counter = ((Element)row.getElementsByTag("td").get(1)).select("a").size();
                        Elements links = ((Element)row.getElementsByTag("td").get(1)).select("a");
                        String[] accountables = new String[counter];
                        for (int i = 0; i < counter; i++)
                            accountables[i] = ((Element)links.get(i)).text() + " @ " + ((Element)links.get(i)).attr("href").replace("DetailResult.faces?id=", "");
                        this.data.put(prop_name.trim(), accountables);
                        continue;
                    }
                    this.data.put(prop_name.trim(), new String[] { prop_value });
                }
            }
        } catch (IOException|java.security.NoSuchAlgorithmException|java.security.KeyManagementException e) {
            e.printStackTrace();
        }
        return this.data;
    }
}
