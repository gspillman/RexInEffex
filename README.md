# RexInEffex

What:

RexInEffex is a simple Java application used to probe and then report on a series of product services for a
given e-commerce client to determine to 'health' of a client prior to integrating a recommendations engine
into the client's site.

Requirements:
  * Java 8
  * Firefox (for real-time browser testing)
  * Access to the trending products and recommendations API
  * Maven

Usage:
  Once RexInEffex has been cloned from this repository, you can fire it off using Maven:

  -- mvn exec:java -Dexec.mainClass="com.bazaarvoice.RexInEffex.RexInEffex" -Dexec.args="<client name> <envionment>"
  -- Note - environment flag is optional.  Choices are QA or Production.  If no flag is specific, test will execute in production

Report:
  Client reports, once generated are stored in logs/ClientReport.txt.  The report will cover the following metrics:

  -- Client name
  -- Number of trending page views found for the client via API (max is 100)
  -- Number of those trending page view products that are marked as available
  -- Number of trending conversions for the client (max is 100)
  -- If at least one available, trending product for the client has 1 or more reviews
  -- The product ID of that product
  -- The tier 1 and/or tier 2 interest associated with that product (if available)
  -- The URL for that product's page
  -- Whether or not the product recommendations was able to match the product's interests from the trending API
  -- Whether or not viewing the given product resulted in a single page view for it in the recommendaints API
  -- If the given product's ID was matched between the trending and recommendations API
  -- The URL to the recommendations API we used for comparison (tracking ID included as part of the URI).

