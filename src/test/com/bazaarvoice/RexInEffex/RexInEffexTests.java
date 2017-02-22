package com.bazaarvoice.RexInEffex;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;


public class RexInEffexTests {

    @Test
    public void calculateScoreWithEmptyResults() throws IOException {

        Results r = genEmptyResult();

        RexInEffex rex = new RexInEffex();
        rex.calculateScore(r, 1);
        Assert.assertTrue(r.totalScore == r.exampleProdUrlWEIGHT + r.hasSomeProductReviewsWEIGHT + r.matchedRecsInterestsWEIGHT + r.productIdWEIGHT +
                r.matchedRecsProdIdWEIGHT + r.matchedRecsInterestsWEIGHT + r.matchedRecsProfileWEIGHT + r.matchedRecsPageViewsWEIGHT);


    }

    @Test
    public void calculateScoreWithNormalResults() throws IOException {

        Results r = genFullResult();

        RexInEffex rex = new RexInEffex();
        rex.calculateScore(r, 1);
        Assert.assertTrue(r.totalScore == r.numAvailableTrendingProds * r.numAvailableTrendingProdsWEIGHT + r.numTrendingProdsTotal * r.numTrendingProdsTotalWEIGHT + r.numTrendingConversion * r.numTrendingConversionWEIGHT + r.exampleProdUrlWEIGHT + r.hasSomeProductReviewsWEIGHT + r.matchedRecsInterestsWEIGHT + r.productIdWEIGHT +
                r.matchedRecsProdIdWEIGHT + r.matchedRecsInterestsWEIGHT + r.matchedRecsProfileWEIGHT + r.matchedRecsPageViewsWEIGHT);
    }

    @Test
    public void restfulHelperHandlesNormalUrl() {

        RexInEffex rex = new RexInEffex();
        Assert.assertNotNull(rex.restfulHelper("http://www.bazaarvoice.com"));

    }

    @Test
    public void restfulHelperHandlesMalformedUrl() {

        RexInEffex rex = new RexInEffex();
        Assert.assertNull(rex.restfulHelper("badUrl."));

    }

    @Test
    public void purposelyFailingTest() {

        Assert.assertTrue(2 + 2 == 5);

    }

    public Results genFullResult() {

        Results r = new Results();

        r.numTrendingProdsTotal = 100;
        r.numTrendingConversion = 100;
        r.exampleProdUrl = "http://testURL.com";
        r.clientName = "testClient";
        r.hasSomeProductReviews = true;
        r.activeBVID = "abcdefghijklmnop1234567890!";
        r.recommendedInterests = "test interest & test other interest";
        r.productId = "123456";
        r.matchedRecsProfile = true;
        r.matchedRecsInterests = true;
        r.matchedRecsPageViews = true;
        r.matchedRecsProdId = true;
        r.profileApiCall = "http://testApiCall";

        return r;
    }

    public Results genEmptyResult() {

        Results r = new Results();

        r.numTrendingProdsTotal = 0;
        r.numTrendingConversion = 0;
        r.exampleProdUrl = null;
        r.clientName = null;
        r.hasSomeProductReviews = null;
        r.activeBVID = null;
        r.recommendedInterests = null;;
        r.productId = null;
        r.matchedRecsProfile = null;
        r.matchedRecsInterests = null;
        r.matchedRecsPageViews = null;
        r.matchedRecsProdId = null;
        r.profileApiCall = null;

        return r;
    }

    /*

    Test shit:

    List clientNames = new ArrayList<String>();
        String singleClientName = null;

        need to test if we have 1 or the other instantiated, you get one or the other method call

        viewTrendingProducts(String trendingUrl, List clientNames)

        buildCsvFile(reportFileName, resultToCsv);

        viewTrendingProducts(String trendingUrl, String singleClientName)

        processTrending(Results result, String savedInitialResponse, int count)

        VisitTrendingProduct(Results result, QueryStrings queries, int count)

        VisitRecommendationsApi(Results result, QueryStrings queries, int count)

        buildCsvFile(String csvFileName, List<Results> resultToCsv)
     */


}
