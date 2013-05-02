/**
 * Copyright 2013-2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at
 *
 *     http://aws.amazon.com/apache2.0/
 *
 *     or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.amazon.aws.supportapi.samples;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.support.AWSSupport;
import com.amazonaws.services.support.AWSSupportClient;
import com.amazonaws.services.support.model.*;

import java.util.*;

/**
 * Sample class to get Trusted Advisor Stats.
 */
public class TrustedAdvisorStats {

	public static void main(String[] args) {
		AWSSupportClient support = new AWSSupportClient(new DefaultAWSCredentialsProviderChain());

		/**
		 * Get a map of checkId to which category it is in.
		 */
		final Map<String, String> checkId2CategoryMap = getChecks(support);

		/**
		 * Get the Trusted Advisor Summaries for all checks.
		 */
		DescribeTrustedAdvisorCheckSummariesRequest request = new DescribeTrustedAdvisorCheckSummariesRequest();
		request.setCheckIds(new LinkedList<String>(checkId2CategoryMap.keySet()));
		DescribeTrustedAdvisorCheckSummariesResult result = support.describeTrustedAdvisorCheckSummaries(request);
		List<TrustedAdvisorCheckSummary> summaries = result.getSummaries();

		/**
		 * Create a map of category to category totals.
		 */
		Map<String, TrustedAdvisorAggregatedCheckSummary> category2TotalMap = new HashMap<String, TrustedAdvisorAggregatedCheckSummary>() {{
			Set<String> categories = new HashSet<String>(checkId2CategoryMap.values());
			for (String category : categories) {
				put(category, new TrustedAdvisorAggregatedCheckSummary());
			}
		}};

		/**
		 * Cycle through all summaries and update the total of the respective category.
		 */
		for (TrustedAdvisorCheckSummary summary : summaries) {
			String checkId = summary.getCheckId();
			String category = checkId2CategoryMap.get(checkId);
			TrustedAdvisorAggregatedCheckSummary aggregatedTotal = category2TotalMap.get(category);
			aggregatedTotal.add(summary);
		}

		/**
		 * Print out the totals.
		 */
		for (Map.Entry<String, TrustedAdvisorAggregatedCheckSummary> entry : category2TotalMap.entrySet()) {
			String category = entry.getKey();
			TrustedAdvisorAggregatedCheckSummary aggregatedTotal = entry.getValue();
			System.out.println(category + " (" + aggregatedTotal.getCategoryStatus() + "): " + aggregatedTotal);
		}
	}

	private static Map<String, String> getChecks(AWSSupport support) {
		DescribeTrustedAdvisorChecksRequest request = new DescribeTrustedAdvisorChecksRequest().withLanguage("en");
		DescribeTrustedAdvisorChecksResult checks = support.describeTrustedAdvisorChecks(request);

		Map<String, String> map = new HashMap<String, String>();

		for (TrustedAdvisorCheckDescription check : checks.getChecks()) {
			map.put(check.getId(), check.getCategory());
		}

		return map;
	}

	/**
	 * An enumerated list of all the different category statuses.
	 */
	private static enum CategoryStatus {
		not_available, ok, warning, error;

		CategoryStatus getWorst(CategoryStatus status) {
			if (status.ordinal() < ordinal()) {
				return this;
			} else {
				return status;
			}
		}
	}

	private final static class TrustedAdvisorAggregatedCheckSummary extends TrustedAdvisorResourcesSummary {
		private CategoryStatus categoryStatus = CategoryStatus.not_available;

		public void add(TrustedAdvisorCheckSummary checkSummary) {
			TrustedAdvisorResourcesSummary resourcesSummary = checkSummary.getResourcesSummary();
			CategoryStatus categoryStatus = CategoryStatus.valueOf(checkSummary.getStatus());
			this.categoryStatus = this.categoryStatus.getWorst(categoryStatus);
			setResourcesFlagged(safeAdd(getResourcesFlagged(), resourcesSummary.getResourcesFlagged()));
			setResourcesIgnored(safeAdd(getResourcesIgnored(), resourcesSummary.getResourcesIgnored()));
			setResourcesProcessed(safeAdd(getResourcesProcessed(), resourcesSummary.getResourcesProcessed()));
			setResourcesSuppressed(safeAdd(getResourcesSuppressed(), resourcesSummary.getResourcesSuppressed()));
		}

		private long safeAdd(Long l1, Long l2) {
			return safeGet(l1) + safeGet(l2);
		}

		private long safeGet(Long l) {
			if (l == null) {
				return 0;
			} else {
				return l;
			}
		}

		public CategoryStatus getCategoryStatus() {
			return categoryStatus;
		}

	}
}
