package com.bazaarvoice.RexInEffex;

public class Results {
    int numAvailableTrendingProds;
    int numTrendingProdsTotal;
    int numTrendingConversion;
    String exampleProdUrl;
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

    int totalScore = 0;
    int scoreDivider = 12;
    int numAvailableTrendingProdsWEIGHT = 1;
    int numTrendingProdsTotalWEIGHT = 1;
    int numTrendingConversionWEIGHT = 1;
    int exampleProdUrlWEIGHT = 100;
    int hasSomeProductReviewsWEIGHT = 100;
    int activeBVIDWEIGHT = 100;
    int recommendedInterestsWEIGHT = 100;
    int productIdWEIGHT = 100;
    int matchedRecsProfileWEIGHT = 100;
    int matchedRecsInterestsWEIGHT = 100;
    int matchedRecsPageViewsWEIGHT = 100;
    int matchedRecsProdIdWEIGHT = 100;

}