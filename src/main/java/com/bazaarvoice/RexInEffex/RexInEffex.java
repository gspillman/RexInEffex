package com.bazaarvoice.RexInEffex;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.jayway.jsonpath.*;
import net.minidev.json.JSONArray;


class RexInEffex {

    public static class Results {

        int numAvailableTrendingProds;
        int numTrendingProdsTotal;
        int numTrendingConversion;
        String exampleProdUrl;
        String clientName;
        Boolean hasSomeProductReviews;
    }

    public static void main(String[] args) throws Exception {

        String clientName = args[0];

        String envType = args[1].toLowerCase();

        String trendingURL = null;

        if (envType.equals("qa")) {
            trendingURL = "https://profiledata-internal-api.qa.us-east-1.nexus.bazaarvoice.com/debug/trending/" + clientName;
        }
        else if (envType.equals("prod") || envType.equals("production") || envType.equals("")) {
            trendingURL = "https://profiledata-internal-api.prod.us-east-1.nexus.bazaarvoice.com/debug/trending/" + clientName;

        }

        viewTrendingProducts(trendingURL, clientName);

    }

    public static void viewTrendingProducts(String trendingUrl, String clientName) {

        if (trendingUrl == null) {
            throw new IllegalArgumentException("Trending URL cannot be null.");
        }

        Results result = new Results();
        result.clientName = clientName;

        Client client = Client.create();

        WebResource webResource = client
                .resource(trendingUrl);

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

        String savedInitialResponse = response.getEntity(String.class);

        //total trending #
        String totalTrendingQuery = "$.productViews";
        JSONArray trendingArrayAll = JsonPath.read(savedInitialResponse, totalTrendingQuery);
        result.numTrendingProdsTotal = trendingArrayAll.size();

        //Available trending #
        String totalAvailableTrendingQuery = "$.productViews[?(@.product.num_reviews >= 10 && @.available == true)]";
        JSONArray trendingArrayAvailable = JsonPath.read(savedInitialResponse, totalAvailableTrendingQuery);
        result.numAvailableTrendingProds = trendingArrayAvailable.size();
        if (result.numAvailableTrendingProds > 0) {
            result.hasSomeProductReviews = true;
        } else {
            result.hasSomeProductReviews = false;
        }

        //trending conversion #
        String totalTrendingConversionsQuery = "$.productConversions";
        JSONArray trendingConversionsAll = JsonPath.read(savedInitialResponse, totalTrendingConversionsQuery);
        result.numTrendingConversion = trendingConversionsAll.size();

        //trending URL string
        String productUrlFromAvailableTrendingQueryPartA = "$.productViews[0][?(@.product.num_reviews >= 10 && @.available == true)]";
        String productUrlFromAvailableTrendingQueryPartB = "$..product_page_url";
        JSONArray allTrendingAvailableProductUrls = JsonPath.read(savedInitialResponse, productUrlFromAvailableTrendingQueryPartA);
        JSONArray singleTrendingAvailableProductUrl = JsonPath.read(allTrendingAvailableProductUrls, productUrlFromAvailableTrendingQueryPartB);
        result.exampleProdUrl = singleTrendingAvailableProductUrl.get(0).toString();

        //Simple sys outs to prove we have a report - we can delete this later when we implement the report class

        System.out.println("Your client is: " + result.clientName);
        System.out.println("Total trending products for client: " + result.numTrendingProdsTotal);
        System.out.println("Available trending products: " + result.numAvailableTrendingProds);
        System.out.println("Current number of recent trending conversions: " + result.numTrendingConversion);
        System.out.println("Client currently has at least an available product with reviews: " + result.hasSomeProductReviews);
        System.out.println("You can view a currently trending, available product here: " + result.exampleProdUrl);

        VisitTrendingProduct(result.exampleProdUrl, result);

    }


    public static void VisitTrendingProduct(String productUrl, Results result) {

        //Get ready for some webdriver fun!

        System.out.println("Our method call is compelte!");

    }

    //static WebDriver driver;
    // static WebDriverWait driverWait;

    //accept main arg as a string (this will be a client name

    //use jersey/jackson to make a request to the trending API in prod

    //Collect the first trending product URL

    //Optionally - collect the first trending product w/ reviews

    //Open webdriver - go to that product

    //Grab the browser cookie and save it

    //Call the recommendations API with that cookie

    //Verify the product is there

    //Verify something else?

    /*
    public void findTrendingProduct(String[] args) {

        //Given the user input the client's site, visit the client's site

        driver = new FirefoxDriver();
        driverWait = new WebDriverWait(driver, 5);
        driver.get("http://" + args[0]);


    }
    */

}
