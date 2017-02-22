package com.bazaarvoice.RexInEffex;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.jayway.jsonpath.*;
import net.minidev.json.JSONArray;
import org.apache.commons.csv.CSVPrinter;
import org.joda.time.DateTime;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class RexInEffex {

    public static String localPath;
    public static String fileName;
    public static String reportFileName;
    public static String errorFileName = "ClientErrors";
    public static List<Results> resultToCsv = new ArrayList();
    public static QueryStrings queries = new QueryStrings();

    public static void main(String[] args) throws IOException {
        setFilePaths();
        List clientNames = setInputArgs(args);
        startClientProcessing(setTrendingUrl(), clientNames);
        buildCsvFile(reportFileName, resultToCsv);
    }

    //Process the trending products query if we have a CSV input file provided

    public static void startClientProcessing(String trendingUrl, List clientNames) throws IOException {

        int count = 0;

        while (count < clientNames.size()) {

            //We need to instantiate a new results object per client name in the csv file otherwise we will overwrite
            //our results before we can print to the output file.
            Results result = new Results();

            result.clientName = clientNames.get(count).toString();
            String savedInitialResponse = restfulHelper(trendingUrl + clientNames.get(count).toString());
            Boolean gate;

            //If a pid is not present in the response, we are confident that no trending data is currently available from the client
            if (!savedInitialResponse.toString().contains("pid")) {
                System.out.println(result.clientName + " does not appear to have any trending products.  Check client name in Workbench or client implementation.");

            } else {

                gate = processTrending(result, savedInitialResponse);

                if (gate == true) {
                    visitTrendingProduct(result);
                    visitRecommendationsApi(result);
                }
                calculateScore(result, count);
            }
            count++;
        }
    }

    public static Boolean processTrending(Results result, String savedInitialResponse) throws IOException {

        Boolean processTrendingProduct;

        //Obtains total trending products #
        JSONArray trendingArrayAll = JsonPath.read(savedInitialResponse, queries.totalTrendingQuery);
        result.numTrendingProdsTotal = trendingArrayAll.size();

        //Obtains total available trending products #
        JSONArray trendingArrayAvailable = JsonPath.read(savedInitialResponse, queries.totalAvailableTrendingQuery);
        result.numAvailableTrendingProds = trendingArrayAvailable.size();
        if (result.numAvailableTrendingProds > 0) {
            result.hasSomeProductReviews = true;
        } else {
            result.hasSomeProductReviews = false;
        }

        //Obtains the trending conversion products #

        JSONArray trendingConversionsAll = JsonPath.read(savedInitialResponse, queries.totalTrendingConversionsQuery);
        result.numTrendingConversion = trendingConversionsAll.size();

        //Hunts for sets of trending URL strings to run through magpie/recommendations API

        int trendingCount = 0;
        JSONArray singleTrendingAvailableProductUrl;
        JSONArray allTrendingAvailableProductUrls = new JSONArray();

        singleTrendingAvailableProductUrl = JsonPath.read(trendingArrayAvailable, queries.singleTrendingProductURLQuery);

        while (trendingCount <= result.numAvailableTrendingProds) {
            allTrendingAvailableProductUrls = JsonPath.read(savedInitialResponse, "$.productViews[" + trendingCount + "][?(@.product.num_reviews >= 1 && @.available == true)]");

            if (singleTrendingAvailableProductUrl.toString().contains("http://") || singleTrendingAvailableProductUrl.toString().contains("https://")) {
                trendingCount = result.numAvailableTrendingProds;
            } else {
                trendingCount++;
            }
        }

        result.productId = JsonPath.read(allTrendingAvailableProductUrls, queries.singleTrendingProductIDQuery).toString();

        //Trims down the product ID from the JSON string object for better storage and partly checks for a valid ID

        if (result.productId.length() < 5) {
            result.productId = "Unable to determine product ID";
        } else {
            result.productId = result.productId.substring(2, result.productId.length() - 2);
        }

        result.recommendedInterests = JsonPath.read(allTrendingAvailableProductUrls, queries.singleTrendingProductInterest).toString();

        //Trims down the product recommendations tuple from the JSON string object for better storage

        if (result.recommendedInterests.length() < 5) {
            result.recommendedInterests = "Unable to determine recommended interests";
        } else {
            result.recommendedInterests = result.recommendedInterests.substring(2, result.recommendedInterests.length() - 2);
        }

        //Sets the first product URL we have from the obtained array

        if (!singleTrendingAvailableProductUrl.isEmpty()) {
            result.exampleProdUrl = singleTrendingAvailableProductUrl.get(0).toString();
        }

        if (result.exampleProdUrl == null) {
            result.exampleProdUrl = "URL not found";
        }

        //Very simple URL validator - note - some client URLS can be strange - beyond hunting for http/https we can't do a lot here without blowing up

        if (!result.exampleProdUrl.contains("https://") && !result.exampleProdUrl.contains("http://")) {

            System.out.println("Product URL, " + result.exampleProdUrl + " does not appear valid for client: " + result.clientName + ". Please check your client ID in Workbench or EDR status and try again.");
            processTrendingProduct = false;

        } else {
            processTrendingProduct = true;
        }
        return processTrendingProduct;
    }

    public static void visitTrendingProduct(Results result) throws IOException {

        //Here, we're going to build a QND instance of Webdriver (using Firefox/Marionette) to test if our selected product URL
        //will trigger Magpie well enough for us to potentially recommend something.

        WebDriver driver;
        String localPathToMarionette = localPath+"/src/main/resources/geckodriver";
        System.setProperty("webdriver.gecko.driver", localPathToMarionette);
        driver = new FirefoxDriver();

        try {
            driver.get(result.exampleProdUrl);
        }
        catch (WebDriverException e) {
            System.out.println("Product URL: " + result.exampleProdUrl + " for client: " + result.clientName + " appears broken - unable to visit via webdriver");
        }
        //Some clients may have have products we do not have authorization to view, this is a check for said case

        Alert alert = null;

        try {
            alert = ExpectedConditions.alertIsPresent().apply(driver);
        }
        catch (TimeoutException e) {
            //Do nothing at this point
        }

        if (alert != null) {
            System.out.println("Client, " + result.clientName + " may have authentication issues with their product URLs.  Skipping to next check");
        }
        else {

            try {
                //Thread sleep bad!  But fire also bad!  You can cook with fire!  Sleep now in the fire!

                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //This is a JS instruction call to webdriver to scroll to the bottom of the page - to help facilitate triggering Magpie
            try {
                driver.findElement(By.tagName("body")).sendKeys(Keys.PAGE_DOWN);
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //Setting the URL to the Recommendations API

            String apiUrl = "http://network.bazaarvoice.com/id.json";

            //Scraping a BV ID from Magpie/The Bazaarvoice API then killing our webdriver instance.

            driver.get(apiUrl);
            try {
                String tempString = driver.manage().getCookieNamed("BVID").toString().split("BVID=")[1];
                result.activeBVID = tempString.split(";")[0];
            } catch(Exception e) {
                //currently, do nothing.
            }

        }
            driver.close();
            driver.quit();
    }

    static void visitRecommendationsApi(Results result) throws IOException {

        if (result.activeBVID == null) {
            System.out.println("The current active BVID appears to be null for client: " + result.clientName + ". Please check your client ID in Workbench or EDR status and try again.");
        } else {

            //Calling the API and querying it for various recommendations aspects.

            result.profileApiCall = "https://profiledata-internal-api.prod.us-east-1.nexus.bazaarvoice.com/profiledata/" + result.activeBVID + "?include=*";
            String savedInitialRecsAPIResponse = restfulHelper(result.profileApiCall);
            result.matchedRecsProfile = JsonPath.read(savedInitialRecsAPIResponse, queries.recommendationsRelatedProfilesQuery).toString().contains(result.activeBVID);
            result.matchedRecsInterests = JsonPath.read(savedInitialRecsAPIResponse, queries.recommendationsInterestQuery).toString().contains(result.recommendedInterests);
            result.matchedRecsPageViews = JsonPath.read(savedInitialRecsAPIResponse, queries.recommendationsSourceViewsQuery).toString().contains("1");
            result.matchedRecsProdId = JsonPath.read(savedInitialRecsAPIResponse, queries.recommendationsRefProductsQuery).toString().contains(result.productId);
        }
    }

    static void calculateScore(Results result, int count) throws IOException {

        //Based on the stats per instance of a given record, we are going to calculate a score based on weights.
        //Weights are defined in the Results class.
        //We are dividing the total score by our number of weighted heuristics to reach a scholastic score
        //Typical High School Grading (tm) - 90-100 = A, 80-89 = B, 70-79 = C, 60-69 = D, 60 > = F

        result.totalScore +=  (result.numAvailableTrendingProds * result.numAvailableTrendingProdsWEIGHT);
        result.totalScore += (result.numTrendingProdsTotal * result.numTrendingProdsTotalWEIGHT);
        result.totalScore += (result.numTrendingConversion * result.numTrendingConversionWEIGHT);

        if (!result.exampleProdUrl.contains("Unable to determine")) {
            result.totalScore += result.exampleProdUrlWEIGHT;
        }

        if (result.hasSomeProductReviews = true) {
            result.totalScore += result.hasSomeProductReviewsWEIGHT;
        }

        if (!result.recommendedInterests.contains("Unable to determine")) {
            result.totalScore += result.recommendedInterestsWEIGHT;
        }

        if (!result.productId.contains("Unable to determine")) {
            result.totalScore += result.productIdWEIGHT;
        }

        if (result.matchedRecsProfile = true) {
            result.totalScore += result.matchedRecsProfileWEIGHT;
        }

        if (result.matchedRecsInterests = true) {
            result.totalScore += result.matchedRecsInterestsWEIGHT;
        }

        if (result.matchedRecsPageViews = true) {
            result.totalScore += result.matchedRecsPageViewsWEIGHT;
        }

        if (result.matchedRecsProdId = true) {
            result.totalScore += result.matchedRecsProdIdWEIGHT;
        }

        /*
          Currently not calculating this in consideration for score:
          result.totalScore += result.activeBVIDWEIGHT;
        */

        if (result.totalScore > 0) {

            result.totalScore = (result.totalScore / result.scoreDivider);
        } else {
            result.totalScore = 0;
        }

        resultToCsv.add(result);
        //resultToCsv.add(count, result);
    }

    //Builds the CSV report file and plops it into your local /logs directory

    private static void buildCsvFile(String csvFileName, List<Results> resultToCsv) throws IOException {

        final String NEW_LINE_DEMARCATOR = "\n";

        final Object [] OUTPUT_FILE_HEADER = {"Client", "Trending_Product_Views", "Available_Trending_Product_Views", "Trending_Conversions", "Trending_Products_Has_Reviews", "Product_ID", "Product_ID_Matches_Recommendations",
                "Trending_Product_URL", "Recorded_Interests", "Interests_Match_Recommendations", "Recommended_Pageview_Matches", "Recommendations_URL", "Score"};

        FileWriter fileWriter = null;
        CSVPrinter csvFilePrinter = null;

        CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_DEMARCATOR);

        try {
            fileWriter = new FileWriter(csvFileName);
            csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
            csvFilePrinter.printRecord(OUTPUT_FILE_HEADER);

            for (Results s : resultToCsv) {

                csvFilePrinter.printRecord(s.clientName, s.numTrendingProdsTotal, s.numAvailableTrendingProds, s.numTrendingConversion, s.hasSomeProductReviews, s.productId, s.matchedRecsProdId,
                        s.exampleProdUrl, s.recommendedInterests, s.matchedRecsInterests, s.matchedRecsPageViews, s.profileApiCall, s.totalScore);
            }

        } finally {
            try {
                fileWriter.flush();
                fileWriter.close();
                csvFilePrinter.close();
            } catch (IOException e) {
                System.out.println("Error while flushing/closing fileWriter/csvPrinter !!!");
                e.printStackTrace();
            }
        }

    }

    public static String restfulHelper(String httpResource) {

        Client client = Client.create();
        WebResource webResource = client
                .resource(httpResource);
        ClientResponse response = null;
        response = webResource.accept("application/json")
                .get(ClientResponse.class);

        if (response.getStatus() != 200) {
            throw new RuntimeException("Failed : HTTP error code : "
                    + response.getStatus());
        }
        return response.getEntity(String.class);
    }

    public static void setFilePaths() {

        //Sets our local file path
        try {
            localPath = new java.io.File( "." ).getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //We're going to overload the System.out feature so we can easily log errors as we process each client
        PrintStream out = null;
        try {
            out = new PrintStream(new FileOutputStream(localPath+"/logs/" + errorFileName + ".txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        System.setOut(out);

        //The output file path/name is generated here
        reportFileName = localPath+"/logs/ClientReport" + DateTime.now().toString() + ".csv";
    }

    public static List setInputArgs(String [] args) throws IOException {

        CSVParser csvFileParser;
        List returnRecords;
        List clients = new ArrayList<String>();

        //If the executable argument contains .csv, assuming that this is a csv file of client names to process.
        //Will generate list based on file and process each client accordingly.
        if (args[0].length() > 4 && args[0].substring(args[0].length() - 4).contains(".csv")) {
            fileName = localPath + "/" + args[0];
            FileReader reader = new FileReader(fileName);

            final String[] FILE_HEADER_MAPPING = {"client"};
            CSVFormat csvFileFormat = CSVFormat.DEFAULT.withHeader(FILE_HEADER_MAPPING);
            csvFileParser = new CSVParser(reader, csvFileFormat);
            returnRecords = csvFileParser.getRecords();

            for (int i = 1; i < returnRecords.size(); i++) {
                CSVRecord record = (CSVRecord) returnRecords.get(i);
                clients.add(record.get("client"));
            }
        }
        //If .csv is not specified at the end of the file, assuming that the user is requesting report for a single client,
        //creates list containing just that single client and processes them accordingly.
        else {
            clients.add(0, args[0]);
        }
        return clients;
    }

    public static String setTrendingUrl() {

        //Our trending API URL is set here
        String trendingURL =  "https://profiledata-internal-api.prod.us-east-1.nexus.bazaarvoice.com/debug/trending/";

        if (trendingURL == null) {
            throw new IllegalArgumentException("Trending URL cannot be null.");
        }

        return trendingURL;
    }
}