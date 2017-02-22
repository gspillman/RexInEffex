package com.bazaarvoice.RexInEffex;

public class QueryStrings {
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