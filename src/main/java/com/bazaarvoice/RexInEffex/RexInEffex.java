package com.bazaarvoice.RexInEffex;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.jayway.jsonpath.*;
import net.minidev.json.JSONArray;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

class RexInEffex {

    public static class Results {

        int numAvailableTrendingProds;
        int numTrendingProdsTotal;
        int numTrendingConversion;
        String exampleProdUrl = "No available trending product page available";
        String clientName;
        Boolean hasSomeProductReviews;
        String activeBVID;
        String recommendedInterests;
        String productId;
        Boolean matchedRecsProfile;
        Boolean matchedRecsInterests;
        Boolean matchedRecsPageViews;
        Boolean matchedRecsProdId;
        String profileApiCall;
    }

    public static class QueryStrings {
        String totalTrendingQuery = "$.productViews";
        String totalAvailableTrendingQuery = "$.productViews[?(@.product.num_reviews >= 1 && @.available == true)]";
        String totalTrendingConversionsQuery = "$.productConversions";
        String singleTrendingProductURLQuery = "$..product_page_url";
        String singleTrendingProductIDQuery = "$..product.product";
        String singleTrendingProductInterest = "$..interests";
        String recommendationsRelatedProfilesQuery = "$..related_profiles";
        String recommendationsInterestQuery = "$..interests";
        String recommendationsSourceViewsQuery = "$..recommendations_source.num_product_views";
        String recommendationsRefProductsQuery = "$..referenced_products";
    }

    public static String envType;

    public static String localPath;

    public static void main(String[] args) {

        try {
            localPath = new java.io.File( "." ).getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String clientName = args[0];

        if (args.length < 2) {
            envType = "prod";
        } else {
            envType = args[1].toLowerCase();
        }

        String trendingURL = null;

        if (envType.equals("qa")) {
            trendingURL = "https://profiledata-internal-api." + envType + ".us-east-1.nexus.bazaarvoice.com/debug/trending/" + clientName;
        }
        else {
            envType = "prod";
            trendingURL = "https://profiledata-internal-api." + envType + ".us-east-1.nexus.bazaarvoice.com/debug/trending/" + clientName;
        }

        viewTrendingProducts(trendingURL, clientName);

    }

    public static void viewTrendingProducts(String trendingUrl, String clientName) {

        if (trendingUrl == null) {
            throw new IllegalArgumentException("Trending URL cannot be null.");
        }

        Results result = new Results();
        QueryStrings queries = new QueryStrings();

        result.clientName = clientName;

        String savedInitialResponse = restfulHelper(trendingUrl);

        //total trending #
        JSONArray trendingArrayAll = JsonPath.read(savedInitialResponse, queries.totalTrendingQuery);
        result.numTrendingProdsTotal = trendingArrayAll.size();

        //Available trending #
        JSONArray trendingArrayAvailable = JsonPath.read(savedInitialResponse, queries.totalAvailableTrendingQuery);
        result.numAvailableTrendingProds = trendingArrayAvailable.size();
        if (result.numAvailableTrendingProds > 0) {
            result.hasSomeProductReviews = true;
        } else {
            result.hasSomeProductReviews = false;
        }

        //trending conversion #

        JSONArray trendingConversionsAll = JsonPath.read(savedInitialResponse, queries.totalTrendingConversionsQuery);
        result.numTrendingConversion = trendingConversionsAll.size();

        //finding a trending URL string

        int trendingCount = 0;
        JSONArray singleTrendingAvailableProductUrl = null;
        while (trendingCount < trendingArrayAvailable.size()) {
            JSONArray allTrendingAvailableProductUrls = JsonPath.read(savedInitialResponse, "$.productViews[" + trendingCount + "][?(@.product.num_reviews >= 1 && @.available == true)]");
            if (JsonPath.read(allTrendingAvailableProductUrls, queries.singleTrendingProductURLQuery).toString() != null) {
                  singleTrendingAvailableProductUrl = JsonPath.read(allTrendingAvailableProductUrls, queries.singleTrendingProductURLQuery);
                  result.productId = JsonPath.read(allTrendingAvailableProductUrls, queries.singleTrendingProductIDQuery).toString();

                  if (result.productId.length() < 7) {
                      result.productId = "Unable to determine product ID";
                  } else {
                      result.productId = result.productId.substring(2, result.productId.length() - 2);
                  }

                  result.recommendedInterests = JsonPath.read(allTrendingAvailableProductUrls, queries.singleTrendingProductInterest).toString();

                  if (result.recommendedInterests.length() < 7) {
                      result.recommendedInterests = "Unable to determine recommended interests";
                  } else {
                      result.recommendedInterests = result.recommendedInterests.substring(2, result.recommendedInterests.length() - 2);
                  }

                break;
            } else {
                trendingCount++;
            }

        }

        if (!singleTrendingAvailableProductUrl.isEmpty()) {
            result.exampleProdUrl = singleTrendingAvailableProductUrl.get(0).toString();
        }

        if (result.exampleProdUrl.contains("http://") || result.exampleProdUrl.contains("http://")) {

            VisitTrendingProduct(result, queries);

        } else {
            System.out.println("Product URL does not appear valid - stopping test");
            GenerateReport(result);
        }

    }

    public static void VisitTrendingProduct(Results result, QueryStrings queries) {

        //Instantiate webdriver using geckodriver

        WebDriver driver;
        String localPathToMarionette = localPath+"/src/main/resources/geckodriver";
        System.setProperty("webdriver.gecko.driver", localPathToMarionette);
        driver = new FirefoxDriver();

        //Visit the product URL from the passed results object

        driver.get(result.exampleProdUrl);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ((JavascriptExecutor)driver).executeScript("scroll(0,2000)");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String apiUrl;

        if (envType.equals("qa")) {
            apiUrl = "http://network-stg.bazaarvoice.com/id.json";
        }
        else {
            apiUrl = "http://network.bazaarvoice.com/id.json";
        }

        //Visit the network env and scrape the BV ID

        driver.get(apiUrl);
        String tempString = driver.manage().getCookieNamed("BVID").toString().split("BVID=")[1];
        result.activeBVID = tempString.split(";")[0];

        driver.close();
        driver.quit();

        VisitRecommendationsApi(result, queries);

    }

    static void VisitRecommendationsApi(Results result, QueryStrings queries) {

        //Visit recommendations API with saved BVID and stuff

        if (result.activeBVID == null) {
            System.out.println("The current active BVID appears to be null - stopping test");
            GenerateReport(result);
        }

        result.profileApiCall = "https://profiledata-internal-api." + envType + ".us-east-1.nexus.bazaarvoice.com/profiledata/" + result.activeBVID + "?include=*";
        String savedInitialRecsAPIResponse = restfulHelper(result.profileApiCall);
        result.matchedRecsProfile =  JsonPath.read(savedInitialRecsAPIResponse, queries.recommendationsRelatedProfilesQuery).toString().contains(result.activeBVID);
        result.matchedRecsInterests =  JsonPath.read(savedInitialRecsAPIResponse, queries.recommendationsInterestQuery).toString().contains(result.recommendedInterests);
        result.matchedRecsPageViews = JsonPath.read(savedInitialRecsAPIResponse, queries.recommendationsSourceViewsQuery).toString().contains("1");
        result.matchedRecsProdId = JsonPath.read(savedInitialRecsAPIResponse, queries.recommendationsRefProductsQuery).toString().contains(result.productId);
        GenerateReport(result);
    }

    static void GenerateReport(Results result) {

        //Simple sys outs to prove we have a report - we can delete this later when we implement the report class

        PrintStream out = null;
        try {
            out = new PrintStream(new FileOutputStream(localPath+"/logs/ClientReport.txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        System.setOut(out);

        System.out.println("////////////////////////////////////////////////////////////////////////////////////");

        System.out.println("Your client is: " + result.clientName);
        System.out.println("Total trending products for client: " + result.numTrendingProdsTotal);
        System.out.println("Available trending products: " + result.numAvailableTrendingProds);
        System.out.println("Current number of recent trending conversions: " + result.numTrendingConversion);
        System.out.println("Client currently has at least an available product with reviews: " + result.hasSomeProductReviews);
        System.out.println("Product ID: " + result.productId);
        System.out.println("Current detected product interests: " + result.recommendedInterests);
        System.out.println("You can view a currently trending, available product here: " + result.exampleProdUrl);
        System.out.println("Ability to match recommendations to trending product interest: " + result.matchedRecsInterests);
        System.out.println("Number of recommendations page views were as expected: " + result.matchedRecsPageViews);
        System.out.println("Ability to match recommendations to trending product ID: " + result.matchedRecsProdId);
        System.out.println("Recommendations ID matches one generated from trending product: " + result.matchedRecsProfile);
        System.out.println("You can view the generated recommendations object call here: " + result.profileApiCall);

        System.out.println("////////////////////////////////////////////////////////////////////////////////////");

    }

    //This is our rest helper - use to to talk to any API.  Returns a JSON string.

    static String restfulHelper(String httpResource) {

        Client client = Client.create();

        WebResource webResource = client
                .resource(httpResource);

        ClientResponse response = null;

        try {
            response = webResource.accept("application/json")
                    .get(ClientResponse.class);

            if (response.getStatus() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + response.getStatus());
            }

        } catch (Exception e) {

            e.printStackTrace();

        }

        return response.getEntity(String.class);

    }

}
